import api from './api';
import { OrganizationSettings, UpdateOrganizationSettingsRequest, RolePermissions, RiskProfile } from '../types/organizationSettings';

/**
 * Service for managing organization-wide settings
 */
export const organizationSettingsService = {
  /**
   * Get current organization settings
   */
  getSettings: () => api.get<OrganizationSettings>('/admin/settings'),

  /**
   * Update organization settings
   */
  updateSettings: (request: UpdateOrganizationSettingsRequest) => 
    api.put<OrganizationSettings>('/admin/settings', request),

  /**
   * Get role permissions configuration
   */
  getRolePermissions: () => api.get<RolePermissions[]>('/admin/role-permissions'),

  /**
   * Get available risk weight profiles
   */
  getRiskProfiles: () => api.get<{ profiles: RiskProfile[] }>('/admin/settings/risk-profiles'),

  /**
   * Reset settings to defaults
   */
  resetToDefaults: () => api.post<OrganizationSettings>('/admin/settings/reset'),
};
