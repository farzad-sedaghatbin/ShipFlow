import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from './ui/dialog';
import { Button } from './ui/button';
import { Label } from './ui/label';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { Checkbox } from './ui/checkbox';
import { Badge } from './ui/badge';
import { Alert, AlertDescription } from './ui/alert';
import { Separator } from './ui/separator';
import { RadioGroup, RadioGroupItem } from './ui/radio-group';
import { 
  Rocket, 
  Loader2, 
  CheckCircle2,
  Circle,
  Lightbulb,
  ListTodo,
  ExternalLink,
  ClipboardCheck
} from 'lucide-react';
import { RetroItem, Cycle, Pitch, Task, RetroColumnType, Project } from '../types';
import { retroService, ConvertToPitchRequest } from '../services/retroService';
import { cycleService } from '../services/cycleService';
import { taskService } from '../services/taskService';
import { projectService } from '../services/projectService';
import { cn } from '../lib/utils';

type ActionType = 'pitch' | 'task' | 'mark-only';

interface ActOnRetroItemsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  retroId: number;
  retroTitle: string;
  projectId: number;
  items: RetroItem[];
  onActionComplete?: () => void;
}

const columnTypeLabels: Record<RetroColumnType, { label: string; icon: typeof Lightbulb }> = {
  WENT_WELL: { label: 'Went Well', icon: CheckCircle2 },
  DID_NOT_GO_WELL: { label: 'Did Not Go Well', icon: Circle },
  TRY_NEXT: { label: 'Try Next', icon: Rocket },
  ACTIONS: { label: 'Actions', icon: ListTodo },
};

