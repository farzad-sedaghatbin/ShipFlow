import type { ActionType, TriggerType } from '../../types/automation';

/**
 * Pure helpers behind the rule form's "Schedule" and "Agent loop" sections: they turn the
 * friendly controls (a time of day, a weekday, a few checkboxes) into the JSON the backend stores
 * in `triggerConfig` / `actionConfig`, and read it back when a rule is edited. Keeping them out of
 * the components means the cron building and parsing can be tested without rendering anything.
 */

export const AGENT_ACTIONS: ActionType[] = [
  'AGENT_STANDUP_DIGEST',
  'AGENT_CYCLE_HEALTH_CHECK',
  'AGENT_BETTING_PREP',
];

export const isAgentLoop = (action: string): action is ActionType =>
  (AGENT_ACTIONS as string[]).includes(action);

/** Triggers an agent loop may run on: a schedule — and, for betting prep, a cycle ending. */
export function allowedTriggers(action: string): TriggerType[] | null {
  if (!isAgentLoop(action)) return null; // any trigger
  return action === 'AGENT_BETTING_PREP' ? ['SCHEDULE', 'CYCLE_ENDED'] : ['SCHEDULE'];
}

// ── Schedule ────────────────────────────────────────────────────────────────

export type SchedulePreset = 'WEEKDAYS' | 'DAILY' | 'WEEKLY' | 'CUSTOM';
export const WEEKDAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'] as const;
export type Weekday = (typeof WEEKDAYS)[number];

export interface ScheduleForm {
  preset: SchedulePreset;
  /** 24-hour "HH:mm". */
  time: string;
  weekday: Weekday;
  /** Used only for CUSTOM: five- or six-field cron. */
  cron: string;
  timezone: string;
}

export const browserTimezone = (): string => {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
  } catch {
    return 'UTC';
  }
};

export const defaultSchedule = (): ScheduleForm => ({
  preset: 'WEEKDAYS',
  time: '09:00',
  weekday: 'MON',
  cron: '',
  timezone: browserTimezone(),
});

const TIME = /^([01]\d|2[0-3]):([0-5]\d)$/;

/** The six-field Spring cron (second minute hour day-of-month month weekday) for a form. */
export function buildCron(f: ScheduleForm): string {
  if (f.preset === 'CUSTOM') return f.cron.trim();
  const m = TIME.exec(f.time);
  const hour = m ? Number(m[1]) : 9;
  const minute = m ? Number(m[2]) : 0;
  const days = f.preset === 'WEEKDAYS' ? 'MON-FRI' : f.preset === 'WEEKLY' ? f.weekday : '*';
  return `0 ${minute} ${hour} * * ${days}`;
}

export const buildScheduleConfig = (f: ScheduleForm): string =>
  JSON.stringify({ cron: buildCron(f), timezone: f.timezone.trim() || 'UTC' });

/** Reads a stored schedule back into form state; anything it doesn't recognise becomes CUSTOM. */
export function parseScheduleConfig(json: string | undefined | null): ScheduleForm {
  const base = defaultSchedule();
  if (!json) return base;
  let cfg: { cron?: unknown; timezone?: unknown };
  try {
    cfg = JSON.parse(json);
  } catch {
    return base;
  }
  const timezone = typeof cfg.timezone === 'string' && cfg.timezone ? cfg.timezone : 'UTC';
  const cron = typeof cfg.cron === 'string' ? cfg.cron.trim().replace(/\s+/g, ' ') : '';
  if (!cron) return { ...base, timezone };

  // "0 M H * * DAYS" (six fields) or "M H * * DAYS" (five) with a plain minute and hour
  const parts = cron.split(' ');
  const fields = parts.length === 5 ? ['0', ...parts] : parts;
  if (fields.length === 6) {
    const [sec, min, hour, dom, month, dow] = fields;
    const plain = sec === '0' && /^\d{1,2}$/.test(min) && /^\d{1,2}$/.test(hour) && dom === '*' && month === '*';
    const time = `${hour.padStart(2, '0')}:${min.padStart(2, '0')}`;
    if (plain && TIME.test(time)) {
      if (dow === 'MON-FRI') return { ...base, preset: 'WEEKDAYS', time, timezone };
      if (dow === '*') return { ...base, preset: 'DAILY', time, timezone };
      if ((WEEKDAYS as readonly string[]).includes(dow)) {
        return { ...base, preset: 'WEEKLY', time, weekday: dow as Weekday, timezone };
      }
    }
  }
  return { ...base, preset: 'CUSTOM', cron, timezone };
}

