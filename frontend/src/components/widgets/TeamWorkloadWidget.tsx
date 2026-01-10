import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Users, TrendingUp } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { teamService } from '../../services/teamService';
import { taskService } from '../../services/taskService';
import { Team, Task } from '../../types';

interface TeamWorkload {
  team: Team;
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  completionRate: number;
}

export function TeamWorkloadWidget() {
  const [workloads, setWorkloads] = useState<TeamWorkload[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadTeamWorkload();
  }, []);

  const loadTeamWorkload = async () => {
    try {
      setLoading(true);
      const [teamsRes, tasksRes] = await Promise.all([
        teamService.getAll(),
        taskService.getAll(0, 1000),
      ]);

      const teams = teamsRes.data;
      const tasks = tasksRes.data.content || [];

      const workloadData = teams.map((team: Team) => {
        // Filter tasks by matching cycle ID
        const teamTasks = tasks.filter((task: Task) => task.cycleId === team.cycleId);
        const completed = teamTasks.filter((t: Task) => t.status === 'DONE').length;
        const inProgress = teamTasks.filter((t: Task) => t.status === 'IN_PROGRESS').length;
        const total = teamTasks.filter((t: Task) => t.status !== 'CANCELLED').length;

        return {
          team,
          totalTasks: total,
          completedTasks: completed,
          inProgressTasks: inProgress,
          completionRate: total > 0 ? (completed / total) * 100 : 0,
        };
      });

      setWorkloads(workloadData.slice(0, 5));
    } catch (error) {
      console.error('Failed to load team workload:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <Users className="w-4 h-4 text-violet-500" />
            Team Workload
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
          <Users className="w-4 h-4 text-violet-500" />
          Team Workload
        </CardTitle>
      </CardHeader>
      <CardContent>
        {workloads.length === 0 ? (
          <p className="text-sm text-muted-foreground">No team data available</p>
        ) : (
          <div className="space-y-3">
            {workloads.map(({ team, totalTasks, completedTasks, inProgressTasks, completionRate }) => (
              <Link
                key={team.id}
                to={`/teams/${team.id}`}
                className="block p-2 rounded-md bg-muted/50 hover:bg-muted transition-colors"
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-foreground">{team.name}</span>
                  <span className="text-xs text-muted-foreground">
                    {completedTasks}/{totalTasks}
                  </span>
                </div>
                <Progress value={completionRate} className="h-1.5 mb-1" />
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <TrendingUp className="w-3 h-3" />
                  {inProgressTasks} in progress • {completionRate.toFixed(0)}% complete
                </div>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
