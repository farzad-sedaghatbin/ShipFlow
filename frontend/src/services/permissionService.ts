import api from './api';

export interface Permission {
  id: number;
  role: UserRole;
  resourceType: ResourceType;
  permissionType: PermissionType;
}

export type UserRole = 'ADMIN' | 'PROJECT_MANAGER' | 'PRODUCT' | 'DEVELOPER' | 'QA';

export type ResourceType = 
  | 'CYCLE' 
  | 'PITCH' 
  | 'BUG' 
  | 'REPORT' 
  | 'PROJECT' 
  | 'TEAM' 
  | 'USER' 
  | 'RISK' 
  | 'DASHBOARD' 
  | 'RETROSPECTIVE' 
  | 'BETTING_TABLE' 
  | 'AI_FEATURES' 
  | 'SYSTEM';

export type PermissionType = 
  | 'CREATE' 
  | 'READ' 
  | 'UPDATE' 
  | 'DELETE' 
  | 'EXECUTE' 
  | 'MANAGE' 
  | 'APPROVE';

export interface PermissionDTO {
  role: UserRole;
  resourceType: ResourceType;
  permissionType: PermissionType;
  description?: string;
}

export interface CreatePermissionRequest {
  role: UserRole;
  resourceType: ResourceType;
  permissionType: PermissionType;
  description?: string;
}

export interface UpdatePermissionRequest {
  description: string;
}

export interface UserPermissionsResponse {
  userId: number;
  username: string;
  role: UserRole;
  permissions: Permission[];
}

class PermissionService {
  /**
   * Get all permissions
   */
  async getAllPermissions(): Promise<Permission[]> {
    const response = await api.get<Permission[]>('/permissions');
    return response.data;
  }

  /**
   * Get all permissions for the current user
   */
  async getCurrentUserPermissions(): Promise<UserPermissionsResponse> {
    const response = await api.get<UserPermissionsResponse>('/permissions/my-permissions');
    return response.data;
  }

  /**
   * Get all permissions for a specific role
   */
  async getPermissionsByRole(role: UserRole): Promise<Permission[]> {
    const response = await api.get<Permission[]>(`/permissions/role/${role}`);
    return response.data;
  }

  /**
   * Create a new permission
   */
  async createPermission(request: CreatePermissionRequest): Promise<Permission> {
    const response = await api.post<Permission>('/permissions', request);
    return response.data;
  }

  /**
   * Update a permission's description
   */
  async updatePermission(id: number, request: UpdatePermissionRequest): Promise<Permission> {
    const response = await api.put<Permission>(`/permissions/${id}`, request);
    return response.data;
  }

  /**
   * Delete a permission
   */
  async deletePermission(id: number): Promise<void> {
    await api.delete(`/permissions/${id}`);
  }

  /**
   * Create multiple permissions at once
   */
  async createBulkPermissions(requests: CreatePermissionRequest[]): Promise<Permission[]> {
    const response = await api.post<Permission[]>('/permissions/bulk', requests);
    return response.data;
  }

  /**
   * Delete multiple permissions at once
   */
  async deleteBulkPermissions(ids: number[]): Promise<void> {
    await api.delete('/permissions/bulk', { data: ids });
  }

  /**
   * Check if current user has a specific permission
   */
  async hasPermission(resourceType: ResourceType, permissionType: PermissionType): Promise<boolean> {
    const response = await api.get<boolean>('/permissions/has-permission', {
      params: { resourceType, permissionType }
    });
    return response.data;
  }

  /**
   * Get all available resource types
   */
  getResourceTypes(): ResourceType[] {
    return [
      'CYCLE', 'PITCH', 'BUG', 'REPORT', 'PROJECT', 'TEAM', 
      'USER', 'RISK', 'DASHBOARD', 'RETROSPECTIVE', 
      'BETTING_TABLE', 'AI_FEATURES', 'SYSTEM'
    ];
  }

  /**
   * Get all available permission types
   */
  getPermissionTypes(): PermissionType[] {
    return ['CREATE', 'READ', 'UPDATE', 'DELETE', 'EXECUTE', 'MANAGE', 'APPROVE'];
  }

  /**
   * Get all available user roles
   */
  getUserRoles(): UserRole[] {
    return ['ADMIN', 'PROJECT_MANAGER', 'PRODUCT', 'DEVELOPER', 'QA'];
  }

  /**
   * Get user-friendly label for resource type
   */
  getResourceLabel(resourceType: ResourceType): string {
    const labels: Record<ResourceType, string> = {
      'CYCLE': 'Cycles',
      'PITCH': 'Pitches',
      'BUG': 'Bugs',
      'REPORT': 'Reports',
      'PROJECT': 'Projects',
      'TEAM': 'Teams',
      'USER': 'Users',
      'RISK': 'Risks',
      'DASHBOARD': 'Dashboard',
      'RETROSPECTIVE': 'Retrospectives',
      'BETTING_TABLE': 'Betting Table',
      'AI_FEATURES': 'AI Features',
      'SYSTEM': 'System Settings'
    };
    return labels[resourceType] || resourceType;
  }

  /**
   * Get user-friendly label for permission type
   */
  getPermissionLabel(permissionType: PermissionType): string {
    const labels: Record<PermissionType, string> = {
      'CREATE': 'Create',
      'READ': 'View',
      'UPDATE': 'Edit',
      'DELETE': 'Delete',
      'EXECUTE': 'Execute',
      'MANAGE': 'Manage',
      'APPROVE': 'Approve'
    };
    return labels[permissionType] || permissionType;
  }

  /**
   * Get role badge color
   */
  getRoleBadgeColor(role: UserRole): string {
    const colors: Record<UserRole, string> = {
      'ADMIN': 'destructive',
      'PROJECT_MANAGER': 'default',
      'PRODUCT': 'secondary',
      'DEVELOPER': 'info',
      'QA': 'warning'
    };
    return colors[role] || 'default';
  }
}

export const permissionService = new PermissionService();
export default permissionService;
