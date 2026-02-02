import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, ChevronDown } from 'lucide-react';
import { useProject } from '../contexts';
import {
  BugReport,
  CreateBugReportRequest,
  UpdateBugReportRequest,
  BugSeverity,
  BugStatus,
  HillChartPoint,
  Task,
  User,
  Person,
} from '../types';
import { hillChartApi } from '../services/hillChartApi';
import { taskService } from '../services/taskService';
import api from '../services/api';
import { useDebounce } from '../hooks/useDebounce';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from './ui/dialog';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from './ui/command';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from './ui/popover';
import { Badge } from './ui/badge';
import { Alert, AlertDescription } from './ui/alert';
import { cn } from '../lib/utils';

interface BugReportModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateBugReportRequest | UpdateBugReportRequest) => Promise<void>;
  bugReport?: BugReport | null;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  testRunId?: number;
}

const severities: BugSeverity[] = ['TRIVIAL', 'MINOR', 'MAJOR', 'CRITICAL', 'BLOCKER'];
const statuses: BugStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED', 'REOPENED', 'WONT_FIX', 'DUPLICATE'];

const severityVariants: Record<BugSeverity, string> = {
  TRIVIAL: 'bg-zinc-500/20 text-zinc-400 border-zinc-500/30',
  MINOR: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  MAJOR: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  CRITICAL: 'bg-red-500/20 text-red-400 border-red-500/30',
  BLOCKER: 'bg-red-600/20 text-red-500 border-red-600/30',
};

const statusVariants: Record<BugStatus, string> = {
  OPEN: 'bg-red-500/20 text-red-400 border-red-500/30',
  IN_PROGRESS: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  RESOLVED: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  VERIFIED: 'bg-green-500/20 text-green-400 border-green-500/30',
  CLOSED: 'bg-zinc-500/20 text-zinc-400 border-zinc-500/30',
  REOPENED: 'bg-red-500/20 text-red-400 border-red-500/30',
  WONT_FIX: 'bg-zinc-500/20 text-zinc-400 border-zinc-500/30',
  DUPLICATE: 'bg-zinc-500/20 text-zinc-400 border-zinc-500/30',
};

