package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ActionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TriggerType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "workflow_automation_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowAutomationTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false, length = 100)
  private String category;

  @Enumerated(EnumType.STRING)
  @Column(name = "trigger_type", nullable = false, length = 100)
  private TriggerType triggerType;

  @Column(name = "default_trigger_config", columnDefinition = "TEXT")
  private String defaultTriggerConfig;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false, length = 100)
  private ActionType actionType;

  @Column(name = "default_action_config", columnDefinition = "TEXT")
  private String defaultActionConfig;

  @Column(name = "icon_name", length = 100)
  private String iconName;

  @Column(name = "is_built_in", nullable = false)
  @Builder.Default
  private boolean builtIn = true;

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private int sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    if (createdAt == null) createdAt = LocalDateTime.now();
  }
}
