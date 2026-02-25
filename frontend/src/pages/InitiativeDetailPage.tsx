import { useCallback, useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
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
import {
  ArrowLeft,
  Pencil,
  Trash2,
  Target,
  Calendar,
  Layers,
  Plus,
  User,
  Clock,
  GripVertical,
} from 'lucide-react';
import { initiativeService } from '../services/initiativeService';
import { epicService } from '../services/epicService';
import { Initiative, InitiativeStatus, Epic, ReorderRequest } from '../types';
import { cn } from '../lib/utils';
import { useToast } from '../contexts';

import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Progress } from '../components/ui/progress';
import { Skeleton } from '../components/ui/skeleton';
import { Separator } from '../components/ui/separator';
import { Markdown } from '../components/ui/markdown';

// ========================
// Sortable Epic Item
// ========================
function SortableEpicItem({ epic, getStatusBadgeVariant, t }: { epic: Epic; getStatusBadgeVariant: (s: string) => 'default' | 'secondary' | 'destructive' | 'outline'; t: (key: string) => string }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: epic.id });
  const style = { transform: CSS.Transform.toString(transform), transition };

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={cn(
        'flex items-center justify-between p-4 rounded-lg border hover:bg-muted/50 group',
        isDragging && 'opacity-50 shadow-lg z-50 bg-background'
      )}
    >
      <div
        {...attributes}
        {...listeners}
        className="cursor-grab active:cursor-grabbing mr-3 text-muted-foreground hover:text-foreground touch-none"
      >
        <GripVertical className="h-5 w-5" />
      </div>
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <div className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: epic.color || '#6366f1' }} />
        <div className="min-w-0">
          <Link to={`/epics/${epic.id}`} className="font-medium hover:underline">
            {epic.name}
          </Link>
          {epic.description && (
            <p className="text-sm text-muted-foreground line-clamp-1">{epic.description}</p>
          )}
        </div>
      </div>
      <div className="flex items-center gap-4 flex-shrink-0">
        <Badge variant={getStatusBadgeVariant(epic.status)}>
          {t(`epics.status.${epic.status.toLowerCase()}`)}
        </Badge>
        <div className="text-sm text-muted-foreground w-20 text-right">
          {epic.pitchCount || 0} {t('epics.pitches')}
        </div>
        <div className="w-32">
          <Progress value={epic.progressPercentage || 0} className="h-2" />
        </div>
      </div>
    </div>
  );
}

const getStatusBadgeVariant = (status: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (status) {
    case 'COMPLETED': return 'default';
    case 'IN_PROGRESS': return 'secondary';
    case 'PLANNED': return 'outline';
    case 'ON_HOLD': return 'destructive';
    case 'CANCELLED': return 'destructive';
    default: return 'outline';
  }
};