const BugReportModal: React.FC<BugReportModalProps> = ({
  open,
  onClose,
  onSubmit,
  bugReport,
  pitchId,
  cycleId,
  teamId,
  testRunId,
}) => {
  const { t } = useTranslation();
  const { isKanbanProject, currentProject } = useProject();
  const isEdit = !!bugReport;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tagInput, setTagInput] = useState('');
  const [scopes, setScopes] = useState<HillChartPoint[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [people, setPeople] = useState<Person[]>([]);
  const [scopeSearch, setScopeSearch] = useState('');
  const [taskSearch, setTaskSearch] = useState('');
  const [personSearch, setPersonSearch] = useState('');
  const [assigneePopoverOpen, setAssigneePopoverOpen] = useState(false);
  const [searchingScopes, setSearchingScopes] = useState(false);
  const [searchingTasks, setSearchingTasks] = useState(false);
  const [loadingPeople, setLoadingPeople] = useState(false);
  
  // Debounce search queries to avoid excessive API calls (uses useDebounce default delay of 300ms defined in the hook)
  const debouncedScopeSearch = useDebounce(scopeSearch);
  const debouncedTaskSearch = useDebounce(taskSearch);

  const [formData, setFormData] = useState<Partial<CreateBugReportRequest>>({});

  // Reset form data when modal opens or bug report changes
  useEffect(() => {
    if (open) {
      setFormData({
        title: bugReport?.title || '',
        description: bugReport?.description || '',
        stepsToReproduce: bugReport?.stepsToReproduce || '',
        expectedBehavior: bugReport?.expectedBehavior || '',
        actualBehavior: bugReport?.actualBehavior || '',
        environment: bugReport?.environment || '',
        severity: bugReport?.severity || 'MAJOR',
        status: bugReport?.status || 'OPEN',
        tags: bugReport?.tagList || [],
        // Auto-set projectId from current project context
        projectId: bugReport?.projectId || currentProject?.id,
        pitchId: bugReport?.pitchId || pitchId,
        cycleId: bugReport?.cycleId || cycleId,
        teamId: bugReport?.teamId || teamId,
        testRunId: bugReport?.testRunId || testRunId,
        assigneePersonId: bugReport?.assigneePersonId,
        scopeId: bugReport?.scopeId,
        taskId: bugReport?.taskId,
      });
      setTagInput('');
      setError(null);
      loadPeople();
    }
  }, [open, bugReport, pitchId, cycleId, teamId, testRunId, currentProject?.id]);

  // Load people for assignee selection
  const loadPeople = async () => {
    if (loadingPeople) return;
    setLoadingPeople(true);
    try {
      const response = await api.get<Person[]>('/persons');
      setPeople(response.data.filter(person => person.isActive));
    } catch (err) {
      console.error('Failed to load people:', err);
    } finally {
      setLoadingPeople(false);
    }
  };

  // Load initial scopes based on pitch context
  useEffect(() => {
    const loadScopes = async () => {
      if (!open) return;
      
      try {
        if (pitchId) {
          // Load scopes for specific pitch
          const scopesRes = await hillChartApi.getHillChartPointsByPitch(pitchId);
          setScopes(scopesRes);
        } else {
          // No pitch context - start with empty, user must search
          setScopes([]);
        }
      } catch (err) {
        console.error('Failed to load scopes:', err);
        setScopes([]);
      }
    };
    loadScopes();
  }, [open, pitchId]);

  // Load initial tasks based on cycle context
  useEffect(() => {
    const loadTasks = async () => {
      if (!open) return;
      
      try {
        if (cycleId) {
          // Load a limited number of tasks for specific cycle to avoid excessive upfront loading
          const tasksRes = await taskService.getByCycleId(cycleId, 0, 50, 'createdAt', 'desc');
          const taskData = Array.isArray(tasksRes.data) ? tasksRes.data : tasksRes.data.content;
          setTasks(taskData);
        } else {
          // No cycle context - start with empty, user must search
          setTasks([]);
        }
      } catch (err) {
        console.error('Failed to load tasks:', err);
        setTasks([]);
      }
    };
    loadTasks();
  }, [open, cycleId]);

  // Server-side search for scopes (minimum 3 characters)
  useEffect(() => {
    const searchScopes = async () => {
      if (!open || debouncedScopeSearch.length < 3) {
        if (!pitchId && debouncedScopeSearch.length > 0 && debouncedScopeSearch.length < 3) {
          // Show message that more characters needed
          setScopes([]);
        }
        return;
      }
      
      setSearchingScopes(true);
      try {
        const scopesRes = await hillChartApi.searchHillChartPoints(debouncedScopeSearch);
        setScopes(scopesRes);
      } catch (err) {
        console.error('Failed to search scopes:', err);
        setScopes([]);
      } finally {
        setSearchingScopes(false);
      }
    };
    searchScopes();
  }, [open, debouncedScopeSearch, pitchId]);

  // Server-side search for tasks (minimum 3 characters)
  useEffect(() => {
    const searchTasks = async () => {
      if (!open || debouncedTaskSearch.length < 3) {
        if (!cycleId && debouncedTaskSearch.length > 0 && debouncedTaskSearch.length < 3) {
          // Show message that more characters needed
          setTasks([]);
        }
        return;
      }
      
      setSearchingTasks(true);
      try {
        const tasksRes = await taskService.search(debouncedTaskSearch, 0, 50);
        setTasks(tasksRes.data.content);
      } catch (err) {
        console.error('Failed to search tasks:', err);
        setTasks([]);
      } finally {
        setSearchingTasks(false);
      }
    };
    searchTasks();
  }, [open, debouncedTaskSearch, cycleId]);

  const handleChange = (field: keyof CreateBugReportRequest, value: unknown) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleAddTag = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && tagInput.trim()) {
      e.preventDefault();
      const newTag = tagInput.trim();
      if (!formData.tags?.includes(newTag)) {
        handleChange('tags', [...(formData.tags || []), newTag]);
      }
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    handleChange('tags', formData.tags?.filter((tag) => tag !== tagToRemove) || []);
  };

  const handleSubmit = async () => {
    if (!formData.title?.trim()) {
      setError('Title is required');
      return;
    }
    if (!formData.description?.trim()) {
      setError('Description is required');
      return;
    }
    if (!formData.severity) {
      setError('Severity is required');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (isEdit) {
        await onSubmit(formData as UpdateBugReportRequest);
      } else {
        await onSubmit(formData as CreateBugReportRequest);
      }
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : t('errors.saveBugReportFailed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? `Edit Bug Report: ${bugReport.bugKey}` : 'Report New Bug'}
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-4 py-4">
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* Title */}
          <div className="space-y-2">
            <Label htmlFor="bug-title">Title *</Label>
            <Input
              id="bug-title"
              value={formData.title}
              onChange={(e) => handleChange('title', e.target.value)}
              placeholder="Brief summary of the bug"
            />
            <p className="text-xs text-muted-foreground">Brief summary of the bug</p>
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="bug-description">Description *</Label>
            <Textarea
              id="bug-description"
              value={formData.description}
              onChange={(e) => handleChange('description', e.target.value)}
              placeholder="Detailed description of the bug (Markdown supported)"
              rows={4}
            />
            <p className="text-xs text-muted-foreground">Detailed description (Markdown supported)</p>
          </div>

          {/* Severity & Status */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Severity *</Label>
              <Select
                value={formData.severity}
                onValueChange={(value) => handleChange('severity', value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select severity" />
                </SelectTrigger>
                <SelectContent>
                  {severities.map((severity) => (
                    <SelectItem key={severity} value={severity}>
                      <Badge variant="outline" className={cn('mr-2', severityVariants[severity])}>
                        {severity}
                      </Badge>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {isEdit && (
              <div className="space-y-2">
                <Label>Status</Label>
                <Select
                  value={formData.status}
                  onValueChange={(value) => handleChange('status', value)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    {statuses.map((status) => (
                      <SelectItem key={status} value={status}>
                        <Badge variant="outline" className={cn('mr-2', statusVariants[status])}>
                          {status.replace('_', ' ')}
                        </Badge>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </div>

          {/* Assignee Selection */}
          <div className="space-y-2">
            <Label>Assignee</Label>
            <Popover open={assigneePopoverOpen} onOpenChange={setAssigneePopoverOpen}>
              <PopoverTrigger asChild>
                <Button
                  variant="outline"
                  role="combobox"
                  aria-expanded={assigneePopoverOpen}
                  className="w-full justify-between"
                >
                  {formData.assigneePersonId 
                    ? people.find(person => person.id === formData.assigneePersonId)?.name
                    : "Select assignee (optional)"}
                  <ChevronDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-full p-0" align="start">
                <Command>
                  <CommandInput 
                    placeholder="Search people..." 
                    value={personSearch}
                    onValueChange={setPersonSearch}
                  />
                  <CommandList>
                    <CommandEmpty>No people found.</CommandEmpty>
                    <CommandGroup>
                      <CommandItem
                        value="none"
                        onSelect={() => {
                          handleChange('assigneePersonId', undefined);
                          setAssigneePopoverOpen(false);
                        }}
                      >
                        Unassigned
                      </CommandItem>
                      {people
                        .filter(person => 
                          !personSearch || 
                          person.name.toLowerCase().includes(personSearch.toLowerCase()) ||
                          person.email.toLowerCase().includes(personSearch.toLowerCase())
                        )
                        .map((person) => (
                        <CommandItem
                          key={person.id}
                          value={`${person.id}-${person.name}`}
                          onSelect={() => {
                            handleChange('assigneePersonId', person.id);
                            setAssigneePopoverOpen(false);
                          }}
                        >
                          {person.name}
                          {person.email && (
                            <span className="ml-2 text-xs text-muted-foreground">({person.email})</span>
                          )}
                        </CommandItem>
                      ))}
                    </CommandGroup>
                  </CommandList>
                </Command>
              </PopoverContent>
            </Popover>
          </div>

          {/* Scope & Task Traceability */}
          <div className={`grid grid-cols-1 ${!isKanbanProject ? 'sm:grid-cols-2' : ''} gap-4`}>
            {/* Hide scope field for Kanban projects - scope is a Shape Up concept tied to pitches */}
            {!isKanbanProject && (
              <div className="space-y-2">
                <Label>Scope (optional)</Label>
                <Select
                  value={formData.scopeId ? String(formData.scopeId) : 'none'}
                  onValueChange={(value) => handleChange('scopeId', value === 'none' ? undefined : Number(value))}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={pitchId && scopes.length === 0 ? "Loading scopes..." : "No specific scope"} />
                  </SelectTrigger>
                  <SelectContent>
                    <div className="px-2 pb-2">
                      <Input
                        placeholder="Search scopes..."
                        value={scopeSearch}
                        onChange={(e) => setScopeSearch(e.target.value)}
                        className="h-8"
                      />
                    </div>
                    <SelectItem value="none">No specific scope</SelectItem>
                    {searchingScopes ? (
                      <div className="py-6 text-center text-sm text-muted-foreground">Searching...</div>
                    ) : !pitchId && scopeSearch.length > 0 && scopeSearch.length < 3 ? (
                      <div className="py-6 text-center text-sm text-muted-foreground">Type at least 3 characters to search</div>
                    ) : scopes.length === 0 && scopeSearch.length >= 3 ? (
                      <div className="py-6 text-center text-sm text-muted-foreground">No scopes found</div>
                    ) : scopes.length === 0 && !pitchId ? (
                      <div className="py-6 text-center text-sm text-muted-foreground">Type to search scopes</div>
                    ) : (
                      scopes.slice(0, 50).map((scope) => (
                        <SelectItem key={scope.id} value={String(scope.id)}>
                          {scope.scope}
                        </SelectItem>
                      ))
                    )}
                    {scopes.length > 50 && (
                      <div className="py-2 text-center text-xs text-muted-foreground">
                        Showing first 50 of {scopes.length} scopes. Refine your search for more specific results.
                      </div>
                    )}
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">
                  {pitchId ? `Link to a specific scope (${scopes.length} available)` : 'Search to find scopes (min 3 chars)'}
                </p>
              </div>
            )}

            <div className="space-y-2">
              <Label>Related Task (optional)</Label>
              <Select
                value={formData.taskId ? String(formData.taskId) : 'none'}
                onValueChange={(value) => handleChange('taskId', value === 'none' ? undefined : Number(value))}
              >
                <SelectTrigger>
                  <SelectValue placeholder={cycleId && tasks.length === 0 ? "Loading tasks..." : "No related task"} />
                </SelectTrigger>
                <SelectContent>
                  <div className="px-2 pb-2">
                    <Input
                      placeholder="Search tasks..."
                      value={taskSearch}
                      onChange={(e) => setTaskSearch(e.target.value)}
                      className="h-8"
                    />
                  </div>
                  <SelectItem value="none">No related task</SelectItem>
                  {searchingTasks ? (
                    <div className="py-6 text-center text-sm text-muted-foreground">Searching...</div>
                  ) : !cycleId && taskSearch.length > 0 && taskSearch.length < 3 ? (
                    <div className="py-6 text-center text-sm text-muted-foreground">Type at least 3 characters to search</div>
                  ) : tasks.length === 0 && taskSearch.length >= 3 ? (
                    <div className="py-6 text-center text-sm text-muted-foreground">No tasks found</div>
                  ) : tasks.length === 0 && !cycleId ? (
                    <div className="py-6 text-center text-sm text-muted-foreground">Type to search tasks</div>
                  ) : (
                    tasks.slice(0, 50).map((task) => (
                      <SelectItem key={task.id} value={String(task.id)}>
                        {task.title}
                      </SelectItem>
                    ))
                  )}
                  {tasks.length > 50 && (
                    <div className="py-2 text-center text-xs text-muted-foreground">
                      Showing first 50 of {tasks.length} tasks. Refine your search for more specific results.
                    </div>
                  )}
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                {cycleId ? `Link to the task that caused or needs to fix this bug (${tasks.length} available)` : 'Search to find tasks (min 3 chars)'}
              </p>
            </div>
          </div>

          {/* Steps to Reproduce */}
          <div className="space-y-2">
            <Label htmlFor="steps-to-reproduce">Steps to Reproduce</Label>
            <Textarea
              id="steps-to-reproduce"
              value={formData.stepsToReproduce}
              onChange={(e) => handleChange('stepsToReproduce', e.target.value)}
              placeholder={`1. Go to...\n2. Click on...\n3. See error`}
              rows={3}
            />
          </div>

          {/* Expected & Actual Behavior */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="expected-behavior">Expected Behavior</Label>
              <Textarea
                id="expected-behavior"
                value={formData.expectedBehavior}
                onChange={(e) => handleChange('expectedBehavior', e.target.value)}
                placeholder="What should happen"
                rows={2}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="actual-behavior">Actual Behavior</Label>
              <Textarea
                id="actual-behavior"
                value={formData.actualBehavior}
                onChange={(e) => handleChange('actualBehavior', e.target.value)}
                placeholder="What actually happens"
                rows={2}
              />
            </div>
          </div>

          {/* Environment */}
          <div className="space-y-2">
            <Label htmlFor="environment">Environment</Label>
            <Input
              id="environment"
              value={formData.environment}
              onChange={(e) => handleChange('environment', e.target.value)}
              placeholder="Browser, OS, version, device, etc."
            />
          </div>

          {/* Tags */}
          <div className="space-y-2">
            <Label htmlFor="tags">Tags</Label>
            <Input
              id="tags"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyDown={handleAddTag}
              placeholder="Add tags (press Enter)"
            />
            {formData.tags && formData.tags.length > 0 && (
              <div className="flex flex-wrap gap-1 mt-2">
                {formData.tags.map((tag) => (
                  <Badge key={tag} variant="outline" className="gap-1">
                    {tag}
                    <button
                      type="button"
                      onClick={() => handleRemoveTag(tag)}
                      className="ml-1 hover:text-destructive"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                ))}
              </div>
            )}
          </div>

          {/* Resolution (edit only) */}
          {isEdit && (
            <div className="space-y-2">
              <Label htmlFor="resolution">Resolution</Label>
              <Textarea
                id="resolution"
                value={(formData as UpdateBugReportRequest).resolution || ''}
                onChange={(e) => handleChange('resolution' as keyof CreateBugReportRequest, e.target.value)}
                placeholder="How was this bug fixed?"
                rows={2}
              />
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={loading}>
            {loading ? 'Saving...' : isEdit ? 'Update' : 'Create'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default BugReportModal;
