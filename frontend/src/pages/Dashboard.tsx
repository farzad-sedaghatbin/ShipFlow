import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQueries } from '@tanstack/react-query';
import {
  RefreshCw,
  FileText,
  Users,
  TrendingUp,
  Rocket,
  Settings,
} from 'lucide-react';
import { cycleService } from '../services/cycleService';
import { pitchService } from '../services/pitchService';
import { teamService } from '../services/teamService';
import { dashboardWidgetApi } from '../services/dashboardApi';
import { Cycle, Pitch, Team } from '../types';
import { DashboardWidget } from '../types/dashboard';
import { DashboardSkeleton } from '../components/Skeletons';
import EmptyState from '../components/EmptyState';
import { useProject } from '../contexts';
import {
  WelcomeIllustration,
} from '../components/illustrations';
import MotionContainer from '../components/MotionContainer';
import { AnimatedCard } from '../components/animations';
import QuickLinks from '../components/QuickLinks';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { DashboardCustomizer } from '../components/DashboardCustomizer';
import { DashboardTabs } from '../components/DashboardTabs';
import { STALE_TIMES, queryKeys } from '../lib/queryClient';

export default function Dashboard() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { currentProject, isAllProjectsSelected, isKanbanProject } = useProject();
  const [showCustomizer, setShowCustomizer] = useState(false);

  const cyclesQueryKey = isAllProjectsSelected
    ? [...queryKeys.cycles.active(), 'my']
    : currentProject
    ? queryKeys.cycles.byProject(currentProject.id)
    : ['cycles', 'none'];

  const [cyclesQuery, pitchesQuery, teamsQuery, widgetsQuery] = useQueries({
    queries: [
      {
        queryKey: cyclesQueryKey,
        queryFn: async (): Promise<Cycle[]> => {
          if (!isAllProjectsSelected && !currentProject) return [];
          const res = isAllProjectsSelected
            ? await cycleService.getMyActiveCycles()
            : await cycleService.getActiveByProject(currentProject!.id);
          return res.data;
        },
        staleTime: STALE_TIMES.entities,
        placeholderData: (previousData: Cycle[] | undefined) => previousData,
      },
      {
        queryKey: [...queryKeys.pitches.lists(), 'my'],
        queryFn: async (): Promise<Pitch[]> => {
          const res = await pitchService.getMyPitches();
          const pitches: Pitch[] = res.data;
          if (!isAllProjectsSelected && currentProject) {
            return pitches.filter((p) => p.projectId === currentProject.id);
          }
          return pitches;
        },
        staleTime: STALE_TIMES.entities,
      },
      {
        queryKey: queryKeys.teams.lists(),
        queryFn: async (): Promise<Team[]> => {
          const res = await teamService.getAll();
          return res.data;
        },
        staleTime: STALE_TIMES.reference,
      },
      {
        queryKey: queryKeys.dashboard.widgets(),
        queryFn: async (): Promise<DashboardWidget[]> => {
          try {
            return await dashboardWidgetApi.getAllWidgets();
          } catch (error) {
            console.error('Failed to load dashboard widgets:', error);
            return [];
          }
        },
        staleTime: STALE_TIMES.reference,
      },
    ],
  });

  const loading = cyclesQuery.isLoading || pitchesQuery.isLoading || teamsQuery.isLoading || widgetsQuery.isLoading;
  const activeCycles: Cycle[] = cyclesQuery.data ?? [];
  const recentPitches: Pitch[] = (pitchesQuery.data ?? []).slice(0, 5);
  const teams: Team[] = teamsQuery.data ?? [];
  const widgets: DashboardWidget[] = widgetsQuery.data ?? [];

  const refreshWidgets = () => { widgetsQuery.refetch(); };

  if (loading) {
    return <DashboardSkeleton />;
  }

  const totalPitches = recentPitches.length;
  const completedPitches = recentPitches.filter((p) => p.status === 'DONE').length;
  const inProgressPitches = recentPitches.filter((p) => ['STARTED', 'IN_PROGRESS', 'TESTING'].includes(p.status)).length;

  const isNewUser = activeCycles.length === 0 && recentPitches.length === 0 && teams.length === 0;

  // Welcome screen for new users
  if (isNewUser) {
    const kanbanSteps = [
      {
        icon: '📋',
        title: t('dashboard.welcome.steps.createTasks.title', 'Create Tasks'),
        description: t('dashboard.welcome.steps.createTasks.description', 'Add tasks to your backlog and organize them on the Kanban board'),
      },
      {
        icon: '🎯',
        title: t('dashboard.welcome.steps.moveCards.title', 'Move Cards'),
        description: t('dashboard.welcome.steps.moveCards.description', 'Drag tasks through TODO, In Progress, and Done columns'),
      },
      {
        icon: '⏱️',
        title: t('dashboard.welcome.steps.trackTime.title', 'Track Time'),
        description: t('dashboard.welcome.steps.trackTime.description', 'Use work log timers to track effort on each task'),
      },
    ];

    const shapeUpSteps = [
      {
        icon: '🔄',
        title: t('dashboard.welcome.steps.createCycle.title'),
        description: t('dashboard.welcome.steps.createCycle.description'),
      },
      {
        icon: '💡',
        title: t('dashboard.welcome.steps.addPitches.title'),
        description: t('dashboard.welcome.steps.addPitches.description'),
      },
      {
        icon: '⛰️',
        title: t('dashboard.welcome.steps.trackProgress.title'),
        description: t('dashboard.welcome.steps.trackProgress.description'),
      },
    ];

    return (
      <div>
        <Card className="bg-gradient-to-br from-primary/5 to-secondary/5 border-primary/10 mb-4">
          <CardContent className="py-6">
            <EmptyState
              illustration={<WelcomeIllustration width={280} height={200} />}
              title={isKanbanProject 
                ? t('dashboard.welcome.kanbanTitle', 'Welcome to Your Kanban Project')
                : t('dashboard.welcome.title')}
              description={isKanbanProject
                ? t('dashboard.welcome.kanbanDescription', 'Start adding tasks and managing your continuous workflow')
                : t('dashboard.welcome.description')}
              size="large"
              onboardingSteps={isKanbanProject ? kanbanSteps : shapeUpSteps}
              action={{
                label: isKanbanProject 
                  ? t('dashboard.welcome.createFirstTask', 'Create First Task')
                  : t('dashboard.welcome.createFirstCycle'),
                onClick: () => window.location.href = isKanbanProject ? '/backlog' : '/cycles/new',
                startIcon: <Rocket className="w-4 h-4 me-2" />,
              }}
              secondaryAction={{
                label: t('dashboard.welcome.learnMore'),
                onClick: () => {
                  if (isKanbanProject) {
                    navigate('/help/project-types');
                  } else {
                    window.open('https://basecamp.com/shapeup', '_blank');
                  }
                },
              }}
            />
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">{t('dashboard.title')}</h1>
          <p className="text-sm text-muted-foreground">
            {isAllProjectsSelected 
              ? t('dashboard.showingAllProjects') 
              : t('dashboard.showingProject', { name: currentProject?.name })}
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setShowCustomizer(!showCustomizer)}
          className="gap-2"
        >
          <Settings className="w-4 h-4" />
          {showCustomizer ? t('dashboard.hideWidgets') : t('dashboard.customizeWidgets')}
        </Button>
      </div>

      {/* Quick Links Section */}
      <MotionContainer delay={0.05} className="mb-4">
        <QuickLinks />
      </MotionContainer>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        {!isKanbanProject && (
        <AnimatedCard
          delay={0.1}
          animation="fadeUp"
          hoverEffect
          className="bg-gradient-to-br from-primary/8 to-primary/2 border-l-4 border-l-primary"
        >
          <CardContent className="p-4">
            <div className="flex items-center mb-2">
              <div className="bg-gradient-to-br from-primary to-primary/80 rounded-xl p-2.5 me-2 shadow-lg shadow-primary/30">
                <RefreshCw className="w-5 h-5 text-primary-foreground" />
              </div>
              <span className="text-sm text-muted-foreground font-semibold">{t('dashboard.activeCycles')}</span>
            </div>
            <p className="text-3xl font-extrabold text-primary">{activeCycles.length}</p>
          </CardContent>
        </AnimatedCard>
        )}

        {!isKanbanProject && (
        <AnimatedCard
          delay={0.2}
          animation="fadeUp"
          hoverEffect
          className="bg-gradient-to-br from-violet-500/8 to-violet-500/2 border-l-4 border-l-violet-500"
        >
          <CardContent className="p-4">
            <div className="flex items-center mb-2">
              <div className="bg-gradient-to-br from-violet-500 to-violet-600 rounded-xl p-2.5 me-2 shadow-lg shadow-violet-500/30">
                <FileText className="w-5 h-5 text-white" />
              </div>
              <span className="text-sm text-muted-foreground font-semibold">{t('dashboard.totalPitches')}</span>
            </div>
            <p className="text-3xl font-extrabold text-violet-500">{totalPitches}</p>
          </CardContent>
        </AnimatedCard>
        )}

        <AnimatedCard
          delay={0.3}
          animation="fadeUp"
          hoverEffect
          className="bg-gradient-to-br from-emerald-500/8 to-emerald-500/2 border-l-4 border-l-emerald-500"
        >
          <CardContent className="p-4">
            <div className="flex items-center mb-2">
              <div className="bg-gradient-to-br from-emerald-500 to-emerald-600 rounded-xl p-2.5 me-2 shadow-lg shadow-emerald-500/30">
                <TrendingUp className="w-5 h-5 text-white" />
              </div>
              <span className="text-sm text-muted-foreground font-semibold">{t('dashboard.completed')}</span>
            </div>
            <p className="text-3xl font-extrabold text-emerald-500">{completedPitches}</p>
          </CardContent>
        </AnimatedCard>

        <AnimatedCard
          delay={0.4}
          animation="fadeUp"
          hoverEffect
          className="bg-gradient-to-br from-amber-500/8 to-amber-500/2 border-l-4 border-l-amber-500"
        >
          <CardContent className="p-4">
            <div className="flex items-center mb-2">
              <div className="bg-gradient-to-br from-amber-500 to-amber-600 rounded-xl p-2.5 me-2 shadow-lg shadow-amber-500/30">
                <Users className="w-5 h-5 text-white" />
              </div>
              <span className="text-sm text-muted-foreground font-semibold">{t('dashboard.inProgress')}</span>
            </div>
            <p className="text-3xl font-extrabold text-amber-600">{inProgressPitches}</p>
          </CardContent>
        </AnimatedCard>
      </div>

      {/* Widget Customizer - appears before widgets when toggled */}
      {showCustomizer && (
        <MotionContainer delay={0.45} className="mb-4">
          <DashboardCustomizer widgets={widgets} onUpdate={refreshWidgets} />
        </MotionContainer>
      )}

      {/* Tabbed Dashboard Content */}
      <MotionContainer delay={0.5}>
        <DashboardTabs
          widgets={widgets}
          activeCycles={activeCycles}
          recentPitches={recentPitches}
          projectId={isAllProjectsSelected ? undefined : currentProject?.id}
          isKanbanProject={isKanbanProject}
        />
      </MotionContainer>
    </div>
  );
}
