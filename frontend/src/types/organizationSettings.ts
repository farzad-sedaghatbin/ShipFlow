/**
 * Types for organization-wide settings
 */

export interface OrganizationSettings {
  id: number;
  organizationName: string;
  
  // Cycle Configuration
  defaultCycleLengthWeeks: number;
  defaultCooldownWeeks: number;
  
  // Risk Thresholds
  riskThresholds: RiskThresholds;
  
  // Categories
  taskCategories: CategoryConfig[];
  pitchCategories: CategoryConfig[];
  
  // Colors
  colors: ColorSettings;
  
  // Bug Configuration
  bugStatuses: BugStatusConfig[];
  severityLevels: SeverityLevelConfig[];
  
  // Other Settings
  timeZone: string;
  dateFormat: string;
  enableNotifications: boolean;
  enableAIFeatures: boolean;
  
  updatedAt: string;
  updatedBy: string;
}

export interface RiskThresholds {
  lowMax: number;      // 0-30 default
  mediumMax: number;   // 31-60 default
  highMax: number;     // 61-85 default
  // Above highMax is CRITICAL (86-100)
}

export interface CategoryConfig {
  id?: number;
  name: string;
  description: string;
  color: string;
  isActive: boolean;
  order: number;
}

export interface ColorSettings {
  appetiteHours: string;    // Color for appetite hours (e.g., "#3B82F6")
  actualHours: string;      // Color for actual hours (e.g., "#10B981")
  overBudget: string;       // Color when over budget (e.g., "#EF4444")
  underBudget: string;      // Color when under budget (e.g., "#22C55E")
}

excolors?: ColorSettings;
  bugStatuses?: BugStatusConfig[];
  severityLevels?: SeverityLevelConfig[];
  port interface BugStatusConfig {
  id?: number;
  name: string;
  description: string;
  color: string;
  isActive: boolean;
  order: number;
  isClosed: boolean;        // Whether this status means bug is closed
}

export interface SeverityLevelConfig {
  id?: number;
  name: string;
  description: string;
  color: string;
  isActive: boolean;
  order: number;
  priority: number;         // 1=Critical, 2=High, 3=Medium, 4=Low
}

export interface UpdateOrganizationSettingsRequest {
  organizationName?: string;
  defaultCycleLengthWeeks?: number;
  defaultCooldownWeeks?: number;
  riskThresholds?: RiskThresholds;
  taskCategories?: CategoryConfig[];
  pitchCategories?: CategoryConfig[];
  timeZone?: string;
  dateFormat?: string;
  enableNotifications?: boolean;
  enableAIFeatures?: boolean;
}

export interface RolePermissions {
  role: string;
  permissions: Permission[];
}

export interface Permission {
  resource: string;      // e.g., "users", "cycles", "pitches"
  actions: PermissionAction[];  // e.g., ["read", "create", "update", "delete"]
}

export type PermissionAction = 'read' | 'create' | 'update' | 'delete' | 'manage';
