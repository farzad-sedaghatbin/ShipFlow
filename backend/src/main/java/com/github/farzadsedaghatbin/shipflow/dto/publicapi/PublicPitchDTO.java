package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicPitchDTO {
  private Long id;
  private String title;
  private String description;
  private Integer appetiteDays;
  private String problemStatement;
  private String solution;
  private String status;
  private Long cycleId;
  private Long epicId;
  private Long targetReleaseId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
