import { useState, useEffect, useCallback, useMemo } from 'react';
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
  Clock,
  Plus,
  Edit,
  Trash2,
  ArrowRight,
  Loader2,
  MessageSquare,
  Activity,
  Image as ImageIcon,
  Video,
  Paperclip,
  Download,
  Eye,
  FolderInput,
  AlertCircle,
  ExternalLink,
} from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from './ui/tabs';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Label } from './ui/label';
import { ScrollArea } from './ui/scroll-area';
import { Skeleton } from './ui/skeleton';
import Comments from './Comments';
import { CustomFieldsSection } from './CustomFieldsSection';
import { Markdown } from './ui/markdown';
import qaTestManagementService from '../services/qaTestManagementService';
import { documentService, UploadedDocument } from '../services/documentService';
import projectService from '../services/projectService';
import { BugReport, BugStatus, BugSeverity, EntityHistory, RevisionType, UpdateBugReportRequest, ProjectMember } from '../types';
import { useAuth } from '../contexts';
import { MoveToProjectDialog } from './MoveToProjectDialog';

interface BugViewDialogProps {
  bug: BugReport | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onEdit?: (bug: BugReport) => void;
  onOpenFullPage?: (bug: BugReport) => void;
  onUpdate?: (bug: BugReport) => void;
  onMoved?: () => void;
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

export function BugViewDialog({ bug, open, onOpenChange, onEdit, onUpdate, onMoved }: BugViewDialogProps) {
  const { t, i18n } = useTranslation();
  const { user } = useAuth();
  const isAdmin = useMemo(() => user?.role === 'ADMIN', [user]);
  // Local copy of bug for optimistic inline-field updates
  const [localBug, setLocalBug] = useState<BugReport | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [activeTab, setActiveTab] = useState('details');
  const [history, setHistory] = useState<EntityHistory[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [historyPage, setHistoryPage] = useState(0);
  const [totalHistoryPages, setTotalHistoryPages] = useState(0);
  
  // Attachments state
  const [attachments, setAttachments] = useState<UploadedDocument[]>([]);
  const [attachmentsLoading, setAttachmentsLoading] = useState(false);
  const [previewAttachment, setPreviewAttachment] = useState<UploadedDocument | null>(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  // Authenticated blob URLs keyed by attachment ID (avoids auth header issues with <img>/<video> src)
  const [blobUrls, setBlobUrls] = useState<Record<number, string>>({});
  const [failedAttachments, setFailedAttachments] = useState<Set<number>>(new Set());
  const [moveDialogOpen, setMoveDialogOpen] = useState(false);

  const pageSize = 20;
  
  // Media file type helpers
  const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'];
  const VIDEO_EXTENSIONS = ['mp4', 'webm', 'mov', 'avi'];
  const isImageFile = (fileType: string) => IMAGE_EXTENSIONS.includes(fileType?.toLowerCase() || '');
  const isVideoFile = (fileType: string) => VIDEO_EXTENSIONS.includes(fileType?.toLowerCase() || '');
  const getAttachmentUrl = (attachment: UploadedDocument) => blobUrls[attachment.id] ?? '';

  // Field name translations mapping
  const fieldNameKeys: Record<string, string> = {
    status: 'history.field.status',
    priority: 'history.field.priority',
    severity: 'history.field.severity',
    assignee: 'history.field.assignee',
    title: 'history.field.title',
    description: 'history.field.description',
    resolution: 'bugs.resolution',
    environment: 'history.field.environment',
    actualBehavior: 'history.field.actualBehavior',
    expectedBehavior: 'history.field.expectedBehavior',
    stepsToReproduce: 'history.field.stepsToReproduce',
  };

  const revisionTypeConfig: Record<RevisionType, { 
    icon: typeof Plus;
    labelKey: string; 
    variant: 'default' | 'secondary' | 'info' | 'warning' | 'destructive' | 'success';
    color: string;
  }> = {
    CREATED: { 
      icon: Plus,
      labelKey: 'history.created', 
      variant: 'success',
      color: 'bg-green-500'
    },
    MODIFIED: { 
      icon: Edit,
      labelKey: 'history.modified', 
      variant: 'info',
      color: 'bg-blue-500'
    },
    DELETED: { 
      icon: Trash2,
      labelKey: 'history.deleted', 
      variant: 'destructive',
      color: 'bg-red-500'
    },
  };

  const loadHistory = useCallback(async () => {
    if (!bug) return;
    setHistoryLoading(true);
    setHistoryError(null);
    try {
      const response = await qaTestManagementService.getBugReportHistory(bug.id, historyPage, pageSize);
      setHistory(response.data?.content || []);
      setTotalHistoryPages(response.data?.page?.totalPages ?? response.data?.totalPages ?? 0);
    } catch (err) {
      console.error('Failed to load history:', err);
      setHistoryError(t('history.loadError'));
    } finally {
      setHistoryLoading(false);
    }
  }, [bug, historyPage, t]);

  useEffect(() => {
    if (open && bug && activeTab === 'activity') {
      loadHistory();
    }
  }, [open, bug, activeTab, loadHistory]);

  // Load attachments when dialog opens, then fetch authenticated blob URLs
  const loadAttachments = useCallback(async () => {
    if (!bug) return;
    setAttachmentsLoading(true);
    try {
      const response = await documentService.getBugAttachments(bug.id);
      const list = response.data || [];
      setAttachments(list);
      // Fetch blob URLs via axios so the Authorization header is included
      const failed = new Set<number>();
      const entries = await Promise.all(
        list.map(async (a) => {
          try {
            const url = await documentService.fetchAttachmentBlobUrl(a.id);
            return [a.id, url] as [number, string];
          } catch {
            failed.add(a.id);
            return null;
          }
        })
      );
      setBlobUrls(Object.fromEntries(entries.filter((e): e is [number, string] => e !== null)));
      setFailedAttachments(failed);
    } catch (err) {
      console.error('Failed to load attachments:', err);
      setAttachments([]);
    } finally {
      setAttachmentsLoading(false);
    }
  }, [bug]);

  useEffect(() => {
    if (open && bug) {
      loadAttachments();
    }
  }, [open, bug, loadAttachments]);

  // Reset state when dialog closes or bug changes; revoke blob URLs to free memory
  useEffect(() => {
    if (!open) {
      setActiveTab('details');
      setHistory([]);
      setHistoryPage(0);
      setAttachments([]);
      setPreviewAttachment(null);
      setFailedAttachments(new Set());
      setMembers([]);
      setBlobUrls((prev) => {
        Object.values(prev).forEach(URL.revokeObjectURL);
        return {};
      });
    }
  }, [open]);

  // Keep localBug in sync with incoming prop
  useEffect(() => { setLocalBug(bug); }, [bug]);

  // Fetch project members for assignee picker
  useEffect(() => {
    if (open && bug?.projectId) {
      projectService.getMembers(bug.projectId)
        .then(data => setMembers(data))
        .catch(() => {});
    }
  }, [open, bug?.projectId]);

  // Hooks must be called unconditionally — keep all of these above the early return.
  const isReporter = useMemo(
    () => !!user && !!bug && (user.userId === bug.reporterId || user.username === bug.reporterName),
    [user, bug]
  );
  const canEditDetails = isAdmin || isReporter;

  const handleInlineUpdate = useCallback(async (patch: UpdateBugReportRequest) => {
    const current = localBug ?? bug;
    if (!current) return;
    setIsSaving(true);
    try {
      const res = await qaTestManagementService.updateBugReport(current.id, patch);
      setLocalBug(res.data);
      onUpdate?.(res.data);
    } catch {
      setLocalBug(bug);
    } finally {
      setIsSaving(false);
    }
  }, [localBug, bug, onUpdate]);

  const handleQaAssigneeUpdate = useCallback(async (qaAssigneeId: number | null) => {
    const current = localBug ?? bug;
    if (!current) return;
    setIsSaving(true);
    try {
      const res = await qaTestManagementService.updateBugQaAssignee(current.id, qaAssigneeId);
      setLocalBug(res.data);
      onUpdate?.(res.data);
    } catch {
      setLocalBug(bug);
    } finally {
      setIsSaving(false);
    }
  }, [localBug, bug, onUpdate]);

  if (!bug) return null;
  // After the guard, bug is non-null so effectiveBug is also non-null.
  const effectiveBug = localBug ?? bug;

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

  const formatRelativeTime = (dateTime: string) => {
    const d = new Date(dateTime);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return t('common.justNow');
    if (diffMins < 60) return t('common.minutesAgo', { count: diffMins });
    if (diffHours < 24) return t('common.hoursAgo', { count: diffHours });
    if (diffDays < 7) return t('common.daysAgo', { count: diffDays });
    return formatDateTime(dateTime);
  };

  const getFieldLabel = (fieldName: string): string => {
    const key = fieldNameKeys[fieldName];
    if (key) {
      return t(key);
    }
    // Fallback: convert camelCase to Title Case
    return fieldName.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase());
  };

  const renderValue = (value: string | null | undefined): React.ReactNode => {
    if (value === null || value === undefined || value === '') {
      return <span className="text-muted-foreground italic">{t('history.emptyValue')}</span>;
    }
    // Truncate long values
    const displayValue = value.length > 50 ? value.substring(0, 50) + '...' : value;
    return <span className="font-medium">{displayValue}</span>;
  };

  const renderActivityTimeline = () => {
    if (historyLoading && history.length === 0) {
      return (
        <div className="space-y-4 p-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="flex gap-3">
              <Skeleton className="h-8 w-8 rounded-full shrink-0" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/2" />
              </div>
            </div>
          ))}
        </div>
      );
    }

