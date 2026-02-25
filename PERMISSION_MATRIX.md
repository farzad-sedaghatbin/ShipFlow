# Permission Matrix - ShapeUp Tracker

## Role-Based Access Control (RBAC)

ShapeUp Tracker uses a 4-tier role model with fine-grained permissions:

- **ADMIN** - Full system access, can manage users, settings, and permissions
- **MANAGER** - Can manage cycles, pitches, teams, and approve bets
- **MEMBER** - Can create/update own work, contribute to pitches and tasks
- **READONLY** - Read-only access across all resources

---

## Permission Matrix

### Legend
- ✅ = Permission granted
- ❌ = Permission denied

| Resource | Action | ADMIN | MANAGER | MEMBER | READONLY |
|----------|--------|-------|---------|--------|----------|
| **CYCLE** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ❌ | ❌ |
| | DELETE | ✅ | ❌ | ❌ | ❌ |
| | MANAGE | ✅ | ✅ | ❌ | ❌ |
| **PITCH** | CREATE | ✅ | ✅ | ✅ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ✅ (own) | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| | APPROVE | ✅ | ✅ | ❌ | ❌ |
| **BUG** | CREATE | ✅ | ✅ | ✅ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ✅ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| **REPORT** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | EXECUTE | ✅ | ✅ | ❌ | ❌ |
| **PROJECT** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ❌ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| **TEAM** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ❌ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| | MANAGE | ✅ | ✅ | ❌ | ❌ |
| **USER** | CREATE | ✅ | ❌ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ❌ | ❌ | ❌ |
| | DELETE | ✅ | ❌ | ❌ | ❌ |
| | MANAGE | ✅ | ❌ | ❌ | ❌ |
| **DASHBOARD** | CREATE | ✅ | ✅ | ✅ (own) | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ✅ (own) | ❌ |
| | DELETE | ✅ | ✅ | ✅ (own) | ❌ |
| **AI_FEATURES** | READ | ✅ | ✅ | ✅ | ❌ |
| | EXECUTE | ✅ | ✅ | ✅ | ❌ |
| | MANAGE | ✅ | ❌ | ❌ | ❌ |
| **RISK** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ❌ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| | MANAGE | ✅ | ✅ | ❌ | ❌ |
| **RETROSPECTIVE** | CREATE | ✅ | ✅ | ✅ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ✅ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| | MANAGE | ✅ | ✅ | ❌ | ❌ |
| **BETTING_TABLE** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ❌ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| | MANAGE | ✅ | ✅ | ❌ | ❌ |
| **SYSTEM** | MANAGE | ✅ | ❌ | ❌ | ❌ |
| **INITIATIVE** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ❌ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| **EPIC** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ❌ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| **RELEASE** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ❌ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| **BACKLOG** | CREATE | ✅ | ✅ | ✅ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ✅ (own) | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| **WORKLOG** | CREATE | ✅ | ✅ | ✅ (own) | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ✅ (own) | ❌ |
| | DELETE | ✅ | ✅ | ✅ (own) | ❌ |
| | MANAGE | ✅ | ✅ | ❌ | ❌ |
| **MEETING** | CREATE | ✅ | ✅ | ✅ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ✅ (own) | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| **METRIC** | CREATE | ✅ | ✅ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ❌ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| **TEST_CASE** | CREATE | ✅ | ✅ | ✅ | ❌ |
| | READ | ✅ | ✅ | ✅ | ✅ |
| | UPDATE | ✅ | ✅ | ✅ | ❌ |
| | DELETE | ✅ | ✅ | ❌ | ❌ |
| | EXECUTE | ✅ | ✅ | ✅ | ❌ |
| **INTEGRATION** | CREATE | ✅ | ❌ | ❌ | ❌ |
| | READ | ✅ | ✅ | ✅ | ❌ |
| | UPDATE | ✅ | ❌ | ❌ | ❌ |
| | DELETE | ✅ | ❌ | ❌ | ❌ |
| | MANAGE | ✅ | ❌ | ❌ | ❌ |
| **WISE_ARCHITECTURE** | READ | ✅ | ✅ | ✅ | ❌ |
| | EXECUTE | ✅ | ✅ | ✅ | ❌ |
| | MANAGE | ✅ | ❌ | ❌ | ❌ |

