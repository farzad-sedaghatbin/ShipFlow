import { useEffect, useCallback, useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  ChevronLeft,
  ChevronRight,
  X,
  Target,
  Layers,
  FileText,
  Package,
  CheckCircle,
  Flag,
  Moon,
  Sun,
  Maximize2,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { RoadmapTimeline } from '../types';

import { Button } from './ui/button';
import { Badge } from './ui/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';

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
    width: `${Math.max(width, 2)}%`,
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

interface PresentationTimelineBarProps {
  startDate?: string;
  endDate?: string;
  timelineStart: Date;
  timelineEnd: Date;
  status: string;
  color?: string;
  progress?: number;
  height?: string;
}

function PresentationTimelineBar({ 
  startDate, 
  endDate, 
  timelineStart, 
  timelineEnd, 
  status, 
  color, 
  progress,
  height = 'h-8'
}: PresentationTimelineBarProps) {
  const start = parseDate(startDate);
  const end = parseDate(endDate);
  const barStyle = calculateBarStyle(start, end, timelineStart, timelineEnd);
  
  if (!barStyle) return null;
  
  return (
    <div
      className={`absolute ${height} rounded-md ${color || getStatusColor(status)} opacity-90 shadow-md`}
      style={{ ...barStyle, top: '50%', transform: 'translateY(-50%)' }}
    >
      {progress !== undefined && (
        <div
          className="h-full bg-white/30 rounded-l-md"
          style={{ width: `${progress}%` }}
        />
      )}
    </div>
  );
}

interface RoadmapPresentationModeProps {
  isOpen: boolean;
  onClose: () => void;
  timeline: RoadmapTimeline | null;
  viewMode: 'quarterly' | 'yearly';
  year: number;
  quarter: number;
  onViewModeChange: (mode: 'quarterly' | 'yearly') => void;
  onNavigate: (direction: 'prev' | 'next') => void;
  expandedInitiatives: Set<number>;
  expandedEpics: Set<number>;
  onToggleInitiative: (id: number) => void;
  onToggleEpic: (id: number) => void;
  projectName?: string;
}

export default function RoadmapPresentationMode({
  isOpen,
  onClose,
  timeline,
  viewMode,
  year,
  quarter,
  onViewModeChange,
  onNavigate,
  expandedInitiatives,
  expandedEpics,
  onToggleInitiative,
  onToggleEpic,
  projectName,
}: RoadmapPresentationModeProps) {
  const { t, i18n } = useTranslation();
  const [isDarkMode, setIsDarkMode] = useState(true);

  // Calculate timeline bounds
  const timelineStart = useMemo(() => {
    if (!timeline) return new Date();
    if (viewMode === 'quarterly') {
      const quarterStart = new Date(year, (quarter - 1) * 3, 1);
      const apiStart = new Date(timeline.startDate);
      return quarterStart > apiStart ? quarterStart : apiStart;
    }
    return new Date(timeline.startDate);
  }, [timeline, viewMode, year, quarter]);

  const timelineEnd = useMemo(() => {
    if (!timeline) return new Date();
    if (viewMode === 'quarterly') {
      const quarterEnd = new Date(year, quarter * 3, 0);
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

  // Keyboard navigation
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    if (!isOpen) return;
    
    switch (e.key) {
      case 'Escape':
        onClose();
        break;
      case 'ArrowLeft':
        e.preventDefault();
        onNavigate('prev');
        break;
      case 'ArrowRight':
        e.preventDefault();
        onNavigate('next');
        break;
      case 'd':
      case 'D':
        setIsDarkMode(prev => !prev);
        break;
      case 'v':
      case 'V':
        onViewModeChange(viewMode === 'quarterly' ? 'yearly' : 'quarterly');
        break;
    }
  }, [isOpen, onClose, onNavigate, viewMode, onViewModeChange]);

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [handleKeyDown]);

  // Prevent body scroll when open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const periodLabel = viewMode === 'quarterly' ? `Q${quarter} ${year}` : `${year}`;

  const content = (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.3 }}
          className={`fixed inset-0 z-50 ${
            isDarkMode 
              ? 'bg-slate-900 text-white' 
              : 'bg-white text-slate-900'
          }`}
        >
          {/* Header */}
          <motion.div 
            initial={{ y: -20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.1 }}
            className={`px-8 py-6 border-b ${isDarkMode ? 'border-slate-700' : 'border-slate-200'}`}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <Target className="h-10 w-10" />
                <div>
                  <h1 className="text-3xl font-bold">{t('roadmap.title')}</h1>
                  <p className={`text-lg ${isDarkMode ? 'text-slate-400' : 'text-slate-600'}`}>
                    {projectName} — {periodLabel}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Badge 
                  variant="outline" 
                  className={`text-sm px-3 py-1 ${isDarkMode ? 'border-slate-600 text-slate-300' : ''}`}
                >
                  <Maximize2 className="h-4 w-4 mr-2" />
                  {t('roadmap.presentationMode')}
                </Badge>
              </div>
            </div>
          </motion.div>

          {/* Main Content */}
          <div className="h-[calc(100vh-180px)] overflow-auto px-8 py-6">
            {/* Releases Timeline */}
            {timeline?.releases && timeline.releases.length > 0 && (
              <motion.div 
                initial={{ y: 20, opacity: 0 }}
                animate={{ y: 0, opacity: 1 }}
                transition={{ delay: 0.2 }}
                className={`rounded-xl p-6 mb-6 ${isDarkMode ? 'bg-slate-800' : 'bg-slate-100'}`}
              >
                <h2 className={`text-xl font-semibold mb-4 flex items-center gap-2 ${isDarkMode ? 'text-slate-200' : 'text-slate-700'}`}>
                  <Package className="h-6 w-6" />
                  {t('roadmap.releases')}
                </h2>
                <div className="relative">
                  {/* Month headers */}
                  <div className={`flex border-b mb-4 ${isDarkMode ? 'border-slate-700' : 'border-slate-300'}`}>
                    {monthHeaders.map((h, i) => (
                      <div 
                        key={i} 
                        className={`text-base font-medium text-center py-2 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`} 
                        style={{ width: `${h.width}%` }}
                      >
                        {h.label}
                      </div>
                    ))}
                  </div>
                  
                  {/* Release markers */}
                  <div className="relative h-20">
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
                                className="absolute flex flex-col items-center cursor-pointer hover:scale-110 transition-transform"
                                style={{ left: `${left}%`, transform: 'translateX(-50%)' }}
                              >
                                <Flag className={`h-8 w-8 ${release.status === 'RELEASED' ? 'text-green-500' : 'text-blue-500'}`} />
                                <span className="text-base font-medium mt-1">{release.version}</span>
                                <Badge variant={getRiskBadgeVariant(release.riskLevel)} className="text-xs">
                                  {release.riskLevel}
                                </Badge>
                              </div>
                            </TooltipTrigger>
                            <TooltipContent className="text-base p-4 space-y-1">
                              <p className="font-bold text-lg">{release.name} ({release.version})</p>
                              <p>{t('roadmap.targetDate')}: {formatLocalizedDate(release.targetDate || '', i18n.language)}</p>
                              <p>{t('roadmap.progress')}: {Math.round(release.progressPercentage || 0)}%</p>
                              <div className="flex gap-3 text-sm text-muted-foreground pt-1 border-t">
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
                            </TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                      );
                    })}
                  </div>
                </div>
              </motion.div>
            )}

            {/* Initiatives & Epics Timeline */}
            <motion.div 
              initial={{ y: 20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.3 }}
              className={`rounded-xl p-6 ${isDarkMode ? 'bg-slate-800' : 'bg-slate-100'}`}
            >
              <h2 className={`text-xl font-semibold mb-4 flex items-center gap-2 ${isDarkMode ? 'text-slate-200' : 'text-slate-700'}`}>
                <Layers className="h-6 w-6" />
                {t('roadmap.initiativesAndEpics')}
              </h2>
              
              {/* Month headers */}
              <div className={`flex border-b mb-4 ${isDarkMode ? 'border-slate-700' : 'border-slate-300'}`}>
                <div className="w-80 shrink-0" />
                {monthHeaders.map((h, i) => (
                  <div 
                    key={i} 
                    className={`text-base font-medium text-center py-2 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`} 
                    style={{ width: `${h.width}%` }}
                  >
                    {h.label}
                  </div>
                ))}
              </div>
              
              {/* Initiatives */}
              <div className="space-y-3">
                {timeline?.initiatives.map((initiative) => (
                  <div key={initiative.id}>
                    {/* Initiative row */}
                    <div 
                      className={`flex items-center rounded-lg cursor-pointer transition-colors ${
                        isDarkMode ? 'hover:bg-slate-700' : 'hover:bg-slate-200'
                      }`}
                      onClick={() => onToggleInitiative(initiative.id)}
                    >
                      <div className="w-80 shrink-0 flex items-center gap-3 py-3 px-3">
                        <div
                          className="w-4 h-4 rounded-full shrink-0 shadow-md"
                          style={{ backgroundColor: initiative.color || '#6366f1' }}
                        />
                        <span className="font-semibold text-lg truncate">{initiative.name}</span>
                        <Badge
                          variant="outline"
                          className={`text-sm shrink-0 ${isDarkMode ? 'border-slate-600' : ''}`}
                        >
                          {t(`initiatives.status.${initiative.status.toLowerCase()}`)}
                        </Badge>
                        {initiative.quarterLabel && (
                          <Badge variant="secondary" className={`text-xs shrink-0 font-normal ${isDarkMode ? 'bg-slate-700' : ''}`}>
                            {initiative.quarterLabel}
                          </Badge>
                        )}
                      </div>
                      <div className="flex-1 relative h-12">
                        <PresentationTimelineBar
                          startDate={initiative.startDate}
                          endDate={initiative.endDate}
                          timelineStart={timelineStart}
                          timelineEnd={timelineEnd}
                          status={initiative.status}
                          color={initiative.color}
                          progress={initiative.progress}
                          height="h-10"
                        />
                      </div>
                    </div>

                    {/* Expanded Epics */}
                    <AnimatePresence>
                      {expandedInitiatives.has(initiative.id) && initiative.epics?.map((epic) => (
                        <motion.div
                          key={epic.id}
                          initial={{ height: 0, opacity: 0 }}
                          animate={{ height: 'auto', opacity: 1 }}
                          exit={{ height: 0, opacity: 0 }}
                          transition={{ duration: 0.2 }}
                        >
                          {/* Epic row */}
                          <div 
                            className={`flex items-center rounded-lg ml-6 cursor-pointer transition-colors ${
                              isDarkMode ? 'hover:bg-slate-700' : 'hover:bg-slate-200'
                            }`}
                            onClick={() => onToggleEpic(epic.id)}
                          >
                            <div className="w-74 shrink-0 flex items-center gap-3 py-2 px-3">
                              <FileText className={`h-5 w-5 shrink-0 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`} />
                              <span className="text-base truncate">{epic.name}</span>
                              <Badge
                                variant="outline"
                                className={`text-xs shrink-0 ${isDarkMode ? 'border-slate-600' : ''}`}
                              >
                                {t(`epics.status.${epic.status.toLowerCase()}`)}
                              </Badge>
                              {epic.quarterLabel && (
                                <Badge variant="secondary" className={`text-[10px] shrink-0 font-normal ${isDarkMode ? 'bg-slate-700' : ''}`}>
                                  {epic.quarterLabel}
                                </Badge>
                              )}
                            </div>
                            <div className="flex-1 relative h-10">
                              <PresentationTimelineBar
                                startDate={epic.startDate}
                                endDate={epic.endDate}
                                timelineStart={timelineStart}
                                timelineEnd={timelineEnd}
                                status={epic.status}
                                color={epic.color}
                                progress={epic.progress}
                                height="h-8"
                              />
                            </div>
                          </div>

                          {/* Expanded Pitches */}
                          <AnimatePresence>
                            {expandedEpics.has(epic.id) && epic.pitches?.map((pitch) => (
                              <motion.div
                                key={pitch.id}
                                initial={{ height: 0, opacity: 0 }}
                                animate={{ height: 'auto', opacity: 1 }}
                                exit={{ height: 0, opacity: 0 }}
                                transition={{ duration: 0.2 }}
                                className={`flex items-center rounded-lg ml-12 ${
                                  isDarkMode ? 'hover:bg-slate-700' : 'hover:bg-slate-200'
                                }`}
                              >
                                <div className="w-68 shrink-0 flex items-center gap-3 py-2 px-3">
                                  <CheckCircle className={`h-4 w-4 shrink-0 ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`} />
                                  <span className={`text-sm truncate ${isDarkMode ? 'text-slate-300' : 'text-slate-600'}`}>
                                    {pitch.title}
                                  </span>
                                </div>
                                <div className="flex-1 relative h-8">
                                  <PresentationTimelineBar
                                    startDate={pitch.startDate}
                                    endDate={pitch.endDate}
                                    timelineStart={timelineStart}
                                    timelineEnd={timelineEnd}
                                    status={pitch.status}
                                    progress={pitch.progress}
                                    height="h-6"
                                  />
                                </div>
                              </motion.div>
                            ))}
                          </AnimatePresence>
                        </motion.div>
                      ))}
                    </AnimatePresence>
                  </div>
                ))}
                
                {/* Orphan Epics */}
                {timeline?.orphanEpics && timeline.orphanEpics.length > 0 && (
                  <div className={`border-t pt-4 mt-4 ${isDarkMode ? 'border-slate-700' : 'border-slate-300'}`}>
                    <p className={`text-base px-3 mb-3 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
                      {t('roadmap.unassignedEpics')}
                    </p>
                    {timeline.orphanEpics.map((epic) => (
                      <div 
                        key={epic.id}
                        className={`flex items-center rounded-lg cursor-pointer transition-colors ${
                          isDarkMode ? 'hover:bg-slate-700' : 'hover:bg-slate-200'
                        }`}
                        onClick={() => onToggleEpic(epic.id)}
                      >
                        <div className="w-80 shrink-0 flex items-center gap-3 py-2 px-3">
                          <FileText className={`h-5 w-5 shrink-0 ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`} />
                          <span className="text-base truncate">{epic.name}</span>
                          <Badge
                            variant="outline"
                            className={`text-xs shrink-0 ${isDarkMode ? 'border-slate-600' : ''}`}
                          >
                            {t(`epics.status.${epic.status.toLowerCase()}`)}
                          </Badge>
                          {epic.quarterLabel && (
                            <Badge variant="secondary" className={`text-[10px] shrink-0 font-normal ${isDarkMode ? 'bg-slate-700' : ''}`}>
                              {epic.quarterLabel}
                            </Badge>
                          )}
                        </div>
                        <div className="flex-1 relative h-10">
                          <PresentationTimelineBar
                            startDate={epic.startDate}
                            endDate={epic.endDate}
                            timelineStart={timelineStart}
                            timelineEnd={timelineEnd}
                            status={epic.status}
                            color={epic.color}
                            progress={epic.progress}
                            height="h-8"
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
              
              {/* Empty state */}
              {(!timeline?.initiatives || timeline.initiatives.length === 0) &&
               (!timeline?.orphanEpics || timeline.orphanEpics.length === 0) && (
                <div className="py-16 text-center">
                  <Target className={`h-16 w-16 mx-auto mb-4 ${isDarkMode ? 'text-slate-600' : 'text-slate-400'}`} />
                  <p className={`text-xl ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
                    {t('roadmap.noItems')}
                  </p>
                </div>
              )}
            </motion.div>

            {/* Legend */}
            <motion.div 
              initial={{ y: 20, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.4 }}
              className={`rounded-xl p-4 mt-6 ${isDarkMode ? 'bg-slate-800' : 'bg-slate-100'}`}
            >
              <div className="flex flex-wrap gap-6 text-base">
                <span className="font-semibold">{t('roadmap.legend')}:</span>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-gray-400" />
                  <span>{t('roadmap.draft')}</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-blue-500" />
                  <span>{t('roadmap.planned')}</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-yellow-500" />
                  <span>{t('roadmap.inProgress')}</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-green-500" />
                  <span>{t('roadmap.completed')}</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded bg-orange-500" />
                  <span>{t('roadmap.onHold')}</span>
                </div>
              </div>
            </motion.div>
          </div>

          {/* Floating Toolbar */}
          <motion.div 
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.5 }}
            className={`fixed bottom-6 left-1/2 -translate-x-1/2 flex items-center gap-3 px-6 py-3 rounded-full shadow-2xl ${
              isDarkMode ? 'bg-slate-800/95 border border-slate-700' : 'bg-white/95 border border-slate-200'
            }`}
          >
            {/* Navigation */}
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant="ghost" 
                    size="icon"
                    onClick={() => onNavigate('prev')}
                    className={isDarkMode ? 'hover:bg-slate-700' : 'hover:bg-slate-100'}
                  >
                    <ChevronLeft className="h-5 w-5" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('roadmap.previousPeriod')} (←)</TooltipContent>
              </Tooltip>
            </TooltipProvider>

            <span className={`text-base font-medium px-3 min-w-[100px] text-center ${isDarkMode ? 'text-slate-200' : 'text-slate-700'}`}>
              {periodLabel}
            </span>

            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant="ghost" 
                    size="icon"
                    onClick={() => onNavigate('next')}
                    className={isDarkMode ? 'hover:bg-slate-700' : 'hover:bg-slate-100'}
                  >
                    <ChevronRight className="h-5 w-5" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('roadmap.nextPeriod')} (→)</TooltipContent>
              </Tooltip>
            </TooltipProvider>

            <div className={`w-px h-6 mx-2 ${isDarkMode ? 'bg-slate-600' : 'bg-slate-300'}`} />

            {/* View Mode Toggle */}
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant={viewMode === 'quarterly' ? 'default' : 'ghost'}
                    size="sm"
                    onClick={() => onViewModeChange('quarterly')}
                    className={viewMode !== 'quarterly' && isDarkMode ? 'hover:bg-slate-700' : ''}
                  >
                    Q
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('roadmap.quarterly')} (V)</TooltipContent>
              </Tooltip>
            </TooltipProvider>

            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant={viewMode === 'yearly' ? 'default' : 'ghost'}
                    size="sm"
                    onClick={() => onViewModeChange('yearly')}
                    className={viewMode !== 'yearly' && isDarkMode ? 'hover:bg-slate-700' : ''}
                  >
                    Y
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('roadmap.yearly')} (V)</TooltipContent>
              </Tooltip>
            </TooltipProvider>

            <div className={`w-px h-6 mx-2 ${isDarkMode ? 'bg-slate-600' : 'bg-slate-300'}`} />

            {/* Dark Mode Toggle */}
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant="ghost" 
                    size="icon"
                    onClick={() => setIsDarkMode(!isDarkMode)}
                    className={isDarkMode ? 'hover:bg-slate-700' : 'hover:bg-slate-100'}
                  >
                    {isDarkMode ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
                  </Button>
                </TooltipTrigger>
                <TooltipContent>
                  {isDarkMode ? t('roadmap.lightMode') : t('roadmap.darkMode')} (D)
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>

            <div className={`w-px h-6 mx-2 ${isDarkMode ? 'bg-slate-600' : 'bg-slate-300'}`} />

            {/* Close Button */}
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant="ghost" 
                    size="icon"
                    onClick={onClose}
                    className={isDarkMode ? 'hover:bg-slate-700' : 'hover:bg-slate-100'}
                  >
                    <X className="h-5 w-5" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('roadmap.exitPresentation')} (Esc)</TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </motion.div>

          {/* Keyboard Shortcuts Hint */}
          <div className={`fixed bottom-6 right-6 text-sm ${isDarkMode ? 'text-slate-500' : 'text-slate-400'}`}>
            <span className="opacity-60">
              ← → {t('roadmap.navigate')} | D {t('roadmap.toggleTheme')} | V {t('roadmap.toggleView')} | Esc {t('roadmap.exit')}
            </span>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );

  return createPortal(content, document.body);
}
