package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicEpicDTO {
  private Long id;
  private String name;
  private String description;
  private String status;
  private String color;
  private LocalDate targetStartDate;
  private LocalDate targetEndDate;
  private Long projectId;
  private Long initiativeId;
  private Long ownerId;
  private Integer sortOrder;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
