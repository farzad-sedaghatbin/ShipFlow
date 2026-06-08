import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';
import {
  AlertTriangle,
  CheckCircle,
  Info,
  RefreshCw,
  Sparkles,
  ChevronRight,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
  dashboardInsightsService,
  type DashboardInsight,
} from '../services/dashboardInsightsService';

interface DashboardInsightsPanelProps {
  projectId: number;
  isKanbanProject?: boolean;
  className?: string;
}

/** Tailwind colour tokens per severity level */
const SEVERITY_CONFIG: Record<
  DashboardInsight['severity'],
  { border: string; badge: string; icon: React.ReactNode }
> = {
  HIGH: {
    border: 'border-l-4 border-l-destructive',
    badge: 'bg-destructive/10 text-destructive',
    icon: <AlertTriangle className="h-4 w-4 text-destructive shrink-0 mt-0.5" />,
  },
  MEDIUM: {
    border: 'border-l-4 border-l-amber-500',
    badge: 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400',
    icon: <AlertTriangle className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />,
  },
  LOW: {
    border: 'border-l-4 border-l-blue-500',
    badge: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
    icon: <Info className="h-4 w-4 text-blue-500 shrink-0 mt-0.5" />,
  },
  INFO: {
    border: 'border-l-4 border-l-muted-foreground',
    badge: 'bg-muted text-muted-foreground',
    icon: <Info className="h-4 w-4 text-muted-foreground shrink-0 mt-0.5" />,
  },
};

function InsightRow({ insight }: { insight: DashboardInsight }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const cfg = SEVERITY_CONFIG[insight.severity];

  return (
    <div className={`flex items-start gap-3 rounded-md p-3 bg-muted/30 ${cfg.border}`}>
      {cfg.icon}
      <div className="flex-1 min-w-0">
        <div className="flex flex-wrap items-center gap-2 mb-0.5">
          <span className="text-sm font-medium">{insight.title}</span>
          {insight.count !== undefined && insight.count !== null && (
            <Badge variant="secondary" className={`text-xs ${cfg.badge}`}>
              {insight.count}
            </Badge>
          )}
        </div>
        <p className="text-xs text-muted-foreground leading-snug">{insight.detail}</p>
      </div>
      {insight.targetRoute && (
        <Button
          variant="ghost"
          size="sm"
          className="shrink-0 h-7 px-2 text-xs"
          onClick={() => navigate(insight.targetRoute!)}
        >
          {t('dashboardInsights.viewDetails')}
          <ChevronRight className="h-3 w-3 ml-0.5" />
        </Button>
      )}
    </div>
  );
}

export function DashboardInsightsPanel({
  projectId,
  isKanbanProject = false,
  className = '',
}: DashboardInsightsPanelProps) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ['dashboardInsights', projectId],
    queryFn: () => dashboardInsightsService.getInsights(projectId),
    staleTime: 1000 * 60 * 55, // 55 min — slightly less than Redis 60 min TTL
    enabled: !isKanbanProject,
  });

  const refreshMutation = useMutation({
    mutationFn: () => dashboardInsightsService.refreshInsights(projectId),
    onSuccess: (fresh) => {
      queryClient.setQueryData(['dashboardInsights', projectId], fresh);
    },
  });

  // Shape Up only
  if (isKanbanProject) return null;

  return (
    <Card className={`w-full ${className}`} data-tour="dashboard-insights-panel">
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="text-base flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-primary" />
              {t('dashboardInsights.title')}
            </CardTitle>
            <p className="text-xs text-muted-foreground mt-0.5">
              {t('dashboardInsights.subtitle')}
            </p>
          </div>
          <Button
            variant="ghost"
            size="sm"
            className="h-7 px-2 text-xs"
            disabled={refreshMutation.isPending}
            onClick={() => refreshMutation.mutate()}
          >
            <RefreshCw
              className={`h-3 w-3 mr-1 ${refreshMutation.isPending ? 'animate-spin' : ''}`}
            />
            {t('dashboardInsights.refresh')}
          </Button>
        </div>
      </CardHeader>

      <CardContent className="pt-0 space-y-2">
        {isLoading ? (
          // Loading skeleton — 3 rows
          <>
            <Skeleton className="h-14 w-full rounded-md" />
            <Skeleton className="h-14 w-full rounded-md" />
            <Skeleton className="h-14 w-full rounded-md" />
          </>
        ) : !data || data.insights.length === 0 ? (
          // Empty success state
          <div className="flex items-center gap-2 py-4 text-sm text-muted-foreground">
            <CheckCircle className="h-4 w-4 text-green-500" />
            {t('dashboardInsights.noAlerts')}
          </div>
        ) : (
          <>
            {data.insights.map((insight, idx) => (
              <InsightRow key={`${insight.type}-${idx}`} insight={insight} />
            ))}

            {data.aiSummary && (
              <div className="mt-3 rounded-md bg-muted/40 px-3 py-2 border border-muted">
                <div className="flex items-center gap-1.5 mb-1">
                  <Sparkles className="h-3 w-3 text-primary" />
                  <span className="text-xs font-medium">{t('dashboardInsights.aiSummary')}</span>
                </div>
                <p className="text-xs text-muted-foreground italic leading-relaxed">
                  {data.aiSummary}
                </p>
              </div>
            )}

            {data.computedAt && (
              <p className="text-right text-[11px] text-muted-foreground pt-1">
                {t('dashboardInsights.updatedAt')}{' '}
                {formatDistanceToNow(new Date(data.computedAt), { addSuffix: true })}
              </p>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
