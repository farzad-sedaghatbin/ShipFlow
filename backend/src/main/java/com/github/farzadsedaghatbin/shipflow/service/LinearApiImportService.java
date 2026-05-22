package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.ImportJobDTO;
import com.github.farzadsedaghatbin.shipflow.dto.LinearTeamDTO;
import com.github.farzadsedaghatbin.shipflow.dto.LinearImportRequest;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Imports issues, cycles, and projects from the Linear GraphQL API (v1.2.0 S29).
 *
 * <p>Flow:
 * <ol>
 *   <li>Call Linear GraphQL to fetch team data (cycles, projects, issues)
 *   <li>Create a ShipFlow project (KANBAN or SCRUM)
 *   <li>Map Linear cycles → ShipFlow Cycles
 *   <li>Map Linear projects → ShipFlow Epics
 *   <li>Map Linear issues → ShipFlow Tasks
 *   <li>Persist an ImportJob tracking the result
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LinearApiImportService {

  private static final String LINEAR_API_URL = "https://api.linear.app/graphql";

  /** GraphQL query to fetch all data needed for a full team import. */
  private static final String TEAM_DATA_QUERY =
      """
      query TeamData($teamId: String!) {
        team(id: $teamId) {
          id
          name
          cycles {
            nodes {
              id
              name
              number
              startsAt
              endsAt
              completedAt
            }
          }
          projects {
            nodes {
              id
              name
              description
            }
          }
          issues(first: 250) {
            nodes {
              id
              title
              description
              priority
              estimate
              state {
                name
                type
              }
              cycle {
                id
              }
              project {
                id
              }
              assignee {
                name
                email
              }
            }
          }
        }
      }
      """;

  /** GraphQL query to list all teams accessible by the token. */
  private static final String TEAMS_QUERY =
      """
      query {
        teams {
          nodes {
            id
            name
            key
          }
        }
      }
      """;

  private final LinearOAuthService linearOAuthService;
  private final ProjectRepository projectRepository;
  private final CycleRepository cycleRepository;
  private final EpicRepository epicRepository;
  private final TaskRepository taskRepository;
  private final ImportJobRepository importJobRepository;
  private final UserRepository userRepository;
  private final PersonRepository personRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Fetch the list of Linear teams accessible by the stored OAuth token.
   *
   * @param accessToken Linear access token
   * @return list of available teams
   */
  @Transactional(readOnly = true)
  public List<LinearTeamDTO> fetchTeams(String accessToken) {
    JsonNode data = callGraphQL(accessToken, TEAMS_QUERY, null);
    List<LinearTeamDTO> teams = new ArrayList<>();
    JsonNode nodes = data.path("teams").path("nodes");
    if (nodes.isArray()) {
      for (JsonNode n : nodes) {
        teams.add(
            LinearTeamDTO.builder()
                .id(n.path("id").asText())
                .name(n.path("name").asText())
                .key(n.path("key").asText())
                .build());
      }
    }
    log.info("Linear fetchTeams: found {} teams", teams.size());
    return teams;
  }

  /**
   * Full import of a Linear team into ShipFlow. Creates a new project with all cycles,
   * epics, and tasks. Records an ImportJob for tracking.
   *
   * @param request import parameters (teamId, projectName, projectType)
   * @param currentUser the user who triggered the import
   * @return ImportJobDTO describing the result
   */
  public ImportJobDTO importFromLinear(LinearImportRequest request, User currentUser) {
    String accessToken = linearOAuthService.getStoredAccessToken();
    if (accessToken == null || accessToken.isBlank()) {
      throw new IllegalStateException("Linear is not connected. Complete OAuth flow first.");
    }

    // Create import job immediately so the caller gets an id
    ImportJob job =
        ImportJob.builder()
            .fileName("linear-api-import-" + request.getTeamId() + ".json")
            .sourceFormat(ImportSourceFormat.LINEAR_API)
            .status(ImportJobStatus.PENDING)
            .createdBy(currentUser)
            .createdAt(LocalDateTime.now())
            .build();
    job = importJobRepository.save(job);

    try {
      job.setStatus(ImportJobStatus.PARSING);
      importJobRepository.save(job);

      // 1. Fetch team data from Linear
      Map<String, Object> variables = Map.of("teamId", request.getTeamId());
      JsonNode data = callGraphQL(accessToken, TEAM_DATA_QUERY, variables);
      JsonNode team = data.path("team");

      // 2. Determine project type
      ProjectType projectType = resolveProjectType(request.getProjectType());

      // 3. Create ShipFlow project
      Project project = createProject(request.getProjectName(), projectType, currentUser);
      job.setProject(project);
      job.setStatus(ImportJobStatus.IMPORTING);
      importJobRepository.save(job);

      // 4. Build cycle ID → Cycle map
      Map<String, Cycle> cycleMap = importCycles(team.path("cycles").path("nodes"), project);

      // 5. Build Linear project ID → Epic map
      Map<String, Epic> epicMap = importEpics(team.path("projects").path("nodes"), project);

      // 6. Import issues as Tasks
      JsonNode issues = team.path("issues").path("nodes");
      int total = issues.size();
      int imported = 0;
      int failed = 0;
      StringBuilder errorLog = new StringBuilder();

      for (JsonNode issue : issues) {
        try {
          importIssue(issue, project, cycleMap, epicMap);
          imported++;
        } catch (Exception e) {
          failed++;
          String issueId = issue.path("id").asText("?");
          errorLog.append("Issue ").append(issueId).append(": ").append(e.getMessage()).append("\n");
          log.debug("Linear import: issue {} failed", issueId, e);
        }
      }

      job.setTotalRows(total);
      job.setImportedRows(imported);
      job.setFailedRows(failed);
      if (errorLog.length() > 0) {
        job.setErrorLog(errorLog.toString());
      }
      job.setStatus(ImportJobStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());
      log.info(
          "Linear import completed: total={} imported={} failed={}", total, imported, failed);

    } catch (Exception e) {
      log.error("Linear import job {} failed at job level", job.getId(), e);
      job.setStatus(ImportJobStatus.FAILED);
      job.setCompletedAt(LocalDateTime.now());
      job.setErrorLog("Job-level failure: " + e.getMessage());
    }

    return toDTO(importJobRepository.save(job));
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private Map<String, Cycle> importCycles(JsonNode cycleNodes, Project project) {
    Map<String, Cycle> cycleMap = new HashMap<>();
    if (!cycleNodes.isArray()) return cycleMap;

    DateTimeFormatter iso = DateTimeFormatter.ISO_DATE_TIME;
    for (JsonNode node : cycleNodes) {
      String linearId = node.path("id").asText();
      String name =
          node.has("name") && !node.path("name").asText().isBlank()
              ? node.path("name").asText()
              : "Cycle #" + node.path("number").asText();

      LocalDate startDate = parseDate(node.path("startsAt").asText(null), iso);
      LocalDate endDate = parseDate(node.path("endsAt").asText(null), iso);
      boolean completed = !node.path("completedAt").isNull() && !node.path("completedAt").isMissingNode();

      if (startDate == null) startDate = LocalDate.now();
      if (endDate == null) endDate = startDate.plusWeeks(6);

      CyclePhase phase = completed ? CyclePhase.BETTING_COOLDOWN : CyclePhase.SHAPING_BUILDING;

      Cycle cycle =
          Cycle.builder()
              .name(name)
              .project(project)
              .phase(phase)
              .isActive(!completed)
              .startDate(startDate)
              .endDate(endDate)
              .build();
      cycleMap.put(linearId, cycleRepository.save(cycle));
    }
    log.debug("Linear import: created {} cycles", cycleMap.size());
    return cycleMap;
  }

  private Map<String, Epic> importEpics(JsonNode projectNodes, Project project) {
    Map<String, Epic> epicMap = new HashMap<>();
    if (!projectNodes.isArray()) return epicMap;

    for (JsonNode node : projectNodes) {
      String linearId = node.path("id").asText();
      String name = node.path("name").asText();
      if (name.isBlank()) continue;

      String description = node.path("description").asText(null);

      Epic epic =
          Epic.builder()
              .name(name)
              .description(description)
              .project(project)
              .status(EpicStatus.IN_PROGRESS)
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build();
      epicMap.put(linearId, epicRepository.save(epic));
    }
    log.debug("Linear import: created {} epics", epicMap.size());
    return epicMap;
  }

  private void importIssue(
      JsonNode issue,
      Project project,
      Map<String, Cycle> cycleMap,
      Map<String, Epic> epicMap) {

    String title = issue.path("title").asText();
    if (title.isBlank()) {
      throw new IllegalArgumentException("Issue has no title");
    }

    String description = issue.path("description").asText(null);
    TaskStatus status = mapLinearStateType(issue.path("state").path("type").asText(null));
    TaskPriority priority = mapLinearPriority(issue.path("priority").asInt(0));

    // Cycle lookup
    Cycle cycle = null;
    JsonNode cycleNode = issue.path("cycle");
    if (!cycleNode.isMissingNode() && !cycleNode.isNull()) {
      cycle = cycleMap.get(cycleNode.path("id").asText());
    }

    // Story points from estimate
    Integer storyPoints = null;
    JsonNode estimateNode = issue.path("estimate");
    if (!estimateNode.isMissingNode() && !estimateNode.isNull()) {
      storyPoints = estimateNode.asInt();
    }

    // Assignee lookup by email, falling back to name
    Person assignee = null;
    JsonNode assigneeNode = issue.path("assignee");
    if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
      String email = assigneeNode.path("email").asText(null);
      String name = assigneeNode.path("name").asText(null);
      if (email != null && !email.isBlank()) {
        List<Person> found = personRepository.searchByNameOrEmail(email);
        if (!found.isEmpty()) assignee = found.get(0);
      }
      if (assignee == null && name != null && !name.isBlank()) {
        List<Person> found = personRepository.searchByNameOrEmail(name);
        if (!found.isEmpty()) assignee = found.get(0);
      }
    }

    Task task =
        Task.builder()
            .title(title)
            .description(description)
            .status(status)
            .priority(priority)
            .category(TaskCategory.PITCH_SCOPE)
            .project(project)
            .cycle(cycle)
            .assignee(assignee)
            .storyPoints(storyPoints)
            .build();

    taskRepository.save(task);
  }

  // ── Mapping helpers ───────────────────────────────────────────────────────

  /**
   * Map Linear state type strings to ShipFlow TaskStatus.
   * Linear types: backlog, unstarted, started, completed, cancelled
   */
  TaskStatus mapLinearStateType(String stateType) {
    if (stateType == null) return TaskStatus.TODO;
    return switch (stateType.toLowerCase()) {
      case "backlog" -> TaskStatus.BACKLOG;
      case "unstarted" -> TaskStatus.TODO;
      case "started" -> TaskStatus.IN_PROGRESS;
      case "completed" -> TaskStatus.DONE;
      case "cancelled" -> TaskStatus.CANCELLED;
      default -> TaskStatus.TODO;
    };
  }

  /**
   * Map Linear priority integers to ShipFlow TaskPriority.
   * Linear: 0=No priority, 1=Urgent, 2=High, 3=Medium, 4=Low
   */
  TaskPriority mapLinearPriority(int linearPriority) {
    return switch (linearPriority) {
      case 1 -> TaskPriority.URGENT;
      case 2 -> TaskPriority.HIGH;
      case 4 -> TaskPriority.LOW;
      default -> TaskPriority.MEDIUM; // 0 (none) and 3 (medium)
    };
  }

  private ProjectType resolveProjectType(String raw) {
    if (raw == null || raw.isBlank()) return ProjectType.KANBAN;
    return switch (raw.trim().toUpperCase()) {
      case "SCRUM" -> ProjectType.SCRUM;
      case "SHAPE_UP" -> ProjectType.SHAPE_UP;
      default -> ProjectType.KANBAN;
    };
  }

  private Project createProject(String projectName, ProjectType projectType, User owner) {
    String key = generateProjectKey(projectName);
    Project project =
        Project.builder()
            .name(projectName)
            .projectKey(key)
            .projectType(projectType)
            .owner(owner)
            .isActive(true)
            .build();
    return projectRepository.save(project);
  }

  /** Generate a collision-resistant 7-char project key (3 letters + 4 digits). */
  private String generateProjectKey(String name) {
    String base = name.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    if (base.length() > 3) base = base.substring(0, 3);
    else if (base.isEmpty()) base = "LIN";
    String candidate;
    int attempts = 0;
    do {
      int suffix = (int) (Math.random() * 9000) + 1000;
      candidate = base + suffix;
      attempts++;
      if (attempts > 100) {
        candidate = "LIN" + System.currentTimeMillis() % 10000;
        break;
      }
    } while (projectRepository.existsByProjectKey(candidate));
    return candidate;
  }

  // ── GraphQL client ────────────────────────────────────────────────────────

  /**
   * Execute a Linear GraphQL request and return the {@code data} node.
   *
   * @param accessToken Bearer token
   * @param query GraphQL query string
   * @param variables optional variables map (may be null)
   * @return the {@code data} field of the response
   */
  private JsonNode callGraphQL(String accessToken, String query, Map<String, Object> variables) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(accessToken);

    Map<String, Object> requestBody = new LinkedHashMap<>();
    requestBody.put("query", query);
    if (variables != null) {
      requestBody.put("variables", variables);
    }

    String responseBody;
    try {
      responseBody =
          restTemplate.postForObject(
              LINEAR_API_URL, new HttpEntity<>(requestBody, headers), String.class);
    } catch (Exception e) {
      throw new IllegalStateException("Linear GraphQL call failed: " + e.getMessage(), e);
    }

    JsonNode root;
    try {
      root = objectMapper.readTree(responseBody);
    } catch (Exception e) {
      throw new IllegalStateException("Could not parse Linear response: " + responseBody, e);
    }

    if (root.has("errors")) {
      String firstError = root.path("errors").path(0).path("message").asText("unknown");
      throw new IllegalStateException("Linear API returned error: " + firstError);
    }

    return root.path("data");
  }

  // ── Date parsing ──────────────────────────────────────────────────────────

  private LocalDate parseDate(String raw, DateTimeFormatter iso) {
    if (raw == null || raw.isBlank()) return null;
    try {
      // Linear returns ISO-8601 instant strings like "2024-01-15T00:00:00.000Z"
      return LocalDate.parse(raw.substring(0, 10));
    } catch (Exception e) {
      log.debug("Could not parse date '{}': {}", raw, e.getMessage());
      return null;
    }
  }

  // ── DTO conversion ────────────────────────────────────────────────────────

  private ImportJobDTO toDTO(ImportJob job) {
    return ImportJobDTO.builder()
        .id(job.getId())
        .fileName(job.getFileName())
        .sourceFormat(job.getSourceFormat() != null ? job.getSourceFormat().name() : null)
        .status(job.getStatus() != null ? job.getStatus().name() : null)
        .totalRows(job.getTotalRows())
        .importedRows(job.getImportedRows())
        .failedRows(job.getFailedRows())
        .errorLog(job.getErrorLog())
        .projectId(job.getProject() != null ? job.getProject().getId() : null)
        .projectName(job.getProject() != null ? job.getProject().getName() : null)
        .createdAt(job.getCreatedAt())
        .completedAt(job.getCompletedAt())
        .build();
  }
}
