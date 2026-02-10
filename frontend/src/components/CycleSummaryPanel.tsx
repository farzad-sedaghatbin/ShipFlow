import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { 
  RefreshCcw, 
  Download, 
  Sparkles, 
  LayoutTemplate,
  Target,
  Package,
  Scissors,
  Lightbulb,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import { Alert, AlertDescription } from './ui/alert';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from './ui/collapsible';
import { narrativeService, CycleSummary, CycleNarrative } from '../services/narrativeService';
import { cn } from '../lib/utils';
import { PermissionGate } from '../hooks/usePermission';

interface NarrativeSectionProps {
  narrative: CycleNarrative | null;
  title: string;
  icon: React.ReactNode;
  onRegenerate: () => void;
  regenerating: boolean;
}

const NarrativeSection: React.FC<NarrativeSectionProps> = ({
  narrative,
  title,
  icon,
  onRegenerate,
  regenerating
}) => {
  const { t } = useTranslation();
  const [open, setOpen] = useState(true);

  if (!narrative) {
    return (
      <Card className="border-dashed">
        <CardContent className="p-4 flex items-center justify-between">
          <div className="flex items-center gap-2 text-muted-foreground">
            {icon}
            <span>{title}</span>
          </div>
          <PermissionGate resource="REPORT" permission="CREATE">
            <Button 
              variant="outline" 
              size="sm" 
              onClick={onRegenerate}
              disabled={regenerating}
            >
              {regenerating ? (
                <RefreshCcw className="h-3 w-3 mr-1 animate-spin" />
              ) : (
                <RefreshCcw className="h-3 w-3 mr-1" />
              )}
              {t('narratives.generate')}
            </Button>
          </PermissionGate>
        </CardContent>
      </Card>
    );
  }

  return (
    <Collapsible open={open} onOpenChange={setOpen}>
      <Card>
        <CollapsibleTrigger asChild>
          <CardHeader className="cursor-pointer hover:bg-muted/30 transition-colors">
            <div className="flex items-center justify-between">
              <CardTitle className="text-sm font-medium flex items-center gap-2">
                {icon}
                {title}
              </CardTitle>
              <div className="flex items-center gap-2">
                <Badge variant="outline" className={cn(
                  "text-xs",
                  narrative.isAiGenerated 
                    ? "bg-purple-500/15 text-purple-500" 
                    : "bg-muted text-muted-foreground"
                )}>
                  {narrative.isAiGenerated ? (
                    <>
                      <Sparkles className="h-3 w-3 mr-1" />
                      {t('narratives.aiGenerated')}
                    </>
                  ) : (
                    <>
                      <LayoutTemplate className="h-3 w-3 mr-1" />
                      {t('narratives.templateBased')}
                    </>
                  )}
                </Badge>
                {open ? (
                  <ChevronUp className="h-4 w-4 text-muted-foreground" />
                ) : (
                  <ChevronDown className="h-4 w-4 text-muted-foreground" />
                )}
              </div>
            </div>
          </CardHeader>
        </CollapsibleTrigger>
        <CollapsibleContent>
          <CardContent className="pt-0">
            <div className="whitespace-pre-wrap break-words text-sm">
              {narrative.content}
            </div>
            <div className="flex items-center justify-between mt-4 pt-4 border-t">
              <span className="text-xs text-muted-foreground">
                {t('narratives.generatedAt')} {new Date(narrative.generatedAt).toLocaleString()}
              </span>
              <PermissionGate resource="REPORT" permission="CREATE">
                <Button 
                  variant="ghost" 
                  size="sm" 
                  onClick={(e) => {
                    e.stopPropagation();
                    onRegenerate();
                  }}
                  disabled={regenerating}
                >
                  {regenerating ? (
                    <RefreshCcw className="h-3 w-3 mr-1 animate-spin" />
                  ) : (
                    <RefreshCcw className="h-3 w-3 mr-1" />
                  )}
                  {t('narratives.regenerate')}
                </Button>
              </PermissionGate>
            </div>
          </CardContent>
        </CollapsibleContent>
      </Card>
    </Collapsible>
  );
};

interface CycleSummaryPanelProps {
  cycleId: number;
  className?: string;
}

export const CycleSummaryPanel: React.FC<CycleSummaryPanelProps> = ({ 
  cycleId,
  className 
}) => {
  const { t } = useTranslation();
  const [summary, setSummary] = useState<CycleSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [regenerating, setRegenerating] = useState<Record<string, boolean>>({});

  const loadSummary = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await narrativeService.getCycleSummary(cycleId);
      setSummary(data);
    } catch (err) {
      setError(t('narratives.loadError'));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSummary();
  }, [cycleId]);

  const handleRegenerate = async (type: CycleNarrative['narrativeType']) => {
    setRegenerating(prev => ({ ...prev, [type]: true }));
    try {
      const narrative = await narrativeService.regenerateNarrative(cycleId, type);
      setSummary(prev => {
        if (!prev) return prev;
        const key = type.toLowerCase().replace(/_([a-z])/g, (_: string, letter: string) => letter.toUpperCase()) as keyof CycleSummary;
        return { ...prev, [key]: narrative };
      });
    } catch (err) {
      console.error('Failed to regenerate narrative:', err);
    } finally {
      setRegenerating(prev => ({ ...prev, [type]: false }));
    }
  };

  const handleRegenerateAll = async () => {
    setRegenerating({ all: true });
    try {
      const data = await narrativeService.regenerateNarratives(cycleId);
      setSummary(data);
    } catch (err) {
      console.error('Failed to regenerate narratives:', err);
    } finally {
      setRegenerating({});
    }
  };

  const handleExport = async () => {
    try {
      const markdown = await narrativeService.exportAsMarkdown(cycleId);
      const blob = new Blob([markdown], { type: 'text/markdown' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `cycle_summary_${cycleId}.md`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Failed to export:', err);
    }
  };

  if (loading) {
    return (
      <Card className={className}>
        <CardHeader>
          <Skeleton className="h-6 w-40" />
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <Skeleton className="h-32" />
            <Skeleton className="h-32" />
            <Skeleton className="h-32" />
            <Skeleton className="h-32" />
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className={className}>
        <CardContent className="p-6">
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  if (!summary) {
    return (
      <Card className={className}>
        <CardContent className="p-6">
          <Alert>
            <AlertDescription>{t('narratives.noNarrativeYet')}</AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Sparkles className="h-5 w-5 text-primary" />
              <CardTitle className="text-lg font-semibold">
                {t('narratives.title')}
              </CardTitle>
            </div>
            <p className="text-sm text-muted-foreground">
              {summary.cycleName} • {summary.projectName}
            </p>
          </div>
          <div className="flex gap-2">
            <PermissionGate resource="REPORT" permission="CREATE">
              <Button 
                variant="outline" 
                size="sm" 
                onClick={handleRegenerateAll}
                disabled={regenerating.all}
              >
                {regenerating.all ? (
                  <RefreshCcw className="h-4 w-4 mr-1 animate-spin" />
                ) : (
                  <RefreshCcw className="h-4 w-4 mr-1" />
                )}
                {t('narratives.regenerateAll')}
              </Button>
            </PermissionGate>
            <Button variant="outline" size="sm" onClick={handleExport}>
              <Download className="h-4 w-4 mr-1" />
              {t('narratives.exportMarkdown')}
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <NarrativeSection
            narrative={summary.whatWeBet}
            title={t('narratives.whatWeBet')}
            icon={<Target className="h-4 w-4 text-blue-500" />}
            onRegenerate={() => handleRegenerate('WHAT_WE_BET')}
            regenerating={regenerating.WHAT_WE_BET || false}
          />
          
          <NarrativeSection
            narrative={summary.whatShipped}
            title={t('narratives.whatShipped')}
            icon={<Package className="h-4 w-4 text-green-500" />}
            onRegenerate={() => handleRegenerate('WHAT_SHIPPED')}
            regenerating={regenerating.WHAT_SHIPPED || false}
          />
          
          <NarrativeSection
            narrative={summary.whatWeCut}
            title={t('narratives.whatWeCut')}
            icon={<Scissors className="h-4 w-4 text-orange-500" />}
            onRegenerate={() => handleRegenerate('WHAT_WE_CUT')}
            regenerating={regenerating.WHAT_WE_CUT || false}
          />
          
          <NarrativeSection
            narrative={summary.surprises}
            title={t('narratives.surprises')}
            icon={<Lightbulb className="h-4 w-4 text-yellow-500" />}
            onRegenerate={() => handleRegenerate('SURPRISES')}
            regenerating={regenerating.SURPRISES || false}
          />
        </div>
      </CardContent>
    </Card>
  );
};

export default CycleSummaryPanel;
