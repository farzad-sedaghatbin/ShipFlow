import { createContext, useContext, useState, useEffect, useCallback, ReactNode, useMemo, useRef } from 'react';
import { Project, ProjectType } from '../types';
import projectService from '../services/projectService';
import { ProjectTypeCapabilities, resolveCapabilities } from '../config/projectTypeCapabilities';

// Special value to represent "All Projects" selection
export const ALL_PROJECTS_ID = -1;

interface ProjectContextType {
  projects: Project[];
  currentProject: Project | null; // null means "All Projects"
  loading: boolean;
  error: string | null;
  isAllProjectsSelected: boolean;
  /** Returns true when project is being switched (for loading indicators) */
  isSwitchingProject: boolean;
  /** Returns true if current project uses Kanban methodology */
  isKanbanProject: boolean;
  /**
   * Returns true if current project uses Shape Up methodology, OR if no specific
   * project is selected ("All Projects"). This legacy-compatible behavior keeps
   * Shape Up views (CycleList, BettingTable) functional when no project is active.
   * Callers that need strict Shape Up detection should use `isStrictlyShapeUp` instead.
   */
  isShapeUpProject: boolean;
  /**
   * Returns true only when a specific project is selected AND its type is SHAPE_UP.
   * Use this when you need to distinguish a real Shape Up project from the
   * "All Projects" null state (where `isShapeUpProject` returns true for compat).
   */
  isStrictlyShapeUp: boolean;
  /** Returns true if current project uses Scrum methodology */
  isScrumProject: boolean;
  /** Returns the project type of the current project, or null if all projects selected */
  currentProjectType: ProjectType | null;
  /** Distinct project types across the org's active projects (empty if none yet). */
  orgProjectTypes: ProjectType[];
  /**
   * Resolved capability set for the current view: the selected project's type,
   * or — in "All Projects" mode — the org's actual project-type mix rather than
   * a hardcoded Shape Up default. Nav, mobile tabs, quick links, keyboard
   * shortcuts, and dashboard stat visibility should all read from this instead
   * of re-deriving their own project-type checks.
   */
  capabilities: ProjectTypeCapabilities;
  selectProject: (project: Project | null) => void;
  selectAllProjects: () => void;
  refreshProjects: () => Promise<void>;
  /** Call when data has finished loading after project switch */
  notifyProjectSwitchComplete: () => void;
}

const ProjectContext = createContext<ProjectContextType | undefined>(undefined);

const SELECTED_PROJECT_KEY = 'shipflow_selected_project_id';
const ALL_PROJECTS_VALUE = 'all';

