import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PluginsSettingsTab } from '../PluginsSettingsTab';
import { pluginService, type PluginDTO } from '../../../services/pluginService';

vi.mock('../../../services/pluginService', () => ({
  pluginService: { listPlugins: vi.fn(), togglePlugin: vi.fn() },
}));
vi.mock('../../../contexts', () => ({ useToast: () => ({ showToast: vi.fn() }) }));

const plugin = (over: Partial<PluginDTO>): PluginDTO => ({
  pluginId: 'demo-plugin',
  displayName: 'Demo Plugin',
  type: 'ui',
  enabled: true,
  ...over,
});

const renderTab = () =>
  render(
    <QueryClientProvider client={new QueryClient()}>
      <PluginsSettingsTab />
    </QueryClientProvider>,
  );

describe('PluginsSettingsTab', () => {
  beforeEach(() => vi.clearAllMocks());

  it('shows the plugin version next to its name, without doubling a leading v', async () => {
    vi.mocked(pluginService.listPlugins).mockResolvedValue({
      data: [
        plugin({ pluginId: 'a', displayName: 'Alpha', version: '1.2.0' }),
        plugin({ pluginId: 'b', displayName: 'Beta', version: 'v2.0.1' }),
      ],
    } as never);

    renderTab();

    expect(await screen.findByText('v1.2.0')).toBeInTheDocument();
    expect(screen.getByText('v2.0.1')).toBeInTheDocument();
    expect(screen.queryByText('vv2.0.1')).not.toBeInTheDocument();
  });

  it('shows no version text when the plugin does not state one', async () => {
    vi.mocked(pluginService.listPlugins).mockResolvedValue({
      data: [plugin({ pluginId: 'c', displayName: 'Gamma', version: null })],
    } as never);

    renderTab();

    await waitFor(() => expect(screen.getByText('Gamma')).toBeInTheDocument());
    expect(screen.queryByText(/^v\d/)).not.toBeInTheDocument();
  });
});
