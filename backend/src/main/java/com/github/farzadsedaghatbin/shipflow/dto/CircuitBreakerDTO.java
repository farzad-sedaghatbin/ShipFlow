package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Circuit Breaker information - Shape Up safety valve Tracks pitches that have overflowed
 * their time budget
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CircuitBreakerDTO {
  private Long pitchId;
  private String pitchTitle;
  private Double appetiteDays;
  private Double daysSpent;
  private Double hoursSpent;
  private Double utilizationPercentage; // (daysSpent / appetiteDays) * 100
  private Double overflowPercentage; // utilizationPercentage - 100
  private Boolean isOverflowing;
  private Boolean isCircuitBreakerTriggered;
  private String circuitBreakerReason;
  private LocalDateTime circuitBreakerDate;
  private String status;
}
