import { describe, it, expect } from 'vitest';
import { cycleParamValue, parseCycleParam } from '../backlogCycleParam';

describe('backlog cycle URL param', () => {
  it('writes "All cycles" into the link so it is not replaced by the active cycle', () => {
    expect(cycleParamValue('all', false)).toBe('all');
  });

  it('writes a specific cycle id', () => {
    expect(cycleParamValue(42, false)).toBe('42');
  });

  it('writes nothing for Kanban projects, which have no cycles', () => {
    expect(cycleParamValue('all', true)).toBeNull();
    expect(cycleParamValue(42, true)).toBeNull();
  });

  it('round-trips every selection through the URL', () => {
    for (const selected of ['all', 7] as const) {
      expect(parseCycleParam(cycleParamValue(selected, false))).toBe(selected);
    }
  });

  it('treats a missing or garbage param as "not in the link"', () => {
    expect(parseCycleParam(null)).toBeNull();
    expect(parseCycleParam('')).toBeNull();
    expect(parseCycleParam('abc')).toBeNull();
  });
});
