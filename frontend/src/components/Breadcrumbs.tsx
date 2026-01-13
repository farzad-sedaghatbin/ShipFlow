import React from 'react';
import { Link, useLocation } from 'react-router-dom';
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
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface BreadcrumbItem {
  label: string;
  path?: string;
  icon?: React.ReactNode;
}

// Map routes to breadcrumb labels and icons
const routeConfig: Record<string, { label: string; icon: React.ReactNode }> = {
  '/dashboard': { label: 'Dashboard', icon: <Home className="h-4 w-4" /> },
  '/projects': { label: 'Projects', icon: <Folder className="h-4 w-4" /> },
  '/cycles': { label: 'Cycles', icon: <Repeat className="h-4 w-4" /> },
  '/cycles/new': { label: 'New Cycle', icon: <Repeat className="h-4 w-4" /> },
  '/pitches': { label: 'Pitch Board', icon: <FileText className="h-4 w-4" /> },
  '/tasks': { label: 'Tasks', icon: <CheckSquare className="h-4 w-4" /> },
  '/health': { label: 'Health Overview', icon: <Activity className="h-4 w-4" /> },
  '/worklogs': { label: 'Work Logs', icon: <Clock className="h-4 w-4" /> },
  '/my-worklogs': { label: 'My Work Logs', icon: <History className="h-4 w-4" /> },
  '/teams': { label: 'Teams', icon: <Users className="h-4 w-4" /> },
  '/people': { label: 'People', icon: <Users2 className="h-4 w-4" /> },
  '/meetings': { label: 'Meetings', icon: <Calendar className="h-4 w-4" /> },
  '/reports': { label: 'Reports', icon: <BarChart3 className="h-4 w-4" /> },
  '/profile': { label: 'Profile', icon: <User className="h-4 w-4" /> },
  '/users': { label: 'User Management', icon: <Shield className="h-4 w-4" /> },
  '/settings': { label: 'Organization Settings', icon: <Settings className="h-4 w-4" /> },
  '/slack': { label: 'Slack Integration', icon: <MessageSquare className="h-4 w-4" /> },
};

// Parse dynamic route segments
const parseDynamicSegment = (segment: string, fullPath: string): { label: string; icon?: React.ReactNode } => {
  // Check if this looks like an ID (numeric)
  if (/^\d+$/.test(segment)) {
    // Check the previous segment to determine context
    if (fullPath.includes('/cycles/')) {
      return { label: `Cycle #${segment}`, icon: <Repeat className="h-4 w-4" /> };
    }
    if (fullPath.includes('/pitches/')) {
      return { label: `Pitch #${segment}`, icon: <FileText className="h-4 w-4" /> };
    }
    return { label: `#${segment}` };
  }
  
  // Handle known dynamic segments
  if (segment === 'edit') {
    return { label: 'Edit' };
  }
  if (segment === 'new') {
    return { label: 'New' };
  }
  if (segment === 'hill-chart') {
    return { label: 'Hill Chart' };
  }
  
  // Return capitalized segment as fallback
  return { label: segment.charAt(0).toUpperCase() + segment.slice(1) };
};

export default function Breadcrumbs() {
  const location = useLocation();
  
  // Don't show breadcrumbs on dashboard
  if (location.pathname === '/dashboard' || location.pathname === '/') {
    return null;
  }

  const pathSegments = location.pathname.split('/').filter(Boolean);
  
  // Build breadcrumb items
  const breadcrumbs: BreadcrumbItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: <Home className="h-4 w-4" /> },
  ];

  let currentPath = '';
  pathSegments.forEach((segment, index) => {
    currentPath += `/${segment}`;
    const isLast = index === pathSegments.length - 1;
    
    // Check if we have a static config for this path
    const config = routeConfig[currentPath];
    
    if (config) {
      breadcrumbs.push({
        label: config.label,
        path: isLast ? undefined : currentPath,
        icon: config.icon,
      });
    } else {
      // Parse dynamic segment
      const dynamicConfig = parseDynamicSegment(segment, currentPath);
      breadcrumbs.push({
        label: dynamicConfig.label,
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
