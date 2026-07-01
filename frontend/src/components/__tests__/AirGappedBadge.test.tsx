import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { TooltipProvider } from '@/components/ui/tooltip';
import { AirGappedBadge } from '../AirGappedBadge';
import { systemService, type AirGappedStatus } from '../../services/systemService';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('../../services/systemService', () => ({
  systemService: { getAirGappedStatus: vi.fn() },
}));

const mockGet = vi.mocked(systemService.getAirGappedStatus);

function renderBadge() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <TooltipProvider>
        <AirGappedBadge />
      </TooltipProvider>
    </QueryClientProvider>
  );
}

const enabledStatus: AirGappedStatus = {
  enabled: true,
  activeProvider: 'ollama',
  activeProviderLocal: true,
  ollamaBaseUrl: 'http://localhost:11434',
  ollamaReachable: true,
  externalMcpEnabled: [],
};

describe('AirGappedBadge', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the badge when air-gapped mode is enabled', async () => {
    mockGet.mockResolvedValue(enabledStatus);
    renderBadge();
    await waitFor(() => {
      expect(screen.getByTestId('air-gapped-badge')).toBeInTheDocument();
    });
    expect(screen.getByText('airGapped.badge')).toBeInTheDocument();
  });

  it('renders nothing when air-gapped mode is disabled', async () => {
    mockGet.mockResolvedValue({ ...enabledStatus, enabled: false });
    renderBadge();
    await waitFor(() => expect(mockGet).toHaveBeenCalled());
    expect(screen.queryByTestId('air-gapped-badge')).not.toBeInTheDocument();
  });
});
