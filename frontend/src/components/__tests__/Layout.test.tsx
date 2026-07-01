import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { TooltipProvider } from '@/components/ui/tooltip';
import Layout from '../Layout';
import packageJson from '../../../package.json';

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
  QAFloatingButton: () => <div data-testid="qa-button">QA Button</div>,
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
});
