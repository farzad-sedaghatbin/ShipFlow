// Custom Dashboard Types

export interface CustomDashboard {
  id: number;
  userId: number;
  name: string;
  description?: string;
  isDefault: boolean;
  layoutConfig?: string; // JSON string
  templateCategory?: string;
  isTemplate: boolean;
  userContextFilter?: boolean; // When true, filter widgets to user's context
  
  // Scope fields
  cycleId?: number;
  cycleName?: string;
  pitchId?: number;
  pitchName?: string;
  teamId?: number;
  teamName?: string;
  
  createdAt: string;
  updatedAt: string;
}

export interface CreateDashboardRequest {
  name: string;
  description?: string;
  isDefault?: boolean;
  layoutConfig?: string;
  templateCategory?: string;
  cloneFromTemplateId?: number;
  
  // Scope fields
  cycleId?: number;
  pitchId?: number;
  teamId?: number;
}

export interface UpdateDashboardRequest {
  name?: string;
  description?: string;
  layoutConfig?: string;
}

export interface DashboardWidgetConfig {
  id?: number;
  dashboardId?: number;
  widgetId?: number;
  widgetType: string;
  position?: number;
  positionX?: number;
  positionY?: number;
  gridX?: number;
  gridY?: number;
  width?: number;
  height?: number;
  gridWidth?: number;
  gridHeight?: number;
  metricId?: number;
  customMetricId?: number;
  config?: any; // Flexible config object
  settings?: string; // JSON string
  createdAt?: string;
  updatedAt?: string;
}

export interface AddWidgetRequest {
  widgetType: string;
  metricId?: number;
  positionX?: number;
  positionY?: number;
  width?: number;
  height?: number;
  config?: any;
}

export interface DashboardLayout {
  breakpoints?: { lg: number; md: number; sm: number; xs: number };
  cols?: { lg: number; md: number; sm: number; xs: number };
  rowHeight?: number;
}

export interface DashboardTemplate {
  id: number;
  name: string;
  description: string;
  category: string;
  previewUrl?: string;
  widgetCount: number;
}
