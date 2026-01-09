import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  RefreshCw,
  FileText,
  Users,
  TrendingUp,
  Plus,
  Rocket,
  Settings,
} from 'lucide-react';
import { cycleService } from '../services/cycleService';
import { pitchService } from '../services/pitchService';
import { teamService } from '../services/teamService';
import { dashboardWidgetApi } from '../services/dashboardApi';
import { Cycle, Pitch, Team } from '../types';
import { DashboardWidget } from '../types/dashboard';
import StatusChip from '../components/StatusChip';
import { HillChartWidget } from '../components/HillChartWidget';
import CycleRiskOverview from '../components/CycleRiskOverview';
import { DashboardSkeleton } from '../components/Skeletons';
import EmptyState from '../components/EmptyState';
import { useProject } from '../contexts';
import {
  EmptyCyclesIllustration,
  EmptyPitchesIllustration,
  WelcomeIllustration,
} from '../components/illustrations';
import MotionContainer from '../components/MotionContainer';
import { AnimatedCard } from '../components/animations';
import RecentActivityFeed from '../components/RecentActivityFeed';
import QuickLinks from '../components/QuickLinks';
import { cn } from '@/lib/utils';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import {
  OverdueTasksWidget,
  BlockedTasksWidget,
  UpcomingDeadlinesWidget,
  MyTasksWidget,
  TeamWorkloadWidget,
  CycleProgressWidget,
  RecentActivityWidget,
} from '../components/widgets';
import { DashboardCustomizer } from '../components/DashboardCustomizer';

