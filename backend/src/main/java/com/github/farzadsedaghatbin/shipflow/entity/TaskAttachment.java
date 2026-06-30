package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** A file attached to a {@link Task} by a user. */
@Entity
@Table(name = "task_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id", nullable = false)
  private Task task;

  /** Original filename as supplied by the uploader (sanitized). */
  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  /** Path on disk, relative to the configured upload directory. */
  @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
  private String filePath;

  @Column(name = "file_size", nullable = false)
  private Long fileSize;

  @Column(name = "content_type", nullable = false, length = 100)
  private String contentType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "uploaded_by")
  private User uploadedBy;

  /** Which storage backend holds the file bytes. Defaults to LOCAL_FS for existing attachments. */
  @Enumerated(EnumType.STRING)
  @Column(name = "storage_provider", length = 16)
  @Builder.Default
  private StorageProviderType storageProvider = StorageProviderType.LOCAL_FS;

  /**
   * Provider-specific key used to retrieve the file (e.g. S3 object key, MinIO path). Nullable —
   * LOCAL_FS attachments use {@code filePath} instead.
   */
  @Column(name = "storage_key", columnDefinition = "TEXT")
  private String storageKey;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
