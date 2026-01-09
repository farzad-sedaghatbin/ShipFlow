import { ChevronDown, Settings, Plus, LayoutGrid, Loader2 } from 'lucide-react';
import { useProject } from '../contexts';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

export default function ProjectSelector() {
  const { projects, currentProject, loading, selectProject, selectAllProjects, isAllProjectsSelected } = useProject();
  const navigate = useNavigate();

  const handleSelectProject = (project: typeof projects[0]) => {
    selectProject(project);
  };

  const handleSelectAllProjects = () => {
    selectAllProjects();
  };

  const handleManageProjects = () => {
    navigate('/projects');
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-2">
        <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" aria-label="Loading projects" />
      </div>
    );
  }

  if (projects.length === 0) {
    return (
      <Button
        variant="outline"
        size="sm"
        onClick={handleManageProjects}
        aria-label="Create your first project"
      >
        <Plus className="h-4 w-4 mr-2" />
        Create Project
      </Button>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className="flex items-center gap-2 min-w-[150px] justify-start"
          aria-label={`Current project: ${isAllProjectsSelected ? 'All Projects' : (currentProject?.name || 'Select Project')}. Click to change project`}
        >
          {isAllProjectsSelected ? (
            <LayoutGrid className="h-5 w-5 text-muted-foreground" />
          ) : (
            <Avatar className="h-6 w-6">
              <AvatarFallback
                className="text-xs"
                style={{ backgroundColor: currentProject?.color || '#8B5CF6' }}
              >
                {currentProject?.projectKey.substring(0, 2) || '?'}
              </AvatarFallback>
            </Avatar>
          )}
          <span className="max-w-[120px] truncate text-sm font-medium">
            {isAllProjectsSelected ? 'All Projects' : (currentProject?.name || 'Select Project')}
          </span>
          <ChevronDown className="h-4 w-4 text-muted-foreground ml-auto" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-56">
        {/* All Projects Option */}
        <DropdownMenuItem
          onClick={handleSelectAllProjects}
          className={cn(
            "flex items-center gap-3",
            isAllProjectsSelected && "bg-accent"
          )}
        >
          <LayoutGrid className="h-4 w-4" />
          <div className="flex flex-col">
            <span className={cn("text-sm", isAllProjectsSelected && "font-semibold")}>
              All Projects
            </span>
            <span className="text-xs text-muted-foreground">
              View data from all projects
            </span>
          </div>
        </DropdownMenuItem>
        
        <DropdownMenuSeparator />
        
        {projects.map((project) => (
          <DropdownMenuItem
            key={project.id}
            onClick={() => handleSelectProject(project)}
            className={cn(
              "flex items-center gap-3",
              currentProject?.id === project.id && "bg-accent"
            )}
          >
            <Avatar className="h-6 w-6">
              <AvatarFallback
                className="text-xs"
                style={{ backgroundColor: project.color || '#8B5CF6' }}
              >
                {project.projectKey.substring(0, 2)}
              </AvatarFallback>
            </Avatar>
            <div className="flex flex-col min-w-0">
              <span className="text-sm truncate">{project.name}</span>
              <span className="text-xs text-muted-foreground">{project.projectKey}</span>
            </div>
          </DropdownMenuItem>
        ))}
        
        <DropdownMenuSeparator />
        
        <DropdownMenuItem onClick={handleManageProjects} className="flex items-center gap-3">
          <Settings className="h-4 w-4" />
          <span>Manage Projects</span>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
