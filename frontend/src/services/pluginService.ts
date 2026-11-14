import api from './api';

export interface PluginDTO {
  pluginId: string;
  displayName: string;
  type: 'risk' | 'report' | 'integration' | 'ui';
  enabled: boolean;
  /** The plugin's own version, when it states one. */
  version?: string | null;
}

/** A sidebar entry contributed by a UI plugin (validated by the backend). */
export interface PluginMenuItem {
  pluginId: string;
  key: string;
  /** Display text by language code; always has "en". */
  labels: Record<string, string>;
  icon: string;
  requiredRole: 'ANY' | 'MANAGER' | 'ADMIN';
}

export interface PluginStat {
  label: string;
  value: string;
  hint?: string;
}

export interface PluginLink {
  label: string;
  target: string;
}

export type PluginBlock =
  | { type: 'MARKDOWN'; data: { text: string } }
  | { type: 'STAT_CARDS'; data: { stats: PluginStat[] } }
  | { type: 'TABLE'; data: { columns: string[]; rows: string[][] } }
  | { type: 'LINK_LIST'; data: { links: PluginLink[] } }
  | { type: 'ALERT'; data: { level: 'info' | 'success' | 'warning' | 'error'; text: string } };

export interface PluginPage {
  pluginId: string;
  pageKey: string;
  title: string;
  blocks: PluginBlock[];
}

export const pluginService = {
  listPlugins: () => api.get<PluginDTO[]>('/admin/plugins'),
  togglePlugin: (pluginId: string, enabled: boolean) =>
    api.patch<PluginDTO>(`/admin/plugins/${encodeURIComponent(pluginId)}/enabled`, { enabled }),
  getMenu: () => api.get<PluginMenuItem[]>('/plugins/ui/menu'),
  getPage: (pluginId: string, pageKey: string, projectId?: number | null) =>
    api.get<PluginPage>(
      `/plugins/ui/${encodeURIComponent(pluginId)}/pages/${encodeURIComponent(pageKey)}`,
      { params: projectId ? { projectId } : undefined },
    ),
};
