import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Paperclip } from 'lucide-react';
import { useProject } from '../contexts';
import { MediaAttachmentUpload } from './MediaAttachmentUpload';
import {
  BugReport,
  CreateBugReportRequest,
  UpdateBugReportRequest,
  BugSeverity,
  BugStatus,
  Task,
  Person,
  Release,
} from '../types';
import { taskService } from '../services/taskService';
import { releaseService } from '../services/releaseService';
import { documentService } from '../services/documentService';
import api from '../services/api';
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
import { Combobox } from './ui/combobox';
import { Badge } from './ui/badge';
import { Alert, AlertDescription } from './ui/alert';

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
  const { currentProject } = useProject();
  const isEdit = !!bugReport;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tagInput, setTagInput] = useState('');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [people, setPeople] = useState<Person[]>([]);
  const [releases, setReleases] = useState<Release[]>([]);
  const [loadingPeople, setLoadingPeople] = useState(false);
  const [pendingAttachments, setPendingAttachments] = useState<File[]>([]);

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
        assigneeId: bugReport?.assigneeId,
        taskId: bugReport?.taskId,
        targetReleaseId: bugReport?.targetReleaseId,
      });
      setTagInput('');
      setError(null);
      setPendingAttachments([]);
      loadPeople();
      loadReleases();
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

  // Load releases for target release selection
  const loadReleases = async () => {
    if (!currentProject?.id) return;
    try {
      const response = await releaseService.getByProject(currentProject.id);
      setReleases(response.data);
    } catch (err) {
      console.error('Failed to load releases:', err);
      setReleases([]);
    }
  };

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
        // Create the bug first
        const createdBug = await onSubmit(formData as CreateBugReportRequest);
        
        // Upload any pending attachments if bug was created successfully
        if (pendingAttachments.length > 0 && createdBug && typeof createdBug === 'object' && 'id' in createdBug) {
          const bugId = (createdBug as BugReport).id;
          try {
            // Upload all pending attachments
            await Promise.all(
              pendingAttachments.map(file => documentService.uploadBugAttachment(bugId, file))
            );
          } catch (uploadErr) {
            console.error('Failed to upload attachments:', uploadErr);
            // Don't fail the whole operation if attachment upload fails
            // The bug was created successfully, user can add attachments later
          }
        }
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
              <Combobox
                options={severities.map(severity => ({ value: severity, label: severity }))}
                value={formData.severity}
                onValueChange={(value) => handleChange('severity', value)}
                placeholder="Select severity"
              />
            </div>

            {isEdit && (
              <div className="space-y-2">
                <Label>Status</Label>
                <Combobox
                  options={statuses.map(status => ({ value: status, label: status.replace('_', ' ') }))}
                  value={formData.status}
                  onValueChange={(value) => handleChange('status', value)}
                  placeholder="Select status"
                />
              </div>
            )}
          </div>

          <div className="space-y-2">
            <Label>{t('bugReports.assignee')}</Label>
            <Combobox
              options={[
                { value: 'unassigned', label: t('bugReports.unassigned') },
                ...people.map(person => ({ value: person.id.toString(), label: person.name }))
              ]}
              value={formData.assigneeId?.toString() || 'unassigned'}
              onValueChange={(value) => handleChange('assigneeId', value === 'unassigned' ? undefined : Number(value))}
              placeholder={t('bugReports.selectAssignee')}
              searchPlaceholder="Search persons..."
            />
          </div>

          {/* Task Traceability */}
          <div className="space-y-2">
            <Label>Related Task (optional)</Label>
            <Combobox
              options={[
                { value: 'none', label: 'No related task' },
                ...tasks.slice(0, 50).map(task => ({ value: String(task.id), label: task.title }))
              ]}
              value={formData.taskId ? String(formData.taskId) : 'none'}
              onValueChange={(value) => handleChange('taskId', value === 'none' ? undefined : Number(value))}
              placeholder={cycleId && tasks.length === 0 ? "Loading tasks..." : "No related task"}
              searchPlaceholder="Search tasks..."
              emptyText={cycleId ? "No tasks found" : "Select a cycle first"}
            />
            <p className="text-xs text-muted-foreground">
              {cycleId ? `Link to the task that caused or needs to fix this bug (${tasks.length} available)` : 'Select a cycle to see available tasks'}
            </p>
          </div>
          {/* Target Release */}
          {releases.length > 0 && (
            <div className="space-y-2">
              <Label>{t('bugReports.targetRelease', 'Target Release')}</Label>
              <Combobox
                options={[
                  { value: 'none', label: t('bugReports.noRelease', 'No target release') },
                  ...releases.map(r => ({ value: String(r.id), label: `${r.name} (${r.version})` }))
                ]}
                value={formData.targetReleaseId ? String(formData.targetReleaseId) : 'none'}
                onValueChange={(value) => handleChange('targetReleaseId', value === 'none' ? undefined : Number(value))}
                placeholder={t('bugReports.selectRelease', 'Select target release')}
                searchPlaceholder={t('bugReports.searchRelease', 'Search releases...')}
              />
              <p className="text-xs text-muted-foreground">
                {t('bugReports.targetReleaseHint', 'Associate this bug with a release for tracking')}
              </p>
            </div>
          )}
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

          {/* Attachments Section */}
          <div className="space-y-2">
            <Label className="flex items-center gap-2">
              <Paperclip className="h-4 w-4" />
              {t('bugAttachments.title')}
            </Label>
            {isEdit && bugReport?.id ? (
              <MediaAttachmentUpload
                bugId={bugReport.id}
                disabled={loading}
                compact
              />
            ) : (
              <MediaAttachmentUpload
                bugId={null}
                disabled={loading}
                compact
                pendingFiles={pendingAttachments}
                onPendingFilesChange={setPendingAttachments}
              />
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