export default function Dashboard() {
  const { currentProject, isAllProjectsSelected } = useProject();
  const [activeCycles, setActiveCycles] = useState<Cycle[]>([]);
  const [recentPitches, setRecentPitches] = useState<Pitch[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [widgets, setWidgets] = useState<DashboardWidget[]>([]);
  const [showCustomizer, setShowCustomizer] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const abortController = new AbortController();
    loadData();
    return () => abortController.abort();
  }, [currentProject, isAllProjectsSelected]);

  const loadData = async () => {
    try {
      setLoading(true);

      let cyclesPromise;
      if (isAllProjectsSelected) {
        cyclesPromise = cycleService.getActive();
      } else if (currentProject) {
        cyclesPromise = cycleService.getActiveByProject(currentProject.id);
      } else {
        cyclesPromise = Promise.resolve({ data: [] });
      }

      const [cyclesRes, pitchesRes, teamsRes, widgetsData] = await Promise.all([
        cyclesPromise,
        pitchService.getAll(),
        teamService.getAll(),
        dashboardWidgetApi.getAllWidgets().catch(() => []),
      ]);

      const cycles = cyclesRes.data;
      setActiveCycles(cycles);

      const cycleIds = new Set(cycles.map((c: Cycle) => c.id));

      let filteredPitches = pitchesRes.data;
      let filteredTeams = teamsRes.data;

      if (!isAllProjectsSelected && currentProject) {
        filteredPitches = pitchesRes.data.filter((p: Pitch) => p.projectId === currentProject.id);
        filteredTeams = teamsRes.data.filter((t: Team) => cycleIds.has(t.cycleId!));
      }

      setRecentPitches(filteredPitches.slice(0, 5));
      setTeams(filteredTeams);
      setWidgets(widgetsData);
    } catch (error: any) {
      if (error.name !== 'CanceledError') {
        console.error('Failed to load dashboard data:', error);
      }
    } finally {
      setLoading(false);
    }
  };

  const renderWidget = (widget: DashboardWidget) => {
    const projectId = isAllProjectsSelected ? undefined : currentProject?.id;
    
    switch (widget.widgetType) {
      case 'OVERDUE_TASKS':
        return <OverdueTasksWidget key={widget.id} />;
      case 'BLOCKED_TASKS':
        return <BlockedTasksWidget key={widget.id} />;
      case 'UPCOMING_DEADLINES':
        return <UpcomingDeadlinesWidget key={widget.id} />;
      case 'MY_TASKS':
        return <MyTasksWidget key={widget.id} />;
      case 'TEAM_WORKLOAD':
        return <TeamWorkloadWidget key={widget.id} />;
      case 'CYCLE_PROGRESS':
        return <CycleProgressWidget key={widget.id} />;
      case 'RECENT_ACTIVITY':
        return <RecentActivityWidget key={widget.id} projectId={projectId} />;
      default:
        return null;
    }
  };

  if (loading) {
    return <DashboardSkeleton />;
  }

  const totalPitches = recentPitches.length;
  const completedPitches = recentPitches.filter((p) => p.status === 'DONE').length;
  const inProgressPitches = recentPitches.filter((p) => ['STARTED', 'IN_PROGRESS', 'TESTING'].includes(p.status)).length;

  const isNewUser = activeCycles.length === 0 && recentPitches.length === 0 && teams.length === 0;

  // Welcome screen for new users
  if (isNewUser) {
    return (
      <div>
        <Card className="bg-gradient-to-br from-primary/5 to-secondary/5 border-primary/10 mb-4">
          <CardContent className="py-6">
            <EmptyState
              illustration={<WelcomeIllustration width={280} height={200} />}
              title="Welcome to Shape Up Tracker! 🎉"
              description="You're all set to start shipping. Shape Up is about making meaningful progress in focused cycles. Let's get you started with your first cycle."
              size="large"
              onboardingSteps={[
                {
                  icon: '🔄',
                  title: 'Create a Cycle',
                  description: 'Start with a 6-week cycle. This is your focused shipping window.',
                },
                {
                  icon: '💡',
                  title: 'Add Pitches',
                  description: 'Shape your ideas into pitches with clear scope and appetite.',
                },
                {
                  icon: '⛰️',
                  title: 'Track on Hill Chart',
                  description: 'Visualize progress as you figure things out and make them happen.',
                },
              ]}
              action={{
                label: 'Create Your First Cycle',
                onClick: () => window.location.href = '/cycles/new',
                startIcon: <Rocket className="w-4 h-4 mr-2" />,
              }}
              secondaryAction={{
                label: 'Learn About Shape Up',
                onClick: () => window.open('https://basecamp.com/shapeup', '_blank'),
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
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-sm text-muted-foreground">
            {isAllProjectsSelected ? 'Showing data from all projects' : `Showing data for ${currentProject?.name}`}
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={() => setShowCustomizer(!showCustomizer)}
          className="gap-2"
        >
          <Settings className="w-4 h-4" />
          {showCustomizer ? 'Hide' : 'Customize'} Widgets
        </Button>
      </div>

      {/* Quick Links Section */}
      <MotionContainer delay={0.05} className="mb-4">
        <QuickLinks />
      </MotionContainer>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        <AnimatedCard
          delay={0.1}
          animation="fadeUp"
          hoverEffect
          className="bg-gradient-to-br from-primary/8 to-primary/2 border-l-4 border-l-primary"
        >
          <CardContent className="p-4">
            <div className="flex items-center mb-2">
              <div className="bg-gradient-to-br from-primary to-primary/80 rounded-xl p-2.5 mr-2 shadow-lg shadow-primary/30">
                <RefreshCw className="w-5 h-5 text-primary-foreground" />
              </div>
              <span className="text-sm text-muted-foreground font-semibold">Active Cycles</span>
            </div>
            <p className="text-3xl font-extrabold text-primary">{activeCycles.length}</p>
          </CardContent>
        </AnimatedCard>

        <AnimatedCard
          delay={0.2}
          animation="fadeUp"
          hoverEffect
          className="bg-gradient-to-br from-violet-500/8 to-violet-500/2 border-l-4 border-l-violet-500"
        >
          <CardContent className="p-4">
            <div className="flex items-center mb-2">
              <div className="bg-gradient-to-br from-violet-500 to-violet-600 rounded-xl p-2.5 mr-2 shadow-lg shadow-violet-500/30">
                <FileText className="w-5 h-5 text-white" />
              </div>
              <span className="text-sm text-muted-foreground font-semibold">Total Pitches</span>
            </div>
            <p className="text-3xl font-extrabold text-violet-500">{totalPitches}</p>
          </CardContent>
        </AnimatedCard>

        <AnimatedCard
          delay={0.3}
          animation="fadeUp"
          hoverEffect
          className="bg-gradient-to-br from-emerald-500/8 to-emerald-500/2 border-l-4 border-l-emerald-500"
        >
          <CardContent className="p-4">
            <div className="flex items-center mb-2">
              <div className="bg-gradient-to-br from-emerald-500 to-emerald-600 rounded-xl p-2.5 mr-2 shadow-lg shadow-emerald-500/30">
                <TrendingUp className="w-5 h-5 text-white" />
              </div>
              <span className="text-sm text-muted-foreground font-semibold">Completed</span>
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
              <div className="bg-gradient-to-br from-amber-500 to-amber-600 rounded-xl p-2.5 mr-2 shadow-lg shadow-amber-500/30">
                <Users className="w-5 h-5 text-white" />
              </div>
              <span className="text-sm text-muted-foreground font-semibold">In Progress</span>
            </div>
            <p className="text-3xl font-extrabold text-amber-600">{inProgressPitches}</p>
          </CardContent>
        </AnimatedCard>
      </div>

      {/* Widget Customizer - appears before widgets when toggled */}
      {showCustomizer && (
        <MotionContainer delay={0.45} className="mb-4">
          <DashboardCustomizer onUpdate={loadData} />
        </MotionContainer>
      )}

      {/* Customizable Widgets Grid */}
      {widgets.filter((w) => w.isVisible).length > 0 && (
        <MotionContainer delay={0.5} className="mb-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {widgets
              .filter((widget) => widget.isVisible)
              .sort((a, b) => a.displayOrder - b.displayOrder)
              .map((widget) => renderWidget(widget))}
          </div>
        </MotionContainer>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {/* Left Column: Active Cycles + Hill Chart */}
        <MotionContainer delay={0.6} className="space-y-3">
          {/* Active Cycles */}
          <Card>
            <CardContent className="p-4">
              <div className="flex justify-between items-center mb-3">
                <h2 className="text-lg font-semibold text-foreground">Active Cycles</h2>
                <Button variant="ghost" size="sm" asChild>
                  <Link to="/cycles">View All</Link>
                </Button>
              </div>
              {activeCycles.length === 0 ? (
                <EmptyState
                  illustration={<EmptyCyclesIllustration width={160} height={120} />}
                  title="No active cycles"
                  description="Start a new cycle to begin shipping"
                  size="small"
                  compact
                  action={{
                    label: 'New Cycle',
                    onClick: () => window.location.href = '/cycles/new',
                    startIcon: <Plus className="w-4 h-4 mr-1" />,
                  }}
                />
              ) : (
                <div className="space-y-2">
                  {activeCycles.map((cycle) => (
                    <Link
                      key={cycle.id}
                      to={`/cycles/${cycle.id}`}
                      className="block p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
                    >
                      <div className="flex justify-between items-center">
                        <span className="font-semibold text-foreground">{cycle.name}</span>
                        <Badge
                          variant="secondary"
                          className={cn(
                            cycle.phase === 'BUILD' && 'bg-primary/15 text-primary',
                            cycle.phase === 'SHAPING' && 'bg-blue-500/15 text-blue-500',
                            cycle.phase === 'BETTING' && 'bg-amber-500/15 text-amber-500',
                            cycle.phase === 'COOLDOWN' && 'bg-violet-500/15 text-violet-500'
                          )}
                        >
                          {cycle.phase}
                        </Badge>
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {new Date(cycle.startDate).toLocaleDateString()} - {new Date(cycle.endDate).toLocaleDateString()}
                      </p>
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Hill Chart Widget */}
          <HillChartWidget
            maxPoints={5}
            projectId={isAllProjectsSelected ? undefined : currentProject?.id}
          />

          {/* Recent Activity Feed */}
          <RecentActivityFeed
            maxItems={5}
            compact
            projectId={isAllProjectsSelected ? undefined : currentProject?.id}
          />
        </MotionContainer>

        {/* Right Column: Recent Pitches */}
        <MotionContainer delay={0.7}>
          <Card>
            <CardContent className="p-4">
              <div className="flex justify-between items-center mb-3">
                <h2 className="text-lg font-semibold text-foreground">Recent Pitches</h2>
                <Button variant="ghost" size="sm" asChild>
                  <Link to="/pitches">View All</Link>
                </Button>
              </div>
              {recentPitches.length === 0 ? (
                <EmptyState
                  illustration={<EmptyPitchesIllustration width={160} height={120} />}
                  title="No pitches yet"
                  description="Shape your ideas into actionable pitches"
                  size="small"
                  compact
                  action={{
                    label: 'Create Pitch',
                    onClick: () => window.location.href = '/pitches/new',
                    startIcon: <Plus className="w-4 h-4 mr-1" />,
                  }}
                />
              ) : (
                <div className="space-y-2">
                  {recentPitches.map((pitch) => (
                    <Link
                      key={pitch.id}
                      to={`/pitches/${pitch.id}`}
                      className="block p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
                    >
                      <div className="flex justify-between items-center mb-1">
                        <span className="font-semibold text-foreground">{pitch.title}</span>
                        <StatusChip status={pitch.status} />
                      </div>
                      <div className="flex justify-between items-center text-sm text-muted-foreground">
                        <span>{pitch.teamName || 'Unassigned'} • {pitch.appetiteDays} days</span>
                        <span>{pitch.progressPercentage?.toFixed(0) || 0}%</span>
                      </div>
                      <Progress
                        value={Math.min(pitch.progressPercentage || 0, 100)}
                        className={cn(
                          'h-1 mt-1',
                          (pitch.progressPercentage || 0) > 100 && '[&>div]:bg-destructive'
                        )}
                      />
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </MotionContainer>

        {/* Cycle Risk Overview */}
        {activeCycles.length > 0 && (
          <div className="col-span-full">
            <h2 className="text-lg font-semibold text-foreground mb-2">AI Risk Analysis</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
              {activeCycles.slice(0, 3).map((cycle) => (
                <CycleRiskOverview
                  key={cycle.id}
                  cycleId={cycle.id}
                  cycleName={cycle.name}
                  compact
                />
              ))}
            </div>
          </div>
        )}

        {/* Full Risk Overview for First Active Cycle */}
        {activeCycles.length > 0 && (
          <div className="col-span-full">
            <CycleRiskOverview
              cycleId={activeCycles[0].id}
              cycleName={activeCycles[0].name}
            />
          </div>
        )}
      </div>
    </div>
  );
}
