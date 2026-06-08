import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate, useParams } from 'react-router-dom';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

// Static imports — always loaded immediately (structural / auth components)
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import { useToast, setToastHandler, ProjectProvider, TourProvider } from './contexts';
import { isRTLLanguage } from './i18n';

// ── Page-level code splitting ─────────────────────────────────────────────────
// Each lazy() call becomes its own JS chunk loaded on first navigation.
// Public pages
const Landing = lazy(() => import('./pages/Landing'));
const Login = lazy(() => import('./pages/Login'));
const SsoCallbackPage = lazy(() => import('./pages/SsoCallbackPage'));
const ReleaseNotes = lazy(() => import('./pages/ReleaseNotes'));
const CompetitorsComparison = lazy(() => import('./pages/CompetitorsComparison'));
const PublicRoadmap = lazy(() => import('./pages/PublicRoadmap'));
const Blog = lazy(() => import('./pages/Blog'));
const BlogPost = lazy(() => import('./pages/BlogPost'));

// Core app pages
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Projects = lazy(() => import('./pages/Projects'));
const ProjectDetail = lazy(() => import('./pages/ProjectDetail'));

// Cycle pages
const CycleList = lazy(() => import('./pages/CycleList'));
const CycleDetail = lazy(() => import('./pages/CycleDetail'));
const CycleForm = lazy(() => import('./pages/CycleForm'));
const CycleHillChart = lazy(() =>
  import('./pages/CycleHillChart').then((m) => ({ default: m.CycleHillChart }))
);
const CircuitBreakerMonitor = lazy(() => import('./pages/CircuitBreakerMonitor'));
const CooldownActivities = lazy(() => import('./pages/CooldownActivities'));

// Pitch / Shape Up pages
const PitchBoard = lazy(() => import('./pages/PitchBoard'));
const PitchDetail = lazy(() => import('./pages/PitchDetail'));
const PitchHillChart = lazy(() =>
  import('./pages/PitchHillChart').then((m) => ({ default: m.PitchHillChart }))
);
const BettingTable = lazy(() => import('./pages/BettingTable'));
const PitchComparisonView = lazy(() => import('./pages/PitchComparisonView'));

// Backlog & Tasks
const BacklogPage = lazy(() => import('./pages/BacklogPage'));
const TaskDetailPage = lazy(() => import('./pages/TaskDetailPage'));

// Scrum
const SprintPlanningPage = lazy(() => import('./pages/SprintPlanningPage'));

// Retros & Health
const RetroList = lazy(() => import('./pages/RetroList'));
const RetroBoard = lazy(() => import('./pages/RetroBoard'));
const HealthOverview = lazy(() => import('./pages/HealthOverview'));

// Reports & Dashboards
const Reports = lazy(() => import('./pages/Reports'));
const DashboardManager = lazy(() => import('./pages/DashboardManager'));
const DashboardView = lazy(() => import('./pages/DashboardView'));
const CustomMetrics = lazy(() => import('./pages/CustomMetrics'));
const MetricBuilder = lazy(() => import('./pages/MetricBuilder'));

// Time tracking & Meetings
const WorkLogsPage = lazy(() => import('./pages/WorkLogsPage'));
const MeetingList = lazy(() => import('./pages/MeetingList'));

// Organisation
const People = lazy(() => import('./pages/People'));
const Teams = lazy(() => import('./pages/Teams'));
const Profile = lazy(() => import('./pages/Profile'));

// Admin
const UserManagement = lazy(() => import('./pages/UserManagement'));
const PermissionManagement = lazy(() => import('./pages/PermissionManagement'));
const OrganizationSettings = lazy(() => import('./pages/OrganizationSettings'));

// Integrations
const SlackIntegration = lazy(() => import('./pages/SlackIntegration'));
const GitHubIntegration = lazy(() => import('./pages/integrations/GitHubIntegration'));
const TeamsIntegration = lazy(() => import('./pages/integrations/TeamsIntegration'));
const McpIntegration = lazy(() => import('./pages/integrations/McpIntegration'));
const ApiKeysPage = lazy(() => import('./pages/integrations/ApiKeysPage'));
const InboundWebhooksIntegration = lazy(
  () => import('./pages/integrations/InboundWebhooksIntegration')
);

