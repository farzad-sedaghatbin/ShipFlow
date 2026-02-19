import { useState } from 'react';
import { useLocation, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  LayoutDashboard,
  Repeat,
  FileText,
  Bug,
  MoreHorizontal,
  X,
  ListTodo,
  BarChart3,
  Users2,
  Users,
  FlaskConical,
  Map,
  Target,
  Layers,
  PackageCheck,
  Activity,
  Brain,
  Dices,
  Calendar,
  Clock,
  Beaker,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useBreakpointHelpers } from '@/hooks/useBreakpoint';

interface NavEntry {
  labelKey: string;
  icon: React.ElementType;
  path: string;
  matchPaths?: string[];
}

interface NavSection {
  titleKey: string;
  items: NavEntry[];
}

// Primary bottom bar tabs — the 4 most-used + More
const primaryTabs: NavEntry[] = [
  { labelKey: 'nav.dashboard', icon: LayoutDashboard, path: '/dashboard' },
  { labelKey: 'nav.cycles', icon: Repeat, path: '/cycles', matchPaths: ['/cycles', '/pitches', '/betting', '/health', '/retros'] },
  { labelKey: 'nav.pitchBoard', icon: FileText, path: '/pitches', matchPaths: ['/pitches'] },
  { labelKey: 'nav.bugReports', icon: Bug, path: '/qa/bug-reports', matchPaths: ['/qa/bug-reports'] },
];

// Full navigation sections for the "More" drawer
const moreSections: NavSection[] = [
  {
    titleKey: 'nav.sections.overview',
    items: [
      { labelKey: 'nav.dashboard', icon: LayoutDashboard, path: '/dashboard' },
      { labelKey: 'nav.cycles', icon: Repeat, path: '/cycles' },
    ],
  },
  {
    titleKey: 'nav.sections.cycleWorkspace',
    items: [
      { labelKey: 'nav.pitchBoard', icon: FileText, path: '/pitches' },
      { labelKey: 'nav.betting', icon: Dices, path: '/betting' },
      { labelKey: 'nav.health', icon: Activity, path: '/health' },
      { labelKey: 'nav.retrospectives', icon: Brain, path: '/retros' },
      { labelKey: 'nav.reports', icon: BarChart3, path: '/reports' },
    ],
  },
  {
    titleKey: 'nav.sections.workManagement',
    items: [
      { labelKey: 'nav.backlog', icon: ListTodo, path: '/backlog' },
      { labelKey: 'nav.workLogs', icon: Clock, path: '/time/logs' },
      { labelKey: 'nav.meetings', icon: Calendar, path: '/meetings' },
    ],
  },
  {
    titleKey: 'nav.sections.quality',
    items: [
      { labelKey: 'nav.testCases', icon: FlaskConical, path: '/qa/test-cases' },
      { labelKey: 'nav.bugReports', icon: Bug, path: '/qa/bug-reports' },
    ],
  },
  {
    titleKey: 'nav.sections.organization',
    items: [
      { labelKey: 'nav.people', icon: Users2, path: '/people' },
      { labelKey: 'nav.teams', icon: Users, path: '/teams' },
    ],
  },
  {
    titleKey: 'nav.sections.rd',
    items: [
      { labelKey: 'nav.wiseArchitecture', icon: Beaker, path: '/rd/wise-architecture' },
    ],
  },
  {
    titleKey: 'nav.sections.roadmap',
    items: [
      { labelKey: 'nav.roadmap', icon: Map, path: '/roadmap' },
      { labelKey: 'nav.initiatives', icon: Target, path: '/initiatives' },
      { labelKey: 'nav.epics', icon: Layers, path: '/epics' },
      { labelKey: 'nav.releases', icon: PackageCheck, path: '/releases-management' },
    ],
  },
];

export default function MobileBottomNav() {
  const { t } = useTranslation();
  const location = useLocation();
  const { isMobile } = useBreakpointHelpers();
  const [moreOpen, setMoreOpen] = useState(false);

  if (!isMobile) return null;

  const isActive = (item: NavEntry) => {
    const paths = item.matchPaths || [item.path];
    return paths.some(p => location.pathname === p || location.pathname.startsWith(p + '/'));
  };

  const moreIsActive = !primaryTabs.some(tab => isActive(tab));

  return (
    <>
      {/* "More" full-screen overlay */}
      {moreOpen && (
        <div className="fixed inset-0 z-[60] bg-background flex flex-col">
          {/* Header */}
          <div className="flex items-center justify-between px-4 h-14 border-b border-border">
            <span className="text-lg font-semibold">{t('nav.menu', 'Menu')}</span>
            <button
              onClick={() => setMoreOpen(false)}
              className="h-11 w-11 flex items-center justify-center rounded-md hover:bg-accent touch-manipulation"
              aria-label="Close menu"
            >
              <X className="h-6 w-6" />
            </button>
          </div>

          {/* Scrollable sections */}
          <div className="flex-1 overflow-y-auto pb-8">
            {moreSections.map((section) => (
              <div key={section.titleKey} className="px-4 pt-4">
                <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                  {t(section.titleKey)}
                </p>
                <div className="grid grid-cols-3 gap-2">
                  {section.items.map((item) => {
                    const Icon = item.icon;
                    const active = isActive(item);
                    return (
                      <Link
                        key={item.path}
                        to={item.path}
                        onClick={() => setMoreOpen(false)}
                        className={cn(
                          "flex flex-col items-center gap-1.5 rounded-xl p-3 min-h-[72px] justify-center transition-colors touch-manipulation",
                          active
                            ? "bg-primary/10 text-primary"
                            : "bg-muted/50 text-muted-foreground hover:bg-accent"
                        )}
                      >
                        <Icon className="h-5 w-5" />
                        <span className="text-[11px] font-medium text-center leading-tight">{t(item.labelKey)}</span>
                      </Link>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Bottom tab bar */}
      <nav
        className="fixed bottom-0 left-0 right-0 z-50 border-t border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80"
        style={{ paddingBottom: 'env(safe-area-inset-bottom, 0px)' }}
      >
        <div className="flex items-stretch justify-around">
          {primaryTabs.map((item) => {
            const Icon = item.icon;
            const active = isActive(item);
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setMoreOpen(false)}
                className={cn(
                  "flex flex-col items-center justify-center gap-0.5 flex-1 py-2 min-h-[56px] text-[10px] font-medium transition-colors touch-manipulation",
                  active
                    ? "text-primary"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                <Icon className={cn("h-5 w-5", active && "text-primary")} />
                <span className="truncate max-w-[64px]">{t(item.labelKey)}</span>
              </Link>
            );
          })}

          {/* More button */}
          <button
            onClick={() => setMoreOpen(prev => !prev)}
            className={cn(
              "flex flex-col items-center justify-center gap-0.5 flex-1 py-2 min-h-[56px] text-[10px] font-medium transition-colors touch-manipulation",
              moreOpen || moreIsActive
                ? "text-primary"
                : "text-muted-foreground hover:text-foreground"
            )}
          >
            <MoreHorizontal className={cn("h-5 w-5", (moreOpen || moreIsActive) && "text-primary")} />
            <span className="truncate max-w-[64px]">{t('nav.more', 'More')}</span>
          </button>
        </div>
      </nav>
    </>
  );
}
