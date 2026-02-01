package com.github.farzadsedaghatbin.shipflow.entity.teams;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "teams_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamsConfiguration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_name", nullable = false, unique = true)
  private String tenantName;

  @Column(name = "webhook_url", nullable = false, length = 1000)
  private String webhookUrl;

  @Column(name = "default_channel")
  private String defaultChannel;

  @Column(name = "is_enabled", nullable = false)
  private Boolean isEnabled;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (isEnabled == null) {
      isEnabled = true;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
