import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Sparkles, RefreshCw, Loader2 } from 'lucide-react';
import { narrativeService, CycleNarrative } from '../services/narrativeService';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import { Alert, AlertDescription } from './ui/alert';
import { Markdown } from './ui/markdown';
import { cn } from '../lib/utils';
import { formatLocalizedDate } from '../utils/dateLocalization';

interface RetroSummaryPanelProps {
  cycleId: number;
  retroStatus: string;
  className?: string;
}

/**
 * Displays an AI-generated retrospective summary for a cycle.
 * Shows a "Summarize with AI" button when no summary exists yet,
 * and a "Regenerate" button once a summary has been produced.
 */
export function RetroSummaryPanel({ cycleId, retroStatus, className }: RetroSummaryPanelProps) {
  const { t, i18n } = useTranslation();
  const [summary, setSummary] = useState<CycleNarrative | null>(null);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load existing summary on mount
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    narrativeService.getRetroSummary(cycleId).then((data) => {
      if (!cancelled) {
        setSummary(data);
        setLoading(false);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [cycleId]);

  const handleGenerate = async () => {
    setGenerating(true);
    setError(null);
    try {
      const data = await narrativeService.generateRetroSummary(cycleId);
      setSummary(data);
    } catch (err: any) {
      const msg =
        err?.response?.data?.message ??
        err?.message ??
        t('common.unknownError', 'An unexpected error occurred.');
      setError(msg);
    } finally {
      setGenerating(false);
    }
  };

  const hasItems = retroStatus !== 'DRAFT';

  return (
    <Card className={cn('mt-4', className)}>
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between gap-2 flex-wrap">
          <CardTitle className="flex items-center gap-2 text-base">
            <Sparkles className="h-4 w-4 text-violet-500" />
            {t('retroSummary.title')}
          </CardTitle>

          {!loading && (
            <Button
              size="sm"
              variant={summary ? 'outline' : 'default'}
              onClick={handleGenerate}
              disabled={generating || !hasItems}
            >
              {generating ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  {t('retroSummary.generating')}
                </>
              ) : summary ? (
                <>
                  <RefreshCw className="mr-2 h-4 w-4" />
                  {t('retroSummary.regenerateButton')}
                </>
              ) : (
                <>
                  <Sparkles className="mr-2 h-4 w-4" />
                  {t('retroSummary.summarizeButton')}
                </>
              )}
            </Button>
          )}
        </div>
      </CardHeader>

      <CardContent>
        {/* Loading skeleton */}
        {loading && (
          <div className="space-y-2">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        )}

        {/* Generating skeleton */}
        {!loading && generating && (
          <div className="space-y-2">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        )}

        {/* No items warning */}
        {!loading && !generating && !hasItems && (
          <Alert>
            <AlertDescription className="text-sm text-muted-foreground">
              {t('retroSummary.noData')}
            </AlertDescription>
          </Alert>
        )}

        {/* Error state */}
        {!loading && !generating && error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {/* Summary content */}
        {!loading && !generating && summary && (
          <div className="space-y-3">
            {/* Metadata badges */}
            <div className="flex items-center gap-2 flex-wrap text-xs text-muted-foreground">
              <Badge variant={summary.isAiGenerated ? 'secondary' : 'outline'} className="text-xs">
                {summary.isAiGenerated
                  ? t('retroSummary.aiGenerated')
                  : t('retroSummary.templateGenerated')}
              </Badge>
              {summary.aiModel && (
                <span className="text-muted-foreground">{summary.aiModel}</span>
              )}
              <span>
                {t('retroSummary.generatedAt')}:{' '}
                {formatLocalizedDate(new Date(summary.generatedAt), i18n.language)}
              </span>
            </div>

            {/* Markdown content */}
            <Markdown content={summary.content} className="text-sm" />
          </div>
        )}
      </CardContent>
    </Card>
  );
}
