import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import dayjs from 'dayjs';
import { toast } from 'sonner';
import { ChevronLeft, Pencil, PlayCircle, Plus, Eye } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Task, TaskStatus, TaskPriority } from '../types';
import { taskService } from '../services/taskService';
import timerService from '../services/timerService';

const statusOptions: { value: TaskStatus; label: string; variant: 'default' | 'secondary' | 'destructive' | 'success' | 'warning' | 'info' | 'outline' }[] = [
  { value: 'BACKLOG', label: 'Backlog', variant: 'secondary' },
  { value: 'TODO', label: 'To Do', variant: 'outline' },
  { value: 'IN_PROGRESS', label: 'In Progress', variant: 'info' },
  { value: 'BLOCKED', label: 'Blocked', variant: 'destructive' },
  { value: 'IN_REVIEW', label: 'In Review', variant: 'warning' },
  { value: 'DONE', label: 'Done', variant: 'success' },
  { value: 'CANCELLED', label: 'Cancelled', variant: 'secondary' },
];

const priorityOptions: { value: TaskPriority; label: string; variant: 'default' | 'secondary' | 'destructive' | 'success' | 'warning' | 'info' | 'outline' }[] = [
  { value: 'LOW', label: 'Low', variant: 'secondary' },
  { value: 'MEDIUM', label: 'Medium', variant: 'info' },
  { value: 'HIGH', label: 'High', variant: 'warning' },
  { value: 'URGENT', label: 'Urgent', variant: 'destructive' },
];