/** A message for the first thing wrong with a schedule form, or null when it looks fine. */
export function scheduleProblem(f: ScheduleForm): 'cron' | 'time' | null {
  if (f.preset === 'CUSTOM') return f.cron.trim() ? null : 'cron';
  return TIME.test(f.time) ? null : 'time';
}

// ── Agent loop delivery settings ─────────────────────────────────────────────

export type LoopChannel = 'SLACK' | 'TEAMS' | 'IN_APP';
export const LOOP_CHANNELS: LoopChannel[] = ['SLACK', 'TEAMS', 'IN_APP'];

export interface AgentForm {
  channels: LoopChannel[];
  /** Optional Slack/Teams channel override. */
  channel: string;
  lookbackHours: number;
  onlyWhenAtRisk: boolean;
  useAi: boolean;
}

export const defaultAgentForm = (): AgentForm => ({
  channels: ['IN_APP'],
  channel: '',
  lookbackHours: 24,
  onlyWhenAtRisk: true,
  useAi: true,
});

/** Only the settings that mean something for this loop are written, so stored JSON stays tidy. */
export function buildAgentConfig(f: AgentForm, action: ActionType): string {
  const cfg: Record<string, unknown> = { channels: f.channels };
  if (f.channel.trim()) cfg.channel = f.channel.trim();
  if (action === 'AGENT_STANDUP_DIGEST') cfg.lookbackHours = f.lookbackHours;
  if (action === 'AGENT_CYCLE_HEALTH_CHECK') cfg.onlyWhenAtRisk = f.onlyWhenAtRisk;
  cfg.useAi = f.useAi;
  return JSON.stringify(cfg);
}

export function parseAgentConfig(json: string | undefined | null): AgentForm {
  const base = defaultAgentForm();
  if (!json) return base;
  let cfg: Record<string, unknown>;
  try {
    cfg = JSON.parse(json);
  } catch {
    return base;
  }
  const channels = Array.isArray(cfg.channels)
    ? (cfg.channels.filter((c) => (LOOP_CHANNELS as unknown[]).includes(c)) as LoopChannel[])
    : base.channels;
  return {
    channels: channels.length ? channels : base.channels,
    channel: typeof cfg.channel === 'string' ? cfg.channel : '',
    lookbackHours: typeof cfg.lookbackHours === 'number' ? cfg.lookbackHours : base.lookbackHours,
    onlyWhenAtRisk: typeof cfg.onlyWhenAtRisk === 'boolean' ? cfg.onlyWhenAtRisk : base.onlyWhenAtRisk,
    useAi: typeof cfg.useAi === 'boolean' ? cfg.useAi : base.useAi,
  };
}

export type AgentProblem = 'channels' | 'lookback' | 'channelName' | null;

export function agentProblem(f: AgentForm, action: ActionType): AgentProblem {
  if (f.channels.length === 0) return 'channels';
  if (action === 'AGENT_STANDUP_DIGEST' && (!Number.isInteger(f.lookbackHours) || f.lookbackHours < 1 || f.lookbackHours > 168)) {
    return 'lookback';
  }
  if (f.channel.trim() && !/^[#@]?[A-Za-z0-9_.-]{1,80}$/.test(f.channel.trim())) return 'channelName';
  return null;
}
