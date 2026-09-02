import { useState } from 'react';
import { useLocation, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  MoreHorizontal,
  X,
  ListTodo,
  Bug,
  Users2,
  Users,
  FlaskConical,
  Map,
  Target,
  Layers,
  PackageCheck,
  Calendar,
  Clock,
  Beaker,
  Workflow,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useBreakpointHelpers } from '@/hooks/useBreakpoint';
import { useProject } from '../contexts';
import type { NavItemConfig } from '../config/projectTypeCapabilities';

interface NavSection {
  titleKey: string;
  items: NavItemConfig[];
}

// Primary tabs and the Cycle/Sprint Workspace section come from
// projectTypeCapabilities.ts (same source Layout.tsx's desktop sidebar reads)
// so mobile can't drift from desktop the way it previously had. The remaining
// sections below aren't project-type-specific.
function getMoreSections(capabilities: ReturnType<typeof useProject>['capabilities']): NavSection[] {
  const sections: NavSection[] = [
    {
      titleKey: 'nav.sections.overview',
      items: capabilities.nav.mainItems,
    },
  ];

  if (capabilities.nav.showWorkspace) {
    sections.push({
      titleKey: capabilities.nav.workspaceSectionTitleKey,
      items: capabilities.nav.workspaceItems,
    });
  }

  const workManagementItems: NavItemConfig[] = [
    { textKey: 'nav.backlog', icon: ListTodo, path: '/backlog' },
  ];
  if (capabilities.nav.showSprintPlanning) {
    workManagementItems.push({ textKey: 'nav.sprintPlanning', icon: Workflow, path: '/sprint-planning' });
  }
  workManagementItems.push(
    { textKey: 'nav.workLogs', icon: Clock, path: '/time/logs' },
    { textKey: 'nav.meetings', icon: Calendar, path: '/meetings' },
  );
  sections.push({ titleKey: 'nav.sections.workManagement', items: workManagementItems });

  sections.push(
    {
      titleKey: 'nav.sections.quality',
      items: [
        { textKey: 'nav.testCases', icon: FlaskConical, path: '/qa/test-cases' },
        { textKey: 'nav.bugReports', icon: Bug, path: '/qa/bug-reports' },
      ],
    },
    {
      titleKey: 'nav.sections.organization',
      items: [
        { textKey: 'nav.people', icon: Users2, path: '/people' },
        { textKey: 'nav.teams', icon: Users, path: '/teams' },
      ],
    },
    {
      titleKey: 'nav.sections.rd',
      items: [
        { textKey: 'nav.wiseArchitecture', icon: Beaker, path: '/rd/wise-architecture' },
      ],
    },
    {
      titleKey: 'nav.sections.roadmap',
      items: [
        { textKey: 'nav.roadmap', icon: Map, path: '/roadmap' },
        { textKey: 'nav.initiatives', icon: Target, path: '/initiatives' },
        { textKey: 'nav.epics', icon: Layers, path: '/epics' },
        { textKey: 'nav.releases', icon: PackageCheck, path: '/releases-management' },
      ],
    },
  );

  return sections;
}

export default function MobileBottomNav() {
  const { t } = useTranslation();
  const location = useLocation();
  const { isMobile } = useBreakpointHelpers();
  const { capabilities } = useProject();
  const [moreOpen, setMoreOpen] = useState(false);

  if (!isMobile) return null;

  const primaryTabs = capabilities.mobile.primaryTabs;
  const moreSections = getMoreSections(capabilities);

  const isActive = (item: NavItemConfig) => {
    const paths = item.matchPaths || [item.path];
    return paths.some(p => location.pathname === p || location.pathname.startsWith(p + '/'));
  };

  const moreHasActive = moreSections.some(section =>
    section.items.some(item => isActive(item)),
  );
  const moreIsActive = !primaryTabs.some(tab => isActive(tab)) && moreHasActive;

  return (
    <>
      {/* "More" full-screen overlay */}
      {moreOpen && (
        <div className="fixed inset-0 z-50 bg-background flex flex-col">
          {/* Header */}
          <div className="flex items-center justify-between px-4 h-14 border-b border-border">
            <span className="text-lg font-semibold">{t('nav.menu')}</span>
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
                        onClick={() => { if (!active) setMoreOpen(false); }}
                        className={cn(
                          "flex flex-col items-center gap-1.5 rounded-xl p-3 min-h-[72px] justify-center transition-colors touch-manipulation",
                          active
                            ? "bg-primary/10 text-primary"
                            : "bg-muted/50 text-muted-foreground hover:bg-accent"
                        )}
                      >
                        <Icon className="h-5 w-5" />
                        <span className="text-[11px] font-medium text-center leading-tight">{t(item.textKey)}</span>
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
                onClick={() => { if (moreOpen) setMoreOpen(false); }}
                className={cn(
                  "flex flex-col items-center justify-center gap-0.5 flex-1 py-2 min-h-[56px] text-[10px] font-medium transition-colors touch-manipulation",
                  active
                    ? "text-primary"
                    : "text-muted-foreground hover:text-foreground"
                )}
              >
                <Icon className={cn("h-5 w-5", active && "text-primary")} />
                <span className="truncate max-w-[64px]">{t(item.textKey)}</span>
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
            <span className="truncate max-w-[64px]">{t('nav.more')}</span>
          </button>
        </div>
      </nav>
    </>
  );
}
