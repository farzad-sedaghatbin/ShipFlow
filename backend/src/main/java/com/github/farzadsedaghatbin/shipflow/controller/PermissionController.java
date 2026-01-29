package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.PermissionDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Permission;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.service.PermissionService;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    @GetMapping("/my-permissions")
    @Operation(summary = "Get permissions for the current user")
    public ResponseEntity<UserPermissionsResponse> getMyPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Permission> permissions = permissionService.getCurrentUserPermissions();
        
        UserPermissionsResponse response = UserPermissionsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .permissions(permissions)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all permissions for a specific role (admin only)")
    public ResponseEntity<List<Permission>> getPermissionsForRole(@PathVariable UserRole role) {
        List<Permission> permissions = permissionService.getPermissionsForRole(role);
        return ResponseEntity.ok(permissions);
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

    @GetMapping("/has-permission")
    @Operation(summary = "Check if current user has a specific permission")
    public ResponseEntity<Boolean> hasPermission(
            @RequestParam ResourceType resourceType,
            @RequestParam PermissionType permissionType) {
        boolean hasPermission = permissionService.hasPermission(resourceType, permissionType);
        return ResponseEntity.ok(hasPermission);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all permissions (admin only)")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new permission (admin only)")
    public ResponseEntity<Permission> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        Permission permission = permissionService.createPermission(
                request.getRole(),
                request.getResourceType(),
                request.getPermissionType(),
                request.getDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(permission);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing permission (admin only)")
    public ResponseEntity<Permission> updatePermission(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionRequest request) {
        Permission permission = permissionService.updatePermission(id, request.getDescription());
        return ResponseEntity.ok(permission);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a permission (admin only)")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create multiple permissions (admin only)")
    public ResponseEntity<List<Permission>> createBulkPermissions(@Valid @RequestBody List<CreatePermissionRequest> requests) {
        List<Permission> permissions = permissionService.createBulkPermissions(requests.stream()
                .map(r -> new PermissionService.PermissionRequest(
                        r.getRole(),
                        r.getResourceType(),
                        r.getPermissionType(),
                        r.getDescription()
                ))
                .collect(Collectors.toList()));
        return ResponseEntity.status(HttpStatus.CREATED).body(permissions);
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete multiple permissions (admin only)")
    public ResponseEntity<Void> deleteBulkPermissions(@RequestBody List<Long> ids) {
        permissionService.deleteBulkPermissions(ids);
        return ResponseEntity.noContent().build();
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
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPermissionsResponse {
        private Long userId;
        private String username;
        private UserRole role;
        private List<Permission> permissions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePermissionRequest {
        private UserRole role;
        private ResourceType resourceType;
        private PermissionType permissionType;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePermissionRequest {
        private String description;
    }
}
