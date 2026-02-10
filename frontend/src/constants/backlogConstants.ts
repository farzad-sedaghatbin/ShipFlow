import { TaskStatus, TaskPriority } from '../types';

export const STATUS_OPTIONS: { 
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

export const PRIORITY_OPTIONS: { 
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
  return STATUS_OPTIONS.find(s => s.value === status)?.variant || 'secondary';
}

export function getPriorityBadgeVariant(priority: TaskPriority) {
  return PRIORITY_OPTIONS.find(p => p.value === priority)?.variant || 'secondary';
}
