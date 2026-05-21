import { useTranslation } from 'react-i18next';
import { Calendar, Users, Folder, CheckCircle } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Badge } from './ui/badge';
import { Label } from './ui/label';
import { Cycle, CyclePhase } from '../types';

interface CycleViewDialogProps {
  cycle: Cycle | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const phaseConfig: Record<CyclePhase, { labelKey: string; variant: 'default' | 'secondary' | 'outline' | 'success' | 'warning' | 'destructive' }> = {
  SHAPING_BUILDING: { labelKey: 'cycles.phases.shaping_building', variant: 'default' },
  BETTING_COOLDOWN: { labelKey: 'cycles.phases.betting_cooldown', variant: 'secondary' },
};

export function CycleViewDialog({ cycle, open, onOpenChange }: CycleViewDialogProps) {
  const { t, i18n } = useTranslation();

  if (!cycle) return null;

  const formatDate = (date: string) => {
    const d = new Date(date);
    return d.toLocaleDateString(i18n.language, { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric' 
    });
  };

  const daysRemaining = () => {
    const end = new Date(cycle.endDate);
    const now = new Date();
    const diff = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    return diff > 0 ? diff : 0;
  };

  const totalDays = () => {
    const start = new Date(cycle.startDate);
    const end = new Date(cycle.endDate);
    return Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Folder className="h-5 w-5 text-primary" />
            {cycle.name}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Status Row */}
          <div className="flex items-center gap-3">
            <Badge variant={phaseConfig[cycle.phase]?.variant || 'default'}>
              {t(phaseConfig[cycle.phase]?.labelKey || cycle.phase)}
            </Badge>
            {cycle.isActive && (
              <Badge variant="success" className="gap-1">
                <CheckCircle className="h-3 w-3" />
                {t('common.active')}
              </Badge>
            )}
          </div>

          {/* Date Information */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {t('cycles.startDate')}
              </Label>
              <div className="font-medium">
                {formatDate(cycle.startDate)}
              </div>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {t('cycles.endDate')}
              </Label>
              <div className="font-medium">
                {formatDate(cycle.endDate)}
              </div>
            </div>
          </div>

          {/* Duration Info */}
          <div className="p-4 bg-muted rounded-lg">
            <div className="grid grid-cols-2 gap-4 text-center">
              <div>
                <div className="text-2xl font-bold text-primary">{totalDays()}</div>
                <div className="text-xs text-muted-foreground">{t('cycles.totalDays')}</div>
              </div>
              <div>
                <div className="text-2xl font-bold text-primary">{daysRemaining()}</div>
                <div className="text-xs text-muted-foreground">{t('cycles.daysRemaining')}</div>
              </div>
            </div>
          </div>

          {/* Counts */}
          <div className="grid grid-cols-2 gap-4">
            {cycle.pitchCount !== undefined && cycle.projectType !== 'SCRUM' && (
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">{t('cycles.pitches')}</Label>
                <div className="font-medium text-lg">{cycle.pitchCount}</div>
              </div>
            )}
            {cycle.teamCount !== undefined && (
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground flex items-center gap-1">
                  <Users className="h-3 w-3" />
                  {t('cycles.teams')}
                </Label>
                <div className="font-medium text-lg">{cycle.teamCount}</div>
              </div>
            )}
          </div>

          {/* Project Info */}
          {cycle.projectName && (
            <div className="space-y-1 border-t pt-4">
              <Label className="text-xs text-muted-foreground">{t('common.project')}</Label>
              <div className="flex items-center gap-2">
                {cycle.projectKey && (
                  <Badge variant="outline">{cycle.projectKey}</Badge>
                )}
                <span className="font-medium">{cycle.projectName}</span>
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default CycleViewDialog;
