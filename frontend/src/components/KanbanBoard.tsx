import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { 
  GripVertical, 
  MoreVertical, 
  Clock, 
  Eye, 
  Pencil, 
  Trash2,
  AlertCircle,
  Shield,
  Plus,
  Settings,
  EyeOff,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Checkbox } from '@/components/ui/checkbox';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/components/ui/popover';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { Task, TaskStatus, TaskPriority } from '../types';
import { useBreakpointHelpers } from '../hooks/useBreakpoint';

// Define the columns for Kanban board based on TaskStatus
const KANBAN_COLUMNS: { status: TaskStatus; labelKey: string; color: string }[] = [
  { status: 'BACKLOG', labelKey: 'backlogPage.statusOptions.backlog', color: 'bg-gray-500' },
  { status: 'TODO', labelKey: 'backlogPage.statusOptions.todo', color: 'bg-blue-500' },
  { status: 'IN_PROGRESS', labelKey: 'backlogPage.statusOptions.inProgress', color: 'bg-yellow-500' },
  { status: 'BLOCKED', labelKey: 'backlogPage.statusOptions.blocked', color: 'bg-red-500' },
  { status: 'IN_REVIEW', labelKey: 'backlogPage.statusOptions.inReview', color: 'bg-purple-500' },
  { status: 'DONE', labelKey: 'backlogPage.statusOptions.done', color: 'bg-green-500' },
];

const priorityColors: Record<TaskPriority, string> = {
  LOW: 'bg-slate-500',
  MEDIUM: 'bg-blue-500',
  HIGH: 'bg-orange-500',
  URGENT: 'bg-red-600',
};

// Essential columns that should always remain visible when using "hide optional"
const ESSENTIAL_COLUMNS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

interface KanbanBoardProps {
  tasks: Task[];
  onStatusChange: (taskId: number, newStatus: TaskStatus) => Promise<void>;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubtask?: (task: Task) => void;
  onStartTimer?: (task: Task) => void;
  loading?: boolean;
  visibleColumns?: TaskStatus[];
  onToggleColumn?: (status: TaskStatus) => void;
}

interface KanbanCardProps {
  task: Task;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubtask?: (task: Task) => void;
  onStartTimer?: (task: Task) => void;
  dragging?: boolean;
}