// QA & Testing
const TestCasesPage = lazy(() => import('./pages/TestCasesPage'));
const TestCaseFormPage = lazy(() => import('./pages/TestCaseFormPage'));
const TestCaseDetailPage = lazy(() => import('./pages/TestCaseDetailPage'));
const TestRunPage = lazy(() => import('./pages/TestRunPage'));
const AITestGeneratePage = lazy(() => import('./pages/AITestGeneratePage'));
const BugReportsPage = lazy(() => import('./pages/BugReportsPage'));
const BugReportDetailPage = lazy(() => import('./pages/BugReportDetailPage'));
const PitchTestPage = lazy(() => import('./pages/PitchTestPage'));
const CycleQADashboardPage = lazy(() => import('./pages/CycleQADashboardPage'));

// AI / R&D
const WiseArchitecturePage = lazy(() => import('./pages/WiseArchitecturePage'));
const AdviceHistoryPage = lazy(() => import('./pages/AdviceHistoryPage'));

// Roadmap & Planning
const RoadmapPage = lazy(() => import('./pages/RoadmapPage'));
const InitiativeListPage = lazy(() => import('./pages/InitiativeListPage'));
const InitiativeDetailPage = lazy(() => import('./pages/InitiativeDetailPage'));
const InitiativeFormPage = lazy(() => import('./pages/InitiativeFormPage'));
const EpicListPage = lazy(() => import('./pages/EpicListPage'));
const EpicDetailPage = lazy(() => import('./pages/EpicDetailPage'));
const EpicFormPage = lazy(() => import('./pages/EpicFormPage'));
const ReleaseListPage = lazy(() => import('./pages/ReleaseListPage'));
const ReleaseDetailPage = lazy(() => import('./pages/ReleaseDetailPage'));
const ReleaseFormPage = lazy(() => import('./pages/ReleaseFormPage'));

// Import
const ImportPage = lazy(() => import('./pages/ImportPage'));

// Help & Guides
const HelpGuides = lazy(() => import('./pages/HelpGuides'));
const GettingStartedGuide = lazy(() => import('./pages/guides/GettingStartedGuide'));
const HillChartsGuide = lazy(() => import('./pages/guides/HillChartsGuide'));
const BettingMeetingGuide = lazy(() => import('./pages/guides/BettingMeetingGuide'));
const AIRiskAdvisorGuide = lazy(() => import('./pages/guides/AIRiskAdvisorGuide'));
const CycleSetupGuide = lazy(() => import('./pages/guides/CycleSetupGuide'));
const ProjectTypesGuide = lazy(() => import('./pages/guides/ProjectTypesGuide'));
const QATestingGuide = lazy(() => import('./pages/guides/QATestingGuide'));
const RetrospectivesGuide = lazy(() => import('./pages/guides/RetrospectivesGuide'));
const CircuitBreakerGuide = lazy(() => import('./pages/guides/CircuitBreakerGuide'));
const CooldownActivitiesGuide = lazy(() => import('./pages/guides/CooldownActivitiesGuide'));
const ReportsGuide = lazy(() => import('./pages/guides/ReportsGuide'));
const WiseArchitectureGuide = lazy(() => import('./pages/guides/WiseArchitectureGuide'));
const ExportDataGuide = lazy(() => import('./pages/guides/ExportDataGuide'));
const WebhooksGuide = lazy(() => import('./pages/guides/WebhooksGuide'));
const PublicApiGuide = lazy(() => import('./pages/guides/PublicApiGuide'));
const McpServerGuide = lazy(() => import('./pages/guides/McpServerGuide'));
const IdeaToRoadmapGuide = lazy(() => import('./pages/guides/IdeaToRoadmapGuide'));
const ImportGuide = lazy(() => import('./pages/guides/ImportGuide'));
const ScrumModeGuide = lazy(() => import('./pages/guides/ScrumModeGuide'));
const MigrationGuide = lazy(() => import('./pages/guides/MigrationGuide'));

// ── Suspense fallback ─────────────────────────────────────────────────────────
function PageLoader() {
  return (
    <div className="flex h-screen w-full items-center justify-center">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
    </div>
  );
}

/** Redirect /tasks/:taskId → /backlog/:taskId (legacy notification actionUrls) */
function TaskToBacklogRedirect() {
  const { taskId } = useParams<{ taskId: string }>();
  return <Navigate to={`/backlog/${taskId}`} replace />;
}

