import { createContext, useContext, useState, useEffect, useCallback, ReactNode, useMemo, useRef } from 'react';
import { Project, ProjectType } from '../types';
import projectService from '../services/projectService';

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
  /** Returns true if current project uses Shape Up methodology */
  isShapeUpProject: boolean;
  /** Returns the project type of the current project, or null if all projects selected */
  currentProjectType: ProjectType | null;
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

      // Try to restore previously selected project
      const savedProjectId = localStorage.getItem(SELECTED_PROJECT_KEY);
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
          localStorage.setItem(SELECTED_PROJECT_KEY, ALL_PROJECTS_VALUE);
        }
      } else {
        // Default to "All Projects" for new users
        setCurrentProject(null);
        localStorage.setItem(SELECTED_PROJECT_KEY, ALL_PROJECTS_VALUE);
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
    if (project) {
      localStorage.setItem(SELECTED_PROJECT_KEY, project.id.toString());
    } else {
      localStorage.setItem(SELECTED_PROJECT_KEY, ALL_PROJECTS_VALUE);
    }
  }, []);

  const selectAllProjects = useCallback(() => {
    // Increment switch ID to track this specific switch operation
    switchIdRef.current += 1;
    setIsSwitchingProject(true);
    setCurrentProject(null);
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
  
  const isShapeUpProject = useMemo(() => 
    currentProject?.projectType === 'SHAPE_UP' || currentProject === null, 
    [currentProject]
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
        currentProjectType,
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