function KanbanCard({ task, onViewTask, onEditTask, onDeleteTask, onAddSubtask, onStartTimer, dragging }: KanbanCardProps) {
  const { t } = useTranslation();
  
  const handleDragStart = (e: React.DragEvent) => {
    e.dataTransfer.setData('taskId', task.id.toString());
    e.dataTransfer.effectAllowed = 'move';
  };

  const isBlocked = task.blockedByCount && task.blockedByCount > 0;
  const isBlocking = task.blockingTasks && task.blockingTasks.length > 0;
  const blockingCount = task.blockingTasks?.length || 0;

  return (
    <Card 
      className={cn(
        "mb-3 cursor-grab hover:shadow-md transition-shadow",
        dragging && "opacity-50 shadow-lg",
        isBlocked && "border-red-500/50"
      )}
      draggable
      onDragStart={handleDragStart}
    >
      <CardContent className="p-3">
        {/* Header with priority and actions */}
        <div className="flex items-start justify-between gap-2 mb-2">
          <div className="flex items-center gap-2">
            <GripVertical className="h-4 w-4 text-muted-foreground cursor-grab" />
            <div className={cn("w-2 h-2 rounded-full", priorityColors[task.priority])} />
            <span className="text-xs text-muted-foreground uppercase">
              {task.priority}
            </span>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-6 w-6">
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
              {onAddSubtask && (
                <DropdownMenuItem onClick={() => onAddSubtask(task)}>
                  <Plus className="h-4 w-4 mr-2" />
                  {t('backlogPage.addSubTask')}
                </DropdownMenuItem>
              )}
              {onStartTimer && (
                <DropdownMenuItem onClick={() => onStartTimer(task)}>
                  <Clock className="h-4 w-4 mr-2" />
                  {t('backlogPage.startTimer')}
                </DropdownMenuItem>
              )}
              <DropdownMenuItem 
                onClick={() => onDeleteTask(task.id)}
                className="text-destructive"
              >
                <Trash2 className="h-4 w-4 mr-2" />
                {t('common.delete')}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {/* Title */}
        <h4 
          className="font-medium text-sm mb-2 line-clamp-2 cursor-pointer hover:text-primary"
          onClick={() => onViewTask(task)}
        >
          {task.title}
        </h4>

        {/* Dependency badges */}
        {(isBlocked || isBlocking) && (
          <div className="flex gap-1 mb-2">
            {isBlocked && (
              <Badge variant="destructive" className="text-xs">
                <AlertCircle className="h-3 w-3 mr-1" />
                {t('backlogPage.blockedBy', { count: task.blockedByCount })}
              </Badge>
            )}
            {isBlocking && (
              <Badge variant="secondary" className="text-xs">
                <Shield className="h-3 w-3 mr-1" />
                {t('backlogPage.blocking', { count: blockingCount })}
              </Badge>
            )}
          </div>
        )}

        {/* Meta info */}
        <div className="flex items-center justify-between text-xs text-muted-foreground">
          <div className="flex items-center gap-2">
            {task.assigneeName && (
              <TooltipProvider>
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Avatar className="h-5 w-5">
                      <AvatarFallback className="text-[10px]">
                        {task.assigneeName.charAt(0).toUpperCase()}
                      </AvatarFallback>
                    </Avatar>
                  </TooltipTrigger>
                  <TooltipContent>{task.assigneeName}</TooltipContent>
                </Tooltip>
              </TooltipProvider>
            )}
            {task.estimateHours && (
              <span className="flex items-center gap-1">
                <Clock className="h-3 w-3" />
                {task.estimateHours}h
              </span>
            )}
          </div>
          {task.pitchTitle && (
            <Badge variant="outline" className="text-[10px]">
              {task.pitchTitle}
            </Badge>
          )}
        </div>

        {/* Tags */}
        {task.tags && task.tags.length > 0 && (
          <div className="flex flex-wrap gap-1 mt-2">
            {task.tags.split(',').slice(0, 3).map((tag: string, index: number) => (
              <Badge key={index} variant="secondary" className="text-[10px]">
                {tag.trim()}
              </Badge>
            ))}
            {task.tags.split(',').length > 3 && (
              <Badge variant="secondary" className="text-[10px]">
                +{task.tags.split(',').length - 3}
              </Badge>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

interface KanbanColumnProps {
  status: TaskStatus;
  labelKey: string;
  color: string;
  tasks: Task[];
  onStatusChange: (taskId: number, newStatus: TaskStatus) => Promise<void>;
  onViewTask: (task: Task) => void;
  onEditTask: (task: Task) => void;
  onDeleteTask: (taskId: number) => void;
  onAddSubtask?: (task: Task) => void;
  onStartTimer?: (task: Task) => void;
}

function KanbanColumn({ 
  status, 
  labelKey, 
  color, 
  tasks, 
  onStatusChange, 
  onViewTask, 
  onEditTask, 
  onDeleteTask,
  onAddSubtask,
  onStartTimer
}: KanbanColumnProps) {
  const { t } = useTranslation();
  const [isDragOver, setIsDragOver] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setIsDragOver(true);
  };

  const handleDragLeave = () => {
    setIsDragOver(false);
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const taskId = parseInt(e.dataTransfer.getData('taskId'), 10);
    if (!isNaN(taskId)) {
      await onStatusChange(taskId, status);
    }
  };

  const columnTasks = tasks.filter(task => task.status === status);

  return (
    <div 
      className={cn(
        "flex-shrink-0 bg-muted/30 rounded-lg p-3 transition-colors",
        // Responsive width: narrower on mobile, wider on desktop
        "w-[280px] sm:w-72",
        // Scroll snap alignment for mobile
        "snap-center",
        isDragOver && "bg-primary/10 ring-2 ring-primary ring-inset"
      )}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* Column Header */}
      <div className="flex items-center gap-2 mb-3 px-1">
        <div className={cn("w-3 h-3 rounded-full", color)} />
        <h3 className="font-semibold text-sm">{t(labelKey)}</h3>
        <Badge variant="secondary" className="ml-auto">
          {columnTasks.length}
        </Badge>
      </div>

      {/* Tasks */}
      <ScrollArea className="h-[calc(100vh-280px)]">
        <div className="pr-2">
          {columnTasks.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground text-sm">
              {t('backlogPage.kanban.noTasks')}
            </div>
          ) : (
            columnTasks.map(task => (
              <KanbanCard
                key={task.id}
                task={task}
                onViewTask={onViewTask}
                onEditTask={onEditTask}
                onDeleteTask={onDeleteTask}
                onAddSubtask={onAddSubtask}
                onStartTimer={onStartTimer}
              />
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  );
}

export default function KanbanBoard({ 
  tasks, 
  onStatusChange, 
  onViewTask, 
  onEditTask, 
  onDeleteTask,
  onAddSubtask,
  onStartTimer,
  loading,
  visibleColumns = KANBAN_COLUMNS.map(col => col.status),
  onToggleColumn
}: KanbanBoardProps) {
  const { t } = useTranslation();
  const { isMobile } = useBreakpointHelpers();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  const visibleColumnsSet = new Set(visibleColumns);
  const hiddenColumnsCount = KANBAN_COLUMNS.length - visibleColumns.length;

  return (
    <div className="space-y-4">
      {/* Column Visibility Controls */}
      {onToggleColumn && (
        <div className="flex items-center gap-2">
          <Popover>
            <PopoverTrigger asChild>
              <Button variant="outline" size="sm" className="gap-2">
                <Settings className="h-4 w-4" />
                {t('backlogPage.kanban.manageColumns')}
                {hiddenColumnsCount > 0 && (
                  <Badge variant="secondary" className="ml-1">
                    <EyeOff className="h-3 w-3 mr-1" />
                    {hiddenColumnsCount}
                  </Badge>
                )}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-64" align="start">
              <div className="space-y-3">
                <h4 className="font-semibold text-sm">{t('backlogPage.kanban.columnVisibility')}</h4>
                <div className="space-y-2">
                  {KANBAN_COLUMNS.map(column => (
                    <div key={column.status} className="flex items-center space-x-2">
                      <Checkbox
                        id={`column-${column.status}`}
                        checked={visibleColumnsSet.has(column.status)}
                        onCheckedChange={() => {
                          onToggleColumn(column.status);
                        }}
                      />
                      <div className="flex items-center gap-2 flex-1">
                        <div className={cn("w-3 h-3 rounded-full", column.color)} />
                        <label
                          htmlFor={`column-${column.status}`}
                          className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer flex-1"
                        >
                          {t(column.labelKey)}
                        </label>
                      </div>
                    </div>
                  ))}
                </div>
                <div className="pt-2 border-t">
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex-1"
                      onClick={() => {
                        KANBAN_COLUMNS.forEach(col => {
                          if (!visibleColumnsSet.has(col.status)) {
                            onToggleColumn(col.status);
                          }
                        });
                      }}
                    >
                      {t('backlogPage.kanban.showAll')}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      className="flex-1"
                      onClick={() => {
                        // Keep at least TODO, IN_PROGRESS, and DONE visible
                        KANBAN_COLUMNS.forEach(col => {
                          if (visibleColumnsSet.has(col.status) && !ESSENTIAL_COLUMNS.includes(col.status)) {
                            onToggleColumn(col.status);
                          }
                        });
                      }}
                    >
                      {t('backlogPage.kanban.hideOptional')}
                    </Button>
                  </div>
                </div>
              </div>
            </PopoverContent>
          </Popover>
        </div>
      )}

      {/* Kanban Board */}
      <div 
        className={cn(
          "flex gap-4 overflow-x-auto pb-4",
          // Mobile: scroll snap for better touch navigation
          isMobile && "snap-x snap-mandatory scroll-smooth -mx-4 px-4",
          // Touch-friendly scrolling
          "touch-pan-x"
        )}
      >
        {KANBAN_COLUMNS
          .filter(column => visibleColumnsSet.has(column.status))
          .map(column => (
            <KanbanColumn
              key={column.status}
              status={column.status}
              labelKey={column.labelKey}
              color={column.color}
              tasks={tasks}
              onStatusChange={onStatusChange}
              onViewTask={onViewTask}
              onEditTask={onEditTask}
              onDeleteTask={onDeleteTask}
              onAddSubtask={onAddSubtask}
              onStartTimer={onStartTimer}
            />
          ))}
      </div>
    </div>
  );
}
