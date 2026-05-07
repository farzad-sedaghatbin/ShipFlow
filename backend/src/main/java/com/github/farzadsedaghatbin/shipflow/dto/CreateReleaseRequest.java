package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseRiskLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

/**
 * Request DTO for creating or updating a Release.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReleaseRequest {

  @NotBlank(message = "Release name is required")
  @Size(max = 255, message = "Name must be less than 255 characters")
  private String name;

  @Size(max = 50, message = "Version must be less than 50 characters")
  private String version;

  private String description;

  @Builder.Default
  private ReleaseStatus status = ReleaseStatus.PLANNING;

  @Builder.Default
  private ReleaseRiskLevel riskLevel = ReleaseRiskLevel.LOW;

  private LocalDate targetDate;

  private LocalDate releaseDate;

  private String releaseNotes;

  @NotNull(message = "Project ID is required")
  private Long projectId;

  /** Cycle IDs to link to this release */
  private List<Long> cycleIds;

  private Integer sortOrder;
}
