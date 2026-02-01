package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/** Entity to store user theme preferences. */
@Entity
@Table(name = "user_preferences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  /** Theme mode: LIGHT, DARK, SYSTEM */
  @Enumerated(EnumType.STRING)
  @Column(name = "theme_mode", nullable = false)
  @Builder.Default
  private ThemeMode themeMode = ThemeMode.SYSTEM;

  /** Primary color preference (hex code) */
  @Column(name = "primary_color")
  private String primaryColor;

  /** Secondary color preference (hex code) */
  @Column(name = "secondary_color")
  private String secondaryColor;

  /** Compact view mode for lists */
  @Column(name = "compact_view")
  @Builder.Default
  private Boolean compactView = false;

  /** Show hill chart animations */
  @Column(name = "enable_animations")
  @Builder.Default
  private Boolean enableAnimations = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
  }
}
