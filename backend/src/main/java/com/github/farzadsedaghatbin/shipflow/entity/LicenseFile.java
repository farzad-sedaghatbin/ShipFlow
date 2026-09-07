package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The currently-uploaded licence file. This table holds exactly one row — see
 * {@code V2026_09_07_0001__license_file.sql}. Uploading a new licence
 * (admin-only, {@code POST /api/license}) replaces this row's content rather
 * than appending history.
 *
 * <p>Not {@code @Audited} (Hibernate Envers), matching the sibling single-row
 * config entities {@link OrganizationSettings} and
 * {@code com.github.farzadsedaghatbin.shipflow.entity.StorageConfig}.
 */
@Entity
@Table(name = "license_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseFile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Raw licence file text: {@code {"payload": {...}, "signature": "<base64>"}}. */
  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "uploaded_at", nullable = false)
  private LocalDateTime uploadedAt;

  /** Username of the admin who uploaded this licence. */
  @Column(name = "uploaded_by")
  private String uploadedBy;
}
