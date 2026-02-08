package com.github.farzadsedaghatbin.shipflow.dto.cooldown;

import com.github.farzadsedaghatbin.shipflow.entity.CooldownActivity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CooldownActivityType;
import java.time.LocalDateTime;
import lombok.*;

/**
 * DTO for CooldownActivity entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooldownActivityDTO {

  private Long id;
  private Long cycleId;
  private String cycleName;
  private CooldownActivityType activityType;
  private String title;
  private String description;
  private CooldownActivityStatus status;
  private Long assigneeId;
  private String assigneeUsername;
  private Long createdById;
  private String createdByUsername;
  private Integer estimatedHours;
  private Integer actualHours;
  private Integer priority;
  private Long relatedPitchId;
  private String relatedPitchTitle;
  private String notes;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static CooldownActivityDTO fromEntity(CooldownActivity entity) {
    if (entity == null) return null;
    
    return CooldownActivityDTO.builder()
        .id(entity.getId())
        .cycleId(entity.getCycle() != null ? entity.getCycle().getId() : null)
        .cycleName(entity.getCycle() != null ? entity.getCycle().getName() : null)
        .activityType(entity.getActivityType())
        .title(entity.getTitle())
        .description(entity.getDescription())
        .status(entity.getStatus())
        .assigneeId(entity.getAssignee() != null ? entity.getAssignee().getId() : null)
        .assigneeUsername(entity.getAssignee() != null ? entity.getAssignee().getUsername() : null)
        .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
        .createdByUsername(entity.getCreatedBy() != null ? entity.getCreatedBy().getUsername() : null)
        .estimatedHours(entity.getEstimatedHours())
        .actualHours(entity.getActualHours())
        .priority(entity.getPriority())
        .relatedPitchId(entity.getRelatedPitch() != null ? entity.getRelatedPitch().getId() : null)
        .relatedPitchTitle(entity.getRelatedPitch() != null ? entity.getRelatedPitch().getTitle() : null)
        .notes(entity.getNotes())
        .startedAt(entity.getStartedAt())
        .completedAt(entity.getCompletedAt())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
