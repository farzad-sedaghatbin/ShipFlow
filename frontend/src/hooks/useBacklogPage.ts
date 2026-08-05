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
  const { isKanbanProject, isStrictlyShapeUp, currentProject, isSwitchingProject, notifyProjectSwitchComplete } = useProject();
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

  // Debt/Improvement work in Shape Up is opportunistic filler picked up when nothing else is
  // scheduled — it doesn't need to be bet on a specific cycle upfront, unlike shaped pitch scope.
  // So this one category+methodology combo may skip the cycle requirement (still needs a project
  // to attach to, since a cycle-less task is stored via a direct project reference).
  const canSkipCycleForDebtImprovement = isStrictlyShapeUp && activeCategory === 'DEBT_IMPROVEMENT' && !!currentProject;

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

  // Centralized page-reset: switching tabs/cycle/any filter can change the result set size, so
  // always land back on page 0 rather than stranding the user on a now-empty page. Safe on
  // mount — page is already 0, so this no-ops. Replaces the scattered manual setPage(0) calls
  // that used to live in individual filter setters (and missed selectedCycle/tabValue/
  // dependencyFilter entirely).
  useEffect(() => { setPage(0); }, [
    selectedCycle, tabValue, statusFilter, priorityFilter, assigneeFilter,
    releaseFilter, searchQuery, dependencyFilter,
  ]);

  // ── Data loaders ──────────────────────────────────────────────────────────────

  const loadInitialData = async () => {
    try {
      // Fix: use project-scoped cycles when a project is selected
      const cyclesPromise = currentProject
        ? cycleService.getActiveByProject(currentProject.id)
        : cycleService.getMyActiveCycles();
      const [cyclesRes, personsRes, teamsRes] = await Promise.all([cyclesPromise, personService.getAll(), teamService.getAll()]);
      setCycles(cyclesRes.data);
      const currentPersonId = user?.personId;
      if (currentPersonId) {
        personsRes.sort((a: Person, b: Person) => (a.id === currentPersonId ? -1 : b.id === currentPersonId ? 1 : 0));
      }
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
        // Kanban has no Pitch concept, so the Feature Tasks / Debt & Improvements
        // split isn't meaningful there - fetch every task for the project instead
        // of filtering by activeCategory (that split is Shape Up-only; see the
        // hidden category-tab UI in BacklogPage.tsx for the same isKanbanProject
        // gate). Large unpaginated fetch so all Kanban columns are populated.
        const response = await taskService.getByProjectIdPaged(
          currentProject.id, 0, KANBAN_PAGE_SIZE, sortBy, sortOrder,
        );
        let filtered = applyCommonFilters(response?.data?.content || []);
        if (tabValue === 'my' && user?.personId) filtered = filtered.filter((t) => t.assigneeId === user.personId);
        setTasks(filtered);
        // dependencyFilter is applied client-side only — fall back to the filtered length so
        // the count doesn't reflect the server's unfiltered total.
        setTotalElements(
          dependencyFilter !== 'all'
            ? filtered.length
            : (response?.data?.page?.totalElements ?? response?.data?.totalElements ?? 0),
        );
      } catch (error) { console.error('Failed to load project tasks:', error); setTasks([]); setTotalElements(0); }
      finally { clearTimeout(timeout); setTasksLoading(false); }
      return;
    }

    if (!selectedCycle) { setTasks([]); setTotalElements(0); setTasksLoading(false); return; }
    setTasksLoading(true);
    const timeout = setTimeout(() => setTasksLoading(false), 10000);
    try {
      let response: any;

      // Server-side search: when query ≥ 3 chars, hit /tasks/search then filter locally
      if (searchQuery.trim().length >= 3) {
        response = await taskService.search(searchQuery.trim(), 0, 500, sortBy, sortOrder);
        let results: Task[] = response?.data?.content || [];
        if (selectedCycle !== 'all') results = results.filter((t) => t.cycleId === selectedCycle);
        results = results.filter((t) => (t.category || 'PITCH_SCOPE') === activeCategory);
        if (statusFilter.length > 0) results = results.filter((t) => excludeMode ? !statusFilter.includes(t.status) : statusFilter.includes(t.status));
        if (priorityFilter.length > 0) results = results.filter((t) => excludeMode ? !priorityFilter.includes(t.priority) : priorityFilter.includes(t.priority));
        if (assigneeFilter.length > 0) results = results.filter((t) => excludeMode ? !assigneeFilter.includes(t.assigneeId || 0) : assigneeFilter.includes(t.assigneeId || 0));
        if (tabValue === 'my' && user?.personId) results = results.filter((t) => t.assigneeId === user.personId);
        results = applyDependencyFilter(results);
        setTasks(results);
        setTotalElements(results.length);
        return;
      }

      if (tabValue === 'my') {
        if (selectedCycle === 'all') {
          response = await taskService.getMy(page, rowsPerPage, sortBy, sortOrder, activeCategory);
        } else {
          response = await taskService.getMyByCycle(selectedCycle, page, rowsPerPage, sortBy, sortOrder, activeCategory);
        }
        const filteredTasks = applyCommonFilters(response?.data?.content || []);
        setTasks(filteredTasks);
        // dependencyFilter is applied client-side only (never sent server-side), so the server's
        // totalElements only reflects the true count when dependencyFilter is 'all' — otherwise
        // fall back to the filtered array length so "Page N of M" doesn't strand the user.
        setTotalElements(
          dependencyFilter !== 'all'
            ? filteredTasks.length
            : (response?.data?.page?.totalElements ?? response?.data?.totalElements ?? 0),
        );
      } else if (selectedCycle === 'all') {
        response = await taskService.getAll(page, rowsPerPage, sortBy, sortOrder, activeCategory);
        const filteredTasks = applyCommonFilters(response?.data?.content || []);
        setTasks(filteredTasks);
        // dependencyFilter is applied client-side only (never sent server-side), so the server's
        // totalElements only reflects the true count when dependencyFilter is 'all' — otherwise
        // fall back to the filtered array length so "Page N of M" doesn't strand the user.
        setTotalElements(
          dependencyFilter !== 'all'
            ? filteredTasks.length
            : (response?.data?.page?.totalElements ?? response?.data?.totalElements ?? 0),
        );
      } else if (statusFilter.length > 0 || priorityFilter.length > 0 || assigneeFilter.length > 0) {
        response = await taskService.getWithFilters(selectedCycle, statusFilter.length > 0 ? statusFilter : undefined, priorityFilter.length > 0 ? priorityFilter : undefined, assigneeFilter.length > 0 ? assigneeFilter : undefined, activeCategory, excludeMode, page, rowsPerPage, sortBy, sortOrder);
        const filteredTasks = applyCommonFilters(response?.data?.content || []);
        setTasks(filteredTasks);
        // dependencyFilter is applied client-side only (never sent server-side), so the server's
        // totalElements only reflects the true count when dependencyFilter is 'all' — otherwise
        // fall back to the filtered array length so "Page N of M" doesn't strand the user.
        setTotalElements(
          dependencyFilter !== 'all'
            ? filteredTasks.length
            : (response?.data?.page?.totalElements ?? response?.data?.totalElements ?? 0),
        );
      } else {
        response = await taskService.getByCycleIdAndCategory(selectedCycle, activeCategory, page, rowsPerPage, sortBy, sortOrder);
        const filteredTasks = applyCommonFilters(response?.data?.content || []);
        setTasks(filteredTasks);
        // dependencyFilter is applied client-side only (never sent server-side), so the server's
        // totalElements only reflects the true count when dependencyFilter is 'all' — otherwise
        // fall back to the filtered array length so "Page N of M" doesn't strand the user.
        setTotalElements(
          dependencyFilter !== 'all'
            ? filteredTasks.length
            : (response?.data?.page?.totalElements ?? response?.data?.totalElements ?? 0),
        );
      }
    } catch (error) { console.error('Failed to load tasks:', error); setTasks([]); setTotalElements(0); }
    finally { clearTimeout(timeout); setTasksLoading(false); }
  };

  const loadStatistics = async () => {
    if (isKanbanProject || selectedCycle === 'all') {
      if (!currentProject) { setStatistics(null); return; }
      try { const r = await taskService.getStatisticsByProjectId(currentProject.id); setStatistics(r.data); }
      catch (error) { console.error('Failed to load project statistics:', error); }
      return;
    }
    if (!selectedCycle) { setStatistics(null); return; }
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
  // Page reset is handled by the centralized effect above (statusFilter is in its deps).
  const handleToggleStatusFilter = (status: TaskStatus) => {
    setStatusFilter(prev =>
      prev.includes(status) ? prev.filter(s => s !== status) : [...prev, status],
    );
  };

  // Toggle single priority in filter (matches new BacklogFilters interface)
  // Page reset is handled by the centralized effect above (priorityFilter is in its deps).
  const handleTogglePriorityFilter = (priority: TaskPriority) => {
    setPriorityFilter(prev =>
      prev.includes(priority) ? prev.filter(p => p !== priority) : [...prev, priority],
    );
  };

  // Page reset is handled by the centralized effect above (assigneeFilter is in its deps).
  const handleToggleAssigneeFilter = (personId: number) => {
    setAssigneeFilter(prev =>
      prev.includes(personId) ? prev.filter(id => id !== personId) : [...prev, personId],
    );
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
        // Cycle-less tasks (Shape Up Debt/Improvement backlog items) carry a direct project
        // reference instead — preserve it so re-saving doesn't fail cycle validation.
        projectId: !task.cycleId ? task.projectId : undefined,
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
      if (!isKanbanProject && (!selectedCycle || selectedCycle === 'all') && !canSkipCycleForDebtImprovement) {
        toast.error(t('backlogPage.selectCycleToCreate'));
        return;
      }
      let cycleIdToUse: number | undefined = typeof selectedCycle === 'number' ? selectedCycle : undefined;
      if (isKanbanProject && currentProject && cycleIdToUse === undefined) {
        const projectCycles = cycles.filter(c => c.projectId === currentProject.id);
        if (projectCycles.length > 0) { cycleIdToUse = projectCycles[0].id; } else { toast.error(t('backlogPage.noDefaultCycle')); return; }
      }
      setEditingTask(null);
      setFormData({
        title: '', description: '', cycleId: cycleIdToUse,
        projectId: cycleIdToUse === undefined && currentProject ? currentProject.id : undefined,
        status: 'BACKLOG', priority: 'MEDIUM', estimateHours: undefined, assigneeId: undefined, pairAssigneeId: undefined, dueDate: undefined, tags: '', category: activeCategory, pitchId: undefined,
      });
      setDueDate(null);
    }
    setDialogOpen(true);
  };

  const handleCloseDialog = () => { setDialogOpen(false); setEditingTask(null); setFieldErrors({}); };

  const handleAddSubTask = (parentTask: Task) => {
    setFormData({
      title: '', description: '', cycleId: parentTask.cycleId,
      // Parent tasks can be cycle-less (Shape Up Debt/Improvement backlog items) — carry the
      // project reference through so the subtask isn't left with neither.
      projectId: !parentTask.cycleId ? parentTask.projectId : undefined,
      parentTaskId: parentTask.id, pitchId: parentTask.pitchId, status: 'TODO', priority: 'MEDIUM', estimateHours: undefined, assigneeId: undefined, pairAssigneeId: undefined, dueDate: undefined, tags: '', category: parentTask.category || activeCategory,
    });
    setDueDate(null);
    setEditingTask(null);
    setDialogOpen(true);
  };

  // Deep-link from TaskDetailPage's "Add subtask" CTA: /backlog?addSubtask=<parentTaskId>
  useEffect(() => {
    const addSubtaskParam = searchParams.get('addSubtask');
    if (!addSubtaskParam) return;
    const parentTaskId = Number(addSubtaskParam);
    if (!parentTaskId) return;
    taskService.getById(parentTaskId)
      .then((r) => handleAddSubTask(r.data))
      .catch((error) => console.error('Failed to load parent task for subtask creation:', error))
      .finally(() => {
        const next = new URLSearchParams(searchParams);
        next.delete('addSubtask');
        setSearchParams(next, { replace: true });
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  const handleSort = (field: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title') => {
    if (sortBy === field) { setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc'); } else { setSortBy(field); setSortOrder('desc'); }
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
    const canSkipCycle = isStrictlyShapeUp && activeCategory === 'DEBT_IMPROVEMENT' && !!formData.projectId;
    if (!isKanbanProject && !canSkipCycle && (!formData.cycleId || formData.cycleId === 0)) {
      errors.cycleId = t('backlogPage.cycleRequired');
    }
    if (formData.estimateHours !== undefined && formData.estimateHours < 0) { errors.estimateHours = t('backlogPage.estimatePositive'); }
    if (formData.actualHours !== undefined && formData.actualHours < 0) { errors.actualHours = t('backlogPage.actualPositive'); }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSaveTask = async () => {
    if (!validateTaskForm()) return;
    try {
      setSaving(true);
      // The "Pitch Tasks" tab only makes sense for tasks actually linked to a pitch — a task
      // created there without picking a pitch is opportunistic debt/improvement work, not
      // shaped pitch scope, even though the tab's category is PITCH_SCOPE.
      const resolvedCategory: TaskCategory =
        activeCategory === 'PITCH_SCOPE' && !formData.pitchId ? 'DEBT_IMPROVEMENT' : activeCategory;
      const data = { ...formData, dueDate: dueDate ? dueDate.format('YYYY-MM-DD') : undefined, category: resolvedCategory };
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

  const handleQuickAssigneeChange = async (taskId: number, assigneeId: number | null) => {
    try { await taskService.updateAssignee(taskId, assigneeId); toast.success(t('backlogPage.assigneeUpdated')); loadTasks(); }
    catch (error: any) { toast.error(getUserFriendlyError(error)); }
  };

  /**
   * Optimistically update local task order after a drag-to-reorder.
   * Called by BacklogTaskTable before the API request is made.
   */
  const handleReorder = (reorderedTasks: Task[]) => {
    setTasks(reorderedTasks);
  };

  // Page reset is handled by the centralized effect above (all of these are in its deps).
  const handleClearFilters = () => {
    setStatusFilter([]); setPriorityFilter([]); setAssigneeFilter([]);
    setDependencyFilter('all'); setReleaseFilter(undefined); setSearchQuery('');
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

  // Kanban has no Pitch concept, so the Feature Tasks / Debt & Improvements split
  // isn't meaningful there (see the hidden category-tab UI in BacklogPage.tsx) -
  // just "Tasks", since loadTasks() above fetches every task regardless of category.
  const categoryTitle = isKanbanProject
    ? t('backlogPage.allTasks')
    : (activeCategory === 'PITCH_SCOPE' ? t('backlogPage.pitchTasks') : t('backlogPage.debtImprovements'));

  const categoryDescription = isKanbanProject
    ? t('backlogPage.categoryDescription.allTasks')
    : (activeCategory === 'PITCH_SCOPE' ? t('backlogPage.categoryDescription.pitchScope') : t('backlogPage.categoryDescription.debtImprovement'));

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
    canSkipCycleForDebtImprovement,

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
    handleQuickAssigneeChange,
    handleReorder,
    handleClearFilters,
    handleExportCsv,
    loadTasks,

    // Derived
    categoryTitle,
    categoryDescription,
  };
}
