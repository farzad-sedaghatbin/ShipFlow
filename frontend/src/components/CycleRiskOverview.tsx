import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  AlertTriangle,
  Lightbulb,
  RefreshCw,
  Sparkles,
  Bot,
  TrendingUp,
  Loader2,
} from 'lucide-react';
import {
  riskService,
  CycleRiskOverviewDTO,
  RiskLevel,
  formatRiskCategory,
  fetchCycleRiskAsync,
  JobStatusResponse,
} from '../services/riskService';
import { cn } from '@/lib/utils';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Separator } from '@/components/ui/separator';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { Markdown } from '@/components/ui/markdown';
import PitchRiskTrendSparkline from './PitchRiskTrendSparkline';
import RiskFactorTooltip from './RiskFactorTooltip';

interface CycleRiskOverviewProps {
  cycleId: number;
  cycleName: string;
  compact?: boolean;
  onError?: (error: string) => void;
}

const riskLevelColors = {
  LOW: 'bg-emerald-500/15 text-emerald-500 border-emerald-500/30',
  MEDIUM: 'bg-amber-500/15 text-amber-500 border-amber-500/30',
  HIGH: 'bg-orange-500/15 text-orange-500 border-orange-500/30',
  CRITICAL: 'bg-destructive/15 text-destructive border-destructive/30',
};

const riskScoreColors = {
  low: 'border-emerald-500 bg-emerald-500/20 text-emerald-500',
  medium: 'border-amber-500 bg-amber-500/20 text-amber-500',
  high: 'border-orange-500 bg-orange-500/20 text-orange-500',
  critical: 'border-destructive bg-destructive/20 text-destructive',
};

function getRiskScoreColorClass(score: number) {
  if (score <= 3) return riskScoreColors.low;
  if (score <= 5) return riskScoreColors.medium;
  if (score <= 7) return riskScoreColors.high;
  return riskScoreColors.critical;
}

