package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "wiki_pages")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WikiPage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "space_id", nullable = false)
  private Long spaceId;

  @Column(name = "parent_id")
  private Long parentId;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 255)
  private String slug;

  @Column(columnDefinition = "TEXT")
  private String content;

  @Column(name = "content_text", columnDefinition = "TEXT")
  private String contentText;

  @Column(nullable = false)
  @Builder.Default
  private Integer position = 0;

  @Column(name = "created_by", nullable = false)
  private Long createdBy;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
