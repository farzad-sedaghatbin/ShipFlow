import { useTranslation } from 'react-i18next';
import dayjs from 'dayjs';
import { ChevronLeft, Eye, Pencil } from 'lucide-react';
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
import { Task } from '../../types';
import { statusOptions, priorityOptions } from './backlogTypes';
import TaskDependencies from '../TaskDependencies';
import { taskService } from '../../services/taskService';

interface BacklogViewDialogProps {
  open: boolean;
  task: Task | null;
  subtasks: Task[];
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
  viewHistory,
  onClose,
  onBack,
  onViewTask,
  onEditTask,
  onReloadTasks,
  setViewDialogTask,
}: BacklogViewDialogProps) {
  const { t } = useTranslation();

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
            {task?.title}
          </DialogTitle>
        </DialogHeader>
        {task && (
          <div className="space-y-6">
            {/* Task Metadata */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <Label className="text-xs text-muted-foreground">{t('common.status')}</Label>
                <div className="mt-1">
                  <Badge variant={statusOptions.find(s => s.value === task.status)?.variant}>
                    {t(statusOptions.find(s => s.value === task.status)?.labelKey || 'backlogPage.statusOptions.backlog')}
                  </Badge>
                </div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.filters.priority')}</Label>
                <div className="mt-1">
                  <Badge variant={priorityOptions.find(p => p.value === task.priority)?.variant}>
                    {t(priorityOptions.find(p => p.value === task.priority)?.labelKey || 'backlogPage.priorityOptions.medium')}
                  </Badge>
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
                <div className="mt-1 font-medium">
                  {task.assigneeName || t('backlogPage.unassigned')}
                </div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.pairAssignee')}</Label>
                <div className="mt-1 font-medium">
                  {task.pairAssigneeName || t('backlogPage.none')}
                </div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.estimate')}</Label>
                <div className="mt-1 font-medium">
                  {task.estimateHours ? `${task.estimateHours}h` : '-'}
                </div>
              </div>
              <div>
                <Label className="text-xs text-muted-foreground">Actual</Label>
                <div className="mt-1 font-medium">
                  {task.actualHours ? `${task.actualHours}h` : '-'}
                </div>
              </div>
              {task.dueDate && (
                <div>
                  <Label className="text-xs text-muted-foreground">{t('backlogPage.dueDate')}</Label>
                  <div className="mt-1 font-medium">
                    {dayjs(task.dueDate).format('MMM D, YYYY')}
                  </div>
                </div>
              )}
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
                        >
                          <Eye className="h-3 w-3" />
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
                        >
                          <Pencil className="h-3 w-3" />
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
