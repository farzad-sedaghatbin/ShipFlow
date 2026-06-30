import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { ProjectMembersTab } from '../components/projectSettings/ProjectMembersTab';
import { projectService } from '../services/projectService';

vi.mock('../services/projectService', () => ({
  projectService: {
    getMembers: vi.fn(),
    grantAccess: vi.fn(),
    revokeAccess: vi.fn(),
  },
}));

vi.mock('../services/commentService', () => ({
  commentService: {
    searchUsersForMention: vi.fn().mockResolvedValue({ data: [] }),
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

const sampleMembers = [
  {
    userId: 2,
    username: 'alice',
    projectRole: 'CONTRIBUTOR' as const,
    grantedAt: '2026-06-20T00:00:00Z',
  },
  {
    userId: 3,
    username: 'bob',
    projectRole: 'VIEWER' as const,
    grantedAt: '2026-06-20T00:00:00Z',
  },
];

beforeEach(() => {
  vi.clearAllMocks();
  (projectService.getMembers as ReturnType<typeof vi.fn>).mockResolvedValue(sampleMembers);
});

describe('ProjectMembersTab', () => {
  it('renders member list', async () => {
    wrap(<ProjectMembersTab projectId={1} isManager={false} />);
    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    expect(screen.getByText('bob')).toBeInTheDocument();
  });

  it('shows role badges when not manager (read-only)', async () => {
    wrap(<ProjectMembersTab projectId={1} isManager={false} />);
    await waitFor(() => screen.getByText('alice'));
    // Should show static badge text
    expect(screen.getByText('projectSettings.role.CONTRIBUTOR')).toBeInTheDocument();
  });

  it('shows role selects and remove buttons when manager', async () => {
    wrap(<ProjectMembersTab projectId={1} isManager={true} />);
    await waitFor(() => screen.getByText('alice'));
    const removeButtons = screen.getAllByRole('button').filter(
      (b) => b.querySelector('svg'),
    );
    expect(removeButtons.length).toBeGreaterThanOrEqual(1);
  });

  it('shows add member button only for manager', async () => {
    const { rerender } = wrap(<ProjectMembersTab projectId={1} isManager={false} />);
    await waitFor(() => screen.getByText('alice'));
    expect(screen.queryByRole('button', { name: /projectSettings\.addMember|add member/i })).toBeNull();

    rerender(
      <QueryClientProvider client={makeClient()}>
        <BrowserRouter>
          <ProjectMembersTab projectId={1} isManager={true} />
        </BrowserRouter>
      </QueryClientProvider>,
    );
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /projectSettings\.addMember|add member/i })).toBeInTheDocument(),
    );
  });

  it('shows no-members message when list is empty', async () => {
    (projectService.getMembers as ReturnType<typeof vi.fn>).mockResolvedValue([]);
    wrap(<ProjectMembersTab projectId={1} isManager={false} />);
    await waitFor(() =>
      expect(screen.getByText(/projectSettings\.noMembers|no members/i)).toBeInTheDocument(),
    );
  });
});
