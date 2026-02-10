import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { ListChecks, TrendingUp, Clock, CheckCircle } from 'lucide-react';
import { CooldownSummaryDTO } from '../services/cooldownActivityService';

interface CooldownSummaryCardsProps {
  summary: CooldownSummaryDTO;
}

export default function CooldownSummaryCards({ summary }: CooldownSummaryCardsProps) {
  const { t } = useTranslation();

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">{t('cooldownActivity.totalActivities')}</CardTitle>
          <ListChecks className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{summary.totalActivities}</div>
          <div className="text-xs text-muted-foreground mt-1">
            {t('cooldownActivity.planned')}: {summary.plannedCount} | {t('cooldownActivity.inProgress')}: {summary.inProgressCount}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">{t('cooldownActivity.completionRate')}</CardTitle>
          <TrendingUp className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{(summary.completionPercentage || 0).toFixed(1)}%</div>
          <div className="text-xs text-muted-foreground mt-1">
            {summary.completedCount} / {summary.totalActivities} {t('cooldownActivity.completed')}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">{t('cooldownActivity.estimatedHours')}</CardTitle>
          <Clock className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{(summary.totalEstimatedHours || 0).toFixed(1)}</div>
          <div className="text-xs text-muted-foreground mt-1">
            {t('cooldownActivity.hoursPlanned')}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">{t('cooldownActivity.actualHours')}</CardTitle>
          <CheckCircle className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{(summary.totalActualHours || 0).toFixed(1)}</div>
          <div className="text-xs text-muted-foreground mt-1">
            {t('cooldownActivity.hoursSpent')}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
