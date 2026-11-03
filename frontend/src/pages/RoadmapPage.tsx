import type { MouseEvent as ReactMouseEvent } from 'react';
import { useCallback, useEffect, useLayoutEffect, useRef, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  Target,
  Layers,
  Package,
  CheckCircle,
  Flag,
  Maximize2,
  ListChecks,
  Ban,
  AlertTriangle,
} from 'lucide-react';
import { roadmapService } from '../services/roadmapService';
import { epicService } from '../services/epicService';
import { initiativeService } from '../services/initiativeService';
import {
  RoadmapTimeline,
  TimelineEpic,
  TimelineTask,
} from '../types';
import { getBarExtent, computeConnectors } from '../utils/roadmapDependencies';
import { Switch } from '../components/ui/switch';
import { useProject, useToast } from '../contexts';

import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import ProjectRequiredDialog from '../components/ProjectRequiredDialog';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import { Skeleton } from '../components/ui/skeleton';
import RoadmapPresentationMode from '../components/RoadmapPresentationMode';

// Helper to format dates for Gantt calculation
const parseDate = (dateStr?: string): Date | null => {
  if (!dateStr) return null;
  return new Date(dateStr);
};

// Calculate position and width for timeline bars
const calculateBarStyle = (
  startDate: Date | null,
  endDate: Date | null,
  timelineStart: Date,
  timelineEnd: Date
): { left: string; width: string } | null => {
  const extent = getBarExtent(startDate, endDate, timelineStart, timelineEnd);
  if (!extent) return null;
  return {
    left: `${extent.left * 100}%`,
    width: `${extent.width * 100}%`,
  };
};

// Single source of truth for status colors (hex values)
const STATUS_COLORS: Record<string, string> = {
  DRAFT: '#94a3b8',
  PLANNED: '#3b82f6',
  IN_PROGRESS: '#8b5cf6',
  COMPLETED: '#22c55e',
  ON_HOLD: '#f97316',
  CANCELLED: '#ef4444',
  PLANNING: '#60a5fa',
  STAGING: '#a855f7',
  RELEASED: '#16a34a',
};

const TASK_STATUS_COLORS: Record<string, string> = {
  BACKLOG: '#94a3b8',
  TODO: '#3b82f6',
  IN_PROGRESS: '#8b5cf6',
  IN_REVIEW: '#a855f7',
  BLOCKED: '#f97316',
  DONE: '#22c55e',
  CANCELLED: '#ef4444',
};

const getStatusCssColor = (status: string): string => {
  return STATUS_COLORS[status] || '#94a3b8';
};

const getRiskBadgeVariant = (risk: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (risk) {
    case 'LOW': return 'secondary';
    case 'MEDIUM': return 'default';
    case 'HIGH': return 'destructive';
    case 'CRITICAL': return 'destructive';
    default: return 'outline';
  }
};

interface TimelineBarProps {
  startDate?: string;
  endDate?: string;
  timelineStart: Date;
  timelineEnd: Date;
  status: string;
  color?: string;
  progress?: number;
  onDatesChange?: (startDate: string, endDate: string) => void;
}

