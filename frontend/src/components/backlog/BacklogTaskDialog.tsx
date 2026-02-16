import { useTranslation } from 'react-i18next';
import { Loader2, TrendingUp } from 'lucide-react';
import dayjs, { Dayjs } from 'dayjs';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Combobox } from '../ui/combobox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import { LocalizedDateInput } from '../LocalizedDateInput';
import { Task, Person, TaskStatus, TaskPriority, TaskCategory, Pitch } from '../../types';
import { TaskFormData } from '../../hooks/useBacklogForm';
import { STATUS_OPTIONS, PRIORITY_OPTIONS } from '../../constants/backlogConstants';

export interface BacklogTaskDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editingTask: Task | null;
  formData: TaskFormData;
  dueDate: Dayjs | null;
  fieldErrors: Record<string, string>;
  saving: boolean;
  persons: Person[];
  pitches: Pitch[];
  isKanbanProject: boolean;
  activeCategory: TaskCategory;
  onFormDataChange: (updates: Partial<TaskFormData>) => void;
  onDueDateChange: (date: Dayjs | null) => void;
  onPitchChange: (pitchId: string) => void;
  onSave: () => void;
  onClose: () => void;
}

export function BacklogTaskDialog({
  open,
  onOpenChange,
  editingTask,
  formData,
  dueDate,
  fieldErrors,
  saving,
  persons,
  pitches,
  isKanbanProject,
  activeCategory,
  onFormDataChange,
  onDueDateChange,
  onPitchChange,
  onSave,
  onClose,
}: BacklogTaskDialogProps) {
  const { t } = useTranslation();

  const dialogTitle = editingTask
    ? (formData.parentTaskId ? t('backlogPage.editSubtask') : t('backlogPage.editTask'))
    : formData.parentTaskId
      ? t('backlogPage.createSubtask')
      : t('backlogPage.createTask');

  const dialogDescription = formData.parentTaskId
    ? t('backlogPage.subtaskDescription')
    : activeCategory === 'PITCH_SCOPE'
      ? t('backlogPage.categoryDescription.pitchScope')
      : t('backlogPage.categoryDescription.debtImprovement');

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{dialogTitle}</DialogTitle>
          <DialogDescription>{dialogDescription}</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          {/* Title */}
          <div className="grid gap-2">
            <Label htmlFor="title">{t('backlogPage.title')} *</Label>
            <Input
              id="title"
              value={formData.title}
              onChange={(e) => onFormDataChange({ title: e.target.value })}
              placeholder={formData.parentTaskId ? t('backlogPage.subtaskTitle') : t('backlogPage.taskTitle')}
              className={fieldErrors.title ? 'border-destructive' : ''}
            />
            {fieldErrors.title && (
              <p className="text-sm text-destructive">{fieldErrors.title}</p>
            )}
          </div>

          {/* Description */}
          <div className="grid gap-2">
            <Label htmlFor="description">{t('backlogPage.description')}</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => onFormDataChange({ description: e.target.value })}
              placeholder={formData.parentTaskId ? t('backlogPage.subtaskDescription') : t('backlogPage.taskDescription')}
              rows={3}
            />
          </div>

          {/* Status and Priority */}
          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="status">{t('common.status')}</Label>
              <Combobox
                options={STATUS_OPTIONS.map(status => ({ value: status.value, label: t(status.labelKey) }))}
                value={formData.status}
                onValueChange={(value) => onFormDataChange({ status: value as TaskStatus })}
                placeholder="Select status"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="priority">{t('backlogPage.filters.priority')}</Label>
              <Combobox
                options={PRIORITY_OPTIONS.map(priority => ({ value: priority.value, label: t(priority.labelKey) }))}
                value={formData.priority}
                onValueChange={(value) => onFormDataChange({ priority: value as TaskPriority })}
                placeholder="Select priority"
              />
            </div>
          </div>

          {/* Assignee and Pair Assignee */}
          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="assignee">{t('backlogPage.assignee')}</Label>
              <Combobox
                options={[
                  { value: 'unassigned', label: t('backlogPage.unassigned') },
                  ...persons.map(person => ({ value: person.id.toString(), label: person.name }))
                ]}
                value={formData.assigneeId?.toString() || 'unassigned'}
                onValueChange={(value) => onFormDataChange({ assigneeId: value === 'unassigned' ? undefined : Number(value) })}
                placeholder={t('backlogPage.selectAssignee')}
                searchPlaceholder="Search persons..."
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="pairAssignee">{t('backlogPage.pairAssignee')}</Label>
              <Combobox
                options={[
                  { value: 'none', label: t('backlogPage.none') },
                  ...persons.filter(p => p.id !== formData.assigneeId).map(person => ({ value: person.id.toString(), label: person.name }))
                ]}
                value={formData.pairAssigneeId?.toString() || 'none'}
                onValueChange={(value) => onFormDataChange({ pairAssigneeId: value === 'none' ? undefined : Number(value) })}
                placeholder={t('backlogPage.selectPair')}
                searchPlaceholder="Search persons..."
              />
            </div>
          </div>

          {/* Estimate Hours and Due Date */}
          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="estimateHours">{t('backlogPage.estimateHours')}</Label>
              <Input
                id="estimateHours"
                type="number"
                min="0"
                step="0.5"
                value={formData.estimateHours || ''}
                onChange={(e) => onFormDataChange({ estimateHours: e.target.value ? Number(e.target.value) : undefined })}
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
                onChange={(val: string) => onDueDateChange(val ? dayjs(val) : null)}
              />
            </div>
          </div>

          {/* Tags */}
          <div className="grid gap-2">
            <Label htmlFor="tags">{t('backlogPage.tags')}</Label>
            <Input
              id="tags"
              value={formData.tags}
              onChange={(e) => onFormDataChange({ tags: e.target.value })}
              placeholder={t('backlogPage.commaSeparated')}
            />
          </div>

          {/* Pitch - Hidden for Kanban projects */}
          {!isKanbanProject && (
            <>
              <div className="grid gap-2">
                <Label>{t('backlogPage.pitch')}</Label>
                <Combobox
                  options={[
                    { value: 'none', label: t('backlogPage.noPitch') },
                    ...pitches.map(pitch => ({ value: String(pitch.id), label: pitch.title }))
                  ]}
                  value={formData.pitchId ? String(formData.pitchId) : 'none'}
                  onValueChange={onPitchChange}
                  placeholder={t('backlogPage.noPitch')}
                  searchPlaceholder="Search pitches..."
                />
              </div>

              {/* Hill Chart Indicator - shown when task will appear on hill chart */}
              {formData.pitchId && !formData.parentTaskId && !editingTask && (
                <div className="flex items-center gap-2 p-3 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400">
                  <TrendingUp className="h-4 w-4 shrink-0" />
                  <p className="text-sm">
                    {t('backlogPage.hillChartAutoCreate', 'This task will automatically appear on the Hill Chart for tracking progress.')}
                  </p>
                </div>
              )}
            </>
          )}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            {t('backlogPage.cancel')}
          </Button>
          <Button onClick={onSave} disabled={saving}>
            {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            {editingTask ? t('backlogPage.update') : t('backlogPage.create')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
