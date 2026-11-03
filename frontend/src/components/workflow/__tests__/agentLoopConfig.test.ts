import { describe, it, expect } from 'vitest';
import {
  agentProblem,
  allowedTriggers,
  buildAgentConfig,
  buildCron,
  buildScheduleConfig,
  defaultAgentForm,
  defaultSchedule,
  isAgentLoop,
  parseAgentConfig,
  parseScheduleConfig,
  scheduleProblem,
  type ScheduleForm,
} from '../agentLoopConfig';

const sched = (over: Partial<ScheduleForm> = {}): ScheduleForm => ({
  preset: 'WEEKDAYS', time: '09:00', weekday: 'MON', cron: '', timezone: 'UTC', ...over,
});

describe('buildCron', () => {
  it('builds the six-field cron for each preset', () => {
    expect(buildCron(sched())).toBe('0 0 9 * * MON-FRI');
    expect(buildCron(sched({ preset: 'DAILY', time: '17:45' }))).toBe('0 45 17 * * *');
    expect(buildCron(sched({ preset: 'WEEKLY', time: '08:05', weekday: 'FRI' }))).toBe('0 5 8 * * FRI');
  });

  it('passes a custom cron through, trimmed', () => {
    expect(buildCron(sched({ preset: 'CUSTOM', cron: '  0 0 10 * * MON,WED,FRI ' }))).toBe('0 0 10 * * MON,WED,FRI');
  });

  it('stores the cron with its timezone, defaulting to UTC', () => {
    expect(JSON.parse(buildScheduleConfig(sched({ timezone: 'Asia/Tehran' }))))
      .toEqual({ cron: '0 0 9 * * MON-FRI', timezone: 'Asia/Tehran' });
    expect(JSON.parse(buildScheduleConfig(sched({ timezone: '  ' }))).timezone).toBe('UTC');
  });
});

describe('parseScheduleConfig', () => {
  it('round-trips every preset', () => {
    for (const s of [
      sched(),
      sched({ preset: 'DAILY', time: '07:30' }),
      sched({ preset: 'WEEKLY', time: '16:00', weekday: 'THU' }),
    ]) {
      expect(parseScheduleConfig(buildScheduleConfig(s))).toMatchObject({
        preset: s.preset, time: s.time, weekday: s.preset === 'WEEKLY' ? s.weekday : 'MON', timezone: 'UTC',
      });
    }
  });

  it('recognises the five-field form too', () => {
    expect(parseScheduleConfig('{"cron":"30 8 * * MON-FRI"}')).toMatchObject({ preset: 'WEEKDAYS', time: '08:30' });
  });

  it('falls back to a custom cron for anything it cannot express with a preset', () => {
    const p = parseScheduleConfig('{"cron":"0 0 10 * * MON,WED,FRI","timezone":"Europe/Berlin"}');
    expect(p).toMatchObject({ preset: 'CUSTOM', cron: '0 0 10 * * MON,WED,FRI', timezone: 'Europe/Berlin' });
    expect(parseScheduleConfig('{"cron":"0 */15 * * * *"}').preset).toBe('CUSTOM');
    expect(parseScheduleConfig('{"cron":"0 0 9 1 * *"}').preset).toBe('CUSTOM'); // day-of-month set
  });

  it('survives missing, empty and broken input', () => {
    for (const bad of [undefined, null, '', 'not json', '{}']) {
      expect(parseScheduleConfig(bad).preset).toBe('WEEKDAYS');
    }
  });
});

describe('scheduleProblem', () => {
  it('flags an empty custom cron and a malformed time', () => {
    expect(scheduleProblem(sched({ preset: 'CUSTOM', cron: ' ' }))).toBe('cron');
    expect(scheduleProblem(sched({ time: '25:00' }))).toBe('time');
    expect(scheduleProblem(sched({ time: '' }))).toBe('time');
    expect(scheduleProblem(sched())).toBeNull();
    expect(scheduleProblem(defaultSchedule())).toBeNull();
  });
});

