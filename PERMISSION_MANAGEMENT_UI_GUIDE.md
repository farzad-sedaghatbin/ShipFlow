# Permission Management UI Guide

## Overview
The Permission Management UI provides a comprehensive interface for administrators to view and understand the role-based access control (RBAC) system in ShipFlow.

## Access
- **Route**: `/permissions`
- **Menu**: Admin > Permissions
- **Required Role**: ADMIN (for full access), all users can view their own permissions

## Features

### 1. Permission Matrix View
**Complete overview of all role permissions across resources**

- **Matrix Table**: Shows all roles (columns) vs. all resources (rows)
- **Permission Indicators**: Abbreviated permission types (C=Create, R=Read, U=Update, D=Delete, E=Execute, M=Manage, A=Approve)
- **Search & Filter**: 
  - Search resources by name
  - Filter by specific resource type
- **Legend**: Explains all permission type abbreviations
- **Hover Details**: Hover over permission badges to see full permission names

**Use Cases:**
- Quick overview of system-wide permissions
- Compare permissions across different roles
- Identify permission gaps or overlaps

### 2. Role Details View
**Deep dive into permissions for a specific role**

- **Role Selector**: Dropdown to select any role (ADMIN, PROJECT_MANAGER, PRODUCT, DEVELOPER, QA)
- **Resource Groups**: Permissions organized by resource type
- **Visual Indicators**: 
  - ✓ Green check = Has permissions
  - ✗ Gray X = No permissions
- **Permission Badges**: Shows all granted permission types per resource
- **Permission Count**: Displays count of permissions per resource

**Use Cases:**
- Understanding what a specific role can do
- Onboarding new team members
- Role assignment decisions
- Security audits

### 3. My Permissions View
**Personal permission dashboard for current user**

- **Role Badge**: Shows your current role with color coding
- **Total Count**: Displays total number of permissions you have
- **Resource Cards**: Grouped permissions by resource type
- **Permission Badges**: All your permissions displayed clearly

**Use Cases:**
- Understand your own access level
- Troubleshoot "access denied" issues
- Verify role assignment

## Role Color Coding

| Role | Color | Badge Style |
|------|-------|-------------|
| ADMIN | Red | Destructive |
| PROJECT_MANAGER | Blue | Default |
| PRODUCT | Gray | Secondary |
| DEVELOPER | Light Blue | Info |
| QA | Yellow | Warning |

## Permission Types

| Abbreviation | Full Name | Description |
|--------------|-----------|-------------|
| C | Create | Create new resources |
| R | Read/View | View resources |
| U | Update/Edit | Modify existing resources |
| D | Delete | Remove resources |
| E | Execute | Execute actions (AI, reports, etc.) |
| M | Manage | Full management control |
| A | Approve | Approve/reject actions |

## Resource Types

| Resource | Description |
|----------|-------------|
| CYCLE | Cycle management |
| PITCH | Pitch management |
| BUG | Bug tracking |
| REPORT | Report generation |
| PROJECT | Project administration |
| TEAM | Team management |
| USER | User administration |
| RISK | Risk management |
| DASHBOARD | Dashboard customization |
| RETROSPECTIVE | Retrospective meetings |
| BETTING_TABLE | Betting table management |
| AI_FEATURES | AI-powered features |
| SYSTEM | System settings |

## Default Permissions by Role

### ADMIN
- **Full Access**: All permissions (C, R, U, D, E, M, A) on all resources
- **Total**: ~200+ permissions

### PROJECT_MANAGER
- **Cycles**: Full management (C, R, U, D, M, A)
- **Pitches**: Full management (C, R, U, D, M, A)
- **Teams**: Full management (C, R, U, D, M)
- **Reports**: Execute, View (E, R)
- **Users**: View only (R)
- **Total**: ~140+ permissions

### PRODUCT
- **Pitches**: Create, View, Edit (C, R, U)
- **Cycles**: View (R)
- **Reports**: View (R)
- **Retrospectives**: Create, View, Edit (C, R, U)
- **Total**: ~60+ permissions

