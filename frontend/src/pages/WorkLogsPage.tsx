import { useEffect, useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2, Pencil, Clock, CalendarDays, Loader2, AlertTriangle, Users, User } from 'lucide-react';
import dayjs from 'dayjs';
import { workLogService } from '../services/workLogService';
import { pitchService } from '../services/pitchService';
import { cycleService } from '../services/cycleService';
import { personService } from '../services/personService';
import { taskService } from '../services/taskService';
import { WorkLog, Pitch, Cycle, Person, Task, CreateWorkLogRequest, CreateWorkLogForSelfRequest } from '../types';
import { useAuth, useToast, useProject } from '../contexts';
import EmptyState from '../components/EmptyState';
import { EmptyWorkLogsIllustration } from '../components/illustrations';
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
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../components/ui/tabs';
import { Textarea } from '../components/ui/textarea';
import { Alert, AlertDescription } from '../components/ui/alert';

export default function WorkLogsPage() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();
  const { currentProject, isAllProjectsSelected, isKanbanProject } = useProject();
  const [activeTab, setActiveTab] = useState<'my' | 'team'>('my');
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<string>('all');
  const [loading, setLoading] = useState(true);
  const [workLogType, setWorkLogType] = useState<'pitch' | 'task'>('task');

  // Form state for personal logs
  const [newWorkLog, setNewWorkLog] = useState<CreateWorkLogForSelfRequest>({
    pitchId: 0,
    date: dayjs().format('YYYY-MM-DD'),
    hoursSpent: 0,
    note: '',
  });
  const [workLogDate, setWorkLogDate] = useState<string>(dayjs().format('YYYY-MM-DD'));
  const [selectedPitchId, setSelectedPitchId] = useState<string>('');
  const [selectedTaskId, setSelectedTaskId] = useState<string>('');

  // Form state for team logs (admin)
  const [teamWorkLog, setTeamWorkLog] = useState<CreateWorkLogRequest>({
    personId: 0,
    pitchId: 0,
    date: dayjs().format('YYYY-MM-DD'),
    hoursSpent: 0,
    note: '',
  });
  const [teamWorkLogDate, setTeamWorkLogDate] = useState<string>(dayjs().format('YYYY-MM-DD'));

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

  // Filter cycles by current project
  const filteredCycles = useMemo(() => {
    if (isAllProjectsSelected) return cycles;
    return cycles.filter(c => c.projectId === currentProject?.id);
  }, [cycles, currentProject, isAllProjectsSelected]);

  useEffect(() => {
    loadInitialData();
  }, []);

  // Reload data when project changes
  useEffect(() => {
    if (selectedCycle) {
      if (selectedCycle === 'all') {
        loadWorkLogs('all');
        // Load pitches and tasks from all active cycles for the form
        loadAllPitches();
        loadAllTasks();
      } else {
        const cycleId = parseInt(selectedCycle, 10);
        loadWorkLogs(cycleId);
        loadPitches(cycleId);
        loadTasks(cycleId);
      }
    }
  }, [selectedCycle, activeTab, currentProject?.id]);

  const loadInitialData = async () => {
    try {
      const [cyclesRes, personsRes] = await Promise.all([
        cycleService.getActive(),
        personService.getAll(true),
      ]);
      setCycles(cyclesRes.data);
      setPersons(personsRes);
      // Default to 'all' - don't auto-select first cycle
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadWorkLogs = async (cycleIdOrAll: number | string) => {
    try {
      let logs: WorkLog[] = [];
      if (cycleIdOrAll === 'all') {
        // Load all work logs across all cycles
        if (activeTab === 'my') {
          const response = await workLogService.getMy();
          logs = response.data;
        } else {
          const response = await workLogService.getAll();
          logs = response.data;
        }
      } else {
        // Load work logs for specific cycle
        const cycleId = cycleIdOrAll as number;
        if (activeTab === 'my') {
          const response = await workLogService.getMyByCycle(cycleId);
          logs = response.data;
        } else {
          const response = await workLogService.getByCycleId(cycleId);
          logs = response.data;
        }
      }
      
      // Filter by current project if one is selected
      if (!isAllProjectsSelected && currentProject) {
        // Filter logs that belong to tasks/pitches in cycles of the current project
        const projectCycleIds = new Set(cycles.filter(c => c.projectId === currentProject.id).map(c => c.id));
        logs = logs.filter(log => {
          // If the log has a cycleId directly, check it
          // Otherwise filter by pitch/task's cycle
          if (log.pitchId) {
            const pitch = pitches.find(p => p.id === log.pitchId);
            return pitch ? projectCycleIds.has(pitch.cycleId) : false;
          }
          if (log.taskId) {
            const task = tasks.find(t => t.id === log.taskId);
            return task ? projectCycleIds.has(task.cycleId) : false;
          }
          return false;
        });
      }
      
      setWorkLogs(logs);
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

  const loadAllPitches = async () => {
    try {
      const response = await pitchService.getAll();
      let allPitches = response.data;
      // Filter by current project if one is selected
      if (!isAllProjectsSelected && currentProject) {
        const projectCycleIds = new Set(cycles.filter(c => c.projectId === currentProject.id).map(c => c.id));
        allPitches = allPitches.filter(p => projectCycleIds.has(p.cycleId));
      }
      setPitches(allPitches);
    } catch (error) {
      console.error('Failed to load pitches:', error);
    }
  };

  const loadTasks = async (cycleId: number) => {
    try {
      const response = await taskService.getByCycleId(cycleId);
      if (response.data && 'content' in response.data) {
        setTasks((response.data as any).content || []);
      } else if (Array.isArray(response.data)) {
        setTasks(response.data);
      }
    } catch (error) {
      console.error('Failed to load tasks:', error);
    }
  };

  const loadAllTasks = async () => {
    try {
      // For a specific project, load by project ID if available
      if (!isAllProjectsSelected && currentProject) {
        const response = await taskService.getByProjectIdPaged(currentProject.id, 0, 1000);
        setTasks(response.data.content || []);
      } else {
        const response = await taskService.getAll(0, 1000);
        setTasks(response.data.content || []);
      }
    } catch (error) {
      console.error('Failed to load tasks:', error);
    }
  };

  // My work log handlers
  const handleCreateMyWorkLog = async () => {
    if (!workLogDate || !newWorkLog.hoursSpent) return;
    if (workLogType === 'pitch' && !selectedPitchId) return;
    if (workLogType === 'task' && !selectedTaskId) return;
    
    try {
      const requestData = {
        ...newWorkLog,
        pitchId: workLogType === 'pitch' ? parseInt(selectedPitchId, 10) : undefined,
        taskId: workLogType === 'task' ? parseInt(selectedTaskId, 10) : undefined,
        date: workLogDate,
      };
      
      await workLogService.createMy(requestData);
      setNewWorkLog({
        pitchId: 0,
        date: dayjs().format('YYYY-MM-DD'),
        hoursSpent: 0,
        note: '',
      });
      setWorkLogDate(dayjs().format('YYYY-MM-DD'));
      setSelectedPitchId('');
      showSuccess(t('workLogs.addSuccess'));
      if (selectedCycle) {
        loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || error.response?.data?.error || t('workLogs.errors.addFailed');
      showError(message);
    }
  };

  const handleDeleteMyWorkLog = async (id: number) => {
    try {
      await workLogService.deleteMy(id);
      showSuccess(t('workLogs.deleteSuccess'));
      if (selectedCycle) {
        loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || t('workLogs.errors.deleteFailed');
      showError(message);
    }
  };

  // Team work log handlers (admin)
  const handleCreateTeamWorkLog = async () => {
    if (!teamWorkLogDate || !teamWorkLog.personId || !teamWorkLog.hoursSpent) return;
    if (!teamWorkLog.pitchId && !teamWorkLog.taskId) return;
    try {
      await workLogService.create({
        ...teamWorkLog,
        date: teamWorkLogDate,
      });
      setTeamWorkLog({
        personId: 0,
        pitchId: 0,
        date: dayjs().format('YYYY-MM-DD'),
        hoursSpent: 0,
        note: '',
      });
      setTeamWorkLogDate(dayjs().format('YYYY-MM-DD'));
      showSuccess(t('workLogs.addSuccess'));
      if (selectedCycle) {
        loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || t('workLogs.errors.addFailed');
      showError(message);
    }
  };

  const handleDeleteTeamWorkLog = async (id: number) => {
    try {
      await workLogService.delete(id);
      showSuccess(t('workLogs.deleteSuccess'));
      if (selectedCycle) {
        loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || t('workLogs.errors.deleteFailed');
      showError(message);
    }
  };

  const handleEditClick = (workLog: WorkLog) => {
    setEditingWorkLog(workLog);
    const hasTask = !!workLog.taskId;
    setEditWorkLogType(hasTask ? 'task' : 'pitch');
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
      const requestData = {
        ...editForm,
        pitchId: editWorkLogType === 'pitch' ? parseInt(editPitchId, 10) : undefined,
        taskId: editWorkLogType === 'task' ? parseInt(editTaskId, 10) : undefined,
        date: editDate,
      };
      
      if (activeTab === 'my') {
        await workLogService.updateMy(editingWorkLog.id, requestData);
      } else {
        await workLogService.update(editingWorkLog.id, {
          personId: editingWorkLog.personId,
          ...requestData,
        });
      }
      showSuccess(t('workLogs.updateSuccess'));
      setEditDialogOpen(false);
      setEditingWorkLog(null);
      if (selectedCycle) {
        loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || t('workLogs.errors.updateFailed');
      showError(message);
    }
  };

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'PROJECT_MANAGER';
  const canLogForSelf = !!user?.personId;

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
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Work Logs</h1>
          <p className="text-muted-foreground">Track time spent on pitches</p>
        </div>
        <div className="flex items-center gap-2">
          {/* Hide cycle selector for Kanban projects - they only have the hidden "Continuous Flow" cycle */}
          {!isKanbanProject && (
            <Select
              value={selectedCycle}
              onValueChange={setSelectedCycle}
            >
              <SelectTrigger className="w-[200px]">
                <SelectValue placeholder={t('workLogsPage.selectCycle')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('workLogsPage.allCycles')}</SelectItem>
                {filteredCycles.map((cycle) => (
                  <SelectItem key={cycle.id} value={cycle.id.toString()}>
                    {cycle.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          )}
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 mb-2">
              <CalendarDays className="h-5 w-5 text-primary" />
              <span className="text-sm text-muted-foreground">{t('workLogs.today')}</span>
            </div>
            <div className="text-3xl font-bold">{todayHours.toFixed(1)}h</div>
            <p className="text-xs text-muted-foreground">
              {t('workLogs.logsToday', { count: todayLogs.length })}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 mb-2">
              <Clock className="h-5 w-5 text-secondary-foreground" />
              <span className="text-sm text-muted-foreground">{t('workLogs.thisCycle')}</span>
            </div>
            <div className="text-3xl font-bold">{totalHours.toFixed(1)}h</div>
            <p className="text-xs text-muted-foreground">
              {t('workLogs.logsCount', { count: workLogs.length })}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as 'my' | 'team')} className="w-full">
        <TabsList>
          <TabsTrigger value="my" className="flex items-center gap-2">
            <User className="h-4 w-4" />
            {t('workLogs.myLogs')}
          </TabsTrigger>
          {isAdmin && (
            <TabsTrigger value="team" className="flex items-center gap-2">
              <Users className="h-4 w-4" />
              {t('workLogs.teamLogs')}
            </TabsTrigger>
          )}
        </TabsList>

        {/* My Logs Tab */}
        <TabsContent value="my" className="space-y-4">
          {!canLogForSelf ? (
            <Alert variant="default" className="border-amber-500 bg-amber-50 dark:bg-amber-950">
              <AlertTriangle className="h-4 w-4 text-amber-500" />
              <AlertDescription>
                {t('workLogs.notLinkedAlert')}
              </AlertDescription>
            </Alert>
          ) : (
            <>
              {/* Quick Log Form */}
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">{t('workLogs.quickLog')}</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-6 gap-4 items-end">
                    <div className="space-y-2">
                      <Label htmlFor="log-type">{t('workLogs.logType')} *</Label>
                      <Select
                        value={workLogType}
                        onValueChange={(value: 'pitch' | 'task') => {
                          setWorkLogType(value);
                          setSelectedPitchId('');
                          setSelectedTaskId('');
                        }}
                      >
                        <SelectTrigger id="log-type">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="task">{t('workLogs.taskSubtask')}</SelectItem>
                          <SelectItem value="pitch">{t('workLogs.pitch')}</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    {workLogType === 'pitch' ? (
                      <div className="space-y-2">
                        <Label htmlFor="my-pitch">{t('workLogs.pitch')} *</Label>
                        <Select
                          value={selectedPitchId}
                          onValueChange={setSelectedPitchId}
                        >
                          <SelectTrigger id="my-pitch">
                            <SelectValue placeholder={t('workLogs.selectPitch')} />
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
                      <div className="space-y-2">
                        <Label htmlFor="my-task">{t('workLogs.taskSubtask')} *</Label>
                        <Select
                          value={selectedTaskId}
                          onValueChange={setSelectedTaskId}
                        >
                          <SelectTrigger id="my-task">
                            <SelectValue placeholder={t('workLogs.selectTask')} />
                          </SelectTrigger>
                          <SelectContent>
                            {tasks.map((t) => (
                              <SelectItem key={t.id} value={t.id.toString()}>
                                {t.parentTaskId && '└─ '}{t.title}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    )}
                    <div className="space-y-2">
                      <Label htmlFor="my-date">{t('workLogs.date')}</Label>
                      <LocalizedDateInput
                        id="my-date"
                        value={workLogDate}
                        onChange={(value) => setWorkLogDate(value)}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="my-hours">{t('workLogs.hours')} *</Label>
                      <Input
                        id="my-hours"
                        type="number"
                        min={0.25}
                        step={0.25}
                        value={newWorkLog.hoursSpent || ''}
                        onChange={(e) => setNewWorkLog({ ...newWorkLog, hoursSpent: parseFloat(e.target.value) || 0 })}
                        placeholder="0"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="my-note">{t('workLogs.note')}</Label>
                      <Input
                        id="my-note"
                        value={newWorkLog.note}
                        onChange={(e) => setNewWorkLog({ ...newWorkLog, note: e.target.value })}
                        placeholder={t('workLogs.optionalNote')}
                      />
                    </div>
                    <Button
                      onClick={handleCreateMyWorkLog}
                      disabled={(workLogType === 'pitch' && !selectedPitchId) || (workLogType === 'task' && !selectedTaskId) || !newWorkLog.hoursSpent}
                    >
                      <Plus className="h-4 w-4 mr-2" />
                      {t('workLogs.logTime')}
                    </Button>
                  </div>
                </CardContent>
              </Card>

              {/* Work Logs Table */}
              <WorkLogsTable
                workLogs={workLogs}
                showPerson={false}
                onEdit={handleEditClick}
                onDelete={handleDeleteMyWorkLog}
              />
            </>
          )}
        </TabsContent>

        {/* Team Logs Tab (Admin) */}
        {isAdmin && (
          <TabsContent value="team" className="space-y-4">
            {/* Add for Team Form */}
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">{t('workLogs.logTimeForTeamMember')}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-6 gap-4 items-end">
                  <div className="space-y-2">
                    <Label htmlFor="team-person">{t('workLogs.person')} *</Label>
                    <Select
                      value={teamWorkLog.personId ? teamWorkLog.personId.toString() : ''}
                      onValueChange={(value) => setTeamWorkLog({ ...teamWorkLog, personId: Number(value) })}
                    >
                      <SelectTrigger id="team-person">
                        <SelectValue placeholder={t('workLogs.selectPerson')} />
                      </SelectTrigger>
                      <SelectContent>
                        {persons.map((p) => (
                          <SelectItem key={p.id} value={p.id.toString()}>
                            {p.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="team-pitch">{t('workLogs.pitch')} *</Label>
                    <Select
                      value={teamWorkLog.pitchId ? teamWorkLog.pitchId.toString() : ''}
                      onValueChange={(value) => setTeamWorkLog({ ...teamWorkLog, pitchId: Number(value) })}
                    >
                      <SelectTrigger id="team-pitch">
                        <SelectValue placeholder={t('workLogs.selectPitch')} />
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
                  <div className="space-y-2">
                    <Label htmlFor="team-date">Date</Label>
                    <LocalizedDateInput
                      id="team-date"
                      value={teamWorkLogDate}
                      onChange={(value) => setTeamWorkLogDate(value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="team-hours">Hours *</Label>
                    <Input
                      id="team-hours"
                      type="number"
                      min={0.25}
                      step={0.25}
                      value={teamWorkLog.hoursSpent || ''}
                      onChange={(e) => setTeamWorkLog({ ...teamWorkLog, hoursSpent: parseFloat(e.target.value) || 0 })}
                      placeholder="0"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="team-note">Note</Label>
                    <Input
                      id="team-note"
                      value={teamWorkLog.note}
                      onChange={(e) => setTeamWorkLog({ ...teamWorkLog, note: e.target.value })}
                      placeholder={t('workLogsPage.optionalNote')}
                    />
                  </div>
                  <Button
                    onClick={handleCreateTeamWorkLog}
                    disabled={!teamWorkLog.personId || (!teamWorkLog.pitchId && !teamWorkLog.taskId) || !teamWorkLog.hoursSpent}
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    Log Time
                  </Button>
                </div>
              </CardContent>
            </Card>

            {/* Work Logs Table */}
            <WorkLogsTable
              workLogs={workLogs}
              showPerson={true}
              onEdit={handleEditClick}
              onDelete={handleDeleteTeamWorkLog}
            />
          </TabsContent>
        )}
      </Tabs>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('workLogs.editWorkLog')}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="edit-log-type">{t('workLogs.logType')}</Label>
              <Select
                value={editWorkLogType}
                onValueChange={(value: 'pitch' | 'task') => {
                  setEditWorkLogType(value);
                  setEditPitchId('');
                  setEditTaskId('');
                }}
              >
                <SelectTrigger id="edit-log-type">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="task">Task/Subtask</SelectItem>
                  <SelectItem value="pitch">Pitch</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {editWorkLogType === 'pitch' ? (
              <div className="grid gap-2">
                <Label htmlFor="edit-pitch">{t('workLogs.pitch')}</Label>
                <Select
                  value={editPitchId}
                  onValueChange={setEditPitchId}
                >
                  <SelectTrigger id="edit-pitch">
                    <SelectValue />
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
              <div className="grid gap-2">
                <Label htmlFor="edit-task">{t('workLogs.taskSubtask')}</Label>
                <Select
                  value={editTaskId}
                  onValueChange={setEditTaskId}
                >
                  <SelectTrigger id="edit-task">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {tasks.map((t) => (
                      <SelectItem key={t.id} value={t.id.toString()}>
                        {t.parentTaskId && '└─ '}{t.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
            <div className="grid gap-2">
              <Label htmlFor="edit-date">{t('workLogs.date')}</Label>
              <LocalizedDateInput
                id="edit-date"
                value={editDate}
                onChange={(value) => setEditDate(value)}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="edit-hours">{t('workLogs.hours')}</Label>
              <Input
                id="edit-hours"
                type="number"
                min={0.25}
                step={0.25}
                value={editForm.hoursSpent}
                onChange={(e) => setEditForm({ ...editForm, hoursSpent: parseFloat(e.target.value) || 0 })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="edit-note">{t('workLogs.note')}</Label>
              <Textarea
                id="edit-note"
                value={editForm.note}
                onChange={(e) => setEditForm({ ...editForm, note: e.target.value })}
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditDialogOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleEditSave}>{t('workLogs.saveChanges')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

interface WorkLogsTableProps {
  workLogs: WorkLog[];
  showPerson: boolean;
  onEdit: (workLog: WorkLog) => void;
  onDelete: (id: number) => void;
}

function WorkLogsTable({ workLogs, showPerson, onEdit, onDelete }: WorkLogsTableProps) {
  const { t, i18n } = useTranslation();
  
  if (workLogs.length === 0) {
    return (
      <EmptyState
        title={t('workLogs.empty.title')}
        description={t('workLogs.empty.description')}
        illustration={<EmptyWorkLogsIllustration />}
      />
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">{t('workLogs.workLogsTable')}</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('workLogs.date')}</TableHead>
              {showPerson && <TableHead>Person</TableHead>}
              <TableHead>Pitch/Task</TableHead>
              <TableHead className="text-right">Hours</TableHead>
              <TableHead>Note</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {workLogs.map((wl) => (
              <TableRow key={wl.id}>
                <TableCell>{formatLocalizedDate(wl.date, i18n.language)}</TableCell>
                {showPerson && <TableCell>{wl.personName}</TableCell>}
                <TableCell>
                  {wl.pitchTitle && (
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs">{t('workLogs.pitch')}</Badge>
                      <span>{wl.pitchTitle}</span>
                    </div>
                  )}
                  {wl.taskTitle && (
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs">{t('workLogs.task')}</Badge>
                      <span>{wl.taskTitle}</span>
                    </div>
                  )}
                </TableCell>
                <TableCell className="text-right">
                  <Badge variant="secondary">{wl.hoursSpent}h</Badge>
                </TableCell>
                <TableCell className="max-w-[200px] truncate">{wl.note || '-'}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => onEdit(wl)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => onDelete(wl.id)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
