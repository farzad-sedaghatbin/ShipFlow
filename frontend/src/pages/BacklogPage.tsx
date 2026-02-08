import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { LocalizedDateInput } from '../components/LocalizedDateInput';
import dayjs, { Dayjs } from 'dayjs';
import { toast } from 'sonner';
import { 
  Plus, 
  Trash2, 
  Pencil, 
  ArrowUp, 
  ArrowDown,
  ChevronLeft,
  ChevronRight,
  Check,
  Wrench,
  FileText,
  Loader2,
  PlayCircle,
  Eye,
  AlertCircle,
  Shield,
  List,
  Kanban,
} from 'lucide-react';
import { cn } from '../lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';

import { Checkbox } from '@/components/ui/checkbox';
import { Progress } from '@/components/ui/progress';
import { Avatar, AvatarImage, AvatarFallback } from '@/components/ui/avatar';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { 
  Table, 
  TableBody, 
  TableCell, 
  TableHead, 
  TableHeader, 
  TableRow 
} from '@/components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { taskService } from '../services/taskService';
import { cycleService } from '../services/cycleService';
import { personService } from '../services/personService';
import { pitchService } from '../services/pitchService';
import { hillChartApi } from '../services/hillChartApi';
import timerService from '../services/timerService';
import { Task, Cycle, Person, Pitch, HillChartPoint, CreateTaskRequest, TaskStatus, TaskPriority, TaskStatistics, TaskCategory } from '../types';
import EmptyState from '../components/EmptyState';
import { EmptyTasksIllustration } from '../components/illustrations';
import TimerWidget from '../components/TimerWidget';
import TaskDependencies from '../components/TaskDependencies';
import { getUserFriendlyError } from '../utils/errorMessages';
import KanbanBoard from '../components/KanbanBoard';
import { useProject, useAuth } from '../contexts';
import { BacklogSkeleton } from '../components/Skeletons';

// View mode type
type ViewMode = 'list' | 'kanban';

const statusOptions: { value: TaskStatus; labelKey: string; variant: 'default' | 'secondary' | 'destructive' | 'success' | 'warning' | 'info' | 'outline' }[] = [
  { value: 'BACKLOG', labelKey: 'backlogPage.statusOptions.backlog', variant: 'secondary' },
  { value: 'TODO', labelKey: 'backlogPage.statusOptions.todo', variant: 'info' },
  { value: 'IN_PROGRESS', labelKey: 'backlogPage.statusOptions.inProgress', variant: 'default' },
  { value: 'BLOCKED', labelKey: 'backlogPage.statusOptions.blocked', variant: 'destructive' },
  { value: 'IN_REVIEW', labelKey: 'backlogPage.statusOptions.inReview', variant: 'warning' },
  { value: 'DONE', labelKey: 'backlogPage.statusOptions.done', variant: 'success' },
  { value: 'CANCELLED', labelKey: 'backlogPage.statusOptions.cancelled', variant: 'secondary' },
];

const priorityOptions: { value: TaskPriority; labelKey: string; variant: 'default' | 'secondary' | 'destructive' | 'success' | 'warning' | 'info' | 'outline' }[] = [
  { value: 'LOW', labelKey: 'backlogPage.priorityOptions.low', variant: 'secondary' },
  { value: 'MEDIUM', labelKey: 'backlogPage.priorityOptions.medium', variant: 'info' },
  { value: 'HIGH', labelKey: 'backlogPage.priorityOptions.high', variant: 'warning' },
  { value: 'URGENT', labelKey: 'backlogPage.priorityOptions.urgent', variant: 'destructive' },
];

