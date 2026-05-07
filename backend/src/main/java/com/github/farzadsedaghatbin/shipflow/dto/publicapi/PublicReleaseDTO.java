package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicReleaseDTO {
  private Long id;
  private String name;
  private String version;
  private String description;
  private String status;
  private String riskLevel;
  private LocalDate targetDate;
  private LocalDate releaseDate;
  private String releaseNotes;
  private Long projectId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
