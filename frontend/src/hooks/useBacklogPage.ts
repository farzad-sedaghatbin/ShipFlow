import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import dayjs, { Dayjs } from 'dayjs';
import { toast } from 'sonner';
import { taskService } from '../services/taskService';
import { cycleService } from '../services/cycleService';
import { personService } from '../services/personService';
import { teamService } from '../services/teamService';
import { pitchService } from '../services/pitchService';
import { releaseService } from '../services/releaseService';
import timerService from '../services/timerService';
import {
  Task,
  Cycle,
  Person,
  Pitch,
  Release,
  Team,
  CreateTaskRequest,
  TaskStatus,
  TaskPriority,
  TaskStatistics,
  TaskCategory,
} from '../types';
import { getUserFriendlyError } from '../utils/errorMessages';
import { useProject, useAuth } from '../contexts';
import { ViewMode } from '../components/backlog';

// Large page size for Kanban — fetches effectively all tasks for the board
const KANBAN_PAGE_SIZE = 500;

export function useBacklogPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const categoryFromUrl = searchParams.get('category') as TaskCategory | null;
  const { isKanbanProject, currentProject, isSwitchingProject, notifyProjectSwitchComplete } = useProject();
  const { user } = useAuth();

  // Data
  const [tasks, setTasks] = useState<Task[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [releases, setReleases] = useState<Release[]>([]);
  const [statistics, setStatistics] = useState<TaskStatistics | null>(null);
  const [subtasks, setSubtasks] = useState<Task[]>([]);

  // Loading
  const [loading, setLoading] = useState(true);
  const [tasksLoading, setTasksLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [exportLoading, setExportLoading] = useState(false);

  // Selection / view
  const [selectedCycle, setSelectedCycle] = useState<number | 'all'>('all');
  const [activeCategory, setActiveCategory] = useState<TaskCategory>(categoryFromUrl || 'PITCH_SCOPE');
  const [tabValue, setTabValue] = useState('all');
  const [viewMode, setViewMode] = useState<ViewMode>(isKanbanProject ? 'kanban' : 'list');
  const [activeTimerTaskId, setActiveTimerTaskId] = useState<number | null>(null);

  // Kanban column visibility
  const [visibleColumns, setVisibleColumns] = useState<TaskStatus[]>([
    'BACKLOG', 'TODO', 'IN_PROGRESS', 'BLOCKED', 'IN_REVIEW', 'DONE', 'CANCELLED',
  ]);

  // Filters / sort / pagination
  const [statusFilter, setStatusFilter] = useState<TaskStatus[]>([]);
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority[]>([]);
  const [assigneeFilter, setAssigneeFilter] = useState<number[]>([]);
  const [releaseFilter, setReleaseFilter] = useState<number | undefined>(undefined);
  const [searchQuery, setSearchQuery] = useState('');
  const [dependencyFilter, setDependencyFilter] = useState<'all' | 'blocked' | 'blocking'>('all');
  const [excludeMode] = useState(false);
  const [sortBy, setSortBy] = useState<'createdAt' | 'priority' | 'status' | 'dueDate' | 'title'>('createdAt');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);

  // Bulk selection + dropdown open states
  const [selectedTaskIds, setSelectedTaskIds] = useState<Set<number>>(new Set());
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const [priorityDropdownOpen, setPriorityDropdownOpen] = useState(false);
  const [dependencyDropdownOpen, setDependencyDropdownOpen] = useState(false);
  const [assigneeDropdownOpen, setAssigneeDropdownOpen] = useState(false);

  // Dialog states
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [formData, setFormData] = useState<CreateTaskRequest>({
    title: '', description: '', cycleId: 0, status: 'BACKLOG', priority: 'MEDIUM',
    estimateHours: undefined, assigneeId: undefined, pairAssigneeId: undefined,
    dueDate: undefined, tags: '', category: 'PITCH_SCOPE', pitchId: undefined,
  });
  const [dueDate, setDueDate] = useState<Dayjs | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; taskId: number | null }>({ open: false, taskId: null });
  const [viewDialog, setViewDialog] = useState<{ open: boolean; task: Task | null }>({ open: false, task: null });
  const [viewHistory, setViewHistory] = useState<Task[]>([]);

  // ── Effects ───────────────────────────────────────────────────────────────────

  useEffect(() => { setViewMode(isKanbanProject ? 'kanban' : 'list'); }, [isKanbanProject]);

  useEffect(() => { loadInitialData(); loadActiveTimer(); }, []);

  // Fix: run loadInitialData on every project switch (including "All Projects" where currentProject is null)
  useEffect(() => { loadInitialData(); }, [currentProject?.id, isKanbanProject]);

  useEffect(() => {
    if (dialogOpen) {
      if (formData.cycleId && formData.cycleId !== 0) { loadPitchesForCycle(formData.cycleId); }
      else { loadAllPitches(); }
    }
  }, [dialogOpen, formData.cycleId]);

  // Fix: include searchQuery, releaseFilter, viewMode in deps
  useEffect(() => {
    if (isKanbanProject && currentProject) { loadTasks(); loadStatistics(); }
    else if (selectedCycle) { loadTasks(); loadStatistics(); }
    else { setTasks([]); setTotalElements(0); setTasksLoading(false); setStatistics(null); }
  }, [
    selectedCycle, currentProject?.id, isKanbanProject, viewMode,
    activeCategory, tabValue,
    statusFilter, priorityFilter, assigneeFilter, releaseFilter, searchQuery,
    excludeMode, page, rowsPerPage, sortBy, sortOrder, dependencyFilter,
  ]);

  // Fix: clear bulk selection when visible task set changes
  useEffect(() => {
    setSelectedTaskIds(new Set());
  }, [tasks]);

  useEffect(() => {
    if (categoryFromUrl && categoryFromUrl !== activeCategory) setActiveCategory(categoryFromUrl);
  }, [categoryFromUrl]);

  // ── Data loaders ──────────────────────────────────────────────────────────────

  const loadInitialData = async () => {
    try {
      // Fix: use project-scoped cycles when a project is selected
      const cyclesPromise = currentProject
        ? cycleService.getActiveByProject(currentProject.id)
        : cycleService.getMyActiveCycles();
      const [cyclesRes, personsRes, teamsRes] = await Promise.all([cyclesPromise, personService.getAll(), teamService.getAll()]);
      setCycles(cyclesRes.data);
      setPersons(personsRes);
      setTeams(teamsRes.data);
      if (currentProject) {
        const projectCycles = cyclesRes.data.filter((c: Cycle) => c.projectId === currentProject.id);
        setSelectedCycle(projectCycles.length > 0 ? projectCycles[0].id : 'all');
        try {
          const releasesRes = await releaseService.getByProject(currentProject.id);
          setReleases(releasesRes.data);
        } catch { setReleases([]); }
      } else {
        setSelectedCycle('all');
        setReleases([]);
      }
    } catch (error) { console.error('Failed to load data:', error); }
    finally { setLoading(false); notifyProjectSwitchComplete(); }
  };

  const loadActiveTimer = async () => {
    try {
      const timer = await timerService.getActiveTimer();
      setActiveTimerTaskId(timer && timer.taskId ? timer.taskId : null);
    } catch (error) { console.error('Failed to load active timer:', error); }
  };

  const applyDependencyFilter = (items: Task[]) => {
    if (dependencyFilter === 'blocked') return items.filter((t) => t.isBlocked && t.blockedByCount && t.blockedByCount > 0);
    if (dependencyFilter === 'blocking') return items.filter((t) => t.blockingTasks && t.blockingTasks.length > 0);
    return items;
  };

  const applyCommonFilters = (items: Task[]) => {
    let result = items;
    if (statusFilter.length > 0) result = result.filter((t) => excludeMode ? !statusFilter.includes(t.status) : statusFilter.includes(t.status));
    if (priorityFilter.length > 0) result = result.filter((t) => excludeMode ? !priorityFilter.includes(t.priority) : priorityFilter.includes(t.priority));
    if (assigneeFilter.length > 0) result = result.filter((t) => excludeMode ? !assigneeFilter.includes(t.assigneeId || 0) : assigneeFilter.includes(t.assigneeId || 0));
    if (searchQuery.trim()) {
      const q = searchQuery.trim().toLowerCase();
      result = result.filter((t) => t.title.toLowerCase().includes(q) || (t.description || '').toLowerCase().includes(q));
    }
    return applyDependencyFilter(result);
  };

  const loadTasks = async () => {
    if (isKanbanProject && currentProject) {
      setTasksLoading(true);
      const timeout = setTimeout(() => setTasksLoading(false), 10000);
      try {
        // Fix: Kanban fetches a large unpaginated set so all columns are populated
        const response = await taskService.getByProjectIdAndCategory(
          currentProject.id, activeCategory, 0, KANBAN_PAGE_SIZE, sortBy, sortOrder,
        );
        let filtered = applyCommonFilters(response?.data?.content || []);
        if (tabValue === 'my' && user?.personId) filtered = filtered.filter((t) => t.assigneeId === user.personId);
        setTasks(filtered);
        setTotalElements(response?.data?.totalElements || 0);
      } catch (error) { console.error('Failed to load project tasks:', error); setTasks([]); setTotalElements(0); }
      finally { clearTimeout(timeout); setTasksLoading(false); }
      return;
    }

    if (!selectedCycle) { setTasks([]); setTotalElements(0); setTasksLoading(false); return; }
    setTasksLoading(true);
    const timeout = setTimeout(() => setTasksLoading(false), 10000);
    try {
      let response: any;
      if (tabValue === 'my') {
        if (selectedCycle === 'all') {
          response = await taskService.getMy(page, rowsPerPage, sortBy, sortOrder);
        } else {
          response = await taskService.getMyByCycle(selectedCycle, page, rowsPerPage, sortBy, sortOrder);
        }
        const filtered = (response?.data?.content || []).filter((t: Task) => (t.category || 'PITCH_SCOPE') === activeCategory);
        setTasks(filtered);
        setTotalElements(selectedCycle === 'all' ? (response?.data?.totalElements || 0) : filtered.length);
      } else if (selectedCycle === 'all') {
        response = await taskService.getAll(page, rowsPerPage, sortBy, sortOrder);
        const byCat = (response?.data?.content || []).filter((t: Task) => (t.category || 'PITCH_SCOPE') === activeCategory);
        setTasks(applyCommonFilters(byCat));
        setTotalElements(response?.data?.totalElements || 0);
      } else if (statusFilter.length > 0 || priorityFilter.length > 0 || assigneeFilter.length > 0) {
        response = await taskService.getWithFilters(selectedCycle, statusFilter.length > 0 ? statusFilter : undefined, priorityFilter.length > 0 ? priorityFilter : undefined, assigneeFilter.length > 0 ? assigneeFilter : undefined, activeCategory, excludeMode, page, rowsPerPage, sortBy, sortOrder);
        setTasks(applyDependencyFilter(response?.data?.content || []));
        setTotalElements(response?.data?.totalElements || 0);
      } else {
        response = await taskService.getByCycleIdAndCategory(selectedCycle, activeCategory, page, rowsPerPage, sortBy, sortOrder);
        setTasks(applyDependencyFilter(response?.data?.content || []));
        setTotalElements(response?.data?.totalElements || 0);
      }
    } catch (error) { console.error('Failed to load tasks:', error); setTasks([]); setTotalElements(0); }
    finally { clearTimeout(timeout); setTasksLoading(false); }
  };

  const loadStatistics = async () => {
    if (isKanbanProject && currentProject) {
      try { const r = await taskService.getStatisticsByProjectId(currentProject.id); setStatistics(r.data); }
      catch (error) { console.error('Failed to load project statistics:', error); }
      return;
    }
    if (!selectedCycle || selectedCycle === 'all') return;
    try { const r = await taskService.getStatisticsByCycleId(selectedCycle); setStatistics(r.data); }
    catch (error) { console.error('Failed to load statistics:', error); }
  };

  const loadPitchesForCycle = async (cycleId: number) => {
    try { const r = await pitchService.getByCycleId(cycleId); setPitches(r.data); }
    catch { setPitches([]); }
  };

  const loadAllPitches = async () => {
    try { const r = await pitchService.getMyPitches(); setPitches(r.data); }
    catch { setPitches([]); }
  };

  // ── Handlers ──────────────────────────────────────────────────────────────────

  const handleToggleColumn = (status: TaskStatus) => {
    setVisibleColumns(prev =>
      prev.includes(status) ? prev.filter(s => s !== status) : [...prev, status],
    );
  };

  // Toggle single status in filter (matches new BacklogFilters interface)
  const handleToggleStatusFilter = (status: TaskStatus) => {
    setStatusFilter(prev =>
      prev.includes(status) ? prev.filter(s => s !== status) : [...prev, status],
    );
    setPage(0);
  };

  // Toggle single priority in filter (matches new BacklogFilters interface)
  const handleTogglePriorityFilter = (priority: TaskPriority) => {
    setPriorityFilter(prev =>
      prev.includes(priority) ? prev.filter(p => p !== priority) : [...prev, priority],
    );
    setPage(0);
  };

  const handleToggleAssigneeFilter = (personId: number) => {
    setAssigneeFilter(prev =>
      prev.includes(personId) ? prev.filter(id => id !== personId) : [...prev, personId],
    );
    setPage(0);
  };

  const handlePitchChange = (pitchId: string) => {
    const pitch = pitchId === 'none' ? undefined : Number(pitchId);
    setFormData({ ...formData, pitchId: pitch });
  };

  const handleCategoryChange = (category: TaskCategory) => {
    setActiveCategory(category);
    setSearchParams({ category });
    setPage(0);
  };

  const handleOpenDialog = (task?: Task) => {
    if (task) {
      setEditingTask(task);
      // Fix: include pitchId and parentTaskId when editing
      setFormData({
        title: task.title,
        description: task.description || '',
        cycleId: task.cycleId,
        status: task.status,
        priority: task.priority,
        estimateHours: task.estimateHours,
        actualHours: task.actualHours,
        teamId: task.teamId,
        assigneeId: task.assigneeId,
        pairAssigneeId: task.pairAssigneeId,
        dueDate: task.dueDate,
        tags: task.tags || '',
        category: task.category || activeCategory,
        pitchId: task.pitchId,
        parentTaskId: task.parentTaskId,
      });
      setDueDate(task.dueDate ? dayjs(task.dueDate) : null);
    } else {
      if (!isKanbanProject && (!selectedCycle || selectedCycle === 'all')) { toast.error(t('backlogPage.selectCycleToCreate')); return; }
      let cycleIdToUse = typeof selectedCycle === 'number' ? selectedCycle : 0;
      if (isKanbanProject && currentProject && cycleIdToUse === 0) {
        const projectCycles = cycles.filter(c => c.projectId === currentProject.id);
        if (projectCycles.length > 0) { cycleIdToUse = projectCycles[0].id; } else { toast.error(t('backlogPage.noDefaultCycle')); return; }
      }
      setEditingTask(null);
      setFormData({ title: '', description: '', cycleId: cycleIdToUse, status: 'BACKLOG', priority: 'MEDIUM', estimateHours: undefined, assigneeId: undefined, pairAssigneeId: undefined, dueDate: undefined, tags: '', category: activeCategory, pitchId: undefined });
      setDueDate(null);
    }
    setDialogOpen(true);
  };

  const handleCloseDialog = () => { setDialogOpen(false); setEditingTask(null); setFieldErrors({}); };

  const handleAddSubTask = (parentTask: Task) => {
    setFormData({ title: '', description: '', cycleId: parentTask.cycleId, parentTaskId: parentTask.id, status: 'BACKLOG', priority: 'MEDIUM', estimateHours: undefined, assigneeId: undefined, pairAssigneeId: undefined, dueDate: undefined, tags: '', category: activeCategory });
    setDueDate(null);
    setEditingTask(null);
    setDialogOpen(true);
  };

  const handleSort = (field: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title') => {
    if (sortBy === field) { setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc'); } else { setSortBy(field); setSortOrder('desc'); }
    setPage(0);
  };

  const handleStartTimer = async (task: Task) => {
    try {
      await timerService.startTimer({ taskId: task.id, note: `Working on: ${task.title}` });
      setActiveTimerTaskId(task.id);
      await loadActiveTimer();
      toast.success(t('backlogPage.timerStarted'));
    } catch (error: any) { toast.error(error.response?.data?.message || 'Failed to start timer'); }
  };

  const handleTimerStopped = () => {
    setActiveTimerTaskId(null);
    loadTasks();
    if (isKanbanProject || selectedCycle !== 'all') loadStatistics();
  };

  const handleViewTask = async (task: Task, addToHistory = true) => {
    if (addToHistory && viewDialog.task) setViewHistory([...viewHistory, viewDialog.task]);
    setViewDialog({ open: true, task });
    try { const r = await taskService.getSubTasks(task.id); setSubtasks(r.data); }
    catch { setSubtasks([]); }
  };

  const handleViewBack = () => {
    if (viewHistory.length > 0) {
      const previousTask = viewHistory[viewHistory.length - 1];
      setViewHistory(viewHistory.slice(0, -1));
      handleViewTask(previousTask, false);
    }
  };

  const handleCloseViewDialog = () => { setViewDialog({ open: false, task: null }); setViewHistory([]); };

  const validateTaskForm = (): boolean => {
    const errors: Record<string, string> = {};
    if (!formData.title.trim()) { errors.title = t('backlogPage.titleRequired'); }
    else if (formData.title.trim().length < 3) { errors.title = t('backlogPage.titleMinLength'); }
    if (!isKanbanProject && (!formData.cycleId || formData.cycleId === 0)) { errors.cycleId = t('backlogPage.cycleRequired'); }
    if (formData.estimateHours !== undefined && formData.estimateHours < 0) { errors.estimateHours = t('backlogPage.estimatePositive'); }
    if (formData.actualHours !== undefined && formData.actualHours < 0) { errors.actualHours = t('backlogPage.actualPositive'); }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSaveTask = async () => {
    if (!validateTaskForm()) return;
    try {
      setSaving(true);
      const data = { ...formData, dueDate: dueDate ? dueDate.format('YYYY-MM-DD') : undefined, category: activeCategory };
      if (editingTask) { await taskService.update(editingTask.id, data); toast.success(t('backlogPage.taskUpdated')); }
      else { await taskService.create(data); toast.success(t('backlogPage.taskCreated')); }
      handleCloseDialog(); loadTasks(); loadStatistics();
    } catch (error: any) { toast.error(getUserFriendlyError(error)); }
    finally { setSaving(false); }
  };

  const handleDeleteTask = async () => {
    if (!deleteDialog.taskId) return;
    try {
      await taskService.delete(deleteDialog.taskId);
      toast.success(t('backlogPage.taskDeleted'));
      setDeleteDialog({ open: false, taskId: null });
      loadTasks(); loadStatistics();
    } catch (error: any) { toast.error(getUserFriendlyError(error)); }
  };

  const handleQuickStatusChange = async (taskId: number, newStatus: TaskStatus) => {
    try { await taskService.updateStatus(taskId, newStatus); toast.success(t('backlogPage.statusUpdated')); loadTasks(); loadStatistics(); }
    catch (error: any) { toast.error(getUserFriendlyError(error)); }
  };

  const handleQuickPriorityChange = async (taskId: number, newPriority: TaskPriority) => {
    try { await taskService.updatePriority(taskId, newPriority); toast.success(t('backlogPage.priorityUpdated')); loadTasks(); loadStatistics(); }
    catch (error: any) { toast.error(getUserFriendlyError(error)); }
  };

  /**
   * Optimistically update local task order after a drag-to-reorder.
   * Called by BacklogTaskTable before the API request is made.
   */
  const handleReorder = (reorderedTasks: Task[]) => {
    setTasks(reorderedTasks);
  };

  const handleClearFilters = () => {
    setStatusFilter([]); setPriorityFilter([]); setAssigneeFilter([]);
    setDependencyFilter('all'); setReleaseFilter(undefined); setSearchQuery('');
    setPage(0);
  };

  const handleExportCsv = async () => {
    if (!currentProject) return;
    const hasCycle = typeof selectedCycle === 'number';
    setExportLoading(true);
    try {
      const response = await taskService.exportTasks({
        projectId: hasCycle ? undefined : currentProject.id,
        cycleId: hasCycle ? selectedCycle : undefined,
        statuses: statusFilter.length > 0 ? statusFilter : undefined,
        priorities: priorityFilter.length > 0 ? priorityFilter : undefined,
        assigneeIds: assigneeFilter.length > 0 ? assigneeFilter : undefined,
        category: activeCategory,
      });
      const blob = new Blob([response.data as unknown as BlobPart], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = hasCycle
        ? `tasks-cycle${selectedCycle}-${new Date().toISOString().slice(0, 10)}.csv`
        : `tasks-${currentProject.id}-${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(anchor);
      anchor.click();
      document.body.removeChild(anchor);
      URL.revokeObjectURL(url);
      toast.success(t('backlogPage.exportSuccess'));
    } catch (error: any) {
      toast.error(getUserFriendlyError(error));
    } finally {
      setExportLoading(false);
    }
  };

  // ── Derived values ────────────────────────────────────────────────────────────

  const categoryTitle = activeCategory === 'PITCH_SCOPE'
    ? (isKanbanProject ? t('backlogPage.featureTasks') : t('backlogPage.pitchTasks'))
    : t('backlogPage.debtImprovements');

  const categoryDescription = activeCategory === 'PITCH_SCOPE'
    ? (isKanbanProject ? t('backlogPage.categoryDescription.featureScope') : t('backlogPage.categoryDescription.pitchScope'))
    : t('backlogPage.categoryDescription.debtImprovement');

  const hasActiveFilters = statusFilter.length > 0 || priorityFilter.length > 0
    || assigneeFilter.length > 0 || dependencyFilter !== 'all' || !!releaseFilter || !!searchQuery;

  return {
    // State
    tasks, totalElements, cycles, persons, teams, pitches, releases, statistics, subtasks,
    loading, tasksLoading, saving, exportLoading,
    selectedCycle, setSelectedCycle,
    activeCategory,
    tabValue, setTabValue,
    viewMode, setViewMode,
    activeTimerTaskId,
    visibleColumns,
    statusFilter, setStatusFilter,
    priorityFilter, setPriorityFilter,
    assigneeFilter, setAssigneeFilter,
    releaseFilter, setReleaseFilter,
    searchQuery, setSearchQuery,
    dependencyFilter, setDependencyFilter,
    sortBy, sortOrder,
    page, setPage,
    rowsPerPage, setRowsPerPage,
    selectedTaskIds, setSelectedTaskIds,
    statusDropdownOpen, setStatusDropdownOpen,
    priorityDropdownOpen, setPriorityDropdownOpen,
    dependencyDropdownOpen, setDependencyDropdownOpen,
    assigneeDropdownOpen, setAssigneeDropdownOpen,
    hasActiveFilters,
    dialogOpen, setDialogOpen,
    editingTask,
    formData, setFormData,
    dueDate, setDueDate,
    fieldErrors,
    deleteDialog, setDeleteDialog,
    viewDialog, setViewDialog,
    viewHistory,
    isKanbanProject,
    isSwitchingProject,
    currentProject,

    // Handlers
    handleToggleColumn,
    handleToggleStatusFilter,
    handleTogglePriorityFilter,
    handleToggleAssigneeFilter,
    handlePitchChange,
    handleCategoryChange,
    handleOpenDialog,
    handleCloseDialog,
    handleAddSubTask,
    handleSort,
    handleStartTimer,
    handleTimerStopped,
    handleViewTask,
    handleViewBack,
    handleCloseViewDialog,
    handleSaveTask,
    handleDeleteTask,
    handleQuickStatusChange,
    handleQuickPriorityChange,
    handleReorder,
    handleClearFilters,
    handleExportCsv,
    loadTasks,

    // Derived
    categoryTitle,
    categoryDescription,
  };
}
