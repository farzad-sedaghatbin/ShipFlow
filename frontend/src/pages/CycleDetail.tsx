import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { safeParseId } from '../utils/validation';
import {
  Pencil,
  FileText,
  Users,
  RotateCcw,
  CheckCircle,
  AlertTriangle,
  Lock,
  BarChart3,
  Clock,
  Target,
  Loader2,
  Zap,
} from 'lucide-react';
import { cycleService } from '../services/cycleService';
import { pitchService } from '../services/pitchService';
import { teamService } from '../services/teamService';
import { retroService } from '../services/retroService';
import { Cycle, Pitch, Team, CycleRetroStatus } from '../types';
import StatusChip from '../components/StatusChip';
import ProgressBar from '../components/ProgressBar';
import { QAFloatingButton } from '../components/QAFloatingButton';
import { NotesList } from '../components/NotesList';
import TaskStatisticsCard from '../components/TaskStatisticsCard';
import { useAuth, useToast } from '../contexts';

import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '../components/ui/alert';
import { Separator } from '../components/ui/separator';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';

export default function CycleDetail() {
  const { t, i18n } = useTranslation();
  const { id: idParam } = useParams<{ id: string }>();
  const id = safeParseId(idParam);
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();
  const [cycle, setCycle] = useState<Cycle | null>(null);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [retroStatus, setRetroStatus] = useState<CycleRetroStatus | null>(null);
  const [retroEnabled, setRetroEnabled] = useState(true);
  const [loading, setLoading] = useState(true);
  const [closeCycleDialog, setCloseCycleDialog] = useState(false);

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'PROJECT_MANAGER';

  useEffect(() => {
    const abortController = new AbortController();
    if (id) {
      loadData(id);
    }
    return () => abortController.abort();
  }, [id]);

  const loadData = async (cycleId: number) => {
    try {
      const [cycleRes, pitchesRes, teamsRes] = await Promise.all([
        cycleService.getById(cycleId),
        pitchService.getByCycleId(cycleId),
        teamService.getByCycleId(cycleId),
      ]);
      setCycle(cycleRes.data);
      setPitches(pitchesRes.data);
      setTeams(teamsRes.data);

      // Load retro status if project exists
      if (cycleRes.data.projectId) {
        try {
          const enabledRes = await retroService.isEnabled(cycleRes.data.projectId);
          setRetroEnabled(enabledRes.data.enabled);
          
          if (enabledRes.data.enabled) {
            const statusRes = await retroService.getCycleStatus(cycleId);
            setRetroStatus(statusRes.data);
          }
        } catch (retroError) {
          console.log('Retro status unavailable:', retroError);
        }
      }
    } catch (error: any) {
      if (error.name !== 'CanceledError') {
        console.error('Failed to load cycle:', error);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCloseCycle = async () => {
    if (!cycle) return;
    try {
      await cycleService.closeCycle(cycle.id);
      showSuccess(t('cycleDetailPage.cycleClosed'));
      setCloseCycleDialog(false);
      loadData(cycle.id);
    } catch (error: any) {
      const message = error.response?.data?.message || error.message || t('cycleDetailPage.closeFailed');
      showError(message);
      setCloseCycleDialog(false);
    }
  };

  if (id === null) {
    return (
      <div className="p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertTriangle className="h-4 w-4" />
          <span className="text-sm">{t('cycleDetailPage.invalidCycleId')}</span>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!cycle) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <p className="text-muted-foreground">{t('cycleDetailPage.cycleNotFound')}</p>
        <Button asChild variant="outline">
          <Link to="/cycles">{t('cycleDetailPage.back')}</Link>
        </Button>
      </div>
    );
  }

  const completedPitches = pitches.filter((p) => p.status === 'DONE').length;
  const totalAppetiteHours = pitches.reduce((sum, p) => sum + (p.appetiteHours || 0), 0);
  const totalActualHours = pitches.reduce((sum, p) => sum + (p.totalHoursSpent || 0), 0);

  const getPhaseClasses = (phase: string) => {
    switch (phase) {
      case 'BUILD': return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
      case 'SHAPING': return 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20';
      case 'BETTING': return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
      case 'COOLDOWN': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
      default: return '';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">{cycle.name}</h1>
          <p className="text-sm text-muted-foreground">
            {formatLocalizedDate(new Date(cycle.startDate), i18n.language)} - {formatLocalizedDate(new Date(cycle.endDate), i18n.language)}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline" className={getPhaseClasses(cycle.phase)}>
            {t(`cycles.phase.${cycle.phase.toLowerCase()}`)}
          </Badge>
          <Badge variant={cycle.isActive ? 'default' : 'secondary'} className={cycle.isActive ? 'bg-green-500/10 text-green-400 border-green-500/20' : ''}>
            {cycle.isActive ? t('cycleDetailPage.active') : t('cycleDetailPage.completed')}
          </Badge>
          <Button variant="outline" size="sm" asChild>
            <Link to={`/cycles/${cycle.id}/hill-chart`}>
              <BarChart3 className="h-4 w-4 mr-2" />
              {t('cycleDetailPage.hillChart')}
            </Link>
          </Button>
          <Button variant="outline" size="sm" asChild>
            <Link to={`/cycles/${cycle.id}/circuit-breaker`}>
              <Zap className="h-4 w-4 mr-2" />
              {t('cycleDetailPage.circuitBreaker')}
            </Link>
          </Button>
          <Button variant="outline" size="sm" asChild>
            <Link to={`/cycles/${cycle.id}/edit`}>
              <Pencil className="h-4 w-4 mr-2" />
              {t('cycleDetailPage.edit')}
            </Link>
          </Button>
          {cycle.isActive && isAdmin && (
            <Button
              variant="default"
              size="sm"
              className="bg-amber-500 hover:bg-amber-600 text-white"
              onClick={() => setCloseCycleDialog(true)}
            >
              <Lock className="h-4 w-4 mr-2" />
              {t('cycleDetailPage.closeCycle')}
            </Button>
          )}
        </div>
      </div>

      {/* Retrospective Status Alert */}
      {retroEnabled && retroStatus && cycle.isActive && (
        <Alert className={retroStatus.canCloseCycle ? 'border-green-500/50 bg-green-500/10' : 'border-amber-500/50 bg-amber-500/10'}>
          {retroStatus.canCloseCycle ? (
            <CheckCircle className="h-4 w-4 text-green-400" />
          ) : (
            <AlertTriangle className="h-4 w-4 text-amber-400" />
          )}
          <AlertTitle className="font-semibold">
            {t('cycleDetailPage.retroStatus')}: {retroStatus.closedRetros}/{retroEnabled ? 1 : 0} {t('cycleDetailPage.retroCompleted')}
          </AlertTitle>
          <AlertDescription className="flex items-center justify-between">
            <span>{retroStatus.message}</span>
            {!retroStatus.canCloseCycle && (
              <Button variant="outline" size="sm" asChild>
                <Link to="/retros">
                  <RotateCcw className="h-4 w-4 mr-2" />
                  {t('cycleDetailPage.createRetro')}
                </Link>
              </Button>
            )}
          </AlertDescription>
        </Alert>
      )}

      {/* Stats Grid */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 text-muted-foreground mb-2">
              <FileText className="h-4 w-4" />
              <span className="text-sm">{t('cycleDetailPage.totalPitches')}</span>
            </div>
            <p className="text-3xl font-bold text-foreground">{pitches.length}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 text-muted-foreground mb-2">
              <CheckCircle className="h-4 w-4" />
              <span className="text-sm">{t('cycleDetailPage.completedPitches')}</span>
            </div>
            <p className="text-3xl font-bold text-green-400">{completedPitches}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 text-muted-foreground mb-2">
              <Target className="h-4 w-4" />
              <span className="text-sm">{t('cycleDetailPage.totalAppetite')}</span>
            </div>
            <p className="text-3xl font-bold text-foreground">{totalAppetiteHours.toFixed(0)}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 text-muted-foreground mb-2">
              <Clock className="h-4 w-4" />
              <span className="text-sm">{t('cycleDetailPage.totalActual')}</span>
            </div>
            <p className={`text-3xl font-bold ${totalActualHours > totalAppetiteHours ? 'text-red-400' : 'text-green-400'}`}>
              {totalActualHours.toFixed(0)}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Pitches Section */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <div className="flex items-center gap-2">
                <FileText className="h-5 w-5 text-primary" />
                <CardTitle>{t('cycleDetailPage.pitches')}</CardTitle>
              </div>
              <Button variant="link" asChild className="px-0">
                <Link to="/pitches">{t('common.viewAll')}</Link>
              </Button>
            </CardHeader>
            <CardContent>
              {pitches.length === 0 ? (
                <p className="text-muted-foreground text-center py-8">{t('cycleDetailPage.noPitches')}</p>
              ) : (
                <div className="space-y-3">
                  {pitches.map((pitch) => (
                    <Link
                      key={pitch.id}
                      to={`/pitches/${pitch.id}`}
                      className="block p-4 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <h4 className="font-semibold text-foreground">{pitch.title}</h4>
                        <StatusChip status={pitch.status} />
                      </div>
                      <div className="flex justify-between items-center mb-2 text-sm text-muted-foreground">
                        <span>{pitch.teamName || t('common.unassigned')} • {pitch.appetiteDays} {t('common.days')} {t('cycles.appetite')}</span>
                        <span>{pitch.totalHoursSpent?.toFixed(1) || 0}h / {pitch.appetiteHours?.toFixed(0) || 0}h</span>
                      </div>
                      <ProgressBar
                        value={pitch.progressPercentage || 0}
                        showPercentage={false}
                        color={(pitch.progressPercentage || 0) > 100 ? 'error' : 'primary'}
                      />
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Teams Card */}
          <Card>
            <CardHeader>
              <div className="flex items-center gap-2">
                <Users className="h-5 w-5 text-primary" />
                <CardTitle>{t('cycleDetailPage.teams')}</CardTitle>
              </div>
            </CardHeader>
            <CardContent>
              {teams.length === 0 ? (
                <p className="text-muted-foreground text-center py-4">{t('cycleDetailPage.noTeams')}</p>
              ) : (
                <div className="space-y-1">
                  {teams.map((team, index) => (
                    <div key={team.id}>
                      <div className="flex justify-between items-center py-3">
                        <span className="font-medium text-foreground">{team.name}</span>
                        <span className="text-sm text-muted-foreground">
                          {team.assignments?.length || 0} {t('cycles.members')}
                        </span>
                      </div>
                      {index < teams.length - 1 && <Separator />}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Task Statistics */}
          <TaskStatisticsCard cycleId={cycle.id} />
        </div>

        {/* Notes Section - Full Width */}
        <div className="lg:col-span-3">
          <NotesList 
            contextType="cycle" 
            contextId={cycle.id} 
            title={t('cycleDetailPage.cycleNotes')}
          />
        </div>
      </div>

      {/* Q&A Floating Button */}
      <QAFloatingButton
        contextType="cycle"
        contextId={cycle.id}
        contextName={cycle.name}
        cycleId={cycle.id}
      />

      {/* Close Cycle Dialog */}
      <Dialog open={closeCycleDialog} onOpenChange={setCloseCycleDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('cycleDetailPage.confirmClose')}</DialogTitle>
            <DialogDescription>
              {retroStatus && !retroStatus.canCloseCycle ? (
                <Alert className="mt-4 border-amber-500/50 bg-amber-500/10">
                  <AlertTriangle className="h-4 w-4 text-amber-400" />
                  <AlertDescription>{retroStatus.message}</AlertDescription>
                </Alert>
              ) : (
                <span>
                  {t('cycleDetailPage.confirmCloseDesc')}
                </span>
              )}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCloseCycleDialog(false)}>
              {t('common.cancel')}
            </Button>
            {retroStatus && !retroStatus.canCloseCycle ? (
              <Button asChild>
                <Link to="/retros">
                  <RotateCcw className="h-4 w-4 mr-2" />
                  {t('cycleDetailPage.createRetroFirst')}
                </Link>
              </Button>
            ) : (
              <Button 
                className="bg-amber-500 hover:bg-amber-600 text-white"
                onClick={handleCloseCycle}
              >
                {t('cycleDetailPage.closeCycle')}
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
