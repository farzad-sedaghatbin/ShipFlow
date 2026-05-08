package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.DependencyType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for pitch dependency information. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchDependencyDTO {
  private Long id;

  /** ID of the pitch that has the dependency (source). */
  private Long sourcePitchId;

  /** Title of the source pitch. */
  private String sourcePitchTitle;

  /** ID of the pitch being depended upon (target). */
  private Long targetPitchId;

  /** Title of the target pitch. */
  private String targetPitchTitle;

  /** Type of dependency relationship. */
  private DependencyType dependencyType;

  /** When this dependency was created. */
  private LocalDateTime createdAt;
}
