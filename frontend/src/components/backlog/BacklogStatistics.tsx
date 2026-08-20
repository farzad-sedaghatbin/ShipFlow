import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { TaskStatistics } from '../../types';

interface BacklogStatisticsProps {
  statistics: TaskStatistics;
}

// No "% Complete" tile here: unlike a Release or Cooldown (TaskStatisticsCard, a fixed scope
// that genuinely progresses toward 100%), the Backlog is a continuously-growing inventory of
// work — new items keep arriving, so a completion ratio never means "done" and mostly just
// sits low, misleadingly reading as a stalled or under-performing project.
export function BacklogStatistics({ statistics }: BacklogStatisticsProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-medium">{t('backlogPage.taskOverview')}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
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
        </div>
      </CardContent>
    </Card>
  );
}
