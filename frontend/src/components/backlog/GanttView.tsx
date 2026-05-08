import { useState, useMemo, useRef, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  format,
  startOfDay,
  addDays,
  differenceInDays,
  isToday,
  parseISO,
  isSameMonth,
} from 'date-fns';
import { ChevronLeft, ChevronRight, Calendar } from 'lucide-react';
import { cn } from '../../lib/utils';
import { Button } from '@/components/ui/button';
import { Task, Cycle, TaskStatus } from '../../types';

// ─── Layout constants ─────────────────────────────────────────────────────────

const ROW_HEIGHT = 36; // px — task row
const GROUP_HEADER_HEIGHT = 32; // px — pitch group header
const BAR_HEIGHT = 24; // px — Gantt bar
const BAR_TOP_OFFSET = (ROW_HEIGHT - BAR_HEIGHT) / 2;
const FALLBACK_BAR_DAYS = 3;
const LEFT_PANEL_WIDTH = 220; // px

// ─── Zoom configuration ───────────────────────────────────────────────────────

type ZoomLevel = 'week' | 'month';

const ZOOM_CONFIG: Record<ZoomLevel, { days: number; colWidth: number }> = {
  week: { days: 28, colWidth: 28 },
  month: { days: 90, colWidth: 14 },
};

// ─── Status → Tailwind colour map ────────────────────────────────────────────

const STATUS_BAR_CLASS: Record<TaskStatus, string> = {
  BACKLOG: 'bg-slate-400',
  TODO: 'bg-gray-400',
  IN_PROGRESS: 'bg-blue-500',
  BLOCKED: 'bg-red-500',
  IN_REVIEW: 'bg-yellow-500',
  DONE: 'bg-green-500',
  CANCELLED: 'bg-gray-300',
};

// ─── Prop types ───────────────────────────────────────────────────────────────

export interface GanttViewProps {
  tasks: Task[];
  cycles: Cycle[];
  selectedCycle: number | 'all';
  onViewTask: (task: Task) => void;
}

// ─── Internal row types ───────────────────────────────────────────────────────

type GroupRow = { type: 'group'; pitchTitle: string };
type TaskRow = { type: 'task'; task: Task };
type GanttRow = GroupRow | TaskRow;

// ─── Helpers ─────────────────────────────────────────────────────────────────

function parseDate(dateStr: string | undefined): Date | null {
  if (!dateStr) return null;
  try {
    return startOfDay(parseISO(dateStr));
  } catch {
    return null;
  }
}

interface BarPosition {
  left: number;
  width: number;
  isVisible: boolean;
}

function computeBarPosition(
  task: Task,
  windowStart: Date,
  totalDays: number,
  colWidth: number
): BarPosition {
  const start = parseDate(task.createdAt) ?? startOfDay(new Date());

  let end: Date;
  if (task.status === 'DONE' || task.status === 'CANCELLED') {
    end =
      parseDate(task.completedAt) ??
      parseDate(task.dueDate) ??
      addDays(start, FALLBACK_BAR_DAYS);
  } else {
    end = parseDate(task.dueDate) ?? addDays(start, FALLBACK_BAR_DAYS);
  }

  // Guarantee at least FALLBACK_BAR_DAYS width so the bar is visible
  if (differenceInDays(end, start) < 1) {
    end = addDays(start, FALLBACK_BAR_DAYS);
  }

  const windowEnd = addDays(windowStart, totalDays);

  // Clamp bar to the visible window
  const clampedStart = start < windowStart ? windowStart : start;
  const clampedEnd = end > windowEnd ? windowEnd : end;

  if (clampedStart >= windowEnd || clampedEnd <= windowStart) {
    return { left: 0, width: 0, isVisible: false };
  }

  const left = differenceInDays(clampedStart, windowStart) * colWidth;
  const width = Math.max(
    differenceInDays(clampedEnd, clampedStart) * colWidth,
    colWidth
  );

  return { left, width, isVisible: true };
}

interface PitchGroup {
  pitchTitle: string;
  tasks: Task[];
}

