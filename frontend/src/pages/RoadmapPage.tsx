import { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  ChevronLeft,
  ChevronRight,
  Target,
  Layers,
  FileText,
  Package,
  CheckCircle,
  Flag,
  Maximize2,
} from 'lucide-react';
import { roadmapService } from '../services/roadmapService';
import {
  RoadmapTimeline,
} from '../types';
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
  if (!startDate && !endDate) return null;
  
  const totalDays = Math.ceil((timelineEnd.getTime() - timelineStart.getTime()) / (1000 * 60 * 60 * 24));
  const start = startDate || endDate!;
  const end = endDate || startDate!;
  
  const startOffset = Math.ceil((start.getTime() - timelineStart.getTime()) / (1000 * 60 * 60 * 24));
  const duration = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;
  
  const left = Math.max(0, (startOffset / totalDays) * 100);
  const width = Math.min((duration / totalDays) * 100, 100 - left);
  
  return {
    left: `${left}%`,
    width: `${Math.max(width, 2)}%`, // Minimum 2% width for visibility
  };
};

const getStatusColor = (status: string): string => {
  const colors: Record<string, string> = {
    DRAFT: 'bg-gray-400',
    PLANNED: 'bg-blue-500',
    IN_PROGRESS: 'bg-yellow-500',
    COMPLETED: 'bg-green-500',
    ON_HOLD: 'bg-orange-500',
    CANCELLED: 'bg-red-500',
    PLANNING: 'bg-blue-400',
    STAGING: 'bg-purple-500',
    RELEASED: 'bg-green-600',
  };
  return colors[status] || 'bg-gray-500';
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
}

function TimelineBar({ startDate, endDate, timelineStart, timelineEnd, status, color, progress }: TimelineBarProps) {
  const start = parseDate(startDate);
  const end = parseDate(endDate);
  const barStyle = calculateBarStyle(start, end, timelineStart, timelineEnd);
  
  if (!barStyle) return null;
  
  return (
    <div
      className={`absolute h-6 rounded ${color || getStatusColor(status)} opacity-80`}
      style={{ ...barStyle, top: '50%', transform: 'translateY(-50%)' }}
    >
      {progress !== undefined && (
        <div
          className="h-full bg-white/30 rounded-l"
          style={{ width: `${progress}%` }}
        />
      )}
    </div>
  );
}

