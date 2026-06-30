import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { CustomFieldsSection } from '../components/CustomFieldsSection';
import { customFieldService } from '../services/customFieldService';

vi.mock('../services/customFieldService', () => ({
  customFieldService: {
    getDefinitions: vi.fn(),
    getValues: vi.fn(),
    bulkUpsertValues: vi.fn(),
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

const textDef = {
  id: 1,
  name: 'Sprint Notes',
  fieldType: 'TEXT' as const,
  entityType: 'TASK' as const,
  required: false,
  sortOrder: 0,
  createdAt: '2026-06-20T00:00:00Z',
  updatedAt: '2026-06-20T00:00:00Z',
};

const checkboxDef = {
  id: 3,
  name: 'Blocked',
  fieldType: 'CHECKBOX' as const,
  entityType: 'TASK' as const,
  required: false,
  sortOrder: 2,
  createdAt: '2026-06-20T00:00:00Z',
  updatedAt: '2026-06-20T00:00:00Z',
};

const noValues = { data: [] };

beforeEach(() => {
  vi.clearAllMocks();
  (customFieldService.getDefinitions as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [textDef] });
  (customFieldService.getValues as ReturnType<typeof vi.fn>).mockResolvedValue(noValues);
  (customFieldService.bulkUpsertValues as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] });
});

describe('CustomFieldsSection', () => {
  it('renders TEXT field as an input', async () => {
    wrap(<CustomFieldsSection entityType="TASK" entityId={1} />);
    await waitFor(() => expect(screen.getByText('Sprint Notes')).toBeInTheDocument());
    expect(screen.getByRole('textbox')).toBeInTheDocument();
  });

  it('renders nothing when no definitions', async () => {
    (customFieldService.getDefinitions as ReturnType<typeof vi.fn>).mockResolvedValue({ data: [] });
    (customFieldService.getValues as ReturnType<typeof vi.fn>).mockResolvedValue(noValues);
    const { container } = wrap(<CustomFieldsSection entityType="TASK" entityId={1} />);
    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });

  it('Save button is disabled when no pending changes', async () => {
    wrap(<CustomFieldsSection entityType="TASK" entityId={1} />);
    await waitFor(() => expect(screen.getByText('Sprint Notes')).toBeInTheDocument());
    const saveBtn = screen.getByRole('button', { name: /customFields\.saveFields|save/i });
    expect(saveBtn).toBeDisabled();
  });

  it('Save button enables after typing and calls bulkUpsertValues', async () => {
    wrap(<CustomFieldsSection entityType="TASK" entityId={1} />);
    await waitFor(() => expect(screen.getByText('Sprint Notes')).toBeInTheDocument());
    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'Hello' } });
    const saveBtn = screen.getByRole('button', { name: /customFields\.saveFields|save/i });
    expect(saveBtn).not.toBeDisabled();
    fireEvent.click(saveBtn);
    await waitFor(() =>
      expect(customFieldService.bulkUpsertValues).toHaveBeenCalledWith({
        entityType: 'TASK',
        entityId: 1,
        values: { 1: 'Hello' },
      }),
    );
  });

  it('shows required asterisk for required fields', async () => {
    (customFieldService.getDefinitions as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: [{ ...textDef, required: true }],
    });
    wrap(<CustomFieldsSection entityType="TASK" entityId={1} />);
    await waitFor(() => expect(screen.getByTitle('Required')).toBeInTheDocument());
  });

  it('renders CHECKBOX as a switch', async () => {
    (customFieldService.getDefinitions as ReturnType<typeof vi.fn>).mockResolvedValue({
      data: [checkboxDef],
    });
    wrap(<CustomFieldsSection entityType="TASK" entityId={1} />);
    await waitFor(() => expect(screen.getByText('Blocked')).toBeInTheDocument());
    expect(screen.getByRole('switch')).toBeInTheDocument();
  });
});
