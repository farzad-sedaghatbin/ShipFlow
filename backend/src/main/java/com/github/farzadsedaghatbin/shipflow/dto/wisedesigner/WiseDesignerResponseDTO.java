package com.github.farzadsedaghatbin.shipflow.dto.wisedesigner;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response from a generate or iterate call. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WiseDesignerResponseDTO {

  private Long artifactId;
  private Long pitchId;
  private String pitchName;

  /** Self-contained HTML document ready to be rendered in a sandboxed iframe. */
  private String htmlContent;

  /** One-line summary of what was generated or changed. */
  private String description;

  /** Session ID for subsequent iterate calls. */
  private String sessionId;

  /** Whether a Wise Architecture session was used as context. */
  private Boolean usedArchContext;

  private LocalDateTime generatedAt;
}
