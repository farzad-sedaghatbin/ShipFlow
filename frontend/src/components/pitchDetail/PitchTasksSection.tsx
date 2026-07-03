import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { Task, TaskStatus, TaskPriority } from '../../types';
import { taskService } from '../../services/taskService';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../ui/dialog';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';

interface PitchTasksSectionProps {
  tasks: Task[];
  pitchId: number;
  cycleId?: number | null;
  onTaskCreated?: (task: Task) => void;
}

export function PitchTasksSection({ tasks, pitchId, cycleId, onTaskCreated }: PitchTasksSectionProps) {
  const { t } = useTranslation();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [title, setTitle] = useState('');
  const [status, setStatus] = useState<TaskStatus>('TODO');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');

  // Group subtasks directly beneath their parent (instead of a flat list) so the
  // hierarchy is visible; orphan subtasks (parent not in this pitch's task list) fall
  // back to top-level order.
  const orderedTasks = useMemo(() => {
    const idsInList = new Set(tasks.map((task) => task.id));
    const childrenByParent = new Map<number, Task[]>();
    for (const task of tasks) {
      if (task.parentTaskId != null && idsInList.has(task.parentTaskId)) {
        const siblings = childrenByParent.get(task.parentTaskId) ?? [];
        siblings.push(task);
        childrenByParent.set(task.parentTaskId, siblings);
      }
    }
    const ordered: Task[] = [];
    for (const task of tasks) {
      const isNestedChild = task.parentTaskId != null && idsInList.has(task.parentTaskId);
      if (isNestedChild) continue;
      ordered.push(task);
      for (const child of childrenByParent.get(task.id) ?? []) {
        ordered.push(child);
      }
    }
    return ordered;
  }, [tasks]);

  const resetForm = () => {
    setTitle('');
    setStatus('TODO');
    setPriority('MEDIUM');
  };

  const handleCreate = async () => {
    if (!title.trim() || !cycleId) return;
    setSaving(true);
    try {
      const res = await taskService.create({
        title: title.trim(),
        cycleId,
        pitchId,
        status,
        priority,
        category: 'PITCH_SCOPE',
      });
      onTaskCreated?.(res.data);
      setDialogOpen(false);
      resetForm();
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle>{t('pitchDetailPage.tasks', 'Tasks')}</CardTitle>
            <div className="flex items-center gap-2">
              {cycleId && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setDialogOpen(true)}
                >
                  <Plus className="h-4 w-4 mr-1" />
                  {t('pitchDetailPage.createTask.button', 'Create Task')}
                </Button>
              )}
              <Link to="/backlog?category=PITCH_SCOPE">
                <Button variant="ghost" size="sm">
                  {t('pitchDetailPage.viewAllTasks', 'View All Tasks')}
                </Button>
              </Link>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {tasks.length === 0 ? (
            <p className="text-muted-foreground text-sm">
              {t('pitchDetailPage.noTasks', 'No tasks linked to this pitch yet.')}
            </p>
          ) : (
            <div className="space-y-2">
              {orderedTasks.map(task => {
                const isSubtask = task.parentTaskId != null;
                return (
                  <div
                    key={task.id}
                    className={isSubtask ? 'pl-6 border-l-2 border-border ml-2' : ''}
                  >
                    <Link
                      to={`/backlog/${task.id}`}
                      className={`block rounded-md border transition-colors ${
                        isSubtask ? 'border-border/60 bg-muted/30' : 'border-border'
                      } hover:bg-muted/50`}
                    >
                      <div className="flex items-center justify-between py-2 px-3 cursor-pointer">
                        <div className="flex items-center gap-3 flex-1 min-w-0">
                          {isSubtask && (
                            <span className="text-muted-foreground text-xs shrink-0" aria-hidden="true">└─</span>
                          )}
                          <Badge
                            variant={
                              task.status === 'DONE' ? 'success' :
                              task.status === 'IN_PROGRESS' ? 'default' :
                              task.status === 'BLOCKED' ? 'destructive' :
                              'secondary'
                            }
                            className="text-[10px] px-1.5 py-0 shrink-0"
                          >
                            {task.status?.replace(/_/g, ' ')}
                          </Badge>
                          <span className="text-sm truncate">{task.title}</span>
                        </div>
                        <div className="flex items-center gap-2 shrink-0 ml-2">
                          {task.assigneeName && (
                            <span className="text-xs text-muted-foreground">{task.assigneeName}</span>
                          )}
                          {task.priority && (
                            <Badge variant="outline" className="text-[10px] px-1.5 py-0">
                              {task.priority}
                            </Badge>
                          )}
                        </div>
                      </div>
                    </Link>
                    {isSubtask && task.parentTaskTitle && (
                      <Link
                        to={`/backlog/${task.parentTaskId}`}
                        className="text-[10px] text-muted-foreground hover:underline hover:text-foreground pl-3"
                      >
                        {t('pitchDetailPage.subtaskOf', 'Subtask of {{parentTitle}}', { parentTitle: task.parentTaskTitle })}
                      </Link>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={dialogOpen} onOpenChange={(open) => { setDialogOpen(open); if (!open) resetForm(); }}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('pitchDetailPage.createTask.dialogTitle', 'Create Task for Pitch')}</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 py-2">
            <div className="space-y-1.5">
              <Label htmlFor="task-title">{t('pitchDetailPage.createTask.title', 'Title')} *</Label>
              <Input
                id="task-title"
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder={t('pitchDetailPage.createTask.titlePlaceholder', 'Enter task title…')}
                autoFocus
                onKeyDown={e => { if (e.key === 'Enter') handleCreate(); }}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label>{t('pitchDetailPage.createTask.status', 'Status')}</Label>
                <Select value={status} onValueChange={v => setStatus(v as TaskStatus)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {(['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'] as TaskStatus[]).map(s => (
                      <SelectItem key={s} value={s}>{s.replace(/_/g, ' ')}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label>{t('pitchDetailPage.createTask.priority', 'Priority')}</Label>
                <Select value={priority} onValueChange={v => setPriority(v as TaskPriority)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as TaskPriority[]).map(p => (
                      <SelectItem key={p} value={p}>{p}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => { setDialogOpen(false); resetForm(); }}>
              {t('common.cancel', 'Cancel')}
            </Button>
            <Button onClick={handleCreate} disabled={!title.trim() || saving}>
              {saving ? t('common.saving', 'Saving…') : t('pitchDetailPage.createTask.submit', 'Create Task')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
