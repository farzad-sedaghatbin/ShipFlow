import { Routes, Route, Navigate } from 'react-router-dom';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Dashboard from './pages/Dashboard';
import CycleList from './pages/CycleList';
import CycleDetail from './pages/CycleDetail';
import CycleForm from './pages/CycleForm';
import PitchBoard from './pages/PitchBoard';
import PitchDetail from './pages/PitchDetail';
import WorkLogsPage from './pages/WorkLogsPage';
import MeetingList from './pages/MeetingList';
import Reports from './pages/Reports';
import Teams from './pages/Teams';
import Login from './pages/Login';
import Landing from './pages/Landing';
import ReleaseNotes from './pages/ReleaseNotes';
import CompetitorsComparison from './pages/CompetitorsComparison';
import Profile from './pages/Profile';
import People from './pages/People';
import UserManagement from './pages/UserManagement';
import PermissionManagement from './pages/PermissionManagement';
import OrganizationSettings from './pages/OrganizationSettings';
import SlackIntegration from './pages/SlackIntegration';
import GitHubIntegration from './pages/integrations/GitHubIntegration';
import TeamsIntegration from './pages/integrations/TeamsIntegration';
import McpIntegration from './pages/integrations/McpIntegration';
import Projects from './pages/Projects';
import ProjectDetail from './pages/ProjectDetail';
import HealthOverview from './pages/HealthOverview';
import BacklogPage from './pages/BacklogPage';
import TaskDetailPage from './pages/TaskDetailPage';
import BettingTable from './pages/BettingTable';
import PitchComparisonView from './pages/PitchComparisonView';
import { PitchHillChart } from './pages/PitchHillChart';
import { CycleHillChart } from './pages/CycleHillChart';
import TestCasesPage from './pages/TestCasesPage';
import TestCaseFormPage from './pages/TestCaseFormPage';
import TestCaseDetailPage from './pages/TestCaseDetailPage';
import TestRunPage from './pages/TestRunPage';
import AITestGeneratePage from './pages/AITestGeneratePage';
import WiseArchitecturePage from './pages/WiseArchitecturePage';
import AdviceHistoryPage from './pages/AdviceHistoryPage';
import BugReportsPage from './pages/BugReportsPage';
import PitchTestPage from './pages/PitchTestPage';
import CycleQADashboardPage from './pages/CycleQADashboardPage';
import RoadmapPage from './pages/RoadmapPage';
import InitiativeListPage from './pages/InitiativeListPage';
import InitiativeDetailPage from './pages/InitiativeDetailPage';
import InitiativeFormPage from './pages/InitiativeFormPage';
import EpicListPage from './pages/EpicListPage';
import EpicDetailPage from './pages/EpicDetailPage';
import EpicFormPage from './pages/EpicFormPage';
import ReleaseListPage from './pages/ReleaseListPage';
import ReleaseDetailPage from './pages/ReleaseDetailPage';
import ReleaseFormPage from './pages/ReleaseFormPage';
import RetroList from './pages/RetroList';
import RetroBoard from './pages/RetroBoard';
import CircuitBreakerMonitor from './pages/CircuitBreakerMonitor';
import CooldownActivities from './pages/CooldownActivities';
import CustomMetrics from './pages/CustomMetrics';
import MetricBuilder from './pages/MetricBuilder';
import DashboardManager from './pages/DashboardManager';
import DashboardView from './pages/DashboardView';
import HelpGuides from './pages/HelpGuides';
import GettingStartedGuide from './pages/guides/GettingStartedGuide';
import HillChartsGuide from './pages/guides/HillChartsGuide';
import BettingMeetingGuide from './pages/guides/BettingMeetingGuide';
import AIRiskAdvisorGuide from './pages/guides/AIRiskAdvisorGuide';
import CycleSetupGuide from './pages/guides/CycleSetupGuide';
import ProjectTypesGuide from './pages/guides/ProjectTypesGuide';
import QATestingGuide from './pages/guides/QATestingGuide';
import RetrospectivesGuide from './pages/guides/RetrospectivesGuide';
import CircuitBreakerGuide from './pages/guides/CircuitBreakerGuide';
import CooldownActivitiesGuide from './pages/guides/CooldownActivitiesGuide';
import ReportsGuide from './pages/guides/ReportsGuide';
import WiseArchitectureGuide from './pages/guides/WiseArchitectureGuide';
import { useToast, setToastHandler, ProjectProvider, TourProvider } from './contexts';
import { isRTLLanguage } from './i18n';

