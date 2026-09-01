import { describe, it, expect } from 'vitest';
import {
  PROJECT_TYPE_CAPABILITIES,
  resolveOrgCapabilities,
  resolveCapabilities,
} from '../projectTypeCapabilities';

describe('resolveOrgCapabilities', () => {
  it('falls back to the minimal Kanban baseline for an org with zero projects', () => {
    expect(resolveOrgCapabilities([])).toBe(PROJECT_TYPE_CAPABILITIES.KANBAN);
  });

  it('resolves a Kanban-only org to Kanban capabilities (no cycles/pitches)', () => {
    const capabilities = resolveOrgCapabilities(['KANBAN']);
    expect(capabilities).toBe(PROJECT_TYPE_CAPABILITIES.KANBAN);
    expect(capabilities.hasCycles).toBe(false);
    expect(capabilities.hasPitches).toBe(false);
    expect(capabilities.nav.showWorkspace).toBe(false);
    expect(capabilities.quickLinkIds).not.toContain('newCycle');
    expect(capabilities.quickLinkIds).not.toContain('viewPitches');
  });

  it('resolves a Scrum-only org to Scrum capabilities (cycles, no pitches)', () => {
    const capabilities = resolveOrgCapabilities(['SCRUM']);
    expect(capabilities).toBe(PROJECT_TYPE_CAPABILITIES.SCRUM);
    expect(capabilities.hasCycles).toBe(true);
    expect(capabilities.hasPitches).toBe(false);
    expect(capabilities.isScrum).toBe(true);
    expect(capabilities.quickLinkIds).not.toContain('viewPitches');
    expect(capabilities.dashboard.showTotalPitchesStat).toBe(false);
    // CYCLE_PROGRESS is included for Scrum — CycleProgressWidget.tsx computes
    // it from Task data ("stories") for Scrum, not Pitch data.
    expect(capabilities.dashboard.overviewWidgetTypes).toContain('CYCLE_PROGRESS');
    expect(capabilities.defaultWidgetTypes).toContain('CYCLE_PROGRESS');
    expect(capabilities.defaultWidgetTypes).not.toContain('HILL_CHART');
    expect(capabilities.defaultWidgetTypes).not.toContain('RECENT_PITCHES');
  });

  it('resolves a Shape-Up-only org to the full Shape Up capability set', () => {
    const capabilities = resolveOrgCapabilities(['SHAPE_UP']);
    expect(capabilities).toBe(PROJECT_TYPE_CAPABILITIES.SHAPE_UP);
    expect(capabilities.hasCycles).toBe(true);
    expect(capabilities.hasPitches).toBe(true);
  });

  it('resolves a mixed org containing Shape Up to the richest (Shape Up) set', () => {
    const capabilities = resolveOrgCapabilities(['KANBAN', 'SHAPE_UP', 'SCRUM']);
    expect(capabilities).toBe(PROJECT_TYPE_CAPABILITIES.SHAPE_UP);
  });

  it('resolves a Scrum+Kanban mix (no Shape Up) to Scrum, not the Kanban baseline', () => {
    const capabilities = resolveOrgCapabilities(['KANBAN', 'SCRUM']);
    expect(capabilities).toBe(PROJECT_TYPE_CAPABILITIES.SCRUM);
  });

  it('de-duplicates repeated project types before resolving', () => {
    const capabilities = resolveOrgCapabilities(['KANBAN', 'KANBAN', 'KANBAN']);
    expect(capabilities).toBe(PROJECT_TYPE_CAPABILITIES.KANBAN);
  });
});

describe('resolveCapabilities', () => {
  it('prefers the currently selected project type over the org aggregate', () => {
    const capabilities = resolveCapabilities('KANBAN', ['SHAPE_UP', 'SCRUM']);
    expect(capabilities).toBe(PROJECT_TYPE_CAPABILITIES.KANBAN);
  });

  it('falls back to the org aggregate in "All Projects" mode (currentProjectType null)', () => {
    // This is the fix for the root-cause bug: a Kanban-only org must not default
    // to Shape Up capabilities just because no specific project is selected.
    const capabilities = resolveCapabilities(null, ['KANBAN']);
    expect(capabilities).toBe(PROJECT_TYPE_CAPABILITIES.KANBAN);
  });

  it('resolves "All Projects" with no projects yet to the minimal baseline, not Shape Up', () => {
    const capabilities = resolveCapabilities(null, []);
    expect(capabilities).toBe(PROJECT_TYPE_CAPABILITIES.KANBAN);
  });
});
