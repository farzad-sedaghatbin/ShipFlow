import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { ArrowRight, ArrowLeft, BarChart2, TrendingDown } from 'lucide-react';
import { toast } from 'sonner';
import { useProject } from '../contexts/ProjectContext';
import { cycleService } from '../services/cycleService';
import { taskService } from '../services/taskService';
import { Task } from '../types';
import { BurndownChart } from '../components/BurndownChart';
import { VelocityChart } from '../components/VelocityChart';


function StoryPointBadge({ points }: { points?: number | null }) {
  if (points == null) return null;
  return (
    <Badge variant="outline" className="ml-2 text-xs font-mono shrink-0">
      {points}
    </Badge>
  );
}

interface TaskCardProps {
  task: Task;
  actionLabel: string;
  actionIcon: React.ReactNode;
  onAction: (task: Task) => void;
  isPending: boolean;
}

function SprintTaskCard({ task, actionLabel, actionIcon, onAction, isPending }: TaskCardProps) {
  const { t } = useTranslation();
  return (
    <div className="flex items-start gap-2 rounded-md border bg-card p-3 shadow-sm">
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1 flex-wrap">
          <span className="text-sm font-medium truncate">{task.title}</span>
          <StoryPointBadge points={task.storyPoints} />
        </div>
        <p className="text-xs text-muted-foreground mt-0.5">
          {task.assigneeName ?? t('sprintPlanning.unassigned')}
        </p>
      </div>
      <Button
        variant="ghost"
        size="icon"
        className="h-7 w-7 shrink-0"
        aria-label={actionLabel}
        onClick={() => onAction(task)}
        disabled={isPending}
      >
        {actionIcon}
      </Button>
    </div>
  );
}

