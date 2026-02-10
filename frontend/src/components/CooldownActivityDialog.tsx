import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Label } from './ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import {
  CooldownActivityDTO,
  CooldownActivityType,
  CooldownActivityStatus,
  CreateCooldownActivityRequest,
  UpdateCooldownActivityRequest,
} from '../services/cooldownActivityService';
import { personService } from '../services/personService';
import { Person } from '../types';
import { useToast } from '../contexts';
import { Loader2 } from 'lucide-react';
import {
  useCreateCooldownActivity,
  useUpdateCooldownActivity,
} from '../hooks/useCooldownActivities';

interface CooldownActivityDialogProps {
  open: boolean;
  onClose: () => void;
  onSave: () => void;
  cycleId: number;
  activity?: CooldownActivityDTO;
}

export default function CooldownActivityDialog({
  open,
  onClose,
  onSave,
  cycleId,
  activity,
}: CooldownActivityDialogProps) {
  const { t } = useTranslation();
  const { showError } = useToast();
  const [people, setPeople] = useState<Person[]>([]);

  // React Query mutations
  const createMutation = useCreateCooldownActivity();
  const updateMutation = useUpdateCooldownActivity();

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    activityType: CooldownActivityType.TECH_DEBT,
    status: CooldownActivityStatus.PLANNED,
    assigneeId: undefined as number | undefined,
    estimatedHours: undefined as number | undefined,
    actualHours: undefined as number | undefined,
  });

  useEffect(() => {
    if (open) {
      loadPeople();
      if (activity) {
        setFormData({
          title: activity.title,
          description: activity.description || '',
          activityType: activity.activityType,
          status: activity.status,
          assigneeId: activity.assigneeId,
          estimatedHours: activity.estimatedHours,
          actualHours: activity.actualHours,
        });
      } else {
        setFormData({
          title: '',
          description: '',
          activityType: CooldownActivityType.TECH_DEBT,
          status: CooldownActivityStatus.PLANNED,
          assigneeId: undefined,
          estimatedHours: undefined,
          actualHours: undefined,
        });
      }
    }
  }, [open, activity]);

  const loadPeople = async () => {
    try {
      const people = await personService.getAll();
      setPeople(people);
    } catch (error) {
      console.error('Failed to load people:', error);
    }
  };

  const handleSubmit = () => {
    if (!formData.title.trim()) {
      showError(t('cooldownActivity.titleRequired'));
      return;
    }

    if (activity) {
      const request: UpdateCooldownActivityRequest = {
        title: formData.title,
        description: formData.description || undefined,
        activityType: formData.activityType,
        status: formData.status,
        assigneeId: formData.assigneeId,
        estimatedHours: formData.estimatedHours,
        actualHours: formData.actualHours,
      };
      updateMutation.mutate(
        { id: activity.id, data: request, cycleId },
        {
          onSuccess: () => {
            onSave();
            onClose();
          },
        }
      );
    } else {
      const request: CreateCooldownActivityRequest = {
        cycleId,
        title: formData.title,
        description: formData.description || undefined,
        activityType: formData.activityType,
        assigneeId: formData.assigneeId,
        estimatedHours: formData.estimatedHours,
      };
      createMutation.mutate(request, {
        onSuccess: () => {
          onSave();
          onClose();
        },
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {activity ? t('cooldownActivity.editActivity') : t('cooldownActivity.createActivity')}
          </DialogTitle>
          <DialogDescription>
            {t('cooldownActivity.dialogDescription')}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* Title */}
          <div>
            <Label htmlFor="title">{t('cooldownActivity.title')} *</Label>
            <Input
              id="title"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              placeholder={t('cooldownActivity.titlePlaceholder')}
            />
          </div>

          {/* Description */}
          <div>
            <Label htmlFor="description">{t('cooldownActivity.description')}</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder={t('cooldownActivity.descriptionPlaceholder')}
              rows={3}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            {/* Activity Type */}
            <div>
              <Label>{t('cooldownActivity.activityType')}</Label>
              <Select
                value={formData.activityType}
                onValueChange={(value) =>
                  setFormData({ ...formData, activityType: value as CooldownActivityType })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={CooldownActivityType.TECH_DEBT}>
                    {t('cooldownActivity.types.techDebt')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.BUG_FIX}>
                    {t('cooldownActivity.types.bugFix')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.REFACTORING}>
                    {t('cooldownActivity.types.refactoring')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.KICKOFF_PREP}>
                    {t('cooldownActivity.types.kickoffPrep')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.LEARNING}>
                    {t('cooldownActivity.types.learning')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.QA}>
                    {t('cooldownActivity.types.qa')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.DOCUMENTATION}>
                    {t('cooldownActivity.types.documentation')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.INFRASTRUCTURE}>
                    {t('cooldownActivity.types.infrastructure')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.RESEARCH}>
                    {t('cooldownActivity.types.research')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.PLANNING}>
                    {t('cooldownActivity.types.planning')}
                  </SelectItem>
                  <SelectItem value={CooldownActivityType.OTHER}>
                    {t('cooldownActivity.types.other')}
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Status (only for edit) */}
            {activity && (
              <div>
                <Label>{t('cooldownActivity.status')}</Label>
                <Select
                  value={formData.status}
                  onValueChange={(value) =>
                    setFormData({ ...formData, status: value as CooldownActivityStatus })
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value={CooldownActivityStatus.PLANNED}>
                      {t('cooldownActivity.statuses.planned')}
                    </SelectItem>
                    <SelectItem value={CooldownActivityStatus.IN_PROGRESS}>
                      {t('cooldownActivity.statuses.inProgress')}
                    </SelectItem>
                    <SelectItem value={CooldownActivityStatus.COMPLETED}>
                      {t('cooldownActivity.statuses.completed')}
                    </SelectItem>
                    <SelectItem value={CooldownActivityStatus.SKIPPED}>
                      {t('cooldownActivity.statuses.skipped')}
                    </SelectItem>
                    <SelectItem value={CooldownActivityStatus.BLOCKED}>
                      {t('cooldownActivity.statuses.blocked')}
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* Assignee */}
            <div className={activity ? 'col-span-2' : ''}>
              <Label>{t('cooldownActivity.assignee')}</Label>
              <Select
                value={formData.assigneeId?.toString() || 'none'}
                onValueChange={(value) =>
                  setFormData({
                    ...formData,
                    assigneeId: value === 'none' ? undefined : parseInt(value),
                  })
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('cooldownActivity.selectAssignee')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{t('cooldownActivity.unassigned')}</SelectItem>
                  {people.map((person) => (
                    <SelectItem key={person.id} value={person.id.toString()}>
                      {person.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            {/* Estimated Hours */}
            <div>
              <Label htmlFor="estimatedHours">{t('cooldownActivity.estimatedHours')}</Label>
              <Input
                id="estimatedHours"
                type="number"
                min="0"
                step="0.5"
                value={formData.estimatedHours || ''}
                onChange={(e) =>
                  setFormData({
                    ...formData,
                    estimatedHours: e.target.value ? parseFloat(e.target.value) : undefined,
                  })
                }
                placeholder="0"
              />
            </div>

            {/* Actual Hours (only for edit) */}
            {activity && (
              <div>
                <Label htmlFor="actualHours">{t('cooldownActivity.actualHours')}</Label>
                <Input
                  id="actualHours"
                  type="number"
                  min="0"
                  step="0.5"
                  value={formData.actualHours || ''}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      actualHours: e.target.value ? parseFloat(e.target.value) : undefined,
                    })
                  }
                  placeholder="0"
                />
              </div>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={onClose}
            disabled={createMutation.isPending || updateMutation.isPending}
          >
            {t('common.cancel')}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={createMutation.isPending || updateMutation.isPending}
          >
            {(createMutation.isPending || updateMutation.isPending) && (
              <Loader2 className="me-2 h-4 w-4 animate-spin" />
            )}
            {activity ? t('common.update') : t('common.create')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
