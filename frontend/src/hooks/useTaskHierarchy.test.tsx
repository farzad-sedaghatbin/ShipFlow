import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { toast } from 'sonner';
import { useTaskHierarchy } from './useTaskHierarchy';
import { taskService } from '../services/taskService';
import type { Task } from '../types';

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (k: string) => k }) }));
vi.mock('sonner', () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
vi.mock('../services/taskService', () => ({
  taskService: { setParent: vi.fn(), assignCycle: vi.fn(), updateStatus: vi.fn() },
}));

const child = { id: 2, title: 'Loose', status: 'BACKLOG', cycleId: 11 } as Task;
const parent = { id: 1, title: 'Parent', status: 'TODO', cycleId: 10 } as Task;

type ToastOpts = { action?: { onClick: () => void } };
const lastToastAction = () => (vi.mocked(toast.success).mock.calls.at(-1)![1] as ToastOpts).action!;

beforeEach(() => vi.clearAllMocks());

describe('useTaskHierarchy', () => {
  it('Undo restores parent, cycle and status from the pre-change snapshot', async () => {
    vi.mocked(taskService.setParent).mockResolvedValueOnce({ data: { ...child, parentTaskId: 1, cycleId: 10, status: 'TODO' } } as never);
    const onChanged = vi.fn();
    const { result } = renderHook(() => useTaskHierarchy(onChanged));

    await act(async () => {
      expect(await result.current.makeSubtask(child, parent)).toBe(true);
    });
    expect(taskService.setParent).toHaveBeenCalledWith(2, 1);

    vi.mocked(taskService.setParent).mockResolvedValueOnce({ data: { ...child, cycleId: 10, status: 'TODO' } } as never);
    await act(async () => lastToastAction().onClick());

    await vi.waitFor(() => expect(taskService.updateStatus).toHaveBeenCalledWith(2, 'BACKLOG'));
    expect(taskService.setParent).toHaveBeenLastCalledWith(2, null);
    expect(taskService.assignCycle).toHaveBeenCalledWith(2, 11);
    expect(onChanged).toHaveBeenCalledTimes(2);
  });

  it('reports a refused conversion without calling onChanged', async () => {
    vi.mocked(taskService.setParent).mockRejectedValueOnce(new Error('nope'));
    const onChanged = vi.fn();
    const { result } = renderHook(() => useTaskHierarchy(onChanged));

    await act(async () => {
      expect(await result.current.makeSubtask(child, parent)).toBe(false);
    });
    expect(toast.error).toHaveBeenCalled();
    expect(onChanged).not.toHaveBeenCalled();
  });

  it('makeTopLevel offers Undo that re-attaches to the same parent', async () => {
    vi.mocked(taskService.setParent).mockResolvedValue({ data: child } as never);
    const sub = { ...child, parentTaskId: 1 } as Task;
    const { result } = renderHook(() => useTaskHierarchy(vi.fn()));

    await act(async () => result.current.makeTopLevel(sub));
    expect(taskService.setParent).toHaveBeenCalledWith(2, null);

    await act(async () => lastToastAction().onClick());
    await vi.waitFor(() => expect(taskService.setParent).toHaveBeenLastCalledWith(2, 1));
  });
});