### DEVELOPER
- **Pitches**: View (R)
- **Bugs**: Create, View, Edit (C, R, U)
- **Tasks**: Create, View, Edit (C, R, U)
- **Work Logs**: Create, View, Edit (C, R, U)
- **Total**: ~40+ permissions

### QA
- **Test Cases**: Full management (C, R, U, D)
- **Bug Reports**: Full management (C, R, U, D)
- **AI Features**: Execute (E)
- **Pitches**: View (R)
- **Total**: ~50+ permissions

## Non-Admin Access

**Regular users** (non-ADMIN roles) have limited access:
- ✅ Can view "My Permissions" tab
- ❌ Cannot view "Permission Matrix" 
- ❌ Cannot view "Role Details"
- ❌ Cannot modify permissions

Users see an info banner explaining access restrictions.

## Technical Details

### API Endpoints Used
```
GET /api/permissions/my-permissions          - Current user's permissions
GET /api/permissions/role/{role}             - Permissions for specific role
GET /api/permissions/has-permission?resource=X&permission=Y - Check permission
```

### Service Layer
- **permissionService.ts**: TypeScript service handling all permission operations
- Type-safe permission checks
- User-friendly label mapping
- Role badge color management

### Components
- **PermissionManagement.tsx**: Main page component
- **PermissionCell**: Matrix cell with abbreviated permissions
- **RolePermissionsDetail**: Role-specific permission list
- **MyPermissionsView**: Current user permission dashboard

## Best Practices

### For Administrators
1. **Regular Audits**: Review permission matrix monthly
2. **Principle of Least Privilege**: Assign minimum required role
3. **Role Transitions**: When promoting users, review new permissions
4. **Onboarding**: Show new users their permissions using "My Permissions"

### For Users
1. **Check Your Permissions**: Visit "My Permissions" if you encounter access issues
2. **Role Understanding**: Understand what your role allows before requesting changes
3. **Request Changes**: If you need additional permissions, discuss role change with admin

## Future Enhancements

The current implementation is **read-only**. Planned features:

1. **Custom Permissions** (v2.0)
   - Create custom permission combinations
   - Assign permissions to individual users
   - Permission templates

2. **Resource-Level Permissions** (v2.1)
   - Permissions on specific projects/cycles
   - Team-based access control
   - Hierarchical permissions

3. **Permission History** (v2.2)
   - Audit log of permission changes
   - Role change tracking
   - Permission usage analytics

4. **External Identity Providers** (v3.0)
   - SAML/LDAP integration
   - SSO with role mapping
   - Group-based permissions

## Troubleshooting

### "Failed to load permissions"
- **Cause**: Backend API error or RBAC disabled
- **Solution**: Check `app.security.rbac.enabled=true` in application.properties
- **Check**: Verify database migration V44 has been applied

### "No permissions found"
- **Cause**: User has no role or role has no permissions
- **Solution**: Admin should assign a valid role to the user
- **Check**: Verify permissions table is populated (should have 200+ rows)

### "You don't have permission"
- **Cause**: Non-admin trying to access restricted views
- **Solution**: This is expected - only admins can view all permissions
- **Alternative**: Use "My Permissions" tab instead

### Permissions not enforced in app
- **Cause**: RBAC aspect disabled in configuration
- **Solution**: Ensure `app.security.rbac.enabled=true`
- **Development**: Test environment may have RBAC disabled

## Related Documentation

- [RBAC_GUIDE.md](./RBAC_GUIDE.md) - Complete RBAC architecture and backend implementation
- [RBAC_IMPLEMENTATION_SUMMARY.md](./RBAC_IMPLEMENTATION_SUMMARY.md) - Implementation details and deliverables
- [DASHBOARD_USER_GUIDE.md](./DASHBOARD_USER_GUIDE.md) - Dashboard features

## Support

For questions or issues:
1. Check RBAC_GUIDE.md for backend configuration
2. Review this guide for UI usage
3. Contact your system administrator
4. Check application logs for errors
