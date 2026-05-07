import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../../utils/dateLocalization';
import { Clock, Calendar } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { cycleService } from '../../services/cycleService';
import { Cycle } from '../../types';
import { cn } from '@/lib/utils';
import { STALE_TIMES } from '../../lib/queryClient';

export function UpcomingDeadlinesWidget() {
  const { t, i18n } = useTranslation();

  const { data: cycles = [], isLoading: loading } = useQuery({
    queryKey: ['widgets', 'upcoming-deadlines'],
    queryFn: async () => {
      const response = await cycleService.getMyActiveCycles();
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      return (response.data as Cycle[])
        .filter((cycle) => {
          const endDate = new Date(cycle.endDate);
          endDate.setHours(0, 0, 0, 0);
          const daysUntilEnd = Math.floor((endDate.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
          return daysUntilEnd >= 0 && daysUntilEnd <= 14;
        })
        .sort((a, b) => new Date(a.endDate).getTime() - new Date(b.endDate).getTime())
        .slice(0, 5);
    },
    staleTime: STALE_TIMES.entities,
  });

  const getDaysUntil = (endDate: string) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const end = new Date(endDate);
    end.setHours(0, 0, 0, 0);
    return Math.floor((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  };

  const getUrgencyColor = (days: number) => {
    if (days <= 3) return 'destructive';
    if (days <= 7) return 'default';
    return 'secondary';
  };

  if (loading) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <Clock className="w-4 h-4 text-amber-500" />
            {t('widgets.upcomingDeadlines')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-sm text-muted-foreground">{t('common.loading')}</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          <Clock className="w-4 h-4 text-amber-500" />
          {t('widgets.upcomingDeadlines')}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {cycles.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t('widgets.noUpcomingDeadlines')}</p>
        ) : (
          <div className="space-y-2">
            {cycles.map((cycle) => {
              const daysUntil = getDaysUntil(cycle.endDate);
              return (
                <Link
                  key={cycle.id}
                  to={`/cycles/${cycle.id}`}
                  className={cn(
                    'block p-2 rounded-md hover:bg-muted transition-colors border',
                    daysUntil <= 3 ? 'bg-destructive/5 border-destructive/20' : 'bg-muted/50 border-border'
                  )}
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-sm font-medium text-foreground line-clamp-1">
                      {cycle.name}
                    </span>
                    <Badge variant={getUrgencyColor(daysUntil)} className="text-xs shrink-0">
                      {daysUntil}d
                    </Badge>
                  </div>
                  <div className="flex items-center gap-1 mt-1 text-xs text-muted-foreground">
                    <Calendar className="w-3 h-3" />
                    Ends: {formatLocalizedDate(new Date(cycle.endDate), i18n.language)}
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
