import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CheckSquare, Circle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { taskService } from '../../services/taskService';
import { Task } from '../../types';
import { cn } from '@/lib/utils';
import { STALE_TIMES } from '../../lib/queryClient';

interface MyTasksData {
  tasks: Task[];
  stats: { total: number; completed: number; inProgress: number };
}

export function MyTasksWidget() {
  const { t } = useTranslation();

  const { data, isLoading: loading } = useQuery<MyTasksData>({
    queryKey: ['widgets', 'my-tasks'],
    queryFn: async () => {
      const response = await taskService.getAll(0, 100);
      const allTasks: Task[] = response.data.content || [];
      // Filter tasks assigned to current user (you'd need user context for this)
      // For now, showing all non-completed tasks
      const myTasks = allTasks.filter(
        (task) => task.status !== 'DONE' && task.status !== 'CANCELLED'
      );
      return {
        tasks: myTasks.slice(0, 5),
        stats: {
          total: myTasks.length,
          completed: allTasks.filter((t) => t.status === 'DONE').length,
          inProgress: myTasks.filter((t) => t.status === 'IN_PROGRESS').length,
        },
      };
    },
    staleTime: STALE_TIMES.tasks,
  });

  const tasks = data?.tasks ?? [];
  const stats = data?.stats ?? { total: 0, completed: 0, inProgress: 0 };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'DONE':
        return 'bg-emerald-500/10 text-emerald-700 border-emerald-500/20';
      case 'IN_PROGRESS':
        return 'bg-blue-500/10 text-blue-700 border-blue-500/20';
      case 'BLOCKED':
        return 'bg-destructive/10 text-destructive border-destructive/20';
      default:
        return 'bg-muted text-muted-foreground border-border';
    }
  };

  const getPriorityColor = (priority?: string) => {
    switch (priority) {
      case 'URGENT':
        return 'destructive';
      case 'HIGH':
        return 'default';
      default:
        return 'secondary';
    }
  };

  if (loading) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <CheckSquare className="w-4 h-4 text-primary" />
            {t('widgets.myTasks')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-sm text-muted-foreground">{t('widgets.loading')}</div>
        </CardContent>
      </Card>
    );
  }

  const completionRate = stats.total > 0 ? (stats.completed / (stats.total + stats.completed)) * 100 : 0;

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          <CheckSquare className="w-4 h-4 text-primary" />
          {t('widgets.myTasks')}
          <Badge variant="secondary" className="ml-auto">
            {stats.total}
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {/* Stats Summary */}
        <div className="mb-3 p-2 rounded-md bg-muted/50">
          <div className="flex items-center justify-between text-xs mb-1">
            <span className="text-muted-foreground">{t('widgets.completionRate')}</span>
            <span className="font-medium">{completionRate.toFixed(0)}%</span>
          </div>
          <Progress value={completionRate} className="h-1.5" />
          <div className="flex gap-3 mt-2 text-xs">
            <span className="text-muted-foreground">
              {t('widgets.inProgressTasks')}: <span className="font-medium text-foreground">{stats.inProgress}</span>
            </span>
            <span className="text-muted-foreground">
              {t('widgets.completed')}: <span className="font-medium text-foreground">{stats.completed}</span>
            </span>
          </div>
        </div>

        {/* Task List */}
        {tasks.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t('widgets.noActiveTasks')}</p>
        ) : (
          <div className="space-y-2">
            {tasks.map((task) => (
              <Link
                key={task.id}
                to={`/tasks/${task.id}`}
                className={cn(
                  'block p-2 rounded-md hover:bg-muted/80 transition-colors border',
                  getStatusColor(task.status)
                )}
              >
                <div className="flex items-start justify-between gap-2 mb-1">
                  <span className="text-sm font-medium text-foreground line-clamp-1">
                    {task.title}
                  </span>
                  {task.priority && (task.priority === 'HIGH' || task.priority === 'URGENT') && (
                    <Badge variant={getPriorityColor(task.priority)} className="text-xs shrink-0">
                      {task.priority}
                    </Badge>
                  )}
                </div>
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <Circle className={cn('w-2 h-2 fill-current', task.status === 'IN_PROGRESS' && 'text-blue-500')} />
                  {task.status.replace('_', ' ')}
                </div>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