function App() {
  const { showToast } = useToast();
  const { i18n } = useTranslation();

  // Connect toast handler for use in api interceptors
  useEffect(() => {
    setToastHandler(showToast);
  }, [showToast]);

  // Ensure RTL direction is always set correctly on every render
  useEffect(() => {
    const currentLang = i18n.language;
    const dir = isRTLLanguage(currentLang) ? 'rtl' : 'ltr';
    
    if (document.documentElement.dir !== dir) {
      document.documentElement.dir = dir;
      document.documentElement.lang = currentLang;
      
      if (dir === 'rtl') {
        document.body.classList.add('rtl');
        document.body.classList.remove('ltr');
      } else {
        document.body.classList.add('ltr');
        document.body.classList.remove('rtl');
      }
    }
  }, [i18n.language]);

  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/welcome" element={<Navigate to="/" replace />} />
      <Route path="/compare" element={<CompetitorsComparison />} />
      <Route path="/releases" element={<ReleaseNotes />} />
      <Route path="/login" element={<Login />} />
      <Route
        path="/*"
        element={
          <ProtectedRoute>
            <ProjectProvider>
              <TourProvider>
                <Layout>
                  <Routes>
                    {/* Main Navigation - paths are relative to parent "/*" route */}
                    <Route path="dashboard" element={<Dashboard />} />
                    <Route path="projects" element={<Projects />} />
                    <Route path="projects/:id" element={<ProjectDetail />} />
                    <Route path="cycles" element={<CycleList />} />
                    <Route path="cycles/new" element={<CycleForm />} />
                    <Route path="cycles/:id" element={<CycleDetail />} />
                    <Route path="cycles/:id/edit" element={<CycleForm />} />
                    <Route path="cycles/:cycleId/hill-chart" element={<CycleHillChart />} />
                    <Route path="cycles/:cycleId/circuit-breaker" element={<CircuitBreakerMonitor />} />
                    <Route path="cycles/:cycleId/cooldown" element={<CooldownActivities />} />

                    {/* Cycle Workspace */}
                    <Route path="pitches" element={<PitchBoard />} />
                    <Route path="pitches/:pitchId/hill-chart" element={<PitchHillChart />} />
                    <Route path="pitches/:id" element={<PitchDetail />} />
                    <Route path="betting" element={<BettingTable />} />
                    <Route path="betting/comparison" element={<PitchComparisonView />} />
                    <Route path="health" element={<HealthOverview />} />
                    <Route path="retros" element={<RetroList />} />
                    <Route path="retros/:id" element={<RetroBoard />} />
                    <Route path="reports" element={<DashboardManager />} />
                    <Route path="reports/cycle-reports" element={<Reports />} />
                    <Route path="reports/:id" element={<DashboardView />} />

                    {/* Backlog */}
                    <Route path="backlog" element={<BacklogPage />} />
                    <Route path="backlog/:taskId" element={<TaskDetailPage />} />
                    {/* Legacy route redirects */}
                    <Route path="backlog/tasks" element={<Navigate to="/backlog?category=PITCH_SCOPE" replace />} />
                    <Route path="backlog/debt" element={<Navigate to="/backlog?category=DEBT_IMPROVEMENT" replace />} />
                    <Route path="tasks" element={<Navigate to="/backlog" replace />} />

                    {/* Time Tracking */}
                    <Route path="time/logs" element={<WorkLogsPage />} />
                    {/* Legacy route redirects */}
                    <Route path="worklogs" element={<Navigate to="/time/logs" replace />} />
                    <Route path="my-worklogs" element={<Navigate to="/time/logs" replace />} />

                    {/* Meetings */}
                    <Route path="meetings" element={<MeetingList />} />

                    {/* Organization */}
                    <Route path="people" element={<People />} />
                    <Route path="teams" element={<Teams />} />

                    {/* User Profile */}
                    <Route path="profile" element={<Profile />} />

                    {/* Admin */}
                    <Route path="users" element={<UserManagement />} />
                    <Route path="permissions" element={<PermissionManagement />} />
                    <Route path="settings" element={<OrganizationSettings />} />
                    <Route path="integrations/slack" element={<SlackIntegration />} />
                    <Route path="integrations/github" element={<GitHubIntegration />} />
                    <Route path="integrations/teams" element={<TeamsIntegration />} />
                    <Route path="integrations/mcp" element={<McpIntegration />} />
                    {/* Legacy route redirects */}
                    <Route path="slack" element={<Navigate to="/integrations/slack" replace />} />

                    {/* Custom Metrics */}
                    <Route path="metrics" element={<CustomMetrics />} />
                    <Route path="metrics/new" element={<MetricBuilder />} />
                    <Route path="metrics/:id/edit" element={<MetricBuilder />} />

                    {/* QA & Testing */}
                    <Route path="qa/test-cases" element={<TestCasesPage />} />
                    <Route path="qa/test-cases/new" element={<TestCaseFormPage />} />
                    <Route path="qa/test-cases/generate" element={<AITestGeneratePage />} />
                    <Route path="rd/wise-architecture" element={<WiseArchitecturePage />} />
                    <Route path="rd/advice-history" element={<AdviceHistoryPage />} />
                    <Route path="qa/test-cases/:id" element={<TestCaseDetailPage />} />
                    <Route path="qa/test-cases/:id/edit" element={<TestCaseFormPage />} />
                    <Route path="qa/test-cases/:id/run" element={<TestRunPage />} />
                    <Route path="qa/bug-reports" element={<BugReportsPage />} />
                    <Route path="pitches/:pitchId/test" element={<PitchTestPage />} />
                    <Route path="cycles/:cycleId/qa-dashboard" element={<CycleQADashboardPage />} />

                    {/* Roadmap & Planning */}
                    <Route path="roadmap" element={<RoadmapPage />} />
                    <Route path="initiatives" element={<InitiativeListPage />} />
                    <Route path="initiatives/new" element={<InitiativeFormPage />} />
                    <Route path="initiatives/:id" element={<InitiativeDetailPage />} />
                    <Route path="initiatives/:id/edit" element={<InitiativeFormPage />} />
                    <Route path="epics" element={<EpicListPage />} />
                    <Route path="epics/new" element={<EpicFormPage />} />
                    <Route path="epics/:id" element={<EpicDetailPage />} />
                    <Route path="epics/:id/edit" element={<EpicFormPage />} />
                    <Route path="releases-management" element={<ReleaseListPage />} />
                    <Route path="releases-management/new" element={<ReleaseFormPage />} />
                    <Route path="releases-management/:id" element={<ReleaseDetailPage />} />
                    <Route path="releases-management/:id/edit" element={<ReleaseFormPage />} />

                    {/* Help & Guides */}
                    <Route path="help" element={<HelpGuides />} />
                    <Route path="help/getting-started" element={<GettingStartedGuide />} />
                    <Route path="help/hill-charts" element={<HillChartsGuide />} />
                    <Route path="help/betting-meeting" element={<BettingMeetingGuide />} />
                    <Route path="help/ai-risk-advisor" element={<AIRiskAdvisorGuide />} />
                    <Route path="help/cycle-setup" element={<CycleSetupGuide />} />
                    <Route path="help/project-types" element={<ProjectTypesGuide />} />
                    <Route path="help/qa-testing" element={<QATestingGuide />} />
                    <Route path="help/retrospectives" element={<RetrospectivesGuide />} />
                    <Route path="help/circuit-breaker" element={<CircuitBreakerGuide />} />
                    <Route path="help/cooldown-activities" element={<CooldownActivitiesGuide />} />
                    <Route path="help/reports" element={<ReportsGuide />} />
                    <Route path="help/wise-architecture" element={<WiseArchitectureGuide />} />


                    {/* Catch-all for unmatched routes within protected area */}
                    <Route path="*" element={<Navigate to="/dashboard" replace />} />
                  </Routes>
                </Layout>
              </TourProvider>
            </ProjectProvider>
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;
