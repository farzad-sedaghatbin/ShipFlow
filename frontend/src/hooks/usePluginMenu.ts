import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { pluginService, type PluginMenuItem } from '@/services/pluginService';
import { pluginIcon } from '@/config/pluginIcons';
import type { NavItemConfig } from '@/config/projectTypeCapabilities';

/** React Query key for the plugin menu — invalidate it when a plugin is enabled or disabled. */
export const PLUGIN_MENU_QUERY_KEY = ['plugins', 'ui', 'menu'] as const;

/** Picks the label for the user's language, falling back to English. */
export function pluginLabel(item: PluginMenuItem, language: string): string {
  const lang = (language || 'en').split('-')[0];
  return item.labels[lang] ?? item.labels.en ?? item.key;
}

export function pluginPath(item: Pick<PluginMenuItem, 'pluginId' | 'key'>): string {
  return `/plugins/${encodeURIComponent(item.pluginId)}/${encodeURIComponent(item.key)}`;
}

/**
 * Sidebar entries contributed by enabled UI plugins, shaped like the built-in nav items so
 * the desktop sidebar and the mobile "More" overlay render them the same way. A failed request
 * yields an empty list: plugins are optional and must never break navigation.
 */
export function usePluginNavItems(): NavItemConfig[] {
  const { i18n } = useTranslation();
  const { data } = useQuery({
    queryKey: PLUGIN_MENU_QUERY_KEY,
    queryFn: async () => (await pluginService.getMenu()).data,
    staleTime: 1000 * 60,
    retry: false,
  });

  return useMemo(
    () =>
      (data ?? []).map((item) => ({
        textKey: '',
        label: pluginLabel(item, i18n.language),
        icon: pluginIcon(item.icon),
        path: pluginPath(item),
      })),
    [data, i18n.language],
  );
}
