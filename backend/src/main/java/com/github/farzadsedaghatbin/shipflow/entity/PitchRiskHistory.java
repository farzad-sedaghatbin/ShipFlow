package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Historical record of pitch risk scores for trend analysis.
 * Snapshots are created:
 * - Daily via scheduled job
 * - On significant events (status change, work log added)
 * - On manual risk analysis refresh
 */
@Entity
@Table(name = "pitch_risk_history", indexes = {
    @Index(name = "idx_pitch_risk_history_pitch", columnList = "pitch_id"),
    @Index(name = "idx_pitch_risk_history_date", columnList = "recorded_at"),
    @Index(name = "idx_pitch_risk_history_pitch_date", columnList = "pitch_id, recorded_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchRiskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id", nullable = false)
    private Pitch pitch;

    @Column(nullable = false)
    private Integer riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;

    /**
     * Serialized JSON of risk factors for this snapshot.
     * Stores RiskFactor[] from RiskAnalysisService.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String riskFactorsJson;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    /**
     * What triggered this risk snapshot.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TriggerType triggerType;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    /**
     * Trigger type for risk history snapshots.
     */
    public enum TriggerType {
        MANUAL,          // User explicitly requested risk analysis
        SCHEDULED,       // Daily automated snapshot
        STATUS_CHANGE,   // Pitch status changed
        WORK_LOG_ADDED,  // New work log entry added
        CIRCUIT_BREAKER  // Circuit breaker triggered
    }
}
