import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { LocalizedDateInput } from '../components/LocalizedDateInput';
import dayjs, { Dayjs } from 'dayjs';
import { toast } from 'sonner';
import { ChevronLeft, Pencil, PlayCircle, Plus, Eye, Loader2, Square, Clock, Link2, FolderInput } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import MarkdownEditor from '@/components/MarkdownEditor';
import { Markdown } from '@/components/ui/markdown';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Combobox } from '@/components/ui/combobox';
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select';
import { Task, TaskStatus, TaskPriority, CreateTaskRequest, Cycle, Person, Pitch, Team, Release } from '../types';
import { taskService } from '../services/taskService';
import { cycleService } from '../services/cycleService';
import { personService } from '../services/personService';
import { teamService } from '../services/teamService';
import { pitchService } from '../services/pitchService';
import { releaseService } from '../services/releaseService';
import timerService, { WorkLogTimer } from '../services/timerService';
import { workLogService } from '../services/workLogService';
import GitHubLinksCard from '../components/GitHubLinksCard';
import { TestCasesSection } from '../components/pitchDetail/TestCasesSection';
import TaskAttachments from '../components/TaskAttachments';
import LinkedWikiPages from '../components/LinkedWikiPages';
import { CustomFieldsSection } from '../components/CustomFieldsSection';
import TaskDependencies from '../components/TaskDependencies';
import Comments from '../components/Comments';
import TaskWorkLogsSection from '../components/TaskWorkLogsSection';
import { SoftDeleteButton } from '../components/SoftDeleteButton';
import { ActivityTimeline } from '../components/ActivityTimeline';
import { getUserFriendlyError } from '../utils/errorMessages';
import { useAuth } from '../contexts';
import { MoveToProjectDialog } from '../components/MoveToProjectDialog';

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
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const { t } = useTranslation();
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();
  const [task, setTask] = useState<Task | null>(null);
  const [subtasks, setSubtasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTimerTaskId, setActiveTimerTaskId] = useState<number | null>(null);
  const [activeTimer, setActiveTimer] = useState<WorkLogTimer | null>(null);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [stopDialogOpen, setStopDialogOpen] = useState(false);
  const [workLogNote, setWorkLogNote] = useState('');
  const [stoppingTimer, setStoppingTimer] = useState(false);
  const [viewSubtask, setViewSubtask] = useState<Task | null>(null);
  const [inlineUpdating, setInlineUpdating] = useState<'status' | 'assignee' | 'release' | null>(null);
  const [releases, setReleases] = useState<Release[]>([]);

  // Edit dialog state
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [formData, setFormData] = useState<CreateTaskRequest>({
    title: '',
    description: '',
    cycleId: 0,
    status: 'BACKLOG',
    priority: 'MEDIUM',
    estimateHours: undefined,
    teamId: undefined,
    assigneeId: undefined,
    pairAssigneeId: undefined,
    dueDate: undefined,
    tags: '',
    category: 'PITCH_SCOPE',
    pitchId: undefined,
  });
  const [dueDate, setDueDate] = useState<Dayjs | null>(null);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [moveDialogOpen, setMoveDialogOpen] = useState(false);

  useEffect(() => {
    if (taskId) {
      loadTask(parseInt(taskId));
      loadActiveTimer();
      loadInitialData();
    }
  }, [taskId]);

  // Tick elapsed seconds when timer is running for this task
  useEffect(() => {
    if (!activeTimer || activeTimer.taskId !== task?.id) return;
    const interval = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(interval);
  }, [activeTimer, task?.id]);

  const loadInitialData = async () => {
    try {
      const [cyclesRes, personsRes, teamsRes] = await Promise.all([
        cycleService.getMyActiveCycles(),
        personService.getAll(),
        teamService.getAll(),
      ]);
      setCycles(cyclesRes.data);
      const currentPersonId = user?.personId;
      if (currentPersonId) {
        personsRes.sort((a: Person, b: Person) => (a.id === currentPersonId ? -1 : b.id === currentPersonId ? 1 : 0));
      }
      setPersons(personsRes);
      setTeams(teamsRes.data);
    } catch (error) {
      console.error('Failed to load data:', error);
    }
  };

  const loadTask = async (id: number) => {
    try {
      setLoading(true);
      const response = await taskService.getById(id);
      setTask(response.data);

      // Load subtasks
      const subtasksResponse = await taskService.getSubTasks(id);
      setSubtasks(subtasksResponse.data || []);

      // Load releases for the target-release picker (standalone, non-pitch tasks only)
      if (!response.data.pitchId && response.data.projectId) {
        try {
          const releasesRes = await releaseService.getByProject(response.data.projectId);
          setReleases(releasesRes.data);
        } catch {
          setReleases([]);
        }
      }
    } catch (error: any) {
      console.error('Failed to load task:', error);
      const status = error?.response?.status;
      if (status === 404) {
        toast.error('This task no longer exists');
      } else {
        toast.error('Failed to load task');
      }
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
        setActiveTimer(timer);
        setElapsedSeconds(timer.elapsedSeconds);
        setWorkLogNote(timer.note || '');
      } else {
        setActiveTimerTaskId(null);
        setActiveTimer(null);
        setElapsedSeconds(0);
      }
    } catch (error) {
      console.error('Failed to load active timer:', error);
    }
  };

  const loadPitchesForCycle = async (cycleId: number) => {
    try {
      const response = await pitchService.getByCycleId(cycleId);
      setPitches(response.data);
    } catch (error) {
      console.error('Failed to load pitches:', error);
      setPitches([]);
    }
  };

  const handlePitchChange = (pitchId: string) => {
    const pitch = pitchId === 'none' ? undefined : Number(pitchId);
    setFormData({ ...formData, pitchId: pitch });
  };

  const handleStartTimer = async () => {
    if (!task) return;
    try {
      await timerService.startTimer({
        taskId: task.id,
        note: `Working on: ${task.title}`,
      });
      await loadActiveTimer();
      toast.success(t('taskDetailPage.timerStarted'));
    } catch (error: any) {
      const message = error.response?.data?.message || t('taskDetailPage.timerStartFailed');
      toast.error(message);
    }
  };

  const formatElapsed = (seconds: number): string => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  const handleStopTimer = async () => {
    if (!activeTimer) return;
    try {
      setStoppingTimer(true);
      const hours = Math.round((elapsedSeconds / 3600) * 4) / 4;
      await workLogService.createMy({
        taskId: activeTimer.taskId,
        pitchId: activeTimer.pitchId,
        date: new Date().toISOString().split('T')[0],
        hoursSpent: hours,
        note: workLogNote.trim() || undefined,
      });
      await timerService.cancelTimer();
      setActiveTimer(null);
      setActiveTimerTaskId(null);
      setElapsedSeconds(0);
      setWorkLogNote('');
      setStopDialogOpen(false);
      toast.success(t('taskDetailPage.timerStopped'));
    } catch (error: any) {
      toast.error(error.response?.data?.message || t('taskDetailPage.timerStopFailed'));
    } finally {
      setStoppingTimer(false);
    }
  };

  const handleCopyPreviewLink = () => {
    if (!task) return;
    const url = `${window.location.origin}/preview/task/${task.id}`;
    navigator.clipboard.writeText(url);
    toast.success(t('common.linkCopied'));
  };

  const handleEdit = () => {
    if (!task) return;
    
    // Populate form with current task data
    setFormData({
      title: task.title,
      description: task.description || '',
      cycleId: task.cycleId,
      status: task.status,
      priority: task.priority,
      estimateHours: task.estimateHours,
      teamId: task.teamId,
      assigneeId: task.assigneeId,
      pairAssigneeId: task.pairAssigneeId,
      dueDate: task.dueDate,
      tags: task.tags || '',
      category: task.category,
      parentTaskId: task.parentTaskId,
      pitchId: task.pitchId,
    });
    
    if (task.dueDate) {
      setDueDate(dayjs(task.dueDate));
    } else {
      setDueDate(null);
    }
    
    // Load pitches for the cycle
    if (task.cycleId) {
      loadPitchesForCycle(task.cycleId);
    }
    
    setFieldErrors({});
    setEditDialogOpen(true);
  };

  const validateTaskForm = (): boolean => {
    const errors: Record<string, string> = {};
    if (!formData.title.trim()) {
      errors.title = 'Title is required';
    }
    if (!formData.cycleId || formData.cycleId === 0) {
      errors.cycleId = 'Cycle is required';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSaveTask = async () => {
    if (!validateTaskForm() || !task) {
      return;
    }

    try {
      setSaving(true);
      const data = {
        ...formData,
        dueDate: dueDate ? dueDate.format('YYYY-MM-DD') : undefined,
      };
      
      await taskService.update(task.id, data);
      toast.success('Task updated successfully');
      setEditDialogOpen(false);
      loadTask(task.id);
    } catch (error: any) {
      const message = getUserFriendlyError(error);
      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

  const handleAddSubtask = () => {
    navigate(`/backlog?addSubtask=${task?.id}`);
  };

  const handleInlineStatusChange = async (newStatus: TaskStatus) => {
    if (!task || newStatus === task.status) return;
    const previous = task.status;
    setTask({ ...task, status: newStatus });
    setInlineUpdating('status');
    try {
      await taskService.updateStatus(task.id, newStatus);
      toast.success(t('backlogPage.statusUpdated'));
    } catch (error: any) {
      setTask((current) => (current ? { ...current, status: previous } : current));
      toast.error(getUserFriendlyError(error));
    } finally {
      setInlineUpdating(null);
    }
  };

  const handleInlineAssigneeChange = async (value: string) => {
    if (!task) return;
    const newAssigneeId = value ? parseInt(value, 10) : null;
    if (newAssigneeId === (task.assigneeId ?? null)) return;
    const previous = { assigneeId: task.assigneeId, assigneeName: task.assigneeName };
    const newAssignee = newAssigneeId ? persons.find((p) => p.id === newAssigneeId) : undefined;
    setTask({ ...task, assigneeId: newAssigneeId ?? undefined, assigneeName: newAssignee?.name });
    setInlineUpdating('assignee');
    try {
      await taskService.updateAssignee(task.id, newAssigneeId);
      toast.success(t('backlogPage.assigneeUpdated'));
    } catch (error: any) {
      setTask((current) => (current ? { ...current, ...previous } : current));
      toast.error(getUserFriendlyError(error));
    } finally {
      setInlineUpdating(null);
    }
  };

  const handleInlineReleaseChange = async (value: string) => {
    if (!task) return;
    const newReleaseId = value === 'none' ? null : parseInt(value, 10);
    if (newReleaseId === (task.targetReleaseId ?? null)) return;
    const previous = { targetReleaseId: task.targetReleaseId, targetReleaseName: task.targetReleaseName, targetReleaseVersion: task.targetReleaseVersion };
    setInlineUpdating('release');
    try {
      if (newReleaseId) {
        const response = await taskService.setTargetRelease(task.id, newReleaseId);
        setTask(response.data);
      } else {
        const response = await taskService.clearTargetRelease(task.id);
        setTask(response.data);
      }
      toast.success(t('taskDetailPage.targetReleaseUpdated'));
    } catch (error: any) {
      setTask((current) => (current ? { ...current, ...previous } : current));
      toast.error(getUserFriendlyError(error));
    } finally {
      setInlineUpdating(null);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-muted-foreground">{t('taskDetailPage.loadingTask')}</div>
      </div>
    );
  }

  if (!task) {
    return null;
  }

  // Subtasks can never be set to Backlog (they're meant to be actioned right away, and the
  // backend rejects it) — filter it out of the editable status controls when this task is a
  // subtask. Read-only badges elsewhere still use the unfiltered `statusOptions`.
  const editableStatusOptions = task.parentTaskId
    ? statusOptions.filter((option) => option.value !== 'BACKLOG')
    : statusOptions;

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
          {t('taskDetailPage.backToBacklog')}
        </Button>
      </div>

      {/* Task Header */}
      <Card data-tour="task-detail">
        <CardHeader>
          {/* Stacks on mobile; the action row (up to 5 controls — Add
              Subtask, Start/Stop Timer with live elapsed time, Copy Link,
              Edit, Delete) also wraps instead of overflowing a narrow
              viewport. */}
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="space-y-2 flex-1">
              <div className="flex items-center gap-2">
                {task.parentTaskId && (
                  <span className="text-muted-foreground">└─</span>
                )}
                <div>
                  <p className="text-sm font-mono text-muted-foreground mb-0.5">
                    {task.projectKey ? `${task.projectKey}-${task.id}` : `#${task.id}`}
                  </p>
                  <CardTitle className="text-2xl">{task.title}</CardTitle>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Select
                  value={task.status}
                  onValueChange={(value) => handleInlineStatusChange(value as TaskStatus)}
                  disabled={inlineUpdating === 'status'}
                >
                  <SelectTrigger className="h-auto w-auto border-0 px-0 py-0 bg-transparent focus:ring-0 gap-1 [&>svg]:hidden">
                    <Badge variant={statusOptions.find(s => s.value === task.status)?.variant} className="cursor-pointer">
                      {statusOptions.find(s => s.value === task.status)?.label}
                    </Badge>
                  </SelectTrigger>
                  <SelectContent>
                    {editableStatusOptions.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Badge variant={priorityOptions.find(p => p.value === task.priority)?.variant}>
                  {priorityOptions.find(p => p.value === task.priority)?.label}
                </Badge>
              </div>
            </div>
            <div className="flex flex-wrap gap-2">
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
              {activeTimerTaskId === task.id ? (
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => setStopDialogOpen(true)}
                  className="gap-2 tabular-nums"
                >
                  <Square className="h-4 w-4" />
                  {t('taskDetailPage.stopTimer')} · {formatElapsed(elapsedSeconds)}
                </Button>
              ) : (
                <Button
                  variant="default"
                  size="sm"
                  onClick={handleStartTimer}
                  disabled={activeTimerTaskId !== null}
                  className="bg-green-600 hover:bg-green-700 gap-2"
                >
                  <PlayCircle className="h-4 w-4" />
                  {t('taskDetailPage.startTimer')}
                </Button>
              )}
              <Button
                variant="ghost"
                size="icon"
                onClick={handleCopyPreviewLink}
                title={t('common.copyLink')}
              >
                <Link2 className="h-4 w-4" />
              </Button>
              <Button
                variant="default"
                size="sm"
                onClick={handleEdit}
              >
                <Pencil className="h-4 w-4 mr-2" />
                Edit
              </Button>
              <SoftDeleteButton
                entityType="task"
                entityId={task.id}
                entityTitle={task.title}
                onSuccess={() => {
                  // Navigate back to backlog after successful deletion
                  navigate('/backlog');
                }}
                variant="outline"
                size="sm"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Task Metadata */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <Label className="text-xs text-muted-foreground">{t('taskDetailPage.assignee')}</Label>
              <div className="mt-1">
                <Combobox
                  options={[
                    { value: '', label: t('common.unassigned') },
                    ...persons.map((person) => ({ value: person.id.toString(), label: person.name })),
                  ]}
                  value={task.assigneeId?.toString() ?? ''}
                  onValueChange={handleInlineAssigneeChange}
                  disabled={inlineUpdating === 'assignee'}
                  searchPlaceholder={t('bugReports.filters.searchAssignee', 'Search people...')}
                  emptyText={t('bugReports.filters.noAssignee', 'No people found')}
                  triggerClassName="h-auto border-0 px-0 py-0 font-medium justify-start hover:bg-transparent [&>svg]:hidden"
                />
              </div>
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">{t('taskDetailPage.pairAssignee')}</Label>
              <div className="mt-1 font-medium">
                {task.pairAssigneeName || t('common.none')}
              </div>
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">{t('taskDetailPage.estimate')}</Label>
              <div className="mt-1 font-medium">
                {task.estimateHours ? `${task.estimateHours}h` : '-'}
              </div>
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">{t('taskDetailPage.actualHours')}</Label>
              <div className="mt-1 font-medium">
                {task.actualHours ? `${task.actualHours}h` : '-'}
              </div>
            </div>
            {task.dueDate && (
              <div>
                <Label className="text-xs text-muted-foreground">{t('taskDetailPage.dueDate')}</Label>
                <div className="mt-1 font-medium">
                  {dayjs(task.dueDate).format('MMM D, YYYY')}
                </div>
              </div>
            )}
            {task.parentTaskTitle && (
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.parentTask')}</Label>
                <div className="mt-1 font-medium">
                  <Link to={`/backlog/${task.parentTaskId}`} className="hover:underline text-primary">
                    {task.parentTaskTitle}
                  </Link>
                </div>
              </div>
            )}
            {task.teamName && (
              <div>
                <Label className="text-xs text-muted-foreground">{t('backlogPage.team')}</Label>
                <div className="mt-1 font-medium">{task.teamName}</div>
              </div>
            )}
            <div>
              <Label className="text-xs text-muted-foreground">{t('taskDetailPage.cycle')}</Label>
              <div className="mt-1 font-medium">
                {task.cycleName}
              </div>
            </div>
            {task.projectName && (
              <div>
                <Label className="text-xs text-muted-foreground">{t('common.project')}</Label>
                <div className="mt-1 flex items-center gap-2">
                  <span className="font-medium">{task.projectName}</span>
                  {isAdmin && (
                    <Button variant="ghost" size="sm" className="h-6 px-2 text-xs text-muted-foreground" onClick={() => setMoveDialogOpen(true)}>
                      <FolderInput className="h-3 w-3 mr-1" />
                      {t('moveToProject.confirm')}
                    </Button>
                  )}
                </div>
              </div>
            )}
            {task.pitchId && task.pitchTitle && (
              <div>
                <Label className="text-xs text-muted-foreground">{t('common.pitch', 'Pitch')}</Label>
                <div className="mt-1 font-medium">
                  <Link to={`/pitches/${task.pitchId}`} className="hover:underline text-primary">
                    {task.pitchTitle}
                  </Link>
                </div>
              </div>
            )}
            {!task.pitchId && (
              <div>
                <Label className="text-xs text-muted-foreground">{t('taskDetailPage.targetRelease')}</Label>
                <div className="mt-1">
                  <Select
                    value={task.targetReleaseId ? String(task.targetReleaseId) : 'none'}
                    onValueChange={handleInlineReleaseChange}
                    disabled={inlineUpdating === 'release'}
                  >
                    <SelectTrigger className="h-auto w-auto border-0 px-0 py-0 bg-transparent focus:ring-0 gap-1 font-medium">
                      {task.targetReleaseId ? (
                        <Badge variant="outline" className="cursor-pointer">
                          v{task.targetReleaseVersion} {task.targetReleaseName}
                        </Badge>
                      ) : (
                        <span className="text-muted-foreground">{t('taskDetailPage.noTargetRelease')}</span>
                      )}
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">{t('taskDetailPage.noTargetRelease')}</SelectItem>
                      {releases.map((release) => (
                        <SelectItem key={release.id} value={String(release.id)}>
                          v{release.version} — {release.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            )}
          </div>

          {/* Description */}
          {task.description && (
            <div>
              <Label className="text-sm font-semibold">{t('common.description')}</Label>
              <div className="mt-2 p-4 bg-muted rounded-md text-sm">
                <Markdown content={task.description} />
              </div>
            </div>
          )}

          {/* Tags */}
          {task.tags && (
            <div>
              <Label className="text-sm font-semibold">{t('taskDetailPage.tags')}</Label>
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
              <Label className="text-xs text-muted-foreground">{t('backlogPage.created')}</Label>
              <div className="mt-1 text-sm">
                {dayjs(task.createdAt).format('MMM D, YYYY h:mm A')}
                {task.createdByName && ` ${t('common.by')} ${task.createdByName}`}
              </div>
            </div>
            <div>
              <Label className="text-xs text-muted-foreground">{t('backlogPage.updated')}</Label>
              <div className="mt-1 text-sm">
                {dayjs(task.updatedAt).format('MMM D, YYYY h:mm A')}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* File Attachments */}
      <TaskAttachments taskId={task.id} />

      {/* Linked Wiki Pages */}
      <LinkedWikiPages entityType="TASK" entityId={task.id} />

      {/* Custom Fields */}
      <CustomFieldsSection
        entityType="TASK"
        entityId={task.id}
        projectId={task.projectId}
      />

      {/* GitHub Integration */}
      <GitHubLinksCard taskId={task.id} />

      {/* Test Cases */}
      <TestCasesSection taskId={task.id} />

      {/* Task Dependencies */}
      <TaskDependencies 
        taskId={task.id} 
        cycleId={task.cycleId}
        onDependenciesChange={() => loadTask(task.id)}
      />

      {/* Comments */}
      <Comments
        entityType="task"
        entityId={task.id}
      />

      {/* Work Logs */}
      <TaskWorkLogsSection taskId={task.id} />

      {/* Activity Timeline */}
      <ActivityTimeline
        entityId={task.id}
        fetchHistory={async (page, size) => {
          const response = await taskService.getHistory(task.id, page, size);
          return response.data;
        }}
      />

      {/* Subtasks */}
      {subtasks.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t('taskDetailPage.subtasks')} ({subtasks.length})</CardTitle>
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
                        {t('backlogPage.assignedTo')}: {subtask.assigneeName}
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
                  <div className="mt-2 p-4 bg-muted rounded-md text-sm">
                    <Markdown content={viewSubtask.description} />
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

      {/* Edit Task Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{t('taskDetailPage.editTask')}</DialogTitle>
            <DialogDescription>
              {t('backlogPage.updateTaskDetails')}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="edit-title">{t('common.title')} *</Label>
              <Input
                id="edit-title"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                placeholder={t('backlogPage.taskTitle')}
                className={fieldErrors.title ? 'border-destructive' : ''}
              />
              {fieldErrors.title && (
                <p className="text-sm text-destructive">{fieldErrors.title}</p>
              )}
            </div>
            
            <div className="grid gap-2">
              <Label>{t('common.description')}</Label>
              <MarkdownEditor
                id="edit-description"
                value={formData.description}
                onChange={(value) => setFormData({ ...formData, description: value })}
                placeholder={t('backlogPage.taskDescription')}
                rows={4}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="edit-status">{t('common.status')} *</Label>
                <Combobox
                  options={editableStatusOptions.map(opt => ({ value: opt.value, label: opt.label }))}
                  value={formData.status}
                  onValueChange={(value) => setFormData({ ...formData, status: value as TaskStatus })}
                  placeholder="Select status"
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="edit-priority">Priority *</Label>
                <Combobox
                  options={priorityOptions.map(opt => ({ value: opt.value, label: opt.label }))}
                  value={formData.priority}
                  onValueChange={(value) => setFormData({ ...formData, priority: value as TaskPriority })}
                  placeholder="Select priority"
                />
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="edit-cycle">Cycle *</Label>
              <Combobox
                options={cycles.map(cycle => ({ value: cycle.id.toString(), label: cycle.name }))}
                value={formData.cycleId !== undefined ? formData.cycleId.toString() : ''}
                onValueChange={(value) => {
                  const cycleId = parseInt(value);
                  setFormData({ ...formData, cycleId, pitchId: undefined });
                  loadPitchesForCycle(cycleId);
                }}
                placeholder="Select cycle"
                className={fieldErrors.cycleId ? 'border-destructive' : ''}
              />
              {fieldErrors.cycleId && (
                <p className="text-sm text-destructive">{fieldErrors.cycleId}</p>
              )}
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="edit-assignee">Assignee</Label>
                <Combobox
                  options={[
                    { value: 'none', label: 'None' },
                    ...persons.map(person => ({ value: person.id.toString(), label: person.name }))
                  ]}
                  value={formData.assigneeId?.toString() || 'none'}
                  onValueChange={(value) =>
                    setFormData({ ...formData, assigneeId: value === 'none' ? undefined : parseInt(value) })
                  }
                  placeholder="Select assignee"
                  searchPlaceholder="Search persons..."
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="edit-pair-assignee">Pair Assignee</Label>
                <Combobox
                  options={[
                    { value: 'none', label: 'None' },
                    ...persons.map(person => ({ value: person.id.toString(), label: person.name }))
                  ]}
                  value={formData.pairAssigneeId?.toString() || 'none'}
                  onValueChange={(value) =>
                    setFormData({ ...formData, pairAssigneeId: value === 'none' ? undefined : parseInt(value) })
                  }
                  placeholder="Select pair assignee"
                  searchPlaceholder="Search persons..."
                />
              </div>
            </div>

            {teams.length > 0 && (
              <div className="grid gap-2">
                <Label>{t('backlogPage.team')}</Label>
                <Combobox
                  options={[
                    { value: 'none', label: t('backlogPage.noTeam') },
                    ...teams.map(team => ({ value: team.id.toString(), label: team.name }))
                  ]}
                  value={formData.teamId?.toString() || 'none'}
                  onValueChange={(value) =>
                    setFormData({ ...formData, teamId: value === 'none' ? undefined : parseInt(value) })
                  }
                  placeholder={t('backlogPage.noTeam')}
                />
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="edit-estimate">Estimate (hours)</Label>
                <Input
                  id="edit-estimate"
                  type="number"
                  min="0"
                  step="0.5"
                  value={formData.estimateHours || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      estimateHours: e.target.value ? parseFloat(e.target.value) : undefined,
                    })
                  }
                  placeholder="0"
                />
              </div>

              <div className="grid gap-2">
                <Label htmlFor="edit-due-date">Due Date</Label>
                <LocalizedDateInput
                  id="edit-due-date"
                  value={dueDate ? dueDate.format('YYYY-MM-DD') : ''}
                  onChange={(val) => setDueDate(val ? dayjs(val) : null)}
                />
              </div>
            </div>

            <div className="grid gap-2">
              <Label htmlFor="edit-tags">Tags</Label>
              <Input
                id="edit-tags"
                value={formData.tags}
                onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                placeholder="comma, separated, tags"
              />
            </div>

            <div className="grid gap-2">
              <Label>Pitch (optional)</Label>
              <Combobox
                options={[
                  { value: 'none', label: 'No pitch (technical debt)' },
                  ...pitches.map(pitch => ({ value: String(pitch.id), label: pitch.title }))
                ]}
                value={formData.pitchId ? String(formData.pitchId) : 'none'}
                onValueChange={handlePitchChange}
                placeholder="No pitch (technical debt)"
                searchPlaceholder="Search pitches..."
              />
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setEditDialogOpen(false)} disabled={saving}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleSaveTask} disabled={saving}>
              {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {t('common.save')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Stop Timer Dialog */}
      <Dialog open={stopDialogOpen} onOpenChange={setStopDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Clock className="h-5 w-5 text-primary" />
              {t('taskDetailPage.stopTimerDialog.title')}
            </DialogTitle>
            <DialogDescription>
              {t('taskDetailPage.stopTimerDialog.description')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="rounded-lg bg-muted p-4 text-center">
              <div className="text-3xl font-bold tabular-nums text-primary">
                {formatElapsed(elapsedSeconds)}
              </div>
              <div className="text-sm text-muted-foreground mt-1">
                {(Math.round((elapsedSeconds / 3600) * 4) / 4).toFixed(2)} {t('taskDetailPage.stopTimerDialog.hours')}
                <span className="ml-1 text-xs">({t('taskDetailPage.stopTimerDialog.rounded')})</span>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="stop-timer-note">{t('taskDetailPage.stopTimerDialog.notes')}</Label>
              <Textarea
                id="stop-timer-note"
                placeholder={t('taskDetailPage.stopTimerDialog.notesPlaceholder')}
                value={workLogNote}
                onChange={(e) => setWorkLogNote(e.target.value)}
                rows={3}
                className="resize-none"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setStopDialogOpen(false)} disabled={stoppingTimer}>
              {t('common.cancel')}
            </Button>
            <Button variant="destructive" onClick={handleStopTimer} disabled={stoppingTimer}>
              {stoppingTimer && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              <Square className="h-4 w-4 mr-2" />
              {t('taskDetailPage.stopTimerDialog.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {isAdmin && task && (
        <MoveToProjectDialog
          open={moveDialogOpen}
          onOpenChange={setMoveDialogOpen}
          entityType="task"
          entityId={task.id}
          entityTitle={task.title}
          currentProjectId={task.projectId}
          onSuccess={() => navigate(-1)}
        />
      )}
    </div>
  );
}
