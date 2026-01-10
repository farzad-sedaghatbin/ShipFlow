import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, Calendar } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { taskService } from '../../services/taskService';
import { Task } from '../../types';

export function OverdueTasksWidget() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadOverdueTasks();
  }, []);

  const loadOverdueTasks = async () => {
    try {
      setLoading(true);
      const response = await taskService.getAll(0, 100);
      const allTasks = response.data.content || [];
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      const overdue = allTasks.filter((task: Task) => {
        if (!task.dueDate || task.status === 'DONE' || task.status === 'CANCELLED') {
          return false;
        }
        const dueDate = new Date(task.dueDate);
        dueDate.setHours(0, 0, 0, 0);
        return dueDate < today;
      });

      setTasks(overdue.slice(0, 5));
    } catch (error) {
      console.error('Failed to load overdue tasks:', error);
    } finally {
      setLoading(false);
    }
  };

  const getDaysOverdue = (dueDate: string) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(dueDate);
    due.setHours(0, 0, 0, 0);
    const diff = Math.floor((today.getTime() - due.getTime()) / (1000 * 60 * 60 * 24));
    return diff;
  };

  if (loading) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-destructive" />
            Overdue Tasks
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-sm text-muted-foreground">Loading...</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          <AlertCircle className="w-4 h-4 text-destructive" />
          Overdue Tasks
          {tasks.length > 0 && (
            <Badge variant="destructive" className="ml-auto">
              {tasks.length}
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {tasks.length === 0 ? (
          <p className="text-sm text-muted-foreground">No overdue tasks! 🎉</p>
        ) : (
          <div className="space-y-2">
            {tasks.map((task) => (
              <Link
                key={task.id}
                to={`/tasks/${task.id}`}
                className="block p-2 rounded-md bg-destructive/5 hover:bg-destructive/10 transition-colors border border-destructive/20"
              >
                <div className="flex items-start justify-between gap-2">
                  <span className="text-sm font-medium text-foreground line-clamp-1">
                    {task.title}
                  </span>
                  <Badge variant="destructive" className="text-xs shrink-0">
                    {getDaysOverdue(task.dueDate!)}d
                  </Badge>
                </div>
                <div className="flex items-center gap-1 mt-1 text-xs text-muted-foreground">
                  <Calendar className="w-3 h-3" />
                  Due: {new Date(task.dueDate!).toLocaleDateString()}
                </div>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
