package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a test run execution in the QA system.
 * Tracks the execution of a test case with its results.
 */
@Entity
@Table(name = "test_runs", indexes = {
    @Index(name = "idx_test_run_test_case", columnList = "test_case_id"),
    @Index(name = "idx_test_run_cycle", columnList = "cycle_id"),
    @Index(name = "idx_test_run_status", columnList = "status"),
    @Index(name = "idx_test_run_executed_by", columnList = "executed_by_id"),
    @Index(name = "idx_test_run_executed_at", columnList = "executed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The test case being executed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    /**
     * The cycle in which this test was run.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cycle_id")
    private Cycle cycle;

    /**
     * The pitch being tested.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id")
    private Pitch pitch;

    /**
     * Status of the test run.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestRunStatus status;

    /**
     * User who executed the test.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_by_id", nullable = false)
    private User executedBy;

    /**
     * When the test was executed.
     */
    @Column(nullable = false)
    private LocalDateTime executedAt;

    /**
     * Duration of the test run in seconds.
     */
    private Integer durationSeconds;

    /**
     * Notes/comments about this test run.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * Actual result observed during execution.
     */
    @Column(columnDefinition = "TEXT")
    private String actualResult;

    /**
     * Environment/build version tested.
     */
    @Column(length = 255)
    private String buildVersion;

    /**
     * Test environment details.
     */
    @Column(length = 500)
    private String environment;

    /**
     * URL or path to screenshots/evidence.
     */
    @Column(columnDefinition = "TEXT")
    private String attachments;

    /**
     * Bug report created from this test run (if failed).
     */
    @OneToOne(mappedBy = "testRun", fetch = FetchType.LAZY)
    private BugReport bugReport;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
    }
}
