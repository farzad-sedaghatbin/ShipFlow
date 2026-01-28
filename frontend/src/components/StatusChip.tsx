import { Badge } from '@/components/ui/badge';
import { PitchStatus } from '../types';
import { cn } from '@/lib/utils';
import { useTranslation } from 'react-i18next';

interface StatusChipProps {
  status: PitchStatus;
  size?: 'small' | 'medium';
}

const statusConfig: Record<PitchStatus, { variant: 'default' | 'secondary' | 'destructive' | 'outline'; translationKey: string; className?: string }> = {
  PENDING: { variant: 'outline', translationKey: 'pitch.statuses.pending', className: 'border-muted-foreground/50 text-muted-foreground' },
  SHAPED: { variant: 'secondary', translationKey: 'pitch.statuses.shaped', className: 'bg-violet-500/15 text-violet-500 hover:bg-violet-500/20' },
  STARTED: { variant: 'secondary', translationKey: 'pitch.statuses.started', className: 'bg-blue-500/15 text-blue-500 hover:bg-blue-500/20' },
  IN_PROGRESS: { variant: 'default', translationKey: 'pitch.statuses.inProgress', className: 'bg-primary/15 text-primary hover:bg-primary/20' },
  TESTING: { variant: 'secondary', translationKey: 'pitch.statuses.testing', className: 'bg-amber-500/15 text-amber-500 hover:bg-amber-500/20' },
  DONE: { variant: 'secondary', translationKey: 'pitch.statuses.done', className: 'bg-emerald-500/15 text-emerald-500 hover:bg-emerald-500/20' },
  COOLDOWN: { variant: 'secondary', translationKey: 'pitch.statuses.cooldown', className: 'bg-cyan-500/15 text-cyan-500 hover:bg-cyan-500/20' },
  CANCELLED: { variant: 'destructive', translationKey: 'pitch.statuses.cancelled', className: 'bg-destructive/15 text-destructive hover:bg-destructive/20' },
};

export default function StatusChip({ status, size = 'small' }: StatusChipProps) {
  const { t } = useTranslation();
  const config = statusConfig[status];
  const label = t(config.translationKey);
  
  return (
    <Badge 
      variant={config.variant}
      className={cn(
        size === 'small' ? 'text-xs px-2 py-0.5' : 'text-sm px-2.5 py-1',
        config.className
      )}
      role="status"
      aria-label={`${t('common.status')}: ${label}`}
    >
      {label}
    </Badge>
  );
}