function TimelineBar({ startDate, endDate, timelineStart, timelineEnd, status, color, progress, onDatesChange }: TimelineBarProps) {
  const { t } = useTranslation();
  const containerRef = useRef<HTMLDivElement>(null);
  const [dragging, setDragging] = useState<'move' | 'start' | 'end' | null>(null);
  const [dragOffset, setDragOffset] = useState({ leftPx: 0, widthPx: 0 });
  const dragStartRef = useRef({ mouseX: 0, origLeftPx: 0, origWidthPx: 0 });

  const onDatesChangeRef = useRef(onDatesChange);
  onDatesChangeRef.current = onDatesChange;

  const start = parseDate(startDate);
  const end = parseDate(endDate);
  const barStyle = calculateBarStyle(start, end, timelineStart, timelineEnd);
  const totalDays = Math.max(1, Math.ceil((timelineEnd.getTime() - timelineStart.getTime()) / (1000 * 60 * 60 * 24)));


  const bgColor = color || getStatusCssColor(status);

  const pxToDateRef = useRef((_pxOffset: number): Date => new Date(timelineStart));
  pxToDateRef.current = (pxOffset: number): Date => {
    if (!containerRef.current) return new Date(timelineStart);
    const containerWidth = containerRef.current.parentElement!.clientWidth;
    const dayOffset = Math.round((pxOffset / containerWidth) * totalDays);
    const d = new Date(timelineStart);
    d.setDate(d.getDate() + dayOffset);
    return d;
  };

  const formatDate = (d: Date) => d.toISOString().split('T')[0];

  const handleMouseDown = (e: ReactMouseEvent, mode: 'move' | 'start' | 'end') => {
    if (!onDatesChangeRef.current || !containerRef.current) return;
    e.preventDefault();
    e.stopPropagation();
    const rect = containerRef.current.getBoundingClientRect();
    dragStartRef.current = {
      mouseX: e.clientX,
      origLeftPx: rect.left - containerRef.current.parentElement!.getBoundingClientRect().left,
      origWidthPx: rect.width,
    };
    setDragOffset({ leftPx: 0, widthPx: 0 });
    setDragging(mode);
  };

  useEffect(() => {
    if (!dragging) return;
    const handleMouseMove = (e: MouseEvent) => {
      const dx = e.clientX - dragStartRef.current.mouseX;
      if (dragging === 'move') {
        setDragOffset({ leftPx: dx, widthPx: 0 });
      } else if (dragging === 'start') {
        setDragOffset({ leftPx: dx, widthPx: -dx });
      } else {
        setDragOffset({ leftPx: 0, widthPx: dx });
      }
    };
    const handleMouseUp = (e: MouseEvent) => {
      const dx = e.clientX - dragStartRef.current.mouseX;
      setDragging(null);
      setDragOffset({ leftPx: 0, widthPx: 0 });
      if (Math.abs(dx) < 5 || !onDatesChangeRef.current) return;

      const { origLeftPx, origWidthPx } = dragStartRef.current;
      let newLeftPx: number, newWidthPx: number;
      if (dragging === 'move') {
        newLeftPx = origLeftPx + dx;
        newWidthPx = origWidthPx;
      } else if (dragging === 'start') {
        newLeftPx = origLeftPx + dx;
        newWidthPx = origWidthPx - dx;
      } else {
        newLeftPx = origLeftPx;
        newWidthPx = origWidthPx + dx;
      }
      if (newWidthPx < 10) return;
      const newStart = pxToDateRef.current(newLeftPx);
      const newEnd = pxToDateRef.current(newLeftPx + newWidthPx);
      onDatesChangeRef.current(formatDate(newStart), formatDate(newEnd));
    };
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
    };
  }, [dragging]);

  const handleSetDefaultDates = () => {
    if (!onDatesChangeRef.current) return;
    const today = new Date();
    const twoWeeksLater = new Date(today);
    twoWeeksLater.setDate(today.getDate() + 14);
    onDatesChangeRef.current(formatDate(today), formatDate(twoWeeksLater));
  };

  if (!barStyle) {
    return (
      <div className="absolute inset-0 flex items-center px-2">
        <div className="flex items-center gap-2 w-full">
          <div className="flex-1 h-2 rounded-full bg-muted overflow-hidden">
            <div
              className="h-full rounded-full"
              style={{ width: `${progress ?? 0}%`, backgroundColor: bgColor }}
            />
          </div>
          <span className="text-[10px] text-muted-foreground font-medium whitespace-nowrap">
            {Math.round(progress ?? 0)}%
          </span>
          {onDatesChange && (
            <button
              type="button"
              onClick={handleSetDefaultDates}
              className="text-[10px] text-primary hover:underline italic"
            >
              {t('roadmap.setDates')}
            </button>
          )}
        </div>
      </div>
    );
  }

  const dragStyle = dragging ? {
    left: `calc(${barStyle.left} + ${dragOffset.leftPx}px)`,
    width: `calc(${barStyle.width} + ${dragOffset.widthPx}px)`,
  } : barStyle;

  return (
    <div
      ref={containerRef}
      className={`absolute h-7 rounded-full ${onDatesChange ? 'cursor-grab' : ''} ${dragging === 'move' ? 'cursor-grabbing opacity-60' : ''} group/bar`}
      style={{
        ...dragStyle,
        top: '50%',
        transform: 'translateY(-50%)',
        backgroundColor: bgColor,
        boxShadow: '0 1px 3px rgba(0,0,0,0.15)',
      }}
      onMouseDown={onDatesChange ? (e) => handleMouseDown(e, 'move') : undefined}
    >
      {progress !== undefined && progress > 0 && (
        <div
          className="h-full rounded-l-full pointer-events-none"
          style={{ width: `${progress}%`, backgroundColor: 'rgba(255,255,255,0.3)' }}
        />
      )}
      <span className="absolute inset-0 flex items-center justify-center text-[10px] text-white font-semibold drop-shadow-sm pointer-events-none">
        {Math.round(progress ?? 0)}%
      </span>
      {onDatesChange && (
        <>
          <div
            className="absolute left-0 top-0 bottom-0 w-2 cursor-col-resize opacity-0 group-hover/bar:opacity-100 bg-white/40 rounded-l-full"
            onMouseDown={(e) => handleMouseDown(e, 'start')}
          />
          <div
            className="absolute right-0 top-0 bottom-0 w-2 cursor-col-resize opacity-0 group-hover/bar:opacity-100 bg-white/40 rounded-r-full"
            onMouseDown={(e) => handleMouseDown(e, 'end')}
          />
        </>
      )}
    </div>
  );
}

interface EpicDependencyInfo {
  blockedBy: string[];
  blocking: string[];
}

interface RoadmapEpicBlockProps {
  epic: TimelineEpic;
  /** Prefix used for the epic's row-index key ('epic' inside an initiative, 'orphan' otherwise). */
  rowKeyPrefix: 'epic' | 'orphan';
  nested: boolean;
  isShapeUp: boolean;
  expanded: boolean;
  onToggle: (id: number) => void;
  rowIndices: Map<string, number>;
  timelineStart: Date;
  timelineEnd: Date;
  onDatesChange: (id: number, startDate: string, endDate: string) => void;
  depInfo?: EpicDependencyInfo;
}

const stripe = (rowIndices: Map<string, number>, key: string) =>
  (rowIndices.get(key) ?? 0) % 2 === 1 ? 'bg-muted/20' : '';

/** Number of expandable children an epic has: pitches for Shape Up, tasks otherwise. */
const getEpicChildCount = (epic: TimelineEpic, isShapeUp: boolean) =>
  (isShapeUp ? epic.pitches?.length : epic.tasks?.length) ?? 0;