export function ActOnRetroItemsDialog({
  open,
  onOpenChange,
  retroId,
  retroTitle,
  projectId,
  items,
  onActionComplete,
}: ActOnRetroItemsDialogProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [actionType, setActionType] = useState<ActionType>('pitch');
  const [selectedItemIds, setSelectedItemIds] = useState<number[]>([]);
  const [customTitle, setCustomTitle] = useState('');
  const [additionalNotes, setAdditionalNotes] = useState('');
  const [appetiteDays, setAppetiteDays] = useState<number>(1);
  const [targetCycleId, setTargetCycleId] = useState<string>('');
  const [selectedProjectId, setSelectedProjectId] = useState<number>(projectId);
  const [availableProjects, setAvailableProjects] = useState<Project[]>([]);
  const [availableCycles, setAvailableCycles] = useState<Cycle[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdPitch, setCreatedPitch] = useState<Pitch | null>(null);
  const [createdTasks, setCreatedTasks] = useState<Task[]>([]);
  const [markedCount, setMarkedCount] = useState(0);

  // Get actionable items (TRY_NEXT and ACTIONS that aren't merged)
  const actionableItems = items.filter(
    (item) =>
      (item.columnType === 'TRY_NEXT' || item.columnType === 'ACTIONS') &&
      !item.mergedIntoId
  );

  // Load available projects once when dialog opens
  useEffect(() => {
    if (open) {
      projectService.getMyProjects().then((projects) => {
        setAvailableProjects(projects);
      }).catch(() => {/* silently fall back to no project list */});
    }
  }, [open]);

  // Load available cycles whenever the selected project changes
  useEffect(() => {
    if (open && selectedProjectId) {
      setTargetCycleId('');
      cycleService.getByProject(selectedProjectId).then((res) => {
        const now = new Date();
        const futureCycles = res.data.filter((cycle) => {
          const endDate = new Date(cycle.endDate);
          return endDate >= now;
        });
        setAvailableCycles(futureCycles);

        if (futureCycles.length > 0) {
          setTargetCycleId(futureCycles[0].id.toString());
        }
      });
    }
  }, [open, selectedProjectId]);

  // Pre-select all actionable items by default
  useEffect(() => {
    if (open && actionableItems.length > 0) {
      setSelectedItemIds(actionableItems.map((item) => item.id));
    }
  }, [open, items]);

  // Generate default title based on action type
  useEffect(() => {
    if (open && !customTitle) {
      if (actionType === 'pitch') {
        setCustomTitle(`Improvements from: ${retroTitle}`);
      } else if (actionType === 'task') {
        setCustomTitle('');
      }
    }
  }, [open, retroTitle, actionType]);

  const handleItemToggle = (itemId: number) => {
    setSelectedItemIds((prev) =>
      prev.includes(itemId)
        ? prev.filter((id) => id !== itemId)
        : [...prev, itemId]
    );
  };

  const handleSelectAll = () => {
    if (selectedItemIds.length === actionableItems.length) {
      setSelectedItemIds([]);
    } else {
      setSelectedItemIds(actionableItems.map((item) => item.id));
    }
  };

  const handleSubmit = async () => {
    if (selectedItemIds.length === 0) {
      setError(t('retroBoard.actOnItems.noItemsSelected', 'Please select at least one item'));
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      if (actionType === 'pitch') {
        // Convert to pitch
        const request: ConvertToPitchRequest = {
          retroItemIds: selectedItemIds,
          targetCycleId: targetCycleId ? Number(targetCycleId) : undefined,
          targetProjectId: selectedProjectId !== projectId ? selectedProjectId : undefined,
          customTitle: customTitle.trim() || undefined,
          additionalNotes: additionalNotes.trim() || undefined,
          appetiteDays,
        };

        const response = await retroService.convertToPitchDraft(retroId, request);
        setCreatedPitch(response.data);
      } else if (actionType === 'task') {
        // Create tasks for each selected item
        if (!targetCycleId) {
          setError(t('retroBoard.actOnItems.cycleRequired', 'Please select a target cycle for tasks'));
          setSubmitting(false);
          return;
        }
        
        const selectedItems = actionableItems.filter(item => selectedItemIds.includes(item.id));
        const tasks: Task[] = [];
        
        for (const item of selectedItems) {
          const taskTitle = customTitle || item.content.substring(0, 100);
          const taskData = {
            title: taskTitle,
            description: item.content,
            projectId: selectedProjectId,
            cycleId: Number(targetCycleId),
            status: 'TODO' as const,
          };
          
          const taskResponse = await taskService.create(taskData);
          tasks.push(taskResponse.data);
          
          // Mark retro item as acted on
          await retroService.markActedOn(item.id, {
            actedOn: true,
            notes: `Converted to task: ${taskResponse.data.title}`,
          });
        }
        
        setCreatedTasks(tasks);
      } else {
        // Just mark as acted on
        for (const itemId of selectedItemIds) {
          await retroService.markActedOn(itemId, {
            actedOn: true,
            notes: additionalNotes.trim() || 'Marked as acted on',
          });
        }
        setMarkedCount(selectedItemIds.length);
      }
      
      onActionComplete?.();
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to process action';
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewResult = () => {
    if (createdPitch) {
      navigate(`/pitches/${createdPitch.id}`);
    } else if (createdTasks.length > 0) {
      navigate(`/backlog`);
    }
    onOpenChange(false);
  };

  const handleClose = () => {
    onOpenChange(false);
    // Reset state after animation
    setTimeout(() => {
      setActionType('pitch');
      setCreatedPitch(null);
      setCreatedTasks([]);
      setMarkedCount(0);
      setSelectedItemIds([]);
      setCustomTitle('');
      setAdditionalNotes('');
      setAppetiteDays(1);
      setSelectedProjectId(projectId);
      setError(null);
    }, 200);
  };

  // Success view after action is completed
  if (createdPitch || createdTasks.length > 0 || markedCount > 0) {
    return (
      <Dialog open={open} onOpenChange={handleClose}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-green-600">
              <CheckCircle2 className="h-5 w-5" />
              {createdPitch && t('retroBoard.actOnItems.pitchCreated', 'Pitch Draft Created!')}
              {createdTasks.length > 0 && t('retroBoard.actOnItems.tasksCreated', 'Tasks Created!')}
              {markedCount > 0 && !createdPitch && !createdTasks.length && t('retroBoard.actOnItems.itemsMarked', 'Items Marked as Acted On!')}
            </DialogTitle>
            <DialogDescription>
              {createdPitch && t('retroBoard.actOnItems.pitchSuccessDesc', 'Your retro insights have been converted to a pitch draft.')}
              {createdTasks.length > 0 && t('retroBoard.actOnItems.tasksSuccessDesc', `${createdTasks.length} task(s) created from your retro items.`)}
              {markedCount > 0 && !createdPitch && !createdTasks.length && t('retroBoard.actOnItems.markedSuccessDesc', `${markedCount} item(s) marked as acted on.`)}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            {createdPitch && (
              <div className="rounded-lg border bg-muted/50 p-4">
                <h4 className="font-medium">{createdPitch.title}</h4>
                <p className="text-sm text-muted-foreground mt-1">
                  {t('retroBoard.actOnItems.appetite', 'Appetite')}: {createdPitch.appetiteDays} {t('common.days', 'days')}
                </p>
                <p className="text-sm text-muted-foreground">
                  {t('retroBoard.actOnItems.cycle', 'Cycle')}: {createdPitch.cycleName}
                </p>
              </div>
            )}

            {createdTasks.length > 0 && (
              <div className="rounded-lg border bg-muted/50 p-4 max-h-48 overflow-y-auto">
                <p className="font-medium mb-2">{t('retroBoard.actOnItems.createdTasks', 'Created Tasks')}:</p>
                <ul className="space-y-1 text-sm">
                  {createdTasks.map(task => (
                    <li key={task.id} className="flex items-start gap-2">
                      <CheckCircle2 className="h-4 w-4 text-green-600 mt-0.5 flex-shrink-0" />
                      <span>{task.title}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            <Alert>
              <Lightbulb className="h-4 w-4" />
              <AlertDescription>
                {createdPitch && t('retroBoard.actOnItems.pitchNextSteps', 
                  'The pitch is now a draft. You can add more details, rabbit holes, and risks before betting.')}
                {createdTasks.length > 0 && t('retroBoard.actOnItems.tasksNextSteps', 
                  'Tasks are ready to be prioritized and assigned in your backlog.')}
                {markedCount > 0 && !createdPitch && !createdTasks.length && t('retroBoard.actOnItems.markedNextSteps', 
                  'Items have been marked as acted on and will be tracked in retrospective reports.')}
              </AlertDescription>
            </Alert>
          </div>

          <DialogFooter className="gap-2">
            <Button variant="outline" onClick={handleClose}>
              {t('common.close', 'Close')}
            </Button>
            {(createdPitch || createdTasks.length > 0) && (
              <Button onClick={handleViewResult}>
                <ExternalLink className="mr-2 h-4 w-4" />
                {createdPitch && t('retroBoard.actOnItems.viewPitch', 'View Pitch')}
                {createdTasks.length > 0 && t('retroBoard.actOnItems.viewTasks', 'View Tasks')}
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <ClipboardCheck className="h-5 w-5 text-primary" />
            {t('retroBoard.actOnItems.title', 'Act on Retro Items')}
          </DialogTitle>
          <DialogDescription>
            {t('retroBoard.actOnItems.description', 
              'Choose how to act on your retrospective action items: convert to pitch, create tasks, or just mark as completed.')}
          </DialogDescription>
        </DialogHeader>

        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <div className="space-y-6 py-4">
          {/* Action Type Selection */}
          <div className="space-y-3">
            <Label className="text-base font-medium">
              {t('retroBoard.actOnItems.actionType', 'What would you like to do?')}
            </Label>
            <RadioGroup value={actionType} onValueChange={(v: string) => setActionType(v as ActionType)}>
              <div className="flex items-start space-x-3 space-y-0 rounded-md border p-4 hover:bg-accent cursor-pointer">
                <RadioGroupItem value="pitch" id="action-pitch" />
                <Label htmlFor="action-pitch" className="cursor-pointer flex-1">
                  <div className="flex items-center gap-2 font-medium">
                    <Rocket className="h-4 w-4" />
                    {t('retroBoard.actOnItems.convertToPitch', 'Convert to Pitch Draft')}
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">
                    {t('retroBoard.actOnItems.pitchDesc', 'Create a pitch for the next betting table to shape these improvements')}
                  </p>
                </Label>
              </div>
              <div className="flex items-start space-x-3 space-y-0 rounded-md border p-4 hover:bg-accent cursor-pointer">
                <RadioGroupItem value="task" id="action-task" />
                <Label htmlFor="action-task" className="cursor-pointer flex-1">
                  <div className="flex items-center gap-2 font-medium">
                    <ListTodo className="h-4 w-4" />
                    {t('retroBoard.actOnItems.convertToTasks', 'Convert to Tasks')}
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">
                    {t('retroBoard.actOnItems.taskDesc', 'Create individual tasks that can be worked on immediately')}
                  </p>
                </Label>
              </div>
              <div className="flex items-start space-x-3 space-y-0 rounded-md border p-4 hover:bg-accent cursor-pointer">
                <RadioGroupItem value="mark-only" id="action-mark" />
                <Label htmlFor="action-mark" className="cursor-pointer flex-1">
                  <div className="flex items-center gap-2 font-medium">
                    <CheckCircle2 className="h-4 w-4" />
                    {t('retroBoard.actOnItems.markOnly', 'Just Mark as Acted On')}
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">
                    {t('retroBoard.actOnItems.markDesc', 'Mark items as completed without creating new work items')}
                  </p>
                </Label>
              </div>
            </RadioGroup>
          </div>

          <Separator />

          {/* Item Selection */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <Label className="text-base font-medium">
                {t('retroBoard.actOnItems.selectItems', 'Select Items')}
              </Label>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleSelectAll}
                className="text-xs"
              >
                {selectedItemIds.length === actionableItems.length
                  ? t('common.deselectAll', 'Deselect All')
                  : t('common.selectAll', 'Select All')}
              </Button>
            </div>

            {actionableItems.length === 0 ? (
              <Alert>
                <AlertDescription>
                  {t('retroBoard.actOnItems.noActionItems', 
                    'No action items found. Add items to the "Try Next" or "Actions" columns first.')}
                </AlertDescription>
              </Alert>
            ) : (
              <div className="space-y-2 max-h-48 overflow-y-auto rounded-lg border p-3">
                {actionableItems.map((item) => {
                  const config = columnTypeLabels[item.columnType];
                  const Icon = config.icon;
                  return (
                    <div
                      key={item.id}
                      className={cn(
                        'flex items-start gap-3 p-2 rounded-md transition-colors cursor-pointer',
                        selectedItemIds.includes(item.id)
                          ? 'bg-primary/10 border border-primary/20'
                          : 'hover:bg-muted'
                      )}
                      onClick={() => handleItemToggle(item.id)}
                    >
                      <Checkbox
                        checked={selectedItemIds.includes(item.id)}
                        className="mt-0.5"
                      />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm">{item.content}</p>
                        <div className="flex items-center gap-2 mt-1">
                          <Badge variant="outline" className="text-xs">
                            <Icon className="h-3 w-3 mr-1" />
                            {config.label}
                          </Badge>
                          {item.voteCount > 0 && (
                            <Badge variant="secondary" className="text-xs">
                              {item.voteCount} {t('common.votes', 'votes')}
                            </Badge>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
            <p className="text-xs text-muted-foreground">
              {selectedItemIds.length} {t('retroBoard.actOnItems.itemsSelected', 'items selected')}
            </p>
          </div>

          {/* Conditional Fields based on action type */}
          {actionType !== 'mark-only' && (
            <>
              <Separator />
              <div className="space-y-4">
                {actionType === 'pitch' && (
                  <div className="space-y-2">
                    <Label htmlFor="pitch-title">
                      {t('retroBoard.actOnItems.pitchTitle', 'Pitch Title')}
                    </Label>
                    <Input
                      id="pitch-title"
                      value={customTitle}
                      onChange={(e) => setCustomTitle(e.target.value)}
                      placeholder={t('retroBoard.actOnItems.titlePlaceholder', 'Enter a title for the pitch')}
                    />
                  </div>
                )}

                {availableProjects.length > 1 && (
                  <div className="space-y-2">
                    <Label htmlFor="target-project">
                      {t('retroBoard.actOnItems.targetProject', 'Target Project')}
                    </Label>
                    <Select
                      value={selectedProjectId.toString()}
                      onValueChange={(v) => setSelectedProjectId(Number(v))}
                    >
                      <SelectTrigger id="target-project">
                        <SelectValue placeholder={t('retroBoard.actOnItems.selectProject', 'Select a project')} />
                      </SelectTrigger>
                      <SelectContent>
                        {availableProjects.map((project) => (
                          <SelectItem key={project.id} value={project.id.toString()}>
                            {project.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                )}

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="target-cycle">
                      {t('retroBoard.actOnItems.targetCycle', 'Target Cycle')}
                      {actionType === 'task' && <span className="text-destructive ml-1">*</span>}
                      {actionType === 'pitch' && ` (${t('common.optional', 'Optional')})`}
                    </Label>
                    <Select value={targetCycleId} onValueChange={setTargetCycleId}>
                      <SelectTrigger id="target-cycle">
                        <SelectValue placeholder={t('retroBoard.actOnItems.selectCycle', 'Select a cycle')} />
                      </SelectTrigger>
                      <SelectContent>
                        {availableCycles.map((cycle) => (
                          <SelectItem key={cycle.id} value={cycle.id.toString()}>
                            {cycle.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  {actionType === 'pitch' && (
                    <div className="space-y-2">
                      <Label htmlFor="appetite">
                        {t('retroBoard.actOnItems.appetite', 'Appetite')} ({t('common.days', 'days')})
                      </Label>
                      <Select
                        value={appetiteDays.toString()}
                        onValueChange={(v) => setAppetiteDays(Number(v))}
                      >
                        <SelectTrigger id="appetite">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {[1, 2, 3, 5, 10, 15, 20, 30].map((days) => (
                            <SelectItem key={days} value={days.toString()}>
                              {days} {t('common.days', 'days')}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  )}
                </div>
              </div>
            </>
          )}

          {/* Notes field for all types */}
          <div className="space-y-2">
            <Label htmlFor="additional-notes">
              {actionType === 'mark-only' 
                ? t('retroBoard.actOnItems.notes', 'Notes') 
                : t('retroBoard.actOnItems.additionalNotes', 'Additional Notes')} ({t('common.optional', 'Optional')})
            </Label>
            <Textarea
              id="additional-notes"
              value={additionalNotes}
              onChange={(e) => setAdditionalNotes(e.target.value)}
              placeholder={t('retroBoard.actOnItems.notesPlaceholder', 
                'Add any additional context or notes...')}
              rows={3}
            />
          </div>
        </div>

        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={handleClose} disabled={submitting}>
            {t('common.cancel', 'Cancel')}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={
              submitting || 
              selectedItemIds.length === 0 || 
              actionableItems.length === 0 ||
              (actionType === 'task' && !targetCycleId)
            }
          >
            {submitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                {t('common.creating', 'Creating...')}
              </>
            ) : (
              <>
                <CheckCircle2 className="mr-2 h-4 w-4" />
                {actionType === 'pitch' && t('retroBoard.actOnItems.createPitch', 'Create Pitch')}
                {actionType === 'task' && t('retroBoard.actOnItems.createTasks', 'Create Tasks')}
                {actionType === 'mark-only' && t('retroBoard.actOnItems.markItems', 'Mark as Acted On')}
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default ActOnRetroItemsDialog;
