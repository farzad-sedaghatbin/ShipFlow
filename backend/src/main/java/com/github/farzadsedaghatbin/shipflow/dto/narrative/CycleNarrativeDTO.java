package com.github.farzadsedaghatbin.shipflow.dto.narrative;

import com.github.farzadsedaghatbin.shipflow.entity.enums.NarrativeType;
import java.time.LocalDateTime;
import lombok.*;

/**
 * DTO for a single cycle narrative section.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleNarrativeDTO {

  private Long id;
  private Long cycleId;
  private String cycleName;
  private NarrativeType narrativeType;
  private String content;
  private Boolean isAiGenerated;
  private String aiModel;
  private LocalDateTime generatedAt;
  private String generatedByUsername;
}