export default function SprintPlanningPage() {
  const { t } = useTranslation();
  const { currentProject, isScrumProject } = useProject();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [selectedCycleId, setSelectedCycleId] = useState<number | null>(null);
  const [pendingTaskId, setPendingTaskId] = useState<number | null>(null);
  const [pendingBacklogTaskId, setPendingBacklogTaskId] = useState<number | null>(null);
  const projectId = currentProject?.id;

  // Redirect non-SCRUM projects away from this page
  useEffect(() => {
    if (currentProject && !isScrumProject) {
      navigate('/backlog', { replace: true });
    }
  }, [currentProject, isScrumProject, navigate]);

  // Fetch cycles for this project
  const { data: cycles, isLoading: cyclesLoading } = useQuery({
    queryKey: ['cycles', 'project', projectId],
    queryFn: () => cycleService.getByProject(projectId!).then((r) => r.data),
    enabled: !!projectId && isScrumProject,
  });

  const selectedCycle = cycles?.find((c) => c.id === selectedCycleId) ?? null;

  // Product backlog: tasks with no sprint assigned — uses dedicated endpoint
  const { data: backlogTasks = [], isLoading: backlogLoading } = useQuery({
    queryKey: ['product-backlog', projectId],
    queryFn: () => taskService.getProductBacklogTasks(projectId!).then((r) => r.data),
    enabled: !!projectId && isScrumProject,
  });

  // Sprint tasks: tasks assigned to the selected cycle
  const { data: sprintTasks, isLoading: sprintTasksLoading } = useQuery({
    queryKey: ['tasks', 'cycle', selectedCycleId],
    queryFn: () => taskService.getByCycleId(selectedCycleId!).then((r) => r.data as Task[]),
    enabled: !!selectedCycleId,
  });

  // Move task to sprint mutation — uses dedicated PATCH /tasks/{id}/cycle to avoid
  // partial-update data loss (parentTaskId, scopeId, releaseId, etc. are preserved server-side)
  const moveToSprintMutation = useMutation({
    mutationFn: (task: Task) => taskService.assignCycle(task.id, selectedCycleId!),
    onMutate: (task) => setPendingTaskId(task.id),
    onSettled: () => setPendingTaskId(null),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['product-backlog', projectId] });
      queryClient.invalidateQueries({ queryKey: ['burndown', selectedCycleId] });
      queryClient.invalidateQueries({ queryKey: ['velocity', projectId] });
      toast.success(t('sprintPlanning.movedToSprint'));
    },
    onError: () => toast.error(t('common.error')),
  });

  // Move task back to product backlog — passes null to clear the cycle assignment
  const moveToBacklogMutation = useMutation({
    mutationFn: (task: Task) => taskService.assignCycle(task.id, null),
    onMutate: (task) => setPendingBacklogTaskId(task.id),
    onSettled: () => setPendingBacklogTaskId(null),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['product-backlog', projectId] });
      queryClient.invalidateQueries({ queryKey: ['burndown', selectedCycleId] });
      queryClient.invalidateQueries({ queryKey: ['velocity', projectId] });
      toast.success(t('sprintPlanning.movedToBacklog'));
    },
    onError: () => toast.error(t('common.error')),
  });

  const sprintTaskList: Task[] = (sprintTasks as Task[] | undefined) ?? [];

  const productBacklogPoints = backlogTasks.reduce(
    (sum, task) => sum + (task.storyPoints ?? 0),
    0,
  );
  const sprintPoints = sprintTaskList.reduce(
    (sum, task) => sum + (task.storyPoints ?? 0),
    0,
  );

  const isLoading = !projectId || cyclesLoading || backlogLoading;

  return (
    <div className="flex flex-col gap-6 p-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold">{t('sprintPlanning.title')}</h1>
        <p className="text-muted-foreground">{t('sprintPlanning.description')}</p>
      </div>

      {/* Sprint selector */}
      <div className="flex items-center gap-3">
        <span className="text-sm font-medium">{t('sprintPlanning.selectSprint')}:</span>
        {cyclesLoading ? (
          <Skeleton className="h-9 w-48" />
        ) : (
          <Select
            value={selectedCycleId?.toString() ?? ''}
            onValueChange={(v) => setSelectedCycleId(v ? Number(v) : null)}
          >
            <SelectTrigger className="w-56">
              <SelectValue placeholder={t('sprintPlanning.noSprint')} />
            </SelectTrigger>
            <SelectContent>
              {cycles?.map((cycle) => (
                <SelectItem key={cycle.id} value={cycle.id.toString()}>
                  {cycle.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
        {selectedCycle?.sprintGoal && (
          <span className="text-sm text-muted-foreground italic truncate max-w-xs">
            {t('sprintPlanning.sprintGoal')}: {selectedCycle.sprintGoal}
          </span>
        )}
      </div>

      {/* Two-column planning board */}
      <div data-tour="sprint-planning-board" className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        {/* Product Backlog */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center justify-between text-base">
              <span>{t('sprintPlanning.productBacklog')}</span>
              <Badge variant="secondary">
                {t('sprintPlanning.totalPoints', { points: productBacklogPoints })}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-2">
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} className="h-14 w-full" />
                ))}
              </div>
            ) : backlogTasks.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-6">
                {t('common.noData')}
              </p>
            ) : (
              <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
                {backlogTasks.map((task) => (
                  <SprintTaskCard
                    key={task.id}
                    task={task}
                    actionLabel={t('sprintPlanning.moveToSprint')}
                    actionIcon={<ArrowRight className="h-4 w-4" />}
                    onAction={(task) => moveToSprintMutation.mutate(task)}
                    isPending={pendingTaskId === task.id || !selectedCycleId}
                  />
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Sprint Backlog */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center justify-between text-base">
              <span>
                {selectedCycle
                  ? selectedCycle.name
                  : t('sprintPlanning.sprintBacklog')}
              </span>
              <Badge variant="secondary">
                {t('sprintPlanning.totalPoints', { points: sprintPoints })}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            {!selectedCycleId ? (
              <p className="text-sm text-muted-foreground text-center py-6">
                {t('sprintPlanning.noSprint')}
              </p>
            ) : sprintTasksLoading ? (
              <div className="space-y-2">
                {[1, 2, 3].map((i) => (
                  <Skeleton key={i} className="h-14 w-full" />
                ))}
              </div>
            ) : sprintTaskList.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-6">
                {t('common.noData')}
              </p>
            ) : (
              <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
                {sprintTaskList.map((task) => (
                  <SprintTaskCard
                    key={task.id}
                    task={task}
                    actionLabel={t('sprintPlanning.moveToBacklog')}
                    actionIcon={<ArrowLeft className="h-4 w-4" />}
                    onAction={(task) => moveToBacklogMutation.mutate(task)}
                    isPending={pendingBacklogTaskId === task.id}
                  />
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Charts */}
      {currentProject && (
        <Tabs defaultValue="burndown">
          <TabsList>
            <TabsTrigger value="burndown" className="gap-2">
              <TrendingDown className="h-4 w-4" />
              {t('sprintPlanning.burndown')}
            </TabsTrigger>
            <TabsTrigger value="velocity" className="gap-2">
              <BarChart2 className="h-4 w-4" />
              {t('sprintPlanning.velocityChart')}
            </TabsTrigger>
          </TabsList>
          <TabsContent value="burndown">
            {selectedCycleId && selectedCycle ? (
              <BurndownChart
                cycleId={selectedCycleId}
                cycleName={selectedCycle.name}
              />
            ) : (
              <Card>
                <CardContent className="flex h-48 items-center justify-center text-sm text-muted-foreground">
                  {t('sprintPlanning.noSprint')}
                </CardContent>
              </Card>
            )}
          </TabsContent>
          <TabsContent value="velocity">
            <VelocityChart projectId={currentProject.id} />
          </TabsContent>
        </Tabs>
      )}
    </div>
  );
}
