import api from './api';
import {
  DashboardWidget,
  CreateDashboardWidgetRequest,
  UpdateDashboardWidgetRequest,
  DashboardNotification
} from '../types/dashboard';

export const dashboardWidgetApi = {
  getAllWidgets: async (): Promise<DashboardWidget[]> => {
    const response = await api.get<DashboardWidget[]>('/dashboard/widgets');
    return response.data;
  },

  getVisibleWidgets: async (): Promise<DashboardWidget[]> => {
    const response = await api.get<DashboardWidget[]>('/dashboard/widgets/visible');
    return response.data;
  },

  createWidget: async (request: CreateDashboardWidgetRequest): Promise<DashboardWidget> => {
    const response = await api.post<DashboardWidget>('/dashboard/widgets', request);
    return response.data;
  },

  updateWidget: async (id: number, request: UpdateDashboardWidgetRequest): Promise<DashboardWidget> => {
    const response = await api.put<DashboardWidget>(`/dashboard/widgets/${id}`, request);
    return response.data;
  },

  bulkUpdateWidgets: async (widgets: DashboardWidget[]): Promise<DashboardWidget[]> => {
    const response = await api.put<DashboardWidget[]>('/dashboard/widgets/bulk', widgets);
    return response.data;
  },

  deleteWidget: async (id: number): Promise<void> => {
    await api.delete(`/dashboard/widgets/${id}`);
  },

  resetToDefaults: async (): Promise<DashboardWidget[]> => {
    const response = await api.post<DashboardWidget[]>('/dashboard/widgets/reset');
    return response.data;
  },
};

export const dashboardNotificationApi = {
  getAllNotifications: async (): Promise<DashboardNotification[]> => {
    const response = await api.get<DashboardNotification[]>('/dashboard/notifications');
    return response.data;
  },

  getUnreadNotifications: async (): Promise<DashboardNotification[]> => {
    const response = await api.get<DashboardNotification[]>('/dashboard/notifications/unread');
    return response.data;
  },

  getUnreadCount: async (): Promise<number> => {
    const response = await api.get<{ count: number }>('/dashboard/notifications/unread/count');
    return response.data.count;
  },

  markAsRead: async (id: number): Promise<DashboardNotification> => {
    const response = await api.put<DashboardNotification>(`/dashboard/notifications/${id}/read`);
    return response.data;
  },

  markAllAsRead: async (): Promise<void> => {
    await api.put('/dashboard/notifications/read-all');
  },

  deleteNotification: async (id: number): Promise<void> => {
    await api.delete(`/dashboard/notifications/${id}`);
  },

  generateNotifications: async (): Promise<void> => {
    await api.post('/dashboard/notifications/generate');
  },
};
