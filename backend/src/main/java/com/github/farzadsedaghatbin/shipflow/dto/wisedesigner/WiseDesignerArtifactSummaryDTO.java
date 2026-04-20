package com.github.farzadsedaghatbin.shipflow.dto.wisedesigner;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lightweight summary used in the artifact list (no htmlContent to keep response small). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WiseDesignerArtifactSummaryDTO {

  private Long artifactId;
  private Long pitchId;
  private String description;
  private String sessionId;
  private Boolean usedArchContext;
  private LocalDateTime createdAt;
}
