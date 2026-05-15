import { Task, TaskStatus, TaskPriority } from '../../types';

export const statusOptions: {
  value: TaskStatus;
  labelKey: string;
  variant: 'default' | 'secondary' | 'destructive' | 'success' | 'warning' | 'info' | 'outline';
}[] = [
  { value: 'BACKLOG', labelKey: 'backlogPage.statusOptions.backlog', variant: 'secondary' },
  { value: 'TODO', labelKey: 'backlogPage.statusOptions.todo', variant: 'info' },
  { value: 'IN_PROGRESS', labelKey: 'backlogPage.statusOptions.inProgress', variant: 'default' },
  { value: 'BLOCKED', labelKey: 'backlogPage.statusOptions.blocked', variant: 'destructive' },
  { value: 'IN_REVIEW', labelKey: 'backlogPage.statusOptions.inReview', variant: 'warning' },
  { value: 'DONE', labelKey: 'backlogPage.statusOptions.done', variant: 'success' },
  { value: 'CANCELLED', labelKey: 'backlogPage.statusOptions.cancelled', variant: 'secondary' },
];

export const priorityOptions: {
  value: TaskPriority;
  labelKey: string;
  variant: 'default' | 'secondary' | 'destructive' | 'success' | 'warning' | 'info' | 'outline';
}[] = [
  { value: 'LOW', labelKey: 'backlogPage.priorityOptions.low', variant: 'secondary' },
  { value: 'MEDIUM', labelKey: 'backlogPage.priorityOptions.medium', variant: 'info' },
  { value: 'HIGH', labelKey: 'backlogPage.priorityOptions.high', variant: 'warning' },
  { value: 'URGENT', labelKey: 'backlogPage.priorityOptions.urgent', variant: 'destructive' },
];

export function getStatusBadgeVariant(status: TaskStatus) {
  return statusOptions.find(s => s.value === status)?.variant || 'secondary';
}

export function getPriorityBadgeVariant(priority: TaskPriority) {
  return priorityOptions.find(p => p.value === priority)?.variant || 'secondary';
}

export type ViewMode = 'list' | 'kanban' | 'gantt' | 'calendar';

export interface ViewDialogState {
  open: boolean;
  task: Task | null;
}

export interface DeleteDialogState {
  open: boolean;
  taskId: number | null;
}