function groupTasksByPitch(tasks: Task[], unassignedLabel: string): PitchGroup[] {
  const map = new Map<string, Task[]>();

  for (const task of tasks) {
    const key = task.pitchTitle ?? unassignedLabel;
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(task);
  }

  const named: PitchGroup[] = [];
  let unassigned: Task[] = [];

  for (const [pitchTitle, groupTasks] of map.entries()) {
    if (pitchTitle === unassignedLabel) {
      unassigned = groupTasks;
    } else {
      named.push({ pitchTitle, tasks: groupTasks });
    }
  }

  named.sort((a, b) => a.pitchTitle.localeCompare(b.pitchTitle));

  if (unassigned.length > 0) {
    named.push({ pitchTitle: unassignedLabel, tasks: unassigned });
  }

  return named;
}

interface MonthSpan {
  label: string;
  spanDays: number;
}

function buildMonthHeaders(windowStart: Date, totalDays: number): MonthSpan[] {
  const result: MonthSpan[] = [];
  const windowEnd = addDays(windowStart, totalDays);
  let cursor = windowStart;

  while (cursor < windowEnd) {
    let count = 0;
    const anchor = cursor;
    while (cursor < windowEnd && isSameMonth(cursor, anchor)) {
      count++;
      cursor = addDays(cursor, 1);
    }
    result.push({ label: format(anchor, 'MMM yyyy'), spanDays: count });
  }

  return result;
}

// ─── Avatar initial ───────────────────────────────────────────────────────────

function AvatarInitial({ name }: { name?: string }) {
  if (!name) return <span className="h-5 w-5 shrink-0" />;
  const initials = name
    .split(' ')
    .map((n) => n[0] ?? '')
    .join('')
    .slice(0, 2)
    .toUpperCase();
  return (
    <span className="inline-flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary/20 text-[10px] font-medium text-primary select-none">
      {initials}
    </span>
  );
}

// ─── GanttView ────────────────────────────────────────────────────────────────

