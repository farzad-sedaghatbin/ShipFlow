package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports project data as JSON or CSV for backup / migration purposes.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataExportService {

  private final ProjectRepository projectRepository;
  private final CycleRepository cycleRepository;
  private final PitchRepository pitchRepository;
  private final TaskRepository taskRepository;
  private final BugReportRepository bugReportRepository;
  private final ReleaseRepository releaseRepository;
  private final EpicRepository epicRepository;

  private final ObjectMapper objectMapper;

  private static final String EXPORT_VERSION = "1.0";

  /**
   * Exports all data for a project as a JSON byte array.
   */
  public byte[] exportProjectAsJson(Long projectId) {
    DataExportDTO export = buildExportDTO(projectId);
    try {
      ObjectMapper mapper = objectMapper.copy()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .enable(SerializationFeature.INDENT_OUTPUT);
      return mapper.writeValueAsBytes(export);
    } catch (Exception e) {
      throw new RuntimeException("Failed to export project as JSON", e);
    }
  }

  /**
   * Exports tasks for a project as CSV.
   */
  public byte[] exportTasksAsCsv(Long projectId) {
    projectRepository.findById(projectId)
        .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

    List<Task> tasks = taskRepository.findAllNotDeleted().stream()
        .filter(t -> t.getCycle() != null
            && t.getCycle().getProject() != null
            && t.getCycle().getProject().getId().equals(projectId))
        .toList();

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

      // CSV header
      writer.println("id,title,status,priority,category,estimateHours,actualHours,cycleId,pitchId,assigneeName,dueDate,completedAt,createdAt");

      for (Task t : tasks) {
        writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
            t.getId(),
            csvEscape(t.getTitle()),
            t.getStatus() != null ? t.getStatus().name() : "",
            t.getPriority() != null ? t.getPriority().name() : "",
            t.getCategory() != null ? t.getCategory().name() : "",
            t.getEstimateHours() != null ? t.getEstimateHours() : "",
            t.getActualHours() != null ? t.getActualHours() : "",
            t.getCycle() != null ? t.getCycle().getId() : "",
            t.getPitch() != null ? t.getPitch().getId() : "",
            t.getAssignee() != null ? csvEscape(t.getAssignee().getName()) : "",
            t.getDueDate() != null ? t.getDueDate().toString() : "",
            t.getCompletedAt() != null ? t.getCompletedAt().toString() : "",
            t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
      }
      writer.flush();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException("Failed to export tasks as CSV", e);
    }
  }

  /**
   * Exports bugs for a project as CSV.
   */
  public byte[] exportBugsAsCsv(Long projectId) {
    List<BugReport> bugs = bugReportRepository.findAll().stream()
        .filter(b -> b.getProject() != null && b.getProject().getId().equals(projectId))
        .toList();

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

      writer.println("id,bugKey,title,severity,status,assigneeName,resolution,resolvedAt,createdAt");

      for (BugReport b : bugs) {
        writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
            b.getId(),
            csvEscape(b.getBugKey()),
            csvEscape(b.getTitle()),
            b.getSeverity() != null ? b.getSeverity().name() : "",
            b.getStatus() != null ? b.getStatus().name() : "",
            b.getAssignee() != null ? csvEscape(b.getAssignee().getName()) : "",
            b.getResolution() != null ? csvEscape(b.getResolution()) : "",
            b.getResolvedAt() != null ? b.getResolvedAt().toString() : "",
            b.getCreatedAt() != null ? b.getCreatedAt().toString() : "");
      }
      writer.flush();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException("Failed to export bugs as CSV", e);
    }
  }

  // ──────────────────────────── internals ────────────────────────────

  private DataExportDTO buildExportDTO(Long projectId) {
    Project project = projectRepository.findById(projectId)
        .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

    List<Cycle> cycles = cycleRepository.findByProjectIdOrderByStartDateDesc(projectId);
    List<Pitch> pitches = pitchRepository.findAllNotDeleted().stream()
        .filter(p -> p.getCycle() != null
            && p.getCycle().getProject() != null
            && p.getCycle().getProject().getId().equals(projectId))
        .toList();
    List<Task> tasks = taskRepository.findAllNotDeleted().stream()
        .filter(t -> t.getCycle() != null
            && t.getCycle().getProject() != null
            && t.getCycle().getProject().getId().equals(projectId))
        .toList();
    List<BugReport> bugs = bugReportRepository.findAll().stream()
        .filter(b -> b.getProject() != null && b.getProject().getId().equals(projectId))
        .toList();
    List<Release> releases = releaseRepository.findByProjectIdNotDeleted(projectId);
    List<Epic> epics = epicRepository.findByProjectIdNotDeleted(projectId);

    return DataExportDTO.builder()
        .version(EXPORT_VERSION)
        .exportedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .projectKey(project.getProjectKey())
        .project(toProjectDTO(project))
        .cycles(cycles.stream().map(this::toCycleDTO).toList())
        .pitches(pitches.stream().map(this::toPitchDTO).toList())
        .tasks(tasks.stream().map(this::toTaskDTO).toList())
        .bugs(bugs.stream().map(this::toBugDTO).toList())
        .releases(releases.stream().map(this::toReleaseDTO).toList())
        .epics(epics.stream().map(this::toEpicDTO).toList())
        .build();
  }

  // ──────────── Mapping helpers ────────────

  private PublicProjectDTO toProjectDTO(Project p) {
    return PublicProjectDTO.builder()
        .id(p.getId()).name(p.getName()).projectKey(p.getProjectKey())
        .description(p.getDescription()).color(p.getColor())
        .projectType(p.getProjectType() != null ? p.getProjectType().name() : null)
        .isActive(p.getIsActive()).createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
        .build();
  }

  private PublicCycleDTO toCycleDTO(Cycle c) {
    return PublicCycleDTO.builder()
        .id(c.getId()).projectId(c.getProject() != null ? c.getProject().getId() : null)
        .name(c.getName()).startDate(c.getStartDate()).endDate(c.getEndDate())
        .phase(c.getPhase() != null ? c.getPhase().name() : null).isActive(c.getIsActive())
        .build();
  }

  private PublicPitchDTO toPitchDTO(Pitch p) {
    return PublicPitchDTO.builder()
        .id(p.getId()).title(p.getTitle()).description(p.getDescription())
        .appetiteDays(p.getAppetiteDays()).problemStatement(p.getProblemStatement())
        .solution(p.getSolution())
        .status(p.getStatus() != null ? p.getStatus().name() : null)
        .cycleId(p.getCycle() != null ? p.getCycle().getId() : null)
        .epicId(p.getEpic() != null ? p.getEpic().getId() : null)
        .targetReleaseId(p.getTargetRelease() != null ? p.getTargetRelease().getId() : null)
        .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt())
        .build();
  }

  private PublicTaskDTO toTaskDTO(Task t) {
    return PublicTaskDTO.builder()
        .id(t.getId()).title(t.getTitle()).description(t.getDescription())
        .status(t.getStatus() != null ? t.getStatus().name() : null)
        .priority(t.getPriority() != null ? t.getPriority().name() : null)
        .category(t.getCategory() != null ? t.getCategory().name() : null)
        .estimateHours(t.getEstimateHours()).actualHours(t.getActualHours())
        .cycleId(t.getCycle() != null ? t.getCycle().getId() : null)
        .pitchId(t.getPitch() != null ? t.getPitch().getId() : null)
        .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
        .assigneeName(t.getAssignee() != null ? t.getAssignee().getName() : null)
        .parentTaskId(t.getParentTask() != null ? t.getParentTask().getId() : null)
        .targetReleaseId(t.getTargetRelease() != null ? t.getTargetRelease().getId() : null)
        .dueDate(t.getDueDate()).completedAt(t.getCompletedAt())
        .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
        .build();
  }

  private PublicBugDTO toBugDTO(BugReport b) {
    return PublicBugDTO.builder()
        .id(b.getId()).bugKey(b.getBugKey()).title(b.getTitle()).description(b.getDescription())
        .severity(b.getSeverity() != null ? b.getSeverity().name() : null)
        .status(b.getStatus() != null ? b.getStatus().name() : null)
        .projectId(b.getProject() != null ? b.getProject().getId() : null)
        .pitchId(b.getPitch() != null ? b.getPitch().getId() : null)
        .cycleId(b.getCycle() != null ? b.getCycle().getId() : null)
        .assigneeId(b.getAssignee() != null ? b.getAssignee().getId() : null)
        .assigneeName(b.getAssignee() != null ? b.getAssignee().getName() : null)
        .targetReleaseId(b.getTargetRelease() != null ? b.getTargetRelease().getId() : null)
        .fixedInReleaseId(b.getFixedInRelease() != null ? b.getFixedInRelease().getId() : null)
        .resolution(b.getResolution()).resolvedAt(b.getResolvedAt())
        .createdAt(b.getCreatedAt()).updatedAt(b.getUpdatedAt())
        .build();
  }

  private PublicReleaseDTO toReleaseDTO(Release r) {
    return PublicReleaseDTO.builder()
        .id(r.getId()).name(r.getName()).version(r.getVersion()).description(r.getDescription())
        .status(r.getStatus() != null ? r.getStatus().name() : null)
        .riskLevel(r.getRiskLevel() != null ? r.getRiskLevel().name() : null)
        .targetDate(r.getTargetDate()).releaseDate(r.getReleaseDate())
        .releaseNotes(r.getReleaseNotes())
        .projectId(r.getProject() != null ? r.getProject().getId() : null)
        .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
        .build();
  }

  private PublicEpicDTO toEpicDTO(Epic e) {
    return PublicEpicDTO.builder()
        .id(e.getId()).name(e.getName()).description(e.getDescription())
        .status(e.getStatus() != null ? e.getStatus().name() : null)
        .color(e.getColor()).targetStartDate(e.getTargetStartDate()).targetEndDate(e.getTargetEndDate())
        .projectId(e.getProject() != null ? e.getProject().getId() : null)
        .initiativeId(e.getInitiative() != null ? e.getInitiative().getId() : null)
        .ownerId(e.getOwner() != null ? e.getOwner().getId() : null)
        .sortOrder(e.getSortOrder()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
        .build();
  }

  private String csvEscape(String val) {
    if (val == null) return "";
    if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
      return "\"" + val.replace("\"", "\"\"") + "\"";
    }
    return val;
  }
}
