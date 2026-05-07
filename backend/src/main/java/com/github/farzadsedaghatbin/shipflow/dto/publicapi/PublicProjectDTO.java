package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicProjectDTO {
  private Long id;
  private String name;
  private String projectKey;
  private String description;
  private String color;
  private String projectType;
  private Boolean isActive;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
