package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for creating a pitch dependency. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePitchDependencyRequest {

  /** ID of the target pitch (the pitch being depended upon or blocked). */
  @NotNull(message = "Target pitch ID is required")
  private Long targetPitchId;

  /** Type of dependency relationship. Defaults to BLOCKS if not specified. */
  @Builder.Default
  private DependencyType dependencyType = DependencyType.BLOCKS;
}