export function ProjectProvider({ children }: { children: ReactNode }) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [currentProject, setCurrentProject] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isSwitchingProject, setIsSwitchingProject] = useState(false);
  // Track switch operations to prevent race conditions
  const switchIdRef = useRef(0);

  const refreshProjects = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await projectService.getActive();
      setProjects(data);

      // Deep link: ?project=<id> (used by "open project in new tab") takes priority over the
      // last-selected project persisted in storage so the new tab lands on the right project.
      const urlProjectId = new URLSearchParams(window.location.search).get('project');
      if (urlProjectId) {
        const urlProject = data.find(p => p.id === parseInt(urlProjectId, 10));
        if (urlProject) {
          setCurrentProject(urlProject);
          sessionStorage.setItem(SELECTED_PROJECT_KEY, urlProject.id.toString());
          return;
        }
      }

      // Try to restore this tab's own previously selected project. sessionStorage is
      // per-tab (never shared across tabs), so switching projects in one tab can't bleed
      // into another. Only a brand-new tab (no sessionStorage entry yet) falls back to
      // localStorage as a one-time seed of "whatever was last selected anywhere" — and
      // immediately mirrors that seed into sessionStorage so this tab tracks independently
      // from then on, instead of re-reading localStorage on every future mount.
      let savedProjectId = sessionStorage.getItem(SELECTED_PROJECT_KEY);
      if (savedProjectId === null) {
        savedProjectId = localStorage.getItem(SELECTED_PROJECT_KEY);
        if (savedProjectId !== null) {
          sessionStorage.setItem(SELECTED_PROJECT_KEY, savedProjectId);
        }
      }

      if (savedProjectId === ALL_PROJECTS_VALUE) {
        // User previously selected "All Projects"
        setCurrentProject(null);
      } else if (savedProjectId) {
        const savedProject = data.find(p => p.id === parseInt(savedProjectId, 10));
        if (savedProject) {
          setCurrentProject(savedProject);
        } else {
          // Default to "All Projects" if saved project not found
          setCurrentProject(null);
          sessionStorage.setItem(SELECTED_PROJECT_KEY, ALL_PROJECTS_VALUE);
        }
      } else {
        // Default to "All Projects" for new users
        setCurrentProject(null);
        sessionStorage.setItem(SELECTED_PROJECT_KEY, ALL_PROJECTS_VALUE);
      }
    } catch (err: any) {
      if (err.name !== 'CanceledError') {
        setError('Failed to load projects');
        console.error('Error loading projects:', err);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const abortController = new AbortController();
    refreshProjects();
    return () => abortController.abort();
  }, [refreshProjects]);

  const selectProject = useCallback((project: Project | null) => {
    // Increment switch ID to track this specific switch operation
    switchIdRef.current += 1;
    setIsSwitchingProject(true);
    setCurrentProject(project);
    const value = project ? project.id.toString() : ALL_PROJECTS_VALUE;
    // sessionStorage is the live, per-tab value — never shared with other tabs.
    // localStorage is only ever read as a brand-new tab's one-time seed (see
    // refreshProjects), so it's kept in sync here purely so a *future* new tab
    // opens to this tab's latest choice rather than a stale one.
    sessionStorage.setItem(SELECTED_PROJECT_KEY, value);
    localStorage.setItem(SELECTED_PROJECT_KEY, value);
  }, []);

  const selectAllProjects = useCallback(() => {
    // Increment switch ID to track this specific switch operation
    switchIdRef.current += 1;
    setIsSwitchingProject(true);
    setCurrentProject(null);
    sessionStorage.setItem(SELECTED_PROJECT_KEY, ALL_PROJECTS_VALUE);
    localStorage.setItem(SELECTED_PROJECT_KEY, ALL_PROJECTS_VALUE);
  }, []);

  const notifyProjectSwitchComplete = useCallback((completedSwitchId?: number) => {
    // Only clear switching state if this completion matches the latest switch
    // This prevents race conditions when switching projects rapidly
    if (completedSwitchId === undefined || completedSwitchId === switchIdRef.current) {
      setIsSwitchingProject(false);
    }
  }, []);

  const isAllProjectsSelected = currentProject === null;

  // Computed properties for project type
  const currentProjectType = useMemo(() => 
    currentProject?.projectType ?? null, 
    [currentProject]
  );
  
  const isKanbanProject = useMemo(() =>
    currentProject?.projectType === 'KANBAN',
    [currentProject]
  );

  // null = "All Projects" selected: default to Shape Up behavior for legacy views
  // (CycleList, BettingTable) that rely on this flag being true when no specific
  // project is active. Callers that need strict "is this project Shape Up" should
  // also check isAllProjectsSelected.
  const isShapeUpProject = useMemo(() =>
    currentProject === null || currentProject?.projectType === 'SHAPE_UP',
    [currentProject]
  );

  const isScrumProject = useMemo(() =>
    currentProject?.projectType === 'SCRUM',
    [currentProject]
  );

  // Strict Shape Up check — true only when a real project with type SHAPE_UP is active.
  // Unlike isShapeUpProject, this returns false when "All Projects" (null) is selected.
  const isStrictlyShapeUp = useMemo(() =>
    currentProject?.projectType === 'SHAPE_UP',
    [currentProject]
  );

  // Distinct project types across the org's active projects — the ground truth
  // for what "All Projects" mode should actually show, instead of defaulting to
  // Shape Up regardless of what the org has.
  const orgProjectTypes = useMemo(() =>
    Array.from(new Set(projects.map(p => p.projectType))),
    [projects]
  );

  const capabilities = useMemo(() =>
    resolveCapabilities(currentProjectType, orgProjectTypes),
    [currentProjectType, orgProjectTypes]
  );

  return (
    <ProjectContext.Provider
      value={{
        projects,
        currentProject,
        loading,
        error,
        isAllProjectsSelected,
        isSwitchingProject,
        isKanbanProject,
        isShapeUpProject,
        isStrictlyShapeUp,
        isScrumProject,
        currentProjectType,
        orgProjectTypes,
        capabilities,
        selectProject,
        selectAllProjects,
        refreshProjects,
        notifyProjectSwitchComplete,
      }}
    >
      {children}
    </ProjectContext.Provider>
  );
}

export function useProject() {
  const context = useContext(ProjectContext);
  if (context === undefined) {
    throw new Error('useProject must be used within a ProjectProvider');
  }
  return context;
}

export default ProjectContext;
