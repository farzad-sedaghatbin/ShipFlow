package com.github.farzadsedaghatbin.shipflow.dto.publicapi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicTaskDTO {
  private Long id;
  private String title;
  private String description;
  private String status;
  private String priority;
  private String category;
  private BigDecimal estimateHours;
  private BigDecimal actualHours;
  private Long cycleId;
  private Long pitchId;
  private Long assigneeId;
  private String assigneeName;
  private Long parentTaskId;
  private Long targetReleaseId;
  private LocalDate dueDate;
  private LocalDateTime completedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
