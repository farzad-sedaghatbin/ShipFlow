import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import dayjs, { Dayjs } from 'dayjs';
import { Task, TaskStatus, TaskPriority, TaskCategory } from '../types';

export interface TaskFormData {
  title: string;
  description: string;
  cycleId?: number;
  parentTaskId?: number;
  status: TaskStatus;
  priority: TaskPriority;
  estimateHours?: number;
  actualHours?: number;
  teamId?: number;
  assigneeId?: number;
  pairAssigneeId?: number;
  dueDate?: string;
  tags: string;
  category: TaskCategory;
  pitchId?: number;
}

export interface UseBacklogFormReturn {
  // Dialog state
  dialogOpen: boolean;
  editingTask: Task | null;
  
  // Form data
  formData: TaskFormData;
  dueDate: Dayjs | null;
  fieldErrors: Record<string, string>;
  saving: boolean;
  
  // View dialog state  
  viewDialogOpen: boolean;
  viewTask: Task | null;
  viewHistory: Task[];
  
  // Delete dialog state
  deleteDialogOpen: boolean;
  deleteTaskId: number | null;
  
  // Actions
  openCreateDialog: (cycleId: number, category: TaskCategory) => void;
  openEditDialog: (task: Task) => void;
  openSubtaskDialog: (parentTask: Task, category: TaskCategory) => void;
  closeDialog: () => void;
  updateFormData: (updates: Partial<TaskFormData>) => void;
  setDueDate: (date: Dayjs | null) => void;
  setSaving: (saving: boolean) => void;
  validateForm: (isKanbanProject?: boolean) => boolean;
  
  // View dialog actions
  openViewDialog: (task: Task) => void;
  closeViewDialog: () => void;
  navigateBack: () => void;
  navigateToTask: (task: Task) => void;
  
  // Delete dialog actions
  openDeleteDialog: (taskId: number) => void;
  closeDeleteDialog: () => void;
  
  // Convenience getters
  getSubmitData: () => TaskFormData & { dueDate?: string };
}

const DEFAULT_FORM_DATA: TaskFormData = {
  title: '',
  description: '',
  cycleId: undefined,
  parentTaskId: undefined,
  status: 'BACKLOG',
  priority: 'MEDIUM',
  estimateHours: undefined,
  actualHours: undefined,
  teamId: undefined,
  assigneeId: undefined,
  pairAssigneeId: undefined,
  dueDate: undefined,
  tags: '',
  category: 'PITCH_SCOPE',
  pitchId: undefined,
};

/**
 * Hook to manage backlog form dialogs and state
 */
