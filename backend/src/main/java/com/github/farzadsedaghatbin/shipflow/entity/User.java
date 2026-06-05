package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id")
  private Person person;

  @Column(nullable = false)
  private Boolean isActive;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @Column
  private LocalDateTime deletedAt;

  // --- SCIM / SSO provisioning fields (V2026_06_06_0001) ---

  @Column(name = "external_user_id", length = 500)
  private String externalUserId;

  @Enumerated(EnumType.STRING)
  @Column(name = "provisioned_via", length = 50)
  @Builder.Default
  private ProvisionedVia provisionedVia = ProvisionedVia.LOCAL;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (isActive == null) {
      isActive = true;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