export default function CycleRiskOverview({
  cycleId,
  cycleName,
  compact = false,
  onError,
}: CycleRiskOverviewProps) {
  const { t } = useTranslation();
  const [riskData, setRiskData] = useState<CycleRiskOverviewDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiJobStatus, setAiJobStatus] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    loadRiskOverview();
  }, [cycleId]);

  const loadRiskOverview = async () => {
    try {
      setLoading(true);
      // First load fast rule-based data - immediate response
      const fastResponse = await riskService.getCycleRiskOverview(cycleId);
      setRiskData(fastResponse.data);
      setLoading(false);
      
      // Then load AI-enhanced data asynchronously with polling
      setAiLoading(true);
      setAiJobStatus('Starting AI analysis...');
      try {
        const aiResult = await fetchCycleRiskAsync(cycleId, (status: JobStatusResponse) => {
          if (status.status === 'PENDING') {
            setAiJobStatus('Queued for AI analysis...');
          } else if (status.status === 'PROCESSING') {
            setAiJobStatus('AI analyzing cycle risks...');
          }
        });
        
        if (aiResult) {
          setRiskData(aiResult);
        }
        setAiJobStatus(null);
      } catch (aiError) {
        console.warn('AI risk analysis unavailable, using rule-based analysis');
        setAiJobStatus(null);
      } finally {
        setAiLoading(false);
      }
    } catch (error: any) {
      console.error('Failed to load cycle risk overview:', error);
      onError?.(t('errors.loadRiskOverviewFailed'));
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      setAiJobStatus('Refreshing AI analysis...');
      
      const aiResult = await fetchCycleRiskAsync(cycleId, (status: JobStatusResponse) => {
        if (status.status === 'PROCESSING') {
          setAiJobStatus('AI re-analyzing cycle...');
        }
      });
      
      if (aiResult) {
        setRiskData(aiResult);
      }
      setAiJobStatus(null);
    } catch (error: any) {
      console.error('Failed to refresh cycle risk:', error);
      onError?.(t('errors.refreshRiskOverviewFailed'));
      setAiJobStatus(null);
    } finally {
      setRefreshing(false);
    }
  };

  const getRiskLevelLabel = (level: RiskLevel): string => {
    switch (level) {
      case 'LOW': return t('riskAdvisor.levelLow');
      case 'MEDIUM': return t('riskAdvisor.levelMedium');
      case 'HIGH': return t('riskAdvisor.levelHigh');
      case 'CRITICAL': return t('riskAdvisor.levelCritical');
      default: return t('common.unknown');
    }
  };

  if (loading) {
    return (
      <Card className={cn(compact && 'h-full')}>
        <CardContent className="p-4">
          <Skeleton className="w-3/5 h-8 mb-2" />
          <div className="flex gap-2 mt-1">
            <Skeleton className="w-20 h-6 rounded-full" />
            <Skeleton className="w-16 h-6 rounded-full" />
          </div>
          <Skeleton className="w-full h-24 mt-2 rounded-lg" />
        </CardContent>
      </Card>
    );
  }

  if (!riskData) {
    return null;
  }

  // Compact view for dashboard cards
  if (compact) {
    return (
      <Card className="h-full">
        <CardContent className="p-4">
          <div className="flex justify-between items-center mb-2">
            <div className="flex items-center gap-1">
              <span className="text-sm font-semibold text-foreground">
                {t('riskAdvisor.cycleRiskTitle', { name: cycleName })}
              </span>
              <Tooltip>
                <TooltipTrigger asChild>
                  {aiLoading ? (
                    <Loader2 className="w-4 h-4 animate-spin text-muted-foreground" />
                  ) : (
                    <Sparkles
                      className={cn(
                        'w-4 h-4',
                        riskData.aiEnabled ? 'text-primary' : 'text-muted-foreground/50'
                      )}
                    />
                  )}
                </TooltipTrigger>
                <TooltipContent>
                  {aiLoading ? (aiJobStatus || t('riskAdvisor.aiAnalyzing')) : (riskData.aiEnabled ? t('riskAdvisor.aiPowered') : t('riskAdvisor.ruleBased'))}
                </TooltipContent>
              </Tooltip>
            </div>
          </div>
          
          <div className="flex items-center gap-3 mb-2">
            <div
              className={cn(
                'w-12 h-12 rounded-full flex items-center justify-center border-2',
                getRiskScoreColorClass(riskData.overallRiskScore)
              )}
            >
              <span className="text-lg font-bold">
                {riskData.overallRiskScore}
              </span>
            </div>
            <div>
              <Badge variant="outline" className={riskLevelColors[riskData.overallRiskLevel]}>
                {getRiskLevelLabel(riskData.overallRiskLevel)}
              </Badge>
              <p className="text-xs text-muted-foreground mt-0.5">
                {t('riskAdvisor.pitchCount', { count: riskData.totalPitches })}
              </p>
            </div>
          </div>

          <div className="flex gap-1">
            {riskData.highRiskPitches > 0 && (
              <Badge variant="outline" className="text-xs border-destructive/50 text-destructive">
                {riskData.highRiskPitches} High
              </Badge>
            )}
            {riskData.mediumRiskPitches > 0 && (
              <Badge variant="outline" className="text-xs border-amber-500/50 text-amber-500">
                {riskData.mediumRiskPitches} Med
              </Badge>
            )}
            {riskData.lowRiskPitches > 0 && (
              <Badge variant="outline" className="text-xs border-emerald-500/50 text-emerald-500">
                {riskData.lowRiskPitches} Low
              </Badge>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  // Full view
  return (
    <Card>
      <CardContent className="p-4">
        {/* Header */}
        <div className="flex justify-between items-start mb-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Sparkles className={cn('w-5 h-5', riskData.aiEnabled ? 'text-primary' : 'text-muted-foreground/50')} />
              <h2 className="text-lg font-semibold text-foreground">
                {t('riskAdvisor.title')}
              </h2>
              {riskData.aiEnabled && (
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Badge variant="outline" className="gap-1">
                      <Bot className="w-3 h-3" />
                      AI
                    </Badge>
                  </TooltipTrigger>
                  <TooltipContent>{t('riskAdvisor.aiTooltip')}</TooltipContent>
                </Tooltip>
              )}
            </div>
            <p className="text-sm text-muted-foreground">
              {cycleName} • {riskData.totalPitches} pitches
            </p>
          </div>
          <Tooltip>
            <TooltipTrigger asChild>
              <Button variant="ghost" size="icon" onClick={handleRefresh} disabled={refreshing} aria-label={t('aria.refreshData')}>
                <RefreshCw className={cn('w-4 h-4', refreshing && 'animate-spin')} aria-hidden="true" />
              </Button>
            </TooltipTrigger>
            <TooltipContent>{t('riskAdvisor.refreshAnalysis')}</TooltipContent>
          </Tooltip>
        </div>

        {/* Overall Risk Score */}
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4 mb-4">
          <div className="md:col-span-4">
            <div className="flex items-center gap-3">
              <div
                className={cn(
                  'w-20 h-20 rounded-full flex items-center justify-center border-[3px]',
                  getRiskScoreColorClass(riskData.overallRiskScore)
                )}
              >
                <span className="text-3xl font-bold">
                  {riskData.overallRiskScore}
                </span>
              </div>
              <div>
                <Badge className={riskLevelColors[riskData.overallRiskLevel]}>
                  {getRiskLevelLabel(riskData.overallRiskLevel)}
                </Badge>
                <p className="text-sm text-muted-foreground mt-1">
                  {t('riskAdvisor.overallScore')}
                </p>
              </div>
            </div>
          </div>

          {/* Risk Distribution */}
          <div className="md:col-span-8">
            <h3 className="text-sm font-semibold text-foreground mb-2">
              {t('riskAdvisor.riskDistribution')}
            </h3>
            <div className="flex gap-2 flex-wrap">
              <div className="text-center p-2 rounded-lg bg-destructive/10 min-w-[70px]">
                <p className="text-2xl font-bold text-destructive">
                  {riskData.highRiskPitches}
                </p>
                <p className="text-xs text-destructive">{t('riskAdvisor.levelHigh')}</p>
              </div>
              <div className="text-center p-2 rounded-lg bg-amber-500/10 min-w-[70px]">
                <p className="text-2xl font-bold text-amber-500">
                  {riskData.mediumRiskPitches}
                </p>
                <p className="text-xs text-amber-500">{t('riskAdvisor.levelMedium')}</p>
              </div>
              <div className="text-center p-2 rounded-lg bg-emerald-500/10 min-w-[70px]">
                <p className="text-2xl font-bold text-emerald-500">
                  {riskData.lowRiskPitches}
                </p>
                <p className="text-xs text-emerald-500">{t('riskAdvisor.levelLow')}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Stats */}
        {riskData.stats && (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mb-4">
            <div className="text-center p-2">
              <p className="text-xs text-muted-foreground">{t('riskAdvisor.avgProgress')}</p>
              <p className="text-lg font-semibold text-foreground">
                {riskData.stats.averageProgress.toFixed(0)}%
              </p>
            </div>
            <div className="text-center p-2">
              <p className="text-xs text-muted-foreground">{t('riskAdvisor.appetiteUsed')}</p>
              <p className={cn(
                'text-lg font-semibold',
                riskData.stats.appetiteUtilization > 100 ? 'text-destructive' : 'text-foreground'
              )}>
                {riskData.stats.appetiteUtilization.toFixed(0)}%
              </p>
            </div>
            <div className="text-center p-2">
              <p className="text-xs text-muted-foreground">{t('riskAdvisor.maxRisk')}</p>
              <p className="text-lg font-semibold text-destructive">
                {riskData.stats.maxRiskScore}
              </p>
            </div>
            <div className="text-center p-2">
              <p className="text-xs text-muted-foreground">{t('riskAdvisor.minRisk')}</p>
              <p className="text-lg font-semibold text-emerald-500">
                {riskData.stats.minRiskScore}
              </p>
            </div>
          </div>
        )}

        <Separator className="my-3" />

        {/* Pitch Risk Summary */}
        {riskData.pitchRisks.length > 0 && (
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-foreground mb-2">
              {t('riskAdvisor.pitchesByRisk')}
            </h3>
            <div className="space-y-1">
              {riskData.pitchRisks.slice(0, 5).map((pitch) => (
                <RiskFactorTooltip
                  key={pitch.pitchId}
                  pitchId={pitch.pitchId}
                  pitchTitle={pitch.pitchTitle}
                  topRiskPreview={pitch.topRisk}
                >
                  <Link
                    to={`/pitches/${pitch.pitchId}`}
                    className="flex items-center justify-between p-2 rounded-lg hover:bg-muted/50 transition-colors"
                  >
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-foreground truncate">
                        {pitch.pitchTitle}
                      </p>
                      <p className="text-xs text-muted-foreground truncate">
                        {pitch.topRisk}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 ml-2">
                      {/* Risk trend sparkline */}
                      <PitchRiskTrendSparkline
                        pitchId={pitch.pitchId}
                        currentScore={pitch.riskScore}
                        days={30}
                        compact
                      />
                      <span
                        className={cn(
                          'text-sm font-bold',
                          pitch.riskScore <= 3 ? 'text-emerald-500' :
                          pitch.riskScore <= 5 ? 'text-amber-500' :
                          pitch.riskScore <= 7 ? 'text-orange-500' : 'text-destructive'
                        )}
                      >
                        {pitch.riskScore}
                      </span>
                      <Badge variant="outline" className={cn('text-xs', riskLevelColors[pitch.riskLevel])}>
                        {getRiskLevelLabel(pitch.riskLevel)}
                      </Badge>
                    </div>
                  </Link>
                </RiskFactorTooltip>
              ))}
            </div>
          </div>
        )}

        {/* Top Risk Factors */}
        {riskData.topRiskFactors.length > 0 && (
          <div className="mb-4">
            <h3 className="text-sm font-semibold text-foreground mb-2">
              {t('riskAdvisor.commonRiskFactors')}
            </h3>
            <div className="flex gap-1 flex-wrap">
              {riskData.topRiskFactors.map((factor, index) => (
                <Badge
                  key={index}
                  variant="outline"
                  className={cn(
                    'gap-1',
                    factor.averageImpact >= 7 ? 'border-destructive/50 text-destructive' :
                    factor.averageImpact >= 4 ? 'border-amber-500/50 text-amber-500' : ''
                  )}
                >
                  <AlertTriangle className="w-3 h-3" />
                  {formatRiskCategory(factor.category)} ({factor.occurrenceCount})
                </Badge>
              ))}
            </div>
          </div>
        )}

        {/* AI Insights */}
        <div className="mb-4">
          <div className="flex items-center gap-2 mb-2">
            <Lightbulb className="w-4 h-4 text-blue-500" />
            <h3 className="text-sm font-semibold text-foreground">{t('riskAdvisor.insights')}</h3>
            {aiLoading && <Loader2 className="w-3 h-3 animate-spin text-muted-foreground" />}
          </div>
          {riskData.cycleInsights.length > 0 ? (
            <div className="space-y-2">
              {riskData.cycleInsights.map((insight, index) => (
                <div key={index} className="text-sm text-muted-foreground">
                  <Markdown content={insight} />
                </div>
              ))}
            </div>
          ) : aiLoading ? (
            <div className="space-y-1">
              <Skeleton className="w-[90%] h-4" />
              <Skeleton className="w-4/5 h-4" />
              <Skeleton className="w-[85%] h-4" />
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">{t('riskAdvisor.noInsights')}</p>
          )}
        </div>

        {/* Recommendations */}
        <div>
          <div className="flex items-center gap-2 mb-2">
            <TrendingUp className="w-4 h-4 text-emerald-500" />
            <h3 className="text-sm font-semibold text-foreground">{t('riskAdvisor.recommendations')}</h3>
            {aiLoading && <Loader2 className="w-3 h-3 animate-spin text-muted-foreground" />}
          </div>
          {riskData.cycleRecommendations.length > 0 ? (
            <div className="space-y-2">
              {riskData.cycleRecommendations.map((rec, index) => (
                <div key={index} className="text-sm text-muted-foreground">
                  <Markdown content={rec} />
                </div>
              ))}
            </div>
          ) : aiLoading ? (
            <div className="space-y-1">
              <Skeleton className="w-[95%] h-4" />
              <Skeleton className="w-[88%] h-4" />
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">{t('riskAdvisor.noRecommendations')}</p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
