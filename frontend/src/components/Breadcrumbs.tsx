import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Home,
  Folder,
  Repeat,
  FileText,
  Clock,
  Users,
  Calendar,
  BarChart3,
  Users2,
  Activity,
  History,
  CheckSquare,
  User,
  Shield,
  ChevronRight,
  Settings,
  MessageSquare,
  Beaker,
  Upload,
  BookOpenText,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useProject, useBreadcrumbLabels } from '../contexts';

interface BreadcrumbItem {
  label: string;
  path?: string;
  icon?: React.ReactNode;
}

// Map routes to breadcrumb translation keys and icons
const routeConfig: Record<string, { key: string; icon: React.ReactNode }> = {
  '/dashboard': { key: 'breadcrumbs.dashboard', icon: <Home className="h-4 w-4" /> },
  '/projects': { key: 'breadcrumbs.projects', icon: <Folder className="h-4 w-4" /> },
  '/cycles': { key: 'breadcrumbs.cycles', icon: <Repeat className="h-4 w-4" /> },
  '/cycles/new': { key: 'breadcrumbs.newCycle', icon: <Repeat className="h-4 w-4" /> },
  '/pitches': { key: 'breadcrumbs.pitchBoard', icon: <FileText className="h-4 w-4" /> },
  '/tasks': { key: 'breadcrumbs.tasks', icon: <CheckSquare className="h-4 w-4" /> },
  '/health': { key: 'breadcrumbs.healthOverview', icon: <Activity className="h-4 w-4" /> },
  '/worklogs': { key: 'breadcrumbs.workLogs', icon: <Clock className="h-4 w-4" /> },
  '/my-worklogs': { key: 'breadcrumbs.myWorkLogs', icon: <History className="h-4 w-4" /> },
  '/teams': { key: 'breadcrumbs.teams', icon: <Users className="h-4 w-4" /> },
  '/people': { key: 'breadcrumbs.people', icon: <Users2 className="h-4 w-4" /> },
  '/meetings': { key: 'breadcrumbs.meetings', icon: <Calendar className="h-4 w-4" /> },
  '/reports': { key: 'breadcrumbs.reports', icon: <BarChart3 className="h-4 w-4" /> },
  '/profile': { key: 'breadcrumbs.profile', icon: <User className="h-4 w-4" /> },
  '/users': { key: 'breadcrumbs.userManagement', icon: <Shield className="h-4 w-4" /> },
  '/settings': { key: 'breadcrumbs.organizationSettings', icon: <Settings className="h-4 w-4" /> },
  '/slack': { key: 'breadcrumbs.slackIntegration', icon: <MessageSquare className="h-4 w-4" /> },
  '/permissions': { key: 'breadcrumbs.permissions', icon: <Shield className="h-4 w-4" /> },
  '/qa': { key: 'breadcrumbs.qa', icon: <CheckSquare className="h-4 w-4" /> },
  '/qa/test-cases': { key: 'breadcrumbs.testCases', icon: <CheckSquare className="h-4 w-4" /> },
  '/qa/bug-reports': { key: 'breadcrumbs.bugReports', icon: <CheckSquare className="h-4 w-4" /> },
  '/rd/wise-architecture': { key: 'breadcrumbs.wiseArchitecture', icon: <Beaker className="h-4 w-4" /> },
  '/integrations': { key: 'breadcrumbs.integrations', icon: <Settings className="h-4 w-4" /> },
  '/integrations/github': { key: 'breadcrumbs.githubIntegration', icon: <Settings className="h-4 w-4" /> },
  '/integrations/teams': { key: 'breadcrumbs.teamsIntegration', icon: <Settings className="h-4 w-4" /> },
  '/time': { key: 'breadcrumbs.time', icon: <Clock className="h-4 w-4" /> },
  '/time/logs': { key: 'breadcrumbs.timeLogs', icon: <Clock className="h-4 w-4" /> },
  '/backlog': { key: 'breadcrumbs.backlog', icon: <FileText className="h-4 w-4" /> },
  '/import': { key: 'breadcrumbs.importData', icon: <Upload className="h-4 w-4" /> },
  '/wiki': { key: 'breadcrumbs.wiki', icon: <BookOpenText className="h-4 w-4" /> },
};

