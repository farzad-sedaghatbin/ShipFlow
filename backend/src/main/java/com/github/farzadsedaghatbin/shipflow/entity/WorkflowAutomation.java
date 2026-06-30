package com.github.farzadsedaghatbin.shipflow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

@Entity
@Table(
    name = "workflow_automations",
    indexes = {
      @Index(name = "idx_automation_project", columnList = "project_id"),
      @Index(name = "idx_automation_trigger_type", columnList = "trigger_type"),
      @Index(name = "idx_automation_enabled", columnList = "enabled"),
      @Index(name = "idx_automation_deleted", columnList = "deleted_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class WorkflowAutomation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @NotAudited
  @Column(columnDefinition = "TEXT")
  private String description;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private Project project;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 100)
  private TriggerType triggerType;

  @NotAudited
  @Column(name = "trigger_config", columnDefinition = "TEXT")
  private String triggerConfig;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false, length = 100)
  private ActionType actionType;

  @NotAudited
  @Column(name = "action_config", columnDefinition = "TEXT")
  private String actionConfig;

  @Column(nullable = false)
  @Builder.Default
  private boolean enabled = true;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private WorkflowAutomationTemplate template;

  @NotAudited
  @Column(name = "execution_count", nullable = false)
  @Builder.Default
  private long executionCount = 0L;

  @NotAudited
  @Column(name = "last_triggered_at")
  private LocalDateTime lastTriggeredAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @NotAudited
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @NotAudited
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deleted_by_id")
  @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
  private User deletedBy;

  @PrePersist
  void prePersist() {
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (updatedAt == null) updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
