import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { 
  History, 
  Plus,
  Edit,
  Trash2,
  ChevronRight,
  User,
  Calendar,
  ArrowRight,
  Loader2
} from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { ScrollArea } from './ui/scroll-area';
import { Skeleton } from './ui/skeleton';
import { EntityHistory, RevisionType, Page } from '../types';

interface EntityHistoryDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  entityName: string;
  entityId: string;
  fetchHistory: (page: number, size: number) => Promise<Page<EntityHistory>>;
}

const revisionTypeConfig: Record<RevisionType, { 
  icon: React.ComponentType<{ className?: string }>;
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

// Field name translations mapping
const fieldNameKeys: Record<string, string> = {
  status: 'history.field.status',
  priority: 'history.field.priority',
  severity: 'history.field.severity',
  assignee: 'history.field.assignee',
  title: 'history.field.title',
  description: 'history.field.description',
  appetite: 'history.field.appetite',
  phase: 'history.field.phase',
  hillPosition: 'history.field.hillPosition',
  progress: 'history.field.progress',
  estimatedHours: 'history.field.estimatedHours',
  actualHours: 'history.field.actualHours',
  environment: 'history.field.environment',
  actualBehavior: 'history.field.actualBehavior',
  expectedBehavior: 'history.field.expectedBehavior',
  stepsToReproduce: 'history.field.stepsToReproduce',
  executionStatus: 'history.field.executionStatus',
  preconditions: 'history.field.preconditions',
  testSteps: 'history.field.testSteps',
  expectedResult: 'history.field.expectedResult',
  automationStatus: 'history.field.automationStatus',
};

export function EntityHistoryDialog({ 
  open, 
  onOpenChange, 
  entityName, 
  entityId,
  fetchHistory 
}: EntityHistoryDialogProps) {
  const { t, i18n } = useTranslation();
  const [history, setHistory] = useState<EntityHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [expandedRevisions, setExpandedRevisions] = useState<Set<number>>(new Set());

  const pageSize = 10;

  const loadHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchHistory(page, pageSize);
      setHistory(response?.content || []);
      setTotalPages(response?.page?.totalPages ?? response?.totalPages ?? 0);
      setTotalElements(response?.page?.totalElements ?? response?.totalElements ?? 0);
    } catch (err) {
      console.error('Failed to load history:', err);
      setError(t('history.loadError'));
    } finally {
      setLoading(false);
    }
  }, [fetchHistory, page, t]);

  useEffect(() => {
    if (open && entityId) {
      loadHistory();
    }
  }, [open, entityId, loadHistory]);

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

  const toggleRevision = (revisionNumber: number) => {
    setExpandedRevisions(prev => {
      const next = new Set(prev);
      if (next.has(revisionNumber)) {
        next.delete(revisionNumber);
      } else {
        next.add(revisionNumber);
      }
      return next;
    });
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
    return <span className="font-medium">{value}</span>;
  };

  const RevisionIcon = ({ type }: { type: RevisionType }) => {
    const config = revisionTypeConfig[type];
    const IconComponent = config.icon;
    return <IconComponent className="h-4 w-4" />;
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[85vh]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <History className="h-5 w-5 text-primary" />
            {t('history.title', { entity: entityName })}
          </DialogTitle>
        </DialogHeader>

        <ScrollArea className="h-[60vh] pr-4">
          {loading && history.length === 0 ? (
            <div className="space-y-4">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="flex gap-4">
                  <Skeleton className="h-10 w-10 rounded-full" />
                  <div className="flex-1 space-y-2">
                    <Skeleton className="h-4 w-3/4" />
                    <Skeleton className="h-3 w-1/2" />
                  </div>
                </div>
              ))}
            </div>
          ) : error ? (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <p className="text-muted-foreground mb-4">{error}</p>
              <Button onClick={loadHistory} variant="outline">
                {t('common.tryAgain')}
              </Button>
            </div>
          ) : history.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <History className="h-12 w-12 text-muted-foreground/50 mb-4" />
              <p className="text-muted-foreground">{t('history.noHistory')}</p>
            </div>
          ) : (
            <div className="relative">
              {/* Timeline line */}
              <div className="absolute left-5 top-0 bottom-0 w-0.5 bg-border" />
              
              <div className="space-y-4">
                {history.map((entry) => {
                  const config = revisionTypeConfig[entry.revisionType];
                  const isExpanded = expandedRevisions.has(entry.revisionNumber);
                  const hasChanges = entry.changes && entry.changes.length > 0;
                  
                  return (
                    <div key={entry.revisionNumber} className="relative pl-12">
                      {/* Timeline dot */}
                      <div 
                        className={`absolute left-3 top-1 h-5 w-5 rounded-full flex items-center justify-center ${config.color} text-white`}
                      >
                        <RevisionIcon type={entry.revisionType} />
                      </div>
                      
                      <div className="bg-muted/50 rounded-lg p-4 border">
                        {/* Header */}
                        <div 
                          className={`flex items-center justify-between ${hasChanges ? 'cursor-pointer' : ''}`}
                          onClick={() => hasChanges && toggleRevision(entry.revisionNumber)}
                        >
                          <div className="flex items-center gap-3">
                            <Badge variant={config.variant}>
                              {t(config.labelKey)}
                            </Badge>
                            <span className="text-sm text-muted-foreground">
                              #{entry.revisionNumber}
                            </span>
                          </div>
                          {hasChanges && (
                            <ChevronRight 
                              className={`h-4 w-4 text-muted-foreground transition-transform ${isExpanded ? 'rotate-90' : ''}`} 
                            />
                          )}
                        </div>
                        
                        {/* Metadata */}
                        <div className="flex flex-wrap items-center gap-4 mt-2 text-sm text-muted-foreground">
                          <div className="flex items-center gap-1">
                            <User className="h-3.5 w-3.5" />
                            <span>{entry.modifiedBy}</span>
                          </div>
                          <div className="flex items-center gap-1">
                            <Calendar className="h-3.5 w-3.5" />
                            <span>{formatDateTime(entry.revisionDate)}</span>
                          </div>
                        </div>
                        
                        {/* Changes (expandable) */}
                        {isExpanded && hasChanges && (
                          <div className="mt-4 space-y-2 border-t pt-4">
                            <h4 className="text-sm font-medium mb-2">{t('history.changes')}</h4>
                            {entry.changes.map((change, changeIndex) => (
                              <div 
                                key={changeIndex} 
                                className="flex flex-col sm:flex-row sm:items-center gap-2 py-2 px-3 bg-background rounded border text-sm"
                              >
                                <span className="font-medium text-foreground min-w-[120px]">
                                  {getFieldLabel(change.fieldName)}:
                                </span>
                                <div className="flex items-center gap-2 flex-1 flex-wrap">
                                  <span className="px-2 py-0.5 bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-300 rounded text-xs">
                                    {renderValue(change.oldValue)}
                                  </span>
                                  <ArrowRight className="h-3 w-3 text-muted-foreground shrink-0" />
                                  <span className="px-2 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded text-xs">
                                    {renderValue(change.newValue)}
                                  </span>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                        
                        {/* Summary for collapsed state */}
                        {!isExpanded && hasChanges && (
                          <p className="mt-2 text-sm text-muted-foreground">
                            {t('history.changesCount', { count: entry.changes.length })}
                          </p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </ScrollArea>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-4 border-t">
            <span className="text-sm text-muted-foreground">
              {t('history.showing', { 
                start: page * pageSize + 1, 
                end: Math.min((page + 1) * pageSize, totalElements),
                total: totalElements 
              })}
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0 || loading}
              >
                {t('common.previous')}
              </Button>
              <span className="text-sm text-muted-foreground">
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1 || loading}
              >
                {t('common.next')}
              </Button>
            </div>
          </div>
        )}
        
        {/* Loading indicator for pagination */}
        {loading && history.length > 0 && (
          <div className="flex items-center justify-center py-2">
            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

export default EntityHistoryDialog;
