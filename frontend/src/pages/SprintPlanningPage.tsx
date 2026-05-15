import { useState } from 'react';
import { useTranslation } from 'react-i18next';
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
import { Task, TaskStatus } from '../types';
import { BurndownChart } from '../components/BurndownChart';
import { VelocityChart } from '../components/VelocityChart';

// Tasks that belong to "product backlog" (not yet in a sprint or in early statuses)
const BACKLOG_STATUSES: TaskStatus[] = ['BACKLOG', 'TODO'];

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
        {task.assigneeName && (
          <p className="text-xs text-muted-foreground mt-0.5">
            {t('sprintPlanning.unassigned') !== task.assigneeName
              ? task.assigneeName
              : t('sprintPlanning.unassigned')}
          </p>
        )}
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
  const { currentProject } = useProject();
  const queryClient = useQueryClient();
  const [selectedCycleId, setSelectedCycleId] = useState<number | null>(null);

  // Fetch cycles for this project
  const { data: cycles, isLoading: cyclesLoading } = useQuery({
    queryKey: ['cycles', 'project', currentProject?.id],
    queryFn: () => cycleService.getByProject(currentProject!.id).then((r) => r.data),
    enabled: !!currentProject?.id,
  });

  const selectedCycle = cycles?.find((c) => c.id === selectedCycleId) ?? null;

  // Product backlog: all project tasks in early statuses without a cycle assignment
  const { data: allProjectTasks, isLoading: allTasksLoading } = useQuery({
    queryKey: ['tasks', 'project', currentProject?.id],
    queryFn: () => taskService.getByProjectId(currentProject!.id).then((r) => r.data),
    enabled: !!currentProject?.id,
  });

  // Sprint tasks: tasks assigned to the selected cycle
  const { data: sprintTasks, isLoading: sprintTasksLoading } = useQuery({
    queryKey: ['tasks', 'cycle', selectedCycleId],
    queryFn: () => taskService.getByCycleId(selectedCycleId!).then((r) => r.data as Task[]),
    enabled: !!selectedCycleId,
  });

  // Move task to sprint mutation
  const moveToSprintMutation = useMutation({
    mutationFn: (task: Task) =>
      taskService.update(task.id, {
        title: task.title,
        description: task.description,
        cycleId: selectedCycleId!,
        pitchId: task.pitchId,
        status: task.status,
        priority: task.priority,
        category: task.category,
        estimateHours: task.estimateHours,
        teamId: task.teamId,
        assigneeId: task.assigneeId,
        pairAssigneeId: task.pairAssigneeId,
        dueDate: task.dueDate,
        tags: task.tags,
        storyPoints: task.storyPoints,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      toast.success(t('sprintPlanning.moveToSprint'));
    },
    onError: () => toast.error(t('common.error')),
  });

  // Move task back to backlog mutation
  const moveToBacklogMutation = useMutation({
    mutationFn: (task: Task) =>
      taskService.update(task.id, {
        title: task.title,
        description: task.description,
        cycleId: 0,
        pitchId: task.pitchId,
        status: task.status === 'IN_PROGRESS' ? 'TODO' : task.status,
        priority: task.priority,
        category: task.category,
        estimateHours: task.estimateHours,
        teamId: task.teamId,
        assigneeId: task.assigneeId,
        pairAssigneeId: task.pairAssigneeId,
        dueDate: task.dueDate,
        tags: task.tags,
        storyPoints: task.storyPoints,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      toast.success(t('sprintPlanning.moveToBacklog'));
    },
    onError: () => toast.error(t('common.error')),
  });

  // Product backlog = tasks not in any sprint OR in BACKLOG/TODO status
  const productBacklogTasks =
    allProjectTasks?.filter(
      (t) =>
        (!t.cycleId || t.cycleId === 0) && BACKLOG_STATUSES.includes(t.status),
    ) ?? [];

  const sprintTaskList: Task[] = (sprintTasks as Task[] | undefined) ?? [];

  const productBacklogPoints = productBacklogTasks.reduce(
    (sum, t) => sum + (t.storyPoints ?? 0),
    0,
  );
  const sprintPoints = sprintTaskList.reduce(
    (sum, t) => sum + (t.storyPoints ?? 0),
    0,
  );

  const isLoading = cyclesLoading || allTasksLoading;

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
                {t('sprintPlanning.totalPoints', { count: productBacklogPoints })}
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
            ) : productBacklogTasks.length === 0 ? (
              <p className="text-sm text-muted-foreground text-center py-6">
                {t('common.noData')}
              </p>
            ) : (
              <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
                {productBacklogTasks.map((task) => (
                  <SprintTaskCard
                    key={task.id}
                    task={task}
                    actionLabel={t('sprintPlanning.moveToSprint')}
                    actionIcon={<ArrowRight className="h-4 w-4" />}
                    onAction={(t) => moveToSprintMutation.mutate(t)}
                    isPending={moveToSprintMutation.isPending || !selectedCycleId}
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
                {t('sprintPlanning.totalPoints', { count: sprintPoints })}
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
                    onAction={(t) => moveToBacklogMutation.mutate(t)}
                    isPending={moveToBacklogMutation.isPending}
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
