import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { LayoutGrid, Brain, Activity } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { DashboardWidget } from '../types/dashboard';
import { Cycle, Pitch } from '../types';
import {
  OverdueTasksWidget,
  BlockedTasksWidget,
  UpcomingDeadlinesWidget,
  MyTasksWidget,
  TeamWorkloadWidget,
  CycleProgressWidget,
  RecentActivityWidget,
  CycleSummaryWidget,
  CycleSignalsWidget,
  AiRiskAdvisoryWidget,
  HillChartWidgetWrapper,
  ActiveCyclesWidget,
  RecentPitchesWidget,
} from './widgets';
import MotionContainer from './MotionContainer';

// Activity tab widget types for filtering
const ACTIVITY_WIDGETS = ['RECENT_ACTIVITY', 'OVERDUE_TASKS', 'BLOCKED_TASKS', 'UPCOMING_DEADLINES', 'MY_TASKS', 'TEAM_WORKLOAD'];

const TAB_STORAGE_KEY = 'dashboard-active-tab';

interface DashboardTabsProps {
  widgets: DashboardWidget[];
  activeCycles: Cycle[];
  recentPitches: Pitch[];
  projectId?: number;
  isKanbanProject?: boolean;
}

export function DashboardTabs({
  widgets,
  activeCycles,
  recentPitches,
  projectId,
  isKanbanProject = false,
}: DashboardTabsProps) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState(() => {
    return localStorage.getItem(TAB_STORAGE_KEY) || 'overview';
  });

  useEffect(() => {
    localStorage.setItem(TAB_STORAGE_KEY, activeTab);
  }, [activeTab]);

  const isWidgetVisible = (widgetType: string) => {
    const widget = widgets.find((w) => w.widgetType === widgetType);
    return widget ? widget.isVisible : true; // Default to visible if not found
  };

  const firstCycle = activeCycles[0];

  const renderOverviewWidgets = () => {
    if (isKanbanProject) return null;

    return (
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {isWidgetVisible('ACTIVE_CYCLES') && (
          <MotionContainer delay={0.1}>
            <ActiveCyclesWidget cycles={activeCycles} />
          </MotionContainer>
        )}
        
        {isWidgetVisible('HILL_CHART') && (
          <MotionContainer delay={0.15}>
            <HillChartWidgetWrapper projectId={projectId} maxPoints={5} />
          </MotionContainer>
        )}
        
        {isWidgetVisible('CYCLE_PROGRESS') && (
          <MotionContainer delay={0.2}>
            <CycleProgressWidget />
          </MotionContainer>
        )}
        
        {isWidgetVisible('RECENT_PITCHES') && (
          <MotionContainer delay={0.25}>
            <RecentPitchesWidget pitches={recentPitches} />
          </MotionContainer>
        )}
      </div>
    );
  };

  const renderAiInsightsWidgets = () => {
    if (isKanbanProject || !firstCycle) {
      return (
        <div className="text-center py-8 text-muted-foreground">
          {t('dashboard.tabs.noAiInsights')}
        </div>
      );
    }

    return (
      <div className="space-y-4">
        {/* AI Risk Advisory - show compact cards for multiple cycles */}
        {isWidgetVisible('AI_RISK_ADVISORY') && activeCycles.length > 0 && (
          <MotionContainer delay={0.1}>
            <div>
              <h3 className="text-sm font-medium text-muted-foreground mb-2">
                {t('dashboard.aiRiskAnalysis')}
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-2 mb-4">
                {activeCycles.slice(0, 3).map((cycle) => (
                  <AiRiskAdvisoryWidget
                    key={cycle.id}
                    cycleId={cycle.id}
                    cycleName={cycle.name}
                    compact
                  />
                ))}
              </div>
              {/* Full risk overview for first cycle */}
              <AiRiskAdvisoryWidget
                cycleId={firstCycle.id}
                cycleName={firstCycle.name}
              />
            </div>
          </MotionContainer>
        )}

        {/* Cycle Signals and Summary full width */}
        {isWidgetVisible('CYCLE_SIGNALS') && (
          <MotionContainer delay={0.15}>
            <CycleSignalsWidget cycleId={firstCycle.id} projectId={projectId} />
          </MotionContainer>
        )}
        
        {isWidgetVisible('CYCLE_SUMMARY') && (
          <MotionContainer delay={0.2}>
            <CycleSummaryWidget cycleId={firstCycle.id} />
          </MotionContainer>
        )}
      </div>
    );
  };

  const renderActivityWidgets = () => {
    const visibleWidgets = ACTIVITY_WIDGETS.filter(isWidgetVisible);
    
    if (visibleWidgets.length === 0) {
      return (
        <div className="text-center py-8 text-muted-foreground">
          {t('dashboard.tabs.noActivityWidgets')}
        </div>
      );
    }

    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {isWidgetVisible('RECENT_ACTIVITY') && (
          <MotionContainer delay={0.1}>
            <RecentActivityWidget projectId={projectId} />
          </MotionContainer>
        )}
        
        {isWidgetVisible('OVERDUE_TASKS') && (
          <MotionContainer delay={0.15}>
            <OverdueTasksWidget />
          </MotionContainer>
        )}
        
        {isWidgetVisible('BLOCKED_TASKS') && (
          <MotionContainer delay={0.2}>
            <BlockedTasksWidget />
          </MotionContainer>
        )}
        
        {isWidgetVisible('UPCOMING_DEADLINES') && (
          <MotionContainer delay={0.25}>
            <UpcomingDeadlinesWidget />
          </MotionContainer>
        )}
        
        {isWidgetVisible('MY_TASKS') && (
          <MotionContainer delay={0.3}>
            <MyTasksWidget />
          </MotionContainer>
        )}
        
        {isWidgetVisible('TEAM_WORKLOAD') && (
          <MotionContainer delay={0.35}>
            <TeamWorkloadWidget />
          </MotionContainer>
        )}
      </div>
    );
  };

  return (
    <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
      <TabsList className="grid w-full grid-cols-3 mb-4">
        <TabsTrigger value="overview" className="gap-2">
          <LayoutGrid className="w-4 h-4" />
          <span className="hidden sm:inline">{t('dashboard.tabs.overview')}</span>
        </TabsTrigger>
        <TabsTrigger value="ai-insights" className="gap-2" disabled={isKanbanProject}>
          <Brain className="w-4 h-4" />
          <span className="hidden sm:inline">{t('dashboard.tabs.aiInsights')}</span>
        </TabsTrigger>
        <TabsTrigger value="activity" className="gap-2">
          <Activity className="w-4 h-4" />
          <span className="hidden sm:inline">{t('dashboard.tabs.activity')}</span>
        </TabsTrigger>
      </TabsList>

      <TabsContent value="overview" className="mt-0">
        {renderOverviewWidgets()}
      </TabsContent>

      <TabsContent value="ai-insights" className="mt-0">
        {renderAiInsightsWidgets()}
      </TabsContent>

      <TabsContent value="activity" className="mt-0">
        {renderActivityWidgets()}
      </TabsContent>
    </Tabs>
  );
}
