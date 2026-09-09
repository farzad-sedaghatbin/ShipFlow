import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Skeleton } from '@/components/ui/skeleton';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { useProject } from '../contexts/ProjectContext';
import { cycleService } from '../services/cycleService';
import { taskService } from '../services/taskService';
import { Task, TaskStatus } from '../types';
import { getUserFriendlyError } from '../utils/errorMessages';
import KanbanBoard from '../components/KanbanBoard';
import EmptyState from '../components/EmptyState';
import { EmptyCyclesIllustration } from '../components/illustrations';

export default function SprintBoardPage() {
  const { t } = useTranslation();
  const { currentProject, isScrumProject } = useProject();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const projectId = currentProject?.id;
  const [deleteTaskId, setDeleteTaskId] = useState<number | null>(null);
  const [deleting, setDeleting] = useState(false);

  // Active cycle for the project — Scrum projects run one active sprint at a time in
  // practice, so the first result (if any) is treated as "the" active sprint.
  const { data: activeCycles, isLoading: cyclesLoading } = useQuery({
    queryKey: ['cycles', 'project', projectId, 'active'],
    queryFn: () => cycleService.getActiveByProject(projectId!).then((r) => r.data),
    enabled: !!projectId && isScrumProject,
  });

  const activeCycle = activeCycles?.[0] ?? null;

  // No page argument — taskService.getByCycleId returns the full unpaginated task list
  // (GET /tasks/cycle/{cycleId}/all) rather than a Page<Task>, which is what a board needs.
  const {
    data: tasks,
    isLoading: tasksLoading,
    refetch: refetchTasks,
  } = useQuery({
    queryKey: ['tasks', 'cycle', activeCycle?.id, 'board'],
    queryFn: () => taskService.getByCycleId(activeCycle!.id).then((r) => r.data as Task[]),
    enabled: !!activeCycle,
  });

  // All hooks are declared above this point — safe to return early without violating
  // the Rules of Hooks (hook count is always the same regardless of which branch renders).
  if (currentProject && !isScrumProject) {
    return <Navigate to="/backlog" replace />;
  }

  const handleStatusChange = async (taskId: number, newStatus: TaskStatus) => {
    try {
      await taskService.updateStatus(taskId, newStatus);
      toast.success(t('backlogPage.statusUpdated'));
      refetchTasks();
    } catch (error) {
      toast.error(getUserFriendlyError(error));
    }
  };

  // No dedicated dialog here — route to the full Task Detail page for both viewing and
  // editing, same as PitchTasksSection.tsx. state.from is read by useBackNavigation (a
  // separate in-flight PR) to send the user back to this board instead of /backlog.
  const handleViewOrEditTask = (task: Task) => {
    navigate(`/backlog/${task.id}`, {
      state: { from: `${location.pathname}${location.search}` },
    });
  };

  const handleDeleteTask = (taskId: number) => setDeleteTaskId(taskId);

  const confirmDeleteTask = async () => {
    if (deleteTaskId == null) return;
    setDeleting(true);
    try {
      await taskService.delete(deleteTaskId);
      toast.success(t('backlogPage.taskDeleted'));
      setDeleteTaskId(null);
      refetchTasks();
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
    } catch (error) {
      toast.error(getUserFriendlyError(error));
    } finally {
      setDeleting(false);
    }
  };

  const isLoading = !projectId || cyclesLoading;

  return (
    <div className="flex flex-col gap-6" data-tour="sprint-board">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold">{t('sprintBoard.title')}</h1>
        <p className="text-muted-foreground">{t('sprintBoard.description')}</p>
      </div>

      {isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-6 w-64" />
          <div className="flex gap-4">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-96 w-72 shrink-0" />
            ))}
          </div>
        </div>
      ) : !activeCycle ? (
        <EmptyState
          illustration={<EmptyCyclesIllustration />}
          title={t('sprintBoard.noActiveSprint.title')}
          description={t('sprintBoard.noActiveSprint.description')}
          action={{
            label: t('sprintBoard.noActiveSprint.action'),
            onClick: () => navigate('/sprint-planning'),
          }}
        />
      ) : (
        <>
          <p className="text-sm text-muted-foreground">
            {t('sprintBoard.activeSprint', { name: activeCycle.name })}
          </p>
          <KanbanBoard
            tasks={tasks ?? []}
            onStatusChange={handleStatusChange}
            onViewTask={handleViewOrEditTask}
            onEditTask={handleViewOrEditTask}
            onDeleteTask={handleDeleteTask}
            loading={tasksLoading}
          />
        </>
      )}

      <ConfirmDialog
        open={deleteTaskId != null}
        onOpenChange={(open) => {
          if (!open) setDeleteTaskId(null);
        }}
        title={t('backlogPage.deleteTask')}
        description={t('backlogPage.confirmDeleteMessage')}
        confirmLabel={t('backlogPage.delete')}
        cancelLabel={t('backlogPage.cancel')}
        onConfirm={confirmDeleteTask}
        variant="destructive"
        loading={deleting}
      />
    </div>
  );
}
