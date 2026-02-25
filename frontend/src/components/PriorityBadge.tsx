import { useTranslation } from 'react-i18next';
import { Badge } from './ui/badge';
import { BusinessValue } from '../types';
import { cn } from '../lib/utils';

interface PriorityBadgeProps {
  priority?: BusinessValue | null;
  className?: string;
  size?: 'sm' | 'md';
}

const priorityConfig: Record<BusinessValue, { color: string; iconColor: string }> = {
  HIGH: { color: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-300 border-red-200 dark:border-red-800', iconColor: 'text-red-500' },
  MEDIUM: { color: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-300 border-yellow-200 dark:border-yellow-800', iconColor: 'text-yellow-500' },
  LOW: { color: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-300 border-green-200 dark:border-green-800', iconColor: 'text-green-500' },
};

export default function PriorityBadge({ priority, className, size = 'sm' }: PriorityBadgeProps) {
  const { t } = useTranslation();

  if (!priority) return null;

  const config = priorityConfig[priority];

  return (
    <Badge
      variant="outline"
      className={cn(
        config.color,
        size === 'sm' ? 'text-[10px] px-1.5 py-0' : 'text-xs px-2 py-0.5',
        className
      )}
    >
      {t(`priority.${priority.toLowerCase()}`)}
    </Badge>
  );
}
