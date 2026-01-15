import { useEffect, useState } from 'react';
import dayjs, { Dayjs } from 'dayjs';
import { toast } from 'sonner';
import { 
  Plus, 
  Trash2, 
  Pencil, 
  ClipboardList, 
  Flag, 
  User, 
  Clock, 
  ArrowUp, 
  ArrowDown,
  ChevronLeft,
  ChevronRight,
  Check,
  AlertCircle,
  List,
} from 'lucide-react';
import { cn } from '../lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Switch } from '@/components/ui/switch';
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
import { Task, Cycle, Person, CreateTaskRequest, TaskStatus, TaskPriority, TaskStatistics } from '../types';
import EmptyState from '../components/EmptyState';
import { EmptyTasksIllustration } from '../components/illustrations';
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

export default function Tasks() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<number | 'all'>('all');
  const [statistics, setStatistics] = useState<TaskStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [tabValue, setTabValue] = useState('all');
  const [statusFilter, setStatusFilter] = useState<TaskStatus[]>([]);
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority[]>([]);
  const [assigneeFilter, setAssigneeFilter] = useState<number[]>([]);
  const [excludeMode, setExcludeMode] = useState(false);
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
  });
  const [dueDate, setDueDate] = useState<Dayjs | null>(null);

  // Multi-select dropdown states
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const [priorityDropdownOpen, setPriorityDropdownOpen] = useState(false);
  const [assigneeDropdownOpen, setAssigneeDropdownOpen] = useState(false);

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedCycle) {
      loadTasks();
      if (selectedCycle !== 'all') {
        loadStatistics();
      }
    }
  }, [selectedCycle, tabValue, statusFilter, priorityFilter, assigneeFilter, excludeMode, page, rowsPerPage, sortBy, sortOrder]);

  const loadInitialData = async () => {
    try {
      const [cyclesRes, personsRes] = await Promise.all([
        cycleService.getActive(),
        personService.getAll(),
      ]);
      setCycles(cyclesRes.data);
      setPersons(personsRes);
      // Don't auto-select a cycle - show all by default
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadTasks = async () => {
    if (!selectedCycle) return;
    try {
      let response: any;
      if (tabValue === 'my') {
        // My Tasks tab - needs a specific cycle
        if (selectedCycle === 'all') return;
        response = await taskService.getMyByCycle(selectedCycle, page, rowsPerPage, sortBy, sortOrder);
      } else if (selectedCycle === 'all') {
        // All cycles - get all tasks
        const allTasksPromises = cycles.map(cycle => 
          taskService.getByCycleId(cycle.id, 0, 1000, sortBy, sortOrder)
        );
        const allResponses = await Promise.all(allTasksPromises);
        const allTasks = allResponses.flatMap(res => {
          const data = res.data;
          if (Array.isArray(data)) {
            return data;
          } else if (data && typeof data === 'object' && 'content' in data) {
            return (data as any).content;
          }
          return [];
        });
        
        // Apply filters manually
        let filteredTasks = allTasks;
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
        return;
      } else if (statusFilter.length > 0 || priorityFilter.length > 0 || assigneeFilter.length > 0) {
        // Use new filter endpoint when filters are applied
        response = await taskService.getWithFilters(
          selectedCycle,
          statusFilter.length > 0 ? statusFilter : undefined,
          priorityFilter.length > 0 ? priorityFilter : undefined,
          assigneeFilter.length > 0 ? assigneeFilter : undefined,
          undefined, // category
          excludeMode,
          page,
          rowsPerPage,
          sortBy,
          sortOrder
        );
      } else {
        response = await taskService.getByCycleId(selectedCycle, page, rowsPerPage, sortBy, sortOrder);
      }
      setTasks(response.data.content);
      setTotalElements(response.data.totalElements);
    } catch (error) {
      console.error('Failed to load tasks:', error);
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
      });
      setDueDate(task.dueDate ? dayjs(task.dueDate) : null);
    } else {
      setEditingTask(null);
      setFormData({
        title: '',
        description: '',
        cycleId: selectedCycle as number,
        status: 'BACKLOG',
        priority: 'MEDIUM',
        estimateHours: undefined,
        assigneeId: undefined,
        pairAssigneeId: undefined,
        dueDate: undefined,
        tags: '',
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
    setPage(0); // Reset to first page when sorting changes
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
      cycleId: selectedCycle as number,
      parentTaskId: parentTask.id,
      status: 'BACKLOG',
      priority: 'MEDIUM',
      estimateHours: undefined,
      assigneeId: undefined,
      pairAssigneeId: undefined,
      dueDate: undefined,
      tags: '',
    });
    setDueDate(null);
    setEditingTask(null);
    setDialogOpen(true);
  };

  // Validate task form
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
      };
      
      if (editingTask) {
        await taskService.update(editingTask.id, data);
        toast.success('Task updated successfully!');
      } else {
        await taskService.create(data);
        toast.success('Task created successfully!');
      }
      
      handleCloseDialog();
      loadTasks();
      loadStatistics();
    } catch (error) {
      toast.error(getUserFriendlyError(error, 'Failed to save task'));
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteTask = async () => {
    if (!deleteDialog.taskId) return;
    try {
      await taskService.delete(deleteDialog.taskId);
      toast.success('Task deleted successfully!');
      setDeleteDialog({ open: false, taskId: null });
      loadTasks();
      loadStatistics();
    } catch (error) {
      toast.error(getUserFriendlyError(error, 'Failed to delete task'));
    }
  };

  const handleStatusChange = async (taskId: number, newStatus: TaskStatus) => {
    try {
      await taskService.updateStatus(taskId, newStatus);
      loadTasks();
      loadStatistics();
    } catch (error) {
      toast.error(getUserFriendlyError(error, 'Failed to update status'));
    }
  };

  const getStatusBadge = (status: TaskStatus) => {
    const option = statusOptions.find(o => o.value === status);
    return <Badge variant={option?.variant}>{option?.label}</Badge>;
  };

  const getPriorityBadge = (priority: TaskPriority) => {
    const option = priorityOptions.find(o => o.value === priority);
    return (
      <Badge variant={option?.variant} className="gap-1">
        <Flag className="h-3 w-3" />
        {option?.label}
      </Badge>
    );
  };

  const toggleStatusFilter = (status: TaskStatus) => {
    setStatusFilter(prev => 
      prev.includes(status) 
        ? prev.filter(s => s !== status)
        : [...prev, status]
    );
  };

  const togglePriorityFilter = (priority: TaskPriority) => {
    setPriorityFilter(prev => 
      prev.includes(priority) 
        ? prev.filter(p => p !== priority)
        : [...prev, priority]
    );
  };

  const toggleAssigneeFilter = (personId: number) => {
    setAssigneeFilter(prev => 
      prev.includes(personId) 
        ? prev.filter(id => id !== personId)
        : [...prev, personId]
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  return (
    <TooltipProvider>
      <div>
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <ClipboardList className="h-6 w-6" />
            Tasks
          </h1>
          <Button
            onClick={() => handleOpenDialog()}
            disabled={!selectedCycle}
            className="w-full sm:w-auto"
          >
            <Plus className="h-4 w-4 mr-2" />
            New Task
          </Button>
        </div>

        {/* Cycle Selector & Filters */}
        <Card className="mb-6">
          <CardContent className="pt-6">
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-6 xl:grid-cols-8 gap-4 items-end">
              {/* Cycle Selector */}
              <div className="space-y-2 sm:col-span-2 md:col-span-1 lg:col-span-2">
                <Label>Cycle</Label>
                <Select
                  value={selectedCycle ? String(selectedCycle) : 'all'}
                  onValueChange={(value) => setSelectedCycle(value === 'all' ? 'all' : Number(value))}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select cycle" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All Cycles</SelectItem>
                    {cycles.map((cycle) => (
                      <SelectItem key={cycle.id} value={String(cycle.id)}>
                        {cycle.projectKey && `[${cycle.projectKey}] `}{cycle.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Status Filter */}
              <div className="space-y-2">
                <Label>Status Filter</Label>
                <DropdownMenu open={statusDropdownOpen} onOpenChange={setStatusDropdownOpen}>
                  <DropdownMenuTrigger asChild>
                    <Button variant="outline" className="w-full justify-start font-normal">
                      {statusFilter.length === 0 ? (
                        <span className="text-muted-foreground">Select status</span>
                      ) : (
                        <div className="flex flex-wrap gap-1">
                          {statusFilter.slice(0, 2).map((value) => (
                            <Badge key={value} variant="secondary" className="text-xs">
                              {statusOptions.find(o => o.value === value)?.label}
                            </Badge>
                          ))}
                          {statusFilter.length > 2 && (
                            <Badge variant="secondary" className="text-xs">
                              +{statusFilter.length - 2}
                            </Badge>
                          )}
                        </div>
                      )}
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent className="w-56">
                    {statusOptions.map((option) => (
                      <DropdownMenuItem
                        key={option.value}
                        onClick={(e) => {
                          e.preventDefault();
                          toggleStatusFilter(option.value);
                        }}
                        className="cursor-pointer"
                      >
                        <Checkbox
                          checked={statusFilter.includes(option.value)}
                          className="mr-2"
                        />
                        {option.label}
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>

              {/* Priority Filter */}
              <div className="space-y-2">
                <Label>Priority Filter</Label>
                <DropdownMenu open={priorityDropdownOpen} onOpenChange={setPriorityDropdownOpen}>
                  <DropdownMenuTrigger asChild>
                    <Button variant="outline" className="w-full justify-start font-normal">
                      {priorityFilter.length === 0 ? (
                        <span className="text-muted-foreground">Select priority</span>
                      ) : (
                        <div className="flex flex-wrap gap-1">
                          {priorityFilter.slice(0, 2).map((value) => (
                            <Badge key={value} variant="secondary" className="text-xs">
                              {priorityOptions.find(o => o.value === value)?.label}
                            </Badge>
                          ))}
                          {priorityFilter.length > 2 && (
                            <Badge variant="secondary" className="text-xs">
                              +{priorityFilter.length - 2}
                            </Badge>
                          )}
                        </div>
                      )}
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent className="w-56">
                    {priorityOptions.map((option) => (
                      <DropdownMenuItem
                        key={option.value}
                        onClick={(e) => {
                          e.preventDefault();
                          togglePriorityFilter(option.value);
                        }}
                        className="cursor-pointer"
                      >
                        <Checkbox
                          checked={priorityFilter.includes(option.value)}
                          className="mr-2"
                        />
                        {option.label}
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>

              {/* Assignee Filter */}
              <div className="space-y-2">
                <Label>Assignee Filter</Label>
                <DropdownMenu open={assigneeDropdownOpen} onOpenChange={setAssigneeDropdownOpen}>
                  <DropdownMenuTrigger asChild>
                    <Button variant="outline" className="w-full justify-start font-normal">
                      {assigneeFilter.length === 0 ? (
                        <span className="text-muted-foreground">Select assignee</span>
                      ) : (
                        <div className="flex flex-wrap gap-1">
                          {assigneeFilter.slice(0, 2).map((value) => {
                            const person = persons.find(p => p.id === value);
                            return (
                              <Badge key={value} variant="secondary" className="text-xs">
                                {person?.name}
                              </Badge>
                            );
                          })}
                          {assigneeFilter.length > 2 && (
                            <Badge variant="secondary" className="text-xs">
                              +{assigneeFilter.length - 2}
                            </Badge>
                          )}
                        </div>
                      )}
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent className="w-56">
                    {persons.map((person) => (
                      <DropdownMenuItem
                        key={person.id}
                        onClick={(e) => {
                          e.preventDefault();
                          toggleAssigneeFilter(person.id);
                        }}
                        className="cursor-pointer"
                      >
                        <Checkbox
                          checked={assigneeFilter.includes(person.id)}
                          className="mr-2"
                        />
                        <div className="flex items-center gap-2">
                          <Avatar className="h-6 w-6">
                            <AvatarImage src={person.avatarUrl} />
                            <AvatarFallback className="text-xs">
                              {person.name.charAt(0)}
                            </AvatarFallback>
                          </Avatar>
                          {person.name}
                        </div>
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>

              {/* Exclude Mode Switch */}
              <div className="space-y-2">
                <Label>Exclude Mode</Label>
                <div className="flex items-center h-9">
                  <Switch
                    checked={excludeMode}
                    onCheckedChange={setExcludeMode}
                  />
                </div>
              </div>

              {/* Clear Filters */}
              <div className="space-y-2">
                <Label className="invisible">Clear</Label>
                <Button
                  variant="outline"
                  size="sm"
                  className="w-full"
                  onClick={() => {
                    setStatusFilter([]);
                    setPriorityFilter([]);
                    setAssigneeFilter([]);
                    setExcludeMode(false);
                  }}
                >
                  Clear
                </Button>
              </div>

              {/* Sort By */}
              <div className="space-y-2">
                <Label>Sort By</Label>
                <Select value={sortBy} onValueChange={(value) => setSortBy(value as any)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="createdAt">Created Date</SelectItem>
                    <SelectItem value="priority">Priority</SelectItem>
                    <SelectItem value="status">Status</SelectItem>
                    <SelectItem value="dueDate">Due Date</SelectItem>
                    <SelectItem value="title">Title</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* Sort Order */}
              <div className="space-y-2">
                <Label className="invisible">Order</Label>
                <Button
                  variant="outline"
                  className="w-full"
                  onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                >
                  {sortOrder === 'asc' ? <ArrowUp className="h-4 w-4 mr-2" /> : <ArrowDown className="h-4 w-4 mr-2" />}
                  {sortOrder === 'asc' ? 'Asc' : 'Desc'}
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Statistics */}
        {statistics && (
          <Card className="mb-6">
            <CardHeader>
              <CardTitle>Cycle Task Statistics</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
                <div className="text-center">
                  <p className="text-3xl font-bold text-primary">{statistics.totalTasks}</p>
                  <p className="text-sm text-muted-foreground">Total</p>
                </div>
                <div className="text-center">
                  <p className="text-3xl font-bold text-success">{statistics.doneTasks}</p>
                  <p className="text-sm text-muted-foreground">Done</p>
                </div>
                <div className="text-center">
                  <p className="text-3xl font-bold text-info">{statistics.inProgressTasks}</p>
                  <p className="text-sm text-muted-foreground">In Progress</p>
                </div>
                <div className="text-center">
                  <p className="text-3xl font-bold text-destructive">{statistics.blockedTasks}</p>
                  <p className="text-sm text-muted-foreground">Blocked</p>
                </div>
                <div className="text-center">
                  <p className="text-3xl font-bold">{statistics.completionPercentage.toFixed(0)}%</p>
                  <p className="text-sm text-muted-foreground">Completion</p>
                </div>
                <div className="text-center">
                  <p className="text-3xl font-bold">{statistics.totalEstimateHours.toFixed(0)}h</p>
                  <p className="text-sm text-muted-foreground">Est. Hours</p>
                </div>
              </div>
              <div className="mt-4">
                <Progress value={statistics.completionPercentage} className="h-3" />
              </div>
            </CardContent>
          </Card>
        )}

        {/* Tabs */}
        <Tabs value={tabValue} onValueChange={setTabValue} className="mb-4">
          <TabsList>
            <TabsTrigger value="all">All Tasks</TabsTrigger>
            <TabsTrigger value="my" className="gap-2">
              <User className="h-4 w-4" />
              My Tasks
            </TabsTrigger>
          </TabsList>
          <TabsContent value="all">
            <TaskTable 
              tasks={tasks}
              totalCount={totalElements}
              page={page}
              rowsPerPage={rowsPerPage}
              onPageChange={handleChangePage}
              onRowsPerPageChange={handleChangeRowsPerPage}
              onEdit={handleOpenDialog} 
              onAddSubTask={handleAddSubTask}
              onDelete={(id) => setDeleteDialog({ open: true, taskId: id })}
              onStatusChange={handleStatusChange}
              getStatusBadge={getStatusBadge}
              getPriorityBadge={getPriorityBadge}
              sortBy={sortBy}
              sortOrder={sortOrder}
              onSort={handleSort}
              statusOptions={statusOptions}
            />
          </TabsContent>
          <TabsContent value="my">
            <TaskTable 
              tasks={tasks}
              totalCount={totalElements}
              page={page}
              rowsPerPage={rowsPerPage}
              onPageChange={handleChangePage}
              onRowsPerPageChange={handleChangeRowsPerPage}
              onEdit={handleOpenDialog} 
              onAddSubTask={handleAddSubTask}
              onDelete={(id) => setDeleteDialog({ open: true, taskId: id })}
              onStatusChange={handleStatusChange}
              getStatusBadge={getStatusBadge}
              getPriorityBadge={getPriorityBadge}
              sortBy={sortBy}
              sortOrder={sortOrder}
              onSort={handleSort}
              statusOptions={statusOptions}
            />
          </TabsContent>
        </Tabs>

        {/* Task Dialog */}
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>
                {editingTask ? 'Edit Task' : 'Create New Task'}
              </DialogTitle>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="title">Title *</Label>
                <Input
                  id="title"
                  value={formData.title}
                  onChange={(e) => {
                    setFormData({ ...formData, title: e.target.value });
                    setFieldErrors((prev) => ({ ...prev, title: '' }));
                  }}
                  className={cn(fieldErrors.title && 'border-destructive')}
                />
                {fieldErrors.title ? (
                  <p className="text-sm text-destructive">{fieldErrors.title}</p>
                ) : (
                  <p className="text-sm text-muted-foreground">Give your task a clear, descriptive title</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  rows={3}
                />
                <p className="text-sm text-muted-foreground">Describe what needs to be done</p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Status</Label>
                  <Select
                    value={formData.status}
                    onValueChange={(value) => setFormData({ ...formData, status: value as TaskStatus })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {statusOptions.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>Priority</Label>
                  <Select
                    value={formData.priority}
                    onValueChange={(value) => setFormData({ ...formData, priority: value as TaskPriority })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {priorityOptions.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          <div className="flex items-center gap-2">
                            <Flag className={cn(
                              "h-4 w-4",
                              option.variant === 'secondary' && 'text-muted-foreground',
                              option.variant === 'info' && 'text-info',
                              option.variant === 'warning' && 'text-warning',
                              option.variant === 'destructive' && 'text-destructive'
                            )} />
                            {option.label}
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Assignee</Label>
                  <Select
                    value={formData.assigneeId ? String(formData.assigneeId) : 'unassigned'}
                    onValueChange={(value) => setFormData({ ...formData, assigneeId: value === 'unassigned' ? undefined : Number(value) })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Unassigned" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="unassigned">Unassigned</SelectItem>
                      {persons.map((person) => (
                        <SelectItem key={person.id} value={String(person.id)}>
                          <div className="flex items-center gap-2">
                            <Avatar className="h-6 w-6">
                              <AvatarImage src={person.avatarUrl} />
                              <AvatarFallback className="text-xs">
                                {person.name.charAt(0)}
                              </AvatarFallback>
                            </Avatar>
                            {person.name}
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>Pair Assignee</Label>
                  <Select
                    value={formData.pairAssigneeId ? String(formData.pairAssigneeId) : 'none'}
                    onValueChange={(value) => setFormData({ ...formData, pairAssigneeId: value === 'none' ? undefined : Number(value) })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="None" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">None</SelectItem>
                      {persons.filter(p => p.id !== formData.assigneeId).map((person) => (
                        <SelectItem key={person.id} value={String(person.id)}>
                          <div className="flex items-center gap-2">
                            <Avatar className="h-6 w-6">
                              <AvatarImage src={person.avatarUrl} />
                              <AvatarFallback className="text-xs">
                                {person.name.charAt(0)}
                              </AvatarFallback>
                            </Avatar>
                            {person.name}
                          </div>
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-2">
                <Label>Parent Task (optional)</Label>
                <Select
                  value={formData.parentTaskId ? String(formData.parentTaskId) : 'none'}
                  onValueChange={(value) => setFormData({ ...formData, parentTaskId: value === 'none' ? undefined : Number(value) })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="No parent task" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">No parent task</SelectItem>
                    {tasks.filter(t => !editingTask || t.id !== editingTask.id).map((task) => (
                      <SelectItem key={task.id} value={String(task.id)}>
                        {task.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-sm text-muted-foreground">Make this a sub-task of another task</p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="estimateHours">Estimate (hours)</Label>
                  <Input
                    id="estimateHours"
                    type="number"
                    min={0}
                    step={0.5}
                    value={formData.estimateHours || ''}
                    onChange={(e) => {
                      setFormData({ ...formData, estimateHours: e.target.value ? parseFloat(e.target.value) : undefined });
                      setFieldErrors((prev) => ({ ...prev, estimateHours: '' }));
                    }}
                    className={cn(fieldErrors.estimateHours && 'border-destructive')}
                  />
                  {fieldErrors.estimateHours && (
                    <p className="text-sm text-destructive">{fieldErrors.estimateHours}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="actualHours">Actual Hours</Label>
                  <Input
                    id="actualHours"
                    type="number"
                    min={0}
                    step={0.5}
                    value={formData.actualHours || ''}
                    onChange={(e) => {
                      setFormData({ ...formData, actualHours: e.target.value ? parseFloat(e.target.value) : undefined });
                      setFieldErrors((prev) => ({ ...prev, actualHours: '' }));
                    }}
                    className={cn(fieldErrors.actualHours && 'border-destructive')}
                  />
                  {fieldErrors.actualHours && (
                    <p className="text-sm text-destructive">{fieldErrors.actualHours}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label>Due Date</Label>
                  <Input
                    type="date"
                    value={dueDate ? dueDate.format('YYYY-MM-DD') : ''}
                    onChange={(e) => setDueDate(e.target.value ? dayjs(e.target.value) : null)}
                    className="w-full"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="tags">Tags (comma separated)</Label>
                <Input
                  id="tags"
                  value={formData.tags}
                  onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
                  placeholder="e.g., bug, tech-debt, refactor"
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={handleCloseDialog} disabled={saving}>
                Cancel
              </Button>
              <Button onClick={handleSaveTask} disabled={saving}>
                {saving && (
                  <svg
                    className="animate-spin -ml-1 mr-2 h-4 w-4"
                    xmlns="http://www.w3.org/2000/svg"
                    fill="none"
                    viewBox="0 0 24 24"
                  >
                    <circle
                      className="opacity-25"
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="4"
                    />
                    <path
                      className="opacity-75"
                      fill="currentColor"
                      d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                    />
                  </svg>
                )}
                {editingTask ? 'Update' : 'Create'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Delete Confirmation Dialog */}
        <Dialog open={deleteDialog.open} onOpenChange={(open) => setDeleteDialog({ open, taskId: open ? deleteDialog.taskId : null })}>
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
    </TooltipProvider>
  );
}

interface TaskTableProps {
  tasks: Task[];
  totalCount: number;
  page: number;
  rowsPerPage: number;
  onPageChange: (newPage: number) => void;
  onRowsPerPageChange: (value: string) => void;
  onEdit: (task: Task) => void;
  onAddSubTask: (task: Task) => void;
  onDelete: (id: number) => void;
  onStatusChange: (id: number, status: TaskStatus) => void;
  getStatusBadge: (status: TaskStatus) => React.ReactNode;
  getPriorityBadge: (priority: TaskPriority) => React.ReactNode;
  sortBy: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title';
  sortOrder: 'asc' | 'desc';
  onSort: (field: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title') => void;
  statusOptions: typeof statusOptions;
}

function TaskTable({ 
  tasks, 
  totalCount, 
  page, 
  rowsPerPage, 
  onPageChange, 
  onRowsPerPageChange, 
  onEdit, 
  onAddSubTask,
  onDelete, 
  onStatusChange, 
  getStatusBadge, 
  getPriorityBadge, 
  sortBy, 
  sortOrder, 
  onSort,
  statusOptions 
}: TaskTableProps) {
  if (tasks.length === 0) {
    return (
      <EmptyState
        illustration={<EmptyTasksIllustration />}
        title="No tasks found"
        description="Create a new task to track independent work items during this cycle."
        size="medium"
      />
    );
  }

  const totalPages = Math.ceil(totalCount / rowsPerPage);
  const startItem = page * rowsPerPage + 1;
  const endItem = Math.min((page + 1) * rowsPerPage, totalCount);

  const SortIcon = ({ field }: { field: string }) => {
    if (sortBy !== field) return null;
    return sortOrder === 'asc' ? <ArrowUp className="h-4 w-4" /> : <ArrowDown className="h-4 w-4" />;
  };

  return (
    <Card>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead 
              className="cursor-pointer select-none hover:bg-muted/50"
              onClick={() => onSort('title')}
            >
              <div className="flex items-center gap-1">
                Title
                <SortIcon field="title" />
              </div>
            </TableHead>
            <TableHead 
              className="cursor-pointer select-none hover:bg-muted/50"
              onClick={() => onSort('status')}
            >
              <div className="flex items-center gap-1">
                Status
                <SortIcon field="status" />
              </div>
            </TableHead>
            <TableHead 
              className="cursor-pointer select-none hover:bg-muted/50"
              onClick={() => onSort('priority')}
            >
              <div className="flex items-center gap-1">
                Priority
                <SortIcon field="priority" />
              </div>
            </TableHead>
            <TableHead>Assignee(s)</TableHead>
            <TableHead>Estimate</TableHead>
            <TableHead 
              className="cursor-pointer select-none hover:bg-muted/50"
              onClick={() => onSort('dueDate')}
            >
              <div className="flex items-center gap-1">
                Due Date
                <SortIcon field="dueDate" />
              </div>
            </TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {tasks.map((task) => (
            <TableRow key={task.id}>
              <TableCell>
                <div className={cn(task.parentTaskId && "ml-6")}>
                  <div className="flex items-center gap-2">
                    {task.parentTaskId && (
                      <div className="text-muted-foreground">
                        <ArrowDown className="h-3 w-3 rotate-90" />
                      </div>
                    )}
                    <p className="font-medium">{task.title}</p>
                    {task.isBlocked && task.blockedByCount && task.blockedByCount > 0 && (
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Badge variant="destructive" className="ml-2 h-5 px-1.5">
                              <AlertCircle className="h-3 w-3 mr-1" />
                              {task.blockedByCount}
                            </Badge>
                          </TooltipTrigger>
                          <TooltipContent className="max-w-xs">
                            <p className="font-semibold mb-1">Blocked by {task.blockedByCount} task{task.blockedByCount > 1 ? 's' : ''}:</p>
                            <ul className="text-sm space-y-0.5">
                              {task.blockedByTasks?.slice(0, 3).map((blocker, idx) => (
                                <li key={idx}>• {blocker.sourceTaskTitle}</li>
                              ))}
                              {task.blockedByTasks && task.blockedByTasks.length > 3 && (
                                <li className="text-muted-foreground">... and {task.blockedByTasks.length - 3} more</li>
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
                            <Badge variant="outline" className="ml-2 h-5 px-1.5">
                              <List className="h-3 w-3 mr-1" />
                              {task.children.length}
                            </Badge>
                          </TooltipTrigger>
                          <TooltipContent className="max-w-xs">
                            <p className="font-semibold mb-1">{task.children.length} subtask{task.children.length > 1 ? 's' : ''}:</p>
                            <ul className="text-sm space-y-0.5">
                              {task.children.slice(0, 3).map((child, idx) => (
                                <li key={idx}>• {child.title}</li>
                              ))}
                              {task.children.length > 3 && (
                                <li className="text-muted-foreground">... and {task.children.length - 3} more</li>
                              )}
                            </ul>
                          </TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                    )}
                  </div>
                  {task.parentTaskTitle && (
                    <p className="text-xs text-muted-foreground ml-5">
                      Sub-task of: {task.parentTaskTitle}
                    </p>
                  )}
                  {task.description && (
                    <p className="text-sm text-muted-foreground ml-5">
                      {task.description.substring(0, 80)}{task.description.length > 80 ? '...' : ''}
                    </p>
                  )}
                  {task.tags && (
                    <div className="flex flex-wrap gap-1 mt-1 ml-5">
                      {task.tags.split(',').map((tag, i) => (
                        <Badge key={i} variant="outline" className="text-xs h-5">
                          {tag.trim()}
                        </Badge>
                      ))}
                    </div>
                  )}
                </div>
              </TableCell>
              <TableCell>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" className="h-auto p-0 hover:bg-transparent">
                      {getStatusBadge(task.status)}
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent>
                    {statusOptions.map((option) => (
                      <DropdownMenuItem
                        key={option.value}
                        onClick={() => onStatusChange(task.id, option.value)}
                        className="cursor-pointer"
                      >
                        {task.status === option.value && <Check className="h-4 w-4 mr-2" />}
                        <span className={task.status === option.value ? '' : 'ml-6'}>{option.label}</span>
                      </DropdownMenuItem>
                    ))}
                  </DropdownMenuContent>
                </DropdownMenu>
              </TableCell>
              <TableCell>{getPriorityBadge(task.priority)}</TableCell>
              <TableCell>
                <div className="flex -space-x-2">
                  {task.assigneeName && (
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Avatar className="h-7 w-7 border-2 border-background">
                          <AvatarImage src={task.assigneeAvatarUrl} />
                          <AvatarFallback className="text-xs">
                            {task.assigneeName.charAt(0)}
                          </AvatarFallback>
                        </Avatar>
                      </TooltipTrigger>
                      <TooltipContent>{task.assigneeName}</TooltipContent>
                    </Tooltip>
                  )}
                  {task.pairAssigneeName && (
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <Avatar className="h-7 w-7 border-2 border-background">
                          <AvatarImage src={task.pairAssigneeAvatarUrl} />
                          <AvatarFallback className="text-xs">
                            {task.pairAssigneeName.charAt(0)}
                          </AvatarFallback>
                        </Avatar>
                      </TooltipTrigger>
                      <TooltipContent>Pair: {task.pairAssigneeName}</TooltipContent>
                    </Tooltip>
                  )}
                </div>
                {!task.assigneeName && !task.pairAssigneeName && (
                  <span className="text-sm text-muted-foreground">Unassigned</span>
                )}
              </TableCell>
              <TableCell>
                {task.estimateHours ? (
                  <div className="flex items-center gap-1">
                    <Clock className="h-4 w-4 text-muted-foreground" />
                    <span>{task.estimateHours}h</span>
                    {task.actualHours && (
                      <span className="text-sm text-muted-foreground">
                        ({task.actualHours}h actual)
                      </span>
                    )}
                  </div>
                ) : (
                  <span className="text-sm text-muted-foreground">-</span>
                )}
              </TableCell>
              <TableCell>
                {task.dueDate ? (
                  <Badge 
                    variant={dayjs(task.dueDate).isBefore(dayjs(), 'day') && task.status !== 'DONE' ? 'destructive' : 'outline'}
                  >
                    {dayjs(task.dueDate).format('MMM D')}
                  </Badge>
                ) : (
                  <span className="text-sm text-muted-foreground">-</span>
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
                          onClick={() => onAddSubTask(task)} 
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
                  <Button variant="ghost" size="icon-sm" onClick={() => onEdit(task)} aria-label={`Edit task: ${task.title}`}>
                    <Pencil className="h-4 w-4" aria-hidden="true" />
                  </Button>
                  <Button variant="ghost" size="icon-sm" onClick={() => onDelete(task.id)} className="text-destructive hover:text-destructive" aria-label={`Delete task: ${task.title}`}>
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      
      {/* Pagination */}
      <div className="flex items-center justify-between px-4 py-3 border-t">
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <span>Rows per page:</span>
          <Select value={String(rowsPerPage)} onValueChange={onRowsPerPageChange}>
            <SelectTrigger className="h-8 w-[70px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="5">5</SelectItem>
              <SelectItem value="10">10</SelectItem>
              <SelectItem value="25">25</SelectItem>
              <SelectItem value="50">50</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-sm text-muted-foreground">
            {startItem}-{endItem} of {totalCount}
          </span>
          <div className="flex items-center gap-1">
            <Button
              variant="outline"
              size="icon-sm"
              onClick={() => onPageChange(page - 1)}
              disabled={page === 0}
              aria-label="Go to previous page"
            >
              <ChevronLeft className="h-4 w-4" aria-hidden="true" />
            </Button>
            <Button
              variant="outline"
              size="icon-sm"
              onClick={() => onPageChange(page + 1)}
              disabled={page >= totalPages - 1}
              aria-label="Go to next page"
            >
              <ChevronRight className="h-4 w-4" aria-hidden="true" />
            </Button>
          </div>
        </div>
      </div>
    </Card>
  );
}
