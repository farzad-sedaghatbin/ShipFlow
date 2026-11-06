import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { Plus, X, AlertCircle, Link as LinkIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Label } from '@/components/ui/label';
import { Task, TaskDependency, DependencyType, CreateTaskDependencyRequest } from '../types';
import { taskService } from '../services/taskService';

interface TaskDependenciesProps {
  taskId: number;
  cycleId: number;
  onDependenciesChange?: () => void;
}

export default function TaskDependencies({ taskId, cycleId, onDependenciesChange }: TaskDependenciesProps) {
  const { t } = useTranslation();
  const [blockingTasks, setBlockingTasks] = useState<TaskDependency[]>([]);
  const [blockedByTasks, setBlockedByTasks] = useState<TaskDependency[]>([]);
  const [relatedTasks, setRelatedTasks] = useState<TaskDependency[]>([]);
  const [loading, setLoading] = useState(true);
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [availableTasks, setAvailableTasks] = useState<Task[]>([]);
  const [selectedTaskId, setSelectedTaskId] = useState<string>('');
  const [dependencyType, setDependencyType] = useState<DependencyType>('BLOCKS');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadDependencies();
  }, [taskId]);

  const loadDependencies = async () => {
    try {
      setLoading(true);
      const response = await taskService.getDependencies(taskId);
      setBlockingTasks(response.data.blocking || []);
      setBlockedByTasks(response.data.blockedBy || []);
      setRelatedTasks(response.data.related || []);
    } catch (error) {
      console.error('Failed to load dependencies:', error);
      toast.error(t('errors.loadDependenciesFailed'));
    } finally {
      setLoading(false);
    }
  };

  const loadAvailableTasks = async () => {
    try {
      const response = await taskService.getByCycleId(cycleId);
      const tasks = Array.isArray(response.data) ? response.data : response.data.content || [];
      // Filter out the current task and already linked tasks
      // The server allows one link per pair of tasks, whatever its type or direction
      const existingTaskIds = new Set([
        taskId,
        ...blockingTasks.map(d => d.otherTaskId),
        ...blockedByTasks.map(d => d.otherTaskId),
        ...relatedTasks.map(d => d.otherTaskId),
      ]);
      setAvailableTasks(tasks.filter(t => !existingTaskIds.has(t.id)));
    } catch (error) {
      console.error('Failed to load available tasks:', error);
      toast.error(t('taskDependencies.toast.loadTasksFailed'));
    }
  };

  const handleAddDependency = async () => {
    if (!selectedTaskId) {
      toast.error(t('taskDependencies.toast.selectFirst'));
      return;
    }

    try {
      setSubmitting(true);
      const request: CreateTaskDependencyRequest = {
        targetTaskId: parseInt(selectedTaskId),
        dependencyType,
      };
      await taskService.addDependency(taskId, request);
      toast.success(t('taskDependencies.toast.added'));
      setShowAddDialog(false);
      setSelectedTaskId('');
      setDependencyType('BLOCKS');
      await loadDependencies();
      onDependenciesChange?.();
    } catch (error: any) {
      const message = error.response?.data?.message || t('errors.addDependencyFailed');
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemoveDependency = async (dependencyId: number) => {
    try {
      await taskService.removeDependency(taskId, dependencyId);
      toast.success(t('taskDependencies.toast.removed'));
      await loadDependencies();
      onDependenciesChange?.();
    } catch (error) {
      console.error('Failed to remove dependency:', error);
      toast.error(t('errors.removeDependencyFailed'));
    }
  };

  const handleOpenAddDialog = () => {
    loadAvailableTasks();
    setShowAddDialog(true);
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <LinkIcon className="h-5 w-5" />
            {t('taskDependencies.title')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center text-muted-foreground">{t('taskDependencies.loading')}</div>
        </CardContent>
      </Card>
    );
  }

  const hasBlockers = blockedByTasks.length > 0;

  // Always link to the task on the *other* end, as the server reports it for this task
  const renderRow = (dep: TaskDependency, tone: string) => (
    <div key={dep.id} className={`flex items-center justify-between p-3 rounded-md ${tone}`}>
      <div className="flex-1">
        <Link to={`/backlog/${dep.otherTaskId}`} className="text-sm font-medium hover:underline">
          {dep.otherTaskTitle}
        </Link>
      </div>
      <Button
        variant="ghost"
        size="sm"
        aria-label={t('taskDependencies.remove')}
        onClick={() => handleRemoveDependency(dep.id)}
      >
        <X className="h-4 w-4" />
      </Button>
    </div>
  );

  return (
    <>
      <Card className={hasBlockers ? 'border-orange-300 dark:border-orange-700' : ''}>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <LinkIcon className="h-5 w-5" />
              {t('taskDependencies.title')}
              {hasBlockers && (
                <Badge variant="destructive" className="ml-2">
                  {t('taskDependencies.blockerBadge', { count: blockedByTasks.length })}
                </Badge>
              )}
            </CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={handleOpenAddDialog}
            >
              <Plus className="h-4 w-4 mr-2" />
              {t('taskDependencies.add')}
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Blocked By - tasks that block this task */}
          {blockedByTasks.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-orange-600" />
                <Label className="text-sm font-semibold text-orange-600">{t('taskDependencies.blockedBy')}</Label>
              </div>
              <div className="space-y-2">
                {blockedByTasks.map((dep) => renderRow(dep, 'bg-orange-50 dark:bg-orange-950/20 border border-orange-200 dark:border-orange-800'))}
              </div>
            </div>
          )}

          {/* Blocking - tasks this task blocks */}
          {blockingTasks.length > 0 && (
            <div className="space-y-2">
              <Label className="text-sm font-semibold">{t('taskDependencies.blocks')}</Label>
              <div className="space-y-2">
                {blockingTasks.map((dep) => renderRow(dep, 'bg-muted'))}
              </div>
            </div>
          )}

          {/* Related - informational links, they block nothing */}
          {relatedTasks.length > 0 && (
            <div className="space-y-2">
              <Label className="text-sm font-semibold">{t('taskDependencies.related')}</Label>
              <div className="space-y-2">
                {relatedTasks.map((dep) => renderRow(dep, 'bg-muted'))}
              </div>
            </div>
          )}

          {blockedByTasks.length === 0 && blockingTasks.length === 0 && relatedTasks.length === 0 && (
            <div className="text-center text-muted-foreground py-8">
              {t('taskDependencies.empty')}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add Dependency Dialog */}
      <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{t('taskDependencies.dialog.title')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="p-4 bg-muted rounded-lg space-y-2">
              <p className="text-sm font-medium">💡 {t('taskDependencies.dialog.guideTitle')}</p>
              <ul className="text-xs text-muted-foreground space-y-1 list-disc list-inside">
                <li>{t('taskDependencies.dialog.guideBlocks')}</li>
                <li>{t('taskDependencies.dialog.guideDependsOn')}</li>
                <li>{t('taskDependencies.dialog.guideRelated')}</li>
              </ul>
            </div>

            <div className="space-y-2">
              <Label htmlFor="task">{t('taskDependencies.dialog.selectTask')}</Label>
              <Select value={selectedTaskId} onValueChange={setSelectedTaskId}>
                <SelectTrigger id="task">
                  <SelectValue placeholder={t('taskDependencies.dialog.selectPlaceholder')} />
                </SelectTrigger>
                <SelectContent>
                  {availableTasks.length === 0 ? (
                    <div className="p-2 text-sm text-muted-foreground text-center">
                      {t('taskDependencies.dialog.noTasks')}
                    </div>
                  ) : (
                    availableTasks.map((task) => (
                      <SelectItem key={task.id} value={task.id.toString()}>
                        {task.title}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="type">{t('taskDependencies.dialog.type')}</Label>
              <Select value={dependencyType} onValueChange={(v) => setDependencyType(v as DependencyType)}>
                <SelectTrigger id="type">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {(['BLOCKS', 'DEPENDS_ON', 'RELATED_TO'] as DependencyType[]).map((type) => (
                    <SelectItem key={type} value={type}>
                      <div className="flex flex-col items-start">
                        <span className="font-medium">{t(`taskDependencies.types.${type}`)}</span>
                        <span className="text-xs text-muted-foreground">{t(`taskDependencies.types.${type}_hint`)}</span>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              <div className="p-3 bg-blue-50 dark:bg-blue-950/20 rounded border border-blue-200 dark:border-blue-800">
                <p className="text-sm">{t(`taskDependencies.result.${dependencyType}`)}</p>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAddDialog(false)}>
              {t('taskDependencies.dialog.cancel')}
            </Button>
            <Button onClick={handleAddDependency} disabled={submitting || !selectedTaskId}>
              {submitting ? t('taskDependencies.dialog.submitting') : t('taskDependencies.dialog.submit')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
