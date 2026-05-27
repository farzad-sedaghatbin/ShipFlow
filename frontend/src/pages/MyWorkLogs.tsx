import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2, Pencil, Clock, CalendarDays, Loader2, AlertTriangle, PlayCircle } from 'lucide-react';
import dayjs from 'dayjs';
import { workLogService } from '../services/workLogService';
import { pitchService } from '../services/pitchService';
import { cycleService } from '../services/cycleService';
import { taskService } from '../services/taskService';
import timerService from '../services/timerService';
import { WorkLog, Pitch, Cycle, Task, CreateWorkLogForSelfRequest } from '../types';
import { useAuth, useToast } from '../contexts';
import EmptyState from '../components/EmptyState';
import { EmptyWorkLogsIllustration } from '../components/illustrations';
import TimerWidget from '../components/TimerWidget';
import { cn } from '../lib/utils';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { LocalizedDateInput } from '../components/LocalizedDateInput';

import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Badge } from '../components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { Textarea } from '../components/ui/textarea';
import { Alert, AlertDescription } from '../components/ui/alert';

export default function MyWorkLogs() {
  const { t, i18n } = useTranslation();
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const PAGE_SIZE = 20;
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [allPitches, setAllPitches] = useState<Pitch[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<string>('all');
  const [loading, setLoading] = useState(true);
  const [workLogType, setWorkLogType] = useState<'pitch' | 'task'>('task');

  // Form state
  const [newWorkLog, setNewWorkLog] = useState<CreateWorkLogForSelfRequest>({
    pitchId: 0,
    date: dayjs().format('YYYY-MM-DD'),
    hoursSpent: 0,
    note: '',
  });
  const [workLogDate, setWorkLogDate] = useState<string>(dayjs().format('YYYY-MM-DD'));
  const [selectedPitchId, setSelectedPitchId] = useState<string>('');
  const [selectedTaskId, setSelectedTaskId] = useState<string>('');

  // Edit dialog state
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editingWorkLog, setEditingWorkLog] = useState<WorkLog | null>(null);
  const [editForm, setEditForm] = useState<CreateWorkLogForSelfRequest>({
    pitchId: 0,
    date: '',
    hoursSpent: 0,
    note: '',
  });
  const [editDate, setEditDate] = useState<string>('');
  const [editPitchId, setEditPitchId] = useState<string>('');
  const [editTaskId, setEditTaskId] = useState<string>('');
  const [editWorkLogType, setEditWorkLogType] = useState<'pitch' | 'task'>('task');

  // Timer state
  const [timerLoading, setTimerLoading] = useState(false);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedCycle && selectedCycle !== 'all') {
      const cycleId = parseInt(selectedCycle, 10);
      loadWorkLogs(cycleId);
      loadPitches(cycleId);
      loadTasks(cycleId);
    } else if (selectedCycle === 'all') {
      loadAllWorkLogs();
      setPitches([]);
      setTasks([]);
    }
  }, [page, selectedCycle]);

  const loadInitialData = async () => {
    try {
      const [cyclesRes, pitchesRes] = await Promise.all([
        cycleService.getMyActiveCycles(),
        pitchService.getAll(),
      ]);
      setCycles(cyclesRes.data);
      setAllPitches(pitchesRes.data);
      // Default to 'all' - load all work logs
      loadAllWorkLogs();
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadAllWorkLogs = async () => {
    try {
      const response = await workLogService.getMy(page, PAGE_SIZE);
      setWorkLogs(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch (error) {
      console.error('Failed to load all work logs:', error);
    }
  };

  const loadWorkLogs = async (cycleId: number) => {
    try {
      const response = await workLogService.getMyByCycle(cycleId, page, PAGE_SIZE);
      setWorkLogs(response.data.content);
      setTotalPages(response.data.totalPages);
      setTotalElements(response.data.totalElements);
    } catch (error) {
      console.error('Failed to load work logs:', error);
    }
  };

  const loadPitches = async (cycleId: number) => {
    try {
      const response = await pitchService.getByCycleId(cycleId);
      setPitches(response.data);
    } catch (error) {
      console.error('Failed to load pitches:', error);
    }
  };

  const loadTasks = async (cycleId: number) => {
    try {
      const response = await taskService.getByCycleId(cycleId);
      // getByCycleId without page params returns Task[]
      setTasks(response.data as Task[]);
    } catch (error) {
      console.error('Failed to load tasks:', error);
    }
  };

  const handleCreateWorkLog = async () => {
    if (!workLogDate || !newWorkLog.hoursSpent) return;
    if (workLogType === 'pitch' && !selectedPitchId) return;
    if (workLogType === 'task' && !selectedTaskId) return;
    
    try {
      const payload: CreateWorkLogForSelfRequest = {
        date: workLogDate,
        hoursSpent: newWorkLog.hoursSpent,
        note: newWorkLog.note,
      };
      
      if (workLogType === 'pitch') {
        payload.pitchId = parseInt(selectedPitchId, 10);
      } else {
        payload.taskId = parseInt(selectedTaskId, 10);
      }
      
      await workLogService.createMy(payload);
      
      setNewWorkLog({
        date: dayjs().format('YYYY-MM-DD'),
        hoursSpent: 0,
        note: '',
      });
      setWorkLogDate(dayjs().format('YYYY-MM-DD'));
      setSelectedPitchId('');
      setSelectedTaskId('');
      showSuccess('Work log added successfully');
      if (selectedCycle === 'all') {
        loadAllWorkLogs();
      } else if (selectedCycle) {
        loadWorkLogs(parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to add work log';
      showError(message);
    }
  };

  const handleDeleteWorkLog = async (id: number) => {
    try {
      await workLogService.deleteMy(id);
      showSuccess('Work log deleted');
      if (selectedCycle === 'all') {
        loadAllWorkLogs();
      } else if (selectedCycle) {
        loadWorkLogs(parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to delete work log';
      showError(message);
    }
  };

  const handleStartTimer = async () => {
    if (workLogType === 'pitch' && !selectedPitchId) {
      showError('Please select a pitch');
      return;
    }
    if (workLogType === 'task' && !selectedTaskId) {
      showError('Please select a task');
      return;
    }

    try {
      setTimerLoading(true);
      await timerService.startTimer({
        pitchId: workLogType === 'pitch' ? parseInt(selectedPitchId, 10) : undefined,
        taskId: workLogType === 'task' ? parseInt(selectedTaskId, 10) : undefined,
        note: newWorkLog.note || undefined,
      });
      showSuccess('Timer started');
      // Clear the note after starting timer
      setNewWorkLog({ ...newWorkLog, note: '' });
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to start timer';
      showError(message);
    } finally {
      setTimerLoading(false);
    }
  };

  const handleTimerStopped = () => {
    // Reload work logs when timer is stopped
    if (selectedCycle === 'all') {
      loadAllWorkLogs();
    } else if (selectedCycle) {
      loadWorkLogs(parseInt(selectedCycle, 10));
    }
    showSuccess('Timer stopped and work log created');
  };

  const handleEditClick = (workLog: WorkLog) => {
    setEditingWorkLog(workLog);
    
    const isTask = !!workLog.taskId;
    setEditWorkLogType(isTask ? 'task' : 'pitch');
    
    setEditForm({
      pitchId: workLog.pitchId,
      taskId: workLog.taskId,
      date: workLog.date,
      hoursSpent: workLog.hoursSpent,
      note: workLog.note || '',
    });
    setEditDate(workLog.date);
    setEditPitchId(workLog.pitchId?.toString() || '');
    setEditTaskId(workLog.taskId?.toString() || '');
    setEditDialogOpen(true);
  };

  const handleEditSave = async () => {
    if (!editingWorkLog || !editDate) return;
    if (editWorkLogType === 'pitch' && !editPitchId) return;
    if (editWorkLogType === 'task' && !editTaskId) return;
    
    try {
      const payload: CreateWorkLogForSelfRequest = {
        date: editDate,
        hoursSpent: editForm.hoursSpent,
        note: editForm.note,
      };
      
      if (editWorkLogType === 'pitch') {
        payload.pitchId = parseInt(editPitchId, 10);
      } else {
        payload.taskId = parseInt(editTaskId, 10);
      }
      
      await workLogService.updateMy(editingWorkLog.id, payload);
      showSuccess('Work log updated successfully');
      setEditDialogOpen(false);
      setEditingWorkLog(null);
      if (selectedCycle === 'all') {
        loadAllWorkLogs();
      } else if (selectedCycle) {
        loadWorkLogs(parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to update work log';
      showError(message);
    }
  };

  if (!user?.personId) {
    return (
      <div>
        <h1 className="text-3xl font-bold mb-4">My Work Logs</h1>
        <Alert variant="warning">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            Your account is not linked to a person profile. Please contact an administrator to link your account.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  const totalHours = workLogs.reduce((sum, wl) => sum + wl.hoursSpent, 0);
  const todayLogs = workLogs.filter(wl => wl.date === dayjs().format('YYYY-MM-DD'));
  const todayHours = todayLogs.reduce((sum, wl) => sum + wl.hoursSpent, 0);

  return (
    <div>
      <h1 className="text-3xl font-bold mb-1">My Work Logs</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Track your daily work on assigned pitches
      </p>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 mb-2">
              <CalendarDays className="h-5 w-5 text-primary" />
              <span className="text-sm text-muted-foreground">Today</span>
            </div>
            <div className="text-3xl font-bold">{todayHours.toFixed(1)}h</div>
            <p className="text-xs text-muted-foreground">
              {todayLogs.length} log{todayLogs.length !== 1 ? 's' : ''} today
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 mb-2">
              <Clock className="h-5 w-5 text-secondary-foreground" />
              <span className="text-sm text-muted-foreground">This Cycle</span>
            </div>
            <div className="text-3xl font-bold">{totalHours.toFixed(1)}h</div>
            <p className="text-xs text-muted-foreground">
              {workLogs.length} total log{workLogs.length !== 1 ? 's' : ''}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Cycle Filter */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex flex-col gap-2">
            <Label htmlFor="cycle-select">Cycle</Label>
            <Select value={selectedCycle} onValueChange={(v) => { setSelectedCycle(v); setPage(0); }}>
              <SelectTrigger className="w-[300px]" id="cycle-select">
                <SelectValue placeholder="Select a cycle" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Cycles</SelectItem>
                {cycles.map((cycle) => (
                  <SelectItem key={cycle.id} value={cycle.id.toString()}>
                    <div className="flex items-center gap-2">
                      {cycle.projectKey && (
                        <Badge variant="secondary" className="text-xs">
                          {cycle.projectKey}
                        </Badge>
                      )}
                      <span>{cycle.name}</span>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Quick Add Work Log Form */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Log Your Work</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {/* Work Log Type Toggle */}
            <div className="flex items-center gap-4">
              <Label>Log time on:</Label>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant={workLogType === 'pitch' ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => {
                    setWorkLogType('pitch');
                    setSelectedTaskId('');
                  }}
                >
                  Pitch
                </Button>
                <Button
                  type="button"
                  variant={workLogType === 'task' ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => {
                    setWorkLogType('task');
                    setSelectedPitchId('');
                  }}
                >
                  Task
                </Button>
              </div>
            </div>
            
            {/* Form Fields */}
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-12 gap-4 items-end">
              {workLogType === 'pitch' ? (
                <div className="md:col-span-4 space-y-2">
                  <Label htmlFor="pitch-select">Pitch *</Label>
                  <Select value={selectedPitchId} onValueChange={setSelectedPitchId}>
                    <SelectTrigger id="pitch-select">
                      <SelectValue placeholder="Select a pitch" />
                    </SelectTrigger>
                    <SelectContent>
                      {pitches.map((p) => (
                        <SelectItem key={p.id} value={p.id.toString()}>
                          {p.title}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              ) : (
                <div className="md:col-span-4 space-y-2">
                  <Label htmlFor="task-select">Task *</Label>
                  <Select value={selectedTaskId} onValueChange={setSelectedTaskId}>
                    <SelectTrigger id="task-select">
                      <SelectValue placeholder="Select a task" />
                    </SelectTrigger>
                    <SelectContent>
                      {tasks.map((t) => (
                        <SelectItem key={t.id} value={t.id.toString()}>
                          {t.title}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              )}
              <div className="md:col-span-2 space-y-2">
                <Label htmlFor="date-input">Date *</Label>
                <LocalizedDateInput
                  id="date-input"
                  value={workLogDate}
                  onChange={(value) => setWorkLogDate(value)}
                />
              </div>
              <div className="md:col-span-2 space-y-2">
                <Label htmlFor="hours-input">Hours *</Label>
                <Input
                  id="hours-input"
                  type="number"
                  value={newWorkLog.hoursSpent || ''}
                  onChange={(e) => setNewWorkLog({ ...newWorkLog, hoursSpent: parseFloat(e.target.value) || 0 })}
                  min={0.25}
                  step={0.25}
                  placeholder="0.0"
                />
              </div>
              <div className="md:col-span-3 space-y-2">
                <Label htmlFor="note-input">Note (optional)</Label>
                <Input
                  id="note-input"
                  value={newWorkLog.note}
                  onChange={(e) => setNewWorkLog({ ...newWorkLog, note: e.target.value })}
                  placeholder="What did you work on?"
                />
              </div>
              <div className="md:col-span-1 flex flex-col gap-2">
                <Button
                  onClick={handleStartTimer}
                  disabled={
                    timerLoading ||
                    (workLogType === 'pitch' && !selectedPitchId) ||
                    (workLogType === 'task' && !selectedTaskId)
                  }
                  variant="default"
                  size="sm"
                  className="w-full bg-green-600 hover:bg-green-700"
                  title="Start a timer - stops automatically when done"
                >
                  <PlayCircle className="h-4 w-4 mr-1" />
                  Start Timer
                </Button>
                <Button
                  onClick={handleCreateWorkLog}
                  disabled={
                    !newWorkLog.hoursSpent ||
                    (workLogType === 'pitch' && !selectedPitchId) ||
                    (workLogType === 'task' && !selectedTaskId)
                  }
                  size="sm"
                  variant="outline"
                  className="w-full"
                >
                  <Plus className="h-4 w-4 mr-1" />
                  Log Hours
                </Button>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Work Logs Table */}
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle>My Recent Work Logs</CardTitle>
            <span className="text-sm text-muted-foreground">
              Total: <strong>{totalHours.toFixed(1)} hours</strong>
            </span>
          </div>
        </CardHeader>
        <CardContent>
          {workLogs.length === 0 ? (
            <EmptyState
              illustration={<EmptyWorkLogsIllustration />}
              title="No work logs yet"
              description="Start tracking your work by adding your first work log entry using the form above"
              size="medium"
            />
          ) : (
            <div className="border rounded-md">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Pitch/Task</TableHead>
                    <TableHead className="text-right">Hours</TableHead>
                    <TableHead>Note</TableHead>
                    <TableHead className="text-center">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {workLogs.map((wl) => (
                    <TableRow
                      key={wl.id}
                      className={cn(
                        wl.date === dayjs().format('YYYY-MM-DD') && 'bg-muted/50'
                      )}
                    >
                      <TableCell>
                        <div className="flex items-center gap-2">
                          {formatLocalizedDate(wl.date, i18n.language)}
                          {wl.date === dayjs().format('YYYY-MM-DD') && (
                            <Badge variant="default" className="text-xs">Today</Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex flex-col gap-1">
                          <span>{wl.pitchTitle || wl.taskTitle}</span>
                          {wl.taskTitle && (
                            <Badge variant="secondary" className="text-xs w-fit">Task</Badge>
                          )}
                          {wl.pitchTitle && (
                            <Badge variant="outline" className="text-xs w-fit">Pitch</Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="text-right">{wl.hoursSpent}h</TableCell>
                      <TableCell>{wl.note || '-'}</TableCell>
                      <TableCell>
                        <div className="flex items-center justify-center gap-1">
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            onClick={() => handleEditClick(wl)}
                            aria-label={`Edit work log for ${wl.pitchTitle || wl.taskTitle}`}
                          >
                            <Pencil className="h-4 w-4" aria-hidden="true" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            onClick={() => handleDeleteWorkLog(wl.id)}
                            className="text-destructive hover:text-destructive"
                            aria-label={`Delete work log for ${wl.pitchTitle || wl.taskTitle}`}
                          >
                            <Trash2 className="h-4 w-4" aria-hidden="true" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <div className="text-sm text-muted-foreground">
                {t('meetingList.pagination.showing', { from: page * PAGE_SIZE + 1, to: Math.min((page + 1) * PAGE_SIZE, totalElements), total: totalElements })}
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                >
                  {t('meetingList.pagination.previous', { defaultValue: 'Previous' })}
                </Button>
                <div className="text-sm text-muted-foreground">
                  {t('meetingList.pagination.page', { current: page + 1, total: totalPages })}
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                >
                  {t('meetingList.pagination.next', { defaultValue: 'Next' })}
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Edit Work Log</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            {/* Work Log Type Toggle in Edit */}
            <div className="flex items-center gap-4">
              <Label>Log time on:</Label>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant={editWorkLogType === 'pitch' ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => {
                    setEditWorkLogType('pitch');
                    setEditTaskId('');
                  }}
                >
                  Pitch
                </Button>
                <Button
                  type="button"
                  variant={editWorkLogType === 'task' ? 'default' : 'outline'}
                  size="sm"
                  onClick={() => {
                    setEditWorkLogType('task');
                    setEditPitchId('');
                  }}
                >
                  Task
                </Button>
              </div>
            </div>
            
            {editWorkLogType === 'pitch' ? (
              <div className="space-y-2">
                <Label htmlFor="edit-pitch">Pitch *</Label>
                <Select value={editPitchId} onValueChange={setEditPitchId}>
                  <SelectTrigger id="edit-pitch">
                    <SelectValue placeholder="Select a pitch" />
                  </SelectTrigger>
                  <SelectContent>
                    {(pitches.length > 0 ? pitches : allPitches).map((p) => (
                      <SelectItem key={p.id} value={p.id.toString()}>
                        {p.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            ) : (
              <div className="space-y-2">
                <Label htmlFor="edit-task">Task *</Label>
                <Select value={editTaskId} onValueChange={setEditTaskId}>
                  <SelectTrigger id="edit-task">
                    <SelectValue placeholder="Select a task" />
                  </SelectTrigger>
                  <SelectContent>
                    {tasks.map((t) => (
                      <SelectItem key={t.id} value={t.id.toString()}>
                        {t.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="edit-date">Date *</Label>
                <LocalizedDateInput
                  id="edit-date"
                  value={editDate}
                  onChange={(value) => setEditDate(value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-hours">Hours *</Label>
                <Input
                  id="edit-hours"
                  type="number"
                  value={editForm.hoursSpent || ''}
                  onChange={(e) => setEditForm({ ...editForm, hoursSpent: parseFloat(e.target.value) || 0 })}
                  min={0.25}
                  step={0.25}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-note">Note</Label>
              <Textarea
                id="edit-note"
                value={editForm.note}
                onChange={(e) => setEditForm({ ...editForm, note: e.target.value })}
                rows={2}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleEditSave}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Timer Widget (floating widget) */}
      <TimerWidget onTimerStopped={handleTimerStopped} />
    </div>
  );
}
