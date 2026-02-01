package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity storing historical calculated values for custom metrics. Enables trend analysis and
 * historical comparisons.
 */
@Entity
@Table(name = "metric_data_points")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricDataPoint {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "metric_id", nullable = false)
  private CustomMetric metric;

  /** The calculated value at this point in time */
  @Column(name = "calculated_value", precision = 20, scale = 4)
  private BigDecimal calculatedValue;

  /** When this value was calculated */
  @Column(name = "calculation_date", nullable = false)
  private LocalDateTime calculationDate;

  /**
   * Additional context about the calculation Example: {"cycleId": 1, "pitchCount": 10,
   * "workLogCount": 150}
   */
  @Column(name = "metadata", columnDefinition = "TEXT")
  private String metadata;

  @PrePersist
  protected void onCreate() {
    if (calculationDate == null) {
      calculationDate = LocalDateTime.now();
    }
  }
}
