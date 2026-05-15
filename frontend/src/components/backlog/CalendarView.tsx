import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  format,
  startOfMonth,
  endOfMonth,
  eachDayOfInterval,
  startOfWeek,
  endOfWeek,
  isSameMonth,
  isToday,
  parseISO,
  addMonths,
  subMonths,
  addWeeks,
  subWeeks,
} from 'date-fns';
import { ChevronLeft, ChevronRight, Calendar } from 'lucide-react';
import { cn } from '../../lib/utils';
import { Button } from '@/components/ui/button';
import { Task, TaskStatus } from '../../types';

// ─── Status colour map ────────────────────────────────────────────────────────

const STATUS_PILL_CLASS: Record<TaskStatus, string> = {
  BACKLOG: 'bg-slate-200 text-slate-700',
  TODO: 'bg-sky-100 text-sky-800',
  IN_PROGRESS: 'bg-blue-500 text-white',
  BLOCKED: 'bg-red-500 text-white',
  IN_REVIEW: 'bg-yellow-400 text-yellow-900',
  DONE: 'bg-green-500 text-white',
  CANCELLED: 'bg-gray-300 text-gray-500 line-through',
};

// ─── Types ───────────────────────────────────────────────────────────────────

type ZoomMode = 'month' | 'week';

export interface CalendarViewProps {
  tasks: Task[];
  onViewTask: (task: Task) => void;
}

// ─── Task pill ────────────────────────────────────────────────────────────────

function TaskPill({
  task,
  onClick,
}: {
  task: Task;
  onClick: (e: React.MouseEvent) => void;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        'w-full text-left truncate rounded px-1.5 py-0.5 text-xs font-medium leading-5 transition-opacity hover:opacity-80',
        STATUS_PILL_CLASS[task.status]
      )}
      title={task.title}
    >
      {task.title}
    </button>
  );
}

// ─── Day cell ─────────────────────────────────────────────────────────────────

function DayCell({
  day,
  tasksForDay,
  isCurrentMonth,
  onViewTask,
}: {
  day: Date;
  tasksForDay: Task[];
  isCurrentMonth: boolean;
  onViewTask: (task: Task) => void;
}) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);

  const MAX_VISIBLE = 3;
  const visibleTasks = expanded ? tasksForDay : tasksForDay.slice(0, MAX_VISIBLE);
  const hiddenCount = tasksForDay.length - MAX_VISIBLE;

  return (
    <div
      className={cn(
        'min-h-[80px] p-1 border-b border-r flex flex-col gap-0.5',
        isCurrentMonth ? 'bg-background' : 'bg-muted/20',
        isToday(day) && 'bg-primary/10 border-primary'
      )}
    >
      <span
        className={cn(
          'text-xs font-medium self-start leading-5 w-6 h-6 flex items-center justify-center rounded-full mb-0.5',
          !isCurrentMonth && 'text-muted-foreground/40',
          isToday(day) && 'bg-primary text-primary-foreground'
        )}
      >
        {format(day, 'd')}
      </span>

      {visibleTasks.map((task) => (
        <TaskPill
          key={task.id}
          task={task}
          onClick={(e) => {
            e.stopPropagation();
            onViewTask(task);
          }}
        />
      ))}

      {!expanded && hiddenCount > 0 && (
        <button
          className="text-xs text-primary hover:underline text-left px-1"
          onClick={() => setExpanded(true)}
        >
          {t('calendarView.moreTasksCount', { count: hiddenCount })}
        </button>
      )}
    </div>
  );
}

// ─── Main CalendarView ────────────────────────────────────────────────────────

