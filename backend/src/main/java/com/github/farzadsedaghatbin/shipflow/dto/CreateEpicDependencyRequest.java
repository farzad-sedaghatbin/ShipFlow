package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for creating an epic dependency. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEpicDependencyRequest {

  /** ID of the target epic (the epic being depended upon or blocked). */
  @NotNull(message = "Target epic ID is required")
  private Long targetEpicId;

  /** Type of dependency relationship. Defaults to BLOCKS if not specified. */
  @Builder.Default
  private DependencyType dependencyType = DependencyType.BLOCKS;
}
