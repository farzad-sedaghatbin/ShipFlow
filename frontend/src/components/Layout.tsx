import { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  LayoutDashboard,
  Repeat,
  FileText,
  Clock,
  Users,
  Calendar,
  BarChart3,
  LogOut,
  User,
  Users2,
  Shield,
  ShieldCheck,
  Settings,
  Folder,
  Activity,
  HelpCircle,
  Dices,
  Bug,
  FlaskConical,
  Brain,
  Menu,
  ChevronDown,
  ChevronRight,
  Sun,
  Moon,
  ListTodo,
  Target,
  MessageSquare,
  Github,
  Plug,
  BookOpen,
  Beaker,
  Map,
  Layers,
  PackageCheck,
  ArrowDownToLine,
  Search,
  Workflow,
  KeyRound,
  Zap,
  BookOpenText,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth, useTour, useTheme } from '../contexts';
import { usePermission } from '../hooks/usePermission';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import {
  Sheet,
  SheetContent,
} from '@/components/ui/sheet';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible';
import ProjectSelector from './ProjectSelector';
import Breadcrumbs from './Breadcrumbs';
import WelcomeTourDialog from './WelcomeTourDialog';
import { QAFloatingButton } from './QAFloatingButton';
import NotificationCenter from './NotificationCenter';
import DashboardSwitcher from './DashboardSwitcher';
import LanguageSelector from './LanguageSelector';
import { useProject } from '../contexts';
import { RouteProgressProvider } from './RouteProgressProvider';
import MobileBottomNav from './MobileBottomNav';
import GlobalSearchCommand from './GlobalSearchCommand';
import { KeyboardShortcutSheet } from './KeyboardShortcutSheet';
import { useKeyboardShortcuts } from '../hooks/useKeyboardShortcuts';
import packageJson from '../../package.json';

const ROUTE_TITLES: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/projects': 'Projects',
  '/cycles': 'Cycles',
  '/pitches': 'Pitches',
  '/betting': 'Betting Table',
  '/health': 'Health Overview',
  '/retros': 'Retrospectives',
  '/dashboards': 'Custom Dashboards',
  '/reports': 'Reports',
  '/backlog': 'Backlog',
  '/people': 'People',
  '/teams': 'Teams',
  '/meetings': 'Meetings',
  '/time': 'Work Logs',
  '/settings': 'Settings',
  '/org-settings': 'Organization Settings',
  '/qa': 'Quality',
  '/rd': 'R&D',
  '/import': 'Import',
  '/roadmap': 'Roadmap',
  '/sprint-planning': 'Sprint Planning',
  '/ai-features': 'AI Features',
  '/wiki': 'Wiki',
};

