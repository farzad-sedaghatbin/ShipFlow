import { useLocation, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  LayoutDashboard,
  Repeat,
  ListTodo,
  BarChart3,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { useBreakpointHelpers } from '@/hooks/useBreakpoint';

interface BottomNavItem {
  labelKey: string;
  icon: React.ElementType;
  path: string;
  matchPaths?: string[];
}

const bottomNavItems: BottomNavItem[] = [
  {
    labelKey: 'nav.dashboard',
    icon: LayoutDashboard,
    path: '/dashboard',
  },
  {
    labelKey: 'nav.cycles',
    icon: Repeat,
    path: '/cycles',
    matchPaths: ['/cycles', '/pitches', '/betting', '/health', '/retros'],
  },
  {
    labelKey: 'nav.backlog',
    icon: ListTodo,
    path: '/backlog',
  },
  {
    labelKey: 'nav.reports',
    icon: BarChart3,
    path: '/reports',
  },
];

export default function MobileBottomNav() {
  const { t } = useTranslation();
  const location = useLocation();
  const { isMobile } = useBreakpointHelpers();

  if (!isMobile) return null;

  const isActive = (item: BottomNavItem) => {
    const paths = item.matchPaths || [item.path];
    return paths.some(p => location.pathname === p || location.pathname.startsWith(p + '/'));
  };

  return (
    <nav
      className="fixed bottom-0 left-0 right-0 z-50 border-t border-border bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80"
      style={{ paddingBottom: 'env(safe-area-inset-bottom, 0px)' }}
    >
      <div className="flex items-stretch justify-around">
        {bottomNavItems.map((item) => {
          const Icon = item.icon;
          const active = isActive(item);
          return (
            <Link
              key={item.path}
              to={item.path}
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
      </div>
    </nav>
  );
}
