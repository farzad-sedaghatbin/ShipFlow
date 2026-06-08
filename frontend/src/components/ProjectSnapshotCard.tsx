import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Brain, RefreshCw, TrendingUp, Layers, AlertTriangle, CheckCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import { projectSnapshotService } from '../services/projectSnapshotService';
import { formatDistanceToNow } from 'date-fns';

interface ProjectSnapshotCardProps {
  projectId: number;
}

const RISK_COLORS: Record<string, string> = {
  HIGH: 'text-red-600 bg-red-50 border-red-200',
  MEDIUM: 'text-amber-600 bg-amber-50 border-amber-200',
  LOW: 'text-green-600 bg-green-50 border-green-200',
};

export function ProjectSnapshotCard({ projectId }: ProjectSnapshotCardProps) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [refreshing, setRefreshing] = useState(false);

  const { data: snapshot, isLoading, isError } = useQuery({
    queryKey: ['projectSnapshot', projectId],
    queryFn: () => projectSnapshotService.getSnapshot(projectId),
    staleTime: 1000 * 60 * 15,
  });

  const refreshMutation = useMutation({
    mutationFn: () => projectSnapshotService.refreshSnapshot(projectId),
    onMutate: () => setRefreshing(true),
    onSuccess: (data) => {
      queryClient.setQueryData(['projectSnapshot', projectId], data);
      setRefreshing(false);
    },
    onError: () => setRefreshing(false),
  });

  if (isLoading) {
    return (
      <Card>
        <CardHeader className="pb-2">
          <Skeleton className="h-5 w-48" />
        </CardHeader>
        <CardContent className="space-y-2">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-4 w-2/3" />
        </CardContent>
      </Card>
    );
  }

  if (isError || !snapshot) return null;

  const completionPct = Math.round(snapshot.avgCompletionRate * 100);
  const riskColor = RISK_COLORS[snapshot.riskPosture] ?? RISK_COLORS.MEDIUM;
  const updatedAgo = formatDistanceToNow(new Date(snapshot.computedAt), { addSuffix: true });

  return (
    <Card>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-sm font-medium flex items-center gap-2">
            <Brain className="h-4 w-4 text-purple-500" />
            {t('projectSnapshot.title')}
          </CardTitle>
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">{t('projectSnapshot.updatedAgo', { time: updatedAgo })}</span>
            <Button
              variant="ghost"
              size="sm"
              className="h-7 w-7 p-0"
              onClick={() => refreshMutation.mutate()}
              disabled={refreshing}
              title={t('projectSnapshot.refresh')}
            >
              <RefreshCw className={`h-3.5 w-3.5 ${refreshing ? 'animate-spin' : ''}`} />
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-3 text-sm">
        {/* Velocity row */}
        <div className="flex items-start gap-2">
          <TrendingUp className="h-4 w-4 text-blue-500 mt-0.5 shrink-0" />
          <div>
            <span className="font-medium">{t('projectSnapshot.velocity')}: </span>
            <span className="text-muted-foreground">
              {t('projectSnapshot.velocityDetail', {
                cycles: snapshot.cyclesCompleted,
                avg: snapshot.avgPitchesPerCycle.toFixed(1),
                completion: completionPct,
                appetite: snapshot.typicalAppetiteDays,
              })}
            </span>
          </div>
        </div>

        {/* Active cycle */}
        {snapshot.activeCycleName && (
          <div className="flex items-start gap-2">
            <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 shrink-0" />
            <div>
              <span className="font-medium">{t('projectSnapshot.activeCycle')}: </span>
              <span className="text-muted-foreground">
                "{snapshot.activeCycleName}" — {snapshot.activeCyclePitchCount} {t('projectSnapshot.pitches')},&nbsp;
                {snapshot.activeCycleCompletionPercent}% {t('projectSnapshot.done')},&nbsp;
                {snapshot.activeCycleDaysRemaining} {t('projectSnapshot.daysLeft')}
              </span>
            </div>
          </div>
        )}

        {/* Roadmap themes */}
        {snapshot.roadmapThemes.length > 0 && (
          <div className="flex items-start gap-2">
            <Layers className="h-4 w-4 text-indigo-500 mt-0.5 shrink-0" />
            <div className="flex flex-wrap gap-1">
              {snapshot.roadmapThemes.map((theme) => (
                <Badge key={theme} variant="secondary" className="text-xs font-normal">
                  {theme}
                </Badge>
              ))}
            </div>
          </div>
        )}

        {/* Known patterns */}
        {snapshot.knownPatterns.length > 0 && (
          <div className="flex items-start gap-2">
            <AlertTriangle className="h-4 w-4 text-amber-500 mt-0.5 shrink-0" />
            <div>
              <span className="font-medium">{t('projectSnapshot.patterns')}: </span>
              <span className="text-muted-foreground text-xs">
                {snapshot.knownPatterns.join(' · ')}
              </span>
            </div>
          </div>
        )}

        {/* Risk posture */}
        <div className="pt-1">
          <span className="text-xs text-muted-foreground">{t('projectSnapshot.riskPosture')}: </span>
          <span className={`text-xs font-medium px-1.5 py-0.5 rounded border ${riskColor}`}>
            {snapshot.riskPosture}
          </span>
        </div>
      </CardContent>
    </Card>
  );
}
