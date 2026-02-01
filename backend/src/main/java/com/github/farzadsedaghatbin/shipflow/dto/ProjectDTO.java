package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ProjectType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDTO {

  private Long id;
  private String name;
  private String projectKey;
  private String description;
  private String color;
  private String logoUrl;
  private Long ownerId;
  private String ownerName;
  private Boolean isActive;

  /**
   * Project methodology type. SHAPE_UP: 6-week cycles with betting, pitches, and cooldown KANBAN:
   * Continuous flow with visual board, no cycles
   */
  private ProjectType projectType;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Integer cycleCount;
  private Integer activeCycleCount;

  /**
   * The current user's role within this project. VIEWER, CONTRIBUTOR, or MANAGER. Null if user has
   * access via ADMIN role or team membership only.
   */
  private String userProjectRole;
}