export default function RoadmapPage() {
  const { t, i18n } = useTranslation();
  const { currentProject, isAllProjectsSelected } = useProject();
  const { showError } = useToast();
  const [timeline, setTimeline] = useState<RoadmapTimeline | null>(null);
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<'quarterly' | 'yearly'>('quarterly');
  const [year, setYear] = useState(new Date().getFullYear());
  const [quarter, setQuarter] = useState(Math.ceil((new Date().getMonth() + 1) / 3));
  const [expandedInitiatives, setExpandedInitiatives] = useState<Set<number>>(new Set());
  const [expandedEpics, setExpandedEpics] = useState<Set<number>>(new Set());
  const [isPresentationMode, setIsPresentationMode] = useState(false);

  useEffect(() => {
    if (!currentProject || isAllProjectsSelected) {
      setTimeline(null);
      setLoading(false);
      return;
    }
    loadTimeline();
  }, [currentProject, isAllProjectsSelected, viewMode, year, quarter]);

  const loadTimeline = async () => {
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
  };

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
                          <div className="text-sm">
                            <p className="font-medium">{release.name} ({release.version})</p>
                            <p className="text-muted-foreground">{t('roadmap.targetDate')}: {formatLocalizedDate(release.targetDate || '', i18n.language)}</p>
                            <p>{t('roadmap.progress')}: {Math.round(release.progressPercentage || 0)}%</p>
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
          <div className="flex border-b mb-2">
            <div className="w-72 shrink-0" />
            {monthHeaders.map((h, i) => (
              <div key={i} className="text-xs text-muted-foreground text-center py-1" style={{ width: `${h.width}%` }}>
                {h.label}
              </div>
            ))}
          </div>
          
          {/* Initiatives */}
          <div className="space-y-2">
            {timeline?.initiatives.map((initiative) => (
              <div key={initiative.id}>
                {/* Initiative row */}
                <div className="flex items-center hover:bg-muted/50 rounded">
                  <div className="w-72 shrink-0 flex items-center gap-2 py-2 px-2">
                    <button
                      onClick={() => toggleInitiative(initiative.id)}
                      className="flex items-center gap-2 flex-1 min-w-0 text-left"
                    >
                      <div
                        className="w-3 h-3 rounded-full shrink-0"
                        style={{ backgroundColor: initiative.color || '#6366f1' }}
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
                  </div>
                  <div className="flex-1 relative h-8">
                    <TimelineBar
                      startDate={initiative.startDate}
                      endDate={initiative.endDate}
                      timelineStart={timelineStart}
                      timelineEnd={timelineEnd}
                      status={initiative.status}
                      color={initiative.color}
                      progress={initiative.progress}
                    />
                  </div>
                </div>
                
                {/* Expanded Epics */}
                {expandedInitiatives.has(initiative.id) && initiative.epics?.map((epic) => (
                  <div key={epic.id}>
                    {/* Epic row */}
                    <div className="flex items-center hover:bg-muted/50 rounded ml-4">
                      <div className="w-68 shrink-0 flex items-center gap-2 py-1 px-2">
                        <button
                          onClick={() => toggleEpic(epic.id)}
                          className="flex items-center gap-2 flex-1 min-w-0 text-left"
                        >
                          <FileText className="h-3 w-3 text-muted-foreground shrink-0" />
                          <Link
                            to={`/epics/${epic.id}`}
                            className="text-sm hover:underline truncate"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {epic.name}
                          </Link>
                        </button>
                        <Badge variant="outline" className="text-[10px] shrink-0">
                          {t(`epics.status.${epic.status.toLowerCase()}`)}
                        </Badge>
                      </div>
                      <div className="flex-1 relative h-6">
                        <TimelineBar
                          startDate={epic.startDate}
                          endDate={epic.endDate}
                          timelineStart={timelineStart}
                          timelineEnd={timelineEnd}
                          status={epic.status}
                          color={epic.color}
                          progress={epic.progress}
                        />
                      </div>
                    </div>
                    
                    {/* Expanded Pitches */}
                    {expandedEpics.has(epic.id) && epic.pitches?.map((pitch) => (
                      <div key={pitch.id} className="flex items-center hover:bg-muted/50 rounded ml-8">
                        <div className="w-40 shrink-0 flex items-center gap-2 py-1 px-2">
                          <CheckCircle className="h-3 w-3 text-muted-foreground" />
                          <Link
                            to={`/pitches/${pitch.id}`}
                            className="text-xs truncate hover:underline"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {pitch.title}
                          </Link>
                        </div>
                        <div className="flex-1 relative h-5">
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
                    ))}
                  </div>
                ))}
              </div>
            ))}
            
            {/* Orphan Epics */}
            {timeline?.orphanEpics && timeline.orphanEpics.length > 0 && (
              <div className="border-t pt-2 mt-2">
                <p className="text-xs text-muted-foreground px-2 mb-2">{t('roadmap.unassignedEpics')}</p>
                {timeline.orphanEpics.map((epic) => (
                  <div key={epic.id}>
                    <div className="flex items-center hover:bg-muted/50 rounded">
                      <div className="w-72 shrink-0 flex items-center gap-2 py-1 px-2">
                        <button
                          onClick={() => toggleEpic(epic.id)}
                          className="flex items-center gap-2 flex-1 min-w-0 text-left"
                        >
                          <FileText className="h-3 w-3 text-muted-foreground shrink-0" />
                          <Link
                            to={`/epics/${epic.id}`}
                            className="text-sm hover:underline truncate"
                            onClick={(e) => e.stopPropagation()}
                          >
                            {epic.name}
                          </Link>
                        </button>
                        <Badge variant="outline" className="text-[10px] shrink-0">
                          {t(`epics.status.${epic.status.toLowerCase()}`)}
                        </Badge>
                      </div>
                      <div className="flex-1 relative h-6">
                        <TimelineBar
                          startDate={epic.startDate}
                          endDate={epic.endDate}
                          timelineStart={timelineStart}
                          timelineEnd={timelineEnd}
                          status={epic.status}
                          color={epic.color}
                          progress={epic.progress}
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
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
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded bg-gray-400" />
              <span>{t('roadmap.draft')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded bg-blue-500" />
              <span>{t('roadmap.planned')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded bg-yellow-500" />
              <span>{t('roadmap.inProgress')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded bg-green-500" />
              <span>{t('roadmap.completed')}</span>
            </div>
            <div className="flex items-center gap-1">
              <div className="w-3 h-3 rounded bg-orange-500" />
              <span>{t('roadmap.onHold')}</span>
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
      />
    </div>
  );
}
