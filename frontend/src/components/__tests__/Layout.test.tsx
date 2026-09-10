import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter, MemoryRouter } from 'react-router-dom';
import { TooltipProvider } from '@/components/ui/tooltip';
import Layout from '../Layout';
import packageJson from '../../../package.json';
import { PROJECT_TYPE_CAPABILITIES } from '../../config/projectTypeCapabilities';

// Mock all context providers
vi.mock('../../contexts', () => ({
  useAuth: () => ({
    user: { username: 'testuser', role: 'ADMIN' },
    logout: vi.fn(),
  }),
  useTour: () => ({
    startTour: vi.fn(),
    hasCompletedTour: true,
  }),
  useTheme: () => ({
    actualMode: 'light',
    toggleTheme: vi.fn(),
  }),
  useProject: () => ({
    isKanbanProject: false,
    isAllProjectsSelected: false,
    isScrumProject: false,
    capabilities: PROJECT_TYPE_CAPABILITIES.SHAPE_UP,
  }),
}));

vi.mock('../../hooks/usePermission', () => ({
  usePermission: () => ({
    hasPermissionSync: () => false,
    hasPermission: () => Promise.resolve(false),
  }),
}));

// Mock child components
vi.mock('../ProjectSelector', () => ({
  default: () => <div data-testid="project-selector">Project Selector</div>,
}));

vi.mock('../Breadcrumbs', () => ({
  default: () => <div data-testid="breadcrumbs">Breadcrumbs</div>,
}));

vi.mock('../WelcomeTourDialog', () => ({
  default: () => <div data-testid="welcome-tour">Welcome Tour</div>,
}));

vi.mock('../QAFloatingButton', () => ({
  QAFloatingButton: ({ contextType }: { contextType: string }) => (
    <div data-testid="qa-button" data-context-type={contextType}>QA Button</div>
  ),
}));

vi.mock('../NotificationCenter', () => ({
  default: () => <div data-testid="notification-center">Notifications</div>,
}));

vi.mock('../DashboardSwitcher', () => ({
  default: () => <div data-testid="dashboard-switcher">Dashboard Switcher</div>,
}));

vi.mock('../LanguageSelector', () => ({
  default: () => <div data-testid="language-selector">Language Selector</div>,
}));

vi.mock('../AirGappedBadge', () => ({
  AirGappedBadge: () => null,
}));

vi.mock('../LicenseBanner', () => ({
  LicenseBanner: () => null,
}));

vi.mock('../RouteProgressProvider', () => ({
  RouteProgressProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

describe('Layout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should display version from package.json', () => {
    render(
      <BrowserRouter>
        <TooltipProvider>
          <Layout>
            <div>Test Content</div>
          </Layout>
        </TooltipProvider>
      </BrowserRouter>
    );

    // Check that the version from package.json is displayed
    const versionText = screen.getByText(`v${packageJson.version}`);
    expect(versionText).toBeInTheDocument();
  });

  it('should display correct version format', () => {
    render(
      <BrowserRouter>
        <TooltipProvider>
          <Layout>
            <div>Test Content</div>
          </Layout>
        </TooltipProvider>
      </BrowserRouter>
    );

    // Verify version matches the expected pattern (v + semantic versioning)
    const versionText = screen.getByText(`v${packageJson.version}`);
    expect(versionText.textContent).toMatch(/^v\d+\.\d+\.\d+$/);
  });

  it('should render children content', () => {
    render(
      <BrowserRouter>
        <TooltipProvider>
          <Layout>
            <div>Test Content</div>
          </Layout>
        </TooltipProvider>
      </BrowserRouter>
    );

    expect(screen.getByText('Test Content')).toBeInTheDocument();
  });

  it('should render navigation components', () => {
    render(
      <BrowserRouter>
        <TooltipProvider>
          <Layout>
            <div>Test Content</div>
          </Layout>
        </TooltipProvider>
      </BrowserRouter>
    );

    expect(screen.getByTestId('project-selector')).toBeInTheDocument();
    expect(screen.getByTestId('breadcrumbs')).toBeInTheDocument();
  });

  describe('global Q&A floating button visibility', () => {
    // On these routes, the page itself renders an entity-scoped QAFloatingButton
    // (contextType="cycle"/"pitch") at the same fixed position — the global
    // "knowledge"-scoped one here must be suppressed or it silently occludes and
    // intercepts every click meant for the page-specific one (see Layout.tsx's
    // isEntityScopedQaRoute comment for the full story).
    it.each([
      ['/cycles/8', 'a Cycle/Sprint Detail page'],
      ['/pitches/42', 'a Pitch Detail page'],
      ['/sprint-planning', 'Sprint Planning'],
    ])('hides the global knowledge Q&A button on %s (%s)', (path) => {
      render(
        <MemoryRouter initialEntries={[path]}>
          <TooltipProvider>
            <Layout>
              <div>Test Content</div>
            </Layout>
          </TooltipProvider>
        </MemoryRouter>
      );

      expect(screen.queryByTestId('qa-button')).not.toBeInTheDocument();
    });

    it.each([
      ['/dashboard', 'Dashboard'],
      ['/cycles', 'the Sprints/Cycles list'],
      ['/pitches', 'the Pitch Board'],
      ['/backlog', 'Backlog'],
      ['/cycles/8/edit', 'a Cycle edit form (not the detail page)'],
    ])('shows the global knowledge Q&A button on %s (%s)', (path) => {
      render(
        <MemoryRouter initialEntries={[path]}>
          <TooltipProvider>
            <Layout>
              <div>Test Content</div>
            </Layout>
          </TooltipProvider>
        </MemoryRouter>
      );

      const button = screen.getByTestId('qa-button');
      expect(button).toBeInTheDocument();
      expect(button).toHaveAttribute('data-context-type', 'knowledge');
    });
  });
});
