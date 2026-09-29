import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import type { ReactNode } from 'react';
import { ProjectProvider, useProject } from '../ProjectContext';
import { Project } from '../../types';

const SELECTED_PROJECT_KEY = 'shipflow_selected_project_id';

const projectA: Project = {
  id: 1,
  name: 'Project A',
  projectKey: 'PA',
  isActive: true,
  projectType: 'SHAPE_UP',
  createdAt: '2026-01-01T00:00:00Z',
};

const projectB: Project = {
  id: 2,
  name: 'Project B',
  projectKey: 'PB',
  isActive: true,
  projectType: 'KANBAN',
  createdAt: '2026-01-01T00:00:00Z',
};

vi.mock('../../services/projectService', () => ({
  default: {
    getActive: vi.fn(async () => [projectA, projectB]),
  },
}));

function wrapper({ children }: { children: ReactNode }) {
  return <ProjectProvider>{children}</ProjectProvider>;
}

describe('ProjectContext — per-tab project selection (sessionStorage)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    window.history.replaceState({}, '', '/');
  });

  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it('seeds this tab from localStorage on first mount and mirrors it into sessionStorage', async () => {
    localStorage.setItem(SELECTED_PROJECT_KEY, String(projectB.id));

    const { result } = renderHook(() => useProject(), { wrapper });

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.currentProject?.id).toBe(projectB.id);
    // The seed is mirrored into sessionStorage so this tab tracks independently from now on.
    expect(sessionStorage.getItem(SELECTED_PROJECT_KEY)).toBe(String(projectB.id));
  });

  it('prefers its own sessionStorage value over localStorage when both are present', async () => {
    localStorage.setItem(SELECTED_PROJECT_KEY, String(projectB.id));
    sessionStorage.setItem(SELECTED_PROJECT_KEY, String(projectA.id));

    const { result } = renderHook(() => useProject(), { wrapper });

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.currentProject?.id).toBe(projectA.id);
  });

  it('selectProject writes the live value to sessionStorage (and seeds localStorage for future new tabs)', async () => {
    const { result } = renderHook(() => useProject(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.selectProject(projectA);
    });

    expect(sessionStorage.getItem(SELECTED_PROJECT_KEY)).toBe(String(projectA.id));
    expect(localStorage.getItem(SELECTED_PROJECT_KEY)).toBe(String(projectA.id));
  });

  it('selectAllProjects records "all" in sessionStorage', async () => {
    const { result } = renderHook(() => useProject(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.selectAllProjects();
    });

    expect(sessionStorage.getItem(SELECTED_PROJECT_KEY)).toBe('all');
    expect(result.current.isAllProjectsSelected).toBe(true);
  });
});

describe('ProjectContext — followEntityProject (cross-project deep links)', () => {
  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
    window.history.replaceState({}, '', '/');
  });

  it("switches this tab to the entity's project without touching the new-tab default", async () => {
    localStorage.setItem(SELECTED_PROJECT_KEY, String(projectA.id));
    const { result } = renderHook(() => useProject(), { wrapper });
    await waitFor(() => expect(result.current.currentProject?.id).toBe(projectA.id));

    act(() => {
      result.current.followEntityProject(projectB.id);
    });

    expect(result.current.currentProject?.id).toBe(projectB.id);
    expect(sessionStorage.getItem(SELECTED_PROJECT_KEY)).toBe(String(projectB.id));
    expect(localStorage.getItem(SELECTED_PROJECT_KEY)).toBe(String(projectA.id));
    expect(result.current.isSwitchingProject).toBe(false);
  });

  it('keeps "All Projects" mode and ignores unknown projects', async () => {
    localStorage.setItem(SELECTED_PROJECT_KEY, 'all');
    const { result } = renderHook(() => useProject(), { wrapper });
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => {
      result.current.followEntityProject(projectB.id);
    });
    expect(result.current.currentProject).toBeNull();

    act(() => {
      result.current.selectProject(projectA);
    });
    act(() => {
      result.current.followEntityProject(999);
    });
    expect(result.current.currentProject?.id).toBe(projectA.id);
  });
});
