import { useQueries } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../../utils/dateLocalization';
import { RefreshCw, Calendar, TrendingUp } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Badge } from '@/components/ui/badge';
import { cycleService } from '../../services/cycleService';
import { pitchService } from '../../services/pitchService';
import { Cycle, Pitch } from '../../types';
import { cn } from '@/lib/utils';
import { STALE_TIMES } from '../../lib/queryClient';

interface CycleProgress {
  cycle: Cycle;
  totalPitches: number;
  completedPitches: number;
  progressPercentage: number;
  daysRemaining: number;
  timeProgress: number;
}

export function CycleProgressWidget() {
  const { i18n } = useTranslation();

  const [cyclesQuery, pitchesQuery] = useQueries({
    queries: [
      {
        queryKey: ['widgets', 'cycle-progress', 'cycles'],
        queryFn: () => cycleService.getMyActiveCycles().then((r) => r.data as Cycle[]),
        staleTime: STALE_TIMES.entities,
      },
      {
        queryKey: ['widgets', 'cycle-progress', 'pitches'],
        queryFn: () => pitchService.getMyPitches().then((r) => r.data as Pitch[]),
        staleTime: STALE_TIMES.entities,
      },
    ],
  });

  const loading = cyclesQuery.isLoading || pitchesQuery.isLoading;

  const cycles: CycleProgress[] = (() => {
    if (!cyclesQuery.data || !pitchesQuery.data) return [];
    const today = new Date();
    const shapeUpCycles = cyclesQuery.data.filter(
      (cycle) => !cycle.projectType || cycle.projectType === 'SHAPE_UP'
    );
    return shapeUpCycles
      .map((cycle) => {
        const cyclePitches = pitchesQuery.data.filter((p) => p.cycleId === cycle.id);
        const completed = cyclePitches.filter((p) => p.status === 'DONE').length;
        const startDate = new Date(cycle.startDate);
        const endDate = new Date(cycle.endDate);
        const totalDays = Math.floor((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
        const elapsedDays = Math.floor((today.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));
        const daysRemaining = Math.floor((endDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
        const timeProgress = Math.min((elapsedDays / totalDays) * 100, 100);
        return {
          cycle,
          totalPitches: cyclePitches.length,
          completedPitches: completed,
          progressPercentage: cyclePitches.length > 0 ? (completed / cyclePitches.length) * 100 : 0,
          daysRemaining: Math.max(daysRemaining, 0),
          timeProgress,
        };
      })
      .slice(0, 3);
  })();

  if (loading) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <RefreshCw className="w-4 h-4 text-primary" />
            {i18n.t('widgets.cycleProgress')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-sm text-muted-foreground">{i18n.t('widgets.loading')}</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          <RefreshCw className="w-4 h-4 text-primary" />
          {i18n.t('widgets.cycleProgress')}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {cycles.length === 0 ? (
          <p className="text-sm text-muted-foreground">{i18n.t('widgets.noActiveCycles')}</p>
        ) : (
          <div className="space-y-3">
            {cycles.map(({ cycle, totalPitches, completedPitches, progressPercentage, daysRemaining, timeProgress }) => (
              <Link
                key={cycle.id}
                to={`/cycles/${cycle.id}`}
                className="block p-2 rounded-md bg-muted/50 hover:bg-muted transition-colors"
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-foreground">{cycle.name}</span>
                  <Badge 
                    variant="secondary"
                    className={cn(
                      cycle.phase === 'SHAPING_BUILDING' && 'bg-primary/15 text-primary',
                      cycle.phase === 'BETTING_COOLDOWN' && 'bg-amber-500/15 text-amber-500',
                    )}
                  >
                    {cycle.phase}
                  </Badge>
                </div>

                <div className="space-y-2">
                  {/* Work Progress */}
                  <div>
                    <div className="flex items-center justify-between text-xs mb-1">
                      <span className="text-muted-foreground">{i18n.t('widgets.workProgress')}</span>
                      <span className="font-medium">{completedPitches}/{totalPitches} {i18n.t('widgets.pitches')}</span>
                    </div>
                    <Progress 
                      value={progressPercentage} 
                      className={cn('h-1.5', progressPercentage > timeProgress && '[&>div]:bg-emerald-500')}
                    />
                  </div>

                  {/* Time Progress */}
                  <div>
                    <div className="flex items-center justify-between text-xs mb-1">
                      <span className="text-muted-foreground">{i18n.t('widgets.timeProgress')}</span>
                      <span className="font-medium">{daysRemaining}d {i18n.t('widgets.remaining')}</span>
                    </div>
                    <Progress value={timeProgress} className="h-1.5" />
                  </div>
                </div>

                <div className="flex items-center gap-2 mt-2 text-xs text-muted-foreground">
                  <Calendar className="w-3 h-3" />
                  {formatLocalizedDate(new Date(cycle.endDate), i18n.language)}
                  {progressPercentage < timeProgress && (
                    <>
                      <span>•</span>
                      <TrendingUp className="w-3 h-3 text-destructive" />
                      <span className="text-destructive">{i18n.t('widgets.behindSchedule')}</span>
                    </>
                  )}
                </div>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
