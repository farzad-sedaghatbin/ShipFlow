import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import BugReportsPage from '../BugReportsPage';
import qaTestManagementService from '../../services/qaTestManagementService';

// The page seeds its status/severity/assignee filter state from a localStorage payload
// (`shipflow.bugFilters`). Pre-v1.8 builds stored these filters as a single scalar rather
// than an array; reading such a legacy value straight into array state made the URL-sync
// effect crash on `.forEach` — bricking the Bug Reports page until the user cleared site
// data. These tests lock in that a malformed (scalar) payload now loads cleanly.

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, fallback?: any) => (typeof fallback === 'string' ? fallback : key),
    i18n: { language: 'en' },
  }),
}));

vi.mock('../../contexts', () => ({
  useProject: () => ({
    currentProject: { id: 1, name: 'Demo' },
    isAllProjectsSelected: false,
    isKanbanProject: false,
    isSwitchingProject: false,
    notifyProjectSwitchComplete: vi.fn(),
  }),
  useAuth: () => ({ user: { userId: 7 } }),
  useToast: () => ({ showToast: vi.fn() }),
}));

vi.mock('../../services/qaTestManagementService', () => ({
  default: {
    getBugReportsWithFilters: vi.fn().mockResolvedValue({
      data: { content: [], totalElements: 0, totalPages: 0 },
    }),
    getBugStats: vi.fn().mockResolvedValue({
      data: { total: 0, open: 0, inProgress: 0, resolved: 0, critical: 0 },
    }),
  },
}));

vi.mock('../../services/cycleService', () => ({
  cycleService: { getMyCycles: vi.fn().mockResolvedValue({ data: [] }) },
}));
vi.mock('../../services/pitchService', () => ({
  pitchService: { getMyPitches: vi.fn().mockResolvedValue({ data: [] }) },
}));
vi.mock('../../services/releaseService', () => ({
  releaseService: { getByProject: vi.fn().mockResolvedValue({ data: [] }) },
}));
vi.mock('../../services/personService', () => ({
  personService: { getAll: vi.fn().mockResolvedValue([]) },
}));

const renderPage = () =>
  render(
    <MemoryRouter initialEntries={['/qa/bug-reports']}>
      <BugReportsPage />
    </MemoryRouter>
  );

describe('BugReportsPage — legacy localStorage filter hardening', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders without crashing when filters were persisted as scalars (pre-v1.8 shape)', async () => {
    localStorage.setItem(
      'shipflow.bugFilters',
      JSON.stringify({
        statusFilter: 'OPEN', // legacy: single string instead of string[]
        severityFilter: 'CRITICAL', // legacy: single string instead of string[]
        assigneeFilter: 42, // legacy: single number instead of number[]
        searchQuery: '',
      })
    );

    expect(() => renderPage()).not.toThrow();

    // The data load fires, proving the URL-sync effect didn't crash on `.forEach`.
    await waitFor(() =>
      expect(qaTestManagementService.getBugReportsWithFilters).toHaveBeenCalled()
    );
  });

  it('still loads cleanly with a well-formed array payload', async () => {
    localStorage.setItem(
      'shipflow.bugFilters',
      JSON.stringify({
        statusFilter: ['OPEN', 'IN_PROGRESS'],
        severityFilter: ['MAJOR'],
        assigneeFilter: [1, 2],
        searchQuery: '',
      })
    );

    expect(() => renderPage()).not.toThrow();
    await waitFor(() =>
      expect(qaTestManagementService.getBugReportsWithFilters).toHaveBeenCalled()
    );
  });
});
