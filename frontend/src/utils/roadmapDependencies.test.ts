import { describe, it, expect } from 'vitest';
import { getBarExtent, hasDependencyConflict, buildConnectorPath, computeConnectors } from './roadmapDependencies';

const start = new Date('2026-01-01');
const end = new Date('2026-04-01'); // 90 days

describe('roadmapDependencies', () => {
  it('computes bar extent like the bars do', () => {
    const e = getBarExtent(new Date('2026-01-01'), new Date('2026-01-10'), start, end)!;
    expect(e.left).toBe(0);
    expect(e.width).toBeCloseTo(10 / 90, 5);
  });

  it('returns null without dates', () => {
    expect(getBarExtent(null, null, start, end)).toBeNull();
  });

  it('detects conflicts only when target starts before source ends', () => {
    expect(hasDependencyConflict('2026-02-10', '2026-02-01')).toBe(true);
    expect(hasDependencyConflict('2026-02-10', '2026-02-10')).toBe(false);
    expect(hasDependencyConflict('2026-02-10', '2026-03-01')).toBe(false);
    expect(hasDependencyConflict(undefined, '2026-03-01')).toBe(false);
  });

  it('builds an elbow path', () => {
    expect(buildConnectorPath(0, 10, 100, 50)).toBe('M 0 10 H 50 V 50 H 100');
    expect(buildConnectorPath(100, 10, 90, 50)).toContain('V 50 H 90');
  });

  it('skips edges with a hidden epic and flags conflicts', () => {
    const epics = new Map([
      [1, { id: 1, startDate: '2026-01-01', endDate: '2026-02-01' }],
      [2, { id: 2, startDate: '2026-01-15', endDate: '2026-03-01' }],
      [3, { id: 3, startDate: '2026-02-10', endDate: '2026-03-01' }],
    ]);
    const centers = new Map([[1, 20], [2, 60]]);
    const out = computeConnectors(
      [{ id: 10, sourceEpicId: 1, targetEpicId: 2 }, { id: 11, sourceEpicId: 1, targetEpicId: 3 }],
      epics, centers, start, end, 900
    );
    expect(out).toHaveLength(1);
    expect(out[0].id).toBe(10);
    expect(out[0].conflict).toBe(true);
  });
});
