package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BettingSlotDTO {
  private Long id;
  private Long cycleId;
  private String cycleName;
  private Long teamId;
  private String teamName;
  private Long pitchId;
  private String pitchTitle;
  private Integer pitchAppetiteDays;
  private String pitchStatus;
  private Integer position;
  private LocalDate startDate;
  private LocalDate endDate;
  private Integer durationWeeks;
  private String notes;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Boolean canFitPitch; // Calculated field for drag validation
}
