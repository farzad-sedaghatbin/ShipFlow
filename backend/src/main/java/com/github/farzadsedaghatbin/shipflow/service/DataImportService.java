package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.publicapi.DataExportDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports project data from JSON or CSV, creating entities in the local database.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class DataImportService {

  private final ProjectRepository projectRepository;
  private final CycleRepository cycleRepository;
  private final TaskRepository taskRepository;
  private final ObjectMapper objectMapper;

  /**
   * Import a full project snapshot from JSON (produced by {@link DataExportService}).
   * Creates the project if it doesn't already exist (matched by projectKey).
   *
   * @return summary of imported counts
   */
  public ImportSummary importProjectFromJson(InputStream inputStream) {
    try {
      DataExportDTO export = objectMapper.readValue(inputStream, DataExportDTO.class);
      ImportSummary summary = new ImportSummary();

      // 1. Project
      if (export.getProject() != null) {
        String key = export.getProject().getProjectKey();
        if (projectRepository.findByProjectKey(key).isEmpty()) {
          Project project = Project.builder()
              .name(export.getProject().getName())
              .projectKey(key)
              .description(export.getProject().getDescription())
              .color(export.getProject().getColor())
              .isActive(export.getProject().getIsActive() != null ? export.getProject().getIsActive() : true)
              .build();
          if (export.getProject().getProjectType() != null) {
            try {
              project.setProjectType(ProjectType.valueOf(export.getProject().getProjectType()));
            } catch (IllegalArgumentException ignored) {
              // keep default
            }
          }
          projectRepository.save(project);
          summary.projectsCreated++;
          log.info("Imported project: {}", key);
        } else {
          log.info("Project with key '{}' already exists – skipping", key);
          summary.projectsSkipped++;
        }
      }

      // 2. Cycles
      if (export.getCycles() != null) {
        Project project = projectRepository.findByProjectKey(export.getProjectKey()).orElse(null);
        if (project != null) {
          for (var cycleDTO : export.getCycles()) {
            Cycle cycle = Cycle.builder()
                .project(project)
                .name(cycleDTO.getName())
                .startDate(cycleDTO.getStartDate())
                .endDate(cycleDTO.getEndDate())
                .isActive(cycleDTO.getIsActive() != null ? cycleDTO.getIsActive() : false)
                .build();
            if (cycleDTO.getPhase() != null) {
              try {
                cycle.setPhase(CyclePhase.valueOf(cycleDTO.getPhase()));
              } catch (IllegalArgumentException ignored) {
                // keep default
              }
            }
            cycleRepository.save(cycle);
            summary.cyclesCreated++;
          }
        }
      }

      // 3. Tasks (simplified – links to first matching cycle by name)
      if (export.getTasks() != null) {
        for (var taskDTO : export.getTasks()) {
          Task task = Task.builder()
              .title(taskDTO.getTitle())
              .description(taskDTO.getDescription())
              .build();
          if (taskDTO.getStatus() != null) {
            try {
              task.setStatus(TaskStatus.valueOf(taskDTO.getStatus()));
            } catch (IllegalArgumentException ignored) {
              task.setStatus(TaskStatus.BACKLOG);
            }
          }
          if (taskDTO.getPriority() != null) {
            try {
              task.setPriority(TaskPriority.valueOf(taskDTO.getPriority()));
            } catch (IllegalArgumentException ignored) {
              // skip
            }
          }
          task.setEstimateHours(taskDTO.getEstimateHours());
          task.setActualHours(taskDTO.getActualHours());
          task.setDueDate(taskDTO.getDueDate());
          task.setCompletedAt(taskDTO.getCompletedAt());
          taskRepository.save(task);
          summary.tasksCreated++;
        }
      }

      log.info("Import complete: {}", summary);
      return summary;

    } catch (Exception e) {
      throw new RuntimeException("Failed to import project from JSON", e);
    }
  }

  /**
   * Import tasks from CSV.
   * Expected header: id,title,status,priority,category,estimateHours,actualHours,cycleId,pitchId,assigneeName,dueDate,completedAt,createdAt
   */
  public ImportSummary importTasksFromCsv(InputStream inputStream) {
    ImportSummary summary = new ImportSummary();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      String header = reader.readLine(); // skip header
      if (header == null) {
        return summary;
      }

      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) continue;
        String[] cols = parseCsvLine(line);
        if (cols.length < 3) continue;

        Task task = Task.builder()
            .title(cols.length > 1 ? cols[1] : "Untitled")
            .build();

        if (cols.length > 2 && !cols[2].isBlank()) {
          try {
            task.setStatus(TaskStatus.valueOf(cols[2]));
          } catch (IllegalArgumentException ignored) {
            task.setStatus(TaskStatus.BACKLOG);
          }
        }
        if (cols.length > 3 && !cols[3].isBlank()) {
          try {
            task.setPriority(TaskPriority.valueOf(cols[3]));
          } catch (IllegalArgumentException ignored) {
            // skip
          }
        }
        if (cols.length > 5 && !cols[5].isBlank()) {
          try {
            task.setEstimateHours(new java.math.BigDecimal(cols[5]));
          } catch (NumberFormatException ignored) {
            // skip
          }
        }
        if (cols.length > 6 && !cols[6].isBlank()) {
          try {
            task.setActualHours(new java.math.BigDecimal(cols[6]));
          } catch (NumberFormatException ignored) {
            // skip
          }
        }

        taskRepository.save(task);
        summary.tasksCreated++;
      }

    } catch (Exception e) {
      throw new RuntimeException("Failed to import tasks from CSV", e);
    }
    log.info("CSV task import complete: {}", summary);
    return summary;
  }

  // ──────────── CSV parsing ────────────

  private String[] parseCsvLine(String line) {
    List<String> columns = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          current.append(c);
        }
      } else {
        if (c == '"') {
          inQuotes = true;
        } else if (c == ',') {
          columns.add(current.toString());
          current = new StringBuilder();
        } else {
          current.append(c);
        }
      }
    }
    columns.add(current.toString());
    return columns.toArray(new String[0]);
  }

  // ──────────── Summary ────────────

  public static class ImportSummary {
    public int projectsCreated;
    public int projectsSkipped;
    public int cyclesCreated;
    public int tasksCreated;
    public int bugsCreated;

    @Override
    public String toString() {
      return String.format("projects=%d (skipped=%d), cycles=%d, tasks=%d, bugs=%d",
          projectsCreated, projectsSkipped, cyclesCreated, tasksCreated, bugsCreated);
    }
  }
}