/** Redirect /bugs/:bugId → /qa/bug-reports/:bugId (legacy notification actionUrls) */
function BugToBugReportRedirect() {
  const { bugId } = useParams<{ bugId: string }>();
  return <Navigate to={`/qa/bug-reports/${bugId}`} replace />;
}

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
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/welcome" element={<Navigate to="/" replace />} />
        <Route path="/compare" element={<CompetitorsComparison />} />
        <Route path="/releases" element={<ReleaseNotes />} />
        <Route path="/public-roadmap" element={<PublicRoadmap />} />
        <Route path="/blog" element={<Blog />} />
        <Route path="/blog/:slug" element={<BlogPost />} />
        <Route path="/login" element={<Login />} />
        {/* SSO callback — processes token from IdP redirect, no auth required */}
        <Route path="/sso-callback" element={<SsoCallbackPage />} />
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
                      <Route
                        path="cycles/:cycleId/circuit-breaker"
                        element={<CircuitBreakerMonitor />}
                      />
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
                      <Route path="dashboards" element={<DashboardManager />} />
                      <Route path="dashboards/:id" element={<DashboardView />} />
                      <Route path="reports" element={<Reports />} />
                      {/* Legacy route redirects */}
                      <Route
                        path="reports/cycle-reports"
                        element={<Navigate to="/reports" replace />}
                      />

                      {/* Scrum */}
                      <Route path="sprint-planning" element={<SprintPlanningPage />} />

                      {/* Backlog */}
                      <Route path="backlog" element={<BacklogPage />} />
                      <Route path="backlog/:taskId" element={<TaskDetailPage />} />
                      {/* Legacy route redirects */}
                      <Route
                        path="backlog/tasks"
                        element={<Navigate to="/backlog?category=PITCH_SCOPE" replace />}
                      />
                      <Route
                        path="backlog/debt"
                        element={<Navigate to="/backlog?category=DEBT_IMPROVEMENT" replace />}
                      />
                      <Route path="tasks" element={<Navigate to="/backlog" replace />} />
                      <Route path="tasks/:taskId" element={<TaskToBacklogRedirect />} />
                      <Route path="bugs/:bugId" element={<BugToBugReportRedirect />} />

                      {/* Time Tracking */}
                      <Route path="time/logs" element={<WorkLogsPage />} />
                      {/* Legacy route redirects */}
                      <Route path="worklogs" element={<Navigate to="/time/logs" replace />} />
                      <Route path="my-worklogs" element={<Navigate to="/time/logs" replace />} />

                      {/* Meetings */}
                      <Route path="meetings" element={<MeetingList />} />

                      {/* Organisation */}
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
                      <Route path="integrations/api-keys" element={<ApiKeysPage />} />
                      <Route
                        path="integrations/inbound-webhooks"
                        element={<InboundWebhooksIntegration />}
                      />
                      {/* Legacy route redirects */}
                      <Route
                        path="slack"
                        element={<Navigate to="/integrations/slack" replace />}
                      />

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
                      <Route path="qa/bug-reports/:id" element={<BugReportDetailPage />} />
                      <Route path="pitches/:pitchId/test" element={<PitchTestPage />} />
                      <Route
                        path="cycles/:cycleId/qa-dashboard"
                        element={<CycleQADashboardPage />}
                      />

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

                      {/* Import */}
                      <Route path="import" element={<ImportPage />} />

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
                      <Route
                        path="help/cooldown-activities"
                        element={<CooldownActivitiesGuide />}
                      />
                      <Route path="help/reports" element={<ReportsGuide />} />
                      <Route path="help/wise-architecture" element={<WiseArchitectureGuide />} />
                      <Route path="help/export-data" element={<ExportDataGuide />} />
                      <Route path="help/webhooks" element={<WebhooksGuide />} />
                      <Route path="help/public-api" element={<PublicApiGuide />} />
                      <Route path="help/mcp-server" element={<McpServerGuide />} />
                      <Route path="help/idea-to-roadmap" element={<IdeaToRoadmapGuide />} />
                      <Route path="help/import" element={<ImportGuide />} />
                      <Route path="help/scrum-mode" element={<ScrumModeGuide />} />
                      <Route path="help/migration" element={<MigrationGuide />} />

                      {/* Catch-all for unmatched routes within protected area */}
                      <Route path="*" element={<Navigate to="/dashboard" replace />} />
                      {/*
                       * S37a deep-link routing review (2026-06-06):
                       * All detail routes (/pitches/:id, /backlog/:taskId, /cycles/:cycleId, etc.)
                       * are registered as flat top-level paths — they do NOT depend on a parent
                       * route loader. ProjectContext is bootstrap-loaded from localStorage on mount
                       * and re-hydrated from the API, so a fresh deep-link load finds the stored
                       * project within the first render. Pages that require a project (RoadmapPage,
                       * BacklogPage) guard with <ProjectRequiredDialog> rather than redirecting,
                       * keeping the URL stable. No routing changes required.
                       */}
                    </Routes>
                  </Layout>
                </TourProvider>
              </ProjectProvider>
            </ProtectedRoute>
          }
        />
      </Routes>
    </Suspense>
  );
}

export default App;
