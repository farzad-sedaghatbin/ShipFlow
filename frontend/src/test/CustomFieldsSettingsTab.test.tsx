import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { CustomFieldsSettingsTab } from '../components/organizationSettings/CustomFieldsSettingsTab';
import { customFieldService } from '../services/customFieldService';

vi.mock('../services/customFieldService', () => ({
  customFieldService: {
    getDefinitions: vi.fn(),
    createDefinition: vi.fn(),
    updateDefinition: vi.fn(),
    deleteDefinition: vi.fn(),
  },
}));

vi.mock('../contexts', () => ({
  useToast: () => ({ showToast: vi.fn() }),
}));

const makeClient = () =>
  new QueryClient({ defaultOptions: { queries: { retry: false } } });

function wrap(ui: React.ReactNode) {
  return render(
    <QueryClientProvider client={makeClient()}>
      <BrowserRouter>{ui}</BrowserRouter>
    </QueryClientProvider>,
  );
}

const sampleDef = {
  id: 1,
  name: 'Sprint Notes',
  fieldType: 'TEXT' as const,
  entityType: 'TASK' as const,
  required: false,
  sortOrder: 0,
  createdAt: '2026-06-20T00:00:00Z',
  updatedAt: '2026-06-20T00:00:00Z',
};

beforeEach(() => {
  vi.clearAllMocks();
  (customFieldService.getDefinitions as ReturnType<typeof vi.fn>).mockResolvedValue({
    data: [sampleDef],
  });
});

describe('CustomFieldsSettingsTab', () => {
  it('renders definition cards after load', async () => {
    wrap(<CustomFieldsSettingsTab />);
    await waitFor(() => expect(screen.getByText('Sprint Notes')).toBeInTheDocument());
  });

  it('opens dialog when create button is clicked', async () => {
    const user = userEvent.setup();
    wrap(<CustomFieldsSettingsTab />);
    await waitFor(() => screen.getByText('Sprint Notes'));

    // The button contains a Plus icon + i18n key text
    const createBtn = screen.getByRole('button', { name: /customFields\.createField|create/i });
    await user.click(createBtn);

    await waitFor(() =>
      expect(screen.getByText(/customFields\.fieldName|field name/i)).toBeInTheDocument(),
    );
  });

  it('shows edit and delete buttons on each definition card', async () => {
    wrap(<CustomFieldsSettingsTab />);
    await waitFor(() => screen.getByText('Sprint Notes'));

    // Each definition card has 2 icon buttons (edit + delete)
    const iconButtons = screen.getAllByRole('button').filter(
      (b) => b.classList.contains('h-7') || b.querySelector('svg'),
    );
    expect(iconButtons.length).toBeGreaterThanOrEqual(2);
  });

  it('switches entity tabs', async () => {
    const user = userEvent.setup();
    wrap(<CustomFieldsSettingsTab />);
    await waitFor(() => screen.getByText('Sprint Notes'));

    (customFieldService.getDefinitions as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: [],
    });

    const pitchTab = screen.getByRole('button', { name: /PITCH|Pitches/i });
    await user.click(pitchTab);

    expect(customFieldService.getDefinitions).toHaveBeenCalledWith('PITCH');
  });
});