---

## Role Migration

Previous roles were automatically migrated to the new 4-tier model:

| Old Role | New Role | Rationale |
|----------|----------|-----------|
| ADMIN | ADMIN | Unchanged - full system access |
| PROJECT_MANAGER | MANAGER | Manager authority over cycles, pitches, teams |
| PRODUCT | MANAGER | Similar authority to PROJECT_MANAGER |
| DEVELOPER | MEMBER | Contributor role |
| QA | MEMBER | Contributor role |

Migration SQL: `V57__migrate_to_simplified_roles.sql`

---

## Backend Implementation

### Permission Check Example

```java
@PreAuthorize("@permissionService.hasPermission('PITCH', 'CREATE')")
public ResponseEntity<Pitch> createPitch(@RequestBody PitchRequest request) {
    // Only users with PITCH:CREATE permission can access
}
```

### Service Layer

```java
permissionService.checkPermission("RISK", "READ"); // Throws if denied
```

---

## Frontend Implementation

### Using the `usePermission` Hook

```tsx
import { usePermission } from '@/hooks/usePermission';

function PitchActions({ pitchId }) {
  const { hasPermission } = usePermission();
  
  const canEdit = await hasPermission('PITCH', 'UPDATE');
  const canDelete = await hasPermission('PITCH', 'DELETE');
  
  return (
    <>
      {canEdit && <Button onClick={handleEdit}>Edit</Button>}
      {canDelete && <Button onClick={handleDelete}>Delete</Button>}
    </>
  );
}
```

### Using the `PermissionGate` Component

```tsx
import { PermissionGate } from '@/hooks/usePermission';

<PermissionGate resource="PITCH" permission="DELETE">
  <Button onClick={handleDelete}>Delete Pitch</Button>
</PermissionGate>
```

---

## Database Schema

Permissions are stored in the `permissions` table:

```sql
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    permission_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_permission UNIQUE (role, resource_type, permission_type)
);
```

Permission matrix defined in: `V58__update_rbac_for_simplified_roles.sql`

---

## API Endpoints

### Get Current User Permissions
```
GET /api/permissions/my-permissions
```

### Check Specific Permission
```
GET /api/permissions/has-permission?resourceType=PITCH&permissionType=CREATE
```

### Get All Permissions for a Role
```
GET /api/permissions/role/MANAGER
```

---

## Common Patterns

### Owner-Based Access
For resources like dashboards, members can manage their own:
- MEMBER can CREATE/UPDATE/DELETE their own dashboards
- MANAGER can manage all dashboards

This is enforced via additional checks:
```java
if (!isOwner && !hasRole("MANAGER")) {
    throw new AccessDeniedException();
}
```

### Context-Aware Permissions
Permissions may vary based on context:
- Members can UPDATE their assigned pitches
- Readonly users have VIEW-only across all resources

---

## Testing Permissions

### Backend Test
```java
@Test
@WithMockUser(roles = "MEMBER")
public void memberCannotDeletePitch() {
    assertThrows(AccessDeniedException.class, () -> {
        pitchService.deletePitch(1L);
    });
}
```

### Frontend Test
```tsx
it('hides delete button for readonly users', async () => {
  const { hasPermission } = usePermission();
  const canDelete = await hasPermission('PITCH', 'DELETE');
  expect(canDelete).toBe(false);
});
```

---

## Security Considerations

1. **Fail Closed** - Deny by default if permission check fails
2. **Cache Wisely** - Frontend permission cache cleared on user change/logout
3. **Audit Trail** - `old_role` column preserved in `users` table for migration audit
4. **Backend Enforcement** - Frontend checks are UX; backend always enforces
5. **Least Privilege** - READONLY role for observers, contractors, stakeholders

---

## See Also

- [V58__update_rbac_for_simplified_roles.sql](backend/src/main/resources/db/migration/V58__update_rbac_for_simplified_roles.sql) - Complete permission matrix
- [PermissionService.java](backend/src/main/java/com/github/farzadsedaghatbin/shipflow/service/PermissionService.java) - Backend permission service
- [usePermission.ts](frontend/src/hooks/usePermission.ts) - Frontend permission hook
