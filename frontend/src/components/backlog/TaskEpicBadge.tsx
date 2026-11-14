import { Link } from 'react-router-dom';
import { Layers } from 'lucide-react';
import { Badge } from '../ui/badge';
import { cn } from '@/lib/utils';
import { Task } from '../../types';

/**
 * Shows which epic a task rolls up to (Kanban/Scrum projects), as a badge linking to the epic.
 * Renders nothing for a task without an epic, so callers can drop it in unconditionally.
 */
export function TaskEpicBadge({ task, className }: { task: Pick<Task, 'epicId' | 'epicName'>; className?: string }) {
  if (!task.epicId) return null;
  const name = task.epicName || `#${task.epicId}`;
  return (
    <Link
      to={`/epics/${task.epicId}`}
      // The badge sits inside clickable rows/cards; following the epic link must not also open the task.
      onClick={(e) => e.stopPropagation()}
      title={name}
      className={cn('inline-flex max-w-[12rem]', className)}
      data-testid="task-epic-badge"
    >
      <Badge variant="secondary" className="gap-1 text-[10px] max-w-full">
        <Layers className="h-3 w-3 shrink-0" />
        <span className="truncate">{name}</span>
      </Badge>
    </Link>
  );
}
