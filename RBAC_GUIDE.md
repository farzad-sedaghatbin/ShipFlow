# Role-Based Access Control (RBAC) System

## Overview

ShipFlow implements a comprehensive Role-Based Access Control (RBAC) system that provides fine-grained permissions for different user roles. This system allows administrators to control who can create, read, update, delete, and manage various resources in the application.

## Architecture

The RBAC system consists of the following components:

### 1. Permission Model

- **Permission Entity**: Represents a permission in the system, linking roles to specific actions on resources
- **ResourceType Enum**: Defines the types of resources that can be controlled (CYCLE, PITCH, BUG, REPORT, PROJECT, TEAM, USER, etc.)
- **PermissionType Enum**: Defines the types of actions that can be performed (CREATE, READ, UPDATE, DELETE, EXECUTE, MANAGE, APPROVE)

### 2. Core Components

#### Entities and Enums

```java
// Resource types
public enum ResourceType {
    CYCLE, PITCH, BUG, REPORT, PROJECT, TEAM, USER, 
    RISK, DASHBOARD, RETROSPECTIVE, BETTING_TABLE, 
    AI_FEATURES, SYSTEM
}

// Permission types
public enum PermissionType {
    CREATE, READ, UPDATE, DELETE, EXECUTE, MANAGE, APPROVE
}

// Permission entity
@Entity
public class Permission {
    private UserRole role;
    private ResourceType resourceType;
    private PermissionType permissionType;
    private String description;
}
```

#### Services

- **PermissionService**: Core service for checking and managing permissions
  - `hasPermission(username, resource, permission)`: Check if a user has a specific permission
  - `requirePermission(resource, permission)`: Throw AccessDeniedException if permission not granted
  - `getCurrentUserPermissions()`: Get all permissions for the current user
  - `getPermissionsForRole(role)`: Get all permissions for a role

#### Annotations

- **@RequirePermission**: Declarative permission checking annotation for controller methods

```java
@RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.CREATE)
public ResponseEntity<CycleDTO> createCycle(@RequestBody CreateCycleRequest request) {
    // ...
}
```

#### Aspect

- **PermissionAspect**: AOP aspect that intercepts @RequirePermission annotations and enforces permissions

## Default Role Permissions

### ADMIN Role
Full access to all resources:
- All CRUD operations on all resources
- System management
- User management
- AI features management

### PROJECT_MANAGER Role
Project and cycle management:
- Create, read, update cycles
- Create, read, update, delete, approve pitches
- Read and update bugs
- Create, read, execute reports
- Manage teams
- Use AI features

### PRODUCT Role
Product planning and bug tracking:
- Create, read, update pitches
- Read cycles
- Create, read, update bugs
- Read reports and dashboards
- Use AI features (read-only)

### DEVELOPER Role
Development and bug tracking:
- Read pitches and cycles
- Create, read, update bugs
- Read reports and dashboards
- Use AI features (read-only)

### QA Role
Quality assurance and testing:
- Read pitches and cycles
- Full CRUD on bugs
- Create and read reports
- Read dashboards
- Use AI features (read-only)

## Usage Guide

### 1. Controller Method Protection

Add `@RequirePermission` annotation to controller methods:

```java
@RestController
@RequestMapping("/api/cycles")
public class CycleController {
    
    @PostMapping
    @RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.CREATE)
    public ResponseEntity<CycleDTO> createCycle(@RequestBody CreateCycleRequest request) {
        return ResponseEntity.ok(cycleService.createCycle(request));
    }
    
    @DeleteMapping("/{id}")
    @RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.DELETE)
    public ResponseEntity<Void> deleteCycle(@PathVariable Long id) {
        cycleService.deleteCycle(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 2. Programmatic Permission Checking

Use `PermissionService` for conditional logic based on permissions:

```java
@Service
public class MyService {
    
    @Autowired
    private PermissionService permissionService;
    
    public void performAction() {
        if (permissionService.hasPermission(ResourceType.PITCH, PermissionType.APPROVE)) {
            // User can approve pitches
            approvePitch();
        } else {
            // User cannot approve pitches
            throw new AccessDeniedException("Cannot approve pitches");
        }
    }
    
    public void requireApproval() {
        // Throws AccessDeniedException if user lacks permission
        permissionService.requirePermission(ResourceType.PITCH, PermissionType.APPROVE);
        approvePitch();
    }
}
```

### 3. Frontend Integration

The frontend can fetch the current user's permissions to show/hide UI elements:

```typescript
// API endpoint: GET /api/permissions/current-user
const permissions = await api.get('/permissions/current-user');

// Check if user can create cycles
const canCreateCycle = permissions.some(p => 
    p.resourceType === 'CYCLE' && p.permissionType === 'CREATE'
);

if (canCreateCycle) {
    // Show "Create Cycle" button
}
```

## Database Migration

The RBAC system is initialized through Flyway migration `V44__add_rbac_permissions.sql`, which creates:

1. `permissions` table
2. Indexes for performance
3. Default permissions for all roles

## Configuration

### Enable/Disable RBAC

```properties
# application.properties
app.security.rbac.enabled=true  # default