    if (historyError) {
      return (
        <div className="flex flex-col items-center justify-center py-8 text-center">
          <p className="text-muted-foreground mb-4">{historyError}</p>
          <Button onClick={loadHistory} variant="outline" size="sm">
            {t('common.tryAgain')}
          </Button>
        </div>
      );
    }

    if (history.length === 0) {
      return (
        <div className="flex flex-col items-center justify-center py-8 text-center">
          <Activity className="h-10 w-10 text-muted-foreground/50 mb-3" />
          <p className="text-muted-foreground text-sm">{t('history.noHistory')}</p>
        </div>
      );
    }

    return (
      <div className="relative">
        {/* Timeline line */}
        <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-border" />
        
        <div className="space-y-1">
          {history.map((entry) => {
            const config = revisionTypeConfig[entry.revisionType];
            const IconComponent = config.icon;
            const hasChanges = entry.changes && entry.changes.length > 0;
            
            return (
              <div key={entry.revisionNumber} className="relative pl-10 py-3 hover:bg-muted/30 rounded-lg transition-colors">
                {/* Timeline dot */}
                <div 
                  className={`absolute left-2 top-4 h-5 w-5 rounded-full flex items-center justify-center ${config.color} text-white shadow-sm`}
                >
                  <IconComponent className="h-3 w-3" />
                </div>
                
                {/* Activity content */}
                <div className="space-y-1">
                  {/* Header line */}
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-medium text-sm">{entry.modifiedBy}</span>
                    <Badge variant={config.variant} className="text-xs px-1.5 py-0">
                      {t(config.labelKey)}
                    </Badge>
                    <span className="text-xs text-muted-foreground">
                      {formatRelativeTime(entry.revisionDate)}
                    </span>
                  </div>
                  
                  {/* Changes */}
                  {hasChanges && (
                    <div className="space-y-1 mt-2">
                      {entry.changes.map((change, changeIndex) => (
                        <div 
                          key={changeIndex} 
                          className="flex items-center gap-2 text-sm flex-wrap"
                        >
                          <span className="text-muted-foreground">
                            {getFieldLabel(change.fieldName)}:
                          </span>
                          <span className="px-1.5 py-0.5 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 rounded text-xs line-through">
                            {renderValue(change.oldValue)}
                          </span>
                          <ArrowRight className="h-3 w-3 text-muted-foreground shrink-0" />
                          <span className="px-1.5 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded text-xs">
                            {renderValue(change.newValue)}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Load more / pagination */}
        {totalHistoryPages > 1 && (
          <div className="flex items-center justify-center gap-2 mt-4 pt-4 border-t">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setHistoryPage(p => Math.max(0, p - 1))}
              disabled={historyPage === 0 || historyLoading}
            >
              {t('common.previous')}
            </Button>
            <span className="text-xs text-muted-foreground">
              {historyPage + 1} / {totalHistoryPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setHistoryPage(p => Math.min(totalHistoryPages - 1, p + 1))}
              disabled={historyPage >= totalHistoryPages - 1 || historyLoading}
            >
              {t('common.next')}
            </Button>
          </div>
        )}

        {historyLoading && history.length > 0 && (
          <div className="flex items-center justify-center py-2">
            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
          </div>
        )}
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl max-h-[90vh] flex flex-col">
        <DialogHeader className="shrink-0">
          {/* Title row — pr-10 avoids Radix close button overlap */}
          <DialogTitle className="pr-10">
            <div className="flex items-center gap-2 min-w-0">
              <Bug className="h-5 w-5 text-destructive shrink-0" />
              <Badge variant="outline" className="font-mono shrink-0">{effectiveBug.bugKey}</Badge>
              <span className="truncate flex-1">{effectiveBug.title}</span>
              <button
                type="button"
                title={t('common.openInNewTab', 'Open in new tab')}
                className="shrink-0 text-muted-foreground hover:text-foreground transition-colors"
                onClick={() => window.open(`/qa/bug-reports/${effectiveBug.id}`, '_blank')}
              >
                <ExternalLink className="h-4 w-4" />
              </button>
            </div>
          </DialogTitle>

          {/* Action row — inline selects + Edit (reporter/admin) + Move (admin) */}
          <div className="flex flex-wrap items-center gap-2 pt-2">
            {/* Inline status select */}
            <Select
              value={effectiveBug.status}
              onValueChange={(v) => handleInlineUpdate({ status: v as BugStatus })}
              disabled={isSaving}
            >
              <SelectTrigger className="h-6 w-auto min-w-[7rem] text-xs border-0 px-2 py-0 bg-transparent focus:ring-0">
                <Badge variant={statusConfig[effectiveBug.status]?.variant || 'default'} className="pointer-events-none">
                  {t(statusConfig[effectiveBug.status]?.labelKey || effectiveBug.status)}
                </Badge>
              </SelectTrigger>
              <SelectContent>
                {(Object.keys(statusConfig) as BugStatus[]).map((s) => (
                  <SelectItem key={s} value={s}>
                    <Badge variant={statusConfig[s].variant} className="text-xs">
                      {t(statusConfig[s].labelKey)}
                    </Badge>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            {/* Inline severity select */}
            <Select
              value={effectiveBug.severity}
              onValueChange={(v) => handleInlineUpdate({ severity: v as BugSeverity })}
              disabled={isSaving}
            >
              <SelectTrigger className="h-6 w-auto min-w-[6rem] text-xs border-0 px-2 py-0 bg-transparent focus:ring-0">
                <Badge variant={severityConfig[effectiveBug.severity]?.variant || 'default'} className="pointer-events-none">
                  {t(severityConfig[effectiveBug.severity]?.labelKey || effectiveBug.severity)}
                </Badge>
              </SelectTrigger>
              <SelectContent>
                {(Object.keys(severityConfig) as BugSeverity[]).map((s) => (
                  <SelectItem key={s} value={s}>
                    <Badge variant={severityConfig[s].variant} className="text-xs">
                      {t(severityConfig[s].labelKey)}
                    </Badge>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>

            <div className="flex items-center gap-2 ml-auto">
              {isAdmin && (
                <Button
                  variant="outline"
                  size="sm"
                  className="h-6 text-xs"
                  onClick={() => setMoveDialogOpen(true)}
                >
                  <FolderInput className="h-3 w-3 mr-1" />
                  {t('moveToProject.confirm')}
                </Button>
              )}
              {canEditDetails && onEdit && (
                <Button
                  variant="outline"
                  size="sm"
                  className="h-6 text-xs gap-1"
                  onClick={() => onEdit(effectiveBug)}
                >
                  <Edit className="h-3 w-3" />
                  {t('common.edit', 'Edit')}
                </Button>
              )}
            </div>
          </div>
        </DialogHeader>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 flex flex-col min-h-0">
          <TabsList className="grid w-full grid-cols-3 shrink-0">
            <TabsTrigger value="details" className="flex items-center gap-2">
              <FileText className="h-4 w-4" />
              {t('common.details')}
            </TabsTrigger>
            <TabsTrigger value="activity" className="flex items-center gap-2">
              <Activity className="h-4 w-4" />
              {t('activity.title')}
            </TabsTrigger>
            <TabsTrigger value="comments" className="flex items-center gap-2">
              <MessageSquare className="h-4 w-4" />
              {t('comments.title')}
            </TabsTrigger>
          </TabsList>

          <div className="flex-1 min-h-0 mt-4">
            <TabsContent value="details" className="h-full m-0">
              <ScrollArea className="h-[55vh] pr-4">
                <div className="space-y-6">
                  {/* Metadata Grid */}
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground flex items-center gap-1">
                        <User className="h-3 w-3" />
                        {t('bugs.reporter')}
                      </Label>
                      <div className="font-medium">{effectiveBug.reporterName || t('common.unknown')}</div>
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground flex items-center gap-1">
                        <User className="h-3 w-3" />
                        {t('bugs.assignee')}
                      </Label>
                      {members.length > 0 ? (
                        <Select
                          value={effectiveBug.assigneeId?.toString() ?? '__none__'}
                          onValueChange={(v) =>
                            handleInlineUpdate({ assigneeId: v === '__none__' ? undefined : Number(v) })
                          }
                          disabled={isSaving}
                        >
                          <SelectTrigger className="h-7 text-sm font-medium border-0 px-0 shadow-none focus:ring-0 -ml-0.5 w-full">
                            <SelectValue placeholder={t('common.unassigned')} />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="__none__">{t('common.unassigned')}</SelectItem>
                            {members.filter(m => m.personId != null).map((m) => (
                              <SelectItem key={m.userId} value={m.personId!.toString()}>
                                {m.username}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      ) : (
                        <div className="font-medium">{effectiveBug.assigneeName || t('common.unassigned')}</div>
                      )}
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground flex items-center gap-1">
                        <User className="h-3 w-3" />
                        {t('bugs.qaAssignee')}
                      </Label>
                      {members.length > 0 ? (
                        <Select
                          value={effectiveBug.qaAssigneeId?.toString() ?? '__none__'}
                          onValueChange={(v) =>
                            handleQaAssigneeUpdate(v === '__none__' ? null : Number(v))
                          }
                          disabled={isSaving}
                        >
                          <SelectTrigger className="h-7 text-sm font-medium border-0 px-0 shadow-none focus:ring-0 -ml-0.5 w-full">
                            <SelectValue placeholder={t('common.unassigned')} />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="__none__">{t('common.unassigned')}</SelectItem>
                            {members.filter(m => m.personId != null).map((m) => (
                              <SelectItem key={m.userId} value={m.personId!.toString()}>
                                {m.username}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      ) : (
                        <div className="font-medium">{effectiveBug.qaAssigneeName || t('common.unassigned')}</div>
                      )}
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground flex items-center gap-1">
                        <Calendar className="h-3 w-3" />
                        {t('common.createdAt')}
                      </Label>
                      <div className="font-medium">{formatDateTime(effectiveBug.createdAt)}</div>
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {t('common.updatedAt')}
                      </Label>
                      <div className="font-medium">{formatDateTime(effectiveBug.updatedAt)}</div>
                    </div>
                  </div>

                  {/* Relationships */}
                  {(bug.pitchTitle || bug.cycleName || bug.teamName || bug.taskTitle) && (
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
                  )}

                  {/* Description */}
                  <div className="space-y-2 border-t pt-4">
                    <Label className="text-xs text-muted-foreground flex items-center gap-1">
                      <FileText className="h-3 w-3" />
                      {t('bugs.description')}
                    </Label>
                    <div className="p-3 bg-muted rounded-md text-sm">
                      <Markdown content={bug.description} />
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

                  {/* Attachments */}
                  <div className="space-y-2 border-t pt-4">
                    <Label className="text-xs text-muted-foreground flex items-center gap-1">
                      <Paperclip className="h-3 w-3" />
                      {t('bugAttachments.title')}
                    </Label>
                    {attachmentsLoading ? (
                      <div className="flex items-center gap-2 py-4">
                        <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                        <span className="text-sm text-muted-foreground">{t('common.loading')}</span>
                      </div>
                    ) : attachments.length > 0 ? (
                      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                        {attachments.map((attachment) => (
                          <button
                            type="button"
                            key={attachment.id}
                            className="relative group rounded-lg overflow-hidden border bg-muted/30 cursor-pointer hover:ring-2 hover:ring-ring focus:outline-none focus:ring-2 focus:ring-ring w-full"
                            onClick={() => {
                              if (!failedAttachments.has(attachment.id)) {
                                setPreviewAttachment(attachment);
                                setPreviewOpen(true);
                              }
                            }}
                          >
                            {failedAttachments.has(attachment.id) ? (
                              <div className="w-full h-20 flex flex-col items-center justify-center bg-muted/50 gap-1">
                                <AlertCircle className="h-6 w-6 text-muted-foreground" />
                                <span className="text-[10px] text-muted-foreground">{t('bugAttachments.unavailable', 'Unavailable')}</span>
                              </div>
                            ) : isImageFile(attachment.fileType) ? (
                              <img
                                src={getAttachmentUrl(attachment)}
                                alt={attachment.originalFileName}
                                className="w-full h-20 object-cover"
                              />
                            ) : isVideoFile(attachment.fileType) ? (
                              <div className="w-full h-20 flex items-center justify-center bg-muted">
                                <Video className="h-8 w-8 text-muted-foreground" />
                              </div>
                            ) : (
                              <div className="w-full h-20 flex items-center justify-center bg-muted">
                                <ImageIcon className="h-8 w-8 text-muted-foreground" />
                              </div>
                            )}
                            
                            {/* Overlay with view icon */}
                            <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center pointer-events-none">
                              <Eye className="h-6 w-6 text-white" />
                            </div>
                            
                            {/* File name */}
                            <div className="absolute bottom-0 left-0 right-0 bg-black/60 px-2 py-1 pointer-events-none">
                              <p className="text-[10px] text-white truncate">
                                {attachment.originalFileName}
                              </p>
                            </div>
                          </button>
                        ))}
                      </div>
                    ) : (
                      <p className="text-sm text-muted-foreground py-2">{t('bugAttachments.noAttachments')}</p>
                    )}
                  </div>

                  {/* Custom Fields */}
                  <CustomFieldsSection
                    entityType="BUG"
                    entityId={bug.id}
                    projectId={bug.projectId}
                  />

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
              </ScrollArea>
            </TabsContent>

            <TabsContent value="activity" className="h-full m-0">
              <ScrollArea className="h-[55vh] pr-4">
                {renderActivityTimeline()}
              </ScrollArea>
            </TabsContent>

            <TabsContent value="comments" className="h-full m-0">
              <ScrollArea className="h-[55vh] pr-4">
                <Comments 
                  entityType="bug" 
                  entityId={bug.id}
                />
              </ScrollArea>
            </TabsContent>
          </div>
        </Tabs>
      </DialogContent>

      {/* Attachment Preview Dialog */}
      <Dialog open={previewOpen} onOpenChange={setPreviewOpen}>
        <DialogContent className="max-w-4xl max-h-[90vh]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {previewAttachment && isImageFile(previewAttachment.fileType) && (
                <ImageIcon className="h-5 w-5" />
              )}
              {previewAttachment && isVideoFile(previewAttachment.fileType) && (
                <Video className="h-5 w-5" />
              )}
              {previewAttachment?.originalFileName}
            </DialogTitle>
          </DialogHeader>
          <div className="flex items-center justify-center overflow-auto max-h-[70vh]">
            {previewAttachment && isImageFile(previewAttachment.fileType) && (
              <img
                src={getAttachmentUrl(previewAttachment)}
                alt={previewAttachment.originalFileName}
                className="max-w-full max-h-full object-contain"
              />
            )}
            {previewAttachment && isVideoFile(previewAttachment.fileType) && (
              <video
                src={getAttachmentUrl(previewAttachment)}
                controls
                className="max-w-full max-h-full"
              >
                {t('bugAttachments.videoNotSupported')}
              </video>
            )}
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                if (previewAttachment) {
                  documentService.downloadAttachment(previewAttachment.id, previewAttachment.originalFileName);
                }
              }}
            >
              <Download className="h-4 w-4 mr-2" />
              {t('common.download')}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {isAdmin && bug && (
        <MoveToProjectDialog
          open={moveDialogOpen}
          onOpenChange={setMoveDialogOpen}
          entityType="bug"
          entityId={bug.id}
          entityTitle={bug.title}
          currentProjectId={bug.projectId}
          onSuccess={() => { onOpenChange(false); onMoved?.(); }}
        />
      )}
    </Dialog>
  );
}

// Re-export with move dialog wired; callers that already use BugViewDialog get the move button for free.
export default BugViewDialog;
