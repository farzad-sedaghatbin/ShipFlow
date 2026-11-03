import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { automationService } from '../../services/automationService';
import { useToast } from '../../contexts';
import type { WorkflowAutomation, TriggerType, ActionType } from '../../types/automation';
import { getUserFriendlyError } from '../../utils/errorMessages';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import { Switch } from '../ui/switch';
import ScheduleFields from './ScheduleFields';
import AgentLoopFields from './AgentLoopFields';
import {
  agentProblem,
  allowedTriggers,
  buildAgentConfig,
  buildScheduleConfig,
  defaultAgentForm,
  defaultSchedule,
  isAgentLoop,
  parseAgentConfig,
  parseScheduleConfig,
  scheduleProblem,
  type AgentForm,
  type ScheduleForm,
} from './agentLoopConfig';

/** `labelKey` entries are translated; the older ones keep their English labels. */
type Option<T> = { value: T; label?: string; labelKey?: string };

const TRIGGER_OPTIONS: Option<TriggerType>[] = [
  { value: 'SCHEDULE', labelKey: 'automation.triggerSchedule' },
  { value: 'TASK_CREATED', label: 'Task Created' },
  { value: 'TASK_STATUS_CHANGED', label: 'Task Status Changed' },
  { value: 'TASK_ASSIGNED', label: 'Task Assigned' },
  { value: 'TASK_COMPLETED', label: 'Task Completed' },
  { value: 'PITCH_CREATED', label: 'Pitch Created' },
  { value: 'PITCH_STATUS_CHANGED', label: 'Pitch Status Changed' },
  { value: 'CYCLE_STARTED', label: 'Cycle Started' },
  { value: 'CYCLE_ENDED', label: 'Cycle Ended' },
  { value: 'CYCLE_STATUS_CHANGED', label: 'Cycle Status Changed' },
  { value: 'COMMENT_ADDED', label: 'Comment Added' },
  { value: 'BETTING_TABLE_LOCKED', label: 'Betting Table Locked' },
  { value: 'HILL_CHART_MOVED', label: 'Hill Chart Moved' },
  { value: 'APPETITE_EXCEEDED', label: 'Appetite Exceeded' },
  { value: 'SCOPE_CREEP_DETECTED', label: 'Scope Creep Detected' },
];

const ACTION_OPTIONS: Option<ActionType>[] = [
  { value: 'AGENT_STANDUP_DIGEST', labelKey: 'automation.action_AGENT_STANDUP_DIGEST' },
  { value: 'AGENT_CYCLE_HEALTH_CHECK', labelKey: 'automation.action_AGENT_CYCLE_HEALTH_CHECK' },
  { value: 'AGENT_BETTING_PREP', labelKey: 'automation.action_AGENT_BETTING_PREP' },
  { value: 'NOTIFY_ASSIGNEE', label: 'Notify Assignee' },
  { value: 'NOTIFY_PROJECT_MEMBERS', label: 'Notify Project Members' },
  { value: 'SEND_WEBHOOK', label: 'Send Webhook' },
  { value: 'SEND_EMAIL', label: 'Send Email' },
  { value: 'ADD_COMMENT', label: 'Add Comment' },
  { value: 'CHANGE_TASK_STATUS', label: 'Change Task Status' },
  { value: 'CREATE_TASK', label: 'Create Task' },
];

