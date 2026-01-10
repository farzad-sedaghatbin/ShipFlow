import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
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
import timerService from '../services/timerService';
import { Task, Cycle, Person, CreateTaskRequest, TaskStatus, TaskPriority, TaskStatistics, TaskCategory } from '../types';
import EmptyState from '../components/EmptyState';
import { EmptyTasksIllustration } from '../components/illustrations';
import TimerWidget from '../components/TimerWidget';
import { getUserFriendlyError } from '../utils/errorMessages';


const statusOptions: { value: TaskStatus; label: string; variant: 'default' | 'secondary' | 'destructive' | 'success' | 'warning' | 'info' | 'outline' }[] = [
  { value: 'BACKLOG', label: 'Backlog', variant: 'secondary' },
  { value: 'TODO', label: 'To Do', variant: 'info' },
  { value: 'IN_PROGRESS', label: 'In Progress', variant: 'default' },
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

export default function BacklogPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const categoryFromUrl = searchParams.get('category') as TaskCategory | null;
  
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
  const [statusFilter, setStatusFilter] = useState<TaskStatus[]>([]);
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority[]>([]);
  const [assigneeFilter, setAssigneeFilter] = useState<number[]>([]);
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
  });
  const [dueDate, setDueDate] = useState<Dayjs | null>(null);

  // Multi-select dropdown states
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const [priorityDropdownOpen, setPriorityDropdownOpen] = useState(false);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const [_assigneeDropdownOpen, _setAssigneeDropdownOpen] = useState(false);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedCycle) {
      loadTasks();
      loadStatistics();
    } else {
      // No cycle selected - ensure clean state
      setTasks([]);
      setTotalElements(0);
      setTasksLoading(false);
      setStatistics(null);
    }
  }, [selectedCycle, activeCategory, tabValue, statusFilter, priorityFilter, assigneeFilter, excludeMode, page, rowsPerPage, sortBy, sortOrder]);

  // Sync URL param to state when URL changes (e.g., browser back/forward)
  useEffect(() => {
    if (categoryFromUrl && categoryFromUrl !== activeCategory) {
      setActiveCategory(categoryFromUrl);
    }
  }, [categoryFromUrl]);

  const loadInitialData = async () => {
    try {
      const [cyclesRes, personsRes] = await Promise.all([
        cycleService.getActive(),
        personService.getAll(),
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

  const loadTasks = async () => {
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
          // Get all my tasks directly from backend
          const response = await taskService.getMy();
          const allTasks = Array.isArray(response.data) ? response.data : [];
          // Filter by category
          const filteredTasks = allTasks.filter((task: Task) => {
            const taskCategory = task.category || 'PITCH_SCOPE';
            return taskCategory === activeCategory;
          });
          setTasks(filteredTasks);
          setTotalElements(filteredTasks.length);
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
        // Get all tasks directly from backend
        const response = await taskService.getAll();
        const allTasks = Array.isArray(response.data) ? response.data : [];
        
        // Filter by category
        let filteredTasks = allTasks.filter((task: Task) => {
          const taskCategory = task.category || 'PITCH_SCOPE';
          return taskCategory === activeCategory;
        });
        
        // Apply additional filters manually
        if (statusFilter.length > 0) {
          filteredTasks = filteredTasks.filter(t => 
            excludeMode ? !statusFilter.includes(t.status) : statusFilter.includes(t.status)
          );
        }
        if (priorityFilter.length > 0) {
          filteredTasks = filteredTasks.filter(t => 
            excludeMode ? !priorityFilter.includes(t.priority) : priorityFilter.includes(t.priority)
          );
        }
        if (assigneeFilter.length > 0) {
          filteredTasks = filteredTasks.filter(t => 
            excludeMode ? !assigneeFilter.includes(t.assigneeId || 0) : assigneeFilter.includes(t.assigneeId || 0)
          );
        }
        
        setTasks(filteredTasks);
        setTotalElements(filteredTasks.length);
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
        setTasks(response?.data?.content || []);
        setTotalElements(response?.data?.totalElements || 0);
      } else {
        // Use category-specific endpoint
        response = await taskService.getByCycleIdAndCategory(selectedCycle, activeCategory, page, rowsPerPage, sortBy, sortOrder);
        setTasks(response?.data?.content || []);
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
    if (!selectedCycle || selectedCycle === 'all') return;
    try {
      const response = await taskService.getStatisticsByCycleId(selectedCycle);
      setStatistics(response.data);
    } catch (error) {
      console.error('Failed to load statistics:', error);
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
      // Only allow creating new task if a cycle is selected
      if (!selectedCycle) {
        toast.error('Please select a cycle first');
        return;
      }
      setEditingTask(null);
      setFormData({
        title: '',
        description: '',
        cycleId: selectedCycle === 'all' ? 0 : selectedCycle,
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
      cycleId: selectedCycle === 'all' ? (cycles[0]?.id || 0) : selectedCycle as number,
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
      toast.success('Timer started for task');
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to start timer';
      toast.error(message);
    }
  };

  const handleTimerStopped = () => {
    // Timer stopped - reload data
    loadTasks();
    if (selectedCycle !== 'all') {
      loadStatistics();
    }
  };

  const validateTaskForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.title.trim()) {
      errors.title = 'Task title is required';
    } else if (formData.title.trim().length < 3) {
      errors.title = 'Task title must be at least 3 characters';
    }

    if (formData.estimateHours !== undefined && formData.estimateHours < 0) {
      errors.estimateHours = 'Estimate hours must be a positive number';
    }

    if (formData.actualHours !== undefined && formData.actualHours < 0) {
      errors.actualHours = 'Actual hours must be a positive number';
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
        toast.success('Task updated successfully');
      } else {
        await taskService.create(data);
        toast.success('Task created successfully');
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
      toast.success('Task deleted successfully');
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
      toast.success('Status updated');
      loadTasks();
      loadStatistics();
    } catch (error: any) {
      const message = getUserFriendlyError(error);
      toast.error(message);
    }
  };

  const getStatusBadgeVariant = (status: TaskStatus) => {
    return statusOptions.find(s => s.value === status)?.variant || 'secondary';
  };

  const getPriorityBadgeVariant = (priority: TaskPriority) => {
    return priorityOptions.find(p => p.value === priority)?.variant || 'secondary';
  };

  const totalPages = Math.ceil(totalElements / rowsPerPage);

  const categoryTitle = activeCategory === 'PITCH_SCOPE' ? 'Pitch Tasks' : 'Debt & Improvements';
  const categoryDescription = activeCategory === 'PITCH_SCOPE' 
    ? 'Tasks scoped to pitches in the current cycle' 
    : 'Technical debt, improvements, and maintenance work';

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Timer Widget */}
      <TimerWidget onTimerStopped={handleTimerStopped} />
      
      {/* Header */}
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Backlog</h1>
          <p className="text-muted-foreground">{categoryDescription}</p>
        </div>
        <div className="flex items-center gap-2">
          {/* Cycle Selector */}
          <Select
            value={selectedCycle === 'all' ? 'all' : selectedCycle?.toString() || ''}
            onValueChange={(value) => setSelectedCycle(value === 'all' ? 'all' : Number(value))}
          >
            <SelectTrigger className="w-[200px]">
              <SelectValue placeholder="Select cycle" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Cycles</SelectItem>
              {cycles.map((cycle) => (
                <SelectItem key={cycle.id} value={cycle.id.toString()}>
                  {cycle.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button 
            onClick={() => handleOpenDialog()} 
            disabled={selectedCycle === 'all' || !selectedCycle}
          >
            <Plus className="mr-2 h-4 w-4" />
            New Task
          </Button>
        </div>
      </div>

      {/* Category Tabs */}
      <Tabs value={activeCategory} onValueChange={(v) => handleCategoryChange(v as TaskCategory)} className="w-full">
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="PITCH_SCOPE" className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            Pitch Tasks
          </TabsTrigger>
          <TabsTrigger value="DEBT_IMPROVEMENT" className="flex items-center gap-2">
            <Wrench className="h-4 w-4" />
            Debt & Improvements
          </TabsTrigger>
        </TabsList>
      </Tabs>

      {/* Statistics Card */}
      {statistics && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Task Overview</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-4">
              <div className="text-center">
                <div className="text-2xl font-bold">{statistics.totalTasks}</div>
                <div className="text-xs text-muted-foreground">Total</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-blue-500">{statistics.todoTasks}</div>
                <div className="text-xs text-muted-foreground">To Do</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-purple-500">{statistics.inProgressTasks}</div>
                <div className="text-xs text-muted-foreground">In Progress</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-red-500">{statistics.blockedTasks}</div>
                <div className="text-xs text-muted-foreground">Blocked</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-yellow-500">{statistics.inReviewTasks}</div>
                <div className="text-xs text-muted-foreground">In Review</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-500">{statistics.doneTasks}</div>
                <div className="text-xs text-muted-foreground">Done</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold">{statistics.completionPercentage}%</div>
                <div className="text-xs text-muted-foreground">Complete</div>
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
            <TabsTrigger value="all">All Tasks</TabsTrigger>
            <TabsTrigger value="my">My Tasks</TabsTrigger>
          </TabsList>

          {/* Filters */}
          <div className="flex items-center gap-2 flex-wrap">
            {/* Status Filter */}
            <DropdownMenu open={statusDropdownOpen} onOpenChange={setStatusDropdownOpen}>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm">
                  Status {statusFilter.length > 0 && `(${statusFilter.length})`}
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
                    {status.label}
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>

            {/* Priority Filter */}
            <DropdownMenu open={priorityDropdownOpen} onOpenChange={setPriorityDropdownOpen}>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm">
                  Priority {priorityFilter.length > 0 && `(${priorityFilter.length})`}
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
                    {priority.label}
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>

            {(statusFilter.length > 0 || priorityFilter.length > 0 || assigneeFilter.length > 0) && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  setStatusFilter([]);
                  setPriorityFilter([]);
                  setAssigneeFilter([]);
                }}
              >
                Clear filters
              </Button>
            )}
          </div>
        </div>

        <TabsContent value="all" className="mt-0">
          <TaskTable />
        </TabsContent>
        <TabsContent value="my" className="mt-0">
          <TaskTable />
        </TabsContent>
      </Tabs>

      {/* Task Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editingTask ? 'Edit Task' : 'Create Task'}</DialogTitle>
            <DialogDescription>
              {activeCategory === 'PITCH_SCOPE' 
                ? 'Create a task scoped to a pitch in this cycle'
                : 'Create a technical debt or improvement task'}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="title">Title *</Label>
              <Input
                id="title"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                placeholder="Task title"
                className={fieldErrors.title ? 'border-destructive' : ''}
              />
              {fieldErrors.title && (
                <p className="text-sm text-destructive">{fieldErrors.title}</p>
              )}
            </div>
            <div className="grid gap-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Task description"
                rows={3}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="status">Status</Label>
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
                        {status.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="priority">Priority</Label>
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
                        {priority.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="assignee">Assignee</Label>
                <Select
                  value={formData.assigneeId?.toString() || 'unassigned'}
                  onValueChange={(value) => setFormData({ ...formData, assigneeId: value === 'unassigned' ? undefined : Number(value) })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select assignee" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="unassigned">Unassigned</SelectItem>
                    {persons.map((person) => (
                      <SelectItem key={person.id} value={person.id.toString()}>
                        {person.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="pairAssignee">Pair Assignee</Label>
                <Select
                  value={formData.pairAssigneeId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ ...formData, pairAssigneeId: value === 'none' ? undefined : Number(value) })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select pair" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
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
                <Label htmlFor="estimateHours">Estimate (hours)</Label>
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
                <Label htmlFor="dueDate">Due Date</Label>
                <Input
                  id="dueDate"
                  type="date"
                  value={dueDate ? dueDate.format('YYYY-MM-DD') : ''}
                  onChange={(e) => setDueDate(e.target.value ? dayjs(e.target.value) : null)}
                />
              </div>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="tags">Tags</Label>
              <Input
                id="tags"
                value={formData.tags}
                onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                placeholder="Comma-separated tags"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={handleCloseDialog}>
              Cancel
            </Button>
            <Button onClick={handleSaveTask} disabled={saving}>
              {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {editingTask ? 'Update' : 'Create'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialog.open} onOpenChange={(open) => setDeleteDialog({ ...deleteDialog, open })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Task</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete this task? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog({ open: false, taskId: null })}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDeleteTask}>
              Delete
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
                    Title
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
                    Status
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
                    Priority
                    {sortBy === 'priority' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                  </Button>
                </TableHead>
                <TableHead>Assignee</TableHead>
                <TableHead>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleSort('dueDate')}
                    className="-ml-3"
                  >
                    Due Date
                    {sortBy === 'dueDate' && (sortOrder === 'asc' ? <ArrowUp className="ml-1 h-4 w-4" /> : <ArrowDown className="ml-1 h-4 w-4" />)}
                  </Button>
                </TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {tasks.map((task) => (
                <TableRow key={task.id}>
                  <TableCell>
                    <div>
                      <div className="font-medium">{task.title}</div>
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
                            {statusOptions.find(s => s.value === task.status)?.label || task.status}
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
                              {status.label}
                            </Badge>
                            {task.status === status.value && <Check className="ml-auto h-4 w-4" />}
                          </DropdownMenuItem>
                        ))}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                  <TableCell>
                    <Badge variant={getPriorityBadgeVariant(task.priority)}>
                      {priorityOptions.find(p => p.value === task.priority)?.label || task.priority}
                    </Badge>
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
                      <span className="text-muted-foreground">Unassigned</span>
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
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleAddSubTask(task)}
                              aria-label={`Add sub-task to: ${task.title}`}
                              className="text-xs"
                            >
                              <Plus className="h-3 w-3 mr-1" aria-hidden="true" />
                              Sub-task
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Add a sub-task under this task</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="default"
                              size="sm"
                              onClick={() => handleStartTimer(task)}
                              aria-label={`Start timer for: ${task.title}`}
                              className="text-xs bg-green-600 hover:bg-green-700"
                            >
                              <PlayCircle className="h-3 w-3 mr-1" aria-hidden="true" />
                              Timer
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Start timer for this task</TooltipContent>
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
                          <TooltipContent>Edit</TooltipContent>
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
                          <TooltipContent>Delete</TooltipContent>
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