export default function BacklogPage() {
  const { t } = useTranslation();
  const [searchParams, setSearchParams] = useSearchParams();
  const categoryFromUrl = searchParams.get('category') as TaskCategory | null;
  const { isKanbanProject, currentProject, isSwitchingProject, notifyProjectSwitchComplete } = useProject();
  const { user } = useAuth();
  
  const [tasks, setTasks] = useState<Task[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<number | 'all'>('all');
  const [statistics, setStatistics] = useState<TaskStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [tasksLoading, setTasksLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeCategory, setActiveCategory] = useState<TaskCategory>(categoryFromUrl || 'PITCH_SCOPE');
  const [tabValue, setTabValue] = useState('all');
  const [activeTimerTaskId, setActiveTimerTaskId] = useState<number | null>(null);
  // View mode: Kanban projects default to kanban view, Shape Up defaults to list
  const [viewMode, setViewMode] = useState<ViewMode>(isKanbanProject ? 'kanban' : 'list');
  // Kanban column visibility - default to showing essential columns
  const [visibleColumns, setVisibleColumns] = useState<TaskStatus[]>(['BACKLOG', 'TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']);
  const [statusFilter, setStatusFilter] = useState<TaskStatus[]>([]);
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority[]>([]);
  const [assigneeFilter, setAssigneeFilter] = useState<number[]>([]);
  const [dependencyFilter, setDependencyFilter] = useState<'all' | 'blocked' | 'blocking'>('all');
  const [excludeMode] = useState(false);
  const [sortBy, setSortBy] = useState<'createdAt' | 'priority' | 'status' | 'dueDate' | 'title'>('createdAt');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; taskId: number | null }>({
    open: false,
    taskId: null,
  });
  const [viewDialog, setViewDialog] = useState<{ open: boolean; task: Task | null }>({
    open: false,
    task: null,
  });
  const [subtasks, setSubtasks] = useState<Task[]>([]);
  const [viewHistory, setViewHistory] = useState<Task[]>([]);

  // Dialog state
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [formData, setFormData] = useState<CreateTaskRequest>({
    title: '',
    description: '',
    cycleId: 0,
    status: 'BACKLOG',
    priority: 'MEDIUM',
    estimateHours: undefined,
    assigneeId: undefined,
    pairAssigneeId: undefined,
    dueDate: undefined,
    tags: '',
    category: activeCategory,
    pitchId: undefined,
    scopeId: undefined,
  });
  const [dueDate, setDueDate] = useState<Dayjs | null>(null);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [scopes, setScopes] = useState<HillChartPoint[]>([]);

  // Multi-select dropdown states
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const [priorityDropdownOpen, setPriorityDropdownOpen] = useState(false);
  const [dependencyDropdownOpen, setDependencyDropdownOpen] = useState(false);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_assigneeDropdownOpen, _setAssigneeDropdownOpen] = useState(false);

  // Sync view mode when project type changes
  useEffect(() => {
    // Set default view mode based on project type
    setViewMode(isKanbanProject ? 'kanban' : 'list');
  }, [isKanbanProject]);

  useEffect(() => {
    loadInitialData();
    loadActiveTimer();
  }, []);

  // Re-load when project changes (for Kanban auto-select)
  useEffect(() => {
    if (currentProject) {
      loadInitialData();
    }
  }, [currentProject?.id, isKanbanProject]);

  useEffect(() => {
    // Load pitches when dialog opens
    if (dialogOpen) {
      if (formData.cycleId && formData.cycleId !== 0) {
        loadPitchesForCycle(formData.cycleId);
      } else {
        loadAllPitches();
      }
    }
  }, [dialogOpen, formData.cycleId]);

  useEffect(() => {
    // For Kanban projects, load tasks by project (no cycle needed)
    if (isKanbanProject && currentProject) {
      loadTasks();
      loadStatistics();
    } else if (selectedCycle) {
      // For Shape Up projects, load tasks by cycle
      loadTasks();
      loadStatistics();
    } else {
      // No cycle selected - ensure clean state
      setTasks([]);
      setTotalElements(0);
      setTasksLoading(false);
      setStatistics(null);
    }
  }, [selectedCycle, currentProject?.id, isKanbanProject, activeCategory, tabValue, statusFilter, priorityFilter, assigneeFilter, excludeMode, page, rowsPerPage, sortBy, sortOrder, dependencyFilter]);

  // Sync URL param to state when URL changes (e.g., browser back/forward)
  useEffect(() => {
    if (categoryFromUrl && categoryFromUrl !== activeCategory) {
      setActiveCategory(categoryFromUrl);
    }
  }, [categoryFromUrl]);

  const loadInitialData = async () => {
    try {
      const [cyclesRes, personsRes] = await Promise.all([
        cycleService.getMyActiveCycles(),
        personService.getAll(),
      ]);
      setCycles(cyclesRes.data);
      setPersons(personsRes);
      
      // Auto-select the first cycle of current project
      // (applies to both Kanban and Shape Up project types)
      if (currentProject) {
        // Filter cycles to only those belonging to the current project
        const projectCycles = cyclesRes.data.filter(c => c.projectId === currentProject.id);
        if (projectCycles.length > 0) {
          setSelectedCycle(projectCycles[0].id);
        } else {
          // No cycles for this project, default to 'all'
          setSelectedCycle('all');
        }
      } else {
        // No project selected (All Projects view), default to 'all'
        setSelectedCycle('all');
      }
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
      notifyProjectSwitchComplete();
    }
  };

  const loadActiveTimer = async () => {
    try {
      const timer = await timerService.getActiveTimer();
      if (timer && timer.taskId) {
        setActiveTimerTaskId(timer.taskId);
      } else {
        setActiveTimerTaskId(null);
      }
    } catch (error) {
      console.error('Failed to load active timer:', error);
    }
  };

  const loadTasks = async () => {
    // For Kanban projects, load tasks by project, not by cycle
    if (isKanbanProject && currentProject) {
      setTasksLoading(true);
      const timeout = setTimeout(() => setTasksLoading(false), 10000);
      
      try {
        const response = await taskService.getByProjectIdAndCategory(
          currentProject.id, 
          activeCategory, 
          page, 
          rowsPerPage, 
          sortBy, 
          sortOrder
        );
        let filteredTasks = response?.data?.content || [];
        
        // Apply additional filters
        if (statusFilter.length > 0) {
          filteredTasks = filteredTasks.filter((t: Task) => 
            excludeMode ? !statusFilter.includes(t.status) : statusFilter.includes(t.status)
          );
        }
        if (priorityFilter.length > 0) {
          filteredTasks = filteredTasks.filter((t: Task) => 
            excludeMode ? !priorityFilter.includes(t.priority) : priorityFilter.includes(t.priority)
          );
        }
        if (assigneeFilter.length > 0) {
          filteredTasks = filteredTasks.filter((t: Task) => 
            excludeMode ? !assigneeFilter.includes(t.assigneeId || 0) : assigneeFilter.includes(t.assigneeId || 0)
          );
        }
        if (dependencyFilter === 'blocked') {
          filteredTasks = filteredTasks.filter((t: Task) => t.isBlocked && t.blockedByCount && t.blockedByCount > 0);
        } else if (dependencyFilter === 'blocking') {
          filteredTasks = filteredTasks.filter((t: Task) => t.blockingTasks && t.blockingTasks.length > 0);
        }
        
        // Filter by tab (my tasks)
        if (tabValue === 'my' && user?.personId) {
          filteredTasks = filteredTasks.filter((t: Task) => t.assigneeId === user.personId);
        }
        
        setTasks(filteredTasks);
        setTotalElements(response?.data?.totalElements || 0);
      } catch (error) {
        console.error('Failed to load project tasks:', error);
        setTasks([]);
        setTotalElements(0);
      } finally {
        clearTimeout(timeout);
        setTasksLoading(false);
      }
      return;
    }
    
    // For Shape Up projects, use cycle-based loading
    if (!selectedCycle) {
      setTasks([]);
      setTotalElements(0);
      setTasksLoading(false);
      return;
    }
    setTasksLoading(true);
    
    // Failsafe timeout to ensure loading state doesn't get stuck
    const timeout = setTimeout(() => {
      setTasksLoading(false);
    }, 10000);
    
    try {
      let response: any;
      if (tabValue === 'my') {
        if (selectedCycle === 'all') {
          // Get all my tasks with server-side pagination and sorting
          response = await taskService.getMy(page, rowsPerPage, sortBy, sortOrder);
          const allTasks = response?.data?.content || [];
          // Filter by category
          const filteredTasks = allTasks.filter((task: Task) => {
            const taskCategory = task.category || 'PITCH_SCOPE';
            return taskCategory === activeCategory;
          });
          setTasks(filteredTasks);
          setTotalElements(response?.data?.totalElements || 0);
        } else {
          response = await taskService.getMyByCycle(selectedCycle, page, rowsPerPage, sortBy, sortOrder);
          // Client-side filter for my tasks until backend supports it
          const allTasks = response?.data?.content || [];
          const filteredTasks = allTasks.filter((task: Task) => {
            const taskCategory = task.category || 'PITCH_SCOPE';
            return taskCategory === activeCategory;
          });
          setTasks(filteredTasks);
          setTotalElements(filteredTasks.length);
        }
      } else if (selectedCycle === 'all') {
        // Get all tasks with server-side pagination and sorting
        response = await taskService.getAll(page, rowsPerPage, sortBy, sortOrder);
        const allTasks = response?.data?.content || [];
        
        // Filter by category
        let filteredTasks = allTasks.filter((task: Task) => {
          const taskCategory = task.category || 'PITCH_SCOPE';
          return taskCategory === activeCategory;
        });
        
        // Apply additional filters manually
        if (statusFilter.length > 0) {
          filteredTasks = filteredTasks.filter((t: Task) => 
            excludeMode ? !statusFilter.includes(t.status) : statusFilter.includes(t.status)
          );
        }
        if (priorityFilter.length > 0) {
          filteredTasks = filteredTasks.filter((t: Task) => 
            excludeMode ? !priorityFilter.includes(t.priority) : priorityFilter.includes(t.priority)
          );
        }
        if (assigneeFilter.length > 0) {
          filteredTasks = filteredTasks.filter((t: Task) => 
            excludeMode ? !assigneeFilter.includes(t.assigneeId || 0) : assigneeFilter.includes(t.assigneeId || 0)
          );
        }
        
        // Apply dependency filter
        if (dependencyFilter === 'blocked') {
          filteredTasks = filteredTasks.filter((t: Task) => t.isBlocked && t.blockedByCount && t.blockedByCount > 0);
        } else if (dependencyFilter === 'blocking') {
          filteredTasks = filteredTasks.filter((t: Task) => t.blockingTasks && t.blockingTasks.length > 0);
        }
        
        setTasks(filteredTasks);
        setTotalElements(response?.data?.totalElements || 0);
      } else if (statusFilter.length > 0 || priorityFilter.length > 0 || assigneeFilter.length > 0) {
        // Use filter endpoint with category
        response = await taskService.getWithFilters(
          selectedCycle,
          statusFilter.length > 0 ? statusFilter : undefined,
          priorityFilter.length > 0 ? priorityFilter : undefined,
          assigneeFilter.length > 0 ? assigneeFilter : undefined,
          activeCategory,
          excludeMode,
          page,
          rowsPerPage,
          sortBy,
          sortOrder
        );
        let filteredTasks = response?.data?.content || [];
        
        // Apply dependency filter
        if (dependencyFilter === 'blocked') {
          filteredTasks = filteredTasks.filter((t: Task) => t.isBlocked && t.blockedByCount && t.blockedByCount > 0);
        } else if (dependencyFilter === 'blocking') {
          filteredTasks = filteredTasks.filter((t: Task) => t.blockingTasks && t.blockingTasks.length > 0);
        }
        
        setTasks(filteredTasks);
        setTotalElements(response?.data?.totalElements || 0);
      } else {
        // Use category-specific endpoint
        response = await taskService.getByCycleIdAndCategory(selectedCycle, activeCategory, page, rowsPerPage, sortBy, sortOrder);
        let filteredTasks = response?.data?.content || [];
        
        // Apply dependency filter
        if (dependencyFilter === 'blocked') {
          filteredTasks = filteredTasks.filter((t: Task) => t.isBlocked && t.blockedByCount && t.blockedByCount > 0);
        } else if (dependencyFilter === 'blocking') {
          filteredTasks = filteredTasks.filter((t: Task) => t.blockingTasks && t.blockingTasks.length > 0);
        }
        
        setTasks(filteredTasks);
        setTotalElements(response?.data?.totalElements || 0);
      }
    } catch (error) {
      console.error('Failed to load tasks:', error);
      setTasks([]);
      setTotalElements(0);
    } finally {
      clearTimeout(timeout);
      setTasksLoading(false);
    }
  };

  const loadStatistics = async () => {
    // For Kanban projects, load project-based statistics
    if (isKanbanProject && currentProject) {
      try {
        const response = await taskService.getStatisticsByProjectId(currentProject.id);
        setStatistics(response.data);
      } catch (error) {
        console.error('Failed to load project statistics:', error);
      }
      return;
    }
    
    // For Shape Up, load cycle-based statistics
    if (!selectedCycle || selectedCycle === 'all') return;
    try {
      const response = await taskService.getStatisticsByCycleId(selectedCycle);
      setStatistics(response.data);
    } catch (error) {
      console.error('Failed to load statistics:', error);
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

  const loadAllPitches = async () => {
    try {
      const response = await pitchService.getMyPitches();
      setPitches(response.data);
    } catch (error) {
      console.error('Failed to load pitches:', error);
      setPitches([]);
    }
  };

  const loadScopesForPitch = async (pitchId: number) => {
    try {
      const response = await hillChartApi.getHillChartPointsByPitch(pitchId);
      setScopes(response);
    } catch (error) {
      console.error('Failed to load scopes:', error);
      setScopes([]);
    }
  };

  const handlePitchChange = (pitchId: string) => {
    const pitch = pitchId === 'none' ? undefined : Number(pitchId);
    setFormData({ ...formData, pitchId: pitch, scopeId: undefined });
    setScopes([]);
    if (pitch) {
      loadScopesForPitch(pitch);
    }
  };

  const handleCategoryChange = (category: TaskCategory) => {
    setActiveCategory(category);
    setSearchParams({ category });
    setPage(0);
  };

  const handleOpenDialog = (task?: Task) => {
    if (task) {
      setEditingTask(task);
      setFormData({
        title: task.title,
        description: task.description || '',
        cycleId: task.cycleId,
        status: task.status,
        priority: task.priority,
        estimateHours: task.estimateHours,
        actualHours: task.actualHours,
        assigneeId: task.assigneeId,
        pairAssigneeId: task.pairAssigneeId,
        dueDate: task.dueDate,
        tags: task.tags || '',
        category: task.category || activeCategory,
      });
      setDueDate(task.dueDate ? dayjs(task.dueDate) : null);
    } else {
      // For Kanban projects, use the auto-selected cycle or find the project's default cycle
      // For Shape Up, require a specific cycle to be selected
      if (!isKanbanProject && (!selectedCycle || selectedCycle === 'all')) {
        toast.error(t('backlogPage.selectCycleToCreate'));
        return;
      }
      
      // Get the cycle ID to use
      let cycleIdToUse = typeof selectedCycle === 'number' ? selectedCycle : 0;
      
      // For Kanban projects, if no cycle is selected yet, find the project's default cycle
      if (isKanbanProject && currentProject && cycleIdToUse === 0) {
        const projectCycles = cycles.filter(c => c.projectId === currentProject.id);
        if (projectCycles.length > 0) {
          cycleIdToUse = projectCycles[0].id;
        } else {
          toast.error(t('backlogPage.noDefaultCycle'));
          return;
        }
      }
      
      setEditingTask(null);
      setFormData({
        title: '',
        description: '',
        cycleId: cycleIdToUse,
        status: 'BACKLOG',
        priority: 'MEDIUM',
        estimateHours: undefined,
        assigneeId: undefined,
        pairAssigneeId: undefined,
        dueDate: undefined,
        tags: '',
        category: activeCategory,
      });
      setDueDate(null);
    }
    setDialogOpen(true);
  };

  const handleSort = (field: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title') => {
    if (sortBy === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(field);
      setSortOrder('desc');
    }
    setPage(0);
  };

  const handleChangePage = (newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (value: string) => {
    setRowsPerPage(parseInt(value, 10));
    setPage(0);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingTask(null);
    setFieldErrors({});
  };

  const handleAddSubTask = (parentTask: Task) => {
    setFormData({
      title: '',
      description: '',
      cycleId: parentTask.cycleId, // Use parent task's cycle ID
      parentTaskId: parentTask.id,
      status: 'BACKLOG',
      priority: 'MEDIUM',
      estimateHours: undefined,
      assigneeId: undefined,
      pairAssigneeId: undefined,
      dueDate: undefined,
      tags: '',
      category: activeCategory,
    });
    setDueDate(null);
    setEditingTask(null);
    setDialogOpen(true);
  };

  const handleStartTimer = async (task: Task) => {
    try {
      await timerService.startTimer({
        taskId: task.id,
        note: `Working on: ${task.title}`,
      });
      setActiveTimerTaskId(task.id);
      // Reload active timer to ensure TimerWidget shows it
      await loadActiveTimer();
      toast.success(t('backlogPage.timerStarted'));
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to start timer';
      toast.error(message);
    }
  };

  const handleTimerStopped = () => {
    // Timer stopped - reload data and clear active timer
    setActiveTimerTaskId(null);
    loadTasks();
    // For Kanban projects, always load statistics (they're project-based)
    // For Shape Up, only load if a specific cycle is selected
    if (isKanbanProject || selectedCycle !== 'all') {
      loadStatistics();
    }
  };

  const handleViewTask = async (task: Task, addToHistory = true) => {
    if (addToHistory && viewDialog.task) {
      setViewHistory([...viewHistory, viewDialog.task]);
    }
    setViewDialog({ open: true, task });
    // Load subtasks if task has any
    try {
      const response = await taskService.getSubTasks(task.id);
      setSubtasks(response.data);
    } catch (error) {
      console.error('Failed to load subtasks:', error);
      setSubtasks([]);
    }
  };

  const handleViewBack = () => {
    if (viewHistory.length > 0) {
      const previousTask = viewHistory[viewHistory.length - 1];
      setViewHistory(viewHistory.slice(0, -1));
      handleViewTask(previousTask, false);
    }
  };

  const handleCloseViewDialog = () => {
    setViewDialog({ open: false, task: null });
    setViewHistory([]);
  };

  const validateTaskForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.title.trim()) {
      errors.title = t('backlogPage.titleRequired');
    } else if (formData.title.trim().length < 3) {
      errors.title = t('backlogPage.titleMinLength');
    }

    // Skip cycle validation for Kanban projects (cycle is auto-selected)
    if (!isKanbanProject && (!formData.cycleId || formData.cycleId === 0)) {
      errors.cycleId = t('backlogPage.cycleRequired');
    }

    if (formData.estimateHours !== undefined && formData.estimateHours < 0) {
      errors.estimateHours = t('backlogPage.estimatePositive');
    }

    if (formData.actualHours !== undefined && formData.actualHours < 0) {
      errors.actualHours = t('backlogPage.actualPositive');
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSaveTask = async () => {
    if (!validateTaskForm()) {
      return;
    }

    try {
      setSaving(true);
      const data = {
        ...formData,
        dueDate: dueDate ? dueDate.format('YYYY-MM-DD') : undefined,
        category: activeCategory,
      };
      
      if (editingTask) {
        await taskService.update(editingTask.id, data);
        toast.success(t('backlogPage.taskUpdated'));
      } else {
        await taskService.create(data);
        toast.success(t('backlogPage.taskCreated'));
      }
      handleCloseDialog();
      loadTasks();
      loadStatistics();
    } catch (error: any) {
      const message = getUserFriendlyError(error);
      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteTask = async () => {
    if (!deleteDialog.taskId) return;
    try {
      await taskService.delete(deleteDialog.taskId);
      toast.success(t('backlogPage.taskDeleted'));
      setDeleteDialog({ open: false, taskId: null });
      loadTasks();
      loadStatistics();
    } catch (error: any) {
      const message = getUserFriendlyError(error);
      toast.error(message);
    }
  };

  const handleQuickStatusChange = async (taskId: number, newStatus: TaskStatus) => {
    try {
      await taskService.updateStatus(taskId, newStatus);
      toast.success(t('backlogPage.statusUpdated'));
      loadTasks();
      loadStatistics();
    } catch (error: any) {
      const message = getUserFriendlyError(error);
      toast.error(message);
    }
  };

  const handleQuickPriorityChange = async (taskId: number, newPriority: TaskPriority) => {
    try {
      await taskService.updatePriority(taskId, newPriority);
      toast.success(t('backlogPage.priorityUpdated'));
      loadTasks();
      loadStatistics();
    } catch (error: any) {
      const message = getUserFriendlyError(error);
      toast.error(message);
    }
  };

  const handleToggleColumn = (status: TaskStatus) => {
    setVisibleColumns(prev => {
      const isVisible = prev.includes(status);
      if (isVisible) {
        // Don't allow hiding all columns, keep at least one
        if (prev.length === 1) return prev;
        return prev.filter(s => s !== status);
      } else {
        return [...prev, status];
      }
    });
  };

  const getStatusBadgeVariant = (status: TaskStatus) => {
    return statusOptions.find(s => s.value === status)?.variant || 'secondary';
  };

  const getPriorityBadgeVariant = (priority: TaskPriority) => {
    return priorityOptions.find(p => p.value === priority)?.variant || 'secondary';
  };

  const totalPages = Math.ceil(totalElements / rowsPerPage);

  // Use different labels for Kanban vs Shape Up projects
  const categoryTitle = activeCategory === 'PITCH_SCOPE' 
    ? (isKanbanProject ? t('backlogPage.featureTasks') : t('backlogPage.pitchTasks'))
    : t('backlogPage.debtImprovements');
  const categoryDescription = activeCategory === 'PITCH_SCOPE' 
    ? (isKanbanProject ? t('backlogPage.categoryDescription.featureScope') : t('backlogPage.categoryDescription.pitchScope'))
    : t('backlogPage.categoryDescription.debtImprovement');

  if (loading || isSwitchingProject) {
    return <BacklogSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Timer Widget */}
      <TimerWidget onTimerStopped={handleTimerStopped} />
      
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">{t('backlogPage.title')}</h1>
          <p className="text-muted-foreground">{categoryDescription}</p>
        </div>
        <div className="flex items-center gap-2">
          {/* Cycle Selector - Hidden for Kanban projects */}
          {!isKanbanProject && (
            <Select
              value={selectedCycle === 'all' ? 'all' : selectedCycle?.toString() || ''}
              onValueChange={(value) => setSelectedCycle(value === 'all' ? 'all' : Number(value))}
            >
              <SelectTrigger className="w-[250px]">
                <SelectValue placeholder={t('backlogPage.selectCycle')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('backlogPage.allCycles')}</SelectItem>
                
                {/* Current Project Cycles */}
                {currentProject && cycles.filter(c => c.projectId === currentProject.id).length > 0 && (
                  <>
                    <div className="px-2 py-1.5 text-xs font-semibold text-muted-foreground">
                      {currentProject.name}
                    </div>
                    {cycles
                      .filter(c => c.projectId === currentProject.id)
                      .map((cycle) => (
                        <SelectItem key={cycle.id} value={cycle.id.toString()}>
                          {cycle.name}
                        </SelectItem>
                      ))}
                  </>
                )}
                
                {/* Other Projects Cycles */}
                {cycles.filter(c => !currentProject || c.projectId !== currentProject.id).length > 0 && (
                  <>
                    <div className="px-2 py-1.5 text-xs font-semibold text-muted-foreground">
                      {t('backlogPage.otherProjects')}
                    </div>
                    {cycles
                      .filter(c => !currentProject || c.projectId !== currentProject.id)
                      .map((cycle) => (
                        <SelectItem key={cycle.id} value={cycle.id.toString()}>
                          {cycle.name}
                        </SelectItem>
                      ))}
                  </>
                )}
              </SelectContent>
            </Select>
          )}
          
          {/* View Mode Toggle */}
          <div className="flex items-center border rounded-md">
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant={viewMode === 'list' ? 'default' : 'ghost'}
                    size="sm"
                    onClick={() => setViewMode('list')}
                    className="rounded-r-none"
                  >
                    <List className="h-4 w-4" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('backlogPage.viewMode.list')}</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button 
                    variant={viewMode === 'kanban' ? 'default' : 'ghost'}
                    size="sm"
                    onClick={() => setViewMode('kanban')}
                    className="rounded-l-none"
                  >
                    <Kanban className="h-4 w-4" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('backlogPage.viewMode.kanban')}</TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </div>

          {/* New Task Button - For Kanban, always enabled; for Shape Up, requires cycle selection */}
          {isKanbanProject ? (
            <Button onClick={() => handleOpenDialog()}>
              <Plus className="mr-2 h-4 w-4" />
              {t('backlogPage.newTask')}
            </Button>
          ) : (
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <span>
                    <Button 
                      onClick={() => handleOpenDialog()} 
                      disabled={selectedCycle === 'all' || !selectedCycle}
                    >
                      <Plus className="mr-2 h-4 w-4" />
                      {t('backlogPage.newTask')}
                    </Button>
                  </span>
                </TooltipTrigger>
                {(selectedCycle === 'all' || !selectedCycle) && (
                  <TooltipContent>
                    <p>{t('backlogPage.selectCycleToCreate')}</p>
                  </TooltipContent>
                )}
              </Tooltip>
            </TooltipProvider>
          )}
        </div>
      </div>

      {/* Category Tabs */}
      <Tabs value={activeCategory} onValueChange={(v) => handleCategoryChange(v as TaskCategory)} className="w-full">
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="PITCH_SCOPE" className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            {isKanbanProject ? t('backlogPage.featureTasks') : t('backlogPage.pitchTasks')}
          </TabsTrigger>
          <TabsTrigger value="DEBT_IMPROVEMENT" className="flex items-center gap-2">
            <Wrench className="h-4 w-4" />
            {t('backlogPage.debtImprovements')}
          </TabsTrigger>
        </TabsList>
      </Tabs>

      {/* Statistics Card */}
      {statistics && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">{t('backlogPage.taskOverview')}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
              <div className="text-center">
                <div className="text-2xl font-bold">{statistics.totalTasks}</div>
                <div className="text-xs text-muted-foreground">{t('backlogPage.total')}</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-blue-500">{statistics.todoTasks}</div>
                <div className="text-xs text-muted-foreground">{t('backlogPage.todo')}</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-purple-500">{statistics.inProgressTasks}</div>
                <div className="text-xs text-muted-foreground">{t('backlogPage.inProgress')}</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-red-500">{statistics.blockedTasks}</div>
                <div className="text-xs text-muted-foreground">{t('backlogPage.blocked')}</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-yellow-500">{statistics.inReviewTasks}</div>
                <div className="text-xs text-muted-foreground">{t('backlogPage.inReview')}</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-500">{statistics.doneTasks}</div>
                <div className="text-xs text-muted-foreground">{t('backlogPage.done')}</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold">{statistics.completionPercentage}%</div>
                <div className="text-xs text-muted-foreground">{t('backlogPage.complete')}</div>
              </div>
            </div>
            <Progress value={statistics.completionPercentage} className="mt-4" />
          </CardContent>
        </Card>
      )}

      {/* View Tabs */}
      <Tabs value={tabValue} onValueChange={setTabValue} className="w-full">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between mb-4">
          <TabsList>
            <TabsTrigger value="all">{t('backlogPage.allTasks')}</TabsTrigger>
            <TabsTrigger value="my">{t('backlogPage.myTasks')}</TabsTrigger>
          </TabsList>

          {/* Filters */}
          <div className="flex items-center gap-2 flex-wrap">
            {/* Status Filter */}
            <DropdownMenu open={statusDropdownOpen} onOpenChange={setStatusDropdownOpen}>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm">
                  {t('backlogPage.filters.status')} {statusFilter.length > 0 && `(${statusFilter.length})`}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                {statusOptions.map((status) => (
                  <DropdownMenuItem
                    key={status.value}
                    onSelect={(e) => {
                      e.preventDefault();
                      if (statusFilter.includes(status.value)) {
                        setStatusFilter(statusFilter.filter(s => s !== status.value));
                      } else {
                        setStatusFilter([...statusFilter, status.value]);
                      }
                    }}
                  >
                    <Checkbox
                      checked={statusFilter.includes(status.value)}
                      className="mr-2"
                    />
                    {t(status.labelKey)}
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>

            {/* Priority Filter */}
            <DropdownMenu open={priorityDropdownOpen} onOpenChange={setPriorityDropdownOpen}>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm">
                  {t('backlogPage.filters.priority')} {priorityFilter.length > 0 && `(${priorityFilter.length})`}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                {priorityOptions.map((priority) => (
                  <DropdownMenuItem
                    key={priority.value}
                    onSelect={(e) => {
                      e.preventDefault();
                      if (priorityFilter.includes(priority.value)) {
                        setPriorityFilter(priorityFilter.filter(p => p !== priority.value));
                      } else {
                        setPriorityFilter([...priorityFilter, priority.value]);
                      }
                    }}
                  >
                    <Checkbox
                      checked={priorityFilter.includes(priority.value)}
                      className="mr-2"
                    />
                    {t(priority.labelKey)}
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>

            {/* Dependency Filter */}
            <DropdownMenu open={dependencyDropdownOpen} onOpenChange={setDependencyDropdownOpen}>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm">
                  {t('backlogPage.filters.dependencies')} {dependencyFilter !== 'all' && `(${dependencyFilter})`}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-48">
                <DropdownMenuItem
                  onSelect={() => setDependencyFilter('all')}
                >
                  <Checkbox
                    checked={dependencyFilter === 'all'}
                    className="mr-2"
                  />
                  {t('backlogPage.filters.allTasks')}
                </DropdownMenuItem>
                <DropdownMenuItem
                  onSelect={() => setDependencyFilter('blocked')}
                >
                  <Checkbox
                    checked={dependencyFilter === 'blocked'}
                    className="mr-2"
                  />
                  {t('backlogPage.filters.blockedTasks')}
                </DropdownMenuItem>
                <DropdownMenuItem
                  onSelect={() => setDependencyFilter('blocking')}
                >
                  <Checkbox
                    checked={dependencyFilter === 'blocking'}
                    className="mr-2"
                  />
                  {t('backlogPage.filters.blockingTasks')}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>

            {(statusFilter.length > 0 || priorityFilter.length > 0 || assigneeFilter.length > 0 || dependencyFilter !== 'all') && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setStatusFilter([]);
                  setPriorityFilter([]);
                  setAssigneeFilter([]);
                  setDependencyFilter('all');
                }}
              >
                {t('backlogPage.filters.clearFilters')}
              </Button>
            )}
          </div>
        </div>

        <TabsContent value="all" className="mt-0">
          {viewMode === 'kanban' ? (
            <KanbanBoard
              tasks={tasks}
              onStatusChange={handleQuickStatusChange}
              onViewTask={(task) => setViewDialog({ open: true, task })}
              onEditTask={(task) => handleOpenDialog(task)}
              onDeleteTask={(taskId) => setDeleteDialog({ open: true, taskId })}
              onAddSubtask={handleAddSubTask}
              onStartTimer={handleStartTimer}
              loading={tasksLoading}
              visibleColumns={visibleColumns}
              onToggleColumn={handleToggleColumn}
            />
          ) : (
            <TaskTable />
          )}
        </TabsContent>
        <TabsContent value="my" className="mt-0">
          {viewMode === 'kanban' ? (
            <KanbanBoard
              tasks={tasks}
              onStatusChange={handleQuickStatusChange}
              onViewTask={(task) => setViewDialog({ open: true, task })}
              onEditTask={(task) => handleOpenDialog(task)}
              onDeleteTask={(taskId) => setDeleteDialog({ open: true, taskId })}
              onAddSubtask={handleAddSubTask}
              onStartTimer={handleStartTimer}
              loading={tasksLoading}
              visibleColumns={visibleColumns}
              onToggleColumn={handleToggleColumn}
            />
          ) : (
            <TaskTable />
          )}
        </TabsContent>
      </Tabs>

      {/* Task Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingTask 
                ? (formData.parentTaskId ? t('backlogPage.editSubtask') : t('backlogPage.editTask'))
                : formData.parentTaskId 
                  ? t('backlogPage.createSubtask')
                  : t('backlogPage.createTask')
              }
            </DialogTitle>
            <DialogDescription>
              {formData.parentTaskId
                ? t('backlogPage.subtaskDescription')
                : activeCategory === 'PITCH_SCOPE' 
                  ? t('backlogPage.categoryDescription.pitchScope')
                  : t('backlogPage.categoryDescription.debtImprovement')
              }
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="title">{t('backlogPage.title')} *</Label>
              <Input
                id="title"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                placeholder={formData.parentTaskId ? t('backlogPage.subtaskTitle') : t('backlogPage.taskTitle')}
                className={fieldErrors.title ? 'border-destructive' : ''}
              />
              {fieldErrors.title && (
                <p className="text-sm text-destructive">{fieldErrors.title}</p>
              )}
            </div>
            <div className="grid gap-2">
              <Label htmlFor="description">{t('backlogPage.description')}</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder={formData.parentTaskId ? t('backlogPage.subtaskDescription') : t('backlogPage.taskDescription')}
                rows={3}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="status">{t('common.status')}</Label>
                <Select
                  value={formData.status}
                  onValueChange={(value) => setFormData({ ...formData, status: value as TaskStatus })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {statusOptions.map((status) => (
                      <SelectItem key={status.value} value={status.value}>
                        {t(status.labelKey)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="priority">{t('backlogPage.filters.priority')}</Label>
                <Select
                  value={formData.priority}
                  onValueChange={(value) => setFormData({ ...formData, priority: value as TaskPriority })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {priorityOptions.map((priority) => (
                      <SelectItem key={priority.value} value={priority.value}>
                        {t(priority.labelKey)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="assignee">{t('backlogPage.assignee')}</Label>
                <Select
                  value={formData.assigneeId?.toString() || 'unassigned'}
                  onValueChange={(value) => setFormData({ ...formData, assigneeId: value === 'unassigned' ? undefined : Number(value) })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('backlogPage.selectAssignee')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="unassigned">{t('backlogPage.unassigned')}</SelectItem>
                    {persons.map((person) => (
                      <SelectItem key={person.id} value={person.id.toString()}>
                        {person.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="pairAssignee">{t('backlogPage.pairAssignee')}</Label>
                <Select
                  value={formData.pairAssigneeId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ ...formData, pairAssigneeId: value === 'none' ? undefined : Number(value) })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('backlogPage.selectPair')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">{t('backlogPage.none')}</SelectItem>
                    {persons.filter(p => p.id !== formData.assigneeId).map((person) => (
                      <SelectItem key={person.id} value={person.id.toString()}>
                        {person.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="estimateHours">{t('backlogPage.estimateHours')}</Label>
                <Input
                  id="estimateHours"
                  type="number"
                  min="0"
                  step="0.5"
                  value={formData.estimateHours || ''}
                  onChange={(e) => setFormData({ ...formData, estimateHours: e.target.value ? Number(e.target.value) : undefined })}
                  className={fieldErrors.estimateHours ? 'border-destructive' : ''}
                />
                {fieldErrors.estimateHours && (
                  <p className="text-sm text-destructive">{fieldErrors.estimateHours}</p>
                )}
              </div>
              <div className="grid gap-2">
                <Label htmlFor="dueDate">{t('backlogPage.dueDate')}</Label>
                <LocalizedDateInput
                  id="dueDate"
                  value={dueDate ? dueDate.format('YYYY-MM-DD') : ''}
                  onChange={(val) => setDueDate(val ? dayjs(val) : null)}
                />
              </div>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="tags">{t('backlogPage.tags')}</Label>
              <Input
                id="tags"
                value={formData.tags}
                onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                placeholder={t('backlogPage.commaSeparated')}
              />
            </div>
            {/* Hide pitch and scope fields for Kanban projects */}
            {!isKanbanProject && (
              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <Label>{t('backlogPage.pitch')}</Label>
                  <Select
                    value={formData.pitchId ? String(formData.pitchId) : 'none'}
                    onValueChange={handlePitchChange}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={t('backlogPage.noPitch')} />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">{t('backlogPage.noPitch')}</SelectItem>
                      {pitches.map((pitch) => (
                        <SelectItem key={pitch.id} value={String(pitch.id)}>
                          {pitch.title}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="grid gap-2">
                  <Label>{t('backlogPage.scope')}</Label>
                  <Select
                    value={formData.scopeId ? String(formData.scopeId) : 'none'}
                    onValueChange={(value) => setFormData({ ...formData, scopeId: value === 'none' ? undefined : Number(value) })}
                    disabled={!formData.pitchId || scopes.length === 0}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={!formData.pitchId ? t('backlogPage.selectPitchFirst') : t('backlogPage.noSpecificScope')} />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">{t('backlogPage.noSpecificScope')}</SelectItem>
                      {scopes.map((scope) => (
                        <SelectItem key={scope.id} value={String(scope.id)}>
                          {scope.scope}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={handleCloseDialog}>
              {t('backlogPage.cancel')}
            </Button>
            <Button onClick={handleSaveTask} disabled={saving}>
              {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {editingTask ? t('backlogPage.update') : t('backlogPage.create')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* View Task Details Dialog */}
      <Dialog open={viewDialog.open} onOpenChange={(open) => !open && handleCloseViewDialog()}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {viewHistory.length > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleViewBack}
                  className="-ml-2"
                >
                  <ChevronLeft className="h-4 w-4" />
                  {t('backlogPage.back')}
                </Button>
              )}
              {viewDialog.task?.parentTaskId && viewHistory.length === 0 && (
                <span className="text-muted-foreground">└─</span>
              )}
              {viewDialog.task?.title}
            </DialogTitle>
          </DialogHeader>
          {viewDialog.task && (
            <div className="space-y-6">
              {/* Task Metadata */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label className="text-xs text-muted-foreground">{t('common.status')}</Label>
                  <div className="mt-1">
                    <Badge variant={statusOptions.find(s => s.value === viewDialog.task?.status)?.variant}>
                      {t(statusOptions.find(s => s.value === viewDialog.task?.status)?.labelKey || 'backlogPage.statusOptions.backlog')}
                    </Badge>
                  </div>
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">{t('backlogPage.filters.priority')}</Label>
                  <div className="mt-1">
                    <Badge variant={priorityOptions.find(p => p.value === viewDialog.task?.priority)?.variant}>
                      {t(priorityOptions.find(p => p.value === viewDialog.task?.priority)?.labelKey || 'backlogPage.priorityOptions.medium')}
                    </Badge>
                  </div>
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">{t('backlogPage.assignee')}</Label>
                  <div className="mt-1 font-medium">
                    {viewDialog.task.assigneeName || t('backlogPage.unassigned')}
                  </div>
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">{t('backlogPage.pairAssignee')}</Label>
                  <div className="mt-1 font-medium">
                    {viewDialog.task.pairAssigneeName || t('backlogPage.none')}
                  </div>
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">{t('backlogPage.estimate')}</Label>
                  <div className="mt-1 font-medium">
                    {viewDialog.task.estimateHours ? `${viewDialog.task.estimateHours}h` : '-'}
                  </div>
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">Actual</Label>
                  <div className="mt-1 font-medium">
                    {viewDialog.task.actualHours ? `${viewDialog.task.actualHours}h` : '-'}
                  </div>
                </div>
                {viewDialog.task.dueDate && (
                  <div>
                    <Label className="text-xs text-muted-foreground">{t('backlogPage.dueDate')}</Label>
                    <div className="mt-1 font-medium">
                      {dayjs(viewDialog.task.dueDate).format('MMM D, YYYY')}
                    </div>
                  </div>
                )}
                {viewDialog.task.parentTaskTitle && (
                  <div>
                    <Label className="text-xs text-muted-foreground">{t('backlogPage.parentTask')}</Label>
                    <div className="mt-1 font-medium">
                      {viewDialog.task.parentTaskTitle}
                    </div>
                  </div>
                )}
              </div>

              {/* Description */}
              {viewDialog.task.description && (
                <div>
                  <Label className="text-xs text-muted-foreground">{t('backlogPage.description')}</Label>
                  <div className="mt-2 p-3 bg-muted rounded-md text-sm whitespace-pre-wrap">
                    {viewDialog.task.description}
                  </div>
                </div>
              )}

              {/* Tags */}
              {viewDialog.task.tags && (
                <div>
                  <Label className="text-xs text-muted-foreground">{t('backlogPage.tags')}</Label>
                  <div className="mt-2 flex flex-wrap gap-1">
                    {viewDialog.task.tags.split(',').map((tag, idx) => (
                      <Badge key={idx} variant="outline" className="text-xs">
                        {tag.trim()}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Task Dependencies */}
              <div className="border-t pt-4">
                <TaskDependencies 
                  taskId={viewDialog.task.id} 
                  cycleId={viewDialog.task.cycleId}
                  onDependenciesChange={() => {
                    if (viewDialog.task) {
                      // Reload the task to get updated dependency info
                      taskService.getById(viewDialog.task.id).then(response => {
                        setViewDialog({ open: true, task: response.data });
                        // Also refresh the main task list
                        loadTasks();
                      }).catch(error => {
                        console.error('Failed to reload task:', error);
                      });
                    }
                  }}
                />
              </div>

              {/* Subtasks */}
              {subtasks.length > 0 && (
                <div>
                  <Label className="text-sm font-semibold">{t('backlogPage.subtasks')} ({subtasks.length})</Label>
                  <div className="mt-2 space-y-2">
                    {subtasks.map((subtask) => (
                      <div
                        key={subtask.id}
                        className="flex items-center justify-between p-3 border rounded-md hover:bg-muted/50 transition-colors"
                      >
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <span className="text-muted-foreground">└─</span>
                            <span className="font-medium">{subtask.title}</span>
                          </div>
                          {subtask.description && (
                            <p className="text-sm text-muted-foreground mt-1 ml-6">
                              {subtask.description}
                            </p>
                          )}
                        </div>
                        <div className="flex items-center gap-2">
                          <Badge variant={statusOptions.find(s => s.value === subtask.status)?.variant} className="text-xs">
                            {t(statusOptions.find(s => s.value === subtask.status)?.labelKey || 'backlogPage.statusOptions.backlog')}
                          </Badge>
                          <Badge variant={priorityOptions.find(p => p.value === subtask.priority)?.variant} className="text-xs">
                            {t(priorityOptions.find(p => p.value === subtask.priority)?.labelKey || 'backlogPage.priorityOptions.medium')}
                          </Badge>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => handleViewTask(subtask)}
                            title={t('backlogPage.viewDetails')}
                          >
                            <Eye className="h-3 w-3" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => {
                              handleCloseViewDialog();
                              handleOpenDialog(subtask);
                            }}
                            title={t('backlogPage.edit')}
                          >
                            <Pencil className="h-3 w-3" />
                          </Button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Timestamps */}
              <div className="grid grid-cols-2 gap-4 pt-4 border-t">
                <div>
                  <Label className="text-xs text-muted-foreground">Created</Label>
                  <div className="mt-1 text-sm">
                    {dayjs(viewDialog.task.createdAt).format('MMM D, YYYY h:mm A')}
                  </div>
                </div>
                <div>
                  <Label className="text-xs text-muted-foreground">Updated</Label>
                  <div className="mt-1 text-sm">
                    {dayjs(viewDialog.task.updatedAt).format('MMM D, YYYY h:mm A')}
                  </div>
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={handleCloseViewDialog}>
              {t('backlogPage.close')}
            </Button>
            <Button onClick={() => {
              handleCloseViewDialog();
              if (viewDialog.task) {
                handleOpenDialog(viewDialog.task);
              }
            }}>
              <Pencil className="h-4 w-4 mr-2" />
              {t('backlogPage.edit')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialog.open} onOpenChange={(open) => setDeleteDialog({ ...deleteDialog, open })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('backlogPage.deleteTask')}</DialogTitle>
            <DialogDescription>
              {t('backlogPage.confirmDeleteMessage')}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog({ open: false, taskId: null })}>
              {t('backlogPage.cancel')}
            </Button>
            <Button variant="destructive" onClick={handleDeleteTask}>
              {t('backlogPage.delete')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );

  function TaskTable() {
    if (!selectedCycle) {
      return (
        <EmptyState
          title="Select a Cycle"
          description="Choose a cycle to view and manage tasks"
          illustration={<EmptyTasksIllustration />}
        />
      );
    }

    if (tasksLoading) {
      return (
        <div className="flex items-center justify-center h-64">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      );
    }

    if (!tasks || tasks.length === 0) {
      return (
        <EmptyState
          title={`No ${categoryTitle}`}
          description={`No tasks found in this category. Create your first task to get started.`}
          illustration={<EmptyTasksIllustration />}
          action={{
            label: 'Create Task',
            onClick: () => handleOpenDialog(),
          }}
        />
      );
    }

    return (
      <div className="space-y-4">
        <Card>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-[40%]">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleSort('title')}
                    className="-ml-3"
                  >
                    {t('backlogPage.title')}
                    {sortBy === 'title' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                  </Button>
                </TableHead>
                <TableHead>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleSort('status')}
                    className="-ml-3"
                  >
                    {t('common.status')}
                    {sortBy === 'status' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                  </Button>
                </TableHead>
                <TableHead>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleSort('priority')}
                    className="-ml-3"
                  >
                    {t('backlogPage.filters.priority')}
                    {sortBy === 'priority' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                  </Button>
                </TableHead>
                <TableHead>{t('backlogPage.assignee')}</TableHead>
                <TableHead>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleSort('dueDate')}
                    className="-ml-3"
                  >
                    {t('backlogPage.dueDate')}
                    {sortBy === 'dueDate' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                  </Button>
                </TableHead>
                <TableHead className="text-right">{t('common.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {tasks.map((task) => (
                <TableRow key={task.id} className={task.parentTaskId ? 'bg-muted/30' : ''}>
                  <TableCell>
                    <div className={task.parentTaskId ? 'pl-6' : ''}>
                      <div className="font-medium flex items-center gap-2">
                        {task.parentTaskId && (
                          <span className="text-muted-foreground text-xs">└─</span>
                        )}
                        <Link 
                          to={`/backlog/${task.id}`}
                          className="hover:underline cursor-pointer text-primary"
                        >
                          {task.title}
                        </Link>
                        {task.isBlocked && task.blockedByCount && task.blockedByCount > 0 && (
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Badge variant="destructive" className="h-5 px-1.5">
                                  <AlertCircle className="h-3 w-3 mr-1" />
                                  {task.blockedByCount}
                                </Badge>
                              </TooltipTrigger>
                              <TooltipContent className="max-w-xs">
                                <p className="font-semibold mb-1">{t('backlogPage.blockedByCount', { count: task.blockedByCount })}:</p>
                                <ul className="text-sm space-y-0.5">
                                  {task.blockedByTasks?.slice(0, 3).map((blocker, idx) => (
                                    <li key={idx}>• {blocker.sourceTaskTitle}</li>
                                  ))}
                                  {task.blockedByTasks && task.blockedByTasks.length > 3 && (
                                    <li className="text-muted-foreground">{t('backlogPage.andMore', { count: task.blockedByTasks.length - 3 })}</li>
                                  )}
                                </ul>
                              </TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                        )}
                        {task.blockingTasks && task.blockingTasks.length > 0 && (
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Badge variant="secondary" className="h-5 px-1.5">
                                  <Shield className="h-3 w-3 mr-1" />
                                  {task.blockingTasks.length}
                                </Badge>
                              </TooltipTrigger>
                              <TooltipContent className="max-w-xs">
                                <p className="font-semibold mb-1">{t('backlogPage.blockingCount', { count: task.blockingTasks.length })}:</p>
                                <ul className="text-sm space-y-0.5">
                                  {task.blockingTasks?.slice(0, 3).map((blocking, idx) => (
                                    <li key={idx}>• {blocking.targetTaskTitle}</li>
                                  ))}
                                  {task.blockingTasks && task.blockingTasks.length > 3 && (
                                    <li className="text-muted-foreground">{t('backlogPage.andMore', { count: task.blockingTasks.length - 3 })}</li>
                                  )}
                                </ul>
                              </TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                        )}
                        {!task.parentTaskId && task.children && task.children.length > 0 && (
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Badge variant="outline" className="h-5 px-1.5">
                                  <List className="h-3 w-3 mr-1" />
                                  {task.children.length}
                                </Badge>
                              </TooltipTrigger>
                              <TooltipContent className="max-w-xs">
                                <p className="font-semibold mb-1">{t('backlogPage.subtaskCount', { count: task.children.length })}:</p>
                                <ul className="text-sm space-y-0.5">
                                  {task.children.slice(0, 3).map((child, idx) => (
                                    <li key={idx}>• {child.title}</li>
                                  ))}
                                  {task.children.length > 3 && (
                                    <li className="text-muted-foreground">{t('backlogPage.andMore', { count: task.children.length - 3 })}</li>
                                  )}
                                </ul>
                              </TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                        )}
                      </div>
                      {task.description && (
                        <div className="text-sm text-muted-foreground line-clamp-1">
                          {task.description}
                        </div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" className="h-auto p-0">
                          <Badge variant={getStatusBadgeVariant(task.status)}>
                            {t(statusOptions.find(s => s.value === task.status)?.labelKey || 'backlogPage.statusOptions.backlog')}
                          </Badge>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="start">
                        {statusOptions.map((status) => (
                          <DropdownMenuItem
                            key={status.value}
                            onClick={() => handleQuickStatusChange(task.id, status.value)}
                          >
                            <Badge variant={status.variant} className="mr-2">
                              {t(status.labelKey)}
                            </Badge>
                            {task.status === status.value && <Check className="ml-auto h-4 w-4" />}
                          </DropdownMenuItem>
                        ))}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" className="h-auto p-0">
                          <Badge variant={getPriorityBadgeVariant(task.priority)}>
                            {t(priorityOptions.find(p => p.value === task.priority)?.labelKey || 'backlogPage.priorityOptions.medium')}
                          </Badge>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="start">
                        {priorityOptions.map((priority) => (
                          <DropdownMenuItem
                            key={priority.value}
                            onClick={() => handleQuickPriorityChange(task.id, priority.value)}
                          >
                            <Badge variant={priority.variant} className="mr-2">
                              {t(priority.labelKey)}
                            </Badge>
                            {task.priority === priority.value && <Check className="ml-auto h-4 w-4" />}
                          </DropdownMenuItem>
                        ))}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                  <TableCell>
                    {task.assigneeName ? (
                      <div className="flex items-center gap-2">
                        <Avatar className="h-6 w-6">
                          {task.assigneeAvatarUrl ? (
                            <AvatarImage src={task.assigneeAvatarUrl} />
                          ) : (
                            <AvatarFallback className="text-xs">
                              {task.assigneeName.charAt(0)}
                            </AvatarFallback>
                          )}
                        </Avatar>
                        <span className="text-sm">{task.assigneeName}</span>
                      </div>
                    ) : (
                      <span className="text-muted-foreground">{t('backlogPage.unassigned')}</span>
                    )}
                  </TableCell>
                  <TableCell>
                    {task.dueDate ? (
                      <span className={cn(
                        "text-sm",
                        dayjs(task.dueDate).isBefore(dayjs(), 'day') && task.status !== 'DONE' && "text-destructive"
                      )}>
                        {dayjs(task.dueDate).format('MMM D, YYYY')}
                      </span>
                    ) : (
                      <span className="text-muted-foreground">-</span>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      {!task.parentTaskId && (
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleAddSubTask(task)}
                                aria-label={`${t('backlogPage.addSubTask')}: ${task.title}`}
                                className="text-xs"
                              >
                                <Plus className="h-3 w-3 mr-1" aria-hidden="true" />
                                {t('backlogPage.subTask')}
                              </Button>
                            </TooltipTrigger>
                            <TooltipContent>{t('backlogPage.addSubTask')}</TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                      )}
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant={activeTimerTaskId === task.id ? 'destructive' : 'default'}
                              size="sm"
                              onClick={() => handleStartTimer(task)}
                              disabled={activeTimerTaskId !== null && activeTimerTaskId !== task.id}
                              aria-label={`${t('backlogPage.startTimer')}: ${task.title}`}
                              className={activeTimerTaskId === task.id ? 'text-xs' : 'text-xs bg-green-600 hover:bg-green-700'}
                            >
                              <PlayCircle className="h-3 w-3 mr-1" aria-hidden="true" />
                              {activeTimerTaskId === task.id ? t('backlogPage.running') : t('backlogPage.timer')}
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>
                            {activeTimerTaskId === task.id 
                              ? t('backlogPage.timerRunning')
                              : activeTimerTaskId 
                                ? t('backlogPage.stopTimerFirst')
                                : t('backlogPage.startTimer')
                            }
                          </TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => handleViewTask(task)}
                            >
                              <Eye className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('backlogPage.viewDetails')}</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => handleOpenDialog(task)}
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('backlogPage.edit')}</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => setDeleteDialog({ open: true, taskId: task.id })}
                            >
                              <Trash2 className="h-4 w-4 text-destructive" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('backlogPage.delete')}</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Card>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between">
            <div className="text-sm text-muted-foreground">
              Showing {page * rowsPerPage + 1} to {Math.min((page + 1) * rowsPerPage, totalElements)} of {totalElements} tasks
            </div>
            <div className="flex items-center gap-2">
              <Select value={rowsPerPage.toString()} onValueChange={handleChangeRowsPerPage}>
                <SelectTrigger className="w-20">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="10">10</SelectItem>
                  <SelectItem value="25">25</SelectItem>
                  <SelectItem value="50">50</SelectItem>
                </SelectContent>
              </Select>
              <div className="flex items-center gap-1">
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => handleChangePage(page - 1)}
                  disabled={page === 0}
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>
                <span className="text-sm px-2">
                  Page {page + 1} of {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => handleChangePage(page + 1)}
                  disabled={page >= totalPages - 1}
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }
}
