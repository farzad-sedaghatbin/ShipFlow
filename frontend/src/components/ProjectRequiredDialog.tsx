import { useTranslation } from 'react-i18next';
import { FolderOpen } from 'lucide-react';
import { useProject } from '../contexts';
import type { Project } from '../types';
import { Avatar, AvatarFallback } from './ui/avatar';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from './ui/dialog';
import { cn } from '@/lib/utils';

interface ProjectRequiredDialogProps {
  /** Whether the dialog is open (typically when isAllProjectsSelected is true) */
  open: boolean;
  /** Optional contextual description, e.g. "to view its epics" */
  featureDescription?: string;
}

/**
 * A modal dialog that prompts the user to select a specific project
 * when they navigate to a page that requires one.
 * Shows a list of available projects for quick selection.
 */
export default function ProjectRequiredDialog({
  open,
  featureDescription,
}: ProjectRequiredDialogProps) {
  const { t } = useTranslation();
  const { projects, selectProject, currentProject } = useProject();

  const handleSelectProject = (project: Project) => {
    selectProject(project);
  };

  return (
    <Dialog open={open}>
      <DialogContent
        className="sm:max-w-md"
        onInteractOutside={(e) => e.preventDefault()}
        onEscapeKeyDown={(e) => e.preventDefault()}
      >
        <DialogHeader className="text-center sm:text-center">
          <div className="mx-auto mb-2 flex h-14 w-14 items-center justify-center rounded-full bg-primary/10">
            <FolderOpen className="h-7 w-7 text-primary" />
          </div>
          <DialogTitle className="text-xl">
            {t('projectRequired.title')}
          </DialogTitle>
          <DialogDescription className="text-sm text-muted-foreground">
            {featureDescription || t('projectRequired.description')}
          </DialogDescription>
        </DialogHeader>

        <div className="mt-2 max-h-[300px] overflow-y-auto">
          <div className="space-y-1">
            {projects.map((project) => (
              <button
                key={project.id}
                onClick={() => handleSelectProject(project)}
                className={cn(
                  'flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition-colors',
                  'hover:bg-accent focus-visible:bg-accent focus-visible:outline-none',
                  currentProject?.id === project.id && 'bg-accent'
                )}
              >
                <Avatar className="h-8 w-8 shrink-0">
                  <AvatarFallback
                    className="text-xs font-medium text-white"
                    style={{ backgroundColor: project.color || '#8B5CF6' }}
                  >
                    {project.projectKey.substring(0, 2)}
                  </AvatarFallback>
                </Avatar>
                <div className="flex flex-col min-w-0">
                  <span className="text-sm font-medium truncate">{project.name}</span>
                  <span className="text-xs text-muted-foreground">{project.projectKey}</span>
                </div>
              </button>
            ))}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
