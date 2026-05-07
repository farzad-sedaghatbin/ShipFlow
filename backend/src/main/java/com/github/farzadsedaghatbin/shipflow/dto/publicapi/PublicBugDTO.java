package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicBugDTO {
  private Long id;
  private String bugKey;
  private String title;
  private String description;
  private String severity;
  private String status;
  private Long projectId;
  private Long pitchId;
  private Long cycleId;
  private Long assigneeId;
  private String assigneeName;
  private Long targetReleaseId;
  private Long fixedInReleaseId;
  private String resolution;
  private LocalDateTime resolvedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
