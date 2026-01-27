import { useTranslation } from 'react-i18next';
import { 
  Bug, 
  Calendar, 
  User, 
  Target,
  FileText,
  Monitor,
  Tag,
  CheckCircle,
  Clock
} from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Badge } from './ui/badge';
import { Label } from './ui/label';
import { BugReport, BugStatus, BugSeverity } from '../types';

interface BugViewDialogProps {
  bug: BugReport | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const severityConfig: Record<BugSeverity, { labelKey: string; variant: 'default' | 'secondary' | 'info' | 'warning' | 'destructive' }> = {
  TRIVIAL: { labelKey: 'bugs.severity.trivial', variant: 'secondary' },
  MINOR: { labelKey: 'bugs.severity.minor', variant: 'info' },
  MAJOR: { labelKey: 'bugs.severity.major', variant: 'warning' },
  CRITICAL: { labelKey: 'bugs.severity.critical', variant: 'destructive' },
  BLOCKER: { labelKey: 'bugs.severity.blocker', variant: 'destructive' },
};

const statusConfig: Record<BugStatus, { labelKey: string; variant: 'default' | 'secondary' | 'info' | 'warning' | 'destructive' | 'success' }> = {
  OPEN: { labelKey: 'bugs.status.open', variant: 'destructive' },
  IN_PROGRESS: { labelKey: 'bugs.status.inProgress', variant: 'default' },
  RESOLVED: { labelKey: 'bugs.status.resolved', variant: 'success' },
  VERIFIED: { labelKey: 'bugs.status.verified', variant: 'success' },
  CLOSED: { labelKey: 'bugs.status.closed', variant: 'secondary' },
  REOPENED: { labelKey: 'bugs.status.reopened', variant: 'warning' },
  WONT_FIX: { labelKey: 'bugs.status.wontFix', variant: 'secondary' },
  DUPLICATE: { labelKey: 'bugs.status.duplicate', variant: 'secondary' },
};

export function BugViewDialog({ bug, open, onOpenChange }: BugViewDialogProps) {
  const { t, i18n } = useTranslation();

  if (!bug) return null;

  const formatDateTime = (dateTime: string) => {
    const d = new Date(dateTime);
    return d.toLocaleString(i18n.language, { 
      year: 'numeric', 
      month: 'short', 
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (!bug) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Bug className="h-5 w-5 text-destructive" />
            <Badge variant="outline" className="font-mono">{bug.bugKey}</Badge>
            <span className="truncate">{bug.title}</span>
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Status and Severity */}
          <div className="flex flex-wrap items-center gap-3">
            <Badge variant={statusConfig[bug.status]?.variant || 'default'}>
              {t(statusConfig[bug.status]?.labelKey || bug.status)}
            </Badge>
            <Badge variant={severityConfig[bug.severity]?.variant || 'default'}>
              {t(severityConfig[bug.severity]?.labelKey || bug.severity)}
            </Badge>
          </div>

          {/* Metadata Grid */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <User className="h-3 w-3" />
                {t('bugs.reporter')}
              </Label>
              <div className="font-medium">{bug.reporterName || t('common.unknown')}</div>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <User className="h-3 w-3" />
                {t('bugs.assignee')}
              </Label>
              <div className="font-medium">{bug.assigneeName || t('common.unassigned')}</div>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Calendar className="h-3 w-3" />
                {t('common.createdAt')}
              </Label>
              <div className="font-medium">{formatDateTime(bug.createdAt)}</div>
            </div>
            <div className="space-y-1">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Clock className="h-3 w-3" />
                {t('common.updatedAt')}
              </Label>
              <div className="font-medium">{formatDateTime(bug.updatedAt)}</div>
            </div>
          </div>

          {/* Relationships */}
          <div className="grid grid-cols-2 gap-4 border-t pt-4">
            {bug.pitchTitle && (
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground flex items-center gap-1">
                  <Target className="h-3 w-3" />
                  {t('bugs.pitch')}
                </Label>
                <div className="font-medium">{bug.pitchTitle}</div>
              </div>
            )}
            {bug.cycleName && (
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">{t('bugs.cycle')}</Label>
                <div className="font-medium">{bug.cycleName}</div>
              </div>
            )}
            {bug.teamName && (
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">{t('bugs.team')}</Label>
                <div className="font-medium">{bug.teamName}</div>
              </div>
            )}
            {bug.taskTitle && (
              <div className="space-y-1">
                <Label className="text-xs text-muted-foreground">{t('bugs.task')}</Label>
                <div className="font-medium">{bug.taskTitle}</div>
              </div>
            )}
          </div>

          {/* Description */}
          <div className="space-y-2 border-t pt-4">
            <Label className="text-xs text-muted-foreground flex items-center gap-1">
              <FileText className="h-3 w-3" />
              {t('bugs.description')}
            </Label>
            <div className="p-3 bg-muted rounded-md text-sm whitespace-pre-wrap">
              {bug.description}
            </div>
          </div>

          {/* Steps to Reproduce */}
          {bug.stepsToReproduce && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground">{t('bugs.stepsToReproduce')}</Label>
              <div className="p-3 bg-muted rounded-md text-sm whitespace-pre-wrap font-mono">
                {bug.stepsToReproduce}
              </div>
            </div>
          )}

          {/* Expected vs Actual Behavior */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {bug.expectedBehavior && (
              <div className="space-y-2">
                <Label className="text-xs text-muted-foreground text-success flex items-center gap-1">
                  <CheckCircle className="h-3 w-3" />
                  {t('bugs.expectedBehavior')}
                </Label>
                <div className="p-3 bg-success/10 border border-success/20 rounded-md text-sm whitespace-pre-wrap">
                  {bug.expectedBehavior}
                </div>
              </div>
            )}
            {bug.actualBehavior && (
              <div className="space-y-2">
                <Label className="text-xs text-muted-foreground text-destructive flex items-center gap-1">
                  <Bug className="h-3 w-3" />
                  {t('bugs.actualBehavior')}
                </Label>
                <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-md text-sm whitespace-pre-wrap">
                  {bug.actualBehavior}
                </div>
              </div>
            )}
          </div>

          {/* Environment */}
          {bug.environment && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Monitor className="h-3 w-3" />
                {t('bugs.environment')}
              </Label>
              <div className="p-3 bg-muted rounded-md text-sm font-mono">
                {bug.environment}
              </div>
            </div>
          )}

          {/* Tags */}
          {(bug.tagList?.length || bug.tags) && (
            <div className="space-y-2">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <Tag className="h-3 w-3" />
                {t('bugs.tags')}
              </Label>
              <div className="flex flex-wrap gap-1">
                {(bug.tagList || bug.tags?.split(',') || []).map((tag, idx) => (
                  <Badge key={idx} variant="outline" className="text-xs">
                    {typeof tag === 'string' ? tag.trim() : tag}
                  </Badge>
                ))}
              </div>
            </div>
          )}

          {/* Resolution */}
          {bug.resolution && (
            <div className="space-y-2 border-t pt-4">
              <Label className="text-xs text-muted-foreground flex items-center gap-1">
                <CheckCircle className="h-3 w-3" />
                {t('bugs.resolution')}
              </Label>
              <div className="p-3 bg-success/10 border border-success/20 rounded-md text-sm whitespace-pre-wrap">
                {bug.resolution}
              </div>
              {bug.resolvedAt && (
                <div className="text-xs text-muted-foreground">
                  {t('bugs.resolvedAt')}: {formatDateTime(bug.resolvedAt)}
                </div>
              )}
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default BugViewDialog;
