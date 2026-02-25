import { useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { GripVertical, FileEdit, Trash2, Tag } from 'lucide-react';
import { Pitch, PitchStatusUtils, BusinessValue } from '../types';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Progress } from './ui/progress';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import PriorityBadge from './PriorityBadge';
import { cn } from '../lib/utils';

// ========================
// Sortable Pitch Item
// ========================

interface SortablePitchItemProps {
  pitch: Pitch;
  onUnlink?: (id: number) => void;
  onStartShaping?: (id: number) => void;
  onPriorityChange?: (id: number, priority: BusinessValue) => void;
  getPitchStatusVariant: (status: string) => 'default' | 'secondary' | 'destructive' | 'outline';
}

function SortablePitchItem({
  pitch,
  onUnlink,
  onStartShaping,
  onPriorityChange,
  getPitchStatusVariant,
}: SortablePitchItemProps) {
  const { t } = useTranslation();
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: pitch.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        'flex items-center justify-between p-4 rounded-lg border hover:bg-muted/50 group',
        isDragging && 'opacity-50 shadow-lg z-50 bg-background'
      )}
    >
      {/* Drag handle */}
      <div
        {...attributes}
        {...listeners}
        className="cursor-grab active:cursor-grabbing mr-3 text-muted-foreground hover:text-foreground touch-none"
      >
        <GripVertical className="h-5 w-5" />
      </div>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <Link
            to={`/pitches/${pitch.id}`}
            className="font-medium hover:underline truncate"
          >
            {pitch.title}
          </Link>
          <PriorityBadge priority={pitch.priority} />
          {pitch.targetReleaseVersion && (
            <Badge variant="outline" className="text-[10px] px-1.5 py-0 bg-blue-50 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 border-blue-200 dark:border-blue-700">
              <Tag className="h-2.5 w-2.5 mr-0.5" />
              v{pitch.targetReleaseVersion}
            </Badge>
          )}
        </div>
        {pitch.description && (
          <p className="text-sm text-muted-foreground line-clamp-1">
            {pitch.description}
          </p>
        )}
        <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground">
          {pitch.cycleName && <span>{pitch.cycleName}</span>}
          {!pitch.cycleName && pitch.status && PitchStatusUtils.isPreCycle(pitch.status) && (
            <span className="italic">{t('pitches.noCycleAssigned')}</span>
          )}
          {pitch.teamName && (
            <>
              <span>•</span>
              <span>{pitch.teamName}</span>
            </>
          )}
          {pitch.appetiteDays && (
            <>
              <span>•</span>
              <span>{pitch.appetiteDays} {t('pitches.days')}</span>
            </>
          )}
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-2 ml-4 flex-shrink-0">
        {/* Priority selector */}
        {onPriorityChange && (
          <Select
            value={pitch.priority || ''}
            onValueChange={(v) => onPriorityChange(pitch.id, v as BusinessValue)}
          >
            <SelectTrigger className="h-7 w-24 text-xs">
              <SelectValue placeholder={t('priority.set')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="HIGH">{t('priority.high')}</SelectItem>
              <SelectItem value="MEDIUM">{t('priority.medium')}</SelectItem>
              <SelectItem value="LOW">{t('priority.low')}</SelectItem>
            </SelectContent>
          </Select>
        )}

        <Badge variant={getPitchStatusVariant(pitch.status)}>
          {t(`pitches.status.${pitch.status.toLowerCase()}`)}
        </Badge>

        {/* Start shaping button for IDEAS */}
        {pitch.status === 'IDEA' && onStartShaping && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => onStartShaping(pitch.id)}
            title={t('pitches.startShaping')}
          >
            <FileEdit className="h-4 w-4 mr-1" />
            {t('pitches.shape')}
          </Button>
        )}

        {/* Progress for in-cycle pitches */}
        {pitch.cycleId && (
          <div className="w-24">
            <Progress value={pitch.progressPercentage || 0} className="h-2" />
          </div>
        )}

        {onUnlink && (
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onUnlink(pitch.id)}
            title={t('epics.unlinkPitch')}
          >
            <Trash2 className="h-4 w-4 text-muted-foreground" />
          </Button>
        )}
      </div>
    </div>
  );
}

// ========================
// SortablePitchList
// ========================

interface SortablePitchListProps {
  pitches: Pitch[];
  onReorder: (pitches: Pitch[]) => void;
  onUnlink?: (id: number) => void;
  onStartShaping?: (id: number) => void;
  onPriorityChange?: (id: number, priority: BusinessValue) => void;
  emptyMessage?: string;
}

const getPitchStatusVariant = (status: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (status) {
    case 'DONE': return 'default';
    case 'IN_PROGRESS': case 'TESTING': case 'STARTED': case 'DRAFT': return 'secondary';
    case 'SHAPED': return 'default';
    case 'PENDING': case 'IDEA': case 'COOLDOWN': return 'outline';
    case 'CANCELLED': return 'destructive';
    default: return 'outline';
  }
};

export default function SortablePitchList({
  pitches,
  onReorder,
  onUnlink,
  onStartShaping,
  onPriorityChange,
  emptyMessage,
}: SortablePitchListProps) {
  const { t } = useTranslation();

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const handleDragEnd = useCallback(
    (event: DragEndEvent) => {
      const { active, over } = event;
      if (!over || active.id === over.id) return;

      const oldIndex = pitches.findIndex((p) => p.id === active.id);
      const newIndex = pitches.findIndex((p) => p.id === over.id);

      if (oldIndex !== -1 && newIndex !== -1) {
        const reordered = arrayMove(pitches, oldIndex, newIndex);
        onReorder(reordered);
      }
    },
    [pitches, onReorder]
  );

  if (pitches.length === 0) {
    return (
      <div className="py-8 text-center text-muted-foreground">
        <p>{emptyMessage || t('epics.noPitches')}</p>
      </div>
    );
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragEnd={handleDragEnd}
    >
      <SortableContext
        items={pitches.map((p) => p.id)}
        strategy={verticalListSortingStrategy}
      >
        <div className="space-y-2">
          {pitches.map((pitch) => (
            <SortablePitchItem
              key={pitch.id}
              pitch={pitch}
              onUnlink={onUnlink}
              onStartShaping={onStartShaping}
              onPriorityChange={onPriorityChange}
              getPitchStatusVariant={getPitchStatusVariant}
            />
          ))}
        </div>
      </SortableContext>
    </DndContext>
  );
}
