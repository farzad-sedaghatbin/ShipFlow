import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Ban, User } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { taskService } from '../../services/taskService';
import { Task } from '../../types';

export function BlockedTasksWidget() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadBlockedTasks();
  }, []);

  const loadBlockedTasks = async () => {
    try {
      setLoading(true);
      const response = await taskService.getAll(0, 100);
      const allTasks = response.data.content || [];
      const blocked = allTasks.filter((task: Task) => task.status === 'BLOCKED');
      setTasks(blocked.slice(0, 5));
    } catch (error) {
      console.error('Failed to load blocked tasks:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <Ban className="w-4 h-4 text-destructive" />
            Blocked Tasks
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
          <Ban className="w-4 h-4 text-destructive" />
          Blocked Tasks
          {tasks.length > 0 && (
            <Badge variant="destructive" className="ml-auto">
              {tasks.length}
            </Badge>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {tasks.length === 0 ? (
          <p className="text-sm text-muted-foreground">No blocked tasks</p>
        ) : (
          <div className="space-y-2">
            {tasks.map((task) => (
              <Link
                key={task.id}
                to={`/tasks/${task.id}`}
                className="block p-2 rounded-md bg-destructive/5 hover:bg-destructive/10 transition-colors border border-destructive/20"
              >
                <span className="text-sm font-medium text-foreground line-clamp-1 block mb-1">
                  {task.title}
                </span>
                {task.assigneeName && (
                  <div className="flex items-center gap-1 text-xs text-muted-foreground">
                    <User className="w-3 h-3" />
                    {task.assigneeName}
                  </div>
                )}
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