function RoadmapTaskRow({
  task,
  rowClass,
  indentClass,
  timelineStart,
  timelineEnd,
}: {
  task: TimelineTask;
  rowClass: string;
  indentClass: string;
  timelineStart: Date;
  timelineEnd: Date;
}) {
  const { t } = useTranslation();
  const due = task.dueDate ? parseDate(task.dueDate) : null;
  const totalDays = Math.max(1, Math.ceil((timelineEnd.getTime() - timelineStart.getTime()) / (1000 * 60 * 60 * 24)));
  const pct = due ? ((due.getTime() - timelineStart.getTime()) / (1000 * 60 * 60 * 24) / totalDays) * 100 : null;
  const color = TASK_STATUS_COLORS[task.status] || '#94a3b8';
  return (
    <div className={`flex items-center h-9 hover:bg-muted/50 ${rowClass}`}>
      <div className={`w-72 shrink-0 flex items-center gap-2 py-1 pr-2 ${indentClass}`}>
        <CheckCircle className="h-3 w-3 shrink-0" style={{ color }} />
        <Link
          to={`/tasks/${task.id}`}
          className="text-xs truncate hover:underline"
          onClick={(e) => e.stopPropagation()}
        >
          {task.title}
        </Link>
      </div>
      <div className="flex-1 relative h-9">
        {pct !== null && pct >= 0 && pct <= 100 && (
          <div
            className="absolute top-1/2 h-3 w-3 -translate-x-1/2 -translate-y-1/2 rotate-45 rounded-[2px]"
            style={{ left: `${pct}%`, backgroundColor: color }}
            title={`${t('roadmap.taskDue')}: ${task.dueDate}`}
          />
        )}
      </div>
    </div>
  );
}

/**
 * One epic row plus its expanded children (pitches in Shape Up, tasks in Kanban/Scrum)
 * and an optional dependency summary row. Used both under initiatives and for orphan epics.
 */
function RoadmapEpicBlock({
  epic,
  rowKeyPrefix,
  nested,
  isShapeUp,
  expanded,
  onToggle,
  rowIndices,
  timelineStart,
  timelineEnd,
  onDatesChange,
  depInfo,
}: RoadmapEpicBlockProps) {
  const { t } = useTranslation();
  const hasDeps = !!depInfo && (depInfo.blockedBy.length > 0 || depInfo.blocking.length > 0);
  const childCount = getEpicChildCount(epic, isShapeUp);
  const expandable = childCount > 0 || hasDeps;
  const labelPad = nested ? 'pl-8 pr-2' : 'px-2';
  const childIndent = nested ? 'pl-14' : 'pl-10';
  const items = isShapeUp ? epic.pitches : epic.tasks;
  const doneCount = isShapeUp
    ? (epic.pitches ?? []).filter((p) => p.status === 'DONE').length
    : (epic.tasks ?? []).filter((tk) => tk.status === 'DONE').length;

  return (
    <div>
      <div
        data-epic-row={epic.id}
        className={`flex items-center h-10 hover:bg-muted/50 ${stripe(rowIndices, `${rowKeyPrefix}-${epic.id}`)}`}
      >
        <div className={`w-72 shrink-0 flex items-center gap-2 py-1 ${labelPad}`}>
          <button onClick={() => onToggle(epic.id)} className="flex items-center gap-2 flex-1 min-w-0 text-left">
            {expandable ? (
              expanded ? (
                <ChevronDown className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
              ) : (
                <ChevronRight className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
              )
            ) : (
              <span className="w-3.5 shrink-0" />
            )}
            <div className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: getStatusCssColor(epic.status) }} />
            <Link to={`/epics/${epic.id}`} className="text-sm hover:underline truncate" onClick={(e) => e.stopPropagation()}>
              {epic.name}
            </Link>
          </button>
          {hasDeps && depInfo && (
            <span
              className="shrink-0 text-muted-foreground"
              title={[
                depInfo.blockedBy.length > 0 ? `${t('roadmap.blockedBy')}: ${depInfo.blockedBy.join(', ')}` : '',
                depInfo.blocking.length > 0 ? `${t('roadmap.blocking')}: ${depInfo.blocking.join(', ')}` : '',
              ].filter(Boolean).join(' | ')}
              aria-label={t('roadmap.hasDependencies')}
            >
              {depInfo.blockedBy.length > 0 ? <Ban className="h-3.5 w-3.5 text-orange-500" /> : <ListChecks className="h-3.5 w-3.5" />}
            </span>
          )}
          <Badge variant="outline" className="text-[10px] shrink-0">
            {t(`epics.status.${epic.status.toLowerCase()}`)}
          </Badge>
          {childCount > 0 && (
            <span className="text-[10px] text-muted-foreground shrink-0">
              {doneCount}/{childCount}
            </span>
          )}
        </div>
        <div className="flex-1 relative h-10">
          <TimelineBar
            startDate={epic.startDate}
            endDate={epic.endDate}
            timelineStart={timelineStart}
            timelineEnd={timelineEnd}
            status={epic.status}
            progress={epic.progress}
            onDatesChange={(s, e) => onDatesChange(epic.id, s, e)}
          />
        </div>
      </div>

      {expanded && hasDeps && depInfo && (
        <div className={`flex items-center h-7 text-[11px] text-muted-foreground hover:bg-muted/50 ${stripe(rowIndices, `dep-${epic.id}`)}`}>
          <div className={`w-72 shrink-0 flex flex-col justify-center pr-2 ${childIndent}`}>
            {depInfo.blockedBy.length > 0 && (
              <span className="truncate">{t('roadmap.blockedBy')}: {depInfo.blockedBy.join(', ')}</span>
            )}
            {depInfo.blocking.length > 0 && (
              <span className="truncate">{t('roadmap.blocking')}: {depInfo.blocking.join(', ')}</span>
            )}
          </div>
          <div className="flex-1" />
        </div>
      )}

      {expanded && items && (isShapeUp
        ? (epic.pitches ?? []).map((pitch) => (
            <div key={pitch.id} className={`flex items-center h-9 hover:bg-muted/50 ${stripe(rowIndices, `pitch-${pitch.id}`)}`}>
              <div className={`w-72 shrink-0 flex items-center gap-2 py-1 pr-2 ${childIndent}`}>
                <CheckCircle className="h-3 w-3 text-muted-foreground shrink-0" />
                <Link to={`/pitches/${pitch.id}`} className="text-xs truncate hover:underline" onClick={(e) => e.stopPropagation()}>
                  {pitch.title}
                </Link>
              </div>
              <div className="flex-1 relative h-9">
                <TimelineBar
                  startDate={pitch.startDate}
                  endDate={pitch.endDate}
                  timelineStart={timelineStart}
                  timelineEnd={timelineEnd}
                  status={pitch.status}
                  progress={pitch.progress}
                />
              </div>
            </div>
          ))
        : (epic.tasks ?? []).map((task) => (
            <RoadmapTaskRow
              key={task.id}
              task={task}
              rowClass={stripe(rowIndices, `task-${task.id}`)}
              indentClass={childIndent}
              timelineStart={timelineStart}
              timelineEnd={timelineEnd}
            />
          )))}
    </div>
  );
}

