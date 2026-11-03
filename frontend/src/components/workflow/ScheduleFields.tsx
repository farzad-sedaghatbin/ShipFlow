import { useTranslation } from 'react-i18next';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select';
import {
  buildCron,
  scheduleProblem,
  WEEKDAYS,
  type SchedulePreset,
  type ScheduleForm,
  type Weekday,
} from './agentLoopConfig';

interface Props {
  value: ScheduleForm;
  onChange: (next: ScheduleForm) => void;
}

const PRESETS: SchedulePreset[] = ['WEEKDAYS', 'DAILY', 'WEEKLY', 'CUSTOM'];

/** "When does it run?" — a preset plus a time of day, or a raw cron for anything else. */
export default function ScheduleFields({ value, onChange }: Props) {
  const { t } = useTranslation();
  const problem = scheduleProblem(value);
  const set = (patch: Partial<ScheduleForm>) => onChange({ ...value, ...patch });

  return (
    <div className="space-y-3 rounded-md border p-3" data-testid="schedule-fields">
      <p className="text-sm font-medium">{t('automation.schedule.title')}</p>

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label>{t('automation.schedule.repeat')}</Label>
          <Select value={value.preset} onValueChange={(v) => set({ preset: v as SchedulePreset })}>
            <SelectTrigger aria-label={t('automation.schedule.repeat')}><SelectValue /></SelectTrigger>
            <SelectContent>
              {PRESETS.map((p) => (
                <SelectItem key={p} value={p}>{t(`automation.schedule.preset.${p}`)}</SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {value.preset === 'WEEKLY' && (
          <div className="space-y-1">
            <Label>{t('automation.schedule.weekday')}</Label>
            <Select value={value.weekday} onValueChange={(v) => set({ weekday: v as Weekday })}>
              <SelectTrigger aria-label={t('automation.schedule.weekday')}><SelectValue /></SelectTrigger>
              <SelectContent>
                {WEEKDAYS.map((d) => (
                  <SelectItem key={d} value={d}>{t(`automation.schedule.day.${d}`)}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        )}

        {value.preset !== 'CUSTOM' && (
          <div className="space-y-1">
            <Label htmlFor="schedule-time">{t('automation.schedule.time')}</Label>
            <Input
              id="schedule-time"
              type="time"
              value={value.time}
              onChange={(e) => set({ time: e.target.value })}
            />
          </div>
        )}
      </div>

      {value.preset === 'CUSTOM' && (
        <div className="space-y-1">
          <Label htmlFor="schedule-cron">{t('automation.schedule.cron')}</Label>
          <Input
            id="schedule-cron"
            className="font-mono text-xs"
            placeholder="0 0 9 * * MON-FRI"
            value={value.cron}
            onChange={(e) => set({ cron: e.target.value })}
          />
          <p className="text-xs text-muted-foreground">{t('automation.schedule.cronHint')}</p>
        </div>
      )}

      <div className="space-y-1">
        <Label htmlFor="schedule-tz">{t('automation.schedule.timezone')}</Label>
        <Input
          id="schedule-tz"
          value={value.timezone}
          onChange={(e) => set({ timezone: e.target.value })}
          placeholder="UTC"
        />
        <p className="text-xs text-muted-foreground">{t('automation.schedule.timezoneHint')}</p>
      </div>

      {problem && (
        <p className="text-xs text-destructive" role="alert">
          {t(problem === 'cron' ? 'automation.schedule.errorCron' : 'automation.schedule.errorTime')}
        </p>
      )}
      {!problem && (
        <p className="text-xs text-muted-foreground">
          {t('automation.schedule.cronIs')} <code className="font-mono">{buildCron(value)}</code>
        </p>
      )}
    </div>
  );
}
