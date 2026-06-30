import { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  ArrowLeft,
  Pencil,
  Trash2,
  Package,
  Calendar,
  Clock,
  CheckCircle2,
  Rocket,
  AlertTriangle,
  Bug,
  ListTodo,
  ShieldAlert,
} from 'lucide-react';
import { releaseService } from '../services/releaseService';
import { pitchService } from '../services/pitchService';
import { Release, ReleaseStatus, Pitch, ReleaseProgress } from '../types';
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

const getRiskBadgeVariant = (risk: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (risk) {
    case 'LOW': return 'outline';
    case 'MEDIUM': return 'secondary';
    case 'HIGH': return 'destructive';
    case 'CRITICAL': return 'destructive';
    default: return 'outline';
  }
};

const getPitchStatusVariant = (status: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (status) {
    case 'DONE': return 'default';
    case 'IN_PROGRESS': return 'secondary';
    case 'TESTING': return 'secondary';
    case 'STARTED': return 'secondary';
    case 'PENDING': return 'outline';
    case 'SHAPED': return 'outline';
    case 'COOLDOWN': return 'outline';
    case 'CANCELLED': return 'destructive';
    default: return 'outline';
  }
};

export default function ReleaseDetailPage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const [release, setRelease] = useState<Release | null>(null);
  const [progress, setProgress] = useState<ReleaseProgress | null>(null);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialog, setDeleteDialog] = useState(false);

  useEffect(() => {
    if (id) {
      loadRelease();
    }
  }, [id]);

  const loadRelease = async () => {
    try {
      setLoading(true);
      const [releaseResponse, progressResponse, pitchesResponse] = await Promise.all([
        releaseService.getById(Number(id)),
        releaseService.getProgress(Number(id)),
        pitchService.getByReleaseId(Number(id)),
      ]);
      setRelease(releaseResponse.data);
      setProgress(progressResponse.data);
      setPitches(pitchesResponse.data);
    } catch (error) {
      console.error('Failed to load release:', error);
      showError(t('releases.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = async (status: ReleaseStatus) => {
    if (!release) return;
    try {
      const response = await releaseService.updateStatus(release.id, status);
      setRelease(response.data);
      showSuccess(t('releases.statusUpdated'));
    } catch (error) {
      showError(t('releases.updateError'));
      console.error('Failed to update status:', error);
    }
  };

  const handleDelete = async () => {
    if (!release) return;
    try {
      await releaseService.delete(release.id);
      showSuccess(t('releases.deleted'));
      navigate('/releases-management');
    } catch (error) {
      showError(t('releases.deleteError'));
      console.error('Failed to delete release:', error);
    }
  };

  const handleUnlinkPitch = async (pitchId: number) => {
    try {
      await pitchService.clearTargetRelease(pitchId);
      setPitches(pitches.filter(p => p.id !== pitchId));
      showSuccess(t('releases.pitchUnlinked'));
    } catch (error) {
      showError(t('releases.unlinkError'));
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

  if (!release) {
    return (
      <div className="container mx-auto py-8">
        <Card>
          <CardContent className="py-12 text-center">
            <Package className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium">{t('releases.notFound')}</h3>
            <Button variant="outline" onClick={() => navigate('/releases-management')} className="mt-4">
              <ArrowLeft className="h-4 w-4 mr-2" />
              {t('releases.backToList')}
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  const completedPitches = pitches.filter(p => p.status === 'DONE').length;
  const progressPercent = pitches.length > 0 ? (completedPitches / pitches.length) * 100 : 0;

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Back button */}
      <Button variant="ghost" onClick={() => navigate('/releases-management')}>
        <ArrowLeft className="h-4 w-4 mr-2" />
        {t('releases.backToList')}
      </Button>

      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-lg bg-indigo-600 flex items-center justify-center">
            <Package className="h-6 w-6 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-bold">{release.name}</h1>
              <Badge variant="outline">{release.version}</Badge>
            </div>
            <p className="text-muted-foreground">{release.projectName}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Badge variant={getRiskBadgeVariant(release.riskLevel)}>
            <AlertTriangle className="h-3 w-3 mr-1" />
            {t(`releases.risk.${release.riskLevel.toLowerCase()}`)}
          </Badge>
          <Select value={release.status} onValueChange={(v) => handleStatusChange(v as ReleaseStatus)}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="PLANNING">{t('releases.status.planning')}</SelectItem>
              <SelectItem value="IN_PROGRESS">{t('releases.status.in_progress')}</SelectItem>
              <SelectItem value="STAGING">{t('releases.status.staging')}</SelectItem>
              <SelectItem value="RELEASED">{t('releases.status.released')}</SelectItem>
              <SelectItem value="CANCELLED">{t('releases.status.cancelled')}</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" onClick={() => navigate(`/releases-management/${release.id}/edit`)}>
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
            <CardTitle>{t('releases.overview')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {release.description && (
              <div>
                <h4 className="font-medium mb-1">{t('releases.descriptionLabel')}</h4>
                <p className="text-muted-foreground">{release.description}</p>
              </div>
            )}
            
            <Separator />
            
            <div className="grid gap-4 sm:grid-cols-2">
              {/* Target Date */}
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <Calendar className="h-4 w-4" />
                  {t('releases.targetDate')}
                </h4>
                <div className="text-sm text-muted-foreground">
                  {release.targetDate ? formatLocalizedDate(release.targetDate, i18n.language) : t('releases.notSet')}
                </div>
              </div>
              
              {/* Release Date */}
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <Rocket className="h-4 w-4" />
                  {t('releases.releaseDate')}
                </h4>
                <div className="text-sm text-muted-foreground">
                  {release.releaseDate ? formatLocalizedDate(release.releaseDate, i18n.language) : t('releases.notYetReleased')}
                </div>
              </div>
              
              {/* Created */}
              <div>
                <h4 className="font-medium mb-2 flex items-center gap-2">
                  <Clock className="h-4 w-4" />
                  {t('releases.created')}
                </h4>
                <div className="text-sm text-muted-foreground">
                  {formatLocalizedDate(release.createdAt, i18n.language)}
                </div>
              </div>
              
              {/* Linked Cycles */}
              {release.cycles && release.cycles.length > 0 && (
                <div>
                  <h4 className="font-medium mb-2">{t('releases.linkedCycles')}</h4>
                  <div className="flex flex-wrap gap-1">
                    {release.cycles.map(cycle => (
                      <Badge key={cycle.id} variant="outline" className="text-xs">
                        {cycle.name}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}
            </div>
            
            {/* Release Notes */}
            {release.releaseNotes && (
              <>
                <Separator />
                <div>
                  <h4 className="font-medium mb-2">{t('releases.releaseNotes')}</h4>
                  <p className="text-sm text-muted-foreground whitespace-pre-wrap">{release.releaseNotes}</p>
                </div>
              </>
            )}
          </CardContent>
        </Card>

        {/* Progress Card */}
        <Card>
          <CardHeader>
            <CardTitle>{t('releases.progress')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="text-center">
              <div className="text-4xl font-bold">
                {Math.round(progressPercent)}%
              </div>
              <p className="text-sm text-muted-foreground">{t('releases.complete')}</p>
            </div>
            <Progress value={progressPercent} className="h-3" />
            
            <div className="grid grid-cols-2 gap-4 text-center pt-4">
              <div>
                <div className="text-2xl font-bold">{pitches.length}</div>
                <p className="text-xs text-muted-foreground">{t('releases.pitches')}</p>
              </div>
              <div>
                <div className="text-2xl font-bold text-green-600">{completedPitches}</div>
                <p className="text-xs text-muted-foreground">{t('releases.completed')}</p>
              </div>
            </div>
            
            {progress && (
              <>
                {/* Tasks breakdown */}
                <div className="border-t pt-4">
                  <h4 className="text-sm font-medium flex items-center gap-2 mb-3">
                    <ListTodo className="h-4 w-4" />
                    {t('releases.taskBreakdown', 'Tasks')}
                  </h4>
                  <div className="grid grid-cols-2 gap-3 text-center">
                    <div>
                      <div className="text-lg font-bold">{progress.totalTasks || 0}</div>
                      <p className="text-xs text-muted-foreground">{t('releases.total', 'Total')}</p>
                    </div>
                    <div>
                      <div className="text-lg font-bold text-green-600">{progress.completedTasks || 0}</div>
                      <p className="text-xs text-muted-foreground">{t('releases.completed')}</p>
                    </div>
                    <div>
                      <div className="text-lg font-bold text-blue-600">{progress.inProgressTasks || 0}</div>
                      <p className="text-xs text-muted-foreground">{t('releases.inProgress', 'In Progress')}</p>
                    </div>
                    <div>
                      <div className="text-lg font-bold text-red-600">{progress.blockedTasks || 0}</div>
                      <p className="text-xs text-muted-foreground">{t('releases.blocked', 'Blocked')}</p>
                    </div>
                  </div>
                  {(progress.totalTasks || 0) > 0 && (
                    <Progress value={progress.taskCompletionPercentage || 0} className="h-2 mt-2" />
                  )}
                </div>

                {/* Bugs breakdown */}
                <div className="border-t pt-4">
                  <h4 className="text-sm font-medium flex items-center gap-2 mb-3">
                    <Bug className="h-4 w-4" />
                    {t('releases.bugBreakdown', 'Bugs')}
                  </h4>
                  <div className="grid grid-cols-2 gap-3 text-center">
                    <div>
                      <div className="text-lg font-bold">{progress.totalBugs || 0}</div>
                      <p className="text-xs text-muted-foreground">{t('releases.total', 'Total')}</p>
                    </div>
                    <div>
                      <div className="text-lg font-bold text-orange-600">{progress.openBugs || 0}</div>
                      <p className="text-xs text-muted-foreground">{t('releases.open', 'Open')}</p>
                    </div>
                    <div>
                      <div className="text-lg font-bold text-green-600">{progress.resolvedBugs || 0}</div>
                      <p className="text-xs text-muted-foreground">{t('releases.resolved', 'Resolved')}</p>
                    </div>
                    <div>
                      <div className="text-lg font-bold text-red-600">{progress.slippedBugs || 0}</div>
                      <p className="text-xs text-muted-foreground">{t('releases.slipped', 'Slipped')}</p>
                    </div>
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Slipped Bugs Warning */}
      {progress && progress.slippedBugList && progress.slippedBugList.length > 0 && (
        <Card className="border-red-200 bg-red-50 dark:bg-red-950/20 dark:border-red-800">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="flex items-center gap-2 text-red-700 dark:text-red-400">
              <ShieldAlert className="h-5 w-5" />
              {t('releases.slippedBugs', 'Slipped Bugs')} ({progress.slippedBugList.length})
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground mb-3">
              {t('releases.slippedBugsDescription', 'These bugs were targeted for this release but have slipped.')}
            </p>
            <div className="space-y-2">
              {progress.slippedBugList.map((bug) => (
                <div
                  key={bug.id}
                  className="flex items-center justify-between p-3 rounded-lg border bg-background"
                >
                  <div className="flex items-center gap-3">
                    <Bug className="h-4 w-4 text-destructive" />
                    <span className="font-medium">{bug.title}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={bug.severity === 'CRITICAL' || bug.severity === 'BLOCKER' ? 'destructive' : 'secondary'}>
                      {bug.severity}
                    </Badge>
                    <Badge variant="outline">{bug.status.replace('_', ' ')}</Badge>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Targeted Pitches */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <CheckCircle2 className="h-5 w-5" />
            {t('releases.targetedPitches')} ({pitches.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          {pitches.length === 0 ? (
            <div className="py-8 text-center text-muted-foreground">
              <CheckCircle2 className="h-8 w-8 mx-auto mb-2" />
              <p>{t('releases.noPitches')}</p>
            </div>
          ) : (
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
                      <span>{pitch.cycleName}</span>
                      {pitch.teamName && (
                        <>
                          <span>•</span>
                          <span>{pitch.teamName}</span>
                        </>
                      )}
                      {pitch.epicName && (
                        <>
                          <span>•</span>
                          <Link to={`/epics/${pitch.epicId}`} className="hover:underline">
                            {pitch.epicName}
                          </Link>
                        </>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <Badge variant={getPitchStatusVariant(pitch.status)}>
                      {t(`pitches.status.${pitch.status.toLowerCase()}`)}
                    </Badge>
                    <div className="w-24">
                      <Progress value={pitch.progressPercentage || 0} className="h-2" />
                    </div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleUnlinkPitch(pitch.id)}
                      title={t('releases.unlinkPitch')}
                    >
                      <Trash2 className="h-4 w-4 text-muted-foreground" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Delete Dialog */}
      <Dialog open={deleteDialog} onOpenChange={setDeleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('releases.deleteConfirm')}</DialogTitle>
            <DialogDescription>
              {t('releases.deleteWarning', { name: release.name })}
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
