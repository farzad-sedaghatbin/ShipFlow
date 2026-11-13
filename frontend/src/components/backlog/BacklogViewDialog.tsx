import { useTranslation } from 'react-i18next';
import dayjs from 'dayjs';
import { ChevronLeft, Eye, Link2, ExternalLink, Pencil } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Combobox } from '@/components/ui/combobox';
import { LocalizedDateInput } from '../LocalizedDateInput';
import { useToast } from '../../contexts';
import { getUserFriendlyError } from '../../utils/errorMessages';
import { Task, Person, CreateTaskRequest, TaskStatus, TaskPriority } from '../../types';
import { statusOptions, priorityOptions } from './backlogTypes';
import TaskDependencies from '../TaskDependencies';
import TaskAttachments from '../TaskAttachments';
import Comments from '../Comments';
import { taskService } from '../../services/taskService';
import TaskEpicField from '../TaskEpicField';

interface BacklogViewDialogProps {
  open: boolean;
  task: Task | null;
  subtasks: Task[];
  persons: Person[];
  viewHistory: Task[];
  onClose: () => void;
  onBack: () => void;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onReloadTasks: () => void;
  setViewDialogTask: (task: Task) => void;
}

export function BacklogViewDialog({
  open,
  task,
  subtasks,
  persons,
  viewHistory,
  onClose,
  onBack,
  onViewTask,
  onEditTask,
  onReloadTasks,
  setViewDialogTask,
}: BacklogViewDialogProps) {
  const { t } = useTranslation();
  const { showSuccess, showError } = useToast();

  // Runs one inline edit, then refreshes the open task and the list behind the dialog.
  const applyEdit = async (request: () => Promise<{ data: Task }>, successKey = 'backlogPage.taskUpdated') => {
    try {
      const res = await request();
      setViewDialogTask(res.data);
      onReloadTasks();
      showSuccess(t(successKey));
    } catch (error) {
      showError(getUserFriendlyError(error));
    }
  };

  // Fields without a dedicated PATCH endpoint go through the full update, same payload as the edit form.
  const updateFields = (current: Task, changes: Partial<CreateTaskRequest>) =>
    applyEdit(() =>
      taskService.update(current.id, {
        title: current.title,
        description: current.description || '',
        cycleId: current.cycleId,
        projectId: !current.cycleId ? current.projectId : undefined,
        status: current.status,
        priority: current.priority,
        estimateHours: current.estimateHours,
        actualHours: current.actualHours,
        teamId: current.teamId,
        assigneeId: current.assigneeId,
        pairAssigneeId: current.pairAssigneeId,
        dueDate: current.dueDate,
        tags: current.tags || '',
        category: current.category,
        pitchId: current.pitchId,
        parentTaskId: current.parentTaskId,
        epicId: current.epicId ?? undefined,
        ...changes,
      }),
    );

  // Shared links open the full task page, not this modal.
  const taskPath = task ? `/backlog/${task.projectKey ? `${task.projectKey}-${task.id}` : task.id}` : '';
  const taskUrl = `${window.location.origin}${taskPath}`;
  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(taskUrl);
      showSuccess(t('common.linkCopied'));
    } catch {
      showError(taskUrl);
    }
  };

  const hoursInput = (current: Task, field: 'estimateHours' | 'actualHours') => (
    <Input
      key={`${current.id}-${field}-${current[field] ?? ''}`}
      type="number"
      min={0}
      step="0.5"
      defaultValue={current[field] ?? ''}
      className="h-9"
      aria-label={t(field === 'estimateHours' ? 'backlogPage.estimate' : 'backlogPage.actual')}
      onBlur={(e) => {
        const value = e.target.value === '' ? undefined : Number(e.target.value);
        if (value !== (current[field] ?? undefined) && (value === undefined || value >= 0)) {
          updateFields(current, { [field]: value });
        }
      }}
    />
  );

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            {viewHistory.length > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={onBack}
                className="-ml-2"
              >
                <ChevronLeft className="h-4 w-4" />
                {t('backlogPage.back')}
              </Button>
            )}
            {task?.parentTaskId && viewHistory.length === 0 && (
              <span className="text-muted-foreground">└─</span>
            )}
            <span className="text-muted-foreground font-mono text-sm font-normal shrink-0">
              {task?.projectKey ? `${task.projectKey}-${task.id}` : `#${task?.id}`}
            </span>
            {task?.title}
          </DialogTitle>
        </DialogHeader>
        {task && (
          <div className="flex flex-wrap items-center gap-2 -mt-2">
            <Button variant="outline" size="sm" onClick={copyLink}>
              <Link2 className="h-4 w-4 mr-2" />
              {t('common.copyLink')}
            </Button>
            <Button variant="outline" size="sm" asChild>
              <a href={taskPath} target="_blank" rel="noopener noreferrer">
                <ExternalLink className="h-4 w-4 mr-2" />
                {t('backlogPage.openFullPage')}
              </a>
            </Button>
          </div>
        )}
        {task && (
          <div className="space-y-6">
            {/* Task Metadata */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label className="text-xs text-muted-foreground">{t('common.status')}</Label>
                <div className="mt-1">
                  <Select
                    value={task.status}
                    onValueChange={(v) => applyEdit(() => taskService.updateStatus(task.id, v as TaskStatus), 'backlogPage.statusUpdated')}
                  >
                    <SelectTrigger className="h-9" aria-label={t('common.status')}>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {statusOptions.map((o) => (
                        <SelectItem key={o.value} value={o.value}>{t(o.labelKey)}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.filters.priority')}</Label>
                <div className="mt-1">
                  <Select
                    value={task.priority}
                    onValueChange={(v) => applyEdit(() => taskService.updatePriority(task.id, v as TaskPriority), 'backlogPage.priorityUpdated')}
                  >
                    <SelectTrigger className="h-9" aria-label={t('backlogPage.filters.priority')}>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {priorityOptions.map((o) => (
                        <SelectItem key={o.value} value={o.value}>{t(o.labelKey)}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              {task.teamName && (
                <div>
                  <Label className="text-xs text-muted-foreground">{t('backlogPage.team')}</Label>
                  <div className="mt-1 font-medium">{task.teamName}</div>
                </div>
              )}
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.assignee')}</Label>
                <div className="mt-1">
                  <Combobox
                    options={[
                      { value: 'unassigned', label: t('backlogPage.unassigned') },
                      ...persons.map((p) => ({ value: p.id.toString(), label: p.name })),
                    ]}
                    value={task.assigneeId?.toString() || 'unassigned'}
                    onValueChange={(v) =>
                      applyEdit(() => taskService.updateAssignee(task.id, v === 'unassigned' ? null : Number(v)), 'backlogPage.assigneeUpdated')
                    }
                    placeholder={t('backlogPage.selectAssignee')}
                  />
                </div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.pairAssignee')}</Label>
                <div className="mt-1">
                  <Combobox
                    options={[
                      { value: 'none', label: t('backlogPage.none') },
                      ...persons.filter((p) => p.id !== task.assigneeId).map((p) => ({ value: p.id.toString(), label: p.name })),
                    ]}
                    value={task.pairAssigneeId?.toString() || 'none'}
                    onValueChange={(v) => updateFields(task, { pairAssigneeId: v === 'none' ? undefined : Number(v) })}
                    placeholder={t('backlogPage.selectPair')}
                  />
                </div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.estimate')}</Label>
                <div className="mt-1">{hoursInput(task, 'estimateHours')}</div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.actual')}</Label>
                <div className="mt-1">{hoursInput(task, 'actualHours')}</div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.dueDate')}</Label>
                <div className="mt-1">
                  <LocalizedDateInput
                    value={task.dueDate ? dayjs(task.dueDate).format('YYYY-MM-DD') : ''}
                    onChange={(v) => {
                      if (v && v !== (task.dueDate ? dayjs(task.dueDate).format('YYYY-MM-DD') : '')) {
                        updateFields(task, { dueDate: v });
                      }
                    }}
                    aria-label={t('backlogPage.dueDate')}
                  />
                </div>
              </div>
              <TaskEpicField
                task={task}
                onChanged={(updated) => {
                  setViewDialogTask(updated);
                  onReloadTasks();
                }}
              />
              {task.parentTaskTitle && (
                <div>
                  <Label className="text-xs text-muted-foreground">{t('backlogPage.parentTask')}</Label>
                  <div className="mt-1 font-medium">
                    {task.parentTaskTitle}
                  </div>
                </div>
              )}
            </div>

            {/* Description */}
            {task.description && (
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.description')}</Label>
                <div className="mt-2 p-3 bg-muted rounded-md text-sm whitespace-pre-wrap">
                  {task.description}
                </div>
              </div>
            )}

            {/* Tags */}
            {task.tags && (
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.tags')}</Label>
                <div className="mt-2 flex flex-wrap gap-1">
                  {task.tags.split(',').map((tag, idx) => (
                    <Badge key={idx} variant="outline" className="text-xs">
                      {tag.trim()}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* Task Dependencies */}
            <div className="border-t pt-4">
              <TaskDependencies
                taskId={task.id}
                cycleId={task.cycleId}
                onDependenciesChange={() => {
                  taskService.getById(task.id).then(response => {
                    setViewDialogTask(response.data);
                    onReloadTasks();
                  }).catch(error => {
                    console.error('Failed to reload task:', error);
                  });
                }}
              />
            </div>

            {/* File Attachments */}
            <div className="border-t pt-4">
              <TaskAttachments taskId={task.id} />
            </div>

            {/* Comments */}
            <div className="border-t pt-4">
              <Comments entityType="task" entityId={task.id} />
            </div>

            {/* Subtasks */}
            {subtasks.length > 0 && (
              <div>
                <Label className="text-sm font-semibold">{t('backlogPage.subtasks')} ({subtasks.length})</Label>
                <div className="mt-2 space-y-2">
                  {subtasks.map((subtask) => (
                    <div
                      key={subtask.id}
                      className="flex items-center justify-between p-3 border rounded-md hover:bg-muted/50 transition-colors"
                    >
                      <div className="flex-1">
                        <div className="flex items-center gap-2">
                          <span className="text-muted-foreground">└─</span>
                          <span className="font-medium">{subtask.title}</span>
                        </div>
                        {subtask.description && (
                          <p className="text-sm text-muted-foreground mt-1 ml-6">
                            {subtask.description}
                          </p>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <Badge variant={statusOptions.find(s => s.value === subtask.status)?.variant} className="text-xs">
                          {t(statusOptions.find(s => s.value === subtask.status)?.labelKey || 'backlogPage.statusOptions.backlog')}
                        </Badge>
                        <Badge variant={priorityOptions.find(p => p.value === subtask.priority)?.variant} className="text-xs">
                          {t(priorityOptions.find(p => p.value === subtask.priority)?.labelKey || 'backlogPage.priorityOptions.medium')}
                        </Badge>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => onViewTask(subtask)}
                          title={t('backlogPage.viewDetails')}
                          aria-label={t('aria.viewTask')}
                        >
                          <Eye className="h-3 w-3" aria-hidden="true" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8"
                          onClick={() => {
                            onClose();
                            onEditTask(subtask);
                          }}
                          title={t('backlogPage.edit')}
                          aria-label={t('aria.editTask')}
                        >
                          <Pencil className="h-3 w-3" aria-hidden="true" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Timestamps */}
            <div className="grid grid-cols-2 gap-4 pt-4 border-t">
              <div>
                <Label className="text-xs text-muted-foreground">Created</Label>
                <div className="mt-1 text-sm">
                  {dayjs(task.createdAt).format('MMM D, YYYY h:mm A')}
                </div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">Updated</Label>
                <div className="mt-1 text-sm">
                  {dayjs(task.updatedAt).format('MMM D, YYYY h:mm A')}
                </div>
              </div>
            </div>
          </div>
        )}
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            {t('backlogPage.close')}
          </Button>
          <Button onClick={() => {
            onClose();
            if (task) {
              onEditTask(task);
            }
          }}>
            <Pencil className="h-4 w-4 mr-2" />
            {t('backlogPage.edit')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
