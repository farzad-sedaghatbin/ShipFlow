import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import dayjs from 'dayjs';
import { 
  ArrowUp, 
  ArrowDown, 
  Loader2, 
  Plus, 
  PlayCircle, 
  Eye, 
  Pencil, 
  Trash2, 
  Check,
  AlertCircle,
  Shield,
  List,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { Button } from '../ui/button';
import { Card } from '../ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../ui/table';
import { Badge } from '../ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../ui/avatar';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '../ui/tooltip';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';
import EmptyState from '../EmptyState';
import { EmptyTasksIllustration } from '../illustrations';
import { Task, TaskStatus, TaskPriority } from '../../types';
import { STATUS_OPTIONS, PRIORITY_OPTIONS, getStatusBadgeVariant, getPriorityBadgeVariant } from '../../constants/backlogConstants';
import { cn } from '../../lib/utils';

export interface BacklogTaskTableProps {
  tasks: Task[];
  isLoading: boolean;
  categoryTitle: string;
  totalElements: number;
  page: number;
  rowsPerPage: number;
  sortBy: string;
  sortOrder: 'asc' | 'desc';
  activeTimerTaskId: number | null;
  onSort: (field: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title') => void;
  onPageChange: (page: number) => void;
  onRowsPerPageChange: (size: number) => void;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubtask: (task: Task) => void;
  onStartTimer: (task: Task) => void;
  onStatusChange: (taskId: number, status: TaskStatus) => void;
  onPriorityChange: (taskId: number, priority: TaskPriority) => void;
  onCreateTask: () => void;
}

export function BacklogTaskTable({
  tasks,
  isLoading,
  categoryTitle,
  totalElements,
  page,
  rowsPerPage,
  sortBy,
  sortOrder,
  activeTimerTaskId,
  onSort,
  onPageChange,
  onRowsPerPageChange,
  onViewTask,
  onEditTask,
  onDeleteTask,
  onAddSubtask,
  onStartTimer,
  onStatusChange,
  onPriorityChange,
  onCreateTask,
}: BacklogTaskTableProps) {
  const { t } = useTranslation();
  const totalPages = Math.ceil(totalElements / rowsPerPage);

  if (isLoading) {
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
        description={`No tasks found in this category. Create your first task to get started.`}
        illustration={<EmptyTasksIllustration />}
        action={{
          label: 'Create Task',
          onClick: onCreateTask,
        }}
      />
    );
  }

  const SortButton = ({ field, label }: { field: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title'; label: string }) => (
    <Button
      variant="ghost"
      size="sm"
      onClick={() => onSort(field)}
      className="-ml-3"
    >
      {label}
      {sortBy === field && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
    </Button>
  );

  return (
    <div className="space-y-4">
      <Card>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[40%]">
                <SortButton field="title" label={t('backlogPage.title')} />
              </TableHead>
              <TableHead>
                <SortButton field="status" label={t('common.status')} />
              </TableHead>
              <TableHead>
                <SortButton field="priority" label={t('backlogPage.filters.priority')} />
              </TableHead>
              <TableHead>{t('backlogPage.assignee')}</TableHead>
              <TableHead>
                <SortButton field="dueDate" label={t('backlogPage.dueDate')} />
              </TableHead>
              <TableHead className="text-right">{t('common.actions')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {tasks.map((task) => (
              <TaskRow
                key={task.id}
                task={task}
                activeTimerTaskId={activeTimerTaskId}
                onViewTask={onViewTask}
                onEditTask={onEditTask}
                onDeleteTask={onDeleteTask}
                onAddSubtask={onAddSubtask}
                onStartTimer={onStartTimer}
                onStatusChange={onStatusChange}
                onPriorityChange={onPriorityChange}
              />
            ))}
          </TableBody>
        </Table>
      </Card>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="text-sm text-muted-foreground">
            Showing {page * rowsPerPage + 1} to {Math.min((page + 1) * rowsPerPage, totalElements)} of {totalElements} tasks
          </div>
          <div className="flex items-center gap-2">
            <Select value={rowsPerPage.toString()} onValueChange={(v) => onRowsPerPageChange(parseInt(v, 10))}>
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
                onClick={() => onPageChange(page - 1)}
                disabled={page === 0}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="text-sm px-2">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="outline"
                size="icon"
                onClick={() => onPageChange(page + 1)}
                disabled={page >= totalPages - 1}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

interface TaskRowProps {
  task: Task;
  activeTimerTaskId: number | null;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubtask: (task: Task) => void;
  onStartTimer: (task: Task) => void;
  onStatusChange: (taskId: number, status: TaskStatus) => void;
  onPriorityChange: (taskId: number, priority: TaskPriority) => void;
}

function TaskRow({
  task,
  activeTimerTaskId,
  onViewTask,
  onEditTask,
  onDeleteTask,
  onAddSubtask,
  onStartTimer,
  onStatusChange,
  onPriorityChange,
}: TaskRowProps) {
  return (
    <TableRow className={task.parentTaskId ? 'bg-muted/30' : ''}>
      <TableCell>
        <TaskTitleCell task={task} />
      </TableCell>
      <TableCell>
        <StatusDropdown task={task} onStatusChange={onStatusChange} />
      </TableCell>
      <TableCell>
        <PriorityDropdown task={task} onPriorityChange={onPriorityChange} />
      </TableCell>
      <TableCell>
        <AssigneeCell task={task} />
      </TableCell>
      <TableCell>
        <DueDateCell task={task} />
      </TableCell>
      <TableCell className="text-right">
        <TaskActions
          task={task}
          activeTimerTaskId={activeTimerTaskId}
          onViewTask={onViewTask}
          onEditTask={onEditTask}
          onDeleteTask={onDeleteTask}
          onAddSubtask={onAddSubtask}
          onStartTimer={onStartTimer}
        />
      </TableCell>
    </TableRow>
  );
}

function TaskTitleCell({ task }: { task: Task }) {
  return (
    <div className={task.parentTaskId ? 'pl-6' : ''}>
      <div className="font-medium flex items-center gap-2">
        {task.parentTaskId && (
          <span className="text-muted-foreground text-xs">└─</span>
        )}
        <Link 
          to={`/backlog/${task.id}`}
          className="hover:underline cursor-pointer text-primary"
        >
          {task.title}
        </Link>
        {task.isBlocked && task.blockedByCount && task.blockedByCount > 0 && (
          <BlockedBadge task={task} />
        )}
        {task.blockingTasks && task.blockingTasks.length > 0 && (
          <BlockingBadge task={task} />
        )}
        {!task.parentTaskId && task.children && task.children.length > 0 && (
          <SubtasksBadge task={task} />
        )}
      </div>
      {task.description && (
        <div className="text-sm text-muted-foreground line-clamp-1">
          {task.description}
        </div>
      )}
    </div>
  );
}

function BlockedBadge({ task }: { task: Task }) {
  const { t } = useTranslation();
  
  return (
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
  );
}

function BlockingBadge({ task }: { task: Task }) {
  const { t } = useTranslation();
  
  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <Badge variant="secondary" className="h-5 px-1.5">
            <Shield className="h-3 w-3 mr-1" />
            {task.blockingTasks?.length}
          </Badge>
        </TooltipTrigger>
        <TooltipContent className="max-w-xs">
          <p className="font-semibold mb-1">{t('backlogPage.blockingCount', { count: task.blockingTasks?.length })}:</p>
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
  );
}

function SubtasksBadge({ task }: { task: Task }) {
  const { t } = useTranslation();
  
  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <Badge variant="outline" className="h-5 px-1.5">
            <List className="h-3 w-3 mr-1" />
            {task.children?.length}
          </Badge>
        </TooltipTrigger>
        <TooltipContent className="max-w-xs">
          <p className="font-semibold mb-1">{t('backlogPage.subtaskCount', { count: task.children?.length })}:</p>
          <ul className="text-sm space-y-0.5">
            {task.children?.slice(0, 3).map((child, idx) => (
              <li key={idx}>• {child.title}</li>
            ))}
            {task.children && task.children.length > 3 && (
              <li className="text-muted-foreground">{t('backlogPage.andMore', { count: task.children.length - 3 })}</li>
            )}
          </ul>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}

function StatusDropdown({ task, onStatusChange }: { task: Task; onStatusChange: (taskId: number, status: TaskStatus) => void }) {
  const { t } = useTranslation();
  
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="sm" className="h-auto p-0">
          <Badge variant={getStatusBadgeVariant(task.status)}>
            {t(STATUS_OPTIONS.find(s => s.value === task.status)?.labelKey || 'backlogPage.statusOptions.backlog')}
          </Badge>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        {STATUS_OPTIONS.map((status) => (
          <DropdownMenuItem
            key={status.value}
            onClick={() => onStatusChange(task.id, status.value)}
          >
            <Badge variant={status.variant} className="mr-2">
              {t(status.labelKey)}
            </Badge>
            {task.status === status.value && <Check className="ml-auto h-4 w-4" />}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function PriorityDropdown({ task, onPriorityChange }: { task: Task; onPriorityChange: (taskId: number, priority: TaskPriority) => void }) {
  const { t } = useTranslation();
  
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="sm" className="h-auto p-0">
          <Badge variant={getPriorityBadgeVariant(task.priority)}>
            {t(PRIORITY_OPTIONS.find(p => p.value === task.priority)?.labelKey || 'backlogPage.priorityOptions.medium')}
          </Badge>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        {PRIORITY_OPTIONS.map((priority) => (
          <DropdownMenuItem
            key={priority.value}
            onClick={() => onPriorityChange(task.id, priority.value)}
          >
            <Badge variant={priority.variant} className="mr-2">
              {t(priority.labelKey)}
            </Badge>
            {task.priority === priority.value && <Check className="ml-auto h-4 w-4" />}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

function AssigneeCell({ task }: { task: Task }) {
  const { t } = useTranslation();
  
  if (!task.assigneeName) {
    return <span className="text-muted-foreground">{t('backlogPage.unassigned')}</span>;
  }
  
  return (
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
  );
}

function DueDateCell({ task }: { task: Task }) {
  if (!task.dueDate) {
    return <span className="text-muted-foreground">-</span>;
  }
  
  const isOverdue = dayjs(task.dueDate).isBefore(dayjs(), 'day') && task.status !== 'DONE';
  
  return (
    <span className={cn("text-sm", isOverdue && "text-destructive")}>
      {dayjs(task.dueDate).format('MMM D, YYYY')}
    </span>
  );
}

interface TaskActionsProps {
  task: Task;
  activeTimerTaskId: number | null;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubtask: (task: Task) => void;
  onStartTimer: (task: Task) => void;
}

function TaskActions({
  task,
  activeTimerTaskId,
  onViewTask,
  onEditTask,
  onDeleteTask,
  onAddSubtask,
  onStartTimer,
}: TaskActionsProps) {
  const { t } = useTranslation();
  
  return (
    <div className="flex justify-end gap-1">
      {/* Add Subtask - only for parent tasks */}
      {!task.parentTaskId && (
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="outline"
                size="sm"
                onClick={() => onAddSubtask(task)}
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
      
      {/* Timer Button */}
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
      
      {/* View Button */}
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="ghost" size="icon" onClick={() => onViewTask(task)}>
              <Eye className="h-4 w-4" />
            </Button>
          </TooltipTrigger>
          <TooltipContent>{t('backlogPage.viewDetails')}</TooltipContent>
        </Tooltip>
      </TooltipProvider>
      
      {/* Edit Button */}
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="ghost" size="icon" onClick={() => onEditTask(task)}>
              <Pencil className="h-4 w-4" />
            </Button>
          </TooltipTrigger>
          <TooltipContent>{t('backlogPage.edit')}</TooltipContent>
        </Tooltip>
      </TooltipProvider>
      
      {/* Delete Button */}
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <Button variant="ghost" size="icon" onClick={() => onDeleteTask(task.id)}>
              <Trash2 className="h-4 w-4 text-destructive" />
            </Button>
          </TooltipTrigger>
          <TooltipContent>{t('backlogPage.delete')}</TooltipContent>
        </Tooltip>
      </TooltipProvider>
    </div>
  );
}
