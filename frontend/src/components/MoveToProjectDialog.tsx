import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FolderInput, Loader2 } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from './ui/dialog';
import { Button } from './ui/button';
import { Label } from './ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { useToast } from '../contexts';
import projectService from '../services/projectService';
import { pitchService } from '../services/pitchService';
import { taskService } from '../services/taskService';
import qaTestManagementService from '../services/qaTestManagementService';
import { Project } from '../types';

type EntityType = 'task' | 'pitch' | 'bug';

interface MoveToProjectDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  entityType: EntityType;
  entityId: number;
  entityTitle: string;
  currentProjectId?: number;
  onSuccess?: () => void;
}

export function MoveToProjectDialog({
  open,
  onOpenChange,
  entityType,
  entityId,
  entityTitle,
  currentProjectId,
  onSuccess,
}: MoveToProjectDialogProps) {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loadingProjects, setLoadingProjects] = useState(false);
  const [selectedProjectId, setSelectedProjectId] = useState<string>('');
  const [moving, setMoving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setSelectedProjectId('');
    setLoadingProjects(true);
    projectService.getAll().then((all) => {
      setProjects(currentProjectId ? all.filter((p) => p.id !== currentProjectId) : all);
    }).catch(() => {
      showToast(t('moveToProject.loadProjectsFailed'), 'error');
    }).finally(() => setLoadingProjects(false));
  }, [open, currentProjectId]);

  const handleMove = async () => {
    if (!selectedProjectId) return;
    const targetId = Number(selectedProjectId);
    setMoving(true);
    try {
      if (entityType === 'task') {
        await taskService.moveToProject(entityId, targetId);
      } else if (entityType === 'pitch') {
        await pitchService.moveToProject(entityId, targetId);
      } else {
        await qaTestManagementService.moveBugToProject(entityId, targetId);
      }
      const targetName = projects.find((p) => p.id === targetId)?.name ?? '';
      showToast(t('moveToProject.success', { entity: entityTitle, project: targetName }), 'success');
      onOpenChange(false);
      onSuccess?.();
    } catch {
      showToast(t('moveToProject.failed'), 'error');
    } finally {
      setMoving(false);
    }
  };

  const entityTypeLabel = entityType === 'task'
    ? t('moveToProject.entityTask')
    : entityType === 'pitch'
    ? t('moveToProject.entityPitch')
    : t('moveToProject.entityBug');

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FolderInput className="h-5 w-5 text-primary" />
            {t('moveToProject.title', { type: entityTypeLabel })}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <p className="text-sm text-muted-foreground">
            {t('moveToProject.description', { entity: entityTitle })}
          </p>

          <div className="space-y-1.5">
            <Label>{t('moveToProject.selectProject')}</Label>
            {loadingProjects ? (
              <div className="flex items-center gap-2 py-2">
                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                <span className="text-sm text-muted-foreground">{t('common.loading')}</span>
              </div>
            ) : (
              <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
                <SelectTrigger>
                  <SelectValue placeholder={t('moveToProject.selectPlaceholder')} />
                </SelectTrigger>
                <SelectContent>
                  {projects.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      <span className="font-mono text-xs text-muted-foreground mr-2">{p.projectKey}</span>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>

          {entityType === 'task' && (
            <p className="text-xs text-muted-foreground">
              {t('moveToProject.taskNote')}
            </p>
          )}
          {entityType === 'pitch' && (
            <p className="text-xs text-muted-foreground">
              {t('moveToProject.pitchNote')}
            </p>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={moving}>
            {t('common.cancel')}
          </Button>
          <Button onClick={handleMove} disabled={!selectedProjectId || moving || loadingProjects}>
            {moving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
            {t('moveToProject.confirm')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default MoveToProjectDialog;