# application-test.properties
app.security.rbac.enabled=false  # disabled for tests
```

### Test Mode

In test mode (when Flyway is disabled), the RBAC aspect is disabled to avoid database dependency. Existing `@PreAuthorize` annotations are used for backward compatibility.

## Best Practices

### 1. Layered Security

Use both `@PreAuthorize` and `@RequirePermission` for defense in depth:

```java
@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'PROJECT_MANAGER')")  // Spring Security check
@RequirePermission(resource = ResourceType.CYCLE, permission = PermissionType.CREATE)  // RBAC check
public ResponseEntity<CycleDTO> createCycle(@RequestBody CreateCycleRequest request) {
    // ...
}
```

### 2. Resource-Level Permissions

For resources that belong to specific users or teams, add ownership checks:

```java
@Service
public class PitchService {
    
    public void updatePitch(Long pitchId, UpdateRequest request) {
        // Check RBAC permission
        permissionService.requirePermission(ResourceType.PITCH, PermissionType.UPDATE);
        
        // Check ownership
        Pitch pitch = pitchRepository.findById(pitchId);
        if (!pitch.getCreatedBy().equals(getCurrentUser()) && !isAdmin()) {
            throw new AccessDeniedException("You can only update your own pitches");
        }
        
        // Perform update
    }
}
```

### 3. Graceful Degradation

The permission system gracefully handles missing permissions table (returns true in catch block) to support:
- Test environments without Flyway
- Development environments during schema evolution
- Backward compatibility with existing `@PreAuthorize` checks

## API Endpoints

### Get Current User Permissions
```
GET /api/permissions/current-user
```
Returns all permissions for the authenticated user.

### Get Permissions for Role (Admin Only)
```
GET /api/permissions/role/{role}
```
Returns all permissions for a specific role.

### Get Permissions for Resource (Admin Only)
```
GET /api/permissions/resource/{resourceType}
```
Returns all permissions for a specific resource type.

## Testing

### Unit Tests

The `PermissionServiceTest` class provides comprehensive coverage:
- Permission checking for valid/invalid users
- Inactive user handling
- Role-based permission checking
- Current user permission retrieval
- Permission creation and deletion

### Integration Tests

Since the test environment has RBAC disabled, existing controller integration tests rely on `@PreAuthorize` annotations.

To test RBAC in production-like environment:
1. Enable Flyway in test profile
2. Load permissions from migration
3. Test with actual permission data

## Future Enhancements

### 1. Custom Permissions
Allow administrators to create custom permissions through UI:
```java
@PostMapping("/api/permissions")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<PermissionDTO> createPermission(@RequestBody CreatePermissionRequest request) {
    return ResponseEntity.ok(permissionService.createPermission(
        request.getRole(),
        request.getResourceType(),
        request.getPermissionType(),
        request.getDescription()
    ));
}
```

### 2. Resource-Level Permissions
Extend permissions to specific resource instances (e.g., "Can edit Cycle #123"):
```java
@Entity
public class ResourcePermission {
    private UserRole role;
    private ResourceType resourceType;
    private Long resourceId;  // Specific resource instance
    private PermissionType permissionType;
}
```

### 3. Team-Based Permissions
Add team-level permissions for collaborative work:
```java
public enum PermissionScope {
    GLOBAL,    // System-wide
    PROJECT,   // Project-level
    TEAM,      // Team-level
    PERSONAL   // User's own resources
}
```

### 4. External Identity Provider Integration
Integrate with enterprise identity providers (LDAP, Active Directory, OAuth2):
- Map external roles to internal permissions
- Sync permissions from identity provider
- Support federated identity

### 5. Permission Inheritance
Implement permission hierarchies where roles inherit permissions:
```java
@Entity
public class RoleHierarchy {
    private UserRole parentRole;
    private UserRole childRole;
    // Child inherits all parent permissions
}
```

## Troubleshooting

### Issue: 403 Forbidden on Previously Working Endpoints
**Cause**: RBAC system was added and permissions are not loaded.
**Solution**: Ensure Flyway migration V44 has run successfully.

### Issue: Tests Failing with Permission Errors
**Cause**: RBAC enabled in test mode without permissions data.
**Solution**: Set `app.security.rbac.enabled=false` in application-test.properties.

### Issue: Permission Check Always Returns True
**Cause**: Exception in permission check falls back to allowing access.
**Solution**: Check logs for errors. Ensure permissions table exists and is populated.

## Migration from Legacy System

For existing deployments:

1. **Backup Database**: Always backup before running migrations
2. **Run Migration**: `V44__add_rbac_permissions.sql` creates tables and default permissions
3. **Test Permissions**: Verify each role can access expected resources
4. **Update Controllers**: Add `@RequirePermission` annotations gradually
5. **Monitor Logs**: Check for permission denials and adjust as needed

## Conclusion

The RBAC system provides ShipFlow with enterprise-grade access control, allowing fine-grained permissions while maintaining backward compatibility. It's designed to be extensible and can grow with your organization's security needs.
