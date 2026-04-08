package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notification_user_mapping",
    uniqueConstraints = @UniqueConstraint(columnNames = {"person_id", "provider_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationUserMapping {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id", nullable = false)
  private Person person;

  @Column(name = "provider_name", nullable = false, length = 50)
  private String providerName;

  @Column(name = "external_user_id", nullable = false)
  private String externalUserId;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
