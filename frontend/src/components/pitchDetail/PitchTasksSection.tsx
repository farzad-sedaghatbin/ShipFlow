import { useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { Plus, GripVertical } from 'lucide-react';
import { toast } from 'sonner';
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { Task, TaskStatus, TaskPriority } from '../../types';
import { taskService } from '../../services/taskService';
import { getUserFriendlyError } from '../../utils/errorMessages';
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
  /** Fired with the full, updated task list after a drag reorder (optimistic update / rollback). */
  onReorder?: (tasks: Task[]) => void;
}

const PRIORITY_RANK: Record<TaskPriority, number> = {
  URGENT: 0,
  HIGH: 1,
  MEDIUM: 2,
  LOW: 3,
};

/**
 * Sorts a sibling group for display. If nobody has ever manually reordered this
 * group (every task still shares the same sortOrder, i.e. the default 0), fall
 * back to priority order (URGENT > HIGH > MEDIUM > LOW). Once a drag assigns
 * distinct sortOrder values, plain numeric sort takes over from then on.
 */
function sortForDisplay(group: Task[]): Task[] {
  const hasCustomOrder = group.some((task) => (task.sortOrder ?? 0) !== (group[0]?.sortOrder ?? 0));
  if (!hasCustomOrder) {
    return [...group].sort((a, b) => (PRIORITY_RANK[a.priority] ?? 4) - (PRIORITY_RANK[b.priority] ?? 4));
  }
  return [...group].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
}

interface SortableTaskRowProps {
  task: Task;
  isSubtask: boolean;
  t: TFunction;
}

function SortableTaskRow({ task, isSubtask, t }: SortableTaskRowProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: task.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={isSubtask ? 'pl-6 border-l-2 border-border ml-2' : ''}
    >
      <div
        className={`flex items-center rounded-md border transition-colors ${
          isSubtask ? 'border-border/60 bg-muted/30' : 'border-border'
        } hover:bg-muted/50`}
      >
        <button
          {...attributes}
          {...listeners}
          type="button"
          className="cursor-grab active:cursor-grabbing text-muted-foreground hover:text-foreground touch-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded shrink-0 px-2 py-2"
          aria-label={t('backlogPage.dragToReorder')}
        >
          <GripVertical className="h-4 w-4" />
        </button>
        <Link
          to={`/backlog/${task.id}`}
          className="flex items-center justify-between py-2 pr-3 flex-1 min-w-0 cursor-pointer"
        >
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
        </Link>
      </div>
      {isSubtask && task.parentTaskTitle && (
        <Link
          to={`/backlog/${task.parentTaskId}`}
          className="text-[10px] text-muted-foreground hover:underline hover:text-foreground pl-3"
        >
          {t('pitchDetailPage.subtaskOf', { parentTitle: task.parentTaskTitle })}
        </Link>
      )}
    </div>
  );
}

interface SubtaskGroupProps {
  subtasks: Task[];
  onDragEndGroup: (activeId: number, overId: number) => void;
  t: TFunction;
}

/**
 * Each parent's subtasks get their own isolated DndContext/SortableContext so a
 * drag can never cross into the top-level list or another parent's subtasks —
 * only reordering among siblings under the same parent is possible.
 */
function SubtaskGroup({ subtasks, onDragEndGroup, t }: SubtaskGroupProps) {
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } })
  );

  const handleDragEnd = (e: DragEndEvent) => {
    if (!e.over || e.active.id === e.over.id) return;
    onDragEndGroup(Number(e.active.id), Number(e.over.id));
  };

  return (
    <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext items={subtasks.map((task) => task.id)} strategy={verticalListSortingStrategy}>
        <div className="space-y-2">
          {subtasks.map((task) => (
            <SortableTaskRow key={task.id} task={task} isSubtask t={t} />
          ))}
        </div>
      </SortableContext>
    </DndContext>
  );
}

export function PitchTasksSection({ tasks, pitchId, cycleId, onTaskCreated, onReorder }: PitchTasksSectionProps) {
  const { t } = useTranslation();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [title, setTitle] = useState('');
  const [status, setStatus] = useState<TaskStatus>('TODO');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } })
  );
  const reorderInFlight = useRef(false);

  // Top-level tasks in this pitch's list (parent not in this list is treated as
  // top-level here too, matching the previous flat-fallback behavior).
  const topLevelTasks = useMemo(() => {
    const idsInList = new Set(tasks.map((task) => task.id));
    const roots = tasks.filter((task) => task.parentTaskId == null || !idsInList.has(task.parentTaskId));
    return sortForDisplay(roots);
  }, [tasks]);

  const childrenByParent = useMemo(() => {
    const idsInList = new Set(tasks.map((task) => task.id));
    const map = new Map<number, Task[]>();
    for (const task of tasks) {
      if (task.parentTaskId != null && idsInList.has(task.parentTaskId)) {
        const siblings = map.get(task.parentTaskId) ?? [];
        siblings.push(task);
        map.set(task.parentTaskId, siblings);
      }
    }
    return map;
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

  // Reorders a single sibling group (top-level tasks, or one parent's subtasks),
  // merges the new sortOrder values back into the full tasks list, updates
  // optimistically, then persists via PATCH /api/tasks/reorder — reverting on
  // failure. Mirrors the drag-to-reorder pattern in BacklogTaskTable.tsx.
  const reorderGroup = async (group: Task[], activeId: number, overId: number) => {
    const fromIndex = group.findIndex((task) => task.id === activeId);
    const toIndex = group.findIndex((task) => task.id === overId);
    if (fromIndex === -1 || toIndex === -1 || fromIndex === toIndex) return;
    if (reorderInFlight.current) return;
    reorderInFlight.current = true;

    const reorderedGroup = arrayMove(group, fromIndex, toIndex);
    const items = reorderedGroup.map((task, idx) => ({ id: task.id, sortOrder: idx + 1 }));
    const sortOrderById = new Map(items.map((item) => [item.id, item.sortOrder]));

    const previousTasks = tasks;
    const updatedTasks = tasks.map((task) =>
      sortOrderById.has(task.id) ? { ...task, sortOrder: sortOrderById.get(task.id)! } : task
    );
    onReorder?.(updatedTasks);

    try {
      await taskService.reorderTasks(items);
      toast.success(t('tasks.reorderSuccess'));
    } catch (err) {
      toast.error(getUserFriendlyError(err, t('tasks.reorderError')));
      onReorder?.(previousTasks);
    } finally {
      reorderInFlight.current = false;
    }
  };

  const handleTopLevelDragEnd = (e: DragEndEvent) => {
    if (!e.over || e.active.id === e.over.id) return;
    reorderGroup(topLevelTasks, Number(e.active.id), Number(e.over.id));
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
            <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleTopLevelDragEnd}>
              <SortableContext items={topLevelTasks.map((task) => task.id)} strategy={verticalListSortingStrategy}>
                <div className="space-y-2">
                  {topLevelTasks.map((task) => {
                    const children = childrenByParent.get(task.id);
                    return (
                      <div key={task.id} className="space-y-2">
                        <SortableTaskRow task={task} isSubtask={false} t={t} />
                        {children && children.length > 0 && (
                          <SubtaskGroup
                            subtasks={sortForDisplay(children)}
                            onDragEndGroup={(activeId, overId) =>
                              reorderGroup(sortForDisplay(children), activeId, overId)
                            }
                            t={t}
                          />
                        )}
                      </div>
                    );
                  })}
                </div>
              </SortableContext>
            </DndContext>
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
