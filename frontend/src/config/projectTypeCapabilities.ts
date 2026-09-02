import type { ComponentType } from 'react';
import {
  LayoutDashboard,
  Repeat,
  Folder,
  FileText,
  Dices,
  Activity,
  Brain,
  BarChart3,
  Bug,
  ListTodo,
} from 'lucide-react';
import { ProjectType } from '../types';

/**
 * Single source of truth for "what does this project type support" — nav items,
 * mobile tabs, quick links, keyboard shortcuts, and dashboard stat visibility.
 * Consumed by Layout.tsx (desktop sidebar), MobileBottomNav.tsx, QuickLinks.tsx,
 * useKeyboardShortcuts.ts, and Dashboard.tsx so these surfaces can't drift from
 * each other the way MobileBottomNav had drifted from Layout.tsx.
 *
 * See PROJECT_TYPE_ARCHITECTURE.md for the full rationale.
 */

export interface NavItemConfig {
  textKey: string;
  icon: ComponentType<{ className?: string }>;
  path: string;
  tourId?: string;
  /** Mobile-only: additional path prefixes that should count as "this tab is active". */
  matchPaths?: string[];
}

export type QuickLinkId = 'newCycle' | 'logWork' | 'viewPitches' | 'tasks' | 'healthCheck' | 'reports';

export type ShortcutId =
  | 'goDashboard'
  | 'goCycles'
  | 'goPitches'
  | 'goTasks'
  | 'goMeetings'
  | 'goReports'
  | 'goHealth'
  | 'newCycle'
  | 'logWork'
  | 'showHelp'
  | 'closeDialog';

export interface ProjectTypeCapabilities {
  projectType: ProjectType | null;
  /** Cycles (Shape Up) or Sprints (Scrum) exist as a concept for this type. */
  hasCycles: boolean;
  /** Pitches/Betting exist as a concept for this type. */
  hasPitches: boolean;
  isScrum: boolean;
  nav: {
    mainItems: NavItemConfig[];
    workspaceItems: NavItemConfig[];
    showWorkspace: boolean;
    workspaceSectionTitleKey: string;
    workspaceGroupTitleKey: string;
    showSprintPlanning: boolean;
    /** Kanban has no workspace section, so Reports is promoted to a top-level item. */
    promoteReportsTopLevel: boolean;
  };
  mobile: {
    primaryTabs: NavItemConfig[];
  };
  quickLinkIds: QuickLinkId[];
  shortcutIds: ShortcutId[];
  dashboard: {
    showActiveCyclesStat: boolean;
    showTotalPitchesStat: boolean;
    showCompletedStat: boolean;
    showInProgressStat: boolean;
    /** Widget types meaningful on the Dashboard "Overview" tab for this type. */
    overviewWidgetTypes: string[];
  };
  /**
   * Widget types ever meaningful for this project type — used to filter what
   * "Customize Widgets" offers and what gets seeded for a new user, so a
   * Kanban/Scrum user isn't offered widgets that can never render for them.
   */
  defaultWidgetTypes: string[];
}

// Widget types shared across all project types (task-based, not tied to
// cycles/pitches) — used by the Activity tab and as the Kanban Overview tab.
const genericWidgetTypes = ['OVERDUE_TASKS', 'BLOCKED_TASKS', 'UPCOMING_DEADLINES', 'MY_TASKS', 'TEAM_WORKLOAD', 'RECENT_ACTIVITY'];

// --- Shared nav item definitions (canonical source — Layout.tsx and
// MobileBottomNav.tsx both import these instead of hand-duplicating them) ---

