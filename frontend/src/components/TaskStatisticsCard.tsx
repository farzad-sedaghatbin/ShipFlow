import { useEffect, useState } from 'react';
import {
  CheckCircle,
  Play,
  Ban,
  ListTodo,
  ClipboardList,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { taskService } from '../services/taskService';
import { TaskStatistics } from '../types';

import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Skeleton } from './ui/skeleton';

interface TaskStatisticsCardProps {
  cycleId: number;
}

export default function TaskStatisticsCard({ cycleId }: TaskStatisticsCardProps) {
  const navigate = useNavigate();
  const [statistics, setStatistics] = useState<TaskStatistics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStatistics();
  }, [cycleId]);

  const loadStatistics = async () => {
    try {
      const response = await taskService.getStatisticsByCycleId(cycleId);
      setStatistics(response.data);
    } catch (error) {
      console.error('Failed to load task statistics:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Card>
        <CardContent className="pt-6 space-y-4">
          <Skeleton className="h-6 w-32" />
          <Skeleton className="h-24 w-full" />
        </CardContent>
      </Card>
    );
  }

  if (!statistics || statistics.totalTasks === 0) {
    return (
      <Card>
        <CardHeader className="flex flex-row items-center justify-between pb-2">
          <div className="flex items-center gap-2">
            <ClipboardList className="h-5 w-5 text-primary" />
            <CardTitle className="text-base">Task Progress</CardTitle>
          </div>
          <Button variant="link" size="sm" onClick={() => navigate('/tasks')} className="px-0">
            View Tasks
          </Button>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground text-center py-4">
            No tasks in this cycle yet. Create tasks for independent work!
          </p>
        </CardContent>
      </Card>
    );
  }

  const getProgressColor = () => {
    if (statistics.completionPercentage >= 80) return 'bg-green-500';
    if (statistics.completionPercentage >= 50) return 'bg-amber-500';
    return 'bg-primary';
  };

  const getTextColor = () => {
    if (statistics.completionPercentage >= 80) return 'text-green-400';
    if (statistics.completionPercentage >= 50) return 'text-amber-400';
    return 'text-primary';
  };

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <div className="flex items-center gap-2">
          <ClipboardList className="h-5 w-5 text-primary" />
          <CardTitle className="text-base">Task Progress</CardTitle>
        </div>
        <Button variant="link" size="sm" onClick={() => navigate('/tasks')} className="px-0">
          View All
        </Button>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Progress Bar */}
        <div className="space-y-2">
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">
              {statistics.doneTasks} of {statistics.totalTasks} tasks completed
            </span>
            <span className={`font-medium ${getTextColor()}`}>
              {statistics.completionPercentage.toFixed(0)}%
            </span>
          </div>
          <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
            <div 
              className={`h-full rounded-full transition-all ${getProgressColor()}`}
              style={{ width: `${statistics.completionPercentage}%` }}
            />
          </div>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-4 gap-2">
          <div className="text-center">
            <div className="flex items-center justify-center gap-1">
              <CheckCircle className="h-4 w-4 text-green-400" />
              <span className="text-xl font-bold text-green-400">{statistics.doneTasks}</span>
            </div>
            <span className="text-xs text-muted-foreground">Done</span>
          </div>
          <div className="text-center">
            <div className="flex items-center justify-center gap-1">
              <Play className="h-4 w-4 text-blue-400" />
              <span className="text-xl font-bold text-blue-400">{statistics.inProgressTasks}</span>
            </div>
            <span className="text-xs text-muted-foreground">In Progress</span>
          </div>
          <div className="text-center">
            <div className="flex items-center justify-center gap-1">
              <Ban className="h-4 w-4 text-red-400" />
              <span className="text-xl font-bold text-red-400">{statistics.blockedTasks}</span>
            </div>
            <span className="text-xs text-muted-foreground">Blocked</span>
          </div>
          <div className="text-center">
            <div className="flex items-center justify-center gap-1">
              <ListTodo className="h-4 w-4 text-muted-foreground" />
              <span className="text-xl font-bold text-foreground">{statistics.backlogTasks + statistics.todoTasks}</span>
            </div>
            <span className="text-xs text-muted-foreground">To Do</span>
          </div>
        </div>

        {/* Additional Stats */}
        {(statistics.totalEstimateHours > 0 || statistics.totalActualHours > 0) && (
          <div className="pt-3 border-t border-border">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-muted-foreground">Estimated Hours</p>
                <p className="text-lg font-semibold text-foreground">
                  {statistics.totalEstimateHours.toFixed(1)}h
                </p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Actual Hours</p>
                <p className="text-lg font-semibold text-foreground">
                  {statistics.totalActualHours.toFixed(1)}h
                </p>
              </div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
