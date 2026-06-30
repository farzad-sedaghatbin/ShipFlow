import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDateTime } from '../utils/dateLocalization';
import { 
  AlertTriangle, 
  Lightbulb, 
  ThumbsUp, 
  ChevronDown, 
  ChevronUp, 
  RefreshCw,
  Sparkles,
  Bot,
  Loader2
} from 'lucide-react';
import {
  riskService,
  PitchRiskDTO,
  RiskLevel,
  RiskBand,
  getRiskScoreColor,
  formatRiskCategory,
  fetchPitchRiskAsync,
  JobStatusResponse,
} from '../services/riskService';
import { Info } from 'lucide-react';
import { RiskFeedbackForm } from './RiskFeedbackForm';
import { RiskQA } from './RiskQA';
import { Card, CardContent } from './ui/card';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Progress } from './ui/progress';
import { Skeleton } from './ui/skeleton';
import { Alert, AlertDescription } from './ui/alert';
import { Separator } from './ui/separator';
import { Markdown } from './ui/markdown';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from './ui/collapsible';
import { cn } from '../lib/utils';

interface RiskInsightsCardProps {
  pitchId: number;
  onError?: (error: string) => void;
}

export default function RiskInsightsCard({ pitchId, onError }: RiskInsightsCardProps) {
  const { i18n, t } = useTranslation();
  const [riskData, setRiskData] = useState<PitchRiskDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiJobStatus, setAiJobStatus] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [expandedSection, setExpandedSection] = useState<string | null>('insights');

  useEffect(() => {
    loadRiskAnalysis();
  }, [pitchId]);

  const loadRiskAnalysis = async () => {
    try {
      setLoading(true);
      // First load fast data (rule-based) - immediate response
      const fastResponse = await riskService.getPitchRisk(pitchId);
      setRiskData(fastResponse.data);
      setLoading(false);
      
      // Then load AI-enhanced data asynchronously with polling
      // This prevents blocking if AI is slow or not cached
      setAiLoading(true);
      setAiJobStatus('Starting AI analysis...');
      try {
        const aiResult = await fetchPitchRiskAsync(pitchId, (status: JobStatusResponse) => {
          // Update UI with job progress
          if (status.status === 'PENDING') {
            setAiJobStatus('Queued for AI analysis...');
          } else if (status.status === 'PROCESSING') {
            setAiJobStatus('AI analyzing pitch risks...');
          }
        });
        
        if (aiResult) {
          setRiskData(aiResult);
          setAiJobStatus(null);
        } else {
          console.warn('AI risk analysis unavailable, using rule-based analysis');
          setAiJobStatus(null);
        }
      } catch (aiError) {
        console.warn('AI risk analysis failed, using rule-based analysis');
        setAiJobStatus(null);
      } finally {
        setAiLoading(false);
      }
    } catch (error: any) {
      console.error('Failed to load risk analysis:', error);
      onError?.(t('errors.loadRiskAnalysisFailed'));
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      setAiJobStatus('Refreshing AI analysis...');
      
      // Use async refresh for non-blocking behavior
      const aiResult = await fetchPitchRiskAsync(pitchId, (status: JobStatusResponse) => {
        if (status.status === 'PROCESSING') {
          setAiJobStatus('AI re-analyzing pitch...');
        }
      });
      
      if (aiResult) {
        setRiskData(aiResult);
      }
      setAiJobStatus(null);
    } catch (error: any) {
      console.error('Failed to refresh risk analysis:', error);
      onError?.(t('errors.refreshRiskAnalysisFailed'));
      setAiJobStatus(null);
    } finally {
      setRefreshing(false);
    }
  };

  const toggleSection = (section: string) => {
    setExpandedSection(expandedSection === section ? null : section);
  };

  const getRiskLevelLabel = (level: RiskLevel): string => {
    switch (level) {
      case 'LOW':
        return t('risk.level.lowRisk');
      case 'MEDIUM':
        return t('risk.level.mediumRisk');
      case 'HIGH':
        return t('risk.level.highRisk');
      case 'CRITICAL':
        return t('risk.level.criticalRisk');
      default:
        return t('risk.level.unknown');
    }
  };

  const getRiskLevelShortLabel = (level: RiskLevel): string => {
    switch (level) {
      case 'LOW':
        return t('risk.level.low');
      case 'MEDIUM':
        return t('risk.level.medium');
      case 'HIGH':
        return t('risk.level.high');
      case 'CRITICAL':
        return t('risk.level.critical');
      default:
        return t('risk.level.unknown');
    }
  };

  const bandColor = (level: RiskLevel): string => {
    switch (level) {
      case 'LOW':
        return '#388e3c';
      case 'MEDIUM':
        return '#fbc02d';
      case 'HIGH':
        return '#f57c00';
      case 'CRITICAL':
        return '#d32f2f';
      default:
        return '#9e9e9e';
    }
  };

  const activeBand: RiskBand | undefined = riskData?.explanation?.bands.find((b) => b.active);

  const getRiskLevelBadgeClass = (level: RiskLevel): string => {
    switch (level) {
      case 'LOW':
        return 'bg-green-500/15 text-green-500 border-green-500/30';
      case 'MEDIUM':
        return 'bg-yellow-500/15 text-yellow-500 border-yellow-500/30';
      case 'HIGH':
        return 'bg-orange-500/15 text-orange-500 border-orange-500/30';
      case 'CRITICAL':
        return 'bg-red-500/15 text-red-500 border-red-500/30';
      default:
        return '';
    }
  };

  if (loading) {
    return (
      <Card>
        <CardContent className="p-6">
          <div className="flex items-center gap-4 mb-4">
            <Skeleton className="h-16 w-16 rounded-full" />
            <div className="flex-1">
              <Skeleton className="h-5 w-2/5 mb-2" />
              <Skeleton className="h-4 w-1/3" />
            </div>
          </div>
          <Skeleton className="h-24 w-full rounded" />
        </CardContent>
      </Card>
    );
  }

  if (!riskData) {
    return (
      <Card>
        <CardContent className="p-6">
          <Alert>
            <AlertTriangle className="h-4 w-4" />
            <AlertDescription>
              Unable to load risk analysis. Please try again later.
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  if (riskData.errorMessage) {
    return (
      <Card>
        <CardContent className="p-6 space-y-4">
          <Alert variant="destructive">
            <AlertDescription>{riskData.errorMessage}</AlertDescription>
          </Alert>
          <Button variant="outline" onClick={handleRefresh}>
            <RefreshCw className="h-4 w-4 mr-2" />
            Retry Analysis
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent className="p-6">
        {/* Header with Risk Score */}
        <div className="flex justify-between items-start mb-6">
          <div>
            <div className="flex items-center gap-2 mb-1">
              {aiLoading ? (
                <Loader2 className="h-5 w-5 animate-spin text-primary" />
              ) : (
                <Sparkles className={cn("h-5 w-5", riskData.aiEnabled ? "text-primary" : "text-muted-foreground")} />
              )}
              <h3 className="text-lg font-semibold">AI Risk Advisor</h3>
              {riskData.aiEnabled && (
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Badge variant="outline" className="gap-1 bg-primary/10 text-primary border-primary/30">
                        <Bot className="h-3 w-3" />
                        AI
                      </Badge>
                    </TooltipTrigger>
                    <TooltipContent>AI-powered risk advisor using Mistral</TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              )}
              {aiLoading && (
                <Badge variant="outline" className="bg-blue-500/10 text-blue-500 border-blue-500/30">
                  {aiJobStatus || 'Loading AI insights...'}
                </Badge>
              )}
            </div>
            <p className="text-sm text-muted-foreground">
              Analyzed {formatLocalizedDateTime(new Date(riskData.analyzedAt), i18n.language)}
            </p>
          </div>
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button 
                  variant="ghost" 
                  size="icon" 
                  onClick={handleRefresh} 
                  disabled={refreshing}
                >
                  <RefreshCw className={cn("h-4 w-4", refreshing && "animate-spin")} />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Refresh analysis</TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>

        {/* Risk Score Display */}
        <div className="mb-6">
          <div className="flex items-center gap-4 mb-3">
            <div
              className="w-20 h-20 rounded-full flex items-center justify-center border-[3px]"
              style={{
                backgroundColor: `${getRiskScoreColor(riskData.riskScore)}20`,
                borderColor: getRiskScoreColor(riskData.riskScore),
              }}
            >
              <span
                className="text-3xl font-bold"
                style={{ color: getRiskScoreColor(riskData.riskScore) }}
              >
                {riskData.riskScore}
              </span>
            </div>
            <div>
              <Badge
                variant="outline"
                className={cn('text-sm font-semibold mb-2', getRiskLevelBadgeClass(riskData.riskLevel))}
              >
                {getRiskLevelLabel(riskData.riskLevel)}
              </Badge>
              {/* Legend line: makes the band + numeric range explicit */}
              {activeBand && (
                <p className="text-sm font-medium">
                  {t('risk.explanation.scoreLine', {
                    score: riskData.riskScore,
                    level: getRiskLevelShortLabel(activeBand.level),
                    min: activeBand.minScore,
                    max: activeBand.maxScore,
                  })}
                </p>
              )}
              <p className="text-sm text-muted-foreground">
                {t('risk.confidence', { value: riskData.confidenceScore })}
              </p>
            </div>
          </div>
          <Progress
            value={riskData.riskScore}
            className="h-2"
            style={{
              ['--progress-background' as any]: getRiskScoreColor(riskData.riskScore),
            }}
          />

          {/* Compact Low | Medium | High | Critical band scale with active band highlighted */}
          {riskData.explanation && riskData.explanation.bands.length > 0 && (
            <div className="mt-3">
              <div className="flex gap-1" role="img" aria-label={t('risk.explanation.scaleAriaLabel')}>
                {riskData.explanation.bands.map((band) => (
                  <TooltipProvider key={band.level}>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <div
                          className={cn(
                            'flex-1 rounded-sm h-2 transition-opacity',
                            band.active ? 'opacity-100 ring-2 ring-offset-1 ring-offset-background' : 'opacity-25'
                          )}
                          style={{
                            backgroundColor: bandColor(band.level),
                            ['--tw-ring-color' as any]: bandColor(band.level),
                          }}
                        />
                      </TooltipTrigger>
                      <TooltipContent>
                        {getRiskLevelShortLabel(band.level)} ({band.minScore}–{band.maxScore})
                      </TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                ))}
              </div>
              <div className="flex justify-between mt-1 text-[10px] text-muted-foreground">
                {riskData.explanation.bands.map((band) => (
                  <span
                    key={band.level}
                    className={cn('flex-1 text-center', band.active && 'font-semibold text-foreground')}
                  >
                    {getRiskLevelShortLabel(band.level)}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Collapsible "How is this calculated?" detail */}
          {riskData.explanation && (
            <Collapsible
              open={expandedSection === 'howCalculated'}
              onOpenChange={() => toggleSection('howCalculated')}
              className="mt-3"
            >
              <CollapsibleTrigger className="flex items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground py-1">
                <Info className="h-3.5 w-3.5" />
                <span>{t('risk.explanation.howCalculated')}</span>
                {expandedSection === 'howCalculated' ? (
                  <ChevronUp className="h-3.5 w-3.5" />
                ) : (
                  <ChevronDown className="h-3.5 w-3.5" />
                )}
              </CollapsibleTrigger>
              <CollapsibleContent>
                <div className="mt-2 rounded-md border bg-muted/30 p-3 space-y-3">
                  {/* Threshold legend */}
                  <div>
                    <p className="text-xs font-semibold mb-1.5">{t('risk.explanation.bandsTitle')}</p>
                    <ul className="space-y-1">
                      {riskData.explanation.bands.map((band) => (
                        <li
                          key={band.level}
                          className={cn(
                            'flex items-center justify-between text-xs',
                            band.active && 'font-semibold'
                          )}
                        >
                          <span className="flex items-center gap-1.5">
                            <span
                              className="inline-block h-2.5 w-2.5 rounded-full"
                              style={{ backgroundColor: bandColor(band.level) }}
                            />
                            {getRiskLevelShortLabel(band.level)}
                          </span>
                          <span className="text-muted-foreground">
                            {band.minScore}–{band.maxScore}
                            {band.active && ` · ${t('risk.explanation.current')}`}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  {/* Per-factor contributions */}
                  {riskData.explanation.factorContributions.length > 0 ? (
                    <div>
                      <p className="text-xs font-semibold mb-1.5">{t('risk.explanation.contributionsTitle')}</p>
                      <ul className="space-y-1.5">
                        {riskData.explanation.factorContributions.map((fc, idx) => (
                          <li key={idx} className="text-xs">
                            <div className="flex items-center justify-between gap-2">
                              <span className="font-medium">{formatRiskCategory(fc.category)}</span>
                              <span className="shrink-0 font-mono text-muted-foreground">
                                +{fc.weightedPoints.toFixed(1)}
                              </span>
                            </div>
                            <p className="text-muted-foreground">
                              {fc.description}
                            </p>
                            <p className="text-[10px] text-muted-foreground">
                              {t('risk.explanation.impactProbability', {
                                impact: fc.impactLevel,
                                probability: fc.probability,
                              })}
                            </p>
                          </li>
                        ))}
                      </ul>
                    </div>
                  ) : (
                    <p className="text-xs text-muted-foreground">{t('risk.explanation.noFactors')}</p>
                  )}
                </div>
              </CollapsibleContent>
            </Collapsible>
          )}
        </div>

        {/* Risk Factors */}
        {riskData.riskFactors.length > 0 && (
          <Collapsible 
            open={expandedSection === 'factors'} 
            onOpenChange={() => toggleSection('factors')}
            className="mb-4"
          >
            <CollapsibleTrigger className="flex items-center justify-between w-full py-2 hover:bg-muted/50 rounded px-2 -mx-2">
              <div className="flex items-center gap-2">
                <AlertTriangle className="h-4 w-4 text-yellow-500" />
                <span className="font-semibold">Risk Factors ({riskData.riskFactors.length})</span>
              </div>
              {expandedSection === 'factors' ? (
                <ChevronUp className="h-4 w-4" />
              ) : (
                <ChevronDown className="h-4 w-4" />
              )}
            </CollapsibleTrigger>
            <CollapsibleContent>
              <div className="space-y-2 py-2">
                {riskData.riskFactors.map((factor, index) => (
                  <div key={index} className="flex items-start gap-3 py-1">
                    <Badge
                      variant="outline"
                      className={cn(
                        'shrink-0 min-w-[28px] justify-center',
                        factor.impactLevel >= 7 
                          ? 'bg-red-500/15 text-red-500 border-red-500/30'
                          : factor.impactLevel >= 4 
                            ? 'bg-yellow-500/15 text-yellow-500 border-yellow-500/30'
                            : ''
                      )}
                    >
                      {factor.impactLevel}
                    </Badge>
                    <div>
                      <p className="text-sm font-medium">{formatRiskCategory(factor.category)}</p>
                      <p className="text-xs text-muted-foreground">{factor.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            </CollapsibleContent>
          </Collapsible>
        )}

        {/* Insights */}
        <Collapsible 
          open={expandedSection === 'insights'} 
          onOpenChange={() => toggleSection('insights')}
          className="mb-4"
        >
          <CollapsibleTrigger className="flex items-center justify-between w-full py-2 hover:bg-muted/50 rounded px-2 -mx-2">
            <div className="flex items-center gap-2">
              <Lightbulb className="h-4 w-4 text-blue-500" />
              <span className="font-semibold">Insights</span>
              {aiLoading && <Loader2 className="h-3 w-3 animate-spin" />}
            </div>
            {expandedSection === 'insights' ? (
              <ChevronUp className="h-4 w-4" />
            ) : (
              <ChevronDown className="h-4 w-4" />
            )}
          </CollapsibleTrigger>
          <CollapsibleContent>
            <div className="py-2">
              {riskData.insights.length > 0 ? (
                <div className="space-y-2">
                  {riskData.insights.map((insight, index) => (
                    <div key={index} className="text-sm">
                      <Markdown content={insight} />
                    </div>
                  ))}
                </div>
              ) : aiLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-4 w-[95%]" />
                  <Skeleton className="h-4 w-[88%]" />
                  <Skeleton className="h-4 w-[92%]" />
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">No insights available yet</p>
              )}
            </div>
          </CollapsibleContent>
        </Collapsible>

        {/* Recommendations */}
        <Collapsible 
          open={expandedSection === 'recommendations'} 
          onOpenChange={() => toggleSection('recommendations')}
        >
          <CollapsibleTrigger className="flex items-center justify-between w-full py-2 hover:bg-muted/50 rounded px-2 -mx-2">
            <div className="flex items-center gap-2">
              <ThumbsUp className="h-4 w-4 text-green-500" />
              <span className="font-semibold">Recommendations</span>
              {aiLoading && <Loader2 className="h-3 w-3 animate-spin" />}
            </div>
            {expandedSection === 'recommendations' ? (
              <ChevronUp className="h-4 w-4" />
            ) : (
              <ChevronDown className="h-4 w-4" />
            )}
          </CollapsibleTrigger>
          <CollapsibleContent>
            <div className="py-2">
              {riskData.recommendations.length > 0 ? (
                <div className="space-y-2">
                  {riskData.recommendations.map((rec, index) => (
                    <div key={index} className="flex items-start gap-2">
                      <span className="text-green-500 font-semibold shrink-0">{index + 1}.</span>
                      <div className="text-sm flex-1">
                        <Markdown content={rec} />
                      </div>
                    </div>
                  ))}
                </div>
              ) : aiLoading ? (
                <div className="space-y-2">
                  <Skeleton className="h-4 w-[90%]" />
                  <Skeleton className="h-4 w-[85%]" />
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">No recommendations available yet</p>
              )}
            </div>
          </CollapsibleContent>
        </Collapsible>

        {/* Q&A Section */}
        {riskData.aiEnabled && (
          <>
            <Separator className="my-4" />
            <RiskQA pitchId={pitchId} pitchTitle={riskData.pitchTitle || 'This Pitch'} />
          </>
        )}

        {/* Feedback Section */}
        <Separator className="my-4" />
        <RiskFeedbackForm
          pitchId={pitchId}
          pitchTitle={riskData.pitchTitle || 'This Pitch'}
          currentRiskScore={riskData.riskScore}
          onFeedbackSubmitted={handleRefresh}
        />
      </CardContent>
    </Card>
  );
}
