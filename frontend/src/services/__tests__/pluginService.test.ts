import { describe, it, expect, vi, beforeEach } from 'vitest';
import api from '../api';
import { pluginService } from '../pluginService';

vi.mock('../api', () => ({ default: { get: vi.fn(), patch: vi.fn() } }));

describe('pluginService UI endpoints', () => {
  beforeEach(() => vi.clearAllMocks());

  it('lists plugins from the admin endpoint', async () => {
    await pluginService.listPlugins();

    expect(api.get).toHaveBeenCalledWith('/v1/admin/plugins');
  });

  it('fetches the menu', async () => {
    await pluginService.getMenu();

    expect(api.get).toHaveBeenCalledWith('/v1/plugins/ui/menu');
  });

  it('fetches a page with an encoded path and the project id when given', async () => {
    await pluginService.getPage('demo-plugin', 'over view', 7);

    expect(api.get).toHaveBeenCalledWith('/v1/plugins/ui/demo-plugin/pages/over%20view', {
      params: { projectId: 7 },
    });
  });

  it('omits the project param when no project is selected', async () => {
    await pluginService.getPage('demo-plugin', 'overview', null);

    expect(api.get).toHaveBeenCalledWith('/v1/plugins/ui/demo-plugin/pages/overview', { params: undefined });
  });

  it('toggles a plugin', async () => {
    await pluginService.togglePlugin('builtin-plugin-showcase', true);

    expect(api.patch).toHaveBeenCalledWith('/v1/admin/plugins/builtin-plugin-showcase/enabled', { enabled: true });
  });
});
