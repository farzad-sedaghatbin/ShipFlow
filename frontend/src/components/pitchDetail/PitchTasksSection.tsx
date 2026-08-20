import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, List, Kanban } from 'lucide-react';
import { toast } from 'sonner';
import { Task, TaskStatus, TaskPriority } from '../../types';
import { taskService } from '../../services/taskService';
import { getUserFriendlyError } from '../../utils/errorMessages';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../ui/dialog';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '../ui/tooltip';
import KanbanBoard from '../KanbanBoard';
import { PitchTaskList } from './PitchTaskList';

type PitchTaskViewMode = 'list' | 'board';

interface PitchTasksSectionProps {
  tasks: Task[];
  pitchId: number;
  /** Pitch's resolved project id (see PitchService.toDTO's cycle→pitch→epic project fallback
   *  chain). Null means the pitch is genuinely unresolvable server-side (no cycle, no direct
   *  project, no epic-with-project) — Create Task is disabled in that case, see §B.5. */
  projectId?: number | null;
  onTaskCreated?: (task: Task) => void;
  /** Fired after a Kanban drag-and-drop status change persists. */
  onTaskUpdated?: (task: Task) => void;
  /** Fired after a task is deleted from the board. */
  onTaskDeleted?: (taskId: number) => void;
}

export function PitchTasksSection({
  tasks,
  pitchId,
  projectId,
  onTaskCreated,
  onTaskUpdated,
  onTaskDeleted,
}: PitchTasksSectionProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  // Defaults to list: unlike the Kanban board (which scatters a task's sub-tasks across whatever
  // status column each one happens to be in), the list groups sub-tasks under their parent and
  // collapses them by default, which is what actually makes top-level tasks easy to find.
  const [viewMode, setViewMode] = useState<PitchTaskViewMode>('list');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [title, setTitle] = useState('');
  const [status, setStatus] = useState<TaskStatus>('TODO');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');
  // Set when the dialog was opened via "Add subtask" from a Kanban card — the created task
  // gets parentTaskId wired to this task instead of being a top-level pitch task.
  const [parentTask, setParentTask] = useState<Task | null>(null);

  const resetForm = () => {
    setTitle('');
    setStatus('TODO');
    setPriority('MEDIUM');
    setParentTask(null);
  };

  const handleOpenCreateDialog = () => {
    setParentTask(null);
    setDialogOpen(true);
  };

  const handleAddSubtask = (task: Task) => {
    setParentTask(task);
    setDialogOpen(true);
  };

  const handleCreate = async () => {
    if (!title.trim()) return;
    setSaving(true);
    try {
      // No cycleId sent — the backend derives it from pitchId alone (a pitch not yet bet to a
      // cycle simply produces a cycle-less task, see §B.5/B.2).
      const res = await taskService.create({
        title: title.trim(),
        pitchId: parentTask?.pitchId ?? pitchId,
        parentTaskId: parentTask?.id,
        status,
        priority,
      });
      onTaskCreated?.(res.data);
      setDialogOpen(false);
      resetForm();
    } catch (error) {
      toast.error(getUserFriendlyError(error));
    } finally {
      setSaving(false);
    }
  };

  const handleStatusChange = async (taskId: number, newStatus: TaskStatus) => {
    try {
      const res = await taskService.updateStatus(taskId, newStatus);
      onTaskUpdated?.(res.data);
      toast.success(t('backlogPage.statusUpdated'));
    } catch (error) {
      toast.error(getUserFriendlyError(error));
    }
  };

  const handleViewTask = (task: Task) => navigate(`/backlog/${task.id}`);
  // This component doesn't own a task detail/edit dialog — route to the same place as "view"
  // (the full task detail page supports editing), matching the plan's sensible-default choice.
  const handleEditTask = (task: Task) => navigate(`/backlog/${task.id}`);

  const handleDeleteTask = async (taskId: number) => {
    if (!window.confirm(t('backlogPage.confirmDelete'))) return;
    try {
      await taskService.delete(taskId);
      toast.success(t('backlogPage.taskDeleted'));
      onTaskDeleted?.(taskId);
    } catch (error) {
      toast.error(getUserFriendlyError(error));
    }
  };

  const createDisabled = !projectId;

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle>{t('pitchDetailPage.tasks', 'Tasks')}</CardTitle>
            <div className="flex items-center gap-2">
              <div className="flex items-center border rounded-md">
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button
                        variant={viewMode === 'list' ? 'default' : 'ghost'}
                        size="sm"
                        onClick={() => setViewMode('list')}
                        className="rounded-r-none border-r"
                      >
                        <List className="h-4 w-4" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>{t('backlogPage.viewMode.list')}</TooltipContent>
                  </Tooltip>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button
                        variant={viewMode === 'board' ? 'default' : 'ghost'}
                        size="sm"
                        onClick={() => setViewMode('board')}
                        className="rounded-l-none"
                      >
                        <Kanban className="h-4 w-4" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>{t('backlogPage.viewMode.kanban')}</TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              </div>
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <span>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={handleOpenCreateDialog}
                        disabled={createDisabled}
                      >
                        <Plus className="h-4 w-4 mr-1" />
                        {t('pitchDetailPage.createTask.button', 'Create Task')}
                      </Button>
                    </span>
                  </TooltipTrigger>
                  {createDisabled && (
                    <TooltipContent>
                      <p>{t('pitchDetailPage.createTask.noProjectTooltip')}</p>
                    </TooltipContent>
                  )}
                </Tooltip>
              </TooltipProvider>
              <Link to="/backlog">
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
          ) : viewMode === 'list' ? (
            <PitchTaskList
              tasks={tasks}
              onStatusChange={handleStatusChange}
              onViewTask={handleViewTask}
              onEditTask={handleEditTask}
              onDeleteTask={handleDeleteTask}
              onAddSubtask={handleAddSubtask}
            />
          ) : (
            // Note: KanbanBoard's drag-and-drop is native HTML5 (draggable/dataTransfer), which
            // has no touch-device support without a polyfill — same limitation as the existing
            // project-level Kanban board on the Backlog page, not a new regression introduced
            // here. This replaces the previous @dnd-kit drag-to-reorder list (which did support
            // touch) per the move to a Kanban board (see §C.2).
            //
            // columnScrollClassName/edgeToEdgeMobileScroll override KanbanBoard's Backlog-page
            // defaults: this board is nested inside a Card partway down the pitch page rather
            // than being the page's own top-level content, so a viewport-relative column height
            // and the page-padding-matched mobile edge-bleed trick both misbehave here (see the
            // scroll feedback in the pitch-page task section).
            <KanbanBoard
              tasks={tasks}
              onStatusChange={handleStatusChange}
              onViewTask={handleViewTask}
              onEditTask={handleEditTask}
              onDeleteTask={handleDeleteTask}
              onAddSubtask={handleAddSubtask}
              hidePitchBadge
              loading={false}
              columnScrollClassName="h-[420px]"
              edgeToEdgeMobileScroll={false}
            />
          )}
        </CardContent>
      </Card>

      <Dialog open={dialogOpen} onOpenChange={(open) => { setDialogOpen(open); if (!open) resetForm(); }}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              {parentTask
                ? t('backlogPage.createSubtask')
                : t('pitchDetailPage.createTask.dialogTitle', 'Create Task for Pitch')}
            </DialogTitle>
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
                    {(['LOW', 'MEDIUM', 'HIGH', 'URGENT'] as TaskPriority[]).map(p => (
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
