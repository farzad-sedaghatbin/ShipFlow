import { useEffect, useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2, Pencil, Clock, CalendarDays, AlertTriangle, Users, User } from 'lucide-react';
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
import { WorkLogsSkeleton } from '../components/Skeletons';

import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { usePermission } from '../hooks/usePermission';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Combobox } from '../components/ui/combobox';
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
  const { currentProject, isAllProjectsSelected, isKanbanProject, isSwitchingProject, notifyProjectSwitchComplete } = useProject();
  const [activeTab, setActiveTab] = useState<'my' | 'team'>('my');
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const PAGE_SIZE = 20;
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<string>('all');
  const [loading, setLoading] = useState(true);
  const [workLogType, setWorkLogType] = useState<'pitch' | 'task'>('task');

  // Summary stats
  const [summaryTodayHours, setSummaryTodayHours] = useState(0);
  const [summaryTodayCount, setSummaryTodayCount] = useState(0);
  const [summaryTotalHours, setSummaryTotalHours] = useState(0);
  const [summaryTotalCount, setSummaryTotalCount] = useState(0);

  // Log dialog state
  const [quickLogDialogOpen, setQuickLogDialogOpen] = useState(false);
  const [teamLogDialogOpen, setTeamLogDialogOpen] = useState(false);

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
  const [teamWorkLogType, setTeamWorkLogType] = useState<'pitch' | 'task'>('task');
  const [teamSelectedPitchId, setTeamSelectedPitchId] = useState<string>('');
  const [teamSelectedTaskId, setTeamSelectedTaskId] = useState<string>('');

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

  // Table filter state
  const [filterPersonId, setFilterPersonId] = useState<string>('all');
  const [filterPitchId, setFilterPitchId] = useState<string>('all');
  const [filterTaskId, setFilterTaskId] = useState<string>('all');
  const [filterDateFrom, setFilterDateFrom] = useState<string>('');
  const [filterDateTo, setFilterDateTo] = useState<string>('');

  const filteredWorkLogs = useMemo(() => {
    return workLogs.filter(wl => {
      if (filterPitchId !== 'all' && wl.pitchId !== parseInt(filterPitchId, 10)) return false;
      if (filterTaskId !== 'all' && wl.taskId !== parseInt(filterTaskId, 10)) return false;
      return true;
    });
  }, [workLogs, filterPitchId, filterTaskId]);

  const filteredCycles = useMemo(() => {
    if (isAllProjectsSelected) return cycles;
    return cycles.filter(c => c.projectId === currentProject?.id);
  }, [cycles, currentProject, isAllProjectsSelected]);

  useEffect(() => {
    setSelectedCycle('all');
    setPage(0);
    setFilterPersonId('all');
    setFilterPitchId('all');
    setFilterTaskId('all');
    setFilterDateFrom('');
    setFilterDateTo('');
  }, [currentProject?.id, isAllProjectsSelected]);

  useEffect(() => { loadInitialData(); }, []);

  useEffect(() => {
    if (selectedCycle) {
      if (selectedCycle === 'all') { loadAllPitches(); loadAllTasks(); }
      else { const cycleId = parseInt(selectedCycle, 10); loadPitches(cycleId); loadTasks(cycleId); }
    }
  }, [selectedCycle, currentProject?.id]);

  useEffect(() => {
    if (selectedCycle) {
      loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
    }
  }, [page, selectedCycle, activeTab, currentProject?.id, filterDateFrom, filterDateTo, filterPersonId]);

  useEffect(() => {
    if (activeTab === 'my') {
      loadSummaryStats();
    } else {
      setSummaryTodayHours(0); setSummaryTodayCount(0);
      setSummaryTotalHours(0); setSummaryTotalCount(0);
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedCycle, activeTab, currentProject?.id, isAllProjectsSelected]);

  const loadSummaryStats = async () => {
    try {
      const cycleId = selectedCycle !== 'all' ? parseInt(selectedCycle, 10) : undefined;
      const projectId = !isAllProjectsSelected && currentProject ? currentProject.id : undefined;
      const response = await workLogService.getMySummary(cycleId, projectId);
      setSummaryTodayHours(response.data.todayHours);
      setSummaryTodayCount(response.data.todayCount);
      setSummaryTotalHours(response.data.totalHours);
      setSummaryTotalCount(response.data.totalCount);
    } catch (error) { console.error('Failed to load work log summary:', error); }
  };

  const loadInitialData = async () => {
    try {
      const [cyclesRes, personsRes] = await Promise.all([
        cycleService.getMyActiveCycles(),
        personService.getAll(true),
      ]);
      setCycles(cyclesRes.data);
      const currentPersonId = user?.personId;
      if (currentPersonId) {
        personsRes.sort((a: Person, b: Person) => (a.id === currentPersonId ? -1 : b.id === currentPersonId ? 1 : 0));
      }
      setPersons(personsRes);
    } catch (error) { console.error('Failed to load data:', error); }
    finally { setLoading(false); }
  };

  const loadWorkLogs = async (cycleIdOrAll: number | string) => {
    const from = filterDateFrom || undefined;
    const to = filterDateTo || undefined;
    const dateFrom = from && to ? from : undefined;
    const dateTo = from && to ? to : undefined;
    const teamPersonId = activeTab === 'team' && filterPersonId !== 'all' ? parseInt(filterPersonId, 10) : undefined;

    try {
      let logs: WorkLog[] = [];
      let serverTotalPages = 0;
      let serverTotalElements = 0;
      if (cycleIdOrAll === 'all') {
        const projectId = !isAllProjectsSelected && currentProject ? currentProject.id : undefined;
        if (activeTab === 'my') {
          const response = await workLogService.getMy(page, PAGE_SIZE, projectId, dateFrom, dateTo);
          logs = response.data.content; serverTotalPages = response.data.page?.totalPages ?? response.data.totalPages ?? 0; serverTotalElements = response.data.page?.totalElements ?? response.data.totalElements ?? 0;
        } else {
          const response = await workLogService.getAll(page, PAGE_SIZE, projectId, dateFrom, dateTo, teamPersonId);
          logs = response.data.content; serverTotalPages = response.data.page?.totalPages ?? response.data.totalPages ?? 0; serverTotalElements = response.data.page?.totalElements ?? response.data.totalElements ?? 0;
        }
      } else {
        const cycleId = cycleIdOrAll as number;
        if (activeTab === 'my') {
          const response = await workLogService.getMyByCycle(cycleId, page, PAGE_SIZE, dateFrom, dateTo);
          logs = response.data.content; serverTotalPages = response.data.page?.totalPages ?? response.data.totalPages ?? 0; serverTotalElements = response.data.page?.totalElements ?? response.data.totalElements ?? 0;
        } else {
          const response = await workLogService.getByCycleId(cycleId, page, PAGE_SIZE, dateFrom, dateTo, teamPersonId);
          logs = response.data.content; serverTotalPages = response.data.page?.totalPages ?? response.data.totalPages ?? 0; serverTotalElements = response.data.page?.totalElements ?? response.data.totalElements ?? 0;
        }
      }
      setWorkLogs(logs); setTotalPages(serverTotalPages); setTotalElements(serverTotalElements);
    } catch (error) { console.error('Failed to load work logs:', error); }
    finally { notifyProjectSwitchComplete(); }
  };

  const loadPitches = async (cycleId: number) => {
    try { const response = await pitchService.getByCycleId(cycleId); setPitches(response.data); }
    catch (error) { console.error('Failed to load pitches:', error); }
  };

  const loadAllPitches = async () => {
    try {
      let allCycles: Cycle[];
      if (isAllProjectsSelected) { const res = await cycleService.getMyCycles(); allCycles = res.data; }
      else if (currentProject) { const res = await cycleService.getByProject(currentProject.id); allCycles = res.data; }
      else { const res = await cycleService.getMyCycles(); allCycles = res.data; }
      if (allCycles.length === 0) { setPitches([]); return; }
      const results = await Promise.all(allCycles.map(cycle => pitchService.getByCycleId(cycle.id)));
      const merged = results.flatMap(r => r.data);
      const unique = Array.from(new Map(merged.map(p => [p.id, p])).values());
      setPitches(unique);
    } catch (error) { console.error('Failed to load pitches:', error); }
  };

  const loadTasks = async (cycleId: number) => {
    try {
      const response = await taskService.getByCycleId(cycleId);
      if (response.data && 'content' in response.data) { setTasks((response.data as any).content || []); }
      else if (Array.isArray(response.data)) { setTasks(response.data); }
    } catch (error) { console.error('Failed to load tasks:', error); }
  };

  const loadAllTasks = async () => {
    try {
      if (!isAllProjectsSelected && currentProject) {
        const response = await taskService.getByProjectIdPaged(currentProject.id, 0, 1000);
        setTasks(response.data.content || []);
      } else {
        // When all projects are selected and no cycle is chosen, don't load tasks across
        // all projects — the dropdown would be unusable. User must pick a cycle first.
        setTasks([]);
      }
    } catch (error) { console.error('Failed to load tasks:', error); }
  };

  const handleCreateMyWorkLog = async () => {
    if (!workLogDate || !newWorkLog.hoursSpent) return;
    if (workLogType === 'pitch' && !selectedPitchId) return;
    if (workLogType === 'task' && !selectedTaskId) return;
    try {
      await workLogService.createMy({
        ...newWorkLog,
        pitchId: workLogType === 'pitch' ? parseInt(selectedPitchId, 10) : undefined,
        taskId: workLogType === 'task' ? parseInt(selectedTaskId, 10) : undefined,
        date: workLogDate,
      });
      setNewWorkLog({ pitchId: 0, date: dayjs().format('YYYY-MM-DD'), hoursSpent: 0, note: '' });
      setWorkLogDate(dayjs().format('YYYY-MM-DD'));
      setSelectedPitchId(''); setSelectedTaskId('');
      setQuickLogDialogOpen(false);
      showSuccess(t('workLogs.addSuccess'));
      loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
    } catch (error: any) {
      showError(error.response?.data?.message || error.response?.data?.error || t('workLogs.errors.addFailed'));
    }
  };

  const handleDeleteMyWorkLog = async (id: number) => {
    try {
      await workLogService.deleteMy(id);
      showSuccess(t('workLogs.deleteSuccess'));
      loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
    } catch (error: any) {
      showError(error.response?.data?.message || t('workLogs.errors.deleteFailed'));
    }
  };

  const handleCreateTeamWorkLog = async () => {
    if (!teamWorkLogDate || !teamWorkLog.personId || !teamWorkLog.hoursSpent) return;
    if (teamWorkLogType === 'pitch' && !teamSelectedPitchId) return;
    if (teamWorkLogType === 'task' && !teamSelectedTaskId) return;
    try {
      await workLogService.create({
        ...teamWorkLog,
        pitchId: teamWorkLogType === 'pitch' ? parseInt(teamSelectedPitchId, 10) : undefined,
        taskId: teamWorkLogType === 'task' ? parseInt(teamSelectedTaskId, 10) : undefined,
        date: teamWorkLogDate,
      });
      setTeamWorkLog({ personId: 0, pitchId: 0, date: dayjs().format('YYYY-MM-DD'), hoursSpent: 0, note: '' });
      setTeamWorkLogDate(dayjs().format('YYYY-MM-DD'));
      setTeamSelectedPitchId(''); setTeamSelectedTaskId('');
      setTeamLogDialogOpen(false);
      showSuccess(t('workLogs.addSuccess'));
      loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
    } catch (error: any) {
      showError(error.response?.data?.message || t('workLogs.errors.addFailed'));
    }
  };

  const handleDeleteTeamWorkLog = async (id: number) => {
    try {
      await workLogService.delete(id);
      showSuccess(t('workLogs.deleteSuccess'));
      loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
    } catch (error: any) {
      showError(error.response?.data?.message || t('workLogs.errors.deleteFailed'));
    }
  };

  const handleEditClick = (workLog: WorkLog) => {
    setEditingWorkLog(workLog);
    const hasTask = !!workLog.taskId;
    setEditWorkLogType(hasTask ? 'task' : 'pitch');
    setEditForm({ pitchId: workLog.pitchId, taskId: workLog.taskId, date: workLog.date, hoursSpent: workLog.hoursSpent, note: workLog.note || '' });
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
      if (activeTab === 'my') { await workLogService.updateMy(editingWorkLog.id, requestData); }
      else { await workLogService.update(editingWorkLog.id, { personId: editingWorkLog.personId, ...requestData }); }
      showSuccess(t('workLogs.updateSuccess'));
      setEditDialogOpen(false); setEditingWorkLog(null);
      loadWorkLogs(selectedCycle === 'all' ? 'all' : parseInt(selectedCycle, 10));
    } catch (error: any) {
      showError(error.response?.data?.message || t('workLogs.errors.updateFailed'));
    }
  };

  const { hasPermission } = usePermission();
  const [canManageLogs, setCanManageLogs] = useState(false);
  const canLogForSelf = !!user?.personId;

  useEffect(() => {
    hasPermission('TEAM', 'MANAGE').then(setCanManageLogs).catch(() => setCanManageLogs(false));
  }, [hasPermission]);

  if (loading || isSwitchingProject) return <WorkLogsSkeleton />;

  const hasActiveFilters = filterPitchId !== 'all' || filterTaskId !== 'all' || filterPersonId !== 'all' || !!filterDateFrom || !!filterDateTo;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Work Logs</h1>
          <p className="text-muted-foreground">Track time spent on pitches and tasks</p>
        </div>
        {!isKanbanProject && (
          <Select value={selectedCycle} onValueChange={(v) => { setSelectedCycle(v); setPage(0); }}>
            <SelectTrigger className="w-[200px]">
              <SelectValue placeholder={t('workLogsPage.selectCycle')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t('workLogsPage.allCycles')}</SelectItem>
              {filteredCycles.map((cycle) => (
                <SelectItem key={cycle.id} value={cycle.id.toString()}>{cycle.name}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 mb-2">
              <CalendarDays className="h-5 w-5 text-primary" />
              <span className="text-sm text-muted-foreground">{t('workLogs.today')}</span>
            </div>
            <div className="text-3xl font-bold">{summaryTodayHours.toFixed(1)}h</div>
            <p className="text-xs text-muted-foreground">{t('workLogs.logsToday', { count: summaryTodayCount })}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 mb-2">
              <Clock className="h-5 w-5 text-secondary-foreground" />
              <span className="text-sm text-muted-foreground">
                {isKanbanProject ? t('workLogs.total', 'Total') : t('workLogs.thisCycle')}
              </span>
            </div>
            <div className="text-3xl font-bold">{summaryTotalHours.toFixed(1)}h</div>
            <p className="text-xs text-muted-foreground">{t('workLogs.logsCount', { count: summaryTotalCount })}</p>
          </CardContent>
        </Card>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={(v) => { setActiveTab(v as 'my' | 'team'); setPage(0); }} className="w-full">
        <div className="flex items-center justify-between">
          <TabsList>
            <TabsTrigger value="my" className="flex items-center gap-2">
              <User className="h-4 w-4" />
              {t('workLogs.myLogs')}
            </TabsTrigger>
            {canManageLogs && (
              <TabsTrigger value="team" className="flex items-center gap-2">
                <Users className="h-4 w-4" />
                {t('workLogs.teamLogs')}
              </TabsTrigger>
            )}
          </TabsList>

          {/* CTA button — context-aware per active tab */}
          {activeTab === 'my' && canLogForSelf && (
            <Button onClick={() => setQuickLogDialogOpen(true)}>
              <Plus className="h-4 w-4 mr-2" />
              {t('workLogs.logTime')}
            </Button>
          )}
          {activeTab === 'team' && canManageLogs && (
            <Button onClick={() => setTeamLogDialogOpen(true)}>
              <Plus className="h-4 w-4 mr-2" />
              {t('workLogs.logTime')}
            </Button>
          )}
        </div>

        {/* My Logs Tab */}
        <TabsContent value="my" className="space-y-4 mt-4">
          {!canLogForSelf ? (
            <Alert variant="default" className="border-amber-500 bg-amber-50 dark:bg-amber-950">
              <AlertTriangle className="h-4 w-4 text-amber-500" />
              <AlertDescription>{t('workLogs.notLinkedAlert')}</AlertDescription>
            </Alert>
          ) : (
            <>
              <LogFilters
                filterDateFrom={filterDateFrom} onFilterDateFromChange={(v) => { setFilterDateFrom(v); setPage(0); }}
                filterDateTo={filterDateTo} onFilterDateToChange={(v) => { setFilterDateTo(v); setPage(0); }}
                filterPitchId={filterPitchId} onFilterPitchIdChange={setFilterPitchId}
                filterTaskId={filterTaskId} onFilterTaskIdChange={setFilterTaskId}
                pitches={pitches} tasks={tasks}
                showPersonFilter={false}
                filterPersonId={filterPersonId} onFilterPersonIdChange={setFilterPersonId}
                persons={[]}
                hasActiveFilters={hasActiveFilters}
                onClearFilters={() => { setFilterPitchId('all'); setFilterTaskId('all'); setFilterPersonId('all'); setFilterDateFrom(''); setFilterDateTo(''); setPage(0); }}
              />
              <WorkLogsTable workLogs={filteredWorkLogs} showPerson={false} onEdit={handleEditClick} onDelete={handleDeleteMyWorkLog} />
              <Pagination page={page} totalPages={totalPages} totalElements={totalElements} pageSize={PAGE_SIZE} onPageChange={setPage} />
            </>
          )}
        </TabsContent>

        {/* Team Logs Tab */}
        {canManageLogs && (
          <TabsContent value="team" className="space-y-4 mt-4">
            <LogFilters
              filterDateFrom={filterDateFrom} onFilterDateFromChange={(v) => { setFilterDateFrom(v); setPage(0); }}
              filterDateTo={filterDateTo} onFilterDateToChange={(v) => { setFilterDateTo(v); setPage(0); }}
              filterPitchId={filterPitchId} onFilterPitchIdChange={setFilterPitchId}
              filterTaskId={filterTaskId} onFilterTaskIdChange={setFilterTaskId}
              pitches={pitches} tasks={tasks}
              showPersonFilter={true}
              filterPersonId={filterPersonId} onFilterPersonIdChange={setFilterPersonId}
              persons={persons}
              hasActiveFilters={hasActiveFilters}
              onClearFilters={() => { setFilterPitchId('all'); setFilterTaskId('all'); setFilterPersonId('all'); setFilterDateFrom(''); setFilterDateTo(''); setPage(0); }}
            />
            <WorkLogsTable workLogs={filteredWorkLogs} showPerson={true} onEdit={handleEditClick} onDelete={handleDeleteTeamWorkLog} />
            <Pagination page={page} totalPages={totalPages} totalElements={totalElements} pageSize={PAGE_SIZE} onPageChange={setPage} />
          </TabsContent>
        )}
      </Tabs>

      {/* Quick Log Dialog (My Logs) */}
      <Dialog open={quickLogDialogOpen} onOpenChange={setQuickLogDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('workLogs.quickLog')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>{t('workLogs.logType')} *</Label>
              <Select value={workLogType} onValueChange={(v: 'pitch' | 'task') => { setWorkLogType(v); setSelectedPitchId(''); setSelectedTaskId(''); }}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="task">{t('workLogs.taskSubtask')}</SelectItem>
                  <SelectItem value="pitch">{t('workLogs.pitch')}</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {workLogType === 'pitch' ? (
              <div className="space-y-2">
                <Label>{t('workLogs.pitch')} *</Label>
                <Select value={selectedPitchId} onValueChange={setSelectedPitchId}>
                  <SelectTrigger><SelectValue placeholder={t('workLogs.selectPitch')} /></SelectTrigger>
                  <SelectContent>
                    {pitches.map((p) => <SelectItem key={p.id} value={p.id.toString()}>{p.title}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            ) : (
              <div className="space-y-2">
                <Label>{t('workLogs.taskSubtask')} *</Label>
                <Select value={selectedTaskId} onValueChange={setSelectedTaskId} disabled={tasks.length === 0}>
                  <SelectTrigger><SelectValue placeholder={tasks.length === 0 ? t('workLogs.selectCycleFirst') : t('workLogs.selectTask')} /></SelectTrigger>
                  <SelectContent>
                    {tasks.map((tk) => <SelectItem key={tk.id} value={tk.id.toString()}>{tk.parentTaskId && '└─ '}{tk.title}</SelectItem>)}
                  </SelectContent>
                </Select>
                {tasks.length === 0 && workLogType === 'task' && (
                  <p className="text-xs text-muted-foreground">{t('workLogs.selectCycleToSeeTasks')}</p>
                )}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>{t('workLogs.date')}</Label>
                <LocalizedDateInput value={workLogDate} onChange={setWorkLogDate} />
              </div>
              <div className="space-y-2">
                <Label>{t('workLogs.hours')} *</Label>
                <Input type="number" min={0.25} step={0.25} value={newWorkLog.hoursSpent || ''} onChange={(e) => setNewWorkLog({ ...newWorkLog, hoursSpent: parseFloat(e.target.value) || 0 })} placeholder="0" />
              </div>
            </div>
            <div className="space-y-2">
              <Label>{t('workLogs.note')}</Label>
              <Textarea value={newWorkLog.note} onChange={(e) => setNewWorkLog({ ...newWorkLog, note: e.target.value })} placeholder={t('workLogs.optionalNote')} rows={2} className="resize-none" />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setQuickLogDialogOpen(false)}>{t('common.cancel')}</Button>
            <Button onClick={handleCreateMyWorkLog} disabled={(workLogType === 'pitch' && !selectedPitchId) || (workLogType === 'task' && !selectedTaskId) || !newWorkLog.hoursSpent}>
              <Plus className="h-4 w-4 mr-2" />{t('workLogs.logTime')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Team Log Dialog */}
      <Dialog open={teamLogDialogOpen} onOpenChange={setTeamLogDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('workLogs.logTimeForTeamMember')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>{t('workLogs.person')} *</Label>
              <Combobox
                options={persons.map((p) => ({ value: p.id.toString(), label: p.name }))}
                value={teamWorkLog.personId ? teamWorkLog.personId.toString() : ''}
                onValueChange={(v) => setTeamWorkLog({ ...teamWorkLog, personId: Number(v) })}
                placeholder={t('workLogs.selectPerson')}
              />
            </div>
            <div className="space-y-2">
              <Label>{t('workLogs.logType')} *</Label>
              <Select value={teamWorkLogType} onValueChange={(v: 'pitch' | 'task') => { setTeamWorkLogType(v); setTeamSelectedPitchId(''); setTeamSelectedTaskId(''); }}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="task">{t('workLogs.taskSubtask')}</SelectItem>
                  <SelectItem value="pitch">{t('workLogs.pitch')}</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {teamWorkLogType === 'pitch' ? (
              <div className="space-y-2">
                <Label>{t('workLogs.pitch')} *</Label>
                <Select value={teamSelectedPitchId} onValueChange={setTeamSelectedPitchId}>
                  <SelectTrigger><SelectValue placeholder={t('workLogs.selectPitch')} /></SelectTrigger>
                  <SelectContent>
                    {pitches.map((p) => <SelectItem key={p.id} value={p.id.toString()}>{p.title}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            ) : (
              <div className="space-y-2">
                <Label>{t('workLogs.taskSubtask')} *</Label>
                <Select value={teamSelectedTaskId} onValueChange={setTeamSelectedTaskId} disabled={tasks.length === 0}>
                  <SelectTrigger><SelectValue placeholder={tasks.length === 0 ? t('workLogs.selectCycleFirst') : t('workLogs.selectTask')} /></SelectTrigger>
                  <SelectContent>
                    {tasks.map((tk) => <SelectItem key={tk.id} value={tk.id.toString()}>{tk.parentTaskId && '└─ '}{tk.title}</SelectItem>)}
                  </SelectContent>
                </Select>
                {tasks.length === 0 && teamWorkLogType === 'task' && (
                  <p className="text-xs text-muted-foreground">{t('workLogs.selectCycleToSeeTasks')}</p>
                )}
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Date</Label>
                <LocalizedDateInput value={teamWorkLogDate} onChange={setTeamWorkLogDate} />
              </div>
              <div className="space-y-2">
                <Label>Hours *</Label>
                <Input type="number" min={0.25} step={0.25} value={teamWorkLog.hoursSpent || ''} onChange={(e) => setTeamWorkLog({ ...teamWorkLog, hoursSpent: parseFloat(e.target.value) || 0 })} placeholder="0" />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Note</Label>
              <Textarea value={teamWorkLog.note} onChange={(e) => setTeamWorkLog({ ...teamWorkLog, note: e.target.value })} placeholder={t('workLogsPage.optionalNote')} rows={2} className="resize-none" />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setTeamLogDialogOpen(false)}>{t('common.cancel')}</Button>
            <Button onClick={handleCreateTeamWorkLog} disabled={!teamWorkLog.personId || (teamWorkLogType === 'pitch' && !teamSelectedPitchId) || (teamWorkLogType === 'task' && !teamSelectedTaskId) || !teamWorkLog.hoursSpent}>
              <Plus className="h-4 w-4 mr-2" />{t('workLogs.logTime')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('workLogs.editWorkLog')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>{t('workLogs.logType')}</Label>
              <Select value={editWorkLogType} onValueChange={(v: 'pitch' | 'task') => { setEditWorkLogType(v); setEditPitchId(''); setEditTaskId(''); }}>
                <SelectTrigger><SelectValue /></SelectTrigger>
                <SelectContent>
                  <SelectItem value="task">Task/Subtask</SelectItem>
                  <SelectItem value="pitch">Pitch</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {editWorkLogType === 'pitch' ? (
              <div className="space-y-2">
                <Label>{t('workLogs.pitch')}</Label>
                <Select value={editPitchId} onValueChange={setEditPitchId}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {pitches.map((p) => <SelectItem key={p.id} value={p.id.toString()}>{p.title}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            ) : (
              <div className="space-y-2">
                <Label>{t('workLogs.taskSubtask')}</Label>
                <Select value={editTaskId} onValueChange={setEditTaskId}>
                  <SelectTrigger><SelectValue /></SelectTrigger>
                  <SelectContent>
                    {tasks.map((tk) => <SelectItem key={tk.id} value={tk.id.toString()}>{tk.parentTaskId && '└─ '}{tk.title}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            )}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>{t('workLogs.date')}</Label>
                <LocalizedDateInput value={editDate} onChange={setEditDate} />
              </div>
              <div className="space-y-2">
                <Label>{t('workLogs.hours')}</Label>
                <Input type="number" min={0.25} step={0.25} value={editForm.hoursSpent} onChange={(e) => setEditForm({ ...editForm, hoursSpent: parseFloat(e.target.value) || 0 })} />
              </div>
            </div>
            <div className="space-y-2">
              <Label>{t('workLogs.note')}</Label>
              <Textarea value={editForm.note} onChange={(e) => setEditForm({ ...editForm, note: e.target.value })} rows={3} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditDialogOpen(false)}>{t('common.cancel')}</Button>
            <Button onClick={handleEditSave}>{t('workLogs.saveChanges')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ── Sub-components ──────────────────────────────────────────────────────────

interface LogFiltersProps {
  filterDateFrom: string; onFilterDateFromChange: (v: string) => void;
  filterDateTo: string; onFilterDateToChange: (v: string) => void;
  filterPitchId: string; onFilterPitchIdChange: (v: string) => void;
  filterTaskId: string; onFilterTaskIdChange: (v: string) => void;
  pitches: Pitch[]; tasks: Task[];
  showPersonFilter: boolean;
  filterPersonId: string; onFilterPersonIdChange: (v: string) => void;
  persons: Person[];
  hasActiveFilters: boolean;
  onClearFilters: () => void;
}

function LogFilters({ filterDateFrom, onFilterDateFromChange, filterDateTo, onFilterDateToChange, filterPitchId, onFilterPitchIdChange, filterTaskId, onFilterTaskIdChange, pitches, tasks, showPersonFilter, filterPersonId, onFilterPersonIdChange, persons, hasActiveFilters, onClearFilters }: LogFiltersProps) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-wrap gap-3 items-end">
      <div className="space-y-1">
        <Label className="text-xs">{t('workLogsPage.filterFrom', 'From')}</Label>
        <LocalizedDateInput value={filterDateFrom} onChange={onFilterDateFromChange} className="w-[140px] h-9" />
      </div>
      <div className="space-y-1">
        <Label className="text-xs">{t('workLogsPage.filterTo', 'To')}</Label>
        <LocalizedDateInput value={filterDateTo} onChange={onFilterDateToChange} className="w-[140px] h-9" />
      </div>
      {showPersonFilter && (
        <div className="space-y-1">
          <Label className="text-xs">{t('workLogsPage.filterByPerson', 'Person')}</Label>
          <Combobox
            options={[
              { value: 'all', label: t('workLogsPage.allPersons', 'All People') },
              ...persons.map(p => ({ value: p.id.toString(), label: p.name })),
            ]}
            value={filterPersonId}
            onValueChange={onFilterPersonIdChange}
            placeholder={t('workLogsPage.allPersons', 'All People')}
            triggerClassName="w-[160px] h-9"
          />
        </div>
      )}
      <div className="space-y-1">
        <Label className="text-xs">{t('workLogsPage.filterByPitch', 'Pitch')}</Label>
        <Select value={filterPitchId} onValueChange={onFilterPitchIdChange}>
          <SelectTrigger className="w-[160px] h-9"><SelectValue placeholder={t('workLogsPage.allPitches', 'All Pitches')} /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t('workLogsPage.allPitches', 'All Pitches')}</SelectItem>
            {pitches.map(p => <SelectItem key={p.id} value={p.id.toString()}>{p.title}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      <div className="space-y-1">
        <Label className="text-xs">{t('workLogsPage.filterByTask', 'Task')}</Label>
        <Select value={filterTaskId} onValueChange={onFilterTaskIdChange}>
          <SelectTrigger className="w-[160px] h-9"><SelectValue placeholder={t('workLogsPage.allTasks', 'All Tasks')} /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t('workLogsPage.allTasks', 'All Tasks')}</SelectItem>
            {tasks.map(tk => <SelectItem key={tk.id} value={tk.id.toString()}>{tk.title}</SelectItem>)}
          </SelectContent>
        </Select>
      </div>
      {hasActiveFilters && (
        <Button variant="ghost" size="sm" onClick={onClearFilters}>
          {t('workLogsPage.clearFilters', 'Clear Filters')}
        </Button>
      )}
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
              <TableHead>Pitch / Task</TableHead>
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
                  {wl.taskId ? (
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs shrink-0">{t('workLogs.task')}</Badge>
                      <Link to={`/backlog/${wl.taskId}`} className="text-primary hover:underline">{wl.taskTitle}</Link>
                    </div>
                  ) : wl.pitchId ? (
                    <div className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs shrink-0">{t('workLogs.pitch')}</Badge>
                      <Link to={`/pitches/${wl.pitchId}`} className="text-primary hover:underline">{wl.pitchTitle}</Link>
                    </div>
                  ) : (
                    <span className="text-muted-foreground">—</span>
                  )}
                </TableCell>
                <TableCell className="text-right">
                  <Badge variant="secondary">{wl.hoursSpent}h</Badge>
                </TableCell>
                <TableCell className="max-w-[200px] truncate text-muted-foreground">{wl.note || '—'}</TableCell>
                <TableCell className="text-right">
                  <div className="flex justify-end gap-1">
                    <Button variant="ghost" size="icon" onClick={() => onEdit(wl)}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button variant="ghost" size="icon" onClick={() => onDelete(wl.id)}>
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

interface PaginationProps {
  page: number; totalPages: number; totalElements: number; pageSize: number;
  onPageChange: (p: number) => void;
}

function Pagination({ page, totalPages, totalElements, pageSize, onPageChange }: PaginationProps) {
  const { t } = useTranslation();
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between mt-4">
      <div className="text-sm text-muted-foreground">
        {t('meetingList.pagination.showing', { from: page * pageSize + 1, to: Math.min((page + 1) * pageSize, totalElements), total: totalElements })}
      </div>
      <div className="flex items-center gap-2">
        <Button variant="outline" size="sm" onClick={() => onPageChange(Math.max(0, page - 1))} disabled={page === 0}>
          {t('meetingList.pagination.previous', { defaultValue: 'Previous' })}
        </Button>
        <span className="text-sm text-muted-foreground">
          {t('meetingList.pagination.page', { current: page + 1, total: totalPages })}
        </span>
        <Button variant="outline" size="sm" onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1}>
          {t('meetingList.pagination.next', { defaultValue: 'Next' })}
        </Button>
      </div>
    </div>
  );
}
