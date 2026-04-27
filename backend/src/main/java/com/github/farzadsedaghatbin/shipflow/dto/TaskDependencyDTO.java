package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for task dependency information. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskDependencyDTO {
  private Long id;

  /** ID of the task that has the dependency (source). */
  private Long sourceTaskId;

  /** Title of the source task. */
  private String sourceTaskTitle;

  /** ID of the task being depended upon (target). */
  private Long targetTaskId;

  /** Title of the target task. */
  private String targetTaskTitle;

  /** Type of dependency relationship. */
  private DependencyType dependencyType;

  /** When this dependency was created. */
  private LocalDateTime createdAt;
}
