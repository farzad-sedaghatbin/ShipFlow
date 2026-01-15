import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, TrendingUp, TrendingDown, AlertTriangle, CheckCircle2, Info } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Alert, AlertDescription, AlertTitle } from '../components/ui/alert';
import { Skeleton } from '../components/ui/skeleton';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { bettingService } from '../services/bettingService';
import { cycleService } from '../services/cycleService';
import { 
  PitchComparison, 
  CapacityAnalysis,
  TeamPerformanceHistory,
  TeamFitAnalysis,
  Cycle,
  PerformanceRating,
  PerformanceTrend,
  ComplexityLevel,
  CapacityWarningSeverity
} from '../types';
import { useToast } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';
import { cn } from '../lib/utils';

const PitchComparisonView = () => {
  const [searchParams] = useSearchParams();
  const cycleId = searchParams.get('cycleId');
  const navigate = useNavigate();
  const { showError } = useToast();

  const [cycle, setCycle] = useState<Cycle | null>(null);
  const [pitchComparisons, setPitchComparisons] = useState<PitchComparison[]>([]);
  const [capacityAnalysis, setCapacityAnalysis] = useState<CapacityAnalysis | null>(null);
  const [teamPerformance, setTeamPerformance] = useState<Record<number, TeamPerformanceHistory>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!cycleId) {
      setError('No cycle selected');
      setLoading(false);
      return;
    }
    loadData();
  }, [cycleId]);

  const loadData = async () => {
    if (!cycleId) return;
    
    try {
      setLoading(true);
      setError(null);

      const [cycleRes, comparisonsRes, capacityRes] = await Promise.all([
        cycleService.getById(Number(cycleId)),
        bettingService.getPitchComparisons(Number(cycleId)),
        bettingService.getCapacityAnalysis(Number(cycleId)),
      ]);

      setCycle(cycleRes.data);
      setPitchComparisons(comparisonsRes.data);
      setCapacityAnalysis(capacityRes.data);

      // Load team performance history for all teams
      const teamIds = new Set<number>();
      comparisonsRes.data.forEach((pitch: PitchComparison) => {
        pitch.teamFitScores.forEach((fit: TeamFitAnalysis) => teamIds.add(fit.teamId));
      });

      const performancePromises = Array.from(teamIds).map(teamId =>
        bettingService.getTeamPerformanceHistory(teamId)
          .then(res => ({ teamId, data: res.data }))
      );

      const performanceResults = await Promise.all(performancePromises);
      const performanceMap: Record<number, TeamPerformanceHistory> = {};
      performanceResults.forEach(result => {
        // Validate that result.data contains expected fields
        if (result.data && result.data.teamId && result.data.teamName) {
          performanceMap[result.teamId] = result.data;
        } else {
          console.warn(`Invalid performance data for team ${result.teamId}:`, result.data);
        }
      });
      setTeamPerformance(performanceMap);

    } catch (err) {
      console.error('Error loading pitch comparison data:', err);
      setError(getUserFriendlyError(err));
      showError(getUserFriendlyError(err));
    } finally {
      setLoading(false);
    }
  };

  const getComplexityColor = (complexity: ComplexityLevel) => {
    switch (complexity) {
      case 'LOW': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'MEDIUM': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
      case 'HIGH': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
    }
  };

  const getSeverityIcon = (severity: CapacityWarningSeverity) => {
    switch (severity) {
      case 'CRITICAL': return <AlertTriangle className="h-4 w-4 text-red-500" />;
      case 'WARNING': return <Info className="h-4 w-4 text-yellow-500" />;
      case 'INFO': return <CheckCircle2 className="h-4 w-4 text-blue-500" />;
    }
  };

  const getSeverityVariant = (severity: CapacityWarningSeverity): 'default' | 'destructive' => {
    switch (severity) {
      case 'CRITICAL': return 'destructive';
      case 'WARNING':
      case 'INFO':
      default:
        return 'default';
    }
  };

  const getPerformanceIcon = (trend: PerformanceTrend) => {
    switch (trend) {
      case 'IMPROVING': return <TrendingUp className="h-4 w-4 text-green-500" />;
      case 'DECLINING': return <TrendingDown className="h-4 w-4 text-red-500" />;
      case 'STABLE': return <div className="h-4 w-4 border-t-2 border-gray-500" />;
    }
  };

  const getPerformanceColor = (rating: PerformanceRating) => {
    switch (rating) {
      case 'EXCELLENT': return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300';
      case 'GOOD': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      case 'FAIR': return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300';
      case 'POOR': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300';
      default: return 'bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-300';
    }
  };

  const formatPercentage = (value: number) => `${Math.round(value * 100)}%`;

  if (loading) {
    return (
      <div className="flex-1 overflow-auto p-8">
        <div className="max-w-7xl mx-auto space-y-6">
          <Skeleton className="h-12 w-96" />
          <Skeleton className="h-64 w-full" />
          <Skeleton className="h-96 w-full" />
        </div>
      </div>
    );
  }

  if (error || !cycle) {
    return (
      <div className="flex-1 overflow-auto p-8">
        <div className="max-w-7xl mx-auto">
          <Alert variant="destructive">
            <AlertTriangle className="h-4 w-4" />
            <AlertTitle>Error</AlertTitle>
            <AlertDescription>{error || 'Failed to load cycle data'}</AlertDescription>
          </Alert>
          <Button onClick={() => navigate(-1)} className="mt-4">
            <ArrowLeft className="mr-2 h-4 w-4" /> Go Back
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-auto p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <Button variant="ghost" onClick={() => navigate(-1)} className="mb-2">
              <ArrowLeft className="mr-2 h-4 w-4" /> Back to Betting Table
            </Button>
            <h1 className="text-3xl font-bold tracking-tight">Pitch Comparison</h1>
            <p className="text-muted-foreground">
              {cycle.name} • Betting Meeting Analysis
            </p>
          </div>
        </div>

        {/* Capacity Analysis */}
        {capacityAnalysis && (
          <Card>
            <CardHeader>
              <CardTitle>Capacity Overview</CardTitle>
              <CardDescription>
                Total: {capacityAnalysis.totalCapacityWeeks} weeks • 
                Allocated: {capacityAnalysis.allocatedWeeks} weeks • 
                Remaining: {capacityAnalysis.remainingWeeks} weeks • 
                Utilization: {formatPercentage(capacityAnalysis.utilizationRate)}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              {/* Warnings */}
              {capacityAnalysis.warnings.length > 0 && (
                <div className="space-y-2">
                  {capacityAnalysis.warnings.map((warning, idx) => (
                    <Alert key={idx} variant={getSeverityVariant(warning.severity)}>
                      <div className="flex items-start gap-2">
                        {getSeverityIcon(warning.severity)}
                        <div className="flex-1">
                          <AlertTitle className="text-sm font-semibold">
                            {warning.teamName} ({formatPercentage(warning.utilizationRate)})
                          </AlertTitle>
                          <AlertDescription className="text-sm">
                            {warning.message}
                          </AlertDescription>
                        </div>
                      </div>
                    </Alert>
                  ))}
                </div>
              )}

              {/* Recommendations */}
              {capacityAnalysis.recommendations.length > 0 && (
                <div className="space-y-2">
                  <h4 className="text-sm font-semibold">Recommendations</h4>
                  <ul className="list-disc list-inside space-y-1 text-sm text-muted-foreground">
                    {capacityAnalysis.recommendations.map((rec, idx) => (
                      <li key={idx}>{rec}</li>
                    ))}
                  </ul>
                </div>
              )}
            </CardContent>
          </Card>
        )}

        {/* Pitch Comparisons */}
        <Card>
          <CardHeader>
            <CardTitle>Shaped Pitches Analysis</CardTitle>
            <CardDescription>
              Compare pitches across teams with historical performance and capacity fit
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-8">
              {pitchComparisons.map((pitch) => (
                <div key={pitch.pitchId} className="border rounded-lg p-4 space-y-4">
                  {/* Pitch Header */}
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold">{pitch.pitchTitle}</h3>
                      <div className="flex items-center gap-2 mt-1">
                        <Badge variant="outline" className="text-xs">
                          {pitch.appetiteWeeks} weeks ({pitch.appetiteDays} days)
                        </Badge>
                        {pitch.complexityLevel && (
                          <Badge className={cn('text-xs', getComplexityColor(pitch.complexityLevel))}>
                            {pitch.complexityLevel}
                          </Badge>
                        )}
                        {pitch.urgency && (
                          <Badge variant="secondary" className="text-xs">
                            {pitch.urgency}
                          </Badge>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Risks & Rabbit Holes */}
                  {(pitch.risks.length > 0 || pitch.rabbitHoles.length > 0) && (
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      {pitch.risks.length > 0 && (
                        <div>
                          <h4 className="font-semibold text-xs text-red-600 dark:text-red-400 mb-1">
                            Risks
                          </h4>
                          <ul className="list-disc list-inside space-y-0.5 text-muted-foreground">
                            {pitch.risks.map((risk, idx) => (
                              <li key={idx} className="text-xs">{risk}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                      {pitch.rabbitHoles.length > 0 && (
                        <div>
                          <h4 className="font-semibold text-xs text-orange-600 dark:text-orange-400 mb-1">
                            Rabbit Holes
                          </h4>
                          <ul className="list-disc list-inside space-y-0.5 text-muted-foreground">
                            {pitch.rabbitHoles.map((hole, idx) => (
                              <li key={idx} className="text-xs">{hole}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Team Fit Analysis Table */}
                  <div>
                    <h4 className="text-sm font-semibold mb-2">Team Capacity Fit</h4>
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Team</TableHead>
                          <TableHead>Historical Performance</TableHead>
                          <TableHead>Available</TableHead>
                          <TableHead>Required</TableHead>
                          <TableHead>Utilization</TableHead>
                          <TableHead>Fit Score</TableHead>
                          <TableHead>Status</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {pitch.teamFitScores.map((fit) => {
                          const performance = teamPerformance[fit.teamId];
                          return (
                            <TableRow key={fit.teamId}>
                              <TableCell className="font-medium">{fit.teamName}</TableCell>
                              <TableCell>
                                {performance && (
                                  <div className="flex items-center gap-2">
                                    <Badge className={cn('text-xs', getPerformanceColor(performance.performanceRating))}>
                                      {performance.performanceRating}
                                    </Badge>
                                    {getPerformanceIcon(performance.trend)}
                                    <span className="text-xs text-muted-foreground">
                                      {performance.completedBets}/{performance.totalBets} bets
                                    </span>
                                  </div>
                                )}
                              </TableCell>
                              <TableCell>{fit.availableWeeks}w</TableCell>
                              <TableCell>{fit.requiredWeeks}w</TableCell>
                              <TableCell>{formatPercentage(fit.utilizationPercent / 100)}</TableCell>
                              <TableCell>
                                <Badge variant={fit.fitScore >= 70 ? 'default' : 'secondary'} className="text-xs">
                                  {fit.fitScore}
                                </Badge>
                              </TableCell>
                              <TableCell>
                                {fit.canFit ? (
                                  <Badge variant="outline" className="text-xs bg-green-50 text-green-700 dark:bg-green-950 dark:text-green-400">
                                    <CheckCircle2 className="h-3 w-3 mr-1" />
                                    Can Fit
                                  </Badge>
                                ) : (
                                  <div>
                                    <Badge variant="outline" className="text-xs bg-red-50 text-red-700 dark:bg-red-950 dark:text-red-400 mb-1">
                                      <AlertTriangle className="h-3 w-3 mr-1" />
                                      Over Capacity
                                    </Badge>
                                    {fit.capacityWarning && (
                                      <p className="text-xs text-muted-foreground mt-1">{fit.capacityWarning}</p>
                                    )}
                                  </div>
                                )}
                              </TableCell>
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  </div>
                </div>
              ))}

              {pitchComparisons.length === 0 && (
                <div className="text-center py-8 text-muted-foreground">
                  No shaped pitches available for this cycle
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default PitchComparisonView;
