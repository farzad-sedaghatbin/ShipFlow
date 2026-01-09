import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { CheckSquare, Circle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { taskService } from '../../services/taskService';
import { Task } from '../../types';
import { cn } from '@/lib/utils';

export function MyTasksWidget() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [stats, setStats] = useState({ total: 0, completed: 0, inProgress: 0 });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadMyTasks();
  }, []);

  const loadMyTasks = async () => {
    try {
      setLoading(true);
      const response = await taskService.getAll();
      // Filter tasks assigned to current user (you'd need user context for this)
      // For now, showing all non-completed tasks
      const myTasks = response.data.filter((task: Task) => 
        task.status !== 'DONE' && task.status !== 'CANCELLED'
      );
      
      setTasks(myTasks.slice(0, 5));
      setStats({
        total: myTasks.length,
        completed: response.data.filter((t: Task) => t.status === 'DONE').length,
        inProgress: myTasks.filter((t: Task) => t.status === 'IN_PROGRESS').length,
      });
    } catch (error) {
      console.error('Failed to load my tasks:', error);
    } finally {
      setLoading(false);
    }
  };

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
            My Tasks
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-sm text-muted-foreground">Loading...</div>
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
          My Tasks
          <Badge variant="secondary" className="ml-auto">
            {stats.total}
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {/* Stats Summary */}
        <div className="mb-3 p-2 rounded-md bg-muted/50">
          <div className="flex items-center justify-between text-xs mb-1">
            <span className="text-muted-foreground">Completion Rate</span>
            <span className="font-medium">{completionRate.toFixed(0)}%</span>
          </div>
          <Progress value={completionRate} className="h-1.5" />
          <div className="flex gap-3 mt-2 text-xs">
            <span className="text-muted-foreground">
              In Progress: <span className="font-medium text-foreground">{stats.inProgress}</span>
            </span>
            <span className="text-muted-foreground">
              Completed: <span className="font-medium text-foreground">{stats.completed}</span>
            </span>
          </div>
        </div>

        {/* Task List */}
        {tasks.length === 0 ? (
          <p className="text-sm text-muted-foreground">No active tasks</p>
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
