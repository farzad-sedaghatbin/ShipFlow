import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  Pencil,
  Archive,
  ArchiveRestore,
  Trash2,
  Plus,
  TrendingUp,
  Users,
  Calendar,
  Loader2,
  Play,
  Pause,
  CheckCircle2,
  ListTodo,
  Kanban,
} from 'lucide-react';
import { Project, Cycle, Team } from '../types';
import projectService from '../services/projectService';
import { cycleService } from '../services/cycleService';
import { teamService } from '../services/teamService';
import { useToast, useProject } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';
import { formatDate } from '../utils/dateUtils';

import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';

export default function ProjectDetail() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { refreshProjects } = useProject();
  
  const [project, setProject] = useState<Project | null>(null);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialog, setDeleteDialog] = useState(false);

  const loadData = useCallback(async () => {
    if (!id) return;
    
    try {
      setLoading(true);
      const projectId = parseInt(id);
      
      const [projectRes, cyclesRes, teamsRes] = await Promise.all([
        projectService.getById(projectId),
        cycleService.getByProject(projectId),
        teamService.getAll(),
      ]);
      
      setProject(projectRes);
      setCycles(cyclesRes.data || []);
      const allTeams = Array.isArray(teamsRes) ? teamsRes : teamsRes.data || [];
      setTeams(allTeams);
    } catch (error) {
      showToast(getUserFriendlyError(error, 'Failed to load project details'), 'error');
      navigate('/projects');
    } finally {
      setLoading(false);
    }
  }, [id, showToast, navigate]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleEdit = () => {
    navigate(`/projects?edit=${id}`);
  };

  const handleToggleArchive = async () => {
    if (!project) return;
    
    try {
      if (project.isActive) {
        await projectService.deactivate(project.id);
        showToast(t('projectDetail.projectArchived'), 'success');
      } else {
        await projectService.activate(project.id);
        showToast(t('projectDetail.projectActivated'), 'success');
      }
      loadData();
      refreshProjects();
    } catch (error) {
      showToast(getUserFriendlyError(error, t('projectDetail.updateStatusFailed')), 'error');
    }
  };

  const handleDelete = async () => {
    if (!project) return;
    
    try {
      await projectService.delete(project.id);
      showToast(t('projectDetail.projectDeleted'), 'success');
      refreshProjects();
      navigate('/projects');
    } catch (error) {
      showToast(getUserFriendlyError(error, t('projectDetail.deleteFailed')), 'error');
    }
  };

  const getCycleStatusBadge = (cycle: Cycle) => {
    const now = new Date();
    const startDate = new Date(cycle.startDate);
    const endDate = new Date(cycle.endDate);
    
    if (now >= startDate && now <= endDate) {
      return (
        <Badge className="bg-green-500" aria-label={t('projectDetail.activeCycle')}>
          <Play className="h-3 w-3 mr-1" aria-hidden="true" />
          {t('projectDetail.active')}
        </Badge>
      );
    } else if (now > endDate) {
      return (
        <Badge variant="secondary" aria-label={t('projectDetail.completedCycle')}>
          <CheckCircle2 className="h-3 w-3 mr-1" aria-hidden="true" />
          {t('projectDetail.completed')}
        </Badge>
      );
    } else {
      return (
        <Badge variant="outline" aria-label={t('projectDetail.upcomingCycle')}>
          <Pause className="h-3 w-3 mr-1" aria-hidden="true" />
          {t('projectDetail.upcoming')}
        </Badge>
      );
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!project) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">{t('projectDetail.notFound')}</p>
        <Button onClick={() => navigate('/projects')} className="mt-4">
          {t('projectDetail.goToProjects')}
        </Button>
      </div>
    );
  }

  const now = new Date();
  const activeCycles = cycles.filter(c => {
    const startDate = new Date(c.startDate);
    const endDate = new Date(c.endDate);
    return now >= startDate && now <= endDate;
  });
  const completedCycles = cycles.filter(c => {
    const endDate = new Date(c.endDate);
    return now > endDate;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => navigate('/projects')}
          aria-label={t('projectDetail.backToProjects')}
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div className="flex-1">
          <div className="flex items-center gap-4 mb-2">
            <Avatar
              className="h-16 w-16"
              style={{ backgroundColor: project.color || '#4A90D9' }}
            >
              <AvatarFallback className="text-white font-semibold text-2xl">
                {project.projectKey.substring(0, 2)}
              </AvatarFallback>
            </Avatar>
            <div>
              <h1 className="text-3xl font-bold">{project.name}</h1>
              <div className="flex gap-2 mt-1">
                <Badge variant="secondary" className="text-sm">
                  {project.projectKey}
                </Badge>
                {!project.isActive && (
                  <Badge variant="secondary">{t('projectDetail.archived')}</Badge>
                )}
              </div>
            </div>
          </div>
        </div>
        
        {/* Action Buttons */}
        <div className="flex gap-2">
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button variant="outline" size="icon" onClick={handleEdit}>
                  <Pencil className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('projectDetail.editProject')}</TooltipContent>
            </Tooltip>
            
            <Tooltip>
              <TooltipTrigger asChild>
                <Button variant="outline" size="icon" onClick={handleToggleArchive}>
                  {project.isActive ? (
                    <Archive className="h-4 w-4" />
                  ) : (
                    <ArchiveRestore className="h-4 w-4" />
                  )}
                </Button>
              </TooltipTrigger>
              <TooltipContent>
                {project.isActive ? t('projectDetail.archive') : t('projectDetail.activate')}
              </TooltipContent>
            </Tooltip>
            
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="outline"
                  size="icon"
                  onClick={() => setDeleteDialog(true)}
                  disabled={project.projectType === 'SHAPE_UP' && (project.cycleCount || 0) > 0}
                  className="text-destructive hover:text-destructive"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>
                {project.projectType === 'SHAPE_UP' && (project.cycleCount || 0) > 0 ? t('projectDetail.cannotDelete') : t('projectDetail.delete')}
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>
      </div>

      {/* Description */}
      {project.description && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t('projectDetail.description')}</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground">{project.description}</p>
          </CardContent>
        </Card>
      )}

      {/* Statistics Grid */}
      <div className={`grid grid-cols-1 gap-4 ${project.projectType === 'KANBAN' ? 'md:grid-cols-2' : 'md:grid-cols-4'}`}>
        {/* Project Type Badge */}
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className={`p-2 rounded-lg ${project.projectType === 'KANBAN' ? 'bg-purple-500/10' : 'bg-primary/10'}`}>
                {project.projectType === 'KANBAN' ? (
                  <Kanban className="h-5 w-5 text-purple-600" />
                ) : (
                  <TrendingUp className="h-5 w-5 text-primary" />
                )}
              </div>
              <div>
                <p className="text-sm text-muted-foreground">{t('projectDetail.projectType')}</p>
                <p className="text-2xl font-bold">
                  {project.projectType === 'KANBAN' ? t('projects.kanban') : t('projects.shapeUp')}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Shape Up specific stats */}
        {project.projectType !== 'KANBAN' && (
          <>
            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-green-500/10">
                    <Play className="h-5 w-5 text-green-600" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">{t('projectDetail.activeCycles')}</p>
                    <p className="text-2xl font-bold">{activeCycles.length}</p>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="pt-6">
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-blue-500/10">
                    <CheckCircle2 className="h-5 w-5 text-blue-600" />
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground">{t('projectDetail.completedCycles')}</p>
                    <p className="text-2xl font-bold">{completedCycles.length}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </>
        )}

        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-lg bg-purple-500/10">
                <Users className="h-5 w-5 text-purple-600" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">{t('projectDetail.teams')}</p>
                <p className="text-2xl font-bold">{teams.length}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Owner Information */}
      {project.ownerName && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t('projectDetail.projectOwner')}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex items-center gap-2">
              <Users className="h-5 w-5 text-muted-foreground" />
              <span className="font-medium">{project.ownerName}</span>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Kanban: Quick Access to Backlog */}
      {project.projectType === 'KANBAN' && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg">{t('projectDetail.backlog')}</CardTitle>
              <Button size="sm" onClick={() => navigate('/backlog')}>
                <ListTodo className="h-4 w-4 mr-2" />
                {t('projectDetail.viewBacklog')}
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground">
              {t('projectDetail.kanbanDescription')}
            </p>
          </CardContent>
        </Card>
      )}

      {/* Shape Up: Cycles List */}
      {project.projectType !== 'KANBAN' && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-lg">{t('projectDetail.cycles')}</CardTitle>
              <Button size="sm" onClick={() => navigate(`/cycles/new?project=${project.id}`)}>
                <Plus className="h-4 w-4 mr-2" />
                {t('projectDetail.newCycle')}
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {cycles.length === 0 ? (
              <p className="text-center text-muted-foreground py-8">
                {t('projectDetail.noCycles')}
              </p>
            ) : (
              <div className="space-y-3">
                {cycles.map((cycle) => (
                  <Link
                    key={cycle.id}
                    to={`/cycles/${cycle.id}`}
                    className="block p-4 rounded-lg border bg-card hover:bg-accent/50 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <h4 className="font-semibold">{cycle.name}</h4>
                          {getCycleStatusBadge(cycle)}
                        </div>
                        <div className="flex items-center gap-4 text-sm text-muted-foreground">
                          <div className="flex items-center gap-1">
                            <Calendar className="h-3 w-3" aria-hidden="true" />
                            <time dateTime={cycle.startDate}>{formatDate(cycle.startDate)}</time>
                            {' - '}
                            <time dateTime={cycle.endDate}>{formatDate(cycle.endDate)}</time>
                          </div>
                          {cycle.pitchCount !== undefined && (
                            <div className="flex items-center gap-1">
                              <TrendingUp className="h-3 w-3" />
                              {cycle.pitchCount} {t('projectDetail.pitches')}
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Teams List */}
      {teams.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t('projectDetail.teams')}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {teams.map((team) => (
                <div
                  key={team.id}
                  className="flex items-center justify-between p-3 rounded-lg border bg-card"
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-primary/10">
                      <Users className="h-4 w-4 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium">{team.name}</p>
                    </div>
                  </div>
                  <Badge variant="outline">
                    {team.assignments?.length || 0} {t('projectDetail.members')}
                  </Badge>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialog} onOpenChange={setDeleteDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('projectDetail.deleteProject')}</DialogTitle>
            <DialogDescription>
              {t('projectDetail.deleteConfirm', { name: project.name })}
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
