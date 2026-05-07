import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { TaskStatistics } from '../../types';

interface BacklogStatisticsProps {
  statistics: TaskStatistics;
}

export function BacklogStatistics({ statistics }: BacklogStatisticsProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium">{t('backlogPage.taskOverview')}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
          <div className="text-center">
            <div className="text-2xl font-bold">{statistics.totalTasks}</div>
            <div className="text-xs text-muted-foreground">{t('backlogPage.total')}</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-blue-500">{statistics.todoTasks}</div>
            <div className="text-xs text-muted-foreground">{t('backlogPage.todo')}</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-purple-500">{statistics.inProgressTasks}</div>
            <div className="text-xs text-muted-foreground">{t('backlogPage.inProgress')}</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-red-500">{statistics.blockedTasks}</div>
            <div className="text-xs text-muted-foreground">{t('backlogPage.blocked')}</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-yellow-500">{statistics.inReviewTasks}</div>
            <div className="text-xs text-muted-foreground">{t('backlogPage.inReview')}</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-green-500">{statistics.doneTasks}</div>
            <div className="text-xs text-muted-foreground">{t('backlogPage.done')}</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold">{statistics.completionPercentage}%</div>
            <div className="text-xs text-muted-foreground">{t('backlogPage.complete')}</div>
          </div>
        </div>
        <Progress value={statistics.completionPercentage} className="mt-4" />
      </CardContent>
    </Card>
  );
}