const dashboardItem: NavItemConfig = { textKey: 'nav.dashboard', icon: LayoutDashboard, path: '/dashboard', tourId: 'dashboard-menu' };
const projectsItem: NavItemConfig = { textKey: 'nav.projects', icon: Folder, path: '/projects', tourId: 'projects-menu' };
const cyclesItem: NavItemConfig = { textKey: 'nav.cycles', icon: Repeat, path: '/cycles', tourId: 'cycles-menu', matchPaths: ['/cycles', '/betting', '/health', '/retros'] };
const sprintsItem: NavItemConfig = { textKey: 'nav.sprints', icon: Repeat, path: '/cycles', tourId: 'cycles-menu', matchPaths: ['/cycles', '/health', '/retros'] };
const pitchBoardItem: NavItemConfig = { textKey: 'nav.pitchBoard', icon: FileText, path: '/pitches', tourId: 'pitches-menu', matchPaths: ['/pitches'] };
const bettingItem: NavItemConfig = { textKey: 'nav.betting', icon: Dices, path: '/betting', tourId: 'betting-menu' };
const healthItem: NavItemConfig = { textKey: 'nav.health', icon: Activity, path: '/health', tourId: 'health-menu' };
const retrosItem: NavItemConfig = { textKey: 'nav.retrospectives', icon: Brain, path: '/retros', tourId: 'retros-menu' };
const dashboardsItem: NavItemConfig = { textKey: 'nav.dashboards', icon: LayoutDashboard, path: '/dashboards', tourId: 'dashboards-menu' };
const reportsItem: NavItemConfig = { textKey: 'nav.reports', icon: BarChart3, path: '/reports', tourId: 'reports-menu' };
const bugReportsItem: NavItemConfig = { textKey: 'nav.bugReports', icon: Bug, path: '/qa/bug-reports', matchPaths: ['/qa/bug-reports'] };
const backlogItem: NavItemConfig = { textKey: 'nav.backlog', icon: ListTodo, path: '/backlog', matchPaths: ['/backlog'] };

export const shapeUpMainNavItems: NavItemConfig[] = [dashboardItem, projectsItem, cyclesItem];
export const scrumMainNavItems: NavItemConfig[] = [dashboardItem, projectsItem, sprintsItem];
export const kanbanMainNavItems: NavItemConfig[] = [dashboardItem, projectsItem];

export const cycleWorkspaceItems: NavItemConfig[] = [pitchBoardItem, bettingItem, healthItem, retrosItem, dashboardsItem, reportsItem];
export const scrumWorkspaceItems: NavItemConfig[] = cycleWorkspaceItems.filter(
  (item) => item.path !== '/pitches' && item.path !== '/betting'
);

// --- Per-type capability definitions ---

const shapeUp: ProjectTypeCapabilities = {
  projectType: 'SHAPE_UP',
  hasCycles: true,
  hasPitches: true,
  isScrum: false,
  nav: {
    mainItems: shapeUpMainNavItems,
    workspaceItems: cycleWorkspaceItems,
    showWorkspace: true,
    workspaceSectionTitleKey: 'nav.sections.cycleWorkspace',
    workspaceGroupTitleKey: 'nav.groups.cycleTools',
    showSprintPlanning: false,
    promoteReportsTopLevel: false,
  },
  mobile: {
    primaryTabs: [dashboardItem, cyclesItem, pitchBoardItem, bugReportsItem],
  },
  quickLinkIds: ['newCycle', 'logWork', 'viewPitches', 'tasks', 'healthCheck', 'reports'],
  shortcutIds: ['goDashboard', 'goCycles', 'goPitches', 'goTasks', 'goMeetings', 'goReports', 'goHealth', 'newCycle', 'logWork', 'showHelp', 'closeDialog'],
  dashboard: {
    showActiveCyclesStat: true,
    showTotalPitchesStat: true,
    showCompletedStat: true,
    showInProgressStat: true,
    overviewWidgetTypes: ['ACTIVE_CYCLES', 'HILL_CHART', 'CYCLE_PROGRESS', 'RECENT_PITCHES'],
  },
  defaultWidgetTypes: [
    ...genericWidgetTypes,
    'CYCLE_PROGRESS', 'CYCLE_SUMMARY', 'CYCLE_SIGNALS', 'AI_RISK_ADVISORY',
    'ACTIVE_CYCLES', 'HILL_CHART', 'RECENT_PITCHES',
  ],
};