export default function RoadmapPage() {
  const { t, i18n } = useTranslation();
  const { currentProject, isAllProjectsSelected } = useProject();
  const { showError, showSuccess } = useToast();
  const [timeline, setTimeline] = useState<RoadmapTimeline | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<'quarterly' | 'yearly'>('quarterly');
  const [year, setYear] = useState(new Date().getFullYear());
  const [quarter, setQuarter] = useState(Math.ceil((new Date().getMonth() + 1) / 3));
  const [expandedInitiatives, setExpandedInitiatives] = useState<Set<number>>(new Set());
  const [expandedEpics, setExpandedEpics] = useState<Set<number>>(new Set());
  const [isPresentationMode, setIsPresentationMode] = useState(false);

  const loadTimeline = useCallback(async () => {
    if (!currentProject) return;
    try {
      setLoading(true);
      const response = viewMode === 'quarterly'
        ? await roadmapService.getQuarterly(currentProject.id, year, quarter)
        : await roadmapService.getYearly(currentProject.id, year);
      setTimeline(response.data);
    } catch (error) {
      console.error('Failed to load roadmap:', error);
      showError(t('roadmap.loadError'));
    } finally {
      setLoading(false);
    }
  }, [currentProject, viewMode, year, quarter, showError, t]);

  const loadTimelineRef = useRef(loadTimeline);
  loadTimelineRef.current = loadTimeline;

  useEffect(() => {
    if (!currentProject || isAllProjectsSelected) {
      setTimeline(null);
      setLoading(false);
      return;
    }
    loadTimelineRef.current();
  }, [currentProject, isAllProjectsSelected, viewMode, year, quarter]);

  const handleInitiativeDatesChange = useCallback(async (id: number, startDate: string, endDate: string) => {
    try {
      await initiativeService.updateDates(id, startDate, endDate);
      showSuccess(t('roadmap.datesUpdated'));
      loadTimelineRef.current();
    } catch {
      showError(t('roadmap.datesUpdateFailed'));
    }
  }, [t, showSuccess, showError]);

  const handleEpicDatesChange = useCallback(async (id: number, startDate: string, endDate: string) => {
    try {
      await epicService.updateDates(id, startDate, endDate);
      showSuccess(t('roadmap.datesUpdated'));
      loadTimelineRef.current();
    } catch {
      showError(t('roadmap.datesUpdateFailed'));
    }
  }, [t, showSuccess, showError]);

  const timelineStart = useMemo(() => {
    if (!timeline) return new Date();
    // In quarterly view, limit to current quarter only
    if (viewMode === 'quarterly') {
      const quarterStart = new Date(year, (quarter - 1) * 3, 1);
      const apiStart = new Date(timeline.startDate);
      return quarterStart > apiStart ? quarterStart : apiStart;
    }
    return new Date(timeline.startDate);
  }, [timeline, viewMode, year, quarter]);

  const timelineEnd = useMemo(() => {
    if (!timeline) return new Date();
    // In quarterly view, limit to current quarter only
    if (viewMode === 'quarterly') {
      const quarterEnd = new Date(year, quarter * 3, 0); // Last day of quarter
      const apiEnd = new Date(timeline.endDate);
      return quarterEnd < apiEnd ? quarterEnd : apiEnd;
    }
    return new Date(timeline.endDate);
  }, [timeline, viewMode, year, quarter]);

  const monthHeaders = useMemo(() => {
    const headers: { label: string; width: number }[] = [];
    const start = new Date(timelineStart);
    const end = new Date(timelineEnd);
    const totalDays = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));

    let current = new Date(start);
    while (current <= end) {
      const monthStart = new Date(current);
      const nextMonth = new Date(current.getFullYear(), current.getMonth() + 1, 1);
      const monthEnd = nextMonth > end ? end : new Date(nextMonth.getTime() - 1);
      const daysInMonth = Math.ceil((monthEnd.getTime() - monthStart.getTime()) / (1000 * 60 * 60 * 24)) + 1;

      headers.push({
        label: current.toLocaleString('default', { month: 'short', year: '2-digit' }),
        width: (daysInMonth / totalDays) * 100,
      });

      current = nextMonth;
    }
    return headers;
  }, [timelineStart, timelineEnd]);

  const monthBoundaries = useMemo(() => {
    let cumulative = 0;
    return monthHeaders.map(h => {
      const offset = cumulative;
      cumulative += h.width;
      return offset;
    }).slice(1); // skip first (0%)
  }, [monthHeaders]);

  const todayPosition = useMemo(() => {
    const today = new Date();
    const dayMs = 1000 * 60 * 60 * 24;
    const totalDays = Math.ceil((timelineEnd.getTime() - timelineStart.getTime()) / dayMs);
    if (totalDays <= 0) return null;
    const dayOffset = Math.ceil((today.getTime() - timelineStart.getTime()) / dayMs);
    const pct = (dayOffset / totalDays) * 100;
    return pct >= 0 && pct <= 100 ? pct : null;
  }, [timelineStart, timelineEnd]);

  const toggleInitiative = (id: number) => {
    setExpandedInitiatives(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleEpic = (id: number) => {
    setExpandedEpics(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const navigatePeriod = (direction: 'prev' | 'next') => {
    if (viewMode === 'quarterly') {
      if (direction === 'prev') {
        if (quarter === 1) {
          setQuarter(4);
          setYear(year - 1);
        } else {
          setQuarter(quarter - 1);
        }
      } else {
        if (quarter === 4) {
          setQuarter(1);
          setYear(year + 1);
        } else {
          setQuarter(quarter + 1);
        }
      }
    } else {
      setYear(direction === 'prev' ? year - 1 : year + 1);
    }
  };

  // Shape Up shows pitches under epics; Kanban/Scrum show tasks. Prefer the server's answer.
  const isShapeUp = (timeline?.projectType ?? currentProject?.projectType ?? 'SHAPE_UP') === 'SHAPE_UP';

  const allEpics = useMemo(() => {
    const list: TimelineEpic[] = [];
    for (const init of timeline?.initiatives ?? []) list.push(...(init.epics ?? []));
    list.push(...(timeline?.orphanEpics ?? []));
    return list;
  }, [timeline]);

  // Per-epic "blocked by / blocking" names, shown on the epic row (readable on mobile and when collapsed)
  const depInfoByEpic = useMemo(() => {
    const names = new Map(allEpics.map((e) => [e.id, e.name]));
    const info = new Map<number, EpicDependencyInfo>();
    const get = (id: number) => {
      let v = info.get(id);
      if (!v) {
        v = { blockedBy: [], blocking: [] };
        info.set(id, v);
      }
      return v;
    };
    for (const d of timeline?.dependencies ?? []) {
      const src = names.get(d.sourceEpicId);
      const tgt = names.get(d.targetEpicId);
      if (src === undefined || tgt === undefined) continue;
      get(d.targetEpicId).blockedBy.push(src);
      get(d.sourceEpicId).blocking.push(tgt);
    }
    return info;
  }, [timeline, allEpics]);

  // Pre-compute flat row indices for stable striping (must be before early returns)
  const rowIndices = useMemo(() => {
    const indices = new Map<string, number>();
    let idx = 0;
    const addEpic = (prefix: 'epic' | 'orphan', epic: TimelineEpic) => {
      indices.set(`${prefix}-${epic.id}`, idx++);
      if (!expandedEpics.has(epic.id)) return;
      if (depInfoByEpic.has(epic.id)) indices.set(`dep-${epic.id}`, idx++);
      if (isShapeUp) {
        for (const pitch of epic.pitches ?? []) indices.set(`pitch-${pitch.id}`, idx++);
      } else {
        for (const task of epic.tasks ?? []) indices.set(`task-${task.id}`, idx++);
      }
    };
    for (const init of timeline?.initiatives ?? []) {
      indices.set(`init-${init.id}`, idx++);
      if (expandedInitiatives.has(init.id)) {
        for (const epic of init.epics ?? []) addEpic('epic', epic);
      }
    }
    for (const epic of timeline?.orphanEpics ?? []) addEpic('orphan', epic);
    return indices;
  }, [timeline, expandedInitiatives, expandedEpics, isShapeUp, depInfoByEpic]);

  // ---- Dependency overlay: measure visible epic rows, then draw connectors ----
  const [showDependencies, setShowDependencies] = useState(true);
  const rowsRef = useRef<HTMLDivElement>(null);
  const [overlay, setOverlay] = useState<{ width: number; height: number; centers: [number, number][] }>({
    width: 0,
    height: 0,
    centers: [],
  });

  useLayoutEffect(() => {
    const el = rowsRef.current;
    if (!el) return;
    const measure = () => {
      const box = el.getBoundingClientRect();
      const centers: [number, number][] = [];
      el.querySelectorAll<HTMLElement>('[data-epic-row]').forEach((row) => {
        const r = row.getBoundingClientRect();
        centers.push([Number(row.dataset.epicRow), Math.round(r.top - box.top + r.height / 2)]);
      });
      const next = { width: Math.max(0, Math.round(box.width) - 288), height: Math.round(box.height), centers };
      setOverlay((prev) => (JSON.stringify(prev) === JSON.stringify(next) ? prev : next));
    };
    measure();
    if (typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    return () => ro.disconnect();
  }, [timeline, expandedInitiatives, expandedEpics, isShapeUp, loading, viewMode, showDependencies]);

  const connectors = useMemo(() => {
    if (!showDependencies || !timeline?.dependencies?.length || overlay.width <= 0) return [];
    return computeConnectors(
      timeline.dependencies,
      new Map(allEpics.map((e) => [e.id, e])),
      new Map(overlay.centers),
      timelineStart,
      timelineEnd,
      overlay.width
    );
  }, [showDependencies, timeline, allEpics, overlay, timelineStart, timelineEnd]);

  if (isAllProjectsSelected) {
    return (
      <ProjectRequiredDialog
        open={true}
        featureDescription={t('roadmap.selectProjectDescription')}
      />
    );
  }

  if (loading) {
    return (
      <div className="container mx-auto py-8 space-y-4">
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Target className="h-6 w-6" />
            {t('roadmap.title')}
          </h1>
          <p className="text-muted-foreground">
            {currentProject?.name} - {viewMode === 'quarterly' ? `Q${quarter} ${year}` : year}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Select value={viewMode} onValueChange={(v) => setViewMode(v as 'quarterly' | 'yearly')}>
            <SelectTrigger className="w-full sm:w-[160px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="quarterly">{t('roadmap.quarterly')}</SelectItem>
              <SelectItem value="yearly">{t('roadmap.yearly')}</SelectItem>
            </SelectContent>
          </Select>

          <Button variant="outline" size="icon" onClick={() => navigatePeriod('prev')}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button variant="outline" size="icon" onClick={() => navigatePeriod('next')}>
            <ChevronRight className="h-4 w-4" />
          </Button>

          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => setIsPresentationMode(true)}
                >
                  <Maximize2 className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>
                <p>{t('roadmap.enterPresentation')}</p>
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>
      </div>

      {/* Releases Timeline */}
      {timeline?.releases && timeline.releases.length > 0 && (
        <Card>
          <CardHeader className="py-3">
            <CardTitle className="text-sm flex items-center gap-2">
              <Package className="h-4 w-4" />
              {t('roadmap.releases')}
            </CardTitle>
          </CardHeader>
          <CardContent className="pb-4">
            <div className="relative overflow-x-auto">
              <div className="min-w-[600px]">
              {/* Month headers */}
              <div className="flex border-b mb-2">
                {monthHeaders.map((h, i) => (
                  <div key={i} className="text-xs text-muted-foreground text-center py-1" style={{ width: `${h.width}%` }}>
                    {h.label}
                  </div>
                ))}
              </div>

              {/* Release markers */}
              <div className="relative h-16">
                {timeline.releases.map((release) => {
                  const releaseDate = parseDate(release.targetDate);
                  if (!releaseDate) return null;

                  const totalDays = Math.ceil((timelineEnd.getTime() - timelineStart.getTime()) / (1000 * 60 * 60 * 24));
                  const dayOffset = Math.ceil((releaseDate.getTime() - timelineStart.getTime()) / (1000 * 60 * 60 * 24));
                  const left = (dayOffset / totalDays) * 100;

                  return (
                    <TooltipProvider key={release.id}>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <div
                            className="absolute flex flex-col items-center cursor-pointer hover:opacity-80"
                            style={{ left: `${left}%`, transform: 'translateX(-50%)' }}
                          >
                            <Link
                              to={`/releases-management/${release.id}`}
                              className="flex flex-col items-center"
                            >
                              <Flag className={`h-5 w-5 ${release.status === 'RELEASED' ? 'text-green-600' : 'text-blue-600'}`} />
                              <span className="text-xs font-medium">{release.version}</span>
                              <Badge variant={getRiskBadgeVariant(release.riskLevel)} className="text-[10px] py-0">
                                {release.riskLevel}
                              </Badge>
                            </Link>
                          </div>
                        </TooltipTrigger>
                        <TooltipContent>
                          <div className="text-sm space-y-1">
                            <p className="font-medium">{release.name} ({release.version})</p>
                            <p className="text-muted-foreground">{t('roadmap.targetDate')}: {formatLocalizedDate(release.targetDate || '', i18n.language)}</p>
                            <p>{t('roadmap.progress')}: {Math.round(release.progressPercentage || 0)}%</p>
                            <div className="flex gap-3 text-xs text-muted-foreground pt-1 border-t">
                              {(release.pitchCount ?? 0) > 0 && (
                                <span>{t('roadmap.releasePitches', { count: release.pitchCount })}</span>
                              )}
                              {(release.taskCount ?? 0) > 0 && (
                                <span>{t('roadmap.releaseTasks', { count: release.taskCount })}</span>
                              )}
                              {(release.bugCount ?? 0) > 0 && (
                                <span>{t('roadmap.releaseBugs', { count: release.bugCount })}</span>
                              )}
                              {(release.pitchCount ?? 0) === 0 && (release.taskCount ?? 0) === 0 && (release.bugCount ?? 0) === 0 && (
                                <span>{t('roadmap.releaseNoItems')}</span>
                              )}
                            </div>
                          </div>
                        </TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                  );
                })}
              </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Initiatives & Epics Timeline */}
      <Card>
        <CardHeader className="py-3">
          <CardTitle className="text-sm flex items-center gap-2">
            <Layers className="h-4 w-4" />
            {t('roadmap.initiativesAndEpics')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
          <div className="min-w-[700px]">
          {/* Month headers */}
          <div className="flex border-b">
            <div className="w-72 shrink-0" />
            <div className="flex-1 flex">
              {monthHeaders.map((h, i) => (
                <div
                  key={i}
                  className={`text-xs font-medium text-muted-foreground text-center py-2 ${i % 2 === 1 ? 'bg-muted/20' : ''}`}
                  style={{ width: `${h.width}%` }}
                >
                  {h.label}
                </div>
              ))}
            </div>
          </div>

          {/* Initiatives rows with grid overlay */}
          <div className="relative" ref={rowsRef}>
            {/* Grid lines overlay — spans full height, aligned with timeline area */}
            <div className="absolute top-0 bottom-0 left-72 right-0 flex pointer-events-none">
              {/* Month boundary lines */}
              {monthBoundaries.map((pct, i) => (
                <div
                  key={`grid-${i}`}
                  className="absolute top-0 bottom-0 w-px bg-border/40"
                  style={{ left: `${pct}%` }}
                />
              ))}
              {/* Alternating month backgrounds */}
              {(() => {
                let cumulative = 0;
                return monthHeaders.map((h, i) => {
                  const left = cumulative;
                  cumulative += h.width;
                  if (i % 2 !== 1) return null;
                  return (
                    <div
                      key={`month-bg-${i}`}
                      className="absolute top-0 bottom-0 bg-muted/10"
                      style={{ left: `${left}%`, width: `${h.width}%` }}
                    />
                  );
                });
              })()}
              {/* Today marker */}
              {todayPosition !== null && (
                <div
                  className="absolute top-0 bottom-0 z-10 flex flex-col items-center"
                  style={{ left: `${todayPosition}%` }}
                >
                  <div className="px-1 py-0.5 rounded text-[9px] font-semibold text-red-600 bg-red-50 border border-red-200 -translate-x-1/2 whitespace-nowrap">
                    {t('roadmap.today')}
                  </div>
                  <div className="w-0.5 flex-1 bg-red-500/70" />
                </div>
              )}
            </div>

            {/* Epic dependency connectors */}
            {connectors.length > 0 && (
              <svg
                className="absolute top-0 left-72 z-20 pointer-events-none overflow-visible"
                width={overlay.width}
                height={overlay.height}
                aria-hidden="true"
              >
                <defs>
                  <marker id="dep-arrow" viewBox="0 0 8 8" refX="7" refY="4" markerWidth="6" markerHeight="6" orient="auto">
                    <path d="M0,0 L8,4 L0,8 z" className="fill-slate-500 dark:fill-slate-300" />
                  </marker>
                  <marker id="dep-arrow-conflict" viewBox="0 0 8 8" refX="7" refY="4" markerWidth="6" markerHeight="6" orient="auto">
                    <path d="M0,0 L8,4 L0,8 z" className="fill-red-500" />
                  </marker>
                </defs>
                {connectors.map((c) => (
                  <path
                    key={c.id}
                    d={c.path}
                    fill="none"
                    strokeWidth={1.5}
                    className={c.conflict ? 'stroke-red-500' : 'stroke-slate-500 dark:stroke-slate-300'}
                    markerEnd={`url(#${c.conflict ? 'dep-arrow-conflict' : 'dep-arrow'})`}
                  />
                ))}
              </svg>
            )}

            {/* Data rows */}
            <div>
              {timeline?.initiatives.map((initiative) => (
                  <div key={initiative.id}>
                    {/* Initiative row */}
                    <div className={`flex items-center h-12 hover:bg-muted/50 ${(rowIndices.get(`init-${initiative.id}`) ?? 0) % 2 === 1 ? 'bg-muted/20' : ''}`}>
                      <div className="w-72 shrink-0 flex items-center gap-2 py-2 px-2">
                        <button
                          onClick={() => toggleInitiative(initiative.id)}
                          className="flex items-center gap-2 flex-1 min-w-0 text-left"
                        >
                          {initiative.epics && initiative.epics.length > 0 ? (
                            expandedInitiatives.has(initiative.id) ? (
                              <ChevronDown className="h-4 w-4 shrink-0 text-muted-foreground" />
                            ) : (
                              <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" />
                            )
                          ) : (
                            <span className="w-4 shrink-0" />
                          )}
                          <div
                            className="w-3 h-3 rounded-full shrink-0"
                            style={{ backgroundColor: initiative.color || getStatusCssColor(initiative.status) }}
                          />
                          <Link
                            to={`/initiatives/${initiative.id}`}
                            className="font-medium hover:underline truncate"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {initiative.name}
                          </Link>
                        </button>
                        <Badge variant="outline" className="text-[10px] shrink-0">
                          {t(`initiatives.status.${initiative.status.toLowerCase()}`)}
                        </Badge>
                        {initiative.epics && initiative.epics.length > 0 && (
                          <span className="text-[10px] text-muted-foreground shrink-0">
                            {t('roadmap.epicCount', { count: initiative.epics.length })}
                          </span>
                        )}
                      </div>
                      <div className="flex-1 relative h-12">
                        <TimelineBar
                          startDate={initiative.startDate}
                          endDate={initiative.endDate}
                          timelineStart={timelineStart}
                          timelineEnd={timelineEnd}
                          status={initiative.status}
                          color={initiative.color}
                          progress={initiative.progress}
                          onDatesChange={(s, e) => handleInitiativeDatesChange(initiative.id, s, e)}
                        />
                      </div>
                    </div>

                    {/* Expanded Epics */}
                    {expandedInitiatives.has(initiative.id) && initiative.epics?.map((epic) => (
                      <RoadmapEpicBlock
                        key={epic.id}
                        epic={epic}
                        rowKeyPrefix="epic"
                        nested
                        isShapeUp={isShapeUp}
                        expanded={expandedEpics.has(epic.id)}
                        onToggle={toggleEpic}
                        rowIndices={rowIndices}
                        timelineStart={timelineStart}
                        timelineEnd={timelineEnd}
                        onDatesChange={handleEpicDatesChange}
                        depInfo={depInfoByEpic.get(epic.id)}
                      />
                    ))}
                  </div>
              ))}

              {/* Orphan Epics */}
              {timeline?.orphanEpics && timeline.orphanEpics.length > 0 && (
                <div className="border-t mt-4">
                  {/* Section header matching initiative group style */}
                  <div className="flex items-center h-10 bg-muted/30 border-b">
                    <div className="w-72 shrink-0 flex items-center gap-2 px-2">
                      <span className="text-sm font-medium text-muted-foreground">{t('roadmap.orphanEpics')}</span>
                      <Badge variant="outline" className="text-[10px]">{timeline.orphanEpics.length}</Badge>
                    </div>
                    <div className="flex-1" />
                  </div>
                  {timeline.orphanEpics.map((epic) => (
                    <RoadmapEpicBlock
                      key={epic.id}
                      epic={epic}
                      rowKeyPrefix="orphan"
                      nested={false}
                      isShapeUp={isShapeUp}
                      expanded={expandedEpics.has(epic.id)}
                      onToggle={toggleEpic}
                      rowIndices={rowIndices}
                      timelineStart={timelineStart}
                      timelineEnd={timelineEnd}
                      onDatesChange={handleEpicDatesChange}
                      depInfo={depInfoByEpic.get(epic.id)}
                    />
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Empty state */}
          {(!timeline?.initiatives || timeline.initiatives.length === 0) &&
           (!timeline?.orphanEpics || timeline.orphanEpics.length === 0) && (
            <div className="py-8 text-center text-muted-foreground">
              <Target className="h-8 w-8 mx-auto mb-2" />
              <p>{t('roadmap.noItems')}</p>
              <div className="flex gap-2 justify-center mt-4">
                <Button variant="outline" asChild>
                  <Link to="/initiatives/new">{t('roadmap.createInitiative')}</Link>
                </Button>
                <Button variant="outline" asChild>
                  <Link to="/releases-management/new">{t('roadmap.createRelease')}</Link>
                </Button>
              </div>
            </div>
          )}
          </div>
          </div>
        </CardContent>
      </Card>

      {/* Legend */}
      <Card>
        <CardContent className="py-3">
          <div className="flex flex-wrap gap-4 text-xs">
            <span className="font-medium">{t('roadmap.legend')}:</span>
            {/* Lifecycle states */}
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: '#94a3b8' }} />
              <span>{t('roadmap.draft')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: '#3b82f6' }} />
              <span>{t('roadmap.planned')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: '#60a5fa' }} />
              <span>{t('roadmap.planning')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: '#8b5cf6' }} />
              <span>{t('roadmap.inProgress')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: '#a855f7' }} />
              <span>{t('roadmap.staging')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: '#22c55e' }} />
              <span>{t('roadmap.completed')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: '#16a34a' }} />
              <span>{t('roadmap.released')}</span>
            </div>
            {/* Exception states */}
            <div className="w-px self-stretch bg-border" />
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: '#f97316' }} />
              <span>{t('roadmap.onHold')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded-full" style={{ backgroundColor: '#ef4444' }} />
              <span>{t('roadmap.cancelled')}</span>
            </div>
            {/* Dependencies */}
            <div className="w-px self-stretch bg-border" />
            <label className="flex items-center gap-2 cursor-pointer">
              <Switch checked={showDependencies} onCheckedChange={setShowDependencies} aria-label={t('roadmap.showDependencies')} />
              <span>{t('roadmap.showDependencies')}</span>
            </label>
            <div className="flex items-center gap-1">
              <span className="inline-block w-5 border-t-2 border-slate-500 dark:border-slate-300" />
              <span>{t('roadmap.dependencyBlocks')}</span>
            </div>
            <div className="flex items-center gap-1">
              <AlertTriangle className="h-3 w-3 text-red-500" />
              <span className="inline-block w-5 border-t-2 border-red-500" />
              <span>{t('roadmap.dependencyConflict')}</span>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Presentation Mode */}
      <RoadmapPresentationMode
        isOpen={isPresentationMode}
        onClose={() => setIsPresentationMode(false)}
        timeline={timeline}
        viewMode={viewMode}
        year={year}
        quarter={quarter}
        onViewModeChange={(mode) => setViewMode(mode)}
        onNavigate={navigatePeriod}
        expandedInitiatives={expandedInitiatives}
        expandedEpics={expandedEpics}
        onToggleInitiative={toggleInitiative}
        onToggleEpic={toggleEpic}
        projectName={currentProject?.name}
        isShapeUp={isShapeUp}
      />
    </div>
  );
}
