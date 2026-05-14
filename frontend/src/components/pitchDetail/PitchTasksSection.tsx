import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Task } from '../../types';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';

interface PitchTasksSectionProps {
  tasks: Task[];
}

export function PitchTasksSection({ tasks }: PitchTasksSectionProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardHeader>
        <div className="flex justify-between items-center">
          <CardTitle>{t('pitchDetailPage.tasks', 'Tasks')}</CardTitle>
          <Link to="/backlog?category=PITCH_SCOPE">
            <Button variant="outline" size="sm">
              {t('pitchDetailPage.viewAllTasks', 'View All Tasks')}
            </Button>
          </Link>
        </div>
      </CardHeader>
      <CardContent>
        {tasks.length === 0 ? (
          <p className="text-muted-foreground">{t('pitchDetailPage.noTasks', 'No tasks linked to this pitch yet.')}</p>
        ) : (
          <div className="space-y-2">
            {tasks.map(task => (
              <Link key={task.id} to={`/backlog/${task.id}`} className="block">
                <div className="flex items-center justify-between py-2 px-3 rounded-md border border-border hover:bg-muted/50 transition-colors cursor-pointer">
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <Badge
                      variant={
                        task.status === 'DONE' ? 'success' :
                        task.status === 'IN_PROGRESS' ? 'default' :
                        task.status === 'BLOCKED' ? 'destructive' :
                        'secondary'
                      }
                      className="text-[10px] px-1.5 py-0 shrink-0"
                    >
                      {task.status?.replace(/_/g, ' ')}
                    </Badge>
                    <span className="text-sm truncate">{task.title}</span>
                  </div>
                  <div className="flex items-center gap-2 shrink-0 ml-2">
                    {task.assigneeName && (
                      <span className="text-xs text-muted-foreground">{task.assigneeName}</span>
                    )}
                    {task.priority && (
                      <Badge variant="outline" className="text-[10px] px-1.5 py-0">
                        {task.priority}
                      </Badge>
                    )}
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
