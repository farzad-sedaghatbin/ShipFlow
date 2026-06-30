package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "wiki_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "page_id", nullable = false)
  private Long pageId;

  @Enumerated(EnumType.STRING)
  @Column(name = "storage_provider", nullable = false, length = 16)
  @Builder.Default
  private StorageProviderType storageProvider = StorageProviderType.LOCAL_FS;

  @Column(name = "storage_key", columnDefinition = "TEXT")
  private String storageKey;

  @Column(name = "file_name", nullable = false, length = 255)
  private String fileName;

  @Column(name = "content_type", length = 128)
  private String contentType;

  @Column(name = "file_size")
  private Long fileSize;

  @Column(name = "uploaded_by", nullable = false)
  private Long uploadedBy;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = OffsetDateTime.now();
    }
  }
}
