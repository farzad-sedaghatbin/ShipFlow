import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { 
  Plus,
  Edit,
  Trash2,
  ArrowRight,
  Loader2,
  Activity,
  RefreshCw
} from 'lucide-react';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Skeleton } from './ui/skeleton';
import { EntityHistory, RevisionType, Page } from '../types';

interface ActivityTimelineProps {
  entityId: number;
  fetchHistory: (page: number, size: number) => Promise<Page<EntityHistory>>;
  title?: string;
  compact?: boolean;
}

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

// Field name translations mapping
const fieldNameKeys: Record<string, string> = {
  status: 'history.field.status',
  priority: 'history.field.priority',
  severity: 'history.field.severity',
  assignee: 'history.field.assignee',
  pairAssignee: 'history.field.pairAssignee',
  title: 'history.field.title',
  description: 'history.field.description',
  resolution: 'bugs.resolution',
  environment: 'history.field.environment',
  actualBehavior: 'history.field.actualBehavior',
  expectedBehavior: 'history.field.expectedBehavior',
  stepsToReproduce: 'history.field.stepsToReproduce',
  category: 'history.field.category',
  estimatedHours: 'history.field.estimatedHours',
  actualHours: 'history.field.actualHours',
};

export function ActivityTimeline({ 
  entityId, 
  fetchHistory, 
  title,
  compact = false 
}: ActivityTimelineProps) {
  const { t, i18n } = useTranslation();
  const [history, setHistory] = useState<EntityHistory[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const pageSize = compact ? 10 : 20;

  const loadHistory = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetchHistory(page, pageSize);
      setHistory(response?.content || []);
      setTotalPages(response?.totalPages || 0);
    } catch (err) {
      console.error('Failed to load history:', err);
      setError(t('history.loadError'));
    } finally {
      setLoading(false);
    }
  }, [fetchHistory, page, pageSize, t]);

  useEffect(() => {
    if (entityId) {
      loadHistory();
    }
  }, [entityId, loadHistory]);

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

  const renderContent = () => {
    if (loading && history.length === 0) {
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

    if (error) {
      return (
        <div className="flex flex-col items-center justify-center py-8 text-center">
          <p className="text-muted-foreground mb-4">{error}</p>
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

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 mt-4 pt-4 border-t">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0 || loading}
            >
              {t('common.previous')}
            </Button>
            <span className="text-xs text-muted-foreground">
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
        )}

        {loading && history.length > 0 && (
          <div className="flex items-center justify-center py-2">
            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
          </div>
        )}
      </div>
    );
  };

  if (compact) {
    return renderContent();
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="text-lg flex items-center gap-2">
          <Activity className="h-5 w-5" />
          {title || t('activity.title')}
        </CardTitle>
        <Button variant="ghost" size="sm" onClick={loadHistory} disabled={loading}>
          <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
        </Button>
      </CardHeader>
      <CardContent>
        {renderContent()}
      </CardContent>
    </Card>
  );
}

export default ActivityTimeline;
