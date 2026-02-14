package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.SignalType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Cache for computed cycle signals to avoid expensive recalculation.
 * Signals are insights derived from cross-cycle analysis.
 */
@Entity
@Table(name = "cycle_signal_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleSignalCache {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Enumerated(EnumType.STRING)
  @Column(name = "signal_type", nullable = false, length = 50)
  private SignalType signalType;

  /** JSON-serialized signal data */
  @Column(name = "signal_data", nullable = false, columnDefinition = "TEXT")
  private String signalData;

  /** Comma-separated cycle IDs included in this calculation */
  @Column(name = "cycles_included")
  private String cyclesIncluded;

  @Column(name = "calculated_at", nullable = false)
  private LocalDateTime calculatedAt;

  @Column(name = "valid_until")
  private LocalDateTime validUntil;

  @PrePersist
  protected void onCreate() {
    if (calculatedAt == null) {
      calculatedAt = LocalDateTime.now();
    }
  }

  public boolean isExpired() {
    return validUntil != null && LocalDateTime.now().isAfter(validUntil);
  }
}
