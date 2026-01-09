import { Badge } from '@/components/ui/badge';
import { PitchStatus } from '../types';
import { cn } from '@/lib/utils';

interface StatusChipProps {
  status: PitchStatus;
  size?: 'small' | 'medium';
}

const statusConfig: Record<PitchStatus, { variant: 'default' | 'secondary' | 'destructive' | 'outline'; label: string; className?: string }> = {
  PENDING: { variant: 'outline', label: 'Pending', className: 'border-muted-foreground/50 text-muted-foreground' },
  SHAPED: { variant: 'secondary', label: 'Shaped', className: 'bg-violet-500/15 text-violet-500 hover:bg-violet-500/20' },
  STARTED: { variant: 'secondary', label: 'Started', className: 'bg-blue-500/15 text-blue-500 hover:bg-blue-500/20' },
  IN_PROGRESS: { variant: 'default', label: 'In Progress', className: 'bg-primary/15 text-primary hover:bg-primary/20' },
  TESTING: { variant: 'secondary', label: 'Testing', className: 'bg-amber-500/15 text-amber-500 hover:bg-amber-500/20' },
  DONE: { variant: 'secondary', label: 'Done', className: 'bg-emerald-500/15 text-emerald-500 hover:bg-emerald-500/20' },
  COOLDOWN: { variant: 'secondary', label: 'Cooldown', className: 'bg-cyan-500/15 text-cyan-500 hover:bg-cyan-500/20' },
  CANCELLED: { variant: 'destructive', label: 'Cancelled', className: 'bg-destructive/15 text-destructive hover:bg-destructive/20' },
};

export default function StatusChip({ status, size = 'small' }: StatusChipProps) {
  const config = statusConfig[status];
  return (
    <Badge 
      variant={config.variant}
      className={cn(
        size === 'small' ? 'text-xs px-2 py-0.5' : 'text-sm px-2.5 py-1',
        config.className
      )}
      role="status"
      aria-label={`Status: ${config.label}`}
    >
      {config.label}
    </Badge>
  );
}