const schema = z.object({
  name: z.string().min(1),
  description: z.string().optional(),
  triggerType: z.string().min(1),
  actionType: z.string().min(1),
  triggerConfig: z.string().optional(),
  actionConfig: z.string().optional(),
  enabled: z.boolean(),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  open: boolean;
  projectId: number;
  automation: WorkflowAutomation | null;
  onClose: () => void;
  onSaved: () => void;
}

export default function AutomationRuleForm({ open, projectId, automation, onClose, onSaved }: Props) {
  const { t } = useTranslation();
  const { showSuccess, showError } = useToast();

  const { register, handleSubmit, setValue, watch, reset, formState: { errors, isSubmitting } } =
    useForm<FormValues>({
      resolver: zodResolver(schema),
      defaultValues: {
        name: '',
        description: '',
        triggerType: 'TASK_COMPLETED',
        actionType: 'NOTIFY_PROJECT_MEMBERS',
        triggerConfig: '{}',
        actionConfig: '{}',
        enabled: true,
      },
    });

  // The structured sections hold their own state and are turned into JSON on save.
  const [schedule, setSchedule] = useState<ScheduleForm>(defaultSchedule);
  const [agent, setAgent] = useState<AgentForm>(defaultAgentForm);

  const triggerType = watch('triggerType');
  const actionType = watch('actionType');
  const agentLoop = isAgentLoop(actionType);
  const triggerChoices = useMemo(() => {
    const allowed = allowedTriggers(actionType);
    return allowed ? TRIGGER_OPTIONS.filter((o) => allowed.includes(o.value)) : TRIGGER_OPTIONS;
  }, [actionType]);
  const problem =
    (triggerType === 'SCHEDULE' && scheduleProblem(schedule) !== null) ||
    (agentLoop && agentProblem(agent, actionType as ActionType) !== null);

  useEffect(() => {
    setSchedule(parseScheduleConfig(automation?.triggerType === 'SCHEDULE' ? automation.triggerConfig : null));
    setAgent(parseAgentConfig(automation && isAgentLoop(automation.actionType) ? automation.actionConfig : null));
    if (automation) {
      reset({
        name: automation.name,
        description: automation.description ?? '',
        triggerType: automation.triggerType,
        actionType: automation.actionType,
        triggerConfig: automation.triggerConfig ?? '{}',
        actionConfig: automation.actionConfig ?? '{}',
        enabled: automation.enabled,
      });
    } else {
      reset({
        name: '',
        description: '',
        triggerType: 'TASK_COMPLETED',
        actionType: 'NOTIFY_PROJECT_MEMBERS',
        triggerConfig: '{}',
        actionConfig: '{}',
        enabled: true,
      });
    }
  }, [automation, reset]);

  const chooseAction = (value: string) => {
    setValue('actionType', value);
    // An agent loop only makes sense on certain triggers: move off one it can't use.
    const allowed = allowedTriggers(value);
    if (allowed && !allowed.includes(triggerType as TriggerType)) {
      setValue('triggerType', allowed[0]);
    }
  };

  const onSubmit = async (values: FormValues) => {
    try {
      const payload = {
        name: values.name,
        description: values.description,
        projectId,
        triggerType: values.triggerType as TriggerType,
        triggerConfig: values.triggerType === 'SCHEDULE' ? buildScheduleConfig(schedule) : values.triggerConfig,
        actionType: values.actionType as ActionType,
        actionConfig: isAgentLoop(values.actionType)
          ? buildAgentConfig(agent, values.actionType as ActionType)
          : values.actionConfig,
        enabled: values.enabled,
      };
      if (automation) {
        await automationService.update(automation.id, payload);
        showSuccess(t('automation.updatedSuccess'));
      } else {
        await automationService.create(payload);
        showSuccess(t('automation.createdSuccess'));
      }
      onSaved();
    } catch (err) {
      showError(getUserFriendlyError(err));
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-h-[90vh] max-w-lg overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {automation ? t('automation.editRule') : t('automation.newRule')}
          </DialogTitle>
          <DialogDescription>{t('automation.ruleFormDesc')}</DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1">
            <Label htmlFor="name">{t('common.name')} *</Label>
            <Input id="name" {...register('name')} />
            {errors.name && <p className="text-xs text-destructive">{t('common.required')}</p>}
          </div>

          <div className="space-y-1">
            <Label htmlFor="description">{t('common.description')}</Label>
            <Textarea id="description" rows={2} {...register('description')} />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <Label>{t('automation.trigger')} *</Label>
              <Select
                value={triggerType}
                onValueChange={v => setValue('triggerType', v)}
              >
                <SelectTrigger aria-label={t('automation.trigger')}><SelectValue /></SelectTrigger>
                <SelectContent>
                  {triggerChoices.map(o => (
                    <SelectItem key={o.value} value={o.value}>{o.labelKey ? t(o.labelKey) : o.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-1">
              <Label>{t('automation.action')} *</Label>
              <Select
                value={actionType}
                onValueChange={chooseAction}
              >
                <SelectTrigger aria-label={t('automation.action')}><SelectValue /></SelectTrigger>
                <SelectContent>
                  {ACTION_OPTIONS.map(o => (
                    <SelectItem key={o.value} value={o.value}>{o.labelKey ? t(o.labelKey) : o.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          {triggerType === 'SCHEDULE' && <ScheduleFields value={schedule} onChange={setSchedule} />}

          {agentLoop ? (
            <AgentLoopFields action={actionType as ActionType} value={agent} onChange={setAgent} />
          ) : (
            <div className="space-y-1">
              <Label htmlFor="actionConfig">{t('automation.actionConfig')}</Label>
              <Textarea
                id="actionConfig"
                rows={3}
                className="font-mono text-xs"
                placeholder='{"message":"A task was completed: {{taskTitle}}"}'
                {...register('actionConfig')}
              />
              <p className="text-xs text-muted-foreground">{t('automation.configHint')}</p>
            </div>
          )}

          <div className="flex items-center gap-2">
            <Switch
              checked={watch('enabled')}
              onCheckedChange={v => setValue('enabled', v)}
            />
            <Label>{t('automation.enabledLabel')}</Label>
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" disabled={isSubmitting || problem}>
              {isSubmitting ? t('common.saving') : t('common.save')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
