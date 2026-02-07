package com.github.farzadsedaghatbin.shipflow.dto.narrative;

import com.github.farzadsedaghatbin.shipflow.entity.enums.NarrativeType;
import lombok.*;

/**
 * Request DTO for generating/regenerating a narrative.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateNarrativeRequest {

  private Long cycleId;
  private NarrativeType narrativeType;
  
  /** Force regeneration even if cached */
  @Builder.Default
  private Boolean forceRegenerate = false;
  
  /** Use AI if available, otherwise templates */
  @Builder.Default
  private Boolean preferAi = true;
}