const scrum: ProjectTypeCapabilities = {
  projectType: 'SCRUM',
  hasCycles: true,
  hasPitches: false,
  isScrum: true,
  nav: {
    mainItems: scrumMainNavItems,
    workspaceItems: scrumWorkspaceItems,
    showWorkspace: true,
    workspaceSectionTitleKey: 'nav.sections.sprintWorkspace',
    workspaceGroupTitleKey: 'nav.groups.sprintTools',
    showSprintPlanning: true,
    promoteReportsTopLevel: false,
  },
  mobile: {
    primaryTabs: [dashboardItem, sprintsItem, backlogItem, bugReportsItem],
  },
  quickLinkIds: ['newCycle', 'logWork', 'tasks', 'healthCheck', 'reports'],
  shortcutIds: ['goDashboard', 'goCycles', 'goTasks', 'goMeetings', 'goReports', 'goHealth', 'newCycle', 'logWork', 'showHelp', 'closeDialog'],
  dashboard: {
    showActiveCyclesStat: true,
    showTotalPitchesStat: false,
    showCompletedStat: false,
    showInProgressStat: false,
    // No hill chart or pitches for Scrum. CYCLE_PROGRESS is included —
    // CycleProgressWidget.tsx computes it from Task data ("stories") for
    // Scrum cycles and Pitch data for Shape Up, rather than assuming pitches.
    overviewWidgetTypes: ['ACTIVE_CYCLES', 'CYCLE_PROGRESS'],
  },
  defaultWidgetTypes: [
    ...genericWidgetTypes,
    'CYCLE_PROGRESS', 'CYCLE_SUMMARY', 'CYCLE_SIGNALS', 'AI_RISK_ADVISORY', 'ACTIVE_CYCLES',
  ],
};

const kanban: ProjectTypeCapabilities = {
  projectType: 'KANBAN',
  hasCycles: false,
  hasPitches: false,
  isScrum: false,
  nav: {
    mainItems: kanbanMainNavItems,
    workspaceItems: [],
    showWorkspace: false,
    workspaceSectionTitleKey: 'nav.sections.cycleWorkspace',
    workspaceGroupTitleKey: 'nav.groups.cycleTools',
    showSprintPlanning: false,
    promoteReportsTopLevel: true,
  },
  mobile: {
    primaryTabs: [dashboardItem, backlogItem, bugReportsItem, reportsItem],
  },
  quickLinkIds: ['logWork', 'tasks', 'reports'],
  shortcutIds: ['goDashboard', 'goTasks', 'goMeetings', 'goReports', 'logWork', 'showHelp', 'closeDialog'],
  dashboard: {
    showActiveCyclesStat: false,
    showTotalPitchesStat: false,
    showCompletedStat: false,
    showInProgressStat: false,
    overviewWidgetTypes: genericWidgetTypes,
  },
  defaultWidgetTypes: genericWidgetTypes,
};

export const PROJECT_TYPE_CAPABILITIES: Record<ProjectType, ProjectTypeCapabilities> = {
  SHAPE_UP: shapeUp,
  SCRUM: scrum,
  KANBAN: kanban,
};

/**
 * Union of capabilities across the distinct project types actually present in the
 * org. A mixed org resolves to the richest type present (Shape Up ⊃ Scrum ⊃ Kanban
 * in nav/feature surface) so no active project's features are hidden. An org with
 * zero projects yet falls back to the minimal Kanban baseline — never Shape Up.
 */
export function resolveOrgCapabilities(orgProjectTypes: ProjectType[]): ProjectTypeCapabilities {
  const distinct = Array.from(new Set(orgProjectTypes));
  if (distinct.length === 0) return kanban;
  if (distinct.length === 1) return PROJECT_TYPE_CAPABILITIES[distinct[0]];
  if (distinct.includes('SHAPE_UP')) return shapeUp;
  if (distinct.includes('SCRUM')) return scrum;
  return kanban;
}

/**
 * Single entry point every consumer should use: a specific project's type wins;
 * "All Projects" resolves via resolveOrgCapabilities(orgProjectTypes) instead of
 * assuming Shape Up.
 */
export function resolveCapabilities(
  currentProjectType: ProjectType | null,
  orgProjectTypes: ProjectType[]
): ProjectTypeCapabilities {
  if (currentProjectType) return PROJECT_TYPE_CAPABILITIES[currentProjectType];
  return resolveOrgCapabilities(orgProjectTypes);
}