function getPageTitle(pathname: string): string {
  // exact match first
  if (ROUTE_TITLES[pathname]) return ROUTE_TITLES[pathname];
  // prefix match: /cycles/123/edit → /cycles → 'Cycles'
  const segment = '/' + pathname.replace(/^\//, '').split('/')[0];
  return ROUTE_TITLES[segment] || 'ShipFlow';
}

interface LayoutProps {
  children: React.ReactNode;
}

interface NavItemConfig {
  textKey: string;
  icon: React.ElementType;
  path: string;
  tourId?: string;
}

// Main navigation items for Shape Up mode (with Cycles)
const shapeUpMainNavItems: NavItemConfig[] = [
  { textKey: 'nav.dashboard', icon: LayoutDashboard, path: '/dashboard', tourId: 'dashboard-menu' },
  { textKey: 'nav.projects', icon: Folder, path: '/projects', tourId: 'projects-menu' },
  { textKey: 'nav.cycles', icon: Repeat, path: '/cycles', tourId: 'cycles-menu' },
];

// Main navigation items for Scrum mode (Sprints — same /cycles path, different label)
const scrumMainNavItems: NavItemConfig[] = [
  { textKey: 'nav.dashboard', icon: LayoutDashboard, path: '/dashboard', tourId: 'dashboard-menu' },
  { textKey: 'nav.projects', icon: Folder, path: '/projects', tourId: 'projects-menu' },
  { textKey: 'nav.sprints', icon: Repeat, path: '/cycles', tourId: 'cycles-menu' },
];

// Main navigation items for Kanban mode (no Cycles)
const kanbanMainNavItems: NavItemConfig[] = [
  { textKey: 'nav.dashboard', icon: LayoutDashboard, path: '/dashboard', tourId: 'dashboard-menu' },
  { textKey: 'nav.projects', icon: Folder, path: '/projects', tourId: 'projects-menu' },
];

// Cycle Workspace - contextual items when viewing cycle content (Shape Up only)
const cycleWorkspaceItems: NavItemConfig[] = [
  { textKey: 'nav.pitchBoard', icon: FileText, path: '/pitches', tourId: 'pitches-menu' },
  { textKey: 'nav.betting', icon: Dices, path: '/betting', tourId: 'betting-menu' },
  { textKey: 'nav.health', icon: Activity, path: '/health', tourId: 'health-menu' },
  { textKey: 'nav.retrospectives', icon: Brain, path: '/retros', tourId: 'retros-menu' },
  { textKey: 'nav.dashboards', icon: LayoutDashboard, path: '/dashboards', tourId: 'dashboards-menu' },
  { textKey: 'nav.reports', icon: BarChart3, path: '/reports', tourId: 'reports-menu' },
];

// Sprint Workspace - Cycle Workspace without Pitch Board and Betting Table (Scrum only)
const scrumWorkspaceItems: NavItemConfig[] = cycleWorkspaceItems.filter(
  (item) => item.path !== '/pitches' && item.path !== '/betting'
);

// People & Teams
const peopleItems: NavItemConfig[] = [
  { textKey: 'nav.people', icon: Users2, path: '/people', tourId: 'people-menu' },
  { textKey: 'nav.teams', icon: Users, path: '/teams', tourId: 'teams-menu' },
];

// Quality section
const qualityItems: NavItemConfig[] = [
  { textKey: 'nav.testCases', icon: FlaskConical, path: '/qa/test-cases', tourId: 'qa-test-cases-menu' },
  { textKey: 'nav.bugReports', icon: Bug, path: '/qa/bug-reports', tourId: 'qa-bug-reports-menu' },
];

// R&D section
const rdItems: NavItemConfig[] = [
  { textKey: 'nav.wiseArchitecture', icon: Beaker, path: '/rd/wise-architecture', tourId: 'rd-wise-architecture-menu' },
];

// Roadmap & Planning section
const roadmapItems: NavItemConfig[] = [
  { textKey: 'nav.roadmap', icon: Map, path: '/roadmap', tourId: 'roadmap-menu' },
  { textKey: 'nav.initiatives', icon: Target, path: '/initiatives', tourId: 'initiatives-menu' },
  { textKey: 'nav.epics', icon: Layers, path: '/epics', tourId: 'epics-menu' },
  { textKey: 'nav.releases', icon: PackageCheck, path: '/releases-management', tourId: 'releases-menu' },
];

// Workflow Automations
const automationsItems: NavItemConfig[] = [
  { textKey: 'nav.automations', icon: Zap, path: '/automations', tourId: 'automations-menu' },
];

// Meetings (accessible from cycle context)
const meetingsItems: NavItemConfig[] = [
  { textKey: 'nav.meetings', icon: Calendar, path: '/meetings', tourId: 'meetings-menu' },
];

// Admin section - User & Access items
const userAccessItems: NavItemConfig[] = [
  { textKey: 'nav.userManagement', icon: Shield, path: '/users' },
  { textKey: 'nav.permissions', icon: ShieldCheck, path: '/permissions' },
];

// Integrations section
// `memberIntegrationItems` are visible to every authenticated user — anything
// here must not require admin permissions on the backend.
const memberIntegrationItems: NavItemConfig[] = [
  { textKey: 'integrations.apiKeys', icon: KeyRound, path: '/integrations/api-keys' },
];
const adminIntegrationItems: NavItemConfig[] = [
  { textKey: 'integrations.slack', icon: MessageSquare, path: '/integrations/slack' },
  { textKey: 'integrations.github', icon: Github, path: '/integrations/github' },
  { textKey: 'integrations.teams', icon: Users2, path: '/integrations/teams' },
  { textKey: 'integrations.mcp', icon: Plug, path: '/integrations/mcp' },
  { textKey: 'integrations.inboundWebhooks', icon: ArrowDownToLine, path: '/integrations/inbound-webhooks' },
];
const integrationItems: NavItemConfig[] = [...memberIntegrationItems, ...adminIntegrationItems];

function NavItem({
  item,
  isActive,
  onClick,
  indent = false,
}: {
  item: NavItemConfig;
  isActive: boolean;
  onClick?: () => void;
  indent?: boolean;
}) {
  const { t } = useTranslation();
  const Icon = item.icon;
  return (
    <Link
      to={item.path}
      onClick={onClick}
      data-tour={item.tourId}
      className={cn(
        "flex items-center gap-3 rounded-md px-3 py-3 text-sm font-medium transition-colors touch-manipulation min-h-[44px]",
        "rtl:flex-row-reverse",
        indent && "ms-4",
        isActive
          ? "bg-primary text-primary-foreground"
          : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
      )}
    >
      <Icon className="h-5 w-5 flex-shrink-0" />
      <span className="flex-1">{t(item.textKey)}</span>
    </Link>
  );
}

function NavGroup({
  titleKey,
  icon: Icon,
  items,
  currentPath,
  onItemClick,
  defaultOpen = false,
}: {
  titleKey: string;
  icon: React.ElementType;
  items: NavItemConfig[];
  currentPath: string;
  onItemClick?: () => void;
  defaultOpen?: boolean;
}) {
  const { t } = useTranslation();
  const hasActiveItem = items.some(item => currentPath === item.path || currentPath.startsWith(item.path + '/'));
  const [isOpen, setIsOpen] = useState(defaultOpen || hasActiveItem);

  return (
    <Collapsible open={isOpen} onOpenChange={setIsOpen}>
      <CollapsibleTrigger className="flex w-full items-center gap-3 rounded-md px-3 py-3 text-sm font-medium text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors touch-manipulation min-h-[44px] rtl:flex-row-reverse">
        <Icon className="h-5 w-5 flex-shrink-0" />
        <span className="flex-1 text-start">{t(titleKey)}</span>
        <ChevronRight className={cn("h-5 w-5 flex-shrink-0 transition-transform rtl:rotate-180", isOpen && "rotate-90")} />
      </CollapsibleTrigger>
      <CollapsibleContent className="mt-1 space-y-1">
        {items.map((item) => (
          <NavItem
            key={item.path}
            item={item}
            isActive={currentPath === item.path || currentPath.startsWith(item.path + '/')}
            onClick={onItemClick}
            indent
          />
        ))}
      </CollapsibleContent>
    </Collapsible>
  );
}

function SectionHeader({ textKey }: { textKey: string }) {
  const { t } = useTranslation();
  return (
    <div className="px-3 py-2 mt-4">
      <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
        {t(textKey)}
      </span>
    </div>
  );
}

function SidebarContent({ onItemClick }: { onItemClick?: () => void }) {
  const location = useLocation();
  const { isKanbanProject, isAllProjectsSelected, isScrumProject } = useProject();
  const { hasPermissionSync, hasPermission } = usePermission();
  const { startTour, hasCompletedTour } = useTour();
  const { actualMode, toggleTheme } = useTheme();
  const { t } = useTranslation();
  const currentPath = location.pathname;

  // Preload essential admin permissions for menu visibility
  useEffect(() => {
    // Preload SYSTEM MANAGE permission to ensure admin menus show immediately
    hasPermission('SYSTEM', 'MANAGE').catch(() => {
      // Ignore errors, permission will be cached as false
    });
  }, [hasPermission]); // Include hasPermission since it's now stable

  // Check if we're in a cycle context (viewing pitches, betting, health, retros, reports)
  const isCycleContext = ['/pitches', '/betting', '/health', '/retros', '/reports'].some(
    path => currentPath.startsWith(path)
  ) || /\/cycles\/\d+/.test(currentPath);

  // Determine which nav items to show based on project type
  // When "All Projects" is selected, show Shape Up navigation (full features)
  const showKanbanFeatures = isKanbanProject && !isAllProjectsSelected;
  const mainNavItems = showKanbanFeatures
    ? kanbanMainNavItems
    : isScrumProject
    ? scrumMainNavItems
    : shapeUpMainNavItems;
  const showCycleWorkspace = !isKanbanProject || isAllProjectsSelected;

  return (
    <div className="flex h-full flex-col" data-tour="sidebar">
      {/* Logo */}
      <div className="flex h-14 items-center border-b border-sidebar-border px-4">
        <Link to="/dashboard" className="flex items-center gap-2">
          <img src="/icon.png" alt="ShipFlow" className="h-8 w-8 rounded-lg" />
          <span className="text-lg font-bold text-foreground">ShipFlow</span>
        </Link>
      </div>

      {/* Navigation */}
      <ScrollArea className="flex-1 px-3 py-4">
        {/* Dashboard Switcher - shown in sidebar on mobile */}
        <div className="lg:hidden mb-4 px-1">
          <DashboardSwitcher onDashboardChange={() => {}} />
        </div>

        {/* Mobile-only quick actions (hidden from header on small screens) */}
        <div className="sm:hidden mb-4 px-1 flex items-center gap-2">
          <Button
            variant="ghost"
            size="icon"
            onClick={startTour}
            className={cn(
              "h-9 w-9 touch-manipulation",
              !hasCompletedTour && "text-primary animate-pulse"
            )}
            aria-label={hasCompletedTour ? t('layout.restartTour') : t('layout.startTour')}
          >
            <HelpCircle className="h-5 w-5" />
          </Button>
          <LanguageSelector className="h-9 w-9 touch-manipulation" />
          <Button
            variant="ghost"
            size="icon"
            onClick={toggleTheme}
            className="h-9 w-9 touch-manipulation"
            aria-label={t('layout.toggleTheme')}
          >
            {actualMode === 'dark' ? (
              <Sun className="h-5 w-5" />
            ) : (
              <Moon className="h-5 w-5" />
            )}
          </Button>
        </div>
        <nav className="flex flex-col gap-1">
          {/* Overview Section */}
          <SectionHeader textKey="nav.sections.overview" />
          {mainNavItems.map((item) => (
            <NavItem
              key={item.path}
              item={item}
              isActive={currentPath === item.path}
              onClick={onItemClick}
            />
          ))}

          {/* Cycle/Sprint Workspace Section - Only for Shape Up/Scrum projects or All Projects */}
          {showCycleWorkspace && (
            <>
              <SectionHeader textKey={isScrumProject ? 'nav.sections.sprintWorkspace' : 'nav.sections.cycleWorkspace'} />
              <NavGroup
                titleKey={isScrumProject ? 'nav.groups.sprintTools' : 'nav.groups.cycleTools'}
                icon={Target}
                items={isScrumProject ? scrumWorkspaceItems : cycleWorkspaceItems}
                currentPath={currentPath}
                onItemClick={onItemClick}
                defaultOpen={isCycleContext}
              />
            </>
          )}

          {/* Work Management Section - Direct items, no collapsible groups */}
          <SectionHeader textKey="nav.sections.workManagement" />
          <NavItem
            item={{ textKey: 'nav.backlog', icon: ListTodo, path: '/backlog', tourId: 'backlog-menu' }}
            isActive={currentPath.startsWith('/backlog')}
            onClick={onItemClick}
          />
          {/* Sprint Planning — visible only for Scrum projects */}
          {isScrumProject && (
            <NavItem
              item={{ textKey: 'nav.sprintPlanning', icon: Workflow, path: '/sprint-planning' }}
              isActive={currentPath.startsWith('/sprint-planning')}
              onClick={onItemClick}
            />
          )}
          <NavItem
            item={{ textKey: 'nav.workLogs', icon: Clock, path: '/time/logs', tourId: 'worklogs-menu' }}
            isActive={currentPath.startsWith('/time')}
            onClick={onItemClick}
          />
          
          {/* Reports - For Kanban projects, show directly (not in Cycle Workspace) */}
          {showKanbanFeatures && (
            <NavItem
              item={{ textKey: 'nav.reports', icon: BarChart3, path: '/reports', tourId: 'reports-menu' }}
              isActive={currentPath.startsWith('/reports')}
              onClick={onItemClick}
            />
          )}

          {/* Knowledge Center */}
          <NavItem
            item={{ textKey: 'nav.knowledge', icon: BookOpen, path: '/knowledge', tourId: 'knowledge-link' }}
            isActive={currentPath.startsWith('/knowledge')}
            onClick={onItemClick}
          />

          {/* Wiki */}
          <NavItem
            item={{ textKey: 'nav.wiki', icon: BookOpenText, path: '/wiki', tourId: 'wiki-link' }}
            isActive={currentPath.startsWith('/wiki')}
            onClick={onItemClick}
          />

          {/* Meetings */}
          {meetingsItems.map((item) => (
            <NavItem
              key={item.path}
              item={item}
              isActive={currentPath === item.path}
              onClick={onItemClick}
            />
          ))}

          {/* Workflow Automations */}
          <SectionHeader textKey="nav.sections.automations" />
          {automationsItems.map((item) => (
            <NavItem
              key={item.path}
              item={item}
              isActive={currentPath === item.path}
              onClick={onItemClick}
            />
          ))}

          {/* Roadmap & Planning Section - moved up for daily relevance */}
          <SectionHeader textKey="nav.sections.roadmap" />
          <NavGroup
            titleKey="nav.groups.planning"
            icon={Map}
            items={roadmapItems}
            currentPath={currentPath}
            onItemClick={onItemClick}
          />

          {/* Quality Section */}
          <SectionHeader textKey="nav.sections.quality" />
          <NavGroup
            titleKey="nav.groups.qaTesting"
            icon={FlaskConical}
            items={qualityItems}
            currentPath={currentPath}
            onItemClick={onItemClick}
          />

          {/* R&D Section */}
          <SectionHeader textKey="nav.sections.rd" />
          <NavGroup
            titleKey="nav.groups.research"
            icon={Beaker}
            items={rdItems}
            currentPath={currentPath}
            onItemClick={onItemClick}
          />

          {/* Help & Guides Section */}
          <SectionHeader textKey="nav.sections.helpSupport" />
          <NavItem
            item={{ textKey: 'nav.helpGuides', icon: BookOpen, path: '/help', tourId: 'help-menu' }}
            isActive={currentPath.startsWith('/help')}
            onClick={onItemClick}
          />

          {/* Organization Section - moved down (less frequently used) */}
          <SectionHeader textKey="nav.sections.organization" />
          <NavGroup
            titleKey="nav.groups.people"
            icon={Users2}
            items={peopleItems}
            currentPath={currentPath}
            onItemClick={onItemClick}
          />
        </nav>

        {/* Member-visible integrations (API Keys etc.) — accessible to non-admins
            so they can mint personal keys without needing admin elevation. */}
        {!hasPermissionSync('SYSTEM', 'MANAGE') && (
          <nav className="flex flex-col gap-1">
            <SectionHeader textKey="nav.sections.integrations" />
            <NavGroup
              titleKey="nav.groups.integrations"
              icon={Plug}
              items={memberIntegrationItems}
              currentPath={currentPath}
              onItemClick={onItemClick}
            />
          </nav>
        )}

        {/* Admin Section */}
        {hasPermissionSync('SYSTEM', 'MANAGE') && (
          <>
            <SectionHeader textKey="nav.sections.administration" />
            <nav className="flex flex-col gap-1">
              <NavGroup
                titleKey="nav.groups.userAccess"
                icon={Shield}
                items={userAccessItems}
                currentPath={currentPath}
                onItemClick={onItemClick}
              />

              <NavItem
                item={{ textKey: 'nav.organizationSettings', icon: Settings, path: '/settings' }}
                isActive={currentPath === '/settings'}
                onClick={onItemClick}
              />

              <NavGroup
                titleKey="nav.groups.integrations"
                icon={Plug}
                items={integrationItems}
                currentPath={currentPath}
                onItemClick={onItemClick}
              />
            </nav>
          </>
        )}
      </ScrollArea>
      
      {/* Footer with version */}
      <div className="border-t border-sidebar-border px-3 py-2">
        <div className="text-xs text-muted-foreground text-center">
          v{packageJson.version}
        </div>
      </div>
    </div>
  );
}

export default function Layout({ children }: LayoutProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const { user, logout } = useAuth();
  const { startTour, hasCompletedTour } = useTour();
  const { actualMode, toggleTheme } = useTheme();
  const { t } = useTranslation();
  const location = useLocation();
  const { showHelp: showShortcuts, setShowHelp: setShowShortcuts } = useKeyboardShortcuts();

  // Dynamic tab title: "Cycles | ShipFlow", "Pitches | ShipFlow", etc.
  useEffect(() => {
    const pageTitle = getPageTitle(location.pathname);
    document.title = pageTitle === 'ShipFlow' ? 'ShipFlow' : `${pageTitle} | ShipFlow`;
  }, [location.pathname]);

  // Global search: Cmd+K / Ctrl+K or plain S key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
        e.preventDefault();
        setSearchOpen((prev) => !prev);
        return;
      }
      if ((e.key === 's' || e.key === 'S') && !e.metaKey && !e.ctrlKey && !e.altKey) {
        const active = document.activeElement;
        const tag = active?.tagName.toLowerCase();
        const typing =
          tag === 'input' ||
          tag === 'textarea' ||
          tag === 'select' ||
          (active as HTMLElement | null)?.isContentEditable;
        if (!typing) {
          e.preventDefault();
          setSearchOpen(true);
        }
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <div className="flex h-screen overflow-hidden bg-background">
      {/* Desktop Sidebar - hidden on mobile (< 768px) */}
      <aside className="hidden lg:flex w-64 flex-shrink-0 border-e border-border bg-sidebar">
        <SidebarContent />
      </aside>

      {/* Mobile Sidebar - Sheet/Drawer for hamburger menu */}
      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent side="start" className="w-72 p-0 bg-sidebar">
          <SidebarContent onItemClick={() => setMobileOpen(false)} />
        </SheetContent>
      </Sheet>

      {/* Main Content - Full width on mobile */}
      <div className="flex flex-1 flex-col w-full lg:w-auto overflow-hidden">
        {/* Header */}
        <header className="sticky top-0 z-40 flex h-14 items-center gap-2 sm:gap-4 border-b border-border bg-background/95 px-3 sm:px-4 backdrop-blur supports-[backdrop-filter]:bg-background/60">
          {/* Mobile Menu Button - Touch-friendly */}
          <Button
            variant="ghost"
            size="icon"
            className="lg:hidden h-11 w-11 touch-manipulation"
            onClick={() => setMobileOpen(true)}
            aria-label="Open navigation menu"
          >
            <Menu className="h-6 w-6" />
            <span className="sr-only">Toggle menu</span>
          </Button>

          {/* Project Selector - Responsive */}
          <div className="flex-1 min-w-0 lg:flex-none" data-tour="project-selector">
            <ProjectSelector />
          </div>

          <div className="hidden sm:flex flex-1" />

          {/* Right side actions - Touch-friendly */}
          <div className="flex items-center gap-1 sm:gap-2">
            {/* Tour Help Button - hidden on mobile to save space for ProjectSelector */}
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={startTour}
                  className={cn(
                    "hidden sm:inline-flex h-11 w-11 touch-manipulation",
                    !hasCompletedTour && "text-primary animate-pulse"
                  )}
                  aria-label={hasCompletedTour ? t('layout.restartTour') : t('layout.startTour')}
                >
                  <HelpCircle className="h-5 w-5 sm:h-6 sm:w-6" />
                  <span className="sr-only">
                    {hasCompletedTour ? t('layout.restartTour') : t('layout.startTour')}
                  </span>
                </Button>
              </TooltipTrigger>
              <TooltipContent>
                {hasCompletedTour ? t('layout.restartTour') : t('layout.startTour')}
              </TooltipContent>
            </Tooltip>

            {/* Language Selector - hidden on mobile to save space for ProjectSelector */}
            <Tooltip>
              <TooltipTrigger asChild>
                <div className="hidden sm:block">
                  <LanguageSelector className="h-11 w-11 touch-manipulation" />
                </div>
              </TooltipTrigger>
              <TooltipContent>
                {t('layout.changeLanguage')}
              </TooltipContent>
            </Tooltip>

            {/* Theme Toggle - hidden on mobile to save space for ProjectSelector */}
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={toggleTheme}
                  className="hidden sm:inline-flex h-11 w-11 touch-manipulation"
                  aria-label={t('layout.toggleTheme')}
                >
                  {actualMode === 'dark' ? (
                    <Sun className="h-5 w-5 sm:h-6 sm:w-6" />
                  ) : (
                    <Moon className="h-5 w-5 sm:h-6 sm:w-6" />
                  )}
                  <span className="sr-only">{t('layout.toggleTheme')}</span>
                </Button>
              </TooltipTrigger>
              <TooltipContent>
                {actualMode === 'dark' ? t('layout.switchToLight') : t('layout.switchToDark')}
              </TooltipContent>
            </Tooltip>

            {/* Global Search */}
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setSearchOpen(true)}
                  className="hidden sm:inline-flex h-11 w-11 touch-manipulation"
                  aria-label={t('globalSearch.shortcut', 'Search')}
                >
                  <Search className="h-5 w-5 sm:h-6 sm:w-6" />
                  <span className="sr-only">{t('globalSearch.shortcut', 'Search')}</span>
                </Button>
              </TooltipTrigger>
              <TooltipContent>
                {t('globalSearch.shortcut', 'Search')} ({navigator.platform?.includes('Mac') ? '⌘' : 'Ctrl'}+K)
              </TooltipContent>
            </Tooltip>

            {/* Notification Center */}
            <NotificationCenter />

            {/* User Menu - Touch-friendly */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className="flex items-center gap-2 px-2 h-11 touch-manipulation"
                  data-tour="user-menu"
                  aria-label={t('layout.userMenu')}
                >
                  <Avatar className="h-8 w-8">
                    <AvatarFallback className="bg-primary text-primary-foreground text-sm">
                      {user?.username?.[0]?.toUpperCase() || 'U'}
                    </AvatarFallback>
                  </Avatar>
                  <span className="hidden sm:inline-block text-sm font-medium">
                    {user?.username}
                  </span>
                  <ChevronDown className="hidden sm:block h-4 w-4 text-muted-foreground" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel className="flex items-center gap-2">
                  <User className="h-4 w-4" />
                  <div className="flex flex-col">
                    <span>{user?.username}</span>
                    <span className="text-xs font-normal text-muted-foreground">
                      {user?.role}
                    </span>
                  </div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                  <Link to="/profile" className="flex items-center gap-2">
                    <Settings className="h-4 w-4" />
                    {t('layout.myProfile')}
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                  onClick={logout}
                  className="flex items-center gap-2 text-destructive focus:text-destructive"
                >
                  <LogOut className="h-4 w-4" />
                  {t('common.logout')}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </header>

        {/* Page Content - Responsive padding with scroll, extra bottom padding for mobile bottom nav */}
        <main className="flex-1 overflow-y-auto overscroll-contain p-3 sm:p-4 md:p-6 pb-20 sm:pb-4 md:pb-6">
          <RouteProgressProvider>
            <Breadcrumbs />
            {children}
          </RouteProgressProvider>
        </main>
      </div>

      {/* Welcome Tour Dialog for new users */}
      <WelcomeTourDialog />

      {/* Global Search Command Palette */}
      <GlobalSearchCommand open={searchOpen} onOpenChange={setSearchOpen} />

      {/* Q&A Floating Button - Available on all pages. Scoped to the whole Knowledge
          Center (wiki + pitches + docs…), not the active cycle, so general questions
          retrieve across all ingested content without entity filtering. */}
      <QAFloatingButton contextType="knowledge" />

      {/* Mobile Bottom Navigation */}
      <MobileBottomNav />

      {/* Keyboard Shortcut Sheet — triggered by ? key */}
      <KeyboardShortcutSheet
        open={showShortcuts}
        onClose={() => setShowShortcuts(false)}
      />
    </div>
  );
}
