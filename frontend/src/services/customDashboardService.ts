import api from './api';
import {
  CustomDashboard,
  CreateDashboardRequest,
  UpdateDashboardRequest,
  DashboardWidgetConfig,
  AddWidgetRequest
} from '../types/customDashboard';

export const customDashboardService = {
  /**
   * Create a new custom dashboard
   */
  create: async (data: CreateDashboardRequest) => {
    const response = await api.post<CustomDashboard>('/dashboards/custom', data);
    return response.data;
  },

  /**
   * Get all dashboards for the current user
   */
  getAll: async () => {
    const response = await api.get<CustomDashboard[]>('/dashboards/custom');
    return response.data;
  },

  /**
   * Get a specific dashboard by ID
   */
  getById: async (id: number) => {
    const response = await api.get<CustomDashboard>(`/dashboards/custom/${id}`);
    return response.data;
  },

  /**
   * Update an existing dashboard
   */
  update: async (id: number, data: UpdateDashboardRequest) => {
    const response = await api.put<CustomDashboard>(`/dashboards/custom/${id}`, data);
    return response.data;
  },

  /**
   * Delete a dashboard
   */
  delete: async (id: number) => {
    const response = await api.delete(`/dashboards/custom/${id}`);
    return response.data;
  },

  /**
   * Set a dashboard as default
   */
  setAsDefault: async (id: number) => {
    const response = await api.post(`/dashboards/custom/${id}/set-default`);
    return response.data;
  },

  /**
   * Toggle user context filter for a dashboard
   * When enabled, widgets show data filtered to user's context (their cycles, teams, tasks)
   * When disabled, widgets show organization-wide data
   */
  toggleUserContextFilter: async (id: number) => {
    const response = await api.put<CustomDashboard>(`/dashboards/custom/${id}/toggle-context-filter`);
    return response.data;
  },

  /**
   * Get available dashboard templates
   */
  getTemplates: async () => {
    const response = await api.get<CustomDashboard[]>('/dashboards/custom/templates');
    return response.data;
  },

  /**
   * Add a widget to a dashboard
   */
  addWidget: async (dashboardId: number, data: AddWidgetRequest) => {
    const response = await api.post<DashboardWidgetConfig>(`/dashboards/custom/${dashboardId}/widgets`, data);
    return response.data;
  },

  /**
   * Get all widgets for a dashboard
   */
  getWidgets: async (dashboardId: number) => {
    const response = await api.get<DashboardWidgetConfig[]>(`/dashboards/custom/${dashboardId}/widgets`);
    return response.data;
  },

  /**
   * Remove a widget from a dashboard
   */
  removeWidget: async (dashboardId: number, widgetId: number) => {
    const response = await api.delete(`/dashboards/custom/${dashboardId}/widgets/${widgetId}`);
    return response.data;
  },

  /**
   * Update a widget configuration
   */
  updateWidget: async (dashboardId: number, widgetId: number, data: DashboardWidgetConfig) => {
    const response = await api.put<DashboardWidgetConfig>(`/dashboards/custom/${dashboardId}/widgets/${widgetId}`, data);
    return response.data;
  }
};

export default customDashboardService;
