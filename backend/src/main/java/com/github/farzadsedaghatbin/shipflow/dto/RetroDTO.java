package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetroDTO {
  private Long id;
  private String title;
  private String notes;
  private RetroStatus status;
  private Long cycleId;
  private String cycleName;
  private Long projectId;
  private String projectName;
  private String projectKey;
  private Long createdById;
  private String createdByName;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private LocalDateTime closedAt;
  private List<RetroItemDTO> items;
  private Integer itemCount;
}
