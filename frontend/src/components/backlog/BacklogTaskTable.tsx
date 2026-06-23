import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useState, useRef } from 'react';
import dayjs from 'dayjs';
import {
  Plus,
  Trash2,
  Pencil,
  ArrowUp,
  ArrowDown,
  ChevronLeft,
  ChevronRight,
  Check,
  PlayCircle,
  Eye,
  AlertCircle,
  Shield,
  List,
  Loader2,
  GripVertical,
} from 'lucide-react';
import {
  DndContext,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
  DragStartEvent,
  DragOverEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { toast } from 'sonner';
import { cn } from '../../lib/utils';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { Checkbox } from '@/components/ui/checkbox';
import { Task, TaskStatus, TaskPriority, Person } from '../../types';
import { taskService } from '../../services/taskService';
import { getUserFriendlyError } from '../../utils/errorMessages';
import EmptyState from '../EmptyState';
import { EmptyTasksIllustration } from '../illustrations';
import BulkActionBar from '../BulkActionBar';
import { statusOptions, priorityOptions, getStatusBadgeVariant, getPriorityBadgeVariant } from './backlogTypes';

// ── SortableTaskRow ───────────────────────────────────────────────────────────

interface SortableTaskRowProps {
  task: Task;
  isBlocked: boolean;
  selectedTaskIds: Set<number>;
  activeTimerTaskId: number | null;
  onSelectedTaskIdsChange?: (ids: Set<number>) => void;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubTask: (task: Task) => void;
  onStartTimer: (task: Task) => void;
  onQuickStatusChange: (taskId: number, status: TaskStatus) => void;
  onQuickPriorityChange: (taskId: number, priority: TaskPriority) => void;
  onQuickAssigneeChange?: (taskId: number, assigneeId: number | null) => void;
  persons?: Person[];
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  t: (key: string, options?: any) => string;
}

function SortableTaskRow({
  task,
  isBlocked,
  selectedTaskIds,
  activeTimerTaskId,
  onSelectedTaskIdsChange,
  onViewTask,
  onEditTask,
  onDeleteTask,
  onAddSubTask,
  onStartTimer,
  onQuickStatusChange,
  onQuickPriorityChange,
  onQuickAssigneeChange,
  persons = [],
  t,
}: SortableTaskRowProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: task.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const toggleSingleTask = (id: number, checked: boolean) => {
    if (!onSelectedTaskIdsChange) return;
    const next = new Set(selectedTaskIds);
    if (checked) next.add(id); else next.delete(id);
    onSelectedTaskIdsChange(next);
  };

  return (
    <TableRow
      ref={setNodeRef}
      style={style}
      className={cn(
        task.parentTaskId ? 'bg-muted/30' : '',
        selectedTaskIds.has(task.id) ? 'bg-primary/5' : '',
        isBlocked ? 'bg-destructive/5 border-destructive/30' : '',
      )}
    >
      {/* Drag handle */}
      <TableCell className="w-6 px-1">
        <button
          {...attributes}
          {...listeners}
          className="cursor-grab active:cursor-grabbing text-muted-foreground hover:text-foreground touch-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded"
          aria-label={t('backlogPage.dragToReorder')}
        >
          <GripVertical className="h-4 w-4" />
        </button>
      </TableCell>

      {onSelectedTaskIdsChange && (
        <TableCell className="w-8">
          <Checkbox
            checked={selectedTaskIds.has(task.id)}
            onCheckedChange={(checked) => toggleSingleTask(task.id, !!checked)}
            onClick={(e) => e.stopPropagation()}
            aria-label={`${t('bulkActions.selectTask')} ${task.title}`}
          />
        </TableCell>
      )}

      <TableCell>
        <div className={task.parentTaskId ? 'pl-6' : ''}>
          <div className="font-medium flex items-center gap-2">
            {task.parentTaskId && (
              <span className="text-muted-foreground text-xs">└─</span>
            )}
            <Link
              to={`/backlog/${task.id}`}
              className="hover:underline cursor-pointer text-primary flex items-center gap-1.5"
            >
              <span className="text-muted-foreground font-mono text-xs font-normal shrink-0">
                {task.projectKey ? `${task.projectKey}-${task.id}` : `#${task.id}`}
              </span>
              {task.title}
            </Link>
            {task.isBlocked && task.blockedByCount && task.blockedByCount > 0 && (
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Badge variant="destructive" className="h-5 px-1.5">
                      <AlertCircle className="h-3 w-3 mr-1" />
                      {task.blockedByCount}
                    </Badge>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="font-semibold mb-1">{t('backlogPage.blockedByCount', { count: task.blockedByCount })}:</p>
                    <ul className="text-sm space-y-0.5">
                      {task.blockedByTasks?.slice(0, 3).map((blocker, idx) => (
                        <li key={idx}>• {blocker.sourceTaskTitle}</li>
                      ))}
                      {task.blockedByTasks && task.blockedByTasks.length > 3 && (
                        <li className="text-muted-foreground">{t('backlogPage.andMore', { count: task.blockedByTasks.length - 3 })}</li>
                      )}
                    </ul>
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            )}
            {task.blockingTasks && task.blockingTasks.length > 0 && (
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Badge variant="secondary" className="h-5 px-1.5">
                      <Shield className="h-3 w-3 mr-1" />
                      {task.blockingTasks.length}
                    </Badge>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="font-semibold mb-1">{t('backlogPage.blockingCount', { count: task.blockingTasks.length })}:</p>
                    <ul className="text-sm space-y-0.5">
                      {task.blockingTasks?.slice(0, 3).map((blocking, idx) => (
                        <li key={idx}>• {blocking.targetTaskTitle}</li>
                      ))}
                      {task.blockingTasks && task.blockingTasks.length > 3 && (
                        <li className="text-muted-foreground">{t('backlogPage.andMore', { count: task.blockingTasks.length - 3 })}</li>
                      )}
                    </ul>
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            )}
            {!task.parentTaskId && task.children && task.children.length > 0 && (
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Badge variant="outline" className="h-5 px-1.5">
                      <List className="h-3 w-3 mr-1" />
                      {task.children.length}
                    </Badge>
                  </TooltipTrigger>
                  <TooltipContent className="max-w-xs">
                    <p className="font-semibold mb-1">{t('backlogPage.subtaskCount', { count: task.children.length })}:</p>
                    <ul className="text-sm space-y-0.5">
                      {task.children.slice(0, 3).map((child, idx) => (
                        <li key={idx}>• {child.title}</li>
                      ))}
                      {task.children.length > 3 && (
                        <li className="text-muted-foreground">{t('backlogPage.andMore', { count: task.children.length - 3 })}</li>
                      )}
                    </ul>
                  </TooltipContent>
                </Tooltip>
              </TooltipProvider>
            )}
          </div>
          {task.description && (
            <div className="text-sm text-muted-foreground line-clamp-1">
              {task.description}
            </div>
          )}
        </div>
      </TableCell>

      <TableCell>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="h-auto p-0">
              <Badge variant={getStatusBadgeVariant(task.status)}>
                {t(statusOptions.find(s => s.value === task.status)?.labelKey || 'backlogPage.statusOptions.backlog')}
              </Badge>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            {statusOptions.map((status) => (
              <DropdownMenuItem
                key={status.value}
                onClick={() => onQuickStatusChange(task.id, status.value)}
              >
                <Badge variant={status.variant} className="mr-2">
                  {t(status.labelKey)}
                </Badge>
                {task.status === status.value && <Check className="ml-auto h-4 w-4" />}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </TableCell>

      <TableCell>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="h-auto p-0">
              <Badge variant={getPriorityBadgeVariant(task.priority)}>
                {t(priorityOptions.find(p => p.value === task.priority)?.labelKey || 'backlogPage.priorityOptions.medium')}
              </Badge>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start">
            {priorityOptions.map((priority) => (
              <DropdownMenuItem
                key={priority.value}
                onClick={() => onQuickPriorityChange(task.id, priority.value)}
              >
                <Badge variant={priority.variant} className="mr-2">
                  {t(priority.labelKey)}
                </Badge>
                {task.priority === priority.value && <Check className="ml-auto h-4 w-4" />}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </TableCell>

      <TableCell className="text-center">
        {task.storyPoints != null ? (
          <Badge variant="outline" className="text-xs font-mono">
            {task.storyPoints}
          </Badge>
        ) : (
          <span className="text-muted-foreground">-</span>
        )}
      </TableCell>

      <TableCell>
        {onQuickAssigneeChange ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-auto p-1 -ml-1" title={t('backlogPage.changeAssignee')}>
                {task.assigneeName ? (
                  <div className="flex items-center gap-2">
                    <Avatar className="h-6 w-6">
                      {task.assigneeAvatarUrl ? (
                        <AvatarImage src={task.assigneeAvatarUrl} />
                      ) : (
                        <AvatarFallback className="text-xs">
                          {task.assigneeName.charAt(0)}
                        </AvatarFallback>
                      )}
                    </Avatar>
                    <span className="text-sm">{task.assigneeName}</span>
                  </div>
                ) : (
                  <span className="text-muted-foreground text-sm">{t('backlogPage.unassigned')}</span>
                )}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="max-h-72 overflow-y-auto">
              <DropdownMenuItem onClick={() => onQuickAssigneeChange(task.id, null)}>
                <span className="text-muted-foreground">{t('backlogPage.unassigned')}</span>
                {task.assigneeId == null && <Check className="ml-auto h-4 w-4" />}
              </DropdownMenuItem>
              {persons.map((person) => (
                <DropdownMenuItem
                  key={person.id}
                  onClick={() => onQuickAssigneeChange(task.id, person.id)}
                >
                  <Avatar className="h-5 w-5 mr-2">
                    {person.avatarUrl ? (
                      <AvatarImage src={person.avatarUrl} />
                    ) : (
                      <AvatarFallback className="text-[0.6rem]">{person.name.charAt(0)}</AvatarFallback>
                    )}
                  </Avatar>
                  {person.name}
                  {task.assigneeId === person.id && <Check className="ml-auto h-4 w-4" />}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        ) : task.assigneeName ? (
          <div className="flex items-center gap-2">
            <Avatar className="h-6 w-6">
              {task.assigneeAvatarUrl ? (
                <AvatarImage src={task.assigneeAvatarUrl} />
              ) : (
                <AvatarFallback className="text-xs">
                  {task.assigneeName.charAt(0)}
                </AvatarFallback>
              )}
            </Avatar>
            <span className="text-sm">{task.assigneeName}</span>
          </div>
        ) : (
          <span className="text-muted-foreground">{t('backlogPage.unassigned')}</span>
        )}
      </TableCell>

      <TableCell>
        {task.dueDate ? (
          <span className={cn(
            'text-sm',
            dayjs(task.dueDate).isBefore(dayjs(), 'day') && task.status !== 'DONE' && 'text-destructive'
          )}>
            {dayjs(task.dueDate).format('MMM D, YYYY')}
          </span>
        ) : (
          <span className="text-muted-foreground">-</span>
        )}
      </TableCell>

      <TableCell className="text-right">
        <div className="flex justify-end gap-1">
          {!task.parentTaskId && (
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => onAddSubTask(task)}
                    aria-label={`${t('backlogPage.addSubTask')}: ${task.title}`}
                    className="text-xs"
                  >
                    <Plus className="h-3 w-3 mr-1" aria-hidden="true" />
                    {t('backlogPage.subTask')}
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('backlogPage.addSubTask')}</TooltipContent>
              </Tooltip>
            </TooltipProvider>
          )}
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant={activeTimerTaskId === task.id ? 'destructive' : 'default'}
                  size="sm"
                  onClick={() => onStartTimer(task)}
                  disabled={activeTimerTaskId !== null && activeTimerTaskId !== task.id}
                  aria-label={`${t('backlogPage.startTimer')}: ${task.title}`}
                  className={activeTimerTaskId === task.id ? 'text-xs' : 'text-xs bg-green-600 hover:bg-green-700'}
                >
                  <PlayCircle className="h-3 w-3 mr-1" aria-hidden="true" />
                  {activeTimerTaskId === task.id ? t('backlogPage.running') : t('backlogPage.timer')}
                </Button>
              </TooltipTrigger>
              <TooltipContent>
                {activeTimerTaskId === task.id
                  ? t('backlogPage.timerRunning')
                  : activeTimerTaskId
                    ? t('backlogPage.stopTimerFirst')
                    : t('backlogPage.startTimer')
                }
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => onViewTask(task)}
                  aria-label={t('aria.viewTask')}
                >
                  <Eye className="h-4 w-4" aria-hidden="true" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('backlogPage.viewDetails')}</TooltipContent>
            </Tooltip>
          </TooltipProvider>
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => onEditTask(task)}
                  aria-label={t('aria.editTask')}
                >
                  <Pencil className="h-4 w-4" aria-hidden="true" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('backlogPage.edit')}</TooltipContent>
            </Tooltip>
          </TooltipProvider>
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => onDeleteTask(task.id)}
                  aria-label={t('aria.deleteTask')}
                >
                  <Trash2 className="h-4 w-4 text-destructive" aria-hidden="true" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('backlogPage.delete')}</TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>
      </TableCell>
    </TableRow>
  );
}

// ── BacklogTaskTable ──────────────────────────────────────────────────────────

export interface BacklogTaskTableProps {
  tasks: Task[];
  totalElements: number;
  tasksLoading: boolean;
  selectedCycle: number | 'all';
  page: number;
  rowsPerPage: number;
  sortBy: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title';
  sortOrder: 'asc' | 'desc';
  activeTimerTaskId: number | null;
  categoryTitle: string;
  persons?: Person[];
  selectedTaskIds?: Set<number>;
  onSelectedTaskIdsChange?: (ids: Set<number>) => void;
  onBulkSuccess?: () => void;
  onSort: (field: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title') => void;
  onChangePage: (page: number) => void;
  onChangeRowsPerPage: (value: string) => void;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubTask: (task: Task) => void;
  onStartTimer: (task: Task) => void;
  onQuickStatusChange: (taskId: number, status: TaskStatus) => void;
  onQuickPriorityChange: (taskId: number, priority: TaskPriority) => void;
  onQuickAssigneeChange?: (taskId: number, assigneeId: number | null) => void;
  onOpenDialog: () => void;
  onReorder?: (tasks: Task[]) => void;
}

export function BacklogTaskTable({
  tasks,
  totalElements,
  tasksLoading,
  selectedCycle,
  page,
  rowsPerPage,
  sortBy,
  sortOrder,
  activeTimerTaskId,
  categoryTitle,
  persons = [],
  selectedTaskIds = new Set(),
  onSelectedTaskIdsChange,
  onBulkSuccess,
  onSort,
  onChangePage,
  onChangeRowsPerPage,
  onViewTask,
  onEditTask,
  onDeleteTask,
  onAddSubTask,
  onStartTimer,
  onQuickStatusChange,
  onQuickPriorityChange,
  onQuickAssigneeChange,
  onOpenDialog,
  onReorder,
}: BacklogTaskTableProps) {
  const { t } = useTranslation();
  const totalPages = Math.ceil(totalElements / rowsPerPage);

  // ── Drag-to-reorder state ─────────────────────────────────────────────────────
  const [activeTaskId, setActiveTaskId] = useState<number | null>(null);
  const [blockedDropIds, setBlockedDropIds] = useState<Set<number>>(new Set());

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } })
  );

  /**
   * Returns a violation message if moving from fromIndex to toIndex would break
   * a BLOCKS or DEPENDS_ON dependency constraint, null otherwise.
   */
  const getViolatedDependency = (items: Task[], fromIndex: number, toIndex: number): string | null => {
    if (fromIndex === toIndex) return null;
    const reordered = arrayMove(items, fromIndex, toIndex);
    const positionOf = (id: number) => reordered.findIndex((t) => t.id === id);

    for (const task of reordered) {
      // BLOCKS: task blocks others — task must come before each target
      if (task.blockingTasks) {
        for (const dep of task.blockingTasks) {
          const targetId = dep.targetTaskId;
          if (targetId == null) continue;
          const blockerPos = positionOf(task.id);
          const blockedPos = positionOf(targetId);
          if (blockerPos === -1 || blockedPos === -1) continue;
          if (blockerPos >= blockedPos) {
            return t('tasks.reorderBlocked');
          }
        }
      }
      // blockedByTasks: this task is blocked by others — blocker must come before task
      if (task.blockedByTasks) {
        for (const dep of task.blockedByTasks) {
          const sourceId = dep.sourceTaskId;
          if (sourceId == null) continue;
          const blockerPos = positionOf(sourceId);
          const blockedPos = positionOf(task.id);
          if (blockerPos === -1 || blockedPos === -1) continue;
          if (blockerPos >= blockedPos) {
            return t('tasks.reorderBlocked');
          }
        }
      }
    }
    return null;
  };

  const handleDragStart = (e: DragStartEvent) => {
    setActiveTaskId(Number(e.active.id));
  };

  const handleDragOver = (e: DragOverEvent) => {
    if (!e.over || !activeTaskId) return;
    const fromIndex = tasks.findIndex((t) => t.id === activeTaskId);
    const toIndex = tasks.findIndex((t) => t.id === Number(e.over!.id));
    const violation = getViolatedDependency(tasks, fromIndex, toIndex);
    if (violation) {
      setBlockedDropIds(new Set([Number(e.over.id)]));
    } else {
      setBlockedDropIds(new Set());
    }
  };

  const reorderInFlight = useRef(false);

  const handleDragEnd = async (e: DragEndEvent) => {
    setActiveTaskId(null);
    setBlockedDropIds(new Set());
    if (!e.over || e.active.id === e.over.id) return;

    const fromIndex = tasks.findIndex((t) => t.id === Number(e.active.id));
    const toIndex = tasks.findIndex((t) => t.id === Number(e.over!.id));
    if (fromIndex === -1 || toIndex === -1) return;

    const violation = getViolatedDependency(tasks, fromIndex, toIndex);
    if (violation) {
      toast.error(violation);
      return;
    }

    // Prevent concurrent reorder requests
    if (reorderInFlight.current) return;
    reorderInFlight.current = true;

    const reordered = arrayMove(tasks, fromIndex, toIndex);
    onReorder?.(reordered);

    try {
      await taskService.reorderTasks(
        reordered.map((task, idx) => ({ id: task.id, sortOrder: idx + 1 }))
      );
      toast.success(t('tasks.reorderSuccess'));
    } catch (err) {
      toast.error(getUserFriendlyError(err, t('tasks.reorderError')));
      onReorder?.(tasks); // revert optimistic update
    } finally {
      reorderInFlight.current = false;
    }
  };

  const toggleAll = (checked: boolean) => {
    if (!onSelectedTaskIdsChange) return;
    onSelectedTaskIdsChange(checked ? new Set(tasks.map(t => t.id)) : new Set());
  };

  if (!selectedCycle) {
    return (
      <EmptyState
        title="Select a Cycle"
        description="Choose a cycle to view and manage tasks"
        illustration={<EmptyTasksIllustration />}
      />
    );
  }

  if (tasksLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!tasks || tasks.length === 0) {
    return (
      <EmptyState
        title={`No ${categoryTitle}`}
        description="No tasks found in this category. Create your first task to get started."
        illustration={<EmptyTasksIllustration />}
        action={{
          label: 'Create Task',
          onClick: onOpenDialog,
        }}
      />
    );
  }

  return (
    <div className="space-y-4">
      <Card>
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragStart={handleDragStart}
          onDragOver={handleDragOver}
          onDragEnd={handleDragEnd}
        >
          <SortableContext items={tasks.map((t) => t.id)} strategy={verticalListSortingStrategy}>
            <Table>
              <TableHeader>
                <TableRow>
                  {/* Drag handle column header */}
                  <TableHead className="w-6 px-1" />
                  {onSelectedTaskIdsChange && (
                    <TableHead className="w-8">
                      <Checkbox
                        checked={tasks.length > 0 && tasks.every(t => selectedTaskIds.has(t.id))}
                        onCheckedChange={(checked) => toggleAll(!!checked)}
                        aria-label={t('bulkActions.selectAll')}
                      />
                    </TableHead>
                  )}
                  <TableHead className="w-[40%]">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onSort('title')}
                      className="-ml-3"
                    >
                      {t('backlogPage.title')}
                      {sortBy === 'title' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                    </Button>
                  </TableHead>
                  <TableHead>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onSort('status')}
                      className="-ml-3"
                    >
                      {t('common.status')}
                      {sortBy === 'status' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                    </Button>
                  </TableHead>
                  <TableHead>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onSort('priority')}
                      className="-ml-3"
                    >
                      {t('backlogPage.filters.priority')}
                      {sortBy === 'priority' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                    </Button>
                  </TableHead>
                  <TableHead className="w-12 text-center">{t('backlogPage.storyPointsAbbr')}</TableHead>
                  <TableHead>{t('backlogPage.assignee')}</TableHead>
                  <TableHead>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onSort('dueDate')}
                      className="-ml-3"
                    >
                      {t('backlogPage.dueDate')}
                      {sortBy === 'dueDate' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                    </Button>
                  </TableHead>
                  <TableHead className="text-right">{t('common.actions')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {tasks.map((task) => (
                  <SortableTaskRow
                    key={task.id}
                    task={task}
                    isBlocked={blockedDropIds.has(task.id)}
                    selectedTaskIds={selectedTaskIds}
                    activeTimerTaskId={activeTimerTaskId}
                    onSelectedTaskIdsChange={onSelectedTaskIdsChange}
                    onViewTask={onViewTask}
                    onEditTask={onEditTask}
                    onDeleteTask={onDeleteTask}
                    onAddSubTask={onAddSubTask}
                    onStartTimer={onStartTimer}
                    onQuickStatusChange={onQuickStatusChange}
                    onQuickPriorityChange={onQuickPriorityChange}
                    onQuickAssigneeChange={onQuickAssigneeChange}
                    persons={persons}
                    t={t}
                  />
                ))}
              </TableBody>
            </Table>
          </SortableContext>
        </DndContext>
      </Card>

      {/* Bulk action bar */}
      {onSelectedTaskIdsChange && (
        <BulkActionBar
          selectedIds={selectedTaskIds}
          persons={persons}
          onClear={() => onSelectedTaskIdsChange(new Set())}
          onSuccess={() => {
            onSelectedTaskIdsChange(new Set());
            onBulkSuccess?.();
          }}
        />
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="text-sm text-muted-foreground">
            Showing {page * rowsPerPage + 1} to {Math.min((page + 1) * rowsPerPage, totalElements)} of {totalElements} tasks
          </div>
          <div className="flex items-center gap-2">
            <Select value={rowsPerPage.toString()} onValueChange={onChangeRowsPerPage}>
              <SelectTrigger className="w-20">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="10">10</SelectItem>
                <SelectItem value="25">25</SelectItem>
                <SelectItem value="50">50</SelectItem>
              </SelectContent>
            </Select>
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="icon"
                onClick={() => onChangePage(page - 1)}
                disabled={page === 0}
                aria-label={t('aria.previousPage')}
              >
                <ChevronLeft className="h-4 w-4" aria-hidden="true" />
              </Button>
              <span className="text-sm px-2">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="outline"
                size="icon"
                onClick={() => onChangePage(page + 1)}
                disabled={page >= totalPages - 1}
                aria-label={t('aria.nextPage')}
              >
                <ChevronRight className="h-4 w-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
