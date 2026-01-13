package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.PermissionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Permission;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for permission management APIs.
 */
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions", description = "Permission management APIs for RBAC")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/current-user")
    @Operation(summary = "Get permissions for the current user")
    public ResponseEntity<List<PermissionDTO>> getCurrentUserPermissions() {
        List<Permission> permissions = permissionService.getCurrentUserPermissions();
        return ResponseEntity.ok(permissions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all permissions for a specific role (admin only)")
    public ResponseEntity<List<PermissionDTO>> getPermissionsForRole(@PathVariable UserRole role) {
        List<Permission> permissions = permissionService.getPermissionsForRole(role);
        return ResponseEntity.ok(permissions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/resource/{resourceType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all permissions for a specific resource type (admin only)")
    public ResponseEntity<List<PermissionDTO>> getPermissionsForResource(@PathVariable ResourceType resourceType) {
        List<Permission> permissions = permissionService.getPermissionsForResource(resourceType);
        return ResponseEntity.ok(permissions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    private PermissionDTO toDTO(Permission permission) {
        return PermissionDTO.builder()
                .id(permission.getId())
                .role(permission.getRole())
                .resourceType(permission.getResourceType())
                .permissionType(permission.getPermissionType())
                .description(permission.getDescription())
                .createdAt(permission.getCreatedAt())
                .build();
    }
}
