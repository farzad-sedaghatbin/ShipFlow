import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ChevronRight,
  ChevronDown,
  Eye,
  Pencil,
  Plus,
  Trash2,
  MoreVertical,
  Check,
  AlertCircle,
  Shield,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { Task, TaskStatus } from '../../types';
import { statusOptions, getStatusBadgeVariant, getPriorityBadgeVariant } from '../backlog/backlogTypes';

const priorityDotColors: Record<Task['priority'], string> = {
  LOW: 'bg-slate-500',
  MEDIUM: 'bg-blue-500',
  HIGH: 'bg-orange-500',
  URGENT: 'bg-red-600',
};

interface PitchTaskListProps {
  tasks: Task[];
  onStatusChange: (taskId: number, newStatus: TaskStatus) => Promise<void>;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubtask: (task: Task) => void;
}

interface TaskRowProps {
  task: Task;
  subtasks: Task[];
  isSubtask: boolean;
  expanded: boolean;
  onToggleExpand: () => void;
  onStatusChange: (taskId: number, newStatus: TaskStatus) => Promise<void>;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubtask: (task: Task) => void;
}

function TaskRow({
  task,
  subtasks,
  isSubtask,
  expanded,
  onToggleExpand,
  onStatusChange,
  onViewTask,
  onEditTask,
  onDeleteTask,
  onAddSubtask,
}: TaskRowProps) {
  const { t } = useTranslation();
  const hasSubtasks = subtasks.length > 0;
  const isBlocked = task.blockedByCount && task.blockedByCount > 0;
  const isBlocking = task.blockingTasks && task.blockingTasks.length > 0;

  return (
    <div
      className={cn(
        'flex items-center gap-2 py-2 px-2',
        isSubtask && 'ps-8 bg-muted/20',
      )}
    >
      {hasSubtasks ? (
        <Button
          variant="ghost"
          size="icon"
          className="h-5 w-5 shrink-0"
          onClick={onToggleExpand}
          aria-label={expanded ? t('common.showLess') : t('common.showMore')}
        >
          {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
        </Button>
      ) : (
        <span className="w-5 shrink-0" />
      )}

      <div className={cn('w-2 h-2 rounded-full shrink-0', priorityDotColors[task.priority])} />

      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-1.5">
          <span
            className="font-medium text-sm truncate cursor-pointer hover:text-primary"
            onClick={() => onViewTask(task)}
            title={task.title}
          >
            {task.title}
          </span>
          {isBlocked && (
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Badge variant="destructive" className="h-5 px-1.5 shrink-0">
                    <AlertCircle className="h-3 w-3" />
                  </Badge>
                </TooltipTrigger>
                <TooltipContent>{t('backlogPage.blockedBy', { count: task.blockedByCount })}</TooltipContent>
              </Tooltip>
            </TooltipProvider>
          )}
          {isBlocking && (
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Badge variant="secondary" className="h-5 px-1.5 shrink-0">
                    <Shield className="h-3 w-3" />
                  </Badge>
                </TooltipTrigger>
                <TooltipContent>{t('backlogPage.blocking', { count: task.blockingTasks?.length || 0 })}</TooltipContent>
              </Tooltip>
            </TooltipProvider>
          )}
        </div>
      </div>

      {hasSubtasks && (
        <Badge variant="outline" className="text-[10px] shrink-0 hidden sm:inline-flex">
          {t('backlogPage.subtaskCount', { count: subtasks.length })}
        </Badge>
      )}

      <Badge variant={getPriorityBadgeVariant(task.priority)} className="text-[10px] shrink-0 hidden sm:inline-flex">
        {task.priority}
      </Badge>

      {task.assigneeName && (
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <Avatar className="h-5 w-5 shrink-0">
                {task.assigneeAvatarUrl ? (
                  <AvatarImage src={task.assigneeAvatarUrl} />
                ) : (
                  <AvatarFallback className="text-[10px]">{task.assigneeName.charAt(0).toUpperCase()}</AvatarFallback>
                )}
              </Avatar>
            </TooltipTrigger>
            <TooltipContent>{task.assigneeName}</TooltipContent>
          </Tooltip>
        </TooltipProvider>
      )}

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="sm" className="h-auto p-0 shrink-0">
            <Badge variant={getStatusBadgeVariant(task.status)} className="cursor-pointer">
              {t(statusOptions.find(s => s.value === task.status)?.labelKey || 'backlogPage.statusOptions.backlog')}
            </Badge>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {statusOptions.map((s) => (
            <DropdownMenuItem key={s.value} onClick={() => onStatusChange(task.id, s.value)}>
              <Badge variant={s.variant} className="mr-2">{t(s.labelKey)}</Badge>
              {task.status === s.value && <Check className="ms-auto h-4 w-4" />}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" className="h-7 w-7 shrink-0">
            <MoreVertical className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={() => onViewTask(task)}>
            <Eye className="h-4 w-4 mr-2" />
            {t('common.view')}
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => onEditTask(task)}>
            <Pencil className="h-4 w-4 mr-2" />
            {t('common.edit')}
          </DropdownMenuItem>
          {!isSubtask && (
            <DropdownMenuItem onClick={() => onAddSubtask(task)}>
              <Plus className="h-4 w-4 mr-2" />
              {t('backlogPage.addSubTask')}
            </DropdownMenuItem>
          )}
          <DropdownMenuItem onClick={() => onDeleteTask(task.id)} className="text-destructive">
            <Trash2 className="h-4 w-4 mr-2" />
            {t('common.delete')}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
}

/**
 * Compact list view for a pitch's tasks — the counterpart to KanbanBoard's board view. Only
 * top-level tasks render as primary rows; sub-tasks are collapsed into an expandable group under
 * their parent instead of appearing as their own rows, which is what makes the "main" tasks easy
 * to scan (the Kanban board scatters a task's sub-tasks across whichever status column each one
 * happens to be in, with no way to tell them apart from top-level work at a glance).
 */
export function PitchTaskList({
  tasks,
  onStatusChange,
  onViewTask,
  onEditTask,
  onDeleteTask,
  onAddSubtask,
}: PitchTaskListProps) {
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());

  const taskIds = new Set(tasks.map(t => t.id));
  // A sub-task whose parent isn't in this task list (edge case) is shown as top-level too,
  // since there's nowhere else to nest it.
  const topLevelTasks = tasks.filter(t => !t.parentTaskId || !taskIds.has(t.parentTaskId));
  const subtasksByParent = new Map<number, Task[]>();
  for (const task of tasks) {
    if (task.parentTaskId && taskIds.has(task.parentTaskId)) {
      const list = subtasksByParent.get(task.parentTaskId) ?? [];
      list.push(task);
      subtasksByParent.set(task.parentTaskId, list);
    }
  }

  const toggleExpand = (taskId: number) => {
    setExpandedIds(prev => {
      const next = new Set(prev);
      if (next.has(taskId)) next.delete(taskId); else next.add(taskId);
      return next;
    });
  };

  return (
    <div className="border rounded-lg divide-y max-h-[500px] overflow-y-auto">
      {topLevelTasks.map(task => {
        const subtasks = subtasksByParent.get(task.id) ?? [];
        const expanded = expandedIds.has(task.id);
        return (
          <div key={task.id}>
            <TaskRow
              task={task}
              subtasks={subtasks}
              isSubtask={false}
              expanded={expanded}
              onToggleExpand={() => toggleExpand(task.id)}
              onStatusChange={onStatusChange}
              onViewTask={onViewTask}
              onEditTask={onEditTask}
              onDeleteTask={onDeleteTask}
              onAddSubtask={onAddSubtask}
            />
            {expanded && subtasks.map(subtask => (
              <TaskRow
                key={subtask.id}
                task={subtask}
                subtasks={[]}
                isSubtask
                expanded={false}
                onToggleExpand={() => {}}
                onStatusChange={onStatusChange}
                onViewTask={onViewTask}
                onEditTask={onEditTask}
                onDeleteTask={onDeleteTask}
                onAddSubtask={onAddSubtask}
              />
            ))}
          </div>
        );
      })}
    </div>
  );
}
