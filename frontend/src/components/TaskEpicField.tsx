import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Layers } from 'lucide-react';
import { epicService } from '../services/epicService';
import { taskService } from '../services/taskService';
import { Task } from '../types';
import { useProject, useToast } from '../contexts';
import { Label } from './ui/label';
import { Badge } from './ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';

const NONE = '__none__';

interface TaskEpicFieldProps {
  task: Task;
  onChanged: (task: Task) => void;
}

/**
 * Epic picker for a task in a Kanban/Scrum project (tasks sit directly under epics there).
 * Renders nothing for Shape Up, where pitches (not tasks) belong to epics.
 */
export default function TaskEpicField({ task, onChanged }: TaskEpicFieldProps) {
  const { t } = useTranslation();
  const { showError } = useToast();
  const { currentProject } = useProject();
  const projectId = task.projectId ?? currentProject?.id;
  const projectType = currentProject?.id === projectId ? currentProject?.projectType : undefined;
  const enabled = !!projectId && !!projectType && projectType !== 'SHAPE_UP';

  const { data: epics = [] } = useQuery({
    queryKey: ['epics', 'project', projectId],
    queryFn: async () => (await epicService.getByProject(projectId!)).data,
    enabled,
  });

  if (!enabled) return null;

  const handleChange = async (value: string) => {
    try {
      const res = await taskService.setEpic(task.id, value === NONE ? null : Number(value));
      onChanged(res.data);
    } catch (error) {
      console.error('Failed to change task epic:', error);
      showError(t('epics.taskLinkError'));
    }
  };

  return (
    <div>
      <Label className="text-xs text-muted-foreground">{t('epics.epic')}</Label>
      <div className="mt-1 space-y-2">
        {task.epicId && (
          <Link to={`/epics/${task.epicId}`}>
            <Badge variant="secondary" className="gap-1">
              <Layers className="h-3 w-3" />
              {task.epicName || `#${task.epicId}`}
            </Badge>
          </Link>
        )}
        <Select value={task.epicId ? String(task.epicId) : NONE} onValueChange={handleChange}>
          <SelectTrigger aria-label={t('epics.epic')}>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={NONE}>{t('epics.noEpic')}</SelectItem>
            {epics.map((e) => (
              <SelectItem key={e.id} value={String(e.id)}>
                {e.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
