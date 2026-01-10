export interface DashboardWidget {
  id: number;
  userId: number;
  widgetType: string;
  isVisible: boolean;
  displayOrder: number;
  layoutConfig?: string;
  settings?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDashboardWidgetRequest {
  widgetType: string;
  isVisible: boolean;
  displayOrder: number;
  layoutConfig?: string;
  settings?: string;
}

export interface UpdateDashboardWidgetRequest {
  isVisible?: boolean;
  displayOrder?: number;
  layoutConfig?: string;
  settings?: string;
}

export interface DashboardNotification {
  id: number;
  userId: number;
  type: string;
  title: string;
  message?: string;
  severity: 'INFO' | 'WARNING' | 'ERROR' | 'CRITICAL';
  actionUrl?: string;
  entityType?: string;
  entityId?: number;
  isRead: boolean;
  readAt?: string;
  expiresAt?: string;
  createdAt: string;
}