export function useBacklogForm(): UseBacklogFormReturn {
  const { t } = useTranslation();
  
  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  
  // Form state
  const [formData, setFormData] = useState<TaskFormData>(DEFAULT_FORM_DATA);
  const [dueDate, setDueDate] = useState<Dayjs | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  
  // View dialog state
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [viewTask, setViewTask] = useState<Task | null>(null);
  const [viewHistory, setViewHistory] = useState<Task[]>([]);
  
  // Delete dialog state
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deleteTaskId, setDeleteTaskId] = useState<number | null>(null);

  // Create dialog actions
  const openCreateDialog = useCallback((cycleId: number, category: TaskCategory) => {
    setEditingTask(null);
    setFormData({
      ...DEFAULT_FORM_DATA,
      cycleId,
      category,
    });
    setDueDate(null);
    setFieldErrors({});
    setDialogOpen(true);
  }, []);

  const openEditDialog = useCallback((task: Task) => {
    setEditingTask(task);
    setFormData({
      title: task.title,
      description: task.description || '',
      cycleId: task.cycleId,
      parentTaskId: task.parentTaskId,
      status: task.status,
      priority: task.priority,
      estimateHours: task.estimateHours,
      actualHours: task.actualHours,
      teamId: task.teamId,
      assigneeId: task.assigneeId,
      pairAssigneeId: task.pairAssigneeId,
      dueDate: task.dueDate,
      tags: task.tags || '',
      category: task.category || 'PITCH_SCOPE',
      pitchId: task.pitchId,
    });
    setDueDate(task.dueDate ? dayjs(task.dueDate) : null);
    setFieldErrors({});
    setDialogOpen(true);
  }, []);

  const openSubtaskDialog = useCallback((parentTask: Task, category: TaskCategory) => {
    setEditingTask(null);
    setFormData({
      ...DEFAULT_FORM_DATA,
      cycleId: parentTask.cycleId,
      parentTaskId: parentTask.id,
      category,
    });
    setDueDate(null);
    setFieldErrors({});
    setDialogOpen(true);
  }, []);

  const closeDialog = useCallback(() => {
    setDialogOpen(false);
    setEditingTask(null);
    setFieldErrors({});
  }, []);

  const updateFormData = useCallback((updates: Partial<TaskFormData>) => {
    setFormData(prev => ({ ...prev, ...updates }));
  }, []);

  const validateForm = useCallback((isKanbanProject?: boolean): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.title.trim()) {
      errors.title = t('backlogPage.titleRequired');
    } else if (formData.title.trim().length < 3) {
      errors.title = t('backlogPage.titleMinLength');
    }

    // Skip cycle validation for Kanban projects (cycle is auto-selected)
    if (!isKanbanProject && (!formData.cycleId || formData.cycleId === 0)) {
      errors.cycleId = t('backlogPage.cycleRequired');
    }

    if (formData.estimateHours !== undefined && formData.estimateHours < 0) {
      errors.estimateHours = t('backlogPage.estimatePositive');
    }

    if (formData.actualHours !== undefined && formData.actualHours < 0) {
      errors.actualHours = t('backlogPage.actualPositive');
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }, [formData, t]);

  const getSubmitData = useCallback(() => {
    return {
      ...formData,
      dueDate: dueDate ? dueDate.format('YYYY-MM-DD') : undefined,
    };
  }, [formData, dueDate]);

  // View dialog actions
  const openViewDialog = useCallback((task: Task) => {
    setViewTask(task);
    setViewDialogOpen(true);
  }, []);

  const closeViewDialog = useCallback(() => {
    setViewDialogOpen(false);
    setViewTask(null);
    setViewHistory([]);
  }, []);

  const navigateToTask = useCallback((task: Task) => {
    if (viewTask) {
      setViewHistory(prev => [...prev, viewTask]);
    }
    setViewTask(task);
  }, [viewTask]);

  const navigateBack = useCallback(() => {
    if (viewHistory.length > 0) {
      const previousTask = viewHistory[viewHistory.length - 1];
      setViewHistory(prev => prev.slice(0, -1));
      setViewTask(previousTask);
    }
  }, [viewHistory]);

  // Delete dialog actions
  const openDeleteDialog = useCallback((taskId: number) => {
    setDeleteTaskId(taskId);
    setDeleteDialogOpen(true);
  }, []);

  const closeDeleteDialog = useCallback(() => {
    setDeleteDialogOpen(false);
    setDeleteTaskId(null);
  }, []);

  return {
    // Dialog state
    dialogOpen,
    editingTask,
    
    // Form data
    formData,
    dueDate,
    fieldErrors,
    saving,
    
    // View dialog state
    viewDialogOpen,
    viewTask,
    viewHistory,
    
    // Delete dialog state
    deleteDialogOpen,
    deleteTaskId,
    
    // Actions
    openCreateDialog,
    openEditDialog,
    openSubtaskDialog,
    closeDialog,
    updateFormData,
    setDueDate,
    setSaving,
    validateForm,
    
    // View dialog actions
    openViewDialog,
    closeViewDialog,
    navigateBack,
    navigateToTask,
    
    // Delete dialog actions
    openDeleteDialog,
    closeDeleteDialog,
    
    // Convenience getters
    getSubmitData,
  };
}