export default function TaskDetailPage() {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();
  const [task, setTask] = useState<Task | null>(null);
  const [subtasks, setSubtasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTimerTaskId, setActiveTimerTaskId] = useState<number | null>(null);
  const [viewSubtask, setViewSubtask] = useState<Task | null>(null);

  useEffect(() => {
    if (taskId) {
      loadTask(parseInt(taskId));
      loadActiveTimer();
    }
  }, [taskId]);

  const loadTask = async (id: number) => {
    try {
      setLoading(true);
      const response = await taskService.getById(id);
      setTask(response.data);
      
      // Load subtasks
      const subtasksResponse = await taskService.getSubTasks(id);
      setSubtasks(subtasksResponse.data);
    } catch (error) {
      console.error('Failed to load task:', error);
      toast.error('Failed to load task');
      navigate('/backlog');
    } finally {
      setLoading(false);
    }
  };

  const loadActiveTimer = async () => {
    try {
      const timer = await timerService.getActiveTimer();
      if (timer && timer.taskId) {
        setActiveTimerTaskId(timer.taskId);
      }
    } catch (error) {
      console.error('Failed to load active timer:', error);
    }
  };

  const handleStartTimer = async () => {
    if (!task) return;
    try {
      await timerService.startTimer({
        taskId: task.id,
        note: `Working on: ${task.title}`,
      });
      setActiveTimerTaskId(task.id);
      await loadActiveTimer();
      toast.success('Timer started for task');
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to start timer';
      toast.error(message);
    }
  };

  const handleEdit = () => {
    navigate(`/backlog?edit=${task?.id}`);
  };

  const handleAddSubtask = () => {
    navigate(`/backlog?addSubtask=${task?.id}`);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-muted-foreground">Loading task...</div>
      </div>
    );
  }

  if (!task) {
    return null;
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header with Back Button */}
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => navigate('/backlog')}
        >
          <ChevronLeft className="h-4 w-4 mr-1" />
          Back to Backlog
        </Button>
      </div>

      {/* Task Header */}
      <Card>
        <CardHeader>
          <div className="flex items-start justify-between">
            <div className="space-y-2 flex-1">
              <div className="flex items-center gap-2">
                {task.parentTaskId && (
                  <span className="text-muted-foreground">└─</span>
                )}
                <CardTitle className="text-2xl">{task.title}</CardTitle>
              </div>
              <div className="flex items-center gap-2">
                <Badge variant={statusOptions.find(s => s.value === task.status)?.variant}>
                  {statusOptions.find(s => s.value === task.status)?.label}
                </Badge>
                <Badge variant={priorityOptions.find(p => p.value === task.priority)?.variant}>
                  {priorityOptions.find(p => p.value === task.priority)?.label}
                </Badge>
              </div>
            </div>
            <div className="flex gap-2">
              {!task.parentTaskId && (
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleAddSubtask}
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Add Subtask
                </Button>
              )}
              <Button
                variant={activeTimerTaskId === task.id ? 'destructive' : 'default'}
                size="sm"
                onClick={handleStartTimer}
                disabled={activeTimerTaskId !== null && activeTimerTaskId !== task.id}
                className={activeTimerTaskId === task.id ? '' : 'bg-green-600 hover:bg-green-700'}
              >
                <PlayCircle className="h-4 w-4 mr-2" />
                {activeTimerTaskId === task.id ? 'Running' : 'Start Timer'}
              </Button>
              <Button
                variant="default"
                size="sm"
                onClick={handleEdit}
              >
                <Pencil className="h-4 w-4 mr-2" />
                Edit
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Task Metadata */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <Label className="text-xs text-muted-foreground">Assignee</Label>
              <div className="mt-1 font-medium">
                {task.assigneeName || 'Unassigned'}
              </div>
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">Pair Assignee</Label>
              <div className="mt-1 font-medium">
                {task.pairAssigneeName || 'None'}
              </div>
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">Estimate</Label>
              <div className="mt-1 font-medium">
                {task.estimateHours ? `${task.estimateHours}h` : '-'}
              </div>
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">Actual</Label>
              <div className="mt-1 font-medium">
                {task.actualHours ? `${task.actualHours}h` : '-'}
              </div>
            </div>
            {task.dueDate && (
              <div>
                <Label className="text-xs text-muted-foreground">Due Date</Label>
                <div className="mt-1 font-medium">
                  {dayjs(task.dueDate).format('MMM D, YYYY')}
                </div>
              </div>
            )}
            {task.parentTaskTitle && (
              <div>
                <Label className="text-xs text-muted-foreground">Parent Task</Label>
                <div className="mt-1 font-medium">
                  <Link to={`/backlog/${task.parentTaskId}`} className="hover:underline text-primary">
                    {task.parentTaskTitle}
                  </Link>
                </div>
              </div>
            )}
            <div>
              <Label className="text-xs text-muted-foreground">Cycle</Label>
              <div className="mt-1 font-medium">
                {task.cycleName}
              </div>
            </div>
            {task.projectName && (
              <div>
                <Label className="text-xs text-muted-foreground">Project</Label>
                <div className="mt-1 font-medium">
                  {task.projectName}
                </div>
              </div>
            )}
          </div>

          {/* Description */}
          {task.description && (
            <div>
              <Label className="text-sm font-semibold">Description</Label>
              <div className="mt-2 p-4 bg-muted rounded-md text-sm whitespace-pre-wrap">
                {task.description}
              </div>
            </div>
          )}

          {/* Tags */}
          {task.tags && (
            <div>
              <Label className="text-sm font-semibold">Tags</Label>
              <div className="mt-2 flex flex-wrap gap-2">
                {task.tags.split(',').map((tag, idx) => (
                  <Badge key={idx} variant="outline">
                    {tag.trim()}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          {/* Timestamps */}
          <div className="grid grid-cols-2 gap-4 pt-4 border-t">
            <div>
              <Label className="text-xs text-muted-foreground">Created</Label>
              <div className="mt-1 text-sm">
                {dayjs(task.createdAt).format('MMM D, YYYY h:mm A')}
                {task.createdByName && ` by ${task.createdByName}`}
              </div>
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">Updated</Label>
              <div className="mt-1 text-sm">
                {dayjs(task.updatedAt).format('MMM D, YYYY h:mm A')}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Subtasks */}
      {subtasks.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Subtasks ({subtasks.length})</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {subtasks.map((subtask) => (
                <div
                  key={subtask.id}
                  className="flex items-start justify-between p-4 border rounded-lg hover:bg-muted/50 transition-colors"
                >
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-muted-foreground">└─</span>
                      <Link 
                        to={`/backlog/${subtask.id}`}
                        className="font-medium hover:underline"
                      >
                        {subtask.title}
                      </Link>
                    </div>
                    {subtask.description && (
                      <p className="text-sm text-muted-foreground ml-6 mt-1">
                        {subtask.description}
                      </p>
                    )}
                    {subtask.assigneeName && (
                      <p className="text-xs text-muted-foreground ml-6 mt-1">
                        Assigned to: {subtask.assigneeName}
                      </p>
                    )}
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={statusOptions.find(s => s.value === subtask.status)?.variant}>
                      {statusOptions.find(s => s.value === subtask.status)?.label}
                    </Badge>
                    <Badge variant={priorityOptions.find(p => p.value === subtask.priority)?.variant}>
                      {priorityOptions.find(p => p.value === subtask.priority)?.label}
                    </Badge>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => setViewSubtask(subtask)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* View Subtask Dialog */}
      <Dialog open={!!viewSubtask} onOpenChange={(open) => !open && setViewSubtask(null)}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{viewSubtask?.title}</DialogTitle>
          </DialogHeader>
          {viewSubtask && (
            <div className="space-y-4">
              {/* Status and Priority */}
              <div className="flex items-center gap-2">
                <Badge variant={statusOptions.find(s => s.value === viewSubtask.status)?.variant}>
                  {statusOptions.find(s => s.value === viewSubtask.status)?.label}
                </Badge>
                <Badge variant={priorityOptions.find(p => p.value === viewSubtask.priority)?.variant}>
                  {priorityOptions.find(p => p.value === viewSubtask.priority)?.label}
                </Badge>
              </div>

              {/* Metadata */}
              <div className="grid grid-cols-2 gap-4">
                {viewSubtask.assigneeName && (
                  <div>
                    <Label className="text-xs text-muted-foreground">Assigned To</Label>
                    <div className="mt-1 font-medium">{viewSubtask.assigneeName}</div>
                  </div>
                )}
                {viewSubtask.estimateHours && (
                  <div>
                    <Label className="text-xs text-muted-foreground">Estimate</Label>
                    <div className="mt-1 font-medium">{viewSubtask.estimateHours}h</div>
                  </div>
                )}
                {viewSubtask.dueDate && (
                  <div>
                    <Label className="text-xs text-muted-foreground">Due Date</Label>
                    <div className="mt-1 font-medium">
                      {dayjs(viewSubtask.dueDate).format('MMM D, YYYY')}
                    </div>
                  </div>
                )}
              </div>

              {/* Description */}
              {viewSubtask.description && (
                <div>
                  <Label className="text-sm font-semibold">Description</Label>
                  <div className="mt-2 p-4 bg-muted rounded-md text-sm whitespace-pre-wrap">
                    {viewSubtask.description}
                  </div>
                </div>
              )}

              {/* Tags */}
              {viewSubtask.tags && (
                <div>
                  <Label className="text-sm font-semibold">Tags</Label>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {viewSubtask.tags.split(',').map((tag, idx) => (
                      <Badge key={idx} variant="outline">
                        {tag.trim()}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex justify-end gap-2 pt-4 border-t">
                <Button variant="outline" onClick={() => setViewSubtask(null)}>
                  Close
                </Button>
                <Button onClick={() => {
                  setViewSubtask(null);
                  navigate(`/backlog/${viewSubtask.id}`);
                }}>
                  View Full Details
                </Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
