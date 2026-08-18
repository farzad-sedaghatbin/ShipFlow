import { Link } from 'react-router-dom';
import {
  RefreshCw,
  FileText,
  Clock,
  BarChart3,
  Activity,
  CheckCircle2,
} from 'lucide-react';
import { motion } from 'motion/react';
import { Card, CardContent } from '@/components/ui/card';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';

interface QuickLink {
  label: string;
  description: string;
  icon: React.ReactNode;
  to: string;
  color: string;
  shortcut?: string;
}

const quickLinks: QuickLink[] = [
  {
    label: 'New Cycle',
    description: 'Start a new development cycle',
    icon: <RefreshCw className="w-6 h-6" />,
    to: '/cycles/new',
    color: '#6366f1', // indigo
    shortcut: '⇧N',
  },
  {
    label: 'Log Work',
    description: 'Record your work hours',
    icon: <Clock className="w-6 h-6" />,
    to: '/worklogs',
    color: '#ec4899', // pink
    shortcut: '⇧W',
  },
  {
    label: 'View Pitches',
    description: 'See all project pitches',
    icon: <FileText className="w-6 h-6" />,
    to: '/pitches',
    color: '#14b8a6', // teal
    shortcut: 'P',
  },
  {
    label: 'Tasks',
    description: 'Manage your tasks',
    icon: <CheckCircle2 className="w-6 h-6" />,
    to: '/tasks',
    color: '#f59e0b', // amber
    shortcut: 'T',
  },
  {
    label: 'Health Check',
    description: 'View project health',
    icon: <Activity className="w-6 h-6" />,
    to: '/health',
    color: '#22c55e', // green
    shortcut: 'H',
  },
  {
    label: 'Reports',
    description: 'Analytics & reports',
    icon: <BarChart3 className="w-6 h-6" />,
    to: '/reports',
    color: '#8b5cf6', // purple
    shortcut: 'R',
  },
];

interface QuickLinksProps {
  compact?: boolean;
}

export function QuickLinks({ compact = false }: QuickLinksProps) {
  return (
    <Card>
      <CardContent className={cn('p-4', compact && 'pb-2')}>
        <h2 
          className="text-lg font-semibold text-foreground mb-2"
          id="quick-links-heading"
        >
          Quick Links
        </h2>
        <div 
          className={cn(
            'grid gap-2',
            compact ? 'grid-cols-3 sm:grid-cols-3' : 'grid-cols-2 sm:grid-cols-3 md:grid-cols-6'
          )}
          role="list" 
          aria-labelledby="quick-links-heading"
        >
          {quickLinks.map((link, index) => (
            <div key={link.label} role="listitem">
              <Tooltip>
                <TooltipTrigger asChild>
                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: index * 0.05, duration: 0.3 }}
                    whileHover={{ scale: 1.05, y: -2 }}
                    whileTap={{ scale: 0.98 }}
                  >
                    <Link
                      to={link.to}
                      aria-label={`${link.label}: ${link.description}${link.shortcut ? `. Keyboard shortcut: ${link.shortcut}` : ''}`}
                      className={cn(
                        'flex flex-col items-center justify-center rounded-lg transition-all duration-200',
                        compact ? 'p-2' : 'p-3',
                        'border hover:shadow-lg focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2'
                      )}
                      style={{
                        backgroundColor: `${link.color}12`,
                        borderColor: `${link.color}25`,
                        // @ts-ignore
                        '--tw-ring-color': link.color,
                      }}
                      onMouseEnter={(e) => {
                        e.currentTarget.style.backgroundColor = `${link.color}20`;
                        e.currentTarget.style.borderColor = `${link.color}40`;
                        e.currentTarget.style.boxShadow = `0 4px 12px ${link.color}30`;
                      }}
                      onMouseLeave={(e) => {
                        e.currentTarget.style.backgroundColor = `${link.color}12`;
                        e.currentTarget.style.borderColor = `${link.color}25`;
                        e.currentTarget.style.boxShadow = 'none';
                      }}
                    >
                      <div
                        className="mb-1"
                        style={{ color: link.color }}
                        aria-hidden="true"
                      >
                        {link.icon}
                      </div>
                      <span
                        className={cn(
                          'font-semibold text-foreground text-center',
                          compact ? 'text-xs' : 'text-sm'
                        )}
                      >
                        {link.label}
                      </span>
                      {!compact && link.shortcut && (
                        <span
                          className="text-muted-foreground text-[10px] font-mono mt-1 px-1.5 py-0.5 rounded bg-muted/50"
                          aria-hidden="true"
                        >
                          {link.shortcut}
                        </span>
                      )}
                    </Link>
                  </motion.div>
                </TooltipTrigger>
                <TooltipContent>
                  <p>{link.description}</p>
                  {link.shortcut && (
                    <p className="text-muted-foreground text-xs">Shortcut: {link.shortcut}</p>
                  )}
                </TooltipContent>
              </Tooltip>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

export default QuickLinks;
