package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for permission information */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDTO {
  private Long id;
  private UserRole role;
  private ResourceType resourceType;
  private PermissionType permissionType;
  private String description;
  private LocalDateTime createdAt;
}