// Parse dynamic route segments
const parseDynamicSegment = (t: (key: string, options?: any) => string, segment: string, fullPath: string): { label: string; icon?: React.ReactNode } => {
  // Check if this looks like an ID (numeric)
  if (/^\d+$/.test(segment)) {
    // Check the previous segment to determine context
    if (fullPath.includes('/cycles/')) {
      return { label: t('breadcrumbs.cycleNumber', { number: segment }), icon: <Repeat className="h-4 w-4" /> };
    }
    if (fullPath.includes('/pitches/')) {
      return { label: t('breadcrumbs.pitchNumber', { number: segment }), icon: <FileText className="h-4 w-4" /> };
    }
    if (fullPath.includes('/projects/')) {
      return { label: t('breadcrumbs.projectNumber', { number: segment }), icon: <Folder className="h-4 w-4" /> };
    }
    if (fullPath.includes('/backlog/')) {
      return { label: t('breadcrumbs.backlogNumber', { number: segment }), icon: <FileText className="h-4 w-4" /> };
    }
    return { label: `#${segment}` };
  }
  
  // Handle known dynamic segments
  if (segment === 'edit') {
    return { label: t('breadcrumbs.edit') };
  }
  if (segment === 'new') {
    return { label: t('breadcrumbs.new') };
  }
  if (segment === 'hill-chart') {
    return { label: t('breadcrumbs.hillChart') };
  }
  if (segment === 'generate') {
    return { label: t('breadcrumbs.generate') };
  }
  if (segment === 'run') {
    return { label: t('breadcrumbs.run') };
  }
  
  // Return capitalized segment as fallback
  return { label: segment.charAt(0).toUpperCase() + segment.slice(1) };
};

export default function Breadcrumbs() {
  const { t } = useTranslation();
  const location = useLocation();
  const { isScrumProject } = useProject();
  // Resolved names published by detail pages (e.g. /cycles/12 → "Q3 Cycle"),
  // used to replace raw numeric-ID labels in the breadcrumb trail.
  const resolvedLabels = useBreadcrumbLabels();
  
  // Don't show breadcrumbs on dashboard
  if (location.pathname === '/dashboard' || location.pathname === '/') {
    return null;
  }

  const pathSegments = location.pathname.split('/').filter(Boolean);
  
  // Build breadcrumb items
  const breadcrumbs: BreadcrumbItem[] = [
    { label: t('breadcrumbs.dashboard'), path: '/dashboard', icon: <Home className="h-4 w-4" /> },
  ];

  let currentPath = '';
  pathSegments.forEach((segment, index) => {
    currentPath += `/${segment}`;
    const isLast = index === pathSegments.length - 1;
    
    // Check if we have a static config for this path
    const config = routeConfig[currentPath];
    // A name published by a detail page takes precedence over both the static
    // label and the numeric-ID fallback.
    const resolved = resolvedLabels[currentPath];

    if (config) {
      // Swap cycle labels to sprint labels for Scrum projects
      let label = resolved ?? t(config.key);
      if (!resolved && isScrumProject) {
        if (config.key === 'breadcrumbs.cycles') label = t('breadcrumbs.sprints');
        else if (config.key === 'breadcrumbs.newCycle') label = t('breadcrumbs.newSprint');
        else if (config.key === 'breadcrumbs.cycleNumber') label = t('breadcrumbs.sprintNumber', { number: '' }).trim();
      }
      breadcrumbs.push({
        label,
        path: isLast ? undefined : currentPath,
        icon: config.icon,
      });
    } else {
      // Parse dynamic segment
      const dynamicConfig = parseDynamicSegment(t, segment, currentPath);
      // A registered name wins; otherwise fall back to the parsed label.
      let dynLabel = resolved ?? dynamicConfig.label;
      // For Scrum projects, "Cycle #N" becomes "Sprint #N" (only when unresolved).
      if (!resolved && isScrumProject && currentPath.includes('/cycles/') && /^\d+$/.test(segment)) {
        dynLabel = t('breadcrumbs.sprintNumber', { number: segment });
      }
      breadcrumbs.push({
        label: dynLabel,
        path: isLast ? undefined : currentPath,
        icon: dynamicConfig.icon,
      });
    }
  });

  return (
    <nav className="mb-4" aria-label="Page breadcrumb navigation">
      <ol className="flex items-center gap-1 text-sm">
        {breadcrumbs.map((item, index) => {
          const isLast = index === breadcrumbs.length - 1;

          return (
            <li key={index} className="flex items-center gap-1">
              {index > 0 && (
                <ChevronRight className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
              )}
              
              {isLast || !item.path ? (
                <span
                  className="flex items-center gap-1.5 font-medium text-foreground"
                  aria-current={isLast ? 'page' : undefined}
                >
                  {item.icon && (
                    <span className="text-primary" aria-hidden="true">
                      {item.icon}
                    </span>
                  )}
                  {item.label}
                </span>
              ) : (
                <Link
                  to={item.path}
                  aria-label={`Navigate to ${item.label}`}
                  className={cn(
                    "flex items-center gap-1.5 rounded px-1.5 py-0.5 -mx-1.5 text-muted-foreground",
                    "transition-colors hover:text-primary hover:bg-accent",
                    "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  )}
                >
                  {item.icon && (
                    <span className="opacity-70" aria-hidden="true">
                      {item.icon}
                    </span>
                  )}
                  {item.label}
                </Link>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
