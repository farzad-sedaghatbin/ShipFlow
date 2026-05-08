package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for epic dependency information. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpicDependencyDTO {
  private Long id;

  /** ID of the epic that has the dependency (source). */
  private Long sourceEpicId;

  /** Name of the source epic. */
  private String sourceEpicName;

  /** ID of the epic being depended upon (target). */
  private Long targetEpicId;

  /** Name of the target epic. */
  private String targetEpicName;

  /** Type of dependency relationship. */
  private DependencyType dependencyType;

  /** When this dependency was created. */
  private LocalDateTime createdAt;
}
