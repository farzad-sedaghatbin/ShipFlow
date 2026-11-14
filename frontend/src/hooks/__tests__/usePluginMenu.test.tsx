import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { pluginLabel, pluginPath, usePluginNavItems } from '../usePluginMenu';
import { pluginService, type PluginMenuItem } from '@/services/pluginService';

vi.mock('@/services/pluginService', async (orig) => ({
  ...(await orig<typeof import('@/services/pluginService')>()),
  pluginService: { getMenu: vi.fn() },
}));

const item: PluginMenuItem = {
  pluginId: 'demo-plugin',
  key: 'overview',
  labels: { en: 'Overview', fa: 'نمای کلی' },
  icon: 'chart',
  requiredRole: 'ANY',
};

const wrapper = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    {children}
  </QueryClientProvider>
);

describe('pluginLabel / pluginPath', () => {
  it('uses the user language and falls back to English', () => {
    expect(pluginLabel(item, 'fa')).toBe('نمای کلی');
    expect(pluginLabel(item, 'fa-IR')).toBe('نمای کلی');
    expect(pluginLabel(item, 'de')).toBe('Overview');
  });

  it('builds an encoded in-app path', () => {
    expect(pluginPath(item)).toBe('/plugins/demo-plugin/overview');
  });
});

describe('usePluginNavItems', () => {
  beforeEach(() => vi.clearAllMocks());

  it('maps the menu into nav items', async () => {
    vi.mocked(pluginService.getMenu).mockResolvedValue({ data: [item] } as never);

    const { result } = renderHook(() => usePluginNavItems(), { wrapper });

    await waitFor(() => expect(result.current).toHaveLength(1));
    expect(result.current[0]).toMatchObject({ label: 'Overview', path: '/plugins/demo-plugin/overview' });
  });

  it('returns an empty list when the request fails', async () => {
    vi.mocked(pluginService.getMenu).mockRejectedValue(new Error('500'));

    const { result } = renderHook(() => usePluginNavItems(), { wrapper });

    await waitFor(() => expect(pluginService.getMenu).toHaveBeenCalled());
    expect(result.current).toEqual([]);
  });
});
