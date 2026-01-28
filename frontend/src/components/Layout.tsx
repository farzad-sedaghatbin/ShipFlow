import { useState } from 'react';
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
  { textKey: 'nav.reports', icon: BarChart3, path: '/reports', tourId: 'reports-menu' },
];

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
const integrationItems: NavItemConfig[] = [
  { textKey: 'integrations.slack', icon: MessageSquare, path: '/integrations/slack' },
  { textKey: 'integrations.github', icon: Github, path: '/integrations/github' },
  { textKey: 'integrations.teams', icon: Users2, path: '/integrations/teams' },
];

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
  const { isKanbanProject, isAllProjectsSelected } = useProject();
  const { hasPermissionSync } = usePermission();
  const currentPath = location.pathname;

  // Check if we're in a cycle context (viewing pitches, betting, health, retros, reports)
  const isCycleContext = ['/pitches', '/betting', '/health', '/retros', '/reports'].some(
    path => currentPath.startsWith(path)
  ) || /\/cycles\/\d+/.test(currentPath);

  // Determine which nav items to show based on project type
  // When "All Projects" is selected, show Shape Up navigation (full features)
  const showKanbanFeatures = isKanbanProject && !isAllProjectsSelected;
  const mainNavItems = showKanbanFeatures ? kanbanMainNavItems : shapeUpMainNavItems;
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

          {/* Cycle Workspace Section - Only for Shape Up projects or All Projects */}
          {showCycleWorkspace && (
            <>
              <SectionHeader textKey="nav.sections.cycleWorkspace" />
              <NavGroup
                titleKey="nav.groups.cycleTools"
                icon={Target}
                items={cycleWorkspaceItems}
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

          {/* Meetings */}
          {meetingsItems.map((item) => (
            <NavItem
              key={item.path}
              item={item}
              isActive={currentPath === item.path}
              onClick={onItemClick}
            />
          ))}

          {/* Organization Section */}
          <SectionHeader textKey="nav.sections.organization" />
          <NavGroup
            titleKey="nav.groups.people"
            icon={Users2}
            items={peopleItems}
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

          {/* Help & Guides Section */}
          <SectionHeader textKey="nav.sections.helpSupport" />
          <NavItem
            item={{ textKey: 'nav.helpGuides', icon: BookOpen, path: '/help', tourId: 'help-menu' }}
            isActive={currentPath.startsWith('/help')}
            onClick={onItemClick}
          />
        </nav>

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
    </div>
  );
}

export default function Layout({ children }: LayoutProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const { user, logout } = useAuth();
  const { startTour, hasCompletedTour } = useTour();
  const { actualMode, toggleTheme } = useTheme();
  const { t } = useTranslation();

  return (
    <div className="flex min-h-screen bg-background">
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
      <div className="flex flex-1 flex-col w-full lg:w-auto">
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

          {/* Dashboard Switcher */}
          <div className="hidden lg:block">
            <DashboardSwitcher onDashboardChange={(dashboardId) => {
              // Dashboard changed - could refresh widgets or navigate
              console.log('Dashboard switched to:', dashboardId);
            }} />
          </div>

          <div className="hidden sm:flex flex-1" />

          {/* Right side actions - Touch-friendly */}
          <div className="flex items-center gap-1 sm:gap-2">
            {/* Tour Help Button */}
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={startTour}
                  className={cn(
                    "h-11 w-11 touch-manipulation",
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

            {/* Language Selector */}
            <Tooltip>
              <TooltipTrigger asChild>
                <div>
                  <LanguageSelector className="h-11 w-11 touch-manipulation" />
                </div>
              </TooltipTrigger>
              <TooltipContent>
                {t('layout.changeLanguage')}
              </TooltipContent>
            </Tooltip>

            {/* Theme Toggle */}
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={toggleTheme}
                  className="h-11 w-11 touch-manipulation"
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

        {/* Page Content - Responsive padding */}
        <main className="flex-1 p-3 sm:p-4 md:p-6">
          <Breadcrumbs />
          {children}
        </main>
      </div>

      {/* Welcome Tour Dialog for new users */}
      <WelcomeTourDialog />

      {/* Q&A Floating Button - Available on all pages */}
      <QAFloatingButton contextType="cycle" contextName="your active cycles" />
    </div>
  );
}
