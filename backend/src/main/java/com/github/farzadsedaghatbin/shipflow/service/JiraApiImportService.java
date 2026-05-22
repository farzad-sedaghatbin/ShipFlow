package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.ImportJobDTO;
import com.github.farzadsedaghatbin.shipflow.dto.JiraImportRequest;
import com.github.farzadsedaghatbin.shipflow.dto.JiraProjectDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

/**
 * Imports issues, sprints, and epics from the Jira REST API (v1.2.0 S30).
 *
 * <p>Flow:
 *
 * <ol>
 *   <li>Fetch boards for the project key → obtain the first board id
 *   <li>Fetch sprints from the board → create ShipFlow Cycles
 *   <li>Fetch all issues with JQL (paginated) — issuetype != Sub-task
 *   <li>First pass: create Epics from issues where issuetype = Epic
 *   <li>Create the ShipFlow project
 *   <li>Second pass: create Tasks from non-Epic issues, linking Epics and Cycles
 *   <li>Persist an ImportJob tracking the result
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class JiraApiImportService {

  private static final String JIRA_API_BASE = "https://api.atlassian.com/ex/jira/%s/rest/api/3";
  private static final String JIRA_AGILE_BASE =
      "https://api.atlassian.com/ex/jira/%s/rest/agile/1.0";

  private final JiraOAuthService jiraOAuthService;
  private final ProjectRepository projectRepository;
  private final CycleRepository cycleRepository;
  private final EpicRepository epicRepository;
  private final TaskRepository taskRepository;
  private final ImportJobRepository importJobRepository;
  private final PersonRepository personRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Fetch the list of Jira projects accessible by the stored OAuth token.
   *
   * @param accessToken Jira access token
   * @param cloudId Atlassian cloud id
   * @return list of available projects
   */
  @Transactional(readOnly = true)
  public List<JiraProjectDTO> fetchProjects(String accessToken, String cloudId) {
    String url = apiBase(cloudId) + "/project/search?expand=description&maxResults=50";
    JsonNode root = getJson(accessToken, url);
    List<JiraProjectDTO> projects = new ArrayList<>();
    JsonNode values = root.path("values");
    if (values.isArray()) {
      for (JsonNode p : values) {
        String desc = p.path("description").asText(null);
        projects.add(
            JiraProjectDTO.builder()
                .id(p.path("id").asText())
                .key(p.path("key").asText())
                .name(p.path("name").asText())
                .description(desc != null && desc.isBlank() ? null : desc)
                .build());
      }
    }
    log.info("Jira fetchProjects: found {} projects", projects.size());
    return projects;
  }

  /**
   * Full import of a Jira project into ShipFlow. Creates a new project with sprints (cycles),
   * epics, and tasks. Records an ImportJob for tracking.
   *
   * @param request import parameters (projectKey, projectName, projectType)
   * @param currentUser the user who triggered the import
   * @return ImportJobDTO describing the result
   */
  public ImportJobDTO importFromJira(JiraImportRequest request, User currentUser) {
    String accessToken = jiraOAuthService.getStoredAccessToken();
    String cloudId = jiraOAuthService.getStoredCloudId();
    if (accessToken == null || accessToken.isBlank()) {
      throw new IllegalStateException("Jira is not connected. Complete OAuth flow first.");
    }
    if (cloudId == null || cloudId.isBlank()) {
      throw new IllegalStateException("Jira cloud id is missing. Reconnect the Jira integration.");
    }

    // Create import job so the caller gets an id immediately
    ImportJob job =
        ImportJob.builder()
            .fileName("jira-api-import-" + request.getProjectKey() + ".json")
            .sourceFormat(ImportSourceFormat.JIRA_API)
            .status(ImportJobStatus.PENDING)
            .createdBy(currentUser)
            .createdAt(LocalDateTime.now())
            .build();
    job = importJobRepository.save(job);

    try {
      job.setStatus(ImportJobStatus.PARSING);
      importJobRepository.save(job);

      // 1. Determine project type
      ProjectType projectType = resolveProjectType(request.getProjectType());

      // 2. Create ShipFlow project
      Project project = createProject(request.getProjectName(), projectType, currentUser);
      job.setProject(project);
      job.setStatus(ImportJobStatus.IMPORTING);
      importJobRepository.save(job);

      // 3. Fetch board id for sprint access
      OptionalInt boardIdOpt = fetchFirstBoardId(accessToken, cloudId, request.getProjectKey());

      // 4. Build sprint id → Cycle map (only if a board was found)
      Map<Integer, Cycle> sprintCycleMap = new HashMap<>();
      if (boardIdOpt.isPresent()) {
        sprintCycleMap = importSprints(accessToken, cloudId, boardIdOpt.getAsInt(), project);
      } else {
        log.info("Jira import: no agile board found for project {}", request.getProjectKey());
      }

      // 5. Fetch all issues (paginated, exclude Sub-tasks)
      List<JsonNode> allIssues =
          fetchAllIssues(accessToken, cloudId, request.getProjectKey());
      log.info("Jira import: fetched {} issues total", allIssues.size());

      // 6. First pass: build Jira epic key → ShipFlow Epic map
      Map<String, Epic> epicMap = importEpics(allIssues, project);

      // 7. Second pass: import non-Epic issues as Tasks
      int total = allIssues.size();
      int imported = 0;
      int failed = 0;
      StringBuilder errorLog = new StringBuilder();

      for (JsonNode issue : allIssues) {
        String issueType = issue.path("fields").path("issuetype").path("name").asText("");
        if ("Epic".equalsIgnoreCase(issueType)) {
          imported++; // already handled in pass 1
          continue;
        }
        try {
          importIssue(issue, project, epicMap, sprintCycleMap);
          imported++;
        } catch (Exception e) {
          failed++;
          String issueKey = issue.path("key").asText("?");
          errorLog.append("Issue ").append(issueKey).append(": ").append(e.getMessage()).append("\n");
          log.debug("Jira import: issue {} failed", issueKey, e);
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
      log.info("Jira import completed: total={} imported={} failed={}", total, imported, failed);

    } catch (Exception e) {
      log.error("Jira import job {} failed at job level", job.getId(), e);
      job.setStatus(ImportJobStatus.FAILED);
      job.setCompletedAt(LocalDateTime.now());
      job.setErrorLog("Job-level failure: " + e.getMessage());
    }

    return toDTO(importJobRepository.save(job));
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private OptionalInt fetchFirstBoardId(String accessToken, String cloudId, String projectKey) {
    try {
      String url =
          agileBase(cloudId)
              + "/board?projectKeyOrId="
              + projectKey
              + "&maxResults=10";
      JsonNode root = getJson(accessToken, url);
      JsonNode values = root.path("values");
      if (values.isArray() && !values.isEmpty()) {
        return OptionalInt.of(values.get(0).path("id").asInt());
      }
    } catch (Exception e) {
      log.warn("Jira: could not fetch boards for project {}: {}", projectKey, e.getMessage());
    }
    return OptionalInt.empty();
  }

  private Map<Integer, Cycle> importSprints(
      String accessToken, String cloudId, int boardId, Project project) {
    Map<Integer, Cycle> sprintMap = new HashMap<>();
    try {
      String url = agileBase(cloudId) + "/board/" + boardId + "/sprint?maxResults=50";
      JsonNode root = getJson(accessToken, url);
      JsonNode values = root.path("values");
      if (!values.isArray()) return sprintMap;

      for (JsonNode sprint : values) {
        int sprintId = sprint.path("id").asInt();
        String name = sprint.path("name").asText("Sprint " + sprintId);
        String state = sprint.path("state").asText("future");
        String goal = sprint.path("goal").asText(null);

        LocalDate startDate = parseDateField(sprint, "startDate");
        LocalDate endDate = parseDateField(sprint, "endDate");
        LocalDate completeDate = parseDateField(sprint, "completeDate");

        boolean completed = completeDate != null || "closed".equalsIgnoreCase(state);
        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = startDate.plusWeeks(2);

        CyclePhase phase =
            completed ? CyclePhase.BETTING_COOLDOWN : CyclePhase.SHAPING_BUILDING;

        Cycle cycle =
            Cycle.builder()
                .name(name)
                .project(project)
                .phase(phase)
                .isActive(!completed)
                .startDate(startDate)
                .endDate(endDate)
                .sprintGoal(goal != null && !goal.isBlank() ? goal : null)
                .build();
        sprintMap.put(sprintId, cycleRepository.save(cycle));
      }
    } catch (Exception e) {
      log.warn("Jira: could not fetch sprints for boardId={}: {}", boardId, e.getMessage());
    }
    log.debug("Jira import: created {} sprint cycles", sprintMap.size());
    return sprintMap;
  }

  /**
   * Paginate through all Jira issues for the project using JQL, excluding Sub-tasks. Fetches 100
   * issues per page until all are retrieved.
   */
  private List<JsonNode> fetchAllIssues(String accessToken, String cloudId, String projectKey) {
    List<JsonNode> all = new ArrayList<>();
    int startAt = 0;
    int pageSize = 100;
    int total = Integer.MAX_VALUE;

    String fields =
        "summary,description,status,priority,issuetype,parent,assignee,"
            + "customfield_10016,customfield_10020";

    while (startAt < total) {
      String jql =
          "project="
              + projectKey
              + " AND issuetype != \"Sub-task\" ORDER BY created ASC";
      String url =
          apiBase(cloudId)
              + "/search?jql="
              + encode(jql)
              + "&maxResults="
              + pageSize
              + "&startAt="
              + startAt
              + "&fields="
              + fields;
      try {
        JsonNode root = getJson(accessToken, url);
        total = root.path("total").asInt(0);
        JsonNode issues = root.path("issues");
        if (!issues.isArray() || issues.isEmpty()) break;
        for (JsonNode issue : issues) {
          all.add(issue);
        }
        startAt += issues.size();
      } catch (Exception e) {
        log.warn("Jira: issue fetch failed at startAt={}: {}", startAt, e.getMessage());
        break;
      }
    }
    return all;
  }

  /**
   * First pass: create ShipFlow Epics from Jira Epic issues. Returns map of Jira issue key →
   * ShipFlow Epic.
   */
  private Map<String, Epic> importEpics(List<JsonNode> issues, Project project) {
    Map<String, Epic> epicMap = new HashMap<>();
    for (JsonNode issue : issues) {
      String issueType = issue.path("fields").path("issuetype").path("name").asText("");
      if (!"Epic".equalsIgnoreCase(issueType)) continue;

      String key = issue.path("key").asText();
      String name = issue.path("fields").path("summary").asText();
      if (name.isBlank()) name = key;

      JsonNode descNode = issue.path("fields").path("description");
      String description = extractDescriptionText(descNode);

      Epic epic =
          Epic.builder()
              .name(name)
              .description(description)
              .project(project)
              .status(EpicStatus.IN_PROGRESS)
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build();
      epicMap.put(key, epicRepository.save(epic));
    }
    log.debug("Jira import: created {} epics", epicMap.size());
    return epicMap;
  }

  /** Second pass: create a ShipFlow Task from a Jira non-Epic issue. */
  private void importIssue(
      JsonNode issue,
      Project project,
      Map<String, Epic> epicMap,
      Map<Integer, Cycle> sprintCycleMap) {

    JsonNode fields = issue.path("fields");
    String title = fields.path("summary").asText();
    if (title.isBlank()) {
      throw new IllegalArgumentException("Issue has no summary");
    }

    JsonNode descNode = fields.path("description");
    String description = extractDescriptionText(descNode);

    TaskStatus status = mapJiraStatus(fields.path("status").path("name").asText(null));
    TaskPriority priority = mapJiraPriority(fields.path("priority").path("name").asText(null));

    // Story points from customfield_10016
    Integer storyPoints = null;
    JsonNode spNode = fields.path("customfield_10016");
    if (!spNode.isMissingNode() && !spNode.isNull() && spNode.isNumber()) {
      storyPoints = spNode.asInt();
    }

    // Sprint → Cycle from customfield_10020
    Cycle cycle = null;
    JsonNode sprintNode = fields.path("customfield_10020");
    if (!sprintNode.isMissingNode() && !sprintNode.isNull()) {
      // In Jira Cloud, customfield_10020 can be an object or an array; handle both
      JsonNode sprintObj = sprintNode.isArray() ? sprintNode.get(0) : sprintNode;
      if (sprintObj != null && !sprintObj.isNull()) {
        int sprintId = sprintObj.path("id").asInt(-1);
        if (sprintId > 0) {
          cycle = sprintCycleMap.get(sprintId);
        }
      }
    }

    // Parent epic → Epic link (parent field holds the Jira epic key for non-Epic issues)
    Epic epic = null;
    JsonNode parentNode = fields.path("parent");
    if (!parentNode.isMissingNode() && !parentNode.isNull()) {
      String parentKey = parentNode.path("key").asText(null);
      if (parentKey != null) {
        epic = epicMap.get(parentKey);
      }
    }

    // Assignee by email
    Person assignee = null;
    JsonNode assigneeNode = fields.path("assignee");
    if (!assigneeNode.isMissingNode() && !assigneeNode.isNull()) {
      String email = assigneeNode.path("emailAddress").asText(null);
      if (email != null && !email.isBlank()) {
        List<Person> found = personRepository.searchByNameOrEmail(email);
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
   * Map Jira status names to ShipFlow TaskStatus. Jira status categories: "To Do", "In Progress",
   * "Done". Individual status names vary by project.
   */
  TaskStatus mapJiraStatus(String statusName) {
    if (statusName == null) return TaskStatus.TODO;
    String lower = statusName.toLowerCase(Locale.ROOT);
    if (lower.contains("done") || lower.contains("closed") || lower.contains("resolved")) {
      return TaskStatus.DONE;
    }
    if (lower.contains("progress") || lower.contains("review") || lower.contains("testing")) {
      return TaskStatus.IN_PROGRESS;
    }
    if (lower.contains("cancel") || lower.contains("won't fix") || lower.contains("wontfix")
        || lower.contains("duplicate")) {
      return TaskStatus.CANCELLED;
    }
    if (lower.contains("blocked")) {
      return TaskStatus.BLOCKED;
    }
    if (lower.contains("backlog")) {
      return TaskStatus.BACKLOG;
    }
    return TaskStatus.TODO;
  }

  /**
   * Map Jira priority names to ShipFlow TaskPriority. Jira: Highest/Critical, High, Medium, Low,
   * Lowest.
   */
  TaskPriority mapJiraPriority(String priorityName) {
    if (priorityName == null) return TaskPriority.MEDIUM;
    return switch (priorityName.toLowerCase(Locale.ROOT)) {
      case "highest", "critical" -> TaskPriority.URGENT;
      case "high" -> TaskPriority.HIGH;
      case "low", "lowest" -> TaskPriority.LOW;
      default -> TaskPriority.MEDIUM;
    };
  }

  private ProjectType resolveProjectType(String raw) {
    if (raw == null || raw.isBlank()) return ProjectType.KANBAN;
    return switch (raw.trim().toUpperCase(Locale.ROOT)) {
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
    else if (base.isEmpty()) base = "JRA";
    String candidate;
    int attempts = 0;
    do {
      int suffix = (int) (Math.random() * 9000) + 1000;
      candidate = base + suffix;
      attempts++;
      if (attempts > 100) {
        candidate = "JRA" + System.currentTimeMillis() % 10000;
        break;
      }
    } while (projectRepository.existsByProjectKey(candidate));
    return candidate;
  }

  // ── REST client ───────────────────────────────────────────────────────────

  private JsonNode getJson(String accessToken, String url) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.set("Accept", "application/json");

    ResponseEntity<String> resp =
        restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    try {
      return objectMapper.readTree(resp.getBody());
    } catch (Exception e) {
      throw new IllegalStateException("Could not parse Jira response from " + url, e);
    }
  }

  // ── Date/text parsing ─────────────────────────────────────────────────────

  private LocalDate parseDateField(JsonNode node, String fieldName) {
    String raw = node.path(fieldName).asText(null);
    if (raw == null || raw.isBlank()) return null;
    try {
      return LocalDate.parse(raw.substring(0, 10));
    } catch (Exception e) {
      log.debug("Jira: could not parse date field '{}' value '{}': {}", fieldName, raw, e.getMessage());
      return null;
    }
  }

  /**
   * Jira Cloud description fields use Atlassian Document Format (ADF). Extract plain text from the
   * ADF "doc" node by concatenating all text leaf nodes. Falls back to raw string value if the
   * node is not ADF.
   */
  private String extractDescriptionText(JsonNode descNode) {
    if (descNode == null || descNode.isMissingNode() || descNode.isNull()) return null;
    if (descNode.isTextual()) {
      String text = descNode.asText();
      return text.isBlank() ? null : text;
    }
    // ADF structure: { "type": "doc", "content": [...] }
    StringBuilder sb = new StringBuilder();
    extractAdfText(descNode, sb);
    String result = sb.toString().trim();
    return result.isBlank() ? null : result;
  }

  private void extractAdfText(JsonNode node, StringBuilder sb) {
    if (node.has("text")) {
      sb.append(node.path("text").asText());
    }
    JsonNode content = node.path("content");
    if (content.isArray()) {
      for (JsonNode child : content) {
        extractAdfText(child, sb);
        // Add a space between block-level nodes
        String type = child.path("type").asText("");
        if ("paragraph".equals(type) || "heading".equals(type)) {
          sb.append(" ");
        }
      }
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

  // ── URL helpers ───────────────────────────────────────────────────────────

  private String apiBase(String cloudId) {
    return String.format(JIRA_API_BASE, cloudId);
  }

  private String agileBase(String cloudId) {
    return String.format(JIRA_AGILE_BASE, cloudId);
  }

  private static String encode(String s) {
    return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
  }
}
