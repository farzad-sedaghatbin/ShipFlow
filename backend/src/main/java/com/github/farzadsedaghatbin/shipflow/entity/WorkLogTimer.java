package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "work_log_timers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkLogTimer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id", nullable = false)
  private Person person;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  private Pitch pitch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  private Task task;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(columnDefinition = "TEXT")
  private String note;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "status", nullable = false)
  @Builder.Default
  private String status = "RUNNING";

  @Column(name = "paused_at")
  private LocalDateTime pausedAt;

  @Column(name = "total_paused_seconds", nullable = false)
  @Builder.Default
  private Long totalPausedSeconds = 0L;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (startTime == null) {
      startTime = LocalDateTime.now();
    }
    if (status == null) {
      status = "RUNNING";
    }
    if (totalPausedSeconds == null) {
      totalPausedSeconds = 0L;
    }
  }
}