export function GanttView({ tasks, onViewTask }: GanttViewProps) {
  const { t } = useTranslation();

  const [zoom, setZoom] = useState<ZoomLevel>('month');
  const [windowStart, setWindowStart] = useState<Date>(() =>
    startOfDay(addDays(new Date(), -7))
  );

  const rightPanelRef = useRef<HTMLDivElement>(null);
  const leftScrollRef = useRef<HTMLDivElement>(null);

  const { days: totalDays, colWidth } = ZOOM_CONFIG[zoom];
  const unassignedLabel = t('ganttView.unassigned');

  // Sync vertical scroll: right panel drives left panel
  useEffect(() => {
    const right = rightPanelRef.current;
    const left = leftScrollRef.current;
    if (!right || !left) return;

    function syncLeft() {
      left!.scrollTop = right!.scrollTop;
    }

    right.addEventListener('scroll', syncLeft, { passive: true });
    return () => right.removeEventListener('scroll', syncLeft);
  }, []);

  const navigate = useCallback(
    (direction: -1 | 1) => {
      const shift = zoom === 'week' ? 7 : 30;
      setWindowStart((prev) => addDays(prev, direction * shift));
    },
    [zoom]
  );

  function goToToday() {
    setWindowStart(startOfDay(addDays(new Date(), -7)));
  }

  // Derived data
  const groups = useMemo(
    () => groupTasksByPitch(tasks, unassignedLabel),
    [tasks, unassignedLabel]
  );

  const monthHeaders = useMemo(
    () => buildMonthHeaders(windowStart, totalDays),
    [windowStart, totalDays]
  );

  const dayHeaders = useMemo(
    () => Array.from({ length: totalDays }, (_, i) => addDays(windowStart, i)),
    [windowStart, totalDays]
  );

  // Today marker
  const today = startOfDay(new Date());
  const todayOffset = differenceInDays(today, windowStart);
  const todayVisible = todayOffset >= 0 && todayOffset < totalDays;
  const todayLeft = todayOffset * colWidth;

  const totalWidth = totalDays * colWidth;

  // Flatten groups into a single row list for aligned rendering
  const rows = useMemo<GanttRow[]>(() => {
    const list: GanttRow[] = [];
    for (const group of groups) {
      list.push({ type: 'group', pitchTitle: group.pitchTitle });
      for (const task of group.tasks) {
        list.push({ type: 'task', task });
      }
    }
    return list;
  }, [groups]);

  const totalHeight = rows.reduce(
    (acc, row) => acc + (row.type === 'group' ? GROUP_HEADER_HEIGHT : ROW_HEIGHT),
    0
  );

  const hasAnyDates = tasks.length > 0;

  // ─── Render ───────────────────────────────────────────────────────────────

  return (
    <div className="flex flex-col h-full rounded-lg border border-border bg-background overflow-hidden">

      {/* Toolbar */}
      <div className="flex items-center justify-between gap-2 px-4 py-2 border-b border-border bg-muted/30 shrink-0">
        {/* Navigation */}
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="sm"
            className="h-7 w-7 p-0"
            onClick={() => navigate(-1)}
            aria-label="Previous"
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-7 w-7 p-0"
            onClick={() => navigate(1)}
            aria-label="Next"
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs gap-1"
            onClick={goToToday}
          >
            <Calendar className="h-3.5 w-3.5" />
            {t('ganttView.today')}
          </Button>
        </div>

        {/* Title */}
        <span className="text-sm font-medium text-foreground">
          {t('ganttView.title')}
        </span>

        {/* Zoom controls */}
        <div className="flex items-center gap-1">
          <Button
            variant={zoom === 'week' ? 'default' : 'ghost'}
            size="sm"
            className="h-7 px-3 text-xs"
            onClick={() => setZoom('week')}
          >
            {t('ganttView.zoomWeek')}
          </Button>
          <Button
            variant={zoom === 'month' ? 'default' : 'ghost'}
            size="sm"
            className="h-7 px-3 text-xs"
            onClick={() => setZoom('month')}
          >
            {t('ganttView.zoomMonth')}
          </Button>
        </div>
      </div>

      {/* Empty state */}
      {!hasAnyDates && (
        <div className="flex flex-1 flex-col items-center justify-center gap-2 text-muted-foreground p-8">
          <Calendar className="h-10 w-10 opacity-30" aria-hidden="true" />
          <p className="text-sm font-medium">{t('ganttView.noTasksWithDates')}</p>
          <p className="text-xs text-center max-w-xs">{t('ganttView.setDueDates')}</p>
        </div>
      )}

      {/* Main two-panel layout */}
      {hasAnyDates && (
        <div className="flex flex-1 overflow-hidden">

          {/* ── Left panel ─────────────────────────────────────────── */}
          <div
            className="shrink-0 border-r border-border flex flex-col"
            style={{ width: LEFT_PANEL_WIDTH }}
          >
            {/* Left header — fixed height matching right panel header */}
            <div
              className="shrink-0 border-b border-border bg-muted/50 flex items-end px-3 pb-1"
              style={{ height: 48 }}
            >
              <span className="text-[11px] font-semibold text-muted-foreground uppercase tracking-wide truncate">
                {t('ganttView.title')}
              </span>
            </div>

            {/* Left rows — overflow hidden, scrollTop synced from right panel */}
            <div
              ref={leftScrollRef}
              className="flex-1 overflow-hidden"
              aria-hidden="true"
              tabIndex={-1}
            >
              <div style={{ height: totalHeight }}>
                {rows.map((row, idx) => {
                  if (row.type === 'group') {
                    return (
                      <div
                        key={`lp-g-${idx}`}
                        className="flex items-center px-3 bg-muted/40 border-b border-border"
                        style={{ height: GROUP_HEADER_HEIGHT }}
                      >
                        <span className="text-xs font-semibold text-foreground truncate">
                          {row.pitchTitle}
                        </span>
                      </div>
                    );
                  }
                  const { task } = row;
                  return (
                    <div
                      key={`lp-t-${task.id}-${idx}`}
                      className="flex items-center gap-2 px-3 border-b border-border/50 hover:bg-muted/30 cursor-pointer"
                      style={{ height: ROW_HEIGHT }}
                      onClick={() => onViewTask(task)}
                      role="button"
                      tabIndex={0}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          onViewTask(task);
                        }
                      }}
                    >
                      <AvatarInitial name={task.assigneeName} />
                      <span
                        className={cn(
                          'text-xs text-foreground truncate flex-1',
                          task.status === 'CANCELLED' && 'line-through text-muted-foreground'
                        )}
                        title={task.title}
                      >
                        {task.title}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* ── Right panel (scrollable both axes) ─────────────────── */}
          <div
            ref={rightPanelRef}
            className="flex-1 overflow-auto"
          >
            {/* Fixed-width inner container */}
            <div style={{ width: totalWidth, minWidth: totalWidth }}>

              {/* Timeline header — sticky top */}
              <div
                className="sticky top-0 z-20 bg-background border-b border-border"
                style={{ height: 48 }}
              >
                {/* Row 1 — Month names */}
                <div className="flex border-b border-border/40" style={{ height: 24 }}>
                  {monthHeaders.map((cell, i) => (
                    <div
                      key={i}
                      className="shrink-0 flex items-center px-2 border-r border-border/30 overflow-hidden"
                      style={{ width: cell.spanDays * colWidth }}
                    >
                      <span className="text-[11px] font-semibold text-muted-foreground truncate">
                        {cell.label}
                      </span>
                    </div>
                  ))}
                </div>

                {/* Row 2 — Day numbers */}
                <div className="flex" style={{ height: 24 }}>
                  {dayHeaders.map((day, i) => {
                    const highlight = isToday(day);
                    return (
                      <div
                        key={i}
                        className={cn(
                          'shrink-0 flex items-center justify-center border-r border-border/20',
                          highlight && 'bg-red-50 dark:bg-red-950/20'
                        )}
                        style={{ width: colWidth }}
                      >
                        {colWidth >= 20 && (
                          <span
                            className={cn(
                              'text-[10px] leading-none',
                              highlight
                                ? 'font-bold text-red-500'
                                : 'text-muted-foreground'
                            )}
                          >
                            {format(day, 'd')}
                          </span>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Timeline body */}
              <div className="relative" style={{ height: totalHeight }}>

                {/* Vertical grid lines (background layer) */}
                <div
                  className="absolute inset-0 flex pointer-events-none"
                  aria-hidden="true"
                >
                  {dayHeaders.map((day, i) => (
                    <div
                      key={i}
                      className={cn(
                        'shrink-0 h-full border-r',
                        isToday(day)
                          ? 'border-red-300 dark:border-red-700/60'
                          : 'border-border/20'
                      )}
                      style={{ width: colWidth }}
                    />
                  ))}
                </div>

                {/* Today vertical marker */}
                {todayVisible && (
                  <div
                    className="absolute top-0 bottom-0 w-0.5 bg-red-500/80 z-10 pointer-events-none"
                    style={{ left: todayLeft }}
                    aria-label={t('ganttView.today')}
                  />
                )}

                {/* Rows with Gantt bars */}
                {rows.map((row, idx) => {
                  if (row.type === 'group') {
                    return (
                      <div
                        key={`rp-g-${idx}`}
                        className="border-b border-border bg-muted/15"
                        style={{ height: GROUP_HEADER_HEIGHT }}
                      />
                    );
                  }

                  const { task } = row;
                  const bar = computeBarPosition(
                    task,
                    windowStart,
                    totalDays,
                    colWidth
                  );

                  return (
                    <div
                      key={`rp-t-${task.id}-${idx}`}
                      className="relative border-b border-border/40"
                      style={{ height: ROW_HEIGHT }}
                    >
                      {bar.isVisible && (
                        <button
                          type="button"
                          className={cn(
                            'absolute cursor-pointer transition-opacity hover:opacity-90',
                            'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1',
                            STATUS_BAR_CLASS[task.status],
                            task.status === 'CANCELLED' && 'opacity-50'
                          )}
                          style={{
                            top: BAR_TOP_OFFSET,
                            left: bar.left,
                            width: bar.width,
                            height: BAR_HEIGHT,
                            borderRadius: 4,
                          }}
                          onClick={() => onViewTask(task)}
                          title={
                            task.dueDate
                              ? `${task.title} — ${format(parseISO(task.dueDate), 'MMM d, yyyy')}`
                              : `${task.title} — ${t('ganttView.noDueDate')}`
                          }
                          aria-label={task.title}
                        >
                          {bar.width > 44 && (
                            <span
                              className={cn(
                                'absolute inset-0 flex items-center px-1.5',
                                'text-[10px] font-medium text-white truncate select-none',
                                task.status === 'CANCELLED' && 'line-through'
                              )}
                            >
                              {task.title}
                            </span>
                          )}
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
