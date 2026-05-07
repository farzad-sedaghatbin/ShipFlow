import { useTranslation } from 'react-i18next';
import { 
  Target, 
  Calendar, 
  Clock, 
  Users, 
  AlertTriangle,
  FileText,
  Link as LinkIcon,
  Ban,
  Lightbulb,
  AlertCircle
} from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Badge } from './ui/badge';
import { Label } from './ui/label';
import { Progress } from './ui/progress';
import { Pitch, PitchStatus } from '../types';

interface PitchViewDialogProps {
  pitch: Pitch | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const statusConfig: Record<PitchStatus, { labelKey: string; variant: 'default' | 'secondary' | 'outline' | 'success' | 'warning' | 'destructive' | 'info' }> = {
  IDEA: { labelKey: 'pitches.status.idea', variant: 'outline' },
  DRAFT: { labelKey: 'pitches.status.draft', variant: 'secondary' },
  PENDING: { labelKey: 'pitches.status.pending', variant: 'secondary' },
  SHAPED: { labelKey: 'pitches.status.shaped', variant: 'info' },
  STARTED: { labelKey: 'pitches.status.started', variant: 'default' },
  IN_PROGRESS: { labelKey: 'pitches.status.inProgress', variant: 'warning' },
  TESTING: { labelKey: 'pitches.status.testing', variant: 'info' },
  DONE: { labelKey: 'pitches.status.done', variant: 'success' },
  COOLDOWN: { labelKey: 'pitches.status.cooldown', variant: 'secondary' },
  CANCELLED: { labelKey: 'pitches.status.cancelled', variant: 'destructive' },
};

export function PitchViewDialog({ pitch, open, onOpenChange }: PitchViewDialogProps) {
  const { t, i18n } = useTranslation();

  if (!pitch) return null;

  const formatDate = (date: string) => {
    const d = new Date(date);
    return d.toLocaleDateString(i18n.language, { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric' 
    });
  };

  const progressPercent = pitch.progressPercentage ?? 0;
  const hoursSpent = pitch.totalHoursSpent ?? 0;
  const appetiteHours = pitch.appetiteHours ?? ((pitch.appetiteDays ?? 0) * 8);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Target className="h-5 w-5 text-primary" />
            {pitch.title}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Status and Team */}
          <div className="flex flex-wrap items-center gap-3">
            <Badge variant={statusConfig[pitch.status]?.variant || 'default'}>
              {t(statusConfig[pitch.status]?.labelKey || pitch.status)}
            </Badge>
            {pitch.teamName && (
              <Badge variant="outline" className="gap-1">
                <Users className="h-3 w-3" />
                {pitch.teamName}
              </Badge>
            )}
          </div>

          {/* Progress */}
          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <Label className="text-xs text-muted-foreground">{t('pitches.progress')}</Label>
              <span className="font-medium">{progressPercent}%</span>
            </div>
            <Progress value={progressPercent} className="h-2" />
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span className="flex items-center gap-1">
                <Clock className="h-3 w-3" />
                {hoursSpent}h / {appetiteHours}h
              </span>
              <span>{pitch.appetiteDays} {t('pitches.days')}</span>
            </div>
          </div>

          {/* Metadata Grid */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {t('common.createdAt')}
              </Label>
              <div className="font-medium">{formatDate(pitch.createdAt)}</div>
            </div>
            {pitch.cycleName && (
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">{t('pitches.cycle')}</Label>
                <div className="font-medium">{pitch.cycleName}</div>
              </div>
            )}
            {pitch.projectName && (
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">{t('common.project')}</Label>
                <div className="flex items-center gap-2">
                  {pitch.projectKey && <Badge variant="outline">{pitch.projectKey}</Badge>}
                  <span className="font-medium">{pitch.projectName}</span>
                </div>
              </div>
            )}
          </div>

          {/* Description */}
          {pitch.description && (
            <div className="space-y-2 border-t pt-4">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <FileText className="h-3 w-3" />
                {t('pitches.description')}
              </Label>
              <div className="p-3 bg-muted rounded-md text-sm whitespace-pre-wrap">
                {pitch.description}
              </div>
            </div>
          )}

          {/* Shape Up Methodology Fields */}
          {pitch.problemStatement && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <AlertCircle className="h-3 w-3" />
                {t('pitches.problemStatement')}
              </Label>
              <div className="p-3 bg-muted rounded-md text-sm whitespace-pre-wrap">
                {pitch.problemStatement}
              </div>
            </div>
          )}

          {pitch.solution && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Lightbulb className="h-3 w-3" />
                {t('pitches.solution')}
              </Label>
              <div className="p-3 bg-muted rounded-md text-sm whitespace-pre-wrap">
                {pitch.solution}
              </div>
            </div>
          )}

          {pitch.rabbitHoles && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground flex items-center gap-1 text-warning">
                <AlertTriangle className="h-3 w-3" />
                {t('pitches.rabbitHoles')}
              </Label>
              <div className="p-3 bg-warning/10 border border-warning/20 rounded-md text-sm whitespace-pre-wrap">
                {pitch.rabbitHoles}
              </div>
            </div>
          )}

          {pitch.risks && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground flex items-center gap-1 text-destructive">
                <AlertTriangle className="h-3 w-3" />
                {t('pitches.risks')}
              </Label>
              <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-md text-sm whitespace-pre-wrap">
                {pitch.risks}
              </div>
            </div>
          )}

          {pitch.noGos && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Ban className="h-3 w-3" />
                {t('pitches.noGos')}
              </Label>
              <div className="p-3 bg-muted rounded-md text-sm whitespace-pre-wrap">
                {pitch.noGos}
              </div>
            </div>
          )}

          {pitch.wireframeLinks && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <LinkIcon className="h-3 w-3" />
                {t('pitches.wireframes')}
              </Label>
              <div className="p-3 bg-muted rounded-md text-sm">
                {pitch.wireframeLinks.split(',').map((link, idx) => (
                  <a
                    key={idx}
                    href={link.trim()}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="block text-primary hover:underline"
                  >
                    {link.trim()}
                  </a>
                ))}
              </div>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default PitchViewDialog;
