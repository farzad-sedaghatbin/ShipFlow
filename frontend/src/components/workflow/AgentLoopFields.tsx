import { useTranslation } from 'react-i18next';
import { Sparkles } from 'lucide-react';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Switch } from '../ui/switch';
import {
  agentProblem,
  LOOP_CHANNELS,
  type AgentForm,
  type LoopChannel,
} from './agentLoopConfig';
import type { ActionType } from '../../types/automation';

interface Props {
  action: ActionType;
  value: AgentForm;
  onChange: (next: AgentForm) => void;
}

/** Delivery settings for the ShipFlow Agent's loops: where it posts, how far back, and whether it may add an AI summary. */
export default function AgentLoopFields({ action, value, onChange }: Props) {
  const { t } = useTranslation();
  const problem = agentProblem(value, action);
  const set = (patch: Partial<AgentForm>) => onChange({ ...value, ...patch });

  const toggleChannel = (c: LoopChannel, on: boolean) =>
    set({ channels: on ? [...value.channels.filter((x) => x !== c), c] : value.channels.filter((x) => x !== c) });

  return (
    <div className="space-y-3 rounded-md border p-3" data-testid="agent-loop-fields">
      <p className="flex items-center gap-2 text-sm font-medium">
        <Sparkles className="h-4 w-4 text-primary" aria-hidden="true" />
        {t('automation.agentLoop.title')}
      </p>
      <p className="text-xs text-muted-foreground">{t(`automation.agentLoop.about.${action}`)}</p>

      <fieldset className="space-y-1">
        <legend className="text-sm">{t('automation.agentLoop.sendTo')}</legend>
        <div className="flex flex-wrap gap-4">
          {LOOP_CHANNELS.map((c) => (
            <label key={c} className="flex items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={value.channels.includes(c)}
                onChange={(e) => toggleChannel(c, e.target.checked)}
              />
              {t(`automation.agentLoop.channel.${c}`)}
            </label>
          ))}
        </div>
        <p className="text-xs text-muted-foreground">{t('automation.agentLoop.sendToHint')}</p>
      </fieldset>

      {(value.channels.includes('SLACK') || value.channels.includes('TEAMS')) && (
        <div className="space-y-1">
          <Label htmlFor="agent-channel">{t('automation.agentLoop.channelName')}</Label>
          <Input
            id="agent-channel"
            value={value.channel}
            onChange={(e) => set({ channel: e.target.value })}
            placeholder="#standup"
          />
          <p className="text-xs text-muted-foreground">{t('automation.agentLoop.channelNameHint')}</p>
        </div>
      )}

      {action === 'AGENT_STANDUP_DIGEST' && (
        <div className="space-y-1">
          <Label htmlFor="agent-lookback">{t('automation.agentLoop.lookback')}</Label>
          <Input
            id="agent-lookback"
            type="number"
            min={1}
            max={168}
            value={value.lookbackHours}
            onChange={(e) => set({ lookbackHours: Number(e.target.value) })}
          />
        </div>
      )}

      {action === 'AGENT_CYCLE_HEALTH_CHECK' && (
        <div className="flex items-center gap-2">
          <Switch
            checked={value.onlyWhenAtRisk}
            onCheckedChange={(v) => set({ onlyWhenAtRisk: v })}
            aria-label={t('automation.agentLoop.onlyWhenAtRisk')}
          />
          <Label>{t('automation.agentLoop.onlyWhenAtRisk')}</Label>
        </div>
      )}

      <div className="flex items-start gap-2">
        <Switch
          checked={value.useAi}
          onCheckedChange={(v) => set({ useAi: v })}
          aria-label={t('automation.agentLoop.useAi')}
        />
        <div>
          <Label>{t('automation.agentLoop.useAi')}</Label>
          <p className="text-xs text-muted-foreground">{t('automation.agentLoop.useAiHint')}</p>
        </div>
      </div>

      {problem && (
        <p className="text-xs text-destructive" role="alert">
          {t(`automation.agentLoop.error.${problem}`)}
        </p>
      )}
    </div>
  );
}
