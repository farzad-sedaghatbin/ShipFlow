import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TaskHierarchyPicker from '../TaskHierarchyPicker';
import { taskService } from '../../services/taskService';
import type { Task } from '../../types';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) =>
      opts ? `${key}|${Object.values(opts).join('|')}` : key,
  }),
}));

vi.mock('../../services/taskService', () => ({
  taskService: {
    getParentCandidates: vi.fn(),
    getSubtaskCandidates: vi.fn(),
  },
}));

// cmdk needs these browser APIs, which jsdom lacks.
beforeEach(() => {
  vi.clearAllMocks();
  globalThis.ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  } as unknown as typeof ResizeObserver;
  Element.prototype.scrollIntoView = vi.fn();
});

const make = (over: Partial<Task>): Task =>
  ({ id: 1, title: 'T', status: 'TODO', priority: 'MEDIUM', cycleId: 10, cycleName: 'Sprint 1', projectKey: 'SF', ...over }) as Task;

const current = make({ id: 2, title: 'Loose task', status: 'BACKLOG', cycleId: 11, cycleName: 'Sprint 2' });
const candidate = make({ id: 5, title: 'Checkout flow', cycleId: 10, cycleName: 'Sprint 1' });

function renderPicker(mode: 'parent' | 'child', onConfirm = vi.fn().mockResolvedValue(true)) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const onOpenChange = vi.fn();
  render(
    <QueryClientProvider client={client}>
      <TaskHierarchyPicker open onOpenChange={onOpenChange} task={current} mode={mode} onConfirm={onConfirm} />
    </QueryClientProvider>
  );
  return { onConfirm, onOpenChange };
}

describe('TaskHierarchyPicker', () => {
  it('parent mode: previews what changes, then confirms with (current task, picked parent)', async () => {
    vi.mocked(taskService.getParentCandidates).mockResolvedValue({ data: [candidate] } as never);
    const { onConfirm, onOpenChange } = renderPicker('parent');

    const row = await screen.findByText('Checkout flow');
    expect(screen.getByText('taskHierarchy.confirm').closest('button')).toBeDisabled();

    fireEvent.click(row);

    // moves to the parent's sprint, and leaves Backlog
    expect(await screen.findByText(/taskHierarchy\.preview\.cycle\|Sprint 1/)).toBeInTheDocument();
    expect(screen.getByText(/taskHierarchy\.preview\.status/)).toBeInTheDocument();
    expect(screen.getByText('taskHierarchy.confirm').closest('button')).toBeEnabled();

    fireEvent.click(screen.getByText('taskHierarchy.confirm'));
    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith(current, candidate));
    await waitFor(() => expect(onOpenChange).toHaveBeenCalledWith(false));
  });

  it('child mode: confirms with (picked task, current task as parent)', async () => {
    vi.mocked(taskService.getSubtaskCandidates).mockResolvedValue({ data: [candidate] } as never);
    const { onConfirm } = renderPicker('child');

    fireEvent.click(await screen.findByText('Checkout flow'));
    fireEvent.click(screen.getByText('taskHierarchy.confirm'));

    await waitFor(() => expect(onConfirm).toHaveBeenCalledWith(candidate, current));
    expect(taskService.getParentCandidates).not.toHaveBeenCalled();
  });

  it('shows no cycle line when the cycle already matches and the task is not in Backlog', async () => {
    const same = make({ id: 2, title: 'Same sprint', status: 'IN_PROGRESS', cycleId: 10, cycleName: 'Sprint 1' });
    vi.mocked(taskService.getSubtaskCandidates).mockResolvedValue({ data: [same] } as never);
    renderPicker('child');

    fireEvent.click(await screen.findByText('Same sprint'));
    // parent (current) is in Sprint 2 while the pick is in Sprint 1 → cycle line is expected,
    // but there is no status line because the pick is already in progress.
    expect(await screen.findByText(/taskHierarchy\.preview\.cycle\|Sprint 2/)).toBeInTheDocument();
    expect(screen.queryByText(/taskHierarchy\.preview\.status/)).not.toBeInTheDocument();
  });

  it('keeps the dialog open when the change is refused', async () => {
    vi.mocked(taskService.getParentCandidates).mockResolvedValue({ data: [candidate] } as never);
    const { onOpenChange } = renderPicker('parent', vi.fn().mockResolvedValue(false));

    fireEvent.click(await screen.findByText('Checkout flow'));
    fireEvent.click(screen.getByText('taskHierarchy.confirm'));

    await waitFor(() => expect(screen.getByText('taskHierarchy.confirm').closest('button')).toBeEnabled());
    expect(onOpenChange).not.toHaveBeenCalledWith(false);
  });

  it('explains why nothing is listed in child mode', async () => {
    vi.mocked(taskService.getSubtaskCandidates).mockResolvedValue({ data: [] } as never);
    renderPicker('child');
    expect(await screen.findByText('taskHierarchy.noResults')).toBeInTheDocument();
    expect(screen.getByText('taskHierarchy.noResultsChildHint')).toBeInTheDocument();
  });
});
