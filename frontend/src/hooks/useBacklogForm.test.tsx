import { describe, it, expect, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import dayjs from 'dayjs';
import { useBacklogForm } from './useBacklogForm';
import { Task } from '../types';

// Mock react-i18next
vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

describe('useBacklogForm', () => {
  describe('Initial State', () => {
    it('should initialize with dialogs closed', () => {
      const { result } = renderHook(() => useBacklogForm());

      expect(result.current.dialogOpen).toBe(false);
      expect(result.current.editingTask).toBeNull();
      expect(result.current.viewDialogOpen).toBe(false);
      expect(result.current.viewTask).toBeNull();
      expect(result.current.deleteDialogOpen).toBe(false);
      expect(result.current.deleteTaskId).toBeNull();
    });

    it('should initialize with default form data', () => {
      const { result } = renderHook(() => useBacklogForm());

      expect(result.current.formData.title).toBe('');
      expect(result.current.formData.description).toBe('');
      expect(result.current.formData.status).toBe('BACKLOG');
      expect(result.current.formData.priority).toBe('MEDIUM');
      expect(result.current.formData.category).toBe('PITCH_SCOPE');
      expect(result.current.dueDate).toBeNull();
      expect(result.current.fieldErrors).toEqual({});
      expect(result.current.saving).toBe(false);
    });
  });

  describe('Create Dialog', () => {
    it('should open create dialog with cycle id and category', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(123, 'DEBT_IMPROVEMENT');
      });

      expect(result.current.dialogOpen).toBe(true);
      expect(result.current.editingTask).toBeNull();
      expect(result.current.formData.cycleId).toBe(123);
      expect(result.current.formData.category).toBe('DEBT_IMPROVEMENT');
      expect(result.current.formData.title).toBe('');
    });

    it('should close dialog and reset state', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(123, 'PITCH_SCOPE');
      });

      act(() => {
        result.current.closeDialog();
      });

      expect(result.current.dialogOpen).toBe(false);
      expect(result.current.editingTask).toBeNull();
      expect(result.current.fieldErrors).toEqual({});
    });
  });

  describe('Edit Dialog', () => {
    const mockTask: Task = {
      id: 1,
      title: 'Test Task',
      description: 'Test Description',
      cycleId: 100,
      status: 'IN_PROGRESS',
      priority: 'HIGH',
      category: 'DEBT_IMPROVEMENT',
      tags: 'tag1,tag2',
      estimateHours: 4,
      actualHours: 2,
      assigneeId: 5,
      pairAssigneeId: 6,
      dueDate: '2024-12-31',
      createdAt: '2024-01-01',
      updatedAt: '2024-01-02',
    };

    it('should open edit dialog with task data', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openEditDialog(mockTask);
      });

      expect(result.current.dialogOpen).toBe(true);
      expect(result.current.editingTask).toEqual(mockTask);
      expect(result.current.formData.title).toBe('Test Task');
      expect(result.current.formData.description).toBe('Test Description');
      expect(result.current.formData.status).toBe('IN_PROGRESS');
      expect(result.current.formData.priority).toBe('HIGH');
      expect(result.current.formData.category).toBe('DEBT_IMPROVEMENT');
      expect(result.current.formData.estimateHours).toBe(4);
      expect(result.current.formData.assigneeId).toBe(5);
      expect(result.current.dueDate).not.toBeNull();
    });
  });

  describe('Subtask Dialog', () => {
    const parentTask: Task = {
      id: 10,
      title: 'Parent Task',
      cycleId: 200,
      status: 'TODO',
      priority: 'MEDIUM',
      createdAt: '2024-01-01',
      updatedAt: '2024-01-02',
    };

    it('should open subtask dialog with parent task id', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openSubtaskDialog(parentTask, 'PITCH_SCOPE');
      });

      expect(result.current.dialogOpen).toBe(true);
      expect(result.current.editingTask).toBeNull();
      expect(result.current.formData.parentTaskId).toBe(10);
      expect(result.current.formData.cycleId).toBe(200);
      expect(result.current.formData.category).toBe('PITCH_SCOPE');
    });
  });

  describe('Form Data Updates', () => {
    it('should update form data', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(1, 'PITCH_SCOPE');
      });

      act(() => {
        result.current.updateFormData({ title: 'New Title', description: 'New Desc' });
      });

      expect(result.current.formData.title).toBe('New Title');
      expect(result.current.formData.description).toBe('New Desc');
    });

    it('should set due date', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.setDueDate(dayjs('2024-12-31'));
      });

      expect(result.current.dueDate).not.toBeNull();
      expect(result.current.dueDate?.format('YYYY-MM-DD')).toBe('2024-12-31');
    });
  });

  describe('Validation', () => {
    it('should validate required title', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(1, 'PITCH_SCOPE');
      });

      let isValid: boolean;
      act(() => {
        isValid = result.current.validateForm();
      });

      expect(isValid!).toBe(false);
      expect(result.current.fieldErrors.title).toBeDefined();
    });

    it('should validate title minimum length', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(1, 'PITCH_SCOPE');
      });

      act(() => {
        result.current.updateFormData({ title: 'ab' });
      });

      let isValid: boolean;
      act(() => {
        isValid = result.current.validateForm();
      });

      expect(isValid!).toBe(false);
      expect(result.current.fieldErrors.title).toBeDefined();
    });

    it('should validate cycle for Shape Up projects', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(0, 'PITCH_SCOPE'); // 0 is invalid cycle
      });

      act(() => {
        result.current.updateFormData({ title: 'Valid Title' });
      });

      let isValid: boolean;
      act(() => {
        isValid = result.current.validateForm(false); // not kanban
      });

      expect(isValid!).toBe(false);
      expect(result.current.fieldErrors.cycleId).toBeDefined();
    });

    it('should skip cycle validation for Kanban projects', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(0, 'PITCH_SCOPE');
      });

      act(() => {
        result.current.updateFormData({ title: 'Valid Title' });
      });

      let isValid: boolean;
      act(() => {
        isValid = result.current.validateForm(true); // kanban project
      });

      expect(isValid!).toBe(true);
      expect(result.current.fieldErrors.cycleId).toBeUndefined();
    });

    it('should validate positive estimate hours', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(1, 'PITCH_SCOPE');
      });

      act(() => {
        result.current.updateFormData({ title: 'Valid Title', estimateHours: -1 });
      });

      let isValid: boolean;
      act(() => {
        isValid = result.current.validateForm(true);
      });

      expect(isValid!).toBe(false);
      expect(result.current.fieldErrors.estimateHours).toBeDefined();
    });

    it('should pass validation with valid data', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(1, 'PITCH_SCOPE');
      });

      act(() => {
        result.current.updateFormData({ title: 'Valid Title' });
      });

      let isValid: boolean;
      act(() => {
        isValid = result.current.validateForm(true);
      });

      expect(isValid!).toBe(true);
      expect(Object.keys(result.current.fieldErrors)).toHaveLength(0);
    });
  });

  describe('getSubmitData', () => {
    it('should return form data with formatted due date', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(1, 'PITCH_SCOPE');
      });

      act(() => {
        result.current.updateFormData({ title: 'Test' });
        result.current.setDueDate(dayjs('2024-06-15'));
      });

      let submitData: ReturnType<typeof result.current.getSubmitData>;
      act(() => {
        submitData = result.current.getSubmitData();
      });

      expect(submitData!.title).toBe('Test');
      expect(submitData!.dueDate).toBe('2024-06-15');
    });

    it('should return undefined due date when not set', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openCreateDialog(1, 'PITCH_SCOPE');
      });

      let submitData: ReturnType<typeof result.current.getSubmitData>;
      act(() => {
        submitData = result.current.getSubmitData();
      });

      expect(submitData!.dueDate).toBeUndefined();
    });
  });

  describe('View Dialog', () => {
    const mockTask: Task = {
      id: 1,
      title: 'Test Task',
      status: 'TODO',
      priority: 'MEDIUM',
      cycleId: 1,
      createdAt: '2024-01-01',
      updatedAt: '2024-01-02',
    };

    it('should open view dialog', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openViewDialog(mockTask);
      });

      expect(result.current.viewDialogOpen).toBe(true);
      expect(result.current.viewTask).toEqual(mockTask);
    });

    it('should close view dialog and clear history', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openViewDialog(mockTask);
      });

      act(() => {
        result.current.closeViewDialog();
      });

      expect(result.current.viewDialogOpen).toBe(false);
      expect(result.current.viewTask).toBeNull();
      expect(result.current.viewHistory).toEqual([]);
    });

    it('should navigate to another task and add to history', () => {
      const { result } = renderHook(() => useBacklogForm());
      const secondTask: Task = { ...mockTask, id: 2, title: 'Second Task' };

      act(() => {
        result.current.openViewDialog(mockTask);
      });

      act(() => {
        result.current.navigateToTask(secondTask);
      });

      expect(result.current.viewTask).toEqual(secondTask);
      expect(result.current.viewHistory).toHaveLength(1);
      expect(result.current.viewHistory[0]).toEqual(mockTask);
    });

    it('should navigate back from history', () => {
      const { result } = renderHook(() => useBacklogForm());
      const secondTask: Task = { ...mockTask, id: 2, title: 'Second Task' };

      act(() => {
        result.current.openViewDialog(mockTask);
      });

      act(() => {
        result.current.navigateToTask(secondTask);
      });

      act(() => {
        result.current.navigateBack();
      });

      expect(result.current.viewTask).toEqual(mockTask);
      expect(result.current.viewHistory).toHaveLength(0);
    });
  });

  describe('Delete Dialog', () => {
    it('should open delete dialog with task id', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openDeleteDialog(42);
      });

      expect(result.current.deleteDialogOpen).toBe(true);
      expect(result.current.deleteTaskId).toBe(42);
    });

    it('should close delete dialog', () => {
      const { result } = renderHook(() => useBacklogForm());

      act(() => {
        result.current.openDeleteDialog(42);
      });

      act(() => {
        result.current.closeDeleteDialog();
      });

      expect(result.current.deleteDialogOpen).toBe(false);
      expect(result.current.deleteTaskId).toBeNull();
    });
  });
});