describe('agent loop settings', () => {
  it('writes only the settings that apply to each loop', () => {
    const f = { ...defaultAgentForm(), channels: ['SLACK', 'IN_APP'] as const, lookbackHours: 48, onlyWhenAtRisk: false };
    const form = { ...f, channels: [...f.channels] };
    expect(JSON.parse(buildAgentConfig(form, 'AGENT_STANDUP_DIGEST')))
      .toEqual({ channels: ['SLACK', 'IN_APP'], lookbackHours: 48, useAi: true });
    expect(JSON.parse(buildAgentConfig(form, 'AGENT_CYCLE_HEALTH_CHECK')))
      .toEqual({ channels: ['SLACK', 'IN_APP'], onlyWhenAtRisk: false, useAi: true });
    expect(JSON.parse(buildAgentConfig(form, 'AGENT_BETTING_PREP')))
      .toEqual({ channels: ['SLACK', 'IN_APP'], useAi: true });
  });

  it('includes a trimmed channel override only when set', () => {
    expect(JSON.parse(buildAgentConfig({ ...defaultAgentForm(), channel: ' #eng ' }, 'AGENT_BETTING_PREP')).channel).toBe('#eng');
    expect(JSON.parse(buildAgentConfig(defaultAgentForm(), 'AGENT_BETTING_PREP'))).not.toHaveProperty('channel');
  });

  it('round-trips and ignores unknown channels', () => {
    const json = '{"channels":["SLACK","EMAIL","TEAMS"],"channel":"#x","lookbackHours":12,"onlyWhenAtRisk":false,"useAi":false}';
    expect(parseAgentConfig(json)).toEqual({
      channels: ['SLACK', 'TEAMS'], channel: '#x', lookbackHours: 12, onlyWhenAtRisk: false, useAi: false,
    });
  });

  it('defaults safely for missing or broken input', () => {
    for (const bad of [undefined, null, '', 'oops', '{}']) {
      expect(parseAgentConfig(bad)).toEqual(defaultAgentForm());
    }
    expect(parseAgentConfig('{"channels":[]}').channels).toEqual(['IN_APP']);
  });

  it('agentProblem catches the same mistakes the server would', () => {
    const ok = defaultAgentForm();
    expect(agentProblem(ok, 'AGENT_STANDUP_DIGEST')).toBeNull();
    expect(agentProblem({ ...ok, channels: [] }, 'AGENT_STANDUP_DIGEST')).toBe('channels');
    expect(agentProblem({ ...ok, lookbackHours: 0 }, 'AGENT_STANDUP_DIGEST')).toBe('lookback');
    expect(agentProblem({ ...ok, lookbackHours: 169 }, 'AGENT_STANDUP_DIGEST')).toBe('lookback');
    expect(agentProblem({ ...ok, lookbackHours: 1.5 }, 'AGENT_STANDUP_DIGEST')).toBe('lookback');
    expect(agentProblem({ ...ok, lookbackHours: 0 }, 'AGENT_CYCLE_HEALTH_CHECK')).toBeNull(); // not used there
    expect(agentProblem({ ...ok, channel: '<!channel>' }, 'AGENT_BETTING_PREP')).toBe('channelName');
    expect(agentProblem({ ...ok, channel: '#eng-standup' }, 'AGENT_BETTING_PREP')).toBeNull();
  });
});

describe('which triggers a loop may use', () => {
  it('knows the three loop actions', () => {
    expect(isAgentLoop('AGENT_STANDUP_DIGEST')).toBe(true);
    expect(isAgentLoop('AGENT_BETTING_PREP')).toBe(true);
    expect(isAgentLoop('NOTIFY_ASSIGNEE')).toBe(false);
  });

  it('standup and health need a schedule; betting prep may also run when a cycle ends; others are unrestricted', () => {
    expect(allowedTriggers('AGENT_STANDUP_DIGEST')).toEqual(['SCHEDULE']);
    expect(allowedTriggers('AGENT_CYCLE_HEALTH_CHECK')).toEqual(['SCHEDULE']);
    expect(allowedTriggers('AGENT_BETTING_PREP')).toEqual(['SCHEDULE', 'CYCLE_ENDED']);
    expect(allowedTriggers('NOTIFY_PROJECT_MEMBERS')).toBeNull();
  });
});
