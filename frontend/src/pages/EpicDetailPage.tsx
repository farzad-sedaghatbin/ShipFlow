import { useEffect, useState, useCallback } from 'react';
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
  GitMerge,
  X,
  Plus,
} from 'lucide-react';
import { epicService } from '../services/epicService';
import { pitchService } from '../services/pitchService';
import { getEpicDependencies, addEpicDependency, removeEpicDependency } from '../services/epicDependencyService';
import { Epic, EpicStatus, Pitch, BusinessValue, EpicDependency, DependencyType } from '../types';
import { useToast } from '../contexts';
import SortablePitchList from '../components/SortablePitchList';

import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
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
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import MarkdownEditor from '../components/MarkdownEditor';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { PermissionGate } from '../hooks/usePermission';
import AIPitchWriterModal from '../components/AIPitchWriterModal';
import { Sparkles } from 'lucide-react';
import { useProject } from '../contexts';

export default function EpicDetailPage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const { currentProject } = useProject();
  const [epic, setEpic] = useState<Epic | null>(null);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialog, setDeleteDialog] = useState(false);

  // Quick Add Idea dialog state
  const [ideaDialog, setIdeaDialog] = useState(false);
  const [ideaTitle, setIdeaTitle] = useState('');
  const [ideaDescription, setIdeaDescription] = useState('');
  const [addingIdea, setAddingIdea] = useState(false);

  // AI Pitch Writer state
  const [aiWriterOpen, setAiWriterOpen] = useState(false);

  // Epic dependency state
  const [blockingEpics, setBlockingEpics] = useState<EpicDependency[]>([]);
  const [blockedByEpics, setBlockedByEpics] = useState<EpicDependency[]>([]);
  const [depDialog, setDepDialog] = useState(false);
  const [allEpics, setAllEpics] = useState<Epic[]>([]);
  const [depTargetEpicId, setDepTargetEpicId] = useState<string>('');
  const [depType, setDepType] = useState<DependencyType>('BLOCKS');
  const [addingDep, setAddingDep] = useState(false);

  useEffect(() => {
    if (id) {
      loadEpic();
    }
  }, [id]);

  const loadEpic = async () => {
    try {
      setLoading(true);
      const epicId = Number(id);
      const [epicRes, pitchesRes, depsRes] = await Promise.all([
        epicService.getById(epicId),
        pitchService.getByEpicId(epicId),
        getEpicDependencies(epicId),
      ]);
      setEpic(epicRes.data);
      setPitches(pitchesRes.data);
      setBlockingEpics(depsRes.blocking);
      setBlockedByEpics(depsRes.blockedBy);
    } catch (error) {
      console.error('Failed to load epic:', error);
      showError(t('epics.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const openDepDialog = async () => {
    try {
      if (allEpics.length === 0 && epic) {
        const res = await epicService.getByProject(epic.projectId);
        setAllEpics(res.data.filter((e) => e.id !== Number(id)));
      }
    } catch (error) {
      console.error('Failed to load epics for dependency selection:', error);
    }
    setDepDialog(true);
  };

  const handleAddDep = async () => {
    if (!depTargetEpicId) return;
    try {
      setAddingDep(true);
      const dep = await addEpicDependency(Number(id), Number(depTargetEpicId), depType);
      if (depType === 'BLOCKS') {
        setBlockingEpics((prev) => [...prev, dep]);
      } else {
        setBlockedByEpics((prev) => [...prev, dep]);
      }
      showSuccess(t('epicDependencies.addDependency'));
      setDepDialog(false);
      setDepTargetEpicId('');
      setDepType('BLOCKS');
    } catch (error: unknown) {
      const msg = error instanceof Error ? error.message : String(error);
      showError(msg || t('common.error'));
    } finally {
      setAddingDep(false);
    }
  };

  const handleRemoveDep = async (depId: number, isBlocking: boolean) => {
    try {
      await removeEpicDependency(Number(id), depId);
      if (isBlocking) {
        setBlockingEpics((prev) => prev.filter((d) => d.id !== depId));
      } else {
        setBlockedByEpics((prev) => prev.filter((d) => d.id !== depId));
      }
      showSuccess(t('epicDependencies.removeDependency'));
    } catch (error) {
      console.error('Failed to remove dependency:', error);
      showError(t('common.error'));
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

  const handleReorder = useCallback(async (reorderedPitches: Pitch[]) => {
    setPitches(reorderedPitches);
    try {
      await pitchService.reorder({
        items: reorderedPitches.map((p, index) => ({ id: p.id, sortOrder: index })),
      });
    } catch (error) {
      console.error('Failed to save order:', error);
      showError(t('pitches.reorderError'));
      // Reload to get server state
      const response = await pitchService.getByEpicId(Number(id));
      setPitches(response.data);
    }
  }, [id, showError, t]);

  const handlePitchTitleSave = useCallback(async (pitchId: number, newTitle: string) => {
    const pitch = pitches.find(p => p.id === pitchId);
    if (!pitch) return;
    try {
      await pitchService.update(pitchId, {
        title: newTitle,
        description: pitch.description,
        appetiteDays: pitch.appetiteDays,
        cycleId: pitch.cycleId,
        teamId: pitch.teamId,
        epicId: pitch.epicId,
        targetReleaseId: pitch.targetReleaseId,
        status: pitch.status,
        problemStatement: pitch.problemStatement,
        solution: pitch.solution,
        rabbitHoles: pitch.rabbitHoles,
        risks: pitch.risks,
        noGos: pitch.noGos,
        wireframeLinks: pitch.wireframeLinks,
        priority: pitch.priority,
        sortOrder: pitch.sortOrder,
      });
      setPitches(prev => prev.map(p => p.id === pitchId ? { ...p, title: newTitle } : p));
      showSuccess(t('pitchDetailPage.titleUpdated'));
    } catch (error) {
      console.error('Failed to update pitch title:', error);
      showError(t('pitchDetailPage.titleUpdateFailed'));
      throw error;
    }
  }, [pitches, showError, showSuccess, t]);

  const handlePriorityChange = useCallback(async (pitchId: number, priority: BusinessValue) => {
    // Capture current pitch via functional update to avoid stale closure
    let snapshot: Pitch | undefined;
    setPitches(prev => {
      snapshot = prev.find(p => p.id === pitchId);
      if (!snapshot) return prev;
      return prev.map(p => p.id === pitchId ? { ...p, priority } : p);
    });
    const pitch = snapshot;
    if (!pitch) return;
    try {
      await pitchService.update(pitchId, {
        title: pitch.title,
        description: pitch.description,
        appetiteDays: pitch.appetiteDays,
        cycleId: pitch.cycleId,
        teamId: pitch.teamId,
        epicId: pitch.epicId,
        targetReleaseId: pitch.targetReleaseId,
        status: pitch.status,
        problemStatement: pitch.problemStatement,
        solution: pitch.solution,
        rabbitHoles: pitch.rabbitHoles,
        risks: pitch.risks,
        noGos: pitch.noGos,
        wireframeLinks: pitch.wireframeLinks,
        priority,
        sortOrder: pitch.sortOrder,
      });
      showSuccess(t('pitches.priorityUpdated'));
    } catch (error) {
      console.error('Failed to update priority:', error);
      showError(t('pitches.updateError'));
      // Revert optimistic update
      setPitches(prev => prev.map(p => p.id === pitchId ? { ...p, priority: pitch.priority } : p));
    }
  }, [showError, showSuccess, t]);

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
          <PermissionGate resource="EPIC" permission="UPDATE">
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
          </PermissionGate>
          <PermissionGate resource="EPIC" permission="DELETE">
            <Button variant="destructive" onClick={() => setDeleteDialog(true)}>
              <Trash2 className="h-4 w-4 mr-2" />
              {t('common.delete')}
            </Button>
          </PermissionGate>
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
                <Markdown content={epic.description} className="text-muted-foreground" />
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

      {/* Epic Dependencies */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <GitMerge className="h-5 w-5" />
            {t('epicDependencies.title')}
          </CardTitle>
          <Button variant="outline" size="sm" onClick={openDepDialog}>
            <Plus className="h-4 w-4 mr-2" />
            {t('epicDependencies.addDependency')}
          </Button>
        </CardHeader>
        <CardContent>
          {blockingEpics.length === 0 && blockedByEpics.length === 0 ? (
            <p className="text-sm text-muted-foreground">{t('epicDependencies.noDependencies')}</p>
          ) : (
            <div className="space-y-4">
              {blockingEpics.length > 0 && (
                <div>
                  <h4 className="text-sm font-medium mb-2">{t('epicDependencies.blocks')}</h4>
                  <div className="flex flex-wrap gap-2">
                    {blockingEpics.map((dep) => (
                      <div key={dep.id} className="flex items-center gap-1 px-2 py-1 rounded-md bg-yellow-50 dark:bg-yellow-900/20 border border-yellow-200 dark:border-yellow-700 text-xs text-yellow-800 dark:text-yellow-200">
                        <Link to={`/epics/${dep.targetEpicId}`} className="hover:underline">
                          {dep.targetEpicName}
                        </Link>
                        <button
                          onClick={() => handleRemoveDep(dep.id, true)}
                          className="ml-1 hover:text-destructive"
                          aria-label={t('epicDependencies.removeDependency')}
                          title={t('epicDependencies.removeDependency')}
                        >
                          <X className="h-3 w-3" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}
              {blockedByEpics.length > 0 && (
                <div>
                  <h4 className="text-sm font-medium mb-2">{t('epicDependencies.blockedBy')}</h4>
                  <div className="flex flex-wrap gap-2">
                    {blockedByEpics.map((dep) => {
                      // DEPENDS_ON: source=current epic, target=dependency — show target
                      // BLOCKS: source=blocker, target=current epic — show source
                      const otherEpicId = dep.dependencyType === 'DEPENDS_ON' ? dep.targetEpicId : dep.sourceEpicId;
                      const otherEpicName = dep.dependencyType === 'DEPENDS_ON' ? dep.targetEpicName : dep.sourceEpicName;
                      return (
                        <div key={dep.id} className="flex items-center gap-1 px-2 py-1 rounded-md bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-700 text-xs text-orange-800 dark:text-orange-200">
                          <Link to={`/epics/${otherEpicId}`} className="hover:underline">
                            {otherEpicName}
                          </Link>
                          <button
                            onClick={() => handleRemoveDep(dep.id, false)}
                            className="ml-1 hover:text-destructive"
                            aria-label={t('epicDependencies.removeDependency')}
                            title={t('epicDependencies.removeDependency')}
                          >
                            <X className="h-3 w-3" />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

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
                <SortablePitchList
                  pitches={pitches}
                  onReorder={handleReorder}
                  onUnlink={handleUnlinkPitch}
                  onStartShaping={handleStartShaping}
                  onPriorityChange={handlePriorityChange}
                  onTitleSave={handlePitchTitleSave}
                />
              </TabsContent>

              <TabsContent value="ideas" className="mt-4">
                <SortablePitchList
                  pitches={pitches.filter(p => p.status === 'IDEA')}
                  onReorder={(reordered) => {
                    const others = pitches.filter(p => p.status !== 'IDEA');
                    handleReorder([...reordered, ...others]);
                  }}
                  onUnlink={handleUnlinkPitch}
                  onStartShaping={handleStartShaping}
                  onPriorityChange={handlePriorityChange}
                  onTitleSave={handlePitchTitleSave}
                  emptyMessage={t('pitches.noIdeas')}
                />
              </TabsContent>

              <TabsContent value="shaping" className="mt-4">
                <SortablePitchList
                  pitches={pitches.filter(p => p.status === 'DRAFT')}
                  onReorder={(reordered) => {
                    const others = pitches.filter(p => p.status !== 'DRAFT');
                    handleReorder([...reordered, ...others]);
                  }}
                  onUnlink={handleUnlinkPitch}
                  onStartShaping={handleStartShaping}
                  onPriorityChange={handlePriorityChange}
                  onTitleSave={handlePitchTitleSave}
                  emptyMessage={t('pitches.noDrafts')}
                />
              </TabsContent>

              <TabsContent value="ready" className="mt-4">
                <SortablePitchList
                  pitches={pitches.filter(p => p.status === 'SHAPED')}
                  onReorder={(reordered) => {
                    const others = pitches.filter(p => p.status !== 'SHAPED');
                    handleReorder([...reordered, ...others]);
                  }}
                  onUnlink={handleUnlinkPitch}
                  onStartShaping={handleStartShaping}
                  onPriorityChange={handlePriorityChange}
                  onTitleSave={handlePitchTitleSave}
                  emptyMessage={t('pitches.noShaped')}
                />
              </TabsContent>
            </Tabs>
          )}
        </CardContent>
      </Card>

      {/* Epic Dependency Dialog */}
      <Dialog open={depDialog} onOpenChange={setDepDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <GitMerge className="h-5 w-5" />
              {t('epicDependencies.addDependency')}
            </DialogTitle>
            <DialogDescription>
              {t('epicDependencies.title')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>{t('epicDependencies.dependencyType')}</Label>
              <Select value={depType} onValueChange={(v) => setDepType(v as DependencyType)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="BLOCKS">{t('epicDependencies.blocks')}</SelectItem>
                  <SelectItem value="DEPENDS_ON">{t('epicDependencies.blockedBy')}</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>{t('epicDependencies.selectEpic')}</Label>
              <Select value={depTargetEpicId} onValueChange={setDepTargetEpicId}>
                <SelectTrigger>
                  <SelectValue placeholder={t('epicDependencies.selectEpic')} />
                </SelectTrigger>
                <SelectContent>
                  {allEpics.map((e) => (
                    <SelectItem key={e.id} value={String(e.id)}>
                      {e.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDepDialog(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleAddDep} disabled={addingDep || !depTargetEpicId}>
              {addingDep ? t('common.saving') : t('epicDependencies.addDependency')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

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
              <MarkdownEditor
                id="ideaDescription"
                value={ideaDescription}
                onChange={(value) => setIdeaDescription(value)}
                placeholder={t('pitches.ideaDescriptionPlaceholder')}
                rows={3}
              />
            </div>
          </div>
          <DialogFooter className="flex-col-reverse sm:flex-row sm:justify-between gap-2">
            <Button
              variant="outline"
              className="gap-2 text-primary border-primary/40 hover:bg-primary/5"
              onClick={() => { setIdeaDialog(false); setAiWriterOpen(true); }}
            >
              <Sparkles className="h-4 w-4" />
              {t('aiPitchWriter.writeWithAI')}
            </Button>
            <div className="flex gap-2">
              <Button variant="outline" onClick={() => setIdeaDialog(false)}>
                {t('common.cancel')}
              </Button>
              <Button onClick={handleAddIdea} disabled={addingIdea || !ideaTitle.trim()}>
                {addingIdea ? t('common.saving') : t('pitches.addIdea')}
              </Button>
            </div>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* AI Pitch Writer — pre-fills the idea form on accept */}
      <AIPitchWriterModal
        open={aiWriterOpen}
        onClose={() => setAiWriterOpen(false)}
        projectId={currentProject?.id}
        onAccept={(draft) => {
          setIdeaTitle(draft.title || '');
          // Compose a concise description from the problem statement and solution
          const parts: string[] = [];
          if (draft.problemStatement) parts.push(`**Problem:** ${draft.problemStatement}`);
          if (draft.solution) parts.push(`**Solution:** ${draft.solution}`);
          setIdeaDescription(parts.join('\n\n'));
          setAiWriterOpen(false);
          setIdeaDialog(true);
        }}
      />

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


