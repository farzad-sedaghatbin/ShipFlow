package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "hill_chart_points")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HillChartPoint {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id", nullable = false)
  private Pitch pitch;

  @Column(nullable = false)
  private String scope;

  @Column(nullable = false)
  private String description;

  // Position on hill: 0-50 is uphill (figuring things out), 50-100 is downhill (executing)
  @Column(nullable = false)
  private Integer position;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
