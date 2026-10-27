import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CheckCircle2, ListChecks, Plus, Unlink, Link2 } from 'lucide-react';
import { taskService } from '../services/taskService';
import { cycleService } from '../services/cycleService';
import { Task, ProjectType } from '../types';
import { useToast } from '../contexts';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Input } from './ui/input';
import { Combobox } from './ui/combobox';

const STATUS_KEYS: Record<string, string> = {
  BACKLOG: 'backlog',
  TODO: 'todo',
  IN_PROGRESS: 'inProgress',
  IN_REVIEW: 'inReview',
  BLOCKED: 'blocked',
  DONE: 'done',
  CANCELLED: 'cancelled',
};

interface EpicTasksSectionProps {
  epicId: number;
  projectId: number;
  projectType: ProjectType;
  tasks: Task[];
  /** Called after any link/unlink/create so the parent can refetch the epic and its tasks. */
  onChanged: () => void;
}

/**
 * Tasks section of the Epic detail page for Kanban/Scrum projects (replaces the pitches section).
 * Supports linking an existing epic-less task, unlinking, and a minimal title-only "New task".
 */
export default function EpicTasksSection({ epicId, projectId, projectType, tasks, onChanged }: EpicTasksSectionProps) {
  const { t } = useTranslation();
  const { showSuccess, showError } = useToast();
  const [candidates, setCandidates] = useState<Task[]>([]);
  const [pickerKey, setPickerKey] = useState(0);
  const [newTitle, setNewTitle] = useState('');
  const [busy, setBusy] = useState(false);

  const loadCandidates = useCallback(async () => {
    try {
      const res = await taskService.getByProjectId(projectId);
      setCandidates(res.data.filter((tk) => !tk.epicId && !tk.parentTaskId && tk.status !== 'CANCELLED'));
    } catch (error) {
      console.error('Failed to load tasks for linking:', error);
    }
  }, [projectId]);

  useEffect(() => {
    loadCandidates();
  }, [loadCandidates, tasks.length]);

  const options = useMemo(
    () => candidates.map((tk) => ({ value: String(tk.id), label: tk.title })),
    [candidates]
  );

  const handleLink = async (value: string) => {
    if (!value) return;
    try {
      setBusy(true);
      await taskService.setEpic(Number(value), epicId);
      showSuccess(t('epics.taskLinked'));
      setPickerKey((k) => k + 1);
      onChanged();
    } catch (error) {
      console.error('Failed to link task:', error);
      showError(t('epics.taskLinkError'));
    } finally {
      setBusy(false);
    }
  };

  const handleUnlink = async (taskId: number) => {
    try {
      setBusy(true);
      await taskService.setEpic(taskId, null);
      showSuccess(t('epics.taskUnlinked'));
      onChanged();
    } catch (error) {
      console.error('Failed to unlink task:', error);
      showError(t('epics.taskLinkError'));
    } finally {
      setBusy(false);
    }
  };

  const handleCreate = async () => {
    const title = newTitle.trim();
    if (!title) return;
    try {
      setBusy(true);
      // Scrum tasks may live in the product backlog; Kanban tasks need a cycle (the board's default one).
      let cycleId: number | undefined;
      if (projectType !== 'SCRUM') {
        const cycles = await cycleService.getActiveByProject(projectId);
        cycleId = cycles.data.find((c) => c.projectId === projectId)?.id;
        if (cycleId === undefined) {
          showError(t('backlogPage.noDefaultCycle'));
          return;
        }
      }
      await taskService.create({
        title,
        epicId,
        cycleId,
        projectId: cycleId === undefined ? projectId : undefined,
        status: 'BACKLOG',
        priority: 'MEDIUM',
      });
      setNewTitle('');
      showSuccess(t('epics.taskCreated'));
      onChanged();
    } catch (error) {
      console.error('Failed to create task:', error);
      showError(t('epics.taskCreateError'));
    } finally {
      setBusy(false);
    }
  };

  const doneCount = tasks.filter((tk) => tk.status === 'DONE').length;

  return (
    <Card data-testid="epic-tasks-section">
      <CardHeader className="space-y-3">
        <CardTitle className="flex items-center gap-2">
          <ListChecks className="h-5 w-5" />
          {t('epics.tasks')} ({doneCount}/{tasks.length})
        </CardTitle>
        <div className="flex flex-col gap-2 sm:flex-row">
          <div className="flex flex-1 gap-2">
            <Input
              value={newTitle}
              onChange={(e) => setNewTitle(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleCreate();
              }}
              placeholder={t('epics.newTaskPlaceholder')}
              aria-label={t('epics.newTask')}
            />
            <Button onClick={handleCreate} disabled={busy || !newTitle.trim()}>
              <Plus className="h-4 w-4 mr-2" />
              {t('epics.newTask')}
            </Button>
          </div>
          <Combobox
            key={pickerKey}
            options={options}
            onValueChange={handleLink}
            placeholder={t('epics.linkExistingTask')}
            searchPlaceholder={t('epics.searchTasks')}
            emptyText={t('epics.noUnlinkedTasks')}
            disabled={busy}
            triggerClassName="sm:w-64"
          />
        </div>
      </CardHeader>
      <CardContent>
        {tasks.length === 0 ? (
          <div className="py-8 text-center text-muted-foreground">
            <Link2 className="h-8 w-8 mx-auto mb-2" />
            <p>{t('epics.noTasks')}</p>
            <p className="text-sm mt-2">{t('epics.noTasksHint')}</p>
          </div>
        ) : (
          <ul className="divide-y">
            {tasks.map((task) => (
              <li key={task.id} className="flex items-center gap-3 py-2">
                <CheckCircle2
                  className={`h-4 w-4 shrink-0 ${task.status === 'DONE' ? 'text-green-600' : 'text-muted-foreground'}`}
                />
                <Link to={`/tasks/${task.id}`} className="flex-1 min-w-0 truncate hover:underline">
                  {task.title}
                </Link>
                <Badge variant="outline" className="shrink-0">
                  {t(`backlogPage.statusOptions.${STATUS_KEYS[task.status] ?? 'backlog'}`)}
                </Badge>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleUnlink(task.id)}
                  disabled={busy}
                  aria-label={t('epics.unlinkTask')}
                  title={t('epics.unlinkTask')}
                >
                  <Unlink className="h-4 w-4" />
                </Button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  );
}
