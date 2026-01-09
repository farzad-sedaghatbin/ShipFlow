package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a slot in the Betting Table where a shaped pitch is assigned
 * to a specific team for implementation during a cycle.
 */
@Entity
@Table(name = "betting_slots", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"cycle_id", "team_id", "position"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BettingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id", nullable = false)
    private Cycle cycle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id")
    private Pitch pitch;

    @Column(nullable = false)
    private Integer position; // Position within the team's track (0, 1, 2, etc.)

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
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

    /**
     * Calculate duration of this slot in weeks
     */
    public int getDurationWeeks() {
        if (startDate == null || endDate == null) return 0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        return (int) Math.ceil(days / 7.0);
    }

    /**
     * Check if a pitch with given appetite (in days) fits in this slot
     */
    public boolean canFitPitch(int appetiteDays) {
        if (startDate == null || endDate == null) return false;
        long slotDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        return appetiteDays <= slotDays;
    }
}
