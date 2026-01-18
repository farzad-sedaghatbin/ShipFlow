import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDateTime } from '../utils/dateLocalization';
import {
  RefreshCw,
  Clock,
  CheckCircle2,
  Play,
  TrendingUp,
  FileText,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { workLogService } from '../services/workLogService';
import { WorkLog, Pitch, Cycle } from '../types';
import { pitchService } from '../services/pitchService';
import { cycleService } from '../services/cycleService';
import { formatDistanceToNow, parseDate } from '../utils';
import { cn } from '@/lib/utils';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

interface ActivityItem {
  id: string;
  type: 'worklog' | 'pitch_update' | 'cycle_update' | 'meeting';
  title: string;
  description: string;
  timestamp: string;
  icon: React.ReactNode;
  color: 'primary' | 'secondary' | 'success' | 'warning' | 'info';
  link?: string;
}

interface RecentActivityFeedProps {
  maxItems?: number;
  compact?: boolean;
  projectId?: number;
}

const colorClasses = {
  primary: 'bg-primary/15 text-primary',
  secondary: 'bg-violet-500/15 text-violet-500',
  success: 'bg-emerald-500/15 text-emerald-500',
  warning: 'bg-amber-500/15 text-amber-500',
  info: 'bg-blue-500/15 text-blue-500',
};

const hoverClasses = {
  primary: 'hover:bg-primary/10',
  secondary: 'hover:bg-violet-500/10',
  success: 'hover:bg-emerald-500/10',
  warning: 'hover:bg-amber-500/10',
  info: 'hover:bg-blue-500/10',
};

export function RecentActivityFeed({ 
  maxItems = 5, 
  compact = false,
  projectId 
}: RecentActivityFeedProps) {
  const { i18n } = useTranslation();
  const [activities, setActivities] = useState<ActivityItem[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadRecentActivity();
  }, [projectId]);

  const loadRecentActivity = async () => {
    try {
      setLoading(true);
      
      const [workLogsRes, pitchesRes, cyclesRes] = await Promise.all([
        workLogService.getMy(),
        pitchService.getAll(),
        cycleService.getActive(),
      ]);

      const activityItems: ActivityItem[] = [];

      const recentWorkLogs = workLogsRes.data
        .filter((log: WorkLog) => {
          const logDate = parseDate(log.date);
          const weekAgo = new Date();
          weekAgo.setDate(weekAgo.getDate() - 7);
          return logDate >= weekAgo && (!projectId || log.projectId === projectId);
        })
        .slice(0, 10);

      recentWorkLogs.forEach((log: WorkLog) => {
        activityItems.push({
          id: `worklog-${log.id}`,
          type: 'worklog',
          title: `Logged ${log.hoursSpent}h on ${log.pitchTitle}`,
          description: log.note || 'Work logged',
          timestamp: log.date,
          icon: <Clock className="w-4 h-4" />,
          color: 'primary',
          link: `/my-worklogs`,
        });
      });

      const recentPitches = pitchesRes.data
        .filter((p: Pitch) => {
          const updated = parseDate(p.updatedAt);
          const weekAgo = new Date();
          weekAgo.setDate(weekAgo.getDate() - 7);
          return updated >= weekAgo && (!projectId || p.projectId === projectId);
        })
        .sort((a: Pitch, b: Pitch) => parseDate(b.updatedAt).getTime() - parseDate(a.updatedAt).getTime())
        .slice(0, 5);

      recentPitches.forEach((pitch: Pitch) => {
        const statusConfig: Record<string, { icon: React.ReactNode; color: ActivityItem['color']; verb: string }> = {
          DONE: { icon: <CheckCircle2 className="w-4 h-4" />, color: 'success', verb: 'completed' },
          IN_PROGRESS: { icon: <TrendingUp className="w-4 h-4" />, color: 'info', verb: 'making progress on' },
          STARTED: { icon: <Play className="w-4 h-4" />, color: 'warning', verb: 'started' },
          TESTING: { icon: <TrendingUp className="w-4 h-4" />, color: 'secondary', verb: 'testing' },
        };
        
        const config = statusConfig[pitch.status] || { icon: <FileText className="w-4 h-4" />, color: 'primary' as const, verb: 'updated' };
        
        activityItems.push({
          id: `pitch-${pitch.id}`,
          type: 'pitch_update',
          title: `${pitch.title}`,
          description: `Status: ${pitch.status} • ${pitch.progressPercentage?.toFixed(0) || 0}% complete`,
          timestamp: pitch.updatedAt,
          icon: config.icon,
          color: config.color,
          link: `/pitches/${pitch.id}`,
        });
      });

      const activeCycles = cyclesRes.data
        .filter((c: Cycle) => !projectId || c.projectId === projectId)
        .slice(0, 3);

      activeCycles.forEach((cycle: Cycle) => {
        activityItems.push({
          id: `cycle-${cycle.id}`,
          type: 'cycle_update',
          title: `${cycle.name}`,
          description: `${cycle.phase} phase • ${cycle.pitchCount || 0} pitches`,
          timestamp: cycle.startDate,
          icon: <RefreshCw className="w-4 h-4" />,
          color: 'secondary',
          link: `/cycles/${cycle.id}`,
        });
      });

      const sortedActivities = activityItems
        .sort((a, b) => parseDate(b.timestamp).getTime() - parseDate(a.timestamp).getTime())
        .slice(0, maxItems);

      setActivities(sortedActivities);
    } catch (error) {
      console.error('Failed to load recent activity:', error);
    } finally {
      setLoading(false);
    }
  };

  const getTimeAgo = (timestamp: string) => {
    return formatDistanceToNow(timestamp);
  };

  if (loading) {
    return (
      <Card>
        <CardContent className="p-4" aria-busy="true" aria-live="polite">
          <h2 className="text-lg font-semibold text-foreground mb-2">
            Recent Activity
          </h2>
          <div className="space-y-3" aria-label="Loading recent activity">
            {[...Array(compact ? 3 : 5)].map((_, idx) => (
              <div key={idx} className="flex items-center gap-3 py-1">
                <Skeleton className="w-9 h-9 rounded-full" />
                <div className="flex-1">
                  <Skeleton className="w-3/5 h-4 mb-1" />
                  <Skeleton className="w-2/5 h-3" />
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (activities.length === 0) {
    return (
      <Card>
        <CardContent className="p-4">
          <h2 className="text-lg font-semibold text-foreground mb-2">
            Recent Activity
          </h2>
          <div
            className="py-8 text-center"
            role="status"
            aria-live="polite"
          >
            <Clock className="w-12 h-12 mx-auto mb-2 opacity-50 text-muted-foreground" aria-hidden="true" />
            <p className="text-sm text-foreground">
              No recent activity to show
            </p>
            <p className="text-xs text-muted-foreground">
              Start logging work or updating pitches
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex justify-between items-center mb-2">
          <h2 className="text-lg font-semibold text-foreground" id="recent-activity-title">
            Recent Activity
          </h2>
          <Badge variant="secondary" className="text-xs">
            {activities.length} items
          </Badge>
        </div>
        <div className="space-y-1" role="feed" aria-label="Recent activity feed">
          <AnimatePresence>
            {activities.map((activity, index) => {
              const content = (
                <motion.div
                  key={activity.id}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  transition={{ delay: index * 0.05, duration: 0.3 }}
                >
                  <div
                    className={cn(
                      'flex items-center gap-3 py-2 px-2 rounded-lg transition-all duration-200',
                      activity.link && hoverClasses[activity.color],
                      activity.link && 'hover:translate-x-1 cursor-pointer'
                    )}
                  >
                    <Avatar className={cn('w-9 h-9', colorClasses[activity.color])}>
                      <AvatarFallback className="bg-transparent">
                        {activity.icon}
                      </AvatarFallback>
                    </Avatar>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-foreground truncate">
                        {activity.title}
                      </p>
                      <div className="flex justify-between items-center">
                        <span className="text-xs text-muted-foreground truncate flex-1">
                          {activity.description}
                        </span>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <span className="text-xs text-muted-foreground/60 ml-2 flex-shrink-0">
                              {getTimeAgo(activity.timestamp)}
                            </span>
                          </TooltipTrigger>
                          <TooltipContent>
                            {formatLocalizedDateTime(parseDate(activity.timestamp), i18n.language)}
                          </TooltipContent>
                        </Tooltip>
                      </div>
                    </div>
                  </div>
                </motion.div>
              );

              return activity.link ? (
                <Link key={activity.id} to={activity.link} className="block">
                  {content}
                </Link>
              ) : (
                content
              );
            })}
          </AnimatePresence>
        </div>
        {!compact && (
          <div className="mt-3 text-center">
            <Button variant="ghost" size="sm" asChild>
              <Link to="/my-worklogs">View all activity</Link>
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default RecentActivityFeed;
