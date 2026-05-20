package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ImportJobStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ImportSourceFormat;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Tracks a single CSV import job for competitor migration tooling (v1.2.0).
 * Supports Jira, Linear, Asana, and generic CSV formats.
 */
@Entity
@Table(
    name = "import_jobs",
    indexes = {
      @Index(name = "idx_import_jobs_created_by", columnList = "created_by_id"),
      @Index(name = "idx_import_jobs_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJob {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Original uploaded file name. */
  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  /** Detected or hinted CSV source format. */
  @Enumerated(EnumType.STRING)
  @Column(name = "source_format", nullable = false, length = 50)
  private ImportSourceFormat sourceFormat;

  /** Current processing status. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private ImportJobStatus status = ImportJobStatus.PENDING;

  /** Total number of data rows (excluding header). */
  @Column(name = "total_rows", nullable = false)
  @Builder.Default
  private int totalRows = 0;

  /** Number of successfully imported rows. */
  @Column(name = "imported_rows", nullable = false)
  @Builder.Default
  private int importedRows = 0;

  /** Number of rows that failed to import. */
  @Column(name = "failed_rows", nullable = false)
  @Builder.Default
  private int failedRows = 0;

  /** Per-row error messages (format: "Row N: <message>\n"). */
  @Column(name = "error_log", columnDefinition = "TEXT")
  private String errorLog;

  /** The Kanban project created/used for this import. Nullable until created. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

  /** User who triggered the import. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id")
  private User createdBy;

  /** When the import was initiated. */
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /** When the import finished (either COMPLETED or FAILED). */
  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (status == null) {
      status = ImportJobStatus.PENDING;
    }
  }
}
