import { useTranslation } from 'react-i18next';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
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
import { LocalizedDateInput } from '../LocalizedDateInput';
import { Task, CreateTaskRequest, TaskStatus, TaskPriority, TaskCategory, Person, Pitch, Team } from '../../types';
import { statusOptions, priorityOptions } from './backlogTypes';
import dayjs, { Dayjs } from 'dayjs';

interface BacklogTaskDialogProps {
  open: boolean;
  editingTask: Task | null;
  formData: CreateTaskRequest;
  dueDate: Dayjs | null;
  fieldErrors: Record<string, string>;
  saving: boolean;
  persons: Person[];
  teams: Team[];
  pitches: Pitch[];
  activeCategory: TaskCategory;
  isKanbanProject: boolean;
  onOpenChange: (open: boolean) => void;
  onFormDataChange: (data: CreateTaskRequest) => void;
  onDueDateChange: (date: Dayjs | null) => void;
  onPitchChange: (pitchId: string) => void;
  onSave: () => void;
  onClose: () => void;
}

export function BacklogTaskDialog({
  open,
  editingTask,
  formData,
  dueDate,
  fieldErrors,
  saving,
  persons,
  teams,
  pitches,
  activeCategory,
  isKanbanProject,
  onOpenChange,
  onFormDataChange,
  onDueDateChange,
  onPitchChange,
  onSave,
  onClose,
}: BacklogTaskDialogProps) {
  const { t } = useTranslation();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
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
              onChange={(e) => onFormDataChange({ ...formData, title: e.target.value })}
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
              onChange={(e) => onFormDataChange({ ...formData, description: e.target.value })}
              placeholder={formData.parentTaskId ? t('backlogPage.subtaskDescription') : t('backlogPage.taskDescription')}
              rows={3}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="status">{t('common.status')}</Label>
              <Select
                value={formData.status}
                onValueChange={(value) => onFormDataChange({ ...formData, status: value as TaskStatus })}
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
                onValueChange={(value) => onFormDataChange({ ...formData, priority: value as TaskPriority })}
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
                onValueChange={(value) => onFormDataChange({ ...formData, assigneeId: value === 'unassigned' ? undefined : Number(value) })}
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
                onValueChange={(value) => onFormDataChange({ ...formData, pairAssigneeId: value === 'none' ? undefined : Number(value) })}
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
          {teams.length > 0 && (
            <div className="grid gap-2">
              <Label>{t('backlogPage.team')}</Label>
              <Select
                value={formData.teamId ? String(formData.teamId) : 'none'}
                onValueChange={(value) => onFormDataChange({ ...formData, teamId: value === 'none' ? undefined : Number(value) })}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('backlogPage.noTeam')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{t('backlogPage.noTeam')}</SelectItem>
                  {teams.map((team) => (
                    <SelectItem key={team.id} value={String(team.id)}>
                      {team.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}
          <div className="grid grid-cols-2 gap-4">
            <div className="grid gap-2">
              <Label htmlFor="estimateHours">{t('backlogPage.estimateHours')}</Label>
              <Input
                id="estimateHours"
                type="number"
                min="0"
                step="0.5"
                value={formData.estimateHours || ''}
                onChange={(e) => onFormDataChange({ ...formData, estimateHours: e.target.value ? Number(e.target.value) : undefined })}
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
                onChange={(val) => onDueDateChange(val ? dayjs(val) : null)}
              />
            </div>
          </div>
          <div className="grid gap-2">
            <Label htmlFor="tags">{t('backlogPage.tags')}</Label>
            <Input
              id="tags"
              value={formData.tags}
              onChange={(e) => onFormDataChange({ ...formData, tags: e.target.value })}
              placeholder={t('backlogPage.commaSeparated')}
            />
          </div>
          {/* Hide pitch field for Kanban projects */}
          {!isKanbanProject && (
            <div className="grid gap-2">
              <Label>{t('backlogPage.pitch')}</Label>
              <Select
                value={formData.pitchId ? String(formData.pitchId) : 'none'}
                onValueChange={onPitchChange}
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
