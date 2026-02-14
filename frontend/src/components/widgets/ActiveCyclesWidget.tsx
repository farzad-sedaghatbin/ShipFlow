import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { Cycle } from '../../types';
import EmptyState from '../EmptyState';
import { EmptyCyclesIllustration } from '../illustrations';
import { formatLocalizedDate } from '../../utils/dateLocalization';

interface ActiveCyclesWidgetProps {
  cycles: Cycle[];
}

export function ActiveCyclesWidget({ cycles }: ActiveCyclesWidgetProps) {
  const { t, i18n } = useTranslation();

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex justify-between items-center mb-3">
          <h2 className="text-lg font-semibold text-foreground">{t('dashboard.activeCycles')}</h2>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/cycles">{t('common.viewAll')}</Link>
          </Button>
        </div>
        {cycles.length === 0 ? (
          <EmptyState
            illustration={<EmptyCyclesIllustration width={160} height={120} />}
            title={t('dashboard.noActiveCycles')}
            description={t('dashboard.noActiveCyclesDescription')}
            size="small"
            compact
            action={{
              label: t('dashboard.newCycle'),
              onClick: () => window.location.href = '/cycles/new',
              startIcon: <Plus className="w-4 h-4 mr-1" />,
            }}
          />
        ) : (
          <div className="space-y-2">
            {cycles.map((cycle) => (
              <Link
                key={cycle.id}
                to={`/cycles/${cycle.id}`}
                className="block p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
              >
                <div className="flex justify-between items-center">
                  <span className="font-semibold text-foreground">{cycle.name}</span>
                  <Badge
                    variant="secondary"
                    className={cn(
                      cycle.phase === 'BUILD' && 'bg-primary/15 text-primary',
                      cycle.phase === 'SHAPING' && 'bg-blue-500/15 text-blue-500',
                      cycle.phase === 'BETTING' && 'bg-amber-500/15 text-amber-500',
                      cycle.phase === 'COOLDOWN' && 'bg-violet-500/15 text-violet-500'
                    )}
                  >
                    {cycle.phase}
                  </Badge>
                </div>
                <p className="text-sm text-muted-foreground">
                  {formatLocalizedDate(new Date(cycle.startDate), i18n.language)} - {formatLocalizedDate(new Date(cycle.endDate), i18n.language)}
                </p>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
