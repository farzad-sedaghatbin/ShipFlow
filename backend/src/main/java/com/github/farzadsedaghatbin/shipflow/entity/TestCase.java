package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCasePriority;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestCaseType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Entity representing a test case in the QA system. Test cases can be linked to
 * pitches for test coverage tracking.
 */
@Entity
@Table(name = "test_cases", indexes = {@Index(name = "idx_test_case_pitch", columnList = "pitch_id"),
    @Index(name = "idx_test_case_cycle", columnList = "cycle_id"),
    @Index(name = "idx_test_case_status", columnList = "status"),
    @Index(name = "idx_test_case_created_by", columnList = "created_by_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class TestCase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Unique identifier for the test case (e.g., TC-001). */
  @NotAudited
  @Column(nullable = false, unique = true, length = 50)
  private String testCaseKey;

  /** Title of the test case. */
  @Column(nullable = false, length = 255)
  private String title;

  /** Detailed description of the test case in Markdown format. */
  @Column(columnDefinition = "TEXT")
  private String description;

  /** Preconditions required for the test. */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String preconditions;

  /** Test steps in Markdown format. */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String steps;

  /** Expected result of the test. */
  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String expectedResult;

  /** The pitch this test case is associated with. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  private Pitch pitch;

  /** The cycle this test case belongs to. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id")
  private Cycle cycle;

  /** The team responsible for this test case. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  /**
   * The scope (hill chart point) this test case is related to (optional). Links
   * the test case to a specific scope for better traceability.
   */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "scope_id")
  private HillChartPoint scope;

  /**
   * The task this test case is related to (optional). Links the test case to a
   * specific task for better traceability.
   */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  private Task task;

  /** The import job that created this test case (nullable for manually created cases). */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "import_job_id")
  private ImportJob importJob;

  /** Type of test case. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TestCaseType type;

  /** Priority of the test case. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TestCasePriority priority;

  /** Status of the test case. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TestCaseStatus status;

  /** Whether this test case was AI-generated. */
  @NotAudited
  @Builder.Default
  @Column(nullable = false)
  private Boolean aiGenerated = false;

  /** Tags for categorization (comma-separated). */
  @NotAudited
  @Column(length = 500)
  private String tags;

  /** Estimated execution time in minutes. */
  @NotAudited
  private Integer estimatedMinutes;

  /** User who created this test case. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id", nullable = false)
  private User createdBy;

  /** User who last updated this test case. */
  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by_id")
  private User updatedBy;

  /** Test runs associated with this test case. */
  @NotAudited
  @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<TestRun> testRuns = new ArrayList<>();

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime createdAt;

  @NotAudited
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // Soft delete fields
  @NotAudited
  @Column
  private LocalDateTime deletedAt;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by_id")
  private User deletedBy;

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
