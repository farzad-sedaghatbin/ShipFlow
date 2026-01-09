import { useState, useEffect } from 'react';
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
  getRiskScoreColor,
  formatRiskCategory,
} from '../services/riskService';
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
  const [riskData, setRiskData] = useState<PitchRiskDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [aiLoading, setAiLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [expandedSection, setExpandedSection] = useState<string | null>('insights');

  useEffect(() => {
    loadRiskAnalysis();
  }, [pitchId]);

  const loadRiskAnalysis = async () => {
    try {
      setLoading(true);
      // First load fast data (rule-based)
      const fastResponse = await riskService.getPitchRisk(pitchId);
      setRiskData(fastResponse.data);
      setLoading(false);
      
      // Then load AI-enhanced data in background
      setAiLoading(true);
      try {
        const aiResponse = await riskService.getPitchRiskWithAI(pitchId);
        setRiskData(aiResponse.data);
      } catch (aiError) {
        console.warn('AI risk analysis unavailable, using rule-based analysis');
      } finally {
        setAiLoading(false);
      }
    } catch (error: any) {
      console.error('Failed to load risk analysis:', error);
      onError?.('Failed to load risk analysis');
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    try {
      setRefreshing(true);
      const response = await riskService.refreshPitchRisk(pitchId);
      setRiskData(response.data);
    } catch (error: any) {
      console.error('Failed to refresh risk analysis:', error);
      onError?.('Failed to refresh risk analysis');
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
        return 'Low Risk';
      case 'MEDIUM':
        return 'Medium Risk';
      case 'HIGH':
        return 'High Risk';
      case 'CRITICAL':
        return 'Critical Risk';
      default:
        return 'Unknown';
    }
  };

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
                  Loading AI insights...
                </Badge>
              )}
            </div>
            <p className="text-sm text-muted-foreground">
              Analyzed {new Date(riskData.analyzedAt).toLocaleString()}
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
              <p className="text-sm text-muted-foreground">
                Confidence: {riskData.confidenceScore}%
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
