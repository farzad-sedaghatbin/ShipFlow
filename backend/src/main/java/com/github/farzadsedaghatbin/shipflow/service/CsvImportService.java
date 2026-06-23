package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.ImportJobDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Core service for competitor migration CSV import (v1.2.0).
 *
 * <p>Supported formats: Jira, Linear, Asana, and Generic CSV.
 * Runs synchronously — suitable for small files in v1.2.0.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvImportService {

  private final ImportJobRepository importJobRepository;
  private final ProjectRepository projectRepository;
  private final TaskRepository taskRepository;
  private final EpicRepository epicRepository;
  private final CycleRepository cycleRepository;
  private final PersonRepository personRepository;
  private final UserRepository userRepository;
  private final BugReportRepository bugReportRepository;

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Entry point: creates an ImportJob, detects format, parses rows, and persists
   * tasks into a new Kanban project. Runs synchronously.
   */
  @Transactional
  public ImportJobDTO importCsv(
      MultipartFile file, String projectName, String formatHint, User currentUser) {

    // Parse and detect format first so sourceFormat (NOT NULL) is known before the first save.
    List<CSVRecord> records;
    ImportSourceFormat format;
    try {
      records = parseRecords(file);
      String[] headers = records.isEmpty() ? new String[0] : extractHeaders(records);
      format = records.isEmpty() ? ImportSourceFormat.GENERIC_CSV : resolveFormat(formatHint, headers);
    } catch (IOException e) {
      throw new RuntimeException("Failed to parse CSV file: " + e.getMessage(), e);
    }

    ImportJob job =
        ImportJob.builder()
            .fileName(file.getOriginalFilename())
            .sourceFormat(format)
            .status(ImportJobStatus.PENDING)
            .createdBy(currentUser)
            .createdAt(LocalDateTime.now())
            .build();
    job = importJobRepository.save(job);

    try {
      job.setStatus(ImportJobStatus.PARSING);
      importJobRepository.save(job);

      if (records.isEmpty()) {
        job.setStatus(ImportJobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        return toDTO(importJobRepository.save(job));
      }

      job.setTotalRows(records.size());

      // Create Kanban project
      Project project = createKanbanProject(projectName, currentUser);
      job.setProject(project);

      job.setStatus(ImportJobStatus.IMPORTING);
      importJobRepository.save(job);

      // Import rows
      int imported = 0;
      int failed = 0;
      StringBuilder errorLog = new StringBuilder();

      Map<String, Cycle> cycleCache = new HashMap<>();
      Map<String, Epic> epicCache = new HashMap<>();

      for (int i = 0; i < records.size(); i++) {
        int rowNum = i + 2; // 1-based + header row
        try {
          boolean success =
              importRow(records.get(i), format, project, currentUser, cycleCache, epicCache);
          if (success) {
            imported++;
          } else {
            failed++;
            errorLog.append("Row ").append(rowNum).append(": skipped — missing required field\n");
          }
        } catch (Exception e) {
          failed++;
          errorLog.append("Row ").append(rowNum).append(": ").append(e.getMessage()).append("\n");
          log.debug("Import row {} failed", rowNum, e);
        }
      }

      job.setImportedRows(imported);
      job.setFailedRows(failed);
      if (errorLog.length() > 0) {
        job.setErrorLog(errorLog.toString());
      }
      job.setStatus(ImportJobStatus.COMPLETED);
      job.setCompletedAt(LocalDateTime.now());

    } catch (Exception e) {
      log.error("Import job {} failed at job level", job.getId(), e);
      job.setStatus(ImportJobStatus.FAILED);
      job.setCompletedAt(LocalDateTime.now());
      job.setErrorLog("Job-level failure: " + e.getMessage());
    }

    return toDTO(importJobRepository.save(job));
  }

  /** Return a single import job by id. */
  public ImportJobDTO getJob(Long id) {
    ImportJob job =
        importJobRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Import job not found: " + id));
    return toDTO(job);
  }

  /** List all import jobs for a user, newest first. */
  public List<ImportJobDTO> listJobs(User currentUser) {
    return importJobRepository.findByCreatedByIdOrderByCreatedAtDesc(currentUser.getId()).stream()
        .map(this::toDTO)
        .toList();
  }

  // -------------------------------------------------------------------------
  // Format detection (package-private for unit tests)
  // -------------------------------------------------------------------------

  /**
   * Auto-detects the CSV source format from the header row.
   * Jira: has "Summary" AND ("Issue Type" OR "Issue key")
   * Linear: has "Title" AND "Cycle"
   * Asana: has ("Task Name" OR "Name") AND ("Section" OR "Assignee Email")
   * Otherwise: GENERIC_CSV
   */
  ImportSourceFormat detectFormat(String[] headers) {
    Set<String> h = new HashSet<>();
    for (String header : headers) {
      h.add(header.trim().toLowerCase());
    }

    boolean hasSummary = h.contains("summary");
    boolean hasIssueType = h.contains("issue type");
    boolean hasIssueKey = h.contains("issue key");
    if (hasSummary && (hasIssueType || hasIssueKey)) {
      return ImportSourceFormat.JIRA_CSV;
    }

    boolean hasTitle = h.contains("title");
    boolean hasCycle = h.contains("cycle");
    if (hasTitle && hasCycle) {
      return ImportSourceFormat.LINEAR_CSV;
    }

    boolean hasTaskName = h.contains("task name") || h.contains("name");
    boolean hasSection = h.contains("section");
    boolean hasAssigneeEmail = h.contains("assignee email");
    if (hasTaskName && (hasSection || hasAssigneeEmail)) {
      return ImportSourceFormat.ASANA_CSV;
    }

    return ImportSourceFormat.GENERIC_CSV;
  }

  // -------------------------------------------------------------------------
  // Status / priority mapping (package-private for unit tests)
  // -------------------------------------------------------------------------

  /** Map Jira status strings to TaskStatus. */
  TaskStatus mapJiraStatus(String raw) {
    if (raw == null) return TaskStatus.TODO;
    return switch (raw.trim().toLowerCase()) {
      case "done", "resolved", "closed" -> TaskStatus.DONE;
      case "in progress", "in development", "in review" -> TaskStatus.IN_PROGRESS;
      case "to do", "open", "reopened" -> TaskStatus.TODO;
      default -> TaskStatus.TODO;
    };
  }

  /** Map Jira priority strings to TaskPriority. */
  TaskPriority mapJiraPriority(String raw) {
    if (raw == null) return TaskPriority.MEDIUM;
    return switch (raw.trim().toLowerCase()) {
      case "highest", "critical", "blocker", "high" -> TaskPriority.HIGH;
      case "low", "lowest", "trivial", "minor" -> TaskPriority.LOW;
      default -> TaskPriority.MEDIUM;
    };
  }

  /** Map Linear status strings to TaskStatus. */
  TaskStatus mapLinearStatus(String raw) {
    if (raw == null) return TaskStatus.TODO;
    return switch (raw.trim().toLowerCase()) {
      case "done", "completed", "cancelled", "canceled" -> TaskStatus.DONE;
      case "in progress", "in review", "in development" -> TaskStatus.IN_PROGRESS;
      case "backlog" -> TaskStatus.BACKLOG;
      default -> TaskStatus.TODO;
    };
  }

  /** Map Linear priority strings to TaskPriority. */
  TaskPriority mapLinearPriority(String raw) {
    if (raw == null) return TaskPriority.MEDIUM;
    return switch (raw.trim().toLowerCase()) {
      case "urgent", "high" -> TaskPriority.HIGH;
      case "low", "no priority" -> TaskPriority.LOW;
      default -> TaskPriority.MEDIUM;
    };
  }

  /** Map Asana section/column to TaskStatus. */
  TaskStatus mapAsanaStatus(String raw) {
    if (raw == null) return TaskStatus.TODO;
    String lower = raw.trim().toLowerCase();
    if (lower.contains("done") || lower.contains("complete") || lower.contains("finished")) {
      return TaskStatus.DONE;
    }
    if (lower.contains("in progress") || lower.contains("doing")) {
      return TaskStatus.IN_PROGRESS;
    }
    return TaskStatus.TODO;
  }

  /** Map generic status strings to TaskStatus. */
  TaskStatus mapGenericStatus(String raw) {
    if (raw == null) return TaskStatus.TODO;
    String lower = raw.trim().toLowerCase();
    if (lower.contains("done") || lower.contains("complete")) return TaskStatus.DONE;
    if (lower.contains("in progress")) return TaskStatus.IN_PROGRESS;
    return TaskStatus.TODO;
  }

  /** Map generic priority strings to TaskPriority. */
  TaskPriority mapGenericPriority(String raw) {
    if (raw == null) return TaskPriority.MEDIUM;
    return switch (raw.trim().toLowerCase()) {
      case "high", "urgent", "critical" -> TaskPriority.HIGH;
      case "low" -> TaskPriority.LOW;
      default -> TaskPriority.MEDIUM;
    };
  }

  // -------------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------------

  private ImportSourceFormat resolveFormat(String hint, String[] headers) {
    if (hint != null) {
      return switch (hint.trim().toLowerCase()) {
        case "jira" -> ImportSourceFormat.JIRA_CSV;
        case "linear" -> ImportSourceFormat.LINEAR_CSV;
        case "asana" -> ImportSourceFormat.ASANA_CSV;
        default -> detectFormat(headers);
      };
    }
    return detectFormat(headers);
  }

  private List<CSVRecord> parseRecords(MultipartFile file) throws IOException {
    try (Reader reader =
            new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
        CSVParser parser =
            CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
      return parser.getRecords();
    }
  }

  private String[] extractHeaders(List<CSVRecord> records) {
    if (records.isEmpty()) return new String[0];
    // CSVParser with setHeader() stores header names in the first record's parser
    // We need to access them differently — use the record's toMap() keyset
    return records.get(0).toMap().keySet().toArray(new String[0]);
  }

  private Project createKanbanProject(String projectName, User owner) {
    String key = generateProjectKey(projectName);
    Project project =
        Project.builder()
            .name(projectName)
            .projectKey(key)
            .projectType(ProjectType.KANBAN)
            .owner(owner)
            .isActive(true)
            .build();
    return projectRepository.save(project);
  }

  /** Generate a unique project key: first 3 chars uppercased + 4 random digits. */
  private String generateProjectKey(String name) {
    String base =
        name.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    if (base.length() > 3) base = base.substring(0, 3);
    else if (base.isEmpty()) base = "IMP";
    String candidate;
    int attempts = 0;
    do {
      int suffix = (int) (Math.random() * 9000) + 1000;
      candidate = base + suffix;
      attempts++;
      if (attempts > 100) {
        candidate = "IMP" + System.currentTimeMillis() % 10000;
        break;
      }
    } while (projectRepository.existsByProjectKey(candidate));
    return candidate;
  }

  /**
   * Imports a single CSV record as a Task. Returns true if successful, false if
   * a required field is missing.
   */
  private boolean importRow(
      CSVRecord record,
      ImportSourceFormat format,
      Project project,
      User currentUser,
      Map<String, Cycle> cycleCache,
      Map<String, Epic> epicCache) {

    return switch (format) {
      case JIRA_CSV -> importJiraRow(record, project, currentUser, cycleCache, epicCache);
      case LINEAR_CSV -> importLinearRow(record, project, currentUser, cycleCache, epicCache);
      case ASANA_CSV -> importAsanaRow(record, project, currentUser);
      case GENERIC_CSV -> importGenericRow(record, project, currentUser);
      // LINEAR_API / JIRA_API / ZEPHYR_XLSX are handled by their own services — not reachable here
      case LINEAR_API -> importGenericRow(record, project, currentUser);
      case JIRA_API -> importGenericRow(record, project, currentUser);
      case ZEPHYR_XLSX -> importGenericRow(record, project, currentUser);
    };
  }

  // ---- Jira ----

  private boolean importJiraRow(
      CSVRecord record,
      Project project,
      User currentUser,
      Map<String, Cycle> cycleCache,
      Map<String, Epic> epicCache) {

    String summary = safeGet(record, "Summary");
    if (summary == null || summary.isBlank()) return false;

    String issueType = safeGet(record, "Issue Type");
    boolean isEpicRow = "Epic".equalsIgnoreCase(issueType);
    if (isEpicRow) {
      // Create epic record
      String epicName = summary;
      epicCache.computeIfAbsent(
          epicName,
          n ->
              epicRepository.save(
                  Epic.builder()
                      .name(n)
                      .project(project)
                      .status(EpicStatus.IN_PROGRESS)
                      .createdAt(LocalDateTime.now())
                      .updatedAt(LocalDateTime.now())
                      .build()));
      return true;
    }

    TaskStatus status = mapJiraStatus(safeGet(record, "Status"));
    TaskPriority priority = mapJiraPriority(safeGet(record, "Priority"));
    String description = safeGet(record, "Description");
    String sprintName = safeGet(record, "Sprint");
    String storyPointsRaw = safeGet(record, "Story Points");
    String labels = safeGet(record, "Labels");
    String assigneeName = safeGet(record, "Assignee");
    String epicName = safeGet(record, "Epic Name");

    Cycle cycle = null;
    if (sprintName != null && !sprintName.isBlank()) {
      cycle = getOrCreateCycle(sprintName, project, cycleCache);
    }

    // Ensure epic exists in the cache (created if needed) for future grouping use
    if (epicName != null && !epicName.isBlank()) {
      final Project proj = project;
      epicCache.computeIfAbsent(
          epicName,
          n ->
              epicRepository.save(
                  Epic.builder()
                      .name(n)
                      .project(proj)
                      .status(EpicStatus.IN_PROGRESS)
                      .createdAt(LocalDateTime.now())
                      .updatedAt(LocalDateTime.now())
                      .build()));
    }

    Integer storyPoints = parseIntSafe(storyPointsRaw);
    Person assignee = findPersonByName(assigneeName);

    if (isJiraBugType(issueType)) {
      BugReport bug = BugReport.builder()
          .bugKey(nextBugKey())
          .title(summary)
          .description(description != null ? description : "")
          .severity(mapJiraSeverity(safeGet(record, "Priority")))
          .status(mapJiraBugStatus(safeGet(record, "Status")))
          .environment(safeGet(record, "Environment"))
          .tags(labels)
          .reporter(currentUser)
          .assignee(assignee)
          .project(project)
          .cycle(cycle)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();
      bugReportRepository.save(bug);
      return true;
    }

    Task task =
        Task.builder()
            .title(summary)
            .description(description)
            .status(status)
            .priority(priority)
            .category(TaskCategory.PITCH_SCOPE)
            .project(project)
            .cycle(cycle)
            .assignee(assignee)
            .tags(labels)
            .storyPoints(storyPoints)
            .build();

    taskRepository.save(task);
    return true;
  }

  private String nextBugKey() {
    Integer max = bugReportRepository.findMaxBugKeyNumber();
    int next = (max != null ? max : 0) + 1;
    return String.format("BUG-%03d", next);
  }

  BugSeverity mapJiraSeverity(String priority) {
    if (priority == null) return BugSeverity.MAJOR;
    return switch (priority.toLowerCase()) {
      case "blocker", "critical" -> BugSeverity.CRITICAL;
      case "major" -> BugSeverity.MAJOR;
      case "minor" -> BugSeverity.MINOR;
      case "trivial" -> BugSeverity.TRIVIAL;
      default -> BugSeverity.MAJOR;
    };
  }

  BugStatus mapJiraBugStatus(String status) {
    if (status == null) return BugStatus.OPEN;
    return switch (status.toLowerCase()) {
      case "in progress", "in development" -> BugStatus.IN_PROGRESS;
      case "resolved", "done", "closed" -> BugStatus.RESOLVED;
      case "verified" -> BugStatus.VERIFIED;
      case "won't fix", "wont fix", "won't do" -> BugStatus.WONT_FIX;
      case "duplicate" -> BugStatus.DUPLICATE;
      case "reopened" -> BugStatus.REOPENED;
      default -> BugStatus.OPEN;
    };
  }

  // ---- Linear ----

  private boolean importLinearRow(
      CSVRecord record,
      Project project,
      User currentUser,
      Map<String, Cycle> cycleCache,
      Map<String, Epic> epicCache) {

    String title = safeGet(record, "Title");
    if (title == null || title.isBlank()) return false;

    TaskStatus status = mapLinearStatus(safeGet(record, "Status"));
    TaskPriority priority = mapLinearPriority(safeGet(record, "Priority"));
    String description = safeGet(record, "Description");
    String cycleName = safeGet(record, "Cycle");
    String estimateRaw = safeGet(record, "Estimate");
    String assigneeName = safeGet(record, "Assignee");
    String label = safeGet(record, "Label");
    String projectField = safeGet(record, "Project");

    Cycle cycle = null;
    if (cycleName != null && !cycleName.isBlank()) {
      cycle = getOrCreateCycle(cycleName, project, cycleCache);
    }

    // Ensure epic exists in the cache (created if needed) for future grouping use
    if (projectField != null && !projectField.isBlank()) {
      final Project proj = project;
      epicCache.computeIfAbsent(
          projectField,
          n ->
              epicRepository.save(
                  Epic.builder()
                      .name(n)
                      .project(proj)
                      .status(EpicStatus.IN_PROGRESS)
                      .createdAt(LocalDateTime.now())
                      .updatedAt(LocalDateTime.now())
                      .build()));
    }

    Integer storyPoints = parseIntSafe(estimateRaw);
    Person assignee = findPersonByName(assigneeName);

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
            .tags(label)
            .storyPoints(storyPoints)
            .build();

    taskRepository.save(task);
    return true;
  }

  // ---- Asana ----

  private boolean importAsanaRow(CSVRecord record, Project project, User currentUser) {
    // Asana uses "Task Name" or "Name"
    String title = safeGet(record, "Task Name");
    if (title == null || title.isBlank()) {
      title = safeGet(record, "Name");
    }
    if (title == null || title.isBlank()) return false;

    // Skip subtasks
    String parentTask = safeGet(record, "Parent Task");
    if (parentTask != null && !parentTask.isBlank()) return false;

    String description = safeGet(record, "Notes");
    String assigneeName = safeGet(record, "Assignee");
    String dueDateRaw = safeGet(record, "Due Date");
    String tags = safeGet(record, "Tags");
    String section = safeGet(record, "Section");
    if (section == null) section = safeGet(record, "Column");

    TaskStatus status = mapAsanaStatus(section);
    Person assignee = findPersonByName(assigneeName);
    LocalDate endDate = parseDateSafe(dueDateRaw);

    Task task =
        Task.builder()
            .title(title)
            .description(description)
            .status(status)
            .priority(TaskPriority.MEDIUM)
            .category(TaskCategory.PITCH_SCOPE)
            .project(project)
            .assignee(assignee)
            .tags(tags)
            .dueDate(endDate)
            .build();

    taskRepository.save(task);
    return true;
  }

  // ---- Generic ----

  private boolean importGenericRow(CSVRecord record, Project project, User currentUser) {
    String title = findColumnValue(record, "title", "name", "summary");
    if (title == null || title.isBlank()) return false;

    String description = findColumnValue(record, "description", "notes");
    String statusRaw = findColumnValue(record, "status");
    String priorityRaw = findColumnValue(record, "priority");
    String assigneeName = findColumnValue(record, "assignee", "owner");
    String issueType = findColumnValue(record, "issue type", "type", "issuetype");

    // Promote bug-type issues to BugReport instead of Task
    if (isJiraBugType(issueType)) {
      Person assignee = findPersonByName(assigneeName);
      BugReport bug = BugReport.builder()
          .bugKey(nextBugKey())
          .title(title)
          .description(description != null ? description : "")
          .severity(mapJiraSeverity(priorityRaw))
          .status(mapJiraBugStatus(statusRaw))
          .reporter(currentUser)
          .assignee(assignee)
          .project(project)
          .createdAt(LocalDateTime.now())
          .updatedAt(LocalDateTime.now())
          .build();
      bugReportRepository.save(bug);
      return true;
    }

    TaskStatus status = mapGenericStatus(statusRaw);
    TaskPriority priority = mapGenericPriority(priorityRaw);
    Person assignee = findPersonByName(assigneeName);

    Task task =
        Task.builder()
            .title(title)
            .description(description)
            .status(status)
            .priority(priority)
            .category(TaskCategory.PITCH_SCOPE)
            .project(project)
            .assignee(assignee)
            .build();

    taskRepository.save(task);
    return true;
  }

  /** Returns true for Jira issue types that should be imported as BugReports. */
  private boolean isJiraBugType(String issueType) {
    if (issueType == null || issueType.isBlank()) return false;
    String lower = issueType.trim().toLowerCase();
    return lower.equals("bug")
        || lower.equals("defect")
        || lower.equals("error")
        || lower.equals("problem")
        || lower.equals("bug report")
        || lower.equals("fault")
        || lower.contains("bug");
  }

  // ---- Cycle de-duplication ----

  private Cycle getOrCreateCycle(String name, Project project, Map<String, Cycle> cache) {
    return cache.computeIfAbsent(
        name,
        n ->
            cycleRepository.save(
                Cycle.builder()
                    .name(n)
                    .project(project)
                    .phase(CyclePhase.SHAPING_BUILDING)
                    .isActive(true)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusWeeks(6))
                    .build()));
  }

  // ---- Person lookup ----

  private Person findPersonByName(String name) {
    if (name == null || name.isBlank()) return null;
    List<Person> matches = personRepository.searchByNameOrEmail(name.trim());
    return matches.isEmpty() ? null : matches.get(0);
  }

  // ---- CSV field helpers ----

  /** Safely read a CSV column value; returns null if column missing or blank. */
  private String safeGet(CSVRecord record, String column) {
    try {
      String val = record.get(column);
      return (val == null || val.isBlank()) ? null : val.trim();
    } catch (IllegalArgumentException e) {
      return null; // column not present in this CSV
    }
  }

  /**
   * Find the first non-blank value matching any of the given column name candidates
   * (case-insensitive prefix match).
   */
  private String findColumnValue(CSVRecord record, String... candidates) {
    Map<String, String> map = record.toMap();
    for (String candidate : candidates) {
      for (Map.Entry<String, String> entry : map.entrySet()) {
        if (entry.getKey().trim().equalsIgnoreCase(candidate)) {
          String val = entry.getValue();
          if (val != null && !val.isBlank()) return val.trim();
        }
      }
    }
    return null;
  }

  // ---- Parsing helpers ----

  private Integer parseIntSafe(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try {
      // Handle decimal like "2.0" from some exports
      return (int) Double.parseDouble(raw.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private LocalDate parseDateSafe(String raw) {
    if (raw == null || raw.isBlank()) return null;
    List<DateTimeFormatter> formatters =
        List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    for (DateTimeFormatter fmt : formatters) {
      try {
        return LocalDate.parse(raw.trim(), fmt);
      } catch (DateTimeParseException e) {
        // try next
      }
    }
    return null;
  }

  // ---- DTO conversion ----

  ImportJobDTO toDTO(ImportJob job) {
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