export default function InitiativeDetailPage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const [initiative, setInitiative] = useState<Initiative | null>(null);
  const [epics, setEpics] = useState<Epic[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialog, setDeleteDialog] = useState(false);
  const [reordering, setReordering] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

  const handleEpicDragEnd = useCallback(async (event: DragEndEvent) => {
    const { active, over } = event;
    if (!over || active.id === over.id) return;

    const oldIndex = epics.findIndex((e) => e.id === active.id);
    const newIndex = epics.findIndex((e) => e.id === over.id);
    const reordered = arrayMove(epics, oldIndex, newIndex);
    setEpics(reordered);

    try {
      setReordering(true);
      const payload: ReorderRequest = {
        items: reordered.map((e, i) => ({ id: e.id, sortOrder: i })),
      };
      await epicService.reorder(payload);
    } catch (err) {
      console.error('Failed to reorder epics', err);
      setEpics(epics); // rollback
    } finally {
      setReordering(false);
    }
  }, [epics]);

  useEffect(() => {
    if (id) {
      loadInitiative();
    }
  }, [id]);

  const loadInitiative = async () => {
    try {
      setLoading(true);
      const response = await initiativeService.getById(Number(id));
      setInitiative(response.data);
      
      // Load epics for this initiative
      const epicsResponse = await epicService.getByInitiative(Number(id));
      setEpics(epicsResponse.data);
    } catch (error) {
      console.error('Failed to load initiative:', error);
      showError(t('initiatives.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (status: InitiativeStatus) => {
    if (!initiative) return;
    try {
      const response = await initiativeService.updateStatus(initiative.id, status);
      setInitiative(response.data);
      showSuccess(t('initiatives.statusUpdated'));
    } catch (error) {
      showError(t('initiatives.updateError'));
      console.error('Failed to update status:', error);
    }
  };

  const handleDelete = async () => {
    if (!initiative) return;
    try {
      await initiativeService.delete(initiative.id);
      showSuccess(t('initiatives.deleted'));
      navigate('/initiatives');
    } catch (error) {
      showError(t('initiatives.deleteError'));
      console.error('Failed to delete initiative:', error);
    }
  };

  if (loading) {
    return (
      <div className="container mx-auto py-8 space-y-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-48" />
        <Skeleton className="h-64" />
      </div>
    );
  }

  if (!initiative) {
    return (
      <div className="container mx-auto py-8">
        <Card>
          <CardContent className="py-12 text-center">
            <Target className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium">{t('initiatives.notFound')}</h3>
            <Button variant="outline" onClick={() => navigate('/initiatives')} className="mt-4">
              <ArrowLeft className="h-4 w-4 mr-2" />
              {t('initiatives.backToList')}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Back button */}
      <Button variant="ghost" onClick={() => navigate('/initiatives')}>
        <ArrowLeft className="h-4 w-4 mr-2" />
        {t('initiatives.backToList')}
      </Button>

      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-start gap-4">
          <div
            className="w-12 h-12 rounded-lg flex items-center justify-center"
            style={{ backgroundColor: initiative.color || '#6366f1' }}
          >
            <Target className="h-6 w-6 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">{initiative.name}</h1>
            <p className="text-muted-foreground">{initiative.projectName}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Select value={initiative.status} onValueChange={(v) => handleStatusChange(v as InitiativeStatus)}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="DRAFT">{t('initiatives.status.draft')}</SelectItem>
              <SelectItem value="PLANNED">{t('initiatives.status.planned')}</SelectItem>
              <SelectItem value="IN_PROGRESS">{t('initiatives.status.inProgress')}</SelectItem>
              <SelectItem value="COMPLETED">{t('initiatives.status.completed')}</SelectItem>
              <SelectItem value="ON_HOLD">{t('initiatives.status.onHold')}</SelectItem>
              <SelectItem value="CANCELLED">{t('initiatives.status.cancelled')}</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" onClick={() => navigate(`/initiatives/${initiative.id}/edit`)}>
            <Pencil className="h-4 w-4 mr-2" />
            {t('common.edit')}
          </Button>
          <Button variant="destructive" onClick={() => setDeleteDialog(true)}>
            <Trash2 className="h-4 w-4 mr-2" />
            {t('common.delete')}
          </Button>
        </div>
      </div>

      {/* Overview */}
      <div className="grid gap-6 md:grid-cols-3">
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle>{t('initiatives.overview')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {initiative.description && (
              <div>
                <h4 className="font-medium mb-1">{t('initiatives.description')}</h4>
                <Markdown content={initiative.description} className="text-muted-foreground" />
              </div>
            )}
            
            <Separator />
            
            <div className="grid gap-4 sm:grid-cols-2">
              {/* Timeline */}
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <Calendar className="h-4 w-4" />
                  {t('initiatives.timeline')}
                </h4>
                <div className="text-sm text-muted-foreground">
                  {initiative.targetStartDate ? formatLocalizedDate(initiative.targetStartDate, i18n.language) : t('initiatives.notSet')}
                  {' - '}
                  {initiative.targetEndDate ? formatLocalizedDate(initiative.targetEndDate, i18n.language) : t('initiatives.notSet')}
                </div>
              </div>
              
              {/* Owner */}
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <User className="h-4 w-4" />
                  {t('initiatives.owner')}
                </h4>
                <div className="text-sm text-muted-foreground">
                  {initiative.ownerName || t('initiatives.notAssigned')}
                </div>
              </div>
              
              {/* Created */}
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <Clock className="h-4 w-4" />
                  {t('initiatives.created')}
                </h4>
                <div className="text-sm text-muted-foreground">
                  {formatLocalizedDate(initiative.createdAt, i18n.language)}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Progress Card */}
        <Card>
          <CardHeader>
            <CardTitle>{t('initiatives.progress')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="text-center">
              <div className="text-4xl font-bold">
                {Math.round(initiative.progressPercentage || 0)}%
              </div>
              <p className="text-sm text-muted-foreground">{t('initiatives.complete')}</p>
            </div>
            <Progress value={initiative.progressPercentage || 0} className="h-3" />
            
            <div className="grid grid-cols-2 gap-4 text-center pt-4">
              <div>
                <div className="text-2xl font-bold">{initiative.epicCount || 0}</div>
                <p className="text-xs text-muted-foreground">{t('initiatives.epics')}</p>
              </div>
              <div>
                <div className="text-2xl font-bold text-green-600">{initiative.completedEpicCount || 0}</div>
                <p className="text-xs text-muted-foreground">{t('initiatives.completedEpics')}</p>
              </div>
              <div>
                <div className="text-2xl font-bold">{initiative.totalPitchCount || 0}</div>
                <p className="text-xs text-muted-foreground">{t('initiatives.pitches')}</p>
              </div>
              <div>
                <div className="text-2xl font-bold text-green-600">{initiative.completedPitchCount || 0}</div>
                <p className="text-xs text-muted-foreground">{t('initiatives.completedPitches')}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Epics */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <Layers className="h-5 w-5" />
            {t('initiatives.epics')} ({epics.length})
            {reordering && <span className="text-xs text-muted-foreground font-normal ml-1">{t('common.saving')}</span>}
          </CardTitle>
          <Button onClick={() => navigate(`/epics/new?initiativeId=${initiative.id}`)}>
            <Plus className="h-4 w-4 mr-2" />
            {t('epics.create')}
          </Button>
        </CardHeader>
        <CardContent>
          {epics.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground">
              <Layers className="h-8 w-8 mx-auto mb-2" />
              <p>{t('initiatives.noEpics')}</p>
            </div>
          ) : (
            <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleEpicDragEnd}>
              <SortableContext items={epics.map((e) => e.id)} strategy={verticalListSortingStrategy}>
                <div className="space-y-3">
                  {epics.map((epic) => (
                    <SortableEpicItem
                      key={epic.id}
                      epic={epic}
                      getStatusBadgeVariant={getStatusBadgeVariant}
                      t={t}
                    />
                  ))}
                </div>
              </SortableContext>
            </DndContext>
          )}
        </CardContent>
      </Card>

      {/* Delete Dialog */}
      <Dialog open={deleteDialog} onOpenChange={setDeleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('initiatives.deleteConfirm')}</DialogTitle>
            <DialogDescription>
              {t('initiatives.deleteWarning', { name: initiative.name })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog(false)}>
              {t('common.cancel')}
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              {t('common.delete')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
