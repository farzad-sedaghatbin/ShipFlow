import { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  ArrowLeft,
  Pencil,
  Trash2,
  Layers,
  Calendar,
  Target,
  Clock,
  CheckCircle2,
  Lightbulb,
  FileEdit,
  Vote,
} from 'lucide-react';
import { epicService } from '../services/epicService';
import { pitchService } from '../services/pitchService';
import { Epic, EpicStatus, Pitch, PitchStatusUtils } from '../types';
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
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';

const getPitchStatusVariant = (status: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (status) {
    case 'DONE': return 'default';
    case 'IN_PROGRESS': return 'secondary';
    case 'TESTING': return 'secondary';
    case 'STARTED': return 'secondary';
    case 'PENDING': return 'outline';
    case 'SHAPED': return 'default';
    case 'DRAFT': return 'secondary';
    case 'IDEA': return 'outline';
    case 'COOLDOWN': return 'outline';
    case 'CANCELLED': return 'destructive';
    default: return 'outline';
  }
};

export default function EpicDetailPage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const [epic, setEpic] = useState<Epic | null>(null);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialog, setDeleteDialog] = useState(false);
  
  // Quick Add Idea dialog state
  const [ideaDialog, setIdeaDialog] = useState(false);
  const [ideaTitle, setIdeaTitle] = useState('');
  const [ideaDescription, setIdeaDescription] = useState('');
  const [addingIdea, setAddingIdea] = useState(false);

  useEffect(() => {
    if (id) {
      loadEpic();
    }
  }, [id]);

  const loadEpic = async () => {
    try {
      setLoading(true);
      const response = await epicService.getById(Number(id));
      setEpic(response.data);
      
      // Load pitches for this epic
      const pitchesResponse = await pitchService.getByEpicId(Number(id));
      setPitches(pitchesResponse.data);
    } catch (error) {
      console.error('Failed to load epic:', error);
      showError(t('epics.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleAddIdea = async () => {
    if (!ideaTitle.trim()) {
      showError(t('pitches.titleRequired'));
      return;
    }
    
    try {
      setAddingIdea(true);
      const response = await pitchService.createIdea({
        title: ideaTitle.trim(),
        description: ideaDescription.trim() || undefined,
        epicId: Number(id),
      });
      setPitches([response.data, ...pitches]);
      showSuccess(t('pitches.ideaCreated'));
      setIdeaDialog(false);
      setIdeaTitle('');
      setIdeaDescription('');
    } catch (error) {
      console.error('Failed to create idea:', error);
      showError(t('pitches.createError'));
    } finally {
      setAddingIdea(false);
    }
  };

  const handleStartShaping = async (pitchId: number) => {
    try {
      const response = await pitchService.startShaping(pitchId);
      setPitches(pitches.map(p => p.id === pitchId ? response.data : p));
      showSuccess(t('pitches.shapingStarted'));
    } catch (error) {
      console.error('Failed to start shaping:', error);
      showError(t('pitches.updateError'));
    }
  };

  const handleStatusChange = async (status: EpicStatus) => {
    if (!epic) return;
    try {
      const response = await epicService.updateStatus(epic.id, status);
      setEpic(response.data);
      showSuccess(t('epics.statusUpdated'));
    } catch (error) {
      showError(t('epics.updateError'));
      console.error('Failed to update status:', error);
    }
  };

  const handleDelete = async () => {
    if (!epic) return;
    try {
      await epicService.delete(epic.id);
      showSuccess(t('epics.deleted'));
      navigate('/epics');
    } catch (error) {
      showError(t('epics.deleteError'));
      console.error('Failed to delete epic:', error);
    }
  };

  const handleUnlinkPitch = async (pitchId: number) => {
    try {
      await pitchService.unlinkFromEpic(pitchId);
      setPitches(pitches.filter(p => p.id !== pitchId));
      showSuccess(t('epics.pitchUnlinked'));
    } catch (error) {
      showError(t('epics.unlinkError'));
      console.error('Failed to unlink pitch:', error);
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

  if (!epic) {
    return (
      <div className="container mx-auto py-8">
        <Card>
          <CardContent className="py-12 text-center">
            <Layers className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium">{t('epics.notFound')}</h3>
            <Button variant="outline" onClick={() => navigate('/epics')} className="mt-4">
              <ArrowLeft className="h-4 w-4 mr-2" />
              {t('epics.backToList')}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const completedPitches = pitches.filter(p => p.status === 'DONE').length;

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Back button */}
      <Button variant="ghost" onClick={() => navigate('/epics')}>
        <ArrowLeft className="h-4 w-4 mr-2" />
        {t('epics.backToList')}
      </Button>

      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-start gap-4">
          <div
            className="w-12 h-12 rounded-lg flex items-center justify-center"
            style={{ backgroundColor: epic.color || '#8b5cf6' }}
          >
            <Layers className="h-6 w-6 text-white" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">{epic.name}</h1>
            <div className="flex items-center gap-2 text-muted-foreground">
              <span>{epic.projectName}</span>
              {epic.initiativeName && (
                <>
                  <span>•</span>
                  <Link to={`/initiatives/${epic.initiativeId}`} className="hover:underline">
                    {epic.initiativeName}
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Select value={epic.status} onValueChange={(v) => handleStatusChange(v as EpicStatus)}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="DRAFT">{t('epics.status.draft')}</SelectItem>
              <SelectItem value="PLANNED">{t('epics.status.planned')}</SelectItem>
              <SelectItem value="IN_PROGRESS">{t('epics.status.in_progress')}</SelectItem>
              <SelectItem value="COMPLETED">{t('epics.status.completed')}</SelectItem>
              <SelectItem value="ON_HOLD">{t('epics.status.on_hold')}</SelectItem>
              <SelectItem value="CANCELLED">{t('epics.status.cancelled')}</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" onClick={() => navigate(`/epics/${epic.id}/edit`)}>
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
            <CardTitle>{t('epics.overview')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {epic.description && (
              <div>
                <h4 className="font-medium mb-1">{t('epics.description')}</h4>
                <p className="text-muted-foreground">{epic.description}</p>
              </div>
            )}
            
            <Separator />
            
            <div className="grid gap-4 sm:grid-cols-2">
              {/* Initiative */}
              {epic.initiativeId && (
                <div>
                  <h4 className="font-medium mb-2 flex items-center gap-2">
                    <Target className="h-4 w-4" />
                    {t('epics.initiative')}
                  </h4>
                  <Link 
                    to={`/initiatives/${epic.initiativeId}`}
                    className="text-sm text-primary hover:underline"
                  >
                    {epic.initiativeName}
                  </Link>
                </div>
              )}
              
              {/* Timeline */}
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <Calendar className="h-4 w-4" />
                  {t('epics.timeline')}
                </h4>
                <div className="text-sm text-muted-foreground">
                  {epic.targetStartDate ? formatLocalizedDate(epic.targetStartDate, i18n.language) : t('epics.notSet')}
                  {' - '}
                  {epic.targetEndDate ? formatLocalizedDate(epic.targetEndDate, i18n.language) : t('epics.notSet')}
                </div>
              </div>
              
              {/* Created */}
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <Clock className="h-4 w-4" />
                  {t('epics.created')}
                </h4>
                <div className="text-sm text-muted-foreground">
                  {formatLocalizedDate(epic.createdAt, i18n.language)}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Progress Card */}
        <Card>
          <CardHeader>
            <CardTitle>{t('epics.progress')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="text-center">
              <div className="text-4xl font-bold">
                {Math.round(epic.progressPercentage || 0)}%
              </div>
              <p className="text-sm text-muted-foreground">{t('epics.complete')}</p>
            </div>
            <Progress value={epic.progressPercentage || 0} className="h-3" />
            
            <div className="grid grid-cols-2 gap-4 text-center pt-4">
              <div>
                <div className="text-2xl font-bold">{pitches.length}</div>
                <p className="text-xs text-muted-foreground">{t('epics.pitches')}</p>
              </div>
              <div>
                <div className="text-2xl font-bold text-green-600">{completedPitches}</div>
                <p className="text-xs text-muted-foreground">{t('epics.completedPitches')}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Pitches */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5" />
            {t('epics.pitches')} ({pitches.length})
          </CardTitle>
          <Button variant="outline" onClick={() => setIdeaDialog(true)}>
            <Lightbulb className="h-4 w-4 mr-2" />
            {t('pitches.quickAddIdea')}
          </Button>
        </CardHeader>
        <CardContent>
          {pitches.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground">
              <Lightbulb className="h-8 w-8 mx-auto mb-2" />
              <p>{t('epics.noPitches')}</p>
              <p className="text-sm mt-2">{t('pitches.addIdeaHint')}</p>
            </div>
          ) : (
            <Tabs defaultValue="all" className="w-full">
              <TabsList className="grid w-full grid-cols-4">
                <TabsTrigger value="all">{t('pitches.all')} ({pitches.length})</TabsTrigger>
                <TabsTrigger value="ideas">
                  <Lightbulb className="h-3 w-3 mr-1" />
                  {t('pitches.ideas')} ({pitches.filter(p => p.status === 'IDEA').length})
                </TabsTrigger>
                <TabsTrigger value="shaping">
                  <FileEdit className="h-3 w-3 mr-1" />
                  {t('pitches.shaping')} ({pitches.filter(p => p.status === 'DRAFT').length})
                </TabsTrigger>
                <TabsTrigger value="ready">
                  <Vote className="h-3 w-3 mr-1" />
                  {t('pitches.readyForBetting')} ({pitches.filter(p => p.status === 'SHAPED').length})
                </TabsTrigger>
              </TabsList>
              
              <TabsContent value="all" className="mt-4">
                <PitchList 
                  pitches={pitches} 
                  t={t}
                  onUnlink={handleUnlinkPitch}
                  onStartShaping={handleStartShaping}
                />
              </TabsContent>
              
              <TabsContent value="ideas" className="mt-4">
                <PitchList 
                  pitches={pitches.filter(p => p.status === 'IDEA')} 
                  t={t}
                  onUnlink={handleUnlinkPitch}
                  onStartShaping={handleStartShaping}
                  emptyMessage={t('pitches.noIdeas')}
                />
              </TabsContent>
              
              <TabsContent value="shaping" className="mt-4">
                <PitchList 
                  pitches={pitches.filter(p => p.status === 'DRAFT')} 
                  t={t}
                  onUnlink={handleUnlinkPitch}
                  onStartShaping={handleStartShaping}
                  emptyMessage={t('pitches.noDrafts')}
                />
              </TabsContent>
              
              <TabsContent value="ready" className="mt-4">
                <PitchList 
                  pitches={pitches.filter(p => p.status === 'SHAPED')} 
                  t={t}
                  onUnlink={handleUnlinkPitch}
                  onStartShaping={handleStartShaping}
                  emptyMessage={t('pitches.noShaped')}
                />
              </TabsContent>
            </Tabs>
          )}
        </CardContent>
      </Card>

      {/* Quick Add Idea Dialog */}
      <Dialog open={ideaDialog} onOpenChange={setIdeaDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Lightbulb className="h-5 w-5" />
              {t('pitches.quickAddIdea')}
            </DialogTitle>
            <DialogDescription>
              {t('pitches.quickAddIdeaDescription')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="ideaTitle">{t('pitches.title')}</Label>
              <Input
                id="ideaTitle"
                value={ideaTitle}
                onChange={(e) => setIdeaTitle(e.target.value)}
                placeholder={t('pitches.titlePlaceholder')}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="ideaDescription">{t('pitches.description')}</Label>
              <Textarea
                id="ideaDescription"
                value={ideaDescription}
                onChange={(e) => setIdeaDescription(e.target.value)}
                placeholder={t('pitches.ideaDescriptionPlaceholder')}
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setIdeaDialog(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleAddIdea} disabled={addingIdea || !ideaTitle.trim()}>
              {addingIdea ? t('common.saving') : t('pitches.addIdea')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Dialog */}
      <Dialog open={deleteDialog} onOpenChange={setDeleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('epics.deleteConfirm')}</DialogTitle>
            <DialogDescription>
              {t('epics.deleteWarning', { name: epic.name })}
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

// Helper component for pitch list
interface PitchListProps {
  pitches: Pitch[];
  t: (key: string) => string;
  onUnlink: (id: number) => void;
  onStartShaping: (id: number) => void;
  emptyMessage?: string;
}

function PitchList({ pitches, t, onUnlink, onStartShaping, emptyMessage }: PitchListProps) {
  if (pitches.length === 0) {
    return (
      <div className="py-8 text-center text-muted-foreground">
        <Lightbulb className="h-8 w-8 mx-auto mb-2" />
        <p>{emptyMessage || t('epics.noPitches')}</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {pitches.map((pitch) => (
        <div
          key={pitch.id}
          className="flex items-center justify-between p-4 rounded-lg border hover:bg-muted/50"
        >
          <div className="flex-1">
            <Link
              to={`/pitches/${pitch.id}`}
              className="font-medium hover:underline"
            >
              {pitch.title}
            </Link>
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
          <div className="flex items-center gap-4">
            <Badge variant={getPitchStatusVariant(pitch.status)}>
              {t(`pitches.status.${pitch.status.toLowerCase()}`)}
            </Badge>
            {/* Show start shaping button for IDEAS */}
            {pitch.status === 'IDEA' && (
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
            {/* Show progress for in-cycle pitches */}
            {pitch.cycleId && (
              <div className="w-24">
                <Progress value={pitch.progressPercentage || 0} className="h-2" />
              </div>
            )}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onUnlink(pitch.id)}
              title={t('epics.unlinkPitch')}
            >
              <Trash2 className="h-4 w-4 text-muted-foreground" />
            </Button>
          </div>
        </div>
      ))}
    </div>
  );
}
