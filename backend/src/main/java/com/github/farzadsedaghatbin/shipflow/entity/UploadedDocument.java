package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Entity for storing uploaded documents and their extracted content. */
@Entity
@Table(name = "uploaded_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedDocument {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String fileName;

  @Column(nullable = false)
  private String originalFileName;

  @Column(nullable = false)
  private String fileType;

  private Long fileSize;

  @Column(length = 500)
  private String storagePath;

  /**
   * Object-storage provider that holds this file. {@code null} for legacy rows written directly to
   * the filesystem (those are served via the {@code storagePath} fallback).
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "storage_provider", length = 20)
  private StorageProviderType storageProvider;

  /**
   * Key returned by the object-storage provider at store time. {@code null} for legacy rows — reads
   * and deletes fall back to {@code storagePath} on the local filesystem.
   */
  @Column(name = "storage_key", length = 512)
  private String storageKey;

  @Column(columnDefinition = "TEXT")
  private String extractedText;

  @Column(nullable = false)
  private boolean textExtracted;

  // Association - document can be linked to pitch, meeting, cycle, or note
  @Column(name = "entity_type")
  private String entityType; // PITCH, MEETING, CYCLE, NOTE

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "uploader_id")
  private Long uploaderId;

  @Column(name = "uploader_username")
  private String uploaderUsername;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "indexed_for_qa")
  private boolean indexedForQA;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