export function CalendarView({ tasks, onViewTask }: CalendarViewProps) {
  const { t } = useTranslation();
  const [currentDate, setCurrentDate] = useState<Date>(new Date());
  const [zoomMode, setZoomMode] = useState<ZoomMode>('month');

  // Days to display in grid
  const gridDays = useMemo<Date[]>(() => {
    if (zoomMode === 'month') {
      const monthStart = startOfMonth(currentDate);
      const monthEnd = endOfMonth(currentDate);
      const gridStart = startOfWeek(monthStart, { weekStartsOn: 0 });
      const gridEnd = endOfWeek(monthEnd, { weekStartsOn: 0 });
      return eachDayOfInterval({ start: gridStart, end: gridEnd });
    } else {
      // Week view: 7 days of current week
      const weekStart = startOfWeek(currentDate, { weekStartsOn: 0 });
      const weekEnd = endOfWeek(currentDate, { weekStartsOn: 0 });
      return eachDayOfInterval({ start: weekStart, end: weekEnd });
    }
  }, [currentDate, zoomMode]);

  // Map each task to a day by dueDate
  const tasksByDay = useMemo<Map<string, Task[]>>(() => {
    const map = new Map<string, Task[]>();
    tasks.forEach((task) => {
      if (!task.dueDate) return;
      const key = format(parseISO(task.dueDate), 'yyyy-MM-dd');
      const existing = map.get(key) ?? [];
      map.set(key, [...existing, task]);
    });
    return map;
  }, [tasks]);

  // Unscheduled tasks
  const unscheduledTasks = useMemo<Task[]>(
    () => tasks.filter((t) => !t.dueDate),
    [tasks]
  );

  // Is anything on the calendar?
  const hasScheduledTasks = useMemo(
    () => tasks.some((t) => t.dueDate),
    [tasks]
  );

  const isEmpty = !hasScheduledTasks && unscheduledTasks.length === 0;

  const dayHeaders = [
    t('calendarView.sun'),
    t('calendarView.mon'),
    t('calendarView.tue'),
    t('calendarView.wed'),
    t('calendarView.thu'),
    t('calendarView.fri'),
    t('calendarView.sat'),
  ];

  // Navigation
  function handlePrev() {
    if (zoomMode === 'month') {
      setCurrentDate((d) => subMonths(d, 1));
    } else {
      setCurrentDate((d) => subWeeks(d, 1));
    }
  }

  function handleNext() {
    if (zoomMode === 'month') {
      setCurrentDate((d) => addMonths(d, 1));
    } else {
      setCurrentDate((d) => addWeeks(d, 1));
    }
  }

  function handleToday() {
    setCurrentDate(new Date());
  }

  const headerLabel =
    zoomMode === 'month'
      ? format(currentDate, 'MMMM yyyy')
      : `${format(gridDays[0], 'MMM d')} – ${format(gridDays[6], 'MMM d, yyyy')}`;

  return (
    <div className="flex gap-4">
      {/* Calendar grid */}
      <div className="flex-1 min-w-0">
        {/* Header row */}
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2">
            <Button variant="outline" size="sm" onClick={handlePrev}>
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <span className="font-semibold text-sm w-44 text-center">{headerLabel}</span>
            <Button variant="outline" size="sm" onClick={handleNext}>
              <ChevronRight className="h-4 w-4" />
            </Button>
            <Button variant="outline" size="sm" onClick={handleToday}>
              {t('calendarView.today')}
            </Button>
          </div>

          {/* Zoom toggle */}
          <div className="flex items-center border rounded-md">
            <Button
              variant={zoomMode === 'week' ? 'default' : 'ghost'}
              size="sm"
              className="rounded-r-none"
              onClick={() => setZoomMode('week')}
            >
              {t('calendarView.zoomWeek')}
            </Button>
            <Button
              variant={zoomMode === 'month' ? 'default' : 'ghost'}
              size="sm"
              className="rounded-l-none border-l"
              onClick={() => setZoomMode('month')}
            >
              {t('calendarView.zoomMonth')}
            </Button>
          </div>
        </div>

        {/* Day-of-week header */}
        <div className="grid grid-cols-7 border-t border-l">
          {dayHeaders.map((label) => (
            <div
              key={label}
              className="text-xs font-medium text-muted-foreground text-center py-1.5 border-b border-r bg-muted/30"
            >
              {label}
            </div>
          ))}
        </div>

        {/* Day cells grid */}
        {isEmpty ? (
          <div className="flex flex-col items-center justify-center py-16 text-muted-foreground gap-3 border border-t-0">
            <Calendar className="h-10 w-10 opacity-30" />
            <p className="text-sm">{t('calendarView.noTasksThisMonth')}</p>
          </div>
        ) : (
          <div className="grid grid-cols-7 border-l">
            {gridDays.map((day) => {
              const key = format(day, 'yyyy-MM-dd');
              const dayTasks = tasksByDay.get(key) ?? [];
              const inCurrentPeriod =
                zoomMode === 'month'
                  ? isSameMonth(day, currentDate)
                  : true;
              return (
                <DayCell
                  key={key}
                  day={day}
                  tasksForDay={dayTasks}
                  isCurrentMonth={inCurrentPeriod}
                  onViewTask={onViewTask}
                />
              );
            })}
          </div>
        )}
      </div>

      {/* Unscheduled sidebar — hidden on small screens */}
      <div className="hidden md:flex flex-col w-[200px] shrink-0">
        <h3 className="text-xs font-semibold text-muted-foreground mb-2 leading-5">
          {t('calendarView.unscheduledCount', { count: unscheduledTasks.length })}
        </h3>
        <div
          className="flex flex-col gap-1 overflow-y-auto pr-0.5"
          style={{ maxHeight: '480px' }}
        >
          {unscheduledTasks.length === 0 ? (
            <p className="text-xs text-muted-foreground/60 italic">
              {t('calendarView.unscheduled')}
            </p>
          ) : (
            unscheduledTasks.map((task) => (
              <TaskPill
                key={task.id}
                task={task}
                onClick={(e) => {
                  e.stopPropagation();
                  onViewTask(task);
                }}
              />
            ))
          )}
        </div>
      </div>
    </div>
  );
}
