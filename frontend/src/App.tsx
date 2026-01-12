import { Routes, Route, Navigate } from 'react-router-dom';
import { useEffect } from 'react';
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
import Profile from './pages/Profile';
import People from './pages/People';
import UserManagement from './pages/UserManagement';
import OrganizationSettings from './pages/OrganizationSettings';
import Projects from './pages/Projects';
import HealthOverview from './pages/HealthOverview';
import BacklogPage from './pages/BacklogPage';
import TaskDetailPage from './pages/TaskDetailPage';
import BettingTable from './pages/BettingTable';
import { PitchHillChart } from './pages/PitchHillChart';
import { CycleHillChart } from './pages/CycleHillChart';
import TestCasesPage from './pages/TestCasesPage';
import TestCaseFormPage from './pages/TestCaseFormPage';
import TestCaseDetailPage from './pages/TestCaseDetailPage';
import TestRunPage from './pages/TestRunPage';
import AITestGeneratePage from './pages/AITestGeneratePage';
import BugReportsPage from './pages/BugReportsPage';
import PitchTestPage from './pages/PitchTestPage';
import CycleQADashboardPage from './pages/CycleQADashboardPage';
import RetroList from './pages/RetroList';
import RetroBoard from './pages/RetroBoard';
import CustomMetrics from './pages/CustomMetrics';
import MetricBuilder from './pages/MetricBuilder';
import DashboardManager from './pages/DashboardManager';
import DashboardView from './pages/DashboardView';
import { useToast, setToastHandler, ProjectProvider, TourProvider } from './contexts';

function App() {
  const { showToast } = useToast();

  // Connect toast handler for use in api interceptors
  useEffect(() => {
    setToastHandler(showToast);
  }, [showToast]);

  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/welcome" element={<Navigate to="/" replace />} />
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
                    <Route path="cycles" element={<CycleList />} />
                    <Route path="cycles/new" element={<CycleForm />} />
                    <Route path="cycles/:id" element={<CycleDetail />} />
                    <Route path="cycles/:id/edit" element={<CycleForm />} />
                    <Route path="cycles/:cycleId/hill-chart" element={<CycleHillChart />} />
                    
                    {/* Cycle Workspace */}
                    <Route path="pitches" element={<PitchBoard />} />
                    <Route path="pitches/:pitchId/hill-chart" element={<PitchHillChart />} />
                    <Route path="pitches/:id" element={<PitchDetail />} />
                    <Route path="betting" element={<BettingTable />} />
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
                    <Route path="settings" element={<OrganizationSettings />} />
                    
                    {/* Custom Metrics */}
                    <Route path="metrics" element={<CustomMetrics />} />
                    <Route path="metrics/new" element={<MetricBuilder />} />
                    <Route path="metrics/:id/edit" element={<MetricBuilder />} />
                    
                    {/* QA & Testing */}
                    <Route path="qa/test-cases" element={<TestCasesPage />} />
                    <Route path="qa/test-cases/new" element={<TestCaseFormPage />} />
                    <Route path="qa/test-cases/generate" element={<AITestGeneratePage />} />
                    <Route path="qa/test-cases/:id" element={<TestCaseDetailPage />} />
                    <Route path="qa/test-cases/:id/edit" element={<TestCaseFormPage />} />
                    <Route path="qa/test-cases/:id/run" element={<TestRunPage />} />
                    <Route path="qa/bug-reports" element={<BugReportsPage />} />
                    <Route path="pitches/:pitchId/test" element={<PitchTestPage />} />
                    <Route path="cycles/:cycleId/qa-dashboard" element={<CycleQADashboardPage />} />
                    
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
