import React, { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Paperclip } from 'lucide-react';
import { useProject, useAuth } from '../contexts';
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
  Pitch,
} from '../types';
import { taskService } from '../services/taskService';
import { releaseService } from '../services/releaseService';
import { pitchService } from '../services/pitchService';
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
import MarkdownEditor from './MarkdownEditor';
import { Badge } from './ui/badge';
import { Checkbox } from './ui/checkbox';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from './ui/popover';
import { Alert, AlertDescription } from './ui/alert';

interface BugReportModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateBugReportRequest | UpdateBugReportRequest) => Promise<BugReport | void | undefined>;
  bugReport?: BugReport | null;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  testRunId?: number;
}

const COMPONENT_OPTIONS = [
  'Frontend',
  'Backend',
  'Mobile (iOS)',
  'Mobile (Android)',
  'API',
  'Database',
  'Authentication',
  'Infrastructure',
  'Other',
];

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
  const { currentProject, isScrumProject } = useProject();
  const { user } = useAuth();
  const isEdit = !!bugReport;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const errorRef = useRef<HTMLDivElement>(null);
  const [tagInput, setTagInput] = useState('');
  const [tasks, setTasks] = useState<Task[]>([]);
  const [people, setPeople] = useState<Person[]>([]);
  const [releases, setReleases] = useState<Release[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [loadingPeople, setLoadingPeople] = useState(false);
  const [pendingAttachments, setPendingAttachments] = useState<File[]>([]);

  const [formData, setFormData] = useState<Partial<CreateBugReportRequest>>({});
  // Parsed multi-component list (stored as comma-separated in formData.component)
  const selectedComponents: string[] = (formData.component || '').split(',').map(s => s.trim()).filter(Boolean);
  const toggleComponent = (c: string) => {
    const next = selectedComponents.includes(c)
      ? selectedComponents.filter(s => s !== c)
      : [...selectedComponents, c];
    handleChange('component', next.join(',') || undefined);
  };

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
        component: bugReport?.component || '',
        severity: bugReport?.severity || 'MAJOR',
        status: bugReport?.status || 'OPEN',
        tags: bugReport?.tagList || [],
        projectId: bugReport?.projectId || currentProject?.id,
        pitchId: bugReport?.pitchId || pitchId,
        cycleId: bugReport?.cycleId || cycleId,
        teamId: bugReport?.teamId || teamId,
        testRunId: bugReport?.testRunId || testRunId,
        assigneeId: bugReport?.assigneeId,
        qaAssigneeId: bugReport?.qaAssigneeId,
        taskId: bugReport?.taskId,
        targetReleaseId: bugReport?.targetReleaseId,
      });
      setTagInput('');
      setError(null);
      setPendingAttachments([]);
      loadPeople();
      loadReleases();
      loadPitches();
    }
  }, [open, bugReport, pitchId, cycleId, teamId, testRunId, currentProject?.id]);

  useEffect(() => {
    if (error) {
      errorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }, [error]);

  const loadPeople = async () => {
    if (loadingPeople) return;
    setLoadingPeople(true);
    try {
      const response = await api.get<Person[]>('/persons');
      const active = response.data.filter(person => person.isActive);
      // Pin the logged-in user's person to the top
      const currentPersonId = user?.personId;
      if (currentPersonId) {
        active.sort((a, b) => {
          if (a.id === currentPersonId) return -1;
          if (b.id === currentPersonId) return 1;
          return 0;
        });
      }
      setPeople(active);
    } catch (err) {
      console.error('Failed to load people:', err);
    } finally {
      setLoadingPeople(false);
    }
  };

  const loadReleases = async () => {
    if (!currentProject?.id) return;
    try {
      const response = await releaseService.getByProject(currentProject.id);
      setReleases(response.data);
    } catch (err) {
      setReleases([]);
    }
  };

  const loadPitches = async () => {
    if (!currentProject?.id) return;
    try {
      const response = await pitchService.getMyPitches();
      // Filter pitches to those belonging to cycles in the current project
      setPitches(response.data);
    } catch (err) {
      setPitches([]);
    }
  };

  // Load initial tasks based on cycle context
  useEffect(() => {
    const loadTasks = async () => {
      if (!open) return;
      try {
        if (cycleId) {
          const tasksRes = await taskService.getByCycleId(cycleId, 0, 50, 'createdAt', 'desc');
          const taskData = Array.isArray(tasksRes.data) ? tasksRes.data : tasksRes.data.content;
          setTasks(taskData);
        } else {
          setTasks([]);
        }
      } catch (err) {
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
      setError(t('bugReports.form.titleRequired', 'Title is required'));
      return;
    }
    if (!formData.description?.trim()) {
      setError(t('bugReports.form.descriptionRequired', 'Description is required'));
      return;
    }
    if (!formData.severity) {
      setError(t('bugReports.form.severityRequired', 'Severity is required'));
      return;
    }

    setLoading(true);
    setError(null);

    try {
      if (isEdit) {
        await onSubmit(formData as UpdateBugReportRequest);
      } else {
        const createdBug = await onSubmit(formData as CreateBugReportRequest);
        if (pendingAttachments.length > 0 && createdBug && typeof createdBug === 'object' && 'id' in createdBug) {
          const bugId = (createdBug as BugReport).id;
          try {
            await Promise.all(
              pendingAttachments.map(file => documentService.uploadBugAttachment(bugId, file))
            );
          } catch (uploadErr) {
            console.error('Failed to upload attachments:', uploadErr);
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

  const pitchLabel = isScrumProject
    ? t('bugReports.form.linkedStory', 'Linked Story / Epic')
    : t('bugReports.form.linkedPitch', 'Linked Pitch');
  const pitchPlaceholder = isScrumProject
    ? t('bugReports.form.selectStory', 'Select story or epic')
    : t('bugReports.form.selectPitch', 'Select pitch');

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {isEdit
              ? `${t('bugReports.form.editTitle', 'Edit Bug')} — ${bugReport.bugKey}`
              : t('bugReports.reportBug', 'Report New Bug')}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-3 py-2">
          {error && (
            <div ref={errorRef}>
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            </div>
          )}

          {/* Title */}
          <div className="space-y-1">
            <Label htmlFor="bug-title">{t('bugReports.form.title', 'Title')} *</Label>
            <Input
              id="bug-title"
              value={formData.title}
              onChange={(e) => handleChange('title', e.target.value)}
              placeholder={t('bugReports.form.titlePlaceholder', 'Brief summary of the bug')}
            />
          </div>

          {/* Severity / Status / Component row */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <div className="space-y-1">
              <Label>{t('bugReports.form.severity', 'Severity')} *</Label>
              <Combobox
                options={severities.map(s => ({ value: s, label: s }))}
                value={formData.severity}
                onValueChange={(value) => handleChange('severity', value)}
                placeholder={t('bugReports.form.selectSeverity', 'Select severity')}
              />
            </div>

            {isEdit && (
              <div className="space-y-1">
                <Label>{t('bugReports.form.status', 'Status')}</Label>
                <Combobox
                  options={statuses.map(s => ({ value: s, label: s.replace('_', ' ') }))}
                  value={formData.status}
                  onValueChange={(value) => handleChange('status', value)}
                  placeholder={t('bugReports.form.selectStatus', 'Select status')}
                />
              </div>
            )}

            <div className="space-y-1">
              <Label>{t('bugReports.component', 'Component')}</Label>
              <Popover>
                <PopoverTrigger asChild>
                  <button
                    type="button"
                    className="w-full min-h-[2.25rem] flex flex-wrap gap-1 items-center px-3 py-1.5 text-sm border rounded-md bg-background hover:bg-accent/30 text-left"
                  >
                    {selectedComponents.length === 0 ? (
                      <span className="text-muted-foreground">{t('bugReports.selectComponent', 'Select component(s)')}</span>
                    ) : selectedComponents.map(c => (
                      <Badge key={c} variant="secondary" className="text-xs">{c}</Badge>
                    ))}
                  </button>
                </PopoverTrigger>
                <PopoverContent className="w-56 p-2" align="start">
                  <div className="space-y-1">
                    {COMPONENT_OPTIONS.map(c => (
                      <label key={c} className="flex items-center gap-2 px-2 py-1.5 rounded cursor-pointer hover:bg-accent/40 text-sm">
                        <Checkbox
                          checked={selectedComponents.includes(c)}
                          onCheckedChange={() => toggleComponent(c)}
                        />
                        {c}
                      </label>
                    ))}
                  </div>
                </PopoverContent>
              </Popover>
            </div>
          </div>

          {/* Assignee / Linked Pitch row */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label>{t('bugReports.assignee', 'Assignee')}</Label>
              <Combobox
                options={[
                  { value: 'unassigned', label: t('bugReports.unassigned', 'Unassigned') },
                  ...people.map(p => ({ value: p.id.toString(), label: p.name })),
                ]}
                value={formData.assigneeId?.toString() || 'unassigned'}
                onValueChange={(value) => handleChange('assigneeId', value === 'unassigned' ? undefined : Number(value))}
                placeholder={t('bugReports.selectAssignee', 'Select assignee')}
                searchPlaceholder={t('bugReports.searchPerson', 'Search persons...')}
              />
            </div>

            <div className="space-y-1">
              <Label>{t('bugs.qaAssignee', 'QA Tester')}</Label>
              <Combobox
                options={[
                  { value: 'unassigned', label: t('bugReports.unassigned', 'Unassigned') },
                  ...people.map(p => ({ value: p.id.toString(), label: p.name })),
                ]}
                value={formData.qaAssigneeId?.toString() || 'unassigned'}
                onValueChange={(value) => handleChange('qaAssigneeId', value === 'unassigned' ? undefined : Number(value))}
                placeholder={t('bugReports.selectQaAssignee', 'Select QA tester')}
                searchPlaceholder={t('bugReports.searchPerson', 'Search persons...')}
              />
            </div>

            <div className="space-y-1">
              <Label>{pitchLabel}</Label>
              <Combobox
                options={[
                  { value: 'none', label: t('bugReports.form.noPitch', 'No linked pitch') },
                  ...pitches.map(p => ({ value: String(p.id), label: p.title })),
                ]}
                value={formData.pitchId ? String(formData.pitchId) : 'none'}
                onValueChange={(value) => handleChange('pitchId', value === 'none' ? undefined : Number(value))}
                placeholder={pitchPlaceholder}
                searchPlaceholder={t('bugReports.form.searchPitch', 'Search...')}
              />
            </div>
          </div>

          {/* Description */}
          <div className="space-y-1">
            <Label htmlFor="bug-description">{t('bugReports.form.description', 'Description')} *</Label>
            <MarkdownEditor
              id="bug-description"
              value={formData.description}
              onChange={(value) => handleChange('description', value)}
              placeholder={t('bugReports.form.descriptionPlaceholder', 'Detailed description of the bug')}
              rows={4}
            />
          </div>

          {/* Steps / Expected / Actual */}
          <div className="space-y-1">
            <Label htmlFor="steps-to-reproduce">{t('bugReports.form.stepsToReproduce', 'Steps to Reproduce')}</Label>
            <Textarea
              id="steps-to-reproduce"
              value={formData.stepsToReproduce}
              onChange={(e) => handleChange('stepsToReproduce', e.target.value)}
              placeholder={`1. Go to...\n2. Click on...\n3. See error`}
              rows={3}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label htmlFor="expected-behavior">{t('bugReports.form.expectedBehavior', 'Expected Behavior')}</Label>
              <Textarea
                id="expected-behavior"
                value={formData.expectedBehavior}
                onChange={(e) => handleChange('expectedBehavior', e.target.value)}
                placeholder={t('bugReports.form.expectedPlaceholder', 'What should happen')}
                rows={2}
              />
            </div>
            <div className="space-y-1">
              <Label htmlFor="actual-behavior">{t('bugReports.form.actualBehavior', 'Actual Behavior')}</Label>
              <Textarea
                id="actual-behavior"
                value={formData.actualBehavior}
                onChange={(e) => handleChange('actualBehavior', e.target.value)}
                placeholder={t('bugReports.form.actualPlaceholder', 'What actually happens')}
                rows={2}
              />
            </div>
          </div>

          {/* Environment / Related Task */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label htmlFor="environment">{t('bugReports.form.environment', 'Environment')}</Label>
              <Input
                id="environment"
                value={formData.environment}
                onChange={(e) => handleChange('environment', e.target.value)}
                placeholder={t('bugReports.form.environmentPlaceholder', 'Browser, OS, version, device…')}
              />
            </div>

            <div className="space-y-1">
              <Label>{t('bugReports.form.relatedTask', 'Related Task')}</Label>
              <Combobox
                options={[
                  { value: 'none', label: t('bugReports.form.noRelatedTask', 'No related task') },
                  ...tasks.slice(0, 50).map(task => ({ value: String(task.id), label: task.title })),
                ]}
                value={formData.taskId ? String(formData.taskId) : 'none'}
                onValueChange={(value) => handleChange('taskId', value === 'none' ? undefined : Number(value))}
                placeholder={cycleId && tasks.length === 0 ? t('common.loading', 'Loading…') : t('bugReports.form.noRelatedTask', 'No related task')}
                searchPlaceholder={t('bugReports.form.searchTask', 'Search tasks...')}
                emptyText={cycleId ? t('bugReports.form.noTasksFound', 'No tasks found') : t('bugReports.form.selectCycleFirst', 'Select a cycle first')}
              />
            </div>
          </div>

          {/* Target Release */}
          {releases.length > 0 && (
            <div className="space-y-1">
              <Label>{t('bugReports.targetRelease', 'Target Release')}</Label>
              <Combobox
                options={[
                  { value: 'none', label: t('bugReports.noRelease', 'No target release') },
                  ...releases.map(r => ({ value: String(r.id), label: `${r.name} (${r.version})` })),
                ]}
                value={formData.targetReleaseId ? String(formData.targetReleaseId) : 'none'}
                onValueChange={(value) => handleChange('targetReleaseId', value === 'none' ? undefined : Number(value))}
                placeholder={t('bugReports.selectRelease', 'Select target release')}
                searchPlaceholder={t('bugReports.searchRelease', 'Search releases...')}
              />
            </div>
          )}

          {/* Tags */}
          <div className="space-y-1">
            <Label htmlFor="tags">{t('bugReports.form.tags', 'Tags')}</Label>
            <Input
              id="tags"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyDown={handleAddTag}
              placeholder={t('bugReports.form.tagsPlaceholder', 'Add tags (press Enter)')}
            />
            {formData.tags && formData.tags.length > 0 && (
              <div className="flex flex-wrap gap-1 mt-1">
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

          {/* Attachments */}
          <div className="space-y-1">
            <Label className="flex items-center gap-2">
              <Paperclip className="h-4 w-4" />
              {t('bugAttachments.title')}
            </Label>
            {isEdit && bugReport?.id ? (
              <MediaAttachmentUpload bugId={bugReport.id} disabled={loading} compact />
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
            <div className="space-y-1">
              <Label htmlFor="resolution">{t('bugReports.form.resolution', 'Resolution')}</Label>
              <Textarea
                id="resolution"
                value={(formData as UpdateBugReportRequest).resolution || ''}
                onChange={(e) => handleChange('resolution' as keyof CreateBugReportRequest, e.target.value)}
                placeholder={t('bugReports.form.resolutionPlaceholder', 'How was this bug fixed?')}
                rows={2}
              />
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={loading}>
            {t('common.cancel', 'Cancel')}
          </Button>
          <Button onClick={handleSubmit} disabled={loading}>
            {loading
              ? t('common.saving', 'Saving…')
              : isEdit
                ? t('common.update', 'Update')
                : t('common.create', 'Create')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default BugReportModal;
