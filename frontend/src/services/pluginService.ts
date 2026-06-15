import api from './api';

export interface PluginDTO {
  pluginId: string;
  displayName: string;
  type: 'risk' | 'report' | 'integration';
  enabled: boolean;
}

export const pluginService = {
  listPlugins: () => api.get<PluginDTO[]>('/admin/plugins'),
  togglePlugin: (pluginId: string, enabled: boolean) =>
    api.patch<PluginDTO>(`/admin/plugins/${encodeURIComponent(pluginId)}/enabled`, { enabled }),
};
