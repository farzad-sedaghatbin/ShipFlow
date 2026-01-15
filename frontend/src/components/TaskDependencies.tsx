import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
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
  const [blockingTasks, setBlockingTasks] = useState<TaskDependency[]>([]);
  const [blockedByTasks, setBlockedByTasks] = useState<TaskDependency[]>([]);
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
    } catch (error) {
      console.error('Failed to load dependencies:', error);
      toast.error('Failed to load task dependencies');
    } finally {
      setLoading(false);
    }
  };

  const loadAvailableTasks = async () => {
    try {
      const response = await taskService.getByCycleId(cycleId);
      const tasks = Array.isArray(response.data) ? response.data : response.data.content || [];
      // Filter out the current task and already linked tasks
      const existingTaskIds = new Set([
        taskId,
        ...blockingTasks.map(d => d.targetTaskId),
        ...blockedByTasks.map(d => d.sourceTaskId),
      ]);
      setAvailableTasks(tasks.filter(t => !existingTaskIds.has(t.id)));
    } catch (error) {
      console.error('Failed to load available tasks:', error);
      toast.error('Failed to load available tasks');
    }
  };

  const handleAddDependency = async () => {
    if (!selectedTaskId) {
      toast.error('Please select a task');
      return;
    }

    try {
      setSubmitting(true);
      const request: CreateTaskDependencyRequest = {
        targetTaskId: parseInt(selectedTaskId),
        dependencyType,
      };
      await taskService.addDependency(taskId, request);
      toast.success('Dependency added successfully');
      setShowAddDialog(false);
      setSelectedTaskId('');
      setDependencyType('BLOCKS');
      await loadDependencies();
      onDependenciesChange?.();
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to add dependency';
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemoveDependency = async (dependencyId: number) => {
    try {
      await taskService.removeDependency(taskId, dependencyId);
      toast.success('Dependency removed');
      await loadDependencies();
      onDependenciesChange?.();
    } catch (error) {
      console.error('Failed to remove dependency:', error);
      toast.error('Failed to remove dependency');
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
            Dependencies
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center text-muted-foreground">Loading...</div>
        </CardContent>
      </Card>
    );
  }

  const hasBlockers = blockedByTasks.length > 0;

  return (
    <>
      <Card className={hasBlockers ? 'border-orange-300 dark:border-orange-700' : ''}>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <LinkIcon className="h-5 w-5" />
              Dependencies
              {hasBlockers && (
                <Badge variant="destructive" className="ml-2">
                  {blockedByTasks.length} Blocker{blockedByTasks.length > 1 ? 's' : ''}
                </Badge>
              )}
            </CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={handleOpenAddDialog}
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Dependency
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Blocked By - Tasks blocking this task */}
          {blockedByTasks.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <AlertCircle className="h-4 w-4 text-orange-600" />
                <Label className="text-sm font-semibold text-orange-600">Blocked By</Label>
              </div>
              <div className="space-y-2">
                {blockedByTasks.map((dep) => (
                  <div
                    key={dep.id}
                    className="flex items-center justify-between p-3 bg-orange-50 dark:bg-orange-950/20 rounded-md border border-orange-200 dark:border-orange-800"
                  >
                    <div className="flex-1">
                      <Link
                        to={`/backlog/${dep.sourceTaskId}`}
                        className="text-sm font-medium hover:underline"
                      >
                        {dep.sourceTaskTitle}
                      </Link>
                      <div className="text-xs text-muted-foreground mt-1">
                        Type: {dep.dependencyType}
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleRemoveDependency(dep.id)}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Blocking - Tasks this task is blocking */}
          {blockingTasks.length > 0 && (
            <div className="space-y-2">
              <Label className="text-sm font-semibold">This Task Blocks</Label>
              <div className="space-y-2">
                {blockingTasks.map((dep) => (
                  <div
                    key={dep.id}
                    className="flex items-center justify-between p-3 bg-muted rounded-md"
                  >
                    <div className="flex-1">
                      <Link
                        to={`/backlog/${dep.targetTaskId}`}
                        className="text-sm font-medium hover:underline"
                      >
                        {dep.targetTaskTitle}
                      </Link>
                      <div className="text-xs text-muted-foreground mt-1">
                        Type: {dep.dependencyType}
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleRemoveDependency(dep.id)}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {blockedByTasks.length === 0 && blockingTasks.length === 0 && (
            <div className="text-center text-muted-foreground py-8">
              No dependencies defined for this task
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add Dependency Dialog */}
      <Dialog open={showAddDialog} onOpenChange={setShowAddDialog}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Add Task Dependency</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="p-4 bg-muted rounded-lg space-y-2">
              <p className="text-sm font-medium">💡 Quick Guide:</p>
              <ul className="text-xs text-muted-foreground space-y-1 list-disc list-inside">
                <li><strong>BLOCKS</strong>: Use when this task must finish before another can start</li>
                <li><strong>DEPENDS_ON</strong>: Use when this task is waiting on another task (you're blocked)</li>
                <li><strong>RELATED_TO</strong>: Use for informational links (no blocking)</li>
              </ul>
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="task">Select Task</Label>
              <Select value={selectedTaskId} onValueChange={setSelectedTaskId}>
                <SelectTrigger id="task">
                  <SelectValue placeholder="Select a task from this cycle" />
                </SelectTrigger>
                <SelectContent>
                  {availableTasks.length === 0 ? (
                    <div className="p-2 text-sm text-muted-foreground text-center">
                      No available tasks in this cycle
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
              <Label htmlFor="type">Dependency Type</Label>
              <Select value={dependencyType} onValueChange={(v) => setDependencyType(v as DependencyType)}>
                <SelectTrigger id="type">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="BLOCKS">
                    <div className="flex flex-col items-start">
                      <span className="font-medium">BLOCKS</span>
                      <span className="text-xs text-muted-foreground">This task prevents the other from starting</span>
                    </div>
                  </SelectItem>
                  <SelectItem value="DEPENDS_ON">
                    <div className="flex flex-col items-start">
                      <span className="font-medium">DEPENDS_ON</span>
                      <span className="text-xs text-muted-foreground">This task waits for the other to finish</span>
                    </div>
                  </SelectItem>
                  <SelectItem value="RELATED_TO">
                    <div className="flex flex-col items-start">
                      <span className="font-medium">RELATED_TO</span>
                      <span className="text-xs text-muted-foreground">Just a reference, no blocking</span>
                    </div>
                  </SelectItem>
                </SelectContent>
              </Select>
              
              <div className="p-3 bg-blue-50 dark:bg-blue-950/20 rounded border border-blue-200 dark:border-blue-800">
                <p className="text-sm">
                  {dependencyType === 'BLOCKS' && (
                    <>
                      <strong>Result:</strong> The selected task will be blocked until this task is completed.
                      The selected task will show this task as a blocker.
                    </>
                  )}
                  {dependencyType === 'DEPENDS_ON' && (
                    <>
                      <strong>Result:</strong> This task cannot be completed until the selected task is done.
                      This task will show as blocked by the selected task.
                    </>
                  )}
                  {dependencyType === 'RELATED_TO' && (
                    <>
                      <strong>Result:</strong> Both tasks will show a reference to each other.
                      No blocking relationship is created.
                    </>
                  )}
                </p>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowAddDialog(false)}>
              Cancel
            </Button>
            <Button onClick={handleAddDependency} disabled={submitting || !selectedTaskId}>
              {submitting ? 'Adding...' : 'Add Dependency'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
