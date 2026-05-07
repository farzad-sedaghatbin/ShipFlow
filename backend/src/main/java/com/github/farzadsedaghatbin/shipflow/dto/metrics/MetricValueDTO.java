package com.github.farzadsedaghatbin.shipflow.dto.metrics;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricValueDTO {
  private Long metricId;
  private String metricName;
  private BigDecimal value;
  private String displayValue; // Formatted based on display format
  private LocalDateTime calculatedAt;
  private String metadata; // JSON with calculation context
}
