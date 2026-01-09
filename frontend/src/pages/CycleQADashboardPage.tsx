import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { safeParseId } from '../utils/validation';
import {
  Bug,
  CheckCircle,
  XCircle,
  ClipboardList,
  AlertTriangle,
  RefreshCw,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { cn } from '../lib/utils';
import qaTestManagementService from '../services/qaTestManagementService';
import { QADashboard, TestCoverage } from '../types';

const CycleQADashboardPage: React.FC = () => {
  const { cycleId: cycleIdParam } = useParams<{ cycleId: string }>();
  const cycleId = safeParseId(cycleIdParam);
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dashboard, setDashboard] = useState<QADashboard | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    if (cycleId) {
      loadDashboard();
    }
  }, [cycleId]);

  const loadDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await qaTestManagementService.getQADashboardByCycle(cycleId!);
      setDashboard(response.data);
    } catch (err) {
      setError('Failed to load QA dashboard');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    await loadDashboard();
    setRefreshing(false);
  };

  const getHealthStatus = (score: number) => {
    if (score >= 80) return 'Healthy';
    if (score >= 60) return 'At Risk';
    return 'Critical';
  };

  const getHealthVariant = (score: number): 'default' | 'secondary' | 'destructive' | 'outline' => {
    if (score >= 80) return 'default';
    if (score >= 60) return 'secondary';
    return 'destructive';
  };

  const getProgressColor = (score: number) => {
    if (score >= 80) return 'bg-green-500';
    if (score >= 60) return 'bg-yellow-500';
    return 'bg-red-500';
  };

  if (cycleId === null) {
    return (
      <div className="p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertTriangle className="h-4 w-4" />
          <span className="text-sm">Invalid cycle ID</span>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="rounded-lg border border-destructive bg-destructive/10 p-4 text-destructive">
        Failed to load dashboard data. Please try again.
      </div>
    );
  }

  const totalBugs = dashboard.totalBugs || 0;
  const openBugs = dashboard.openBugs || 0;
  const criticalBugs = dashboard.criticalBugs + dashboard.blockerBugs || 0;
  const passRate = dashboard.overallPassRate || 0;

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">QA Dashboard</h1>
          <p className="text-sm text-muted-foreground">
            Cycle #{cycleId} - {dashboard.cycleName || 'Current Cycle'}
          </p>
        </div>
        <Button
          variant="ghost"
          size="icon"
          onClick={handleRefresh}
          disabled={refreshing}
        >
          <RefreshCw className={cn('h-5 w-5', refreshing && 'animate-spin')} />
        </Button>
      </div>

      {error && (
        <div className="rounded-lg border border-destructive bg-destructive/10 p-4 text-destructive mb-4">
          {error}
        </div>
      )}

      {/* Health Overview */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
        <Card>
          <CardContent className="pt-6 text-center">
            <div className="relative inline-flex mb-4">
              <svg className="w-24 h-24 transform -rotate-90">
                <circle
                  className="text-muted stroke-current"
                  strokeWidth="8"
                  fill="transparent"
                  r="44"
                  cx="48"
                  cy="48"
                />
                <circle
                  className={cn(
                    'stroke-current transition-all duration-300',
                    passRate >= 80 ? 'text-green-500' : passRate >= 60 ? 'text-yellow-500' : 'text-red-500'
                  )}
                  strokeWidth="8"
                  strokeLinecap="round"
                  fill="transparent"
                  r="44"
                  cx="48"
                  cy="48"
                  strokeDasharray={`${passRate * 2.76} 276`}
                />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-2xl font-bold">{passRate.toFixed(0)}%</span>
              </div>
            </div>
            <h3 className="text-lg font-semibold">Overall Pass Rate</h3>
            <Badge variant={getHealthVariant(passRate)} className="mt-2">
              {getHealthStatus(passRate)}
            </Badge>
          </CardContent>
        </Card>

        <div className="md:col-span-2">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <Card>
              <CardContent className="pt-6 text-center">
                <ClipboardList className="h-10 w-10 text-primary mx-auto mb-2" />
                <p className="text-3xl font-bold">{dashboard.totalTestCases || 0}</p>
                <p className="text-sm text-muted-foreground">Test Cases</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <CheckCircle className="h-10 w-10 text-green-500 mx-auto mb-2" />
                <p className="text-3xl font-bold text-green-500">{dashboard.passedRuns || 0}</p>
                <p className="text-sm text-muted-foreground">Passed</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <XCircle className="h-10 w-10 text-red-500 mx-auto mb-2" />
                <p className="text-3xl font-bold text-red-500">{dashboard.failedRuns || 0}</p>
                <p className="text-sm text-muted-foreground">Failed</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <Bug className="h-10 w-10 text-red-500 mx-auto mb-2" />
                <p className="text-3xl font-bold text-red-500">{openBugs}</p>
                <p className="text-sm text-muted-foreground">Open Bugs</p>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>

      {/* Critical Issues Alert */}
      {criticalBugs > 0 && (
        <div className="rounded-lg border border-destructive bg-destructive/10 p-4 mb-6 flex items-center gap-3">
          <AlertTriangle className="h-5 w-5 text-destructive" />
          <p className="font-medium text-destructive">
            {criticalBugs} Critical/Blocker bug{criticalBugs > 1 ? 's' : ''} require immediate attention!
          </p>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Test Coverage by Pitch */}
        <Card>
          <CardHeader>
            <CardTitle>Coverage by Pitch</CardTitle>
          </CardHeader>
          <CardContent>
            {dashboard.pitchCoverage && dashboard.pitchCoverage.length > 0 ? (
              <div className="space-y-4">
                {dashboard.pitchCoverage.map((coverage: TestCoverage) => (
                  <div key={coverage.pitchId}>
                    <div className="flex justify-between mb-1">
                      <span
                        className="text-sm cursor-pointer hover:text-primary transition-colors"
                        onClick={() => navigate(`/pitches/${coverage.pitchId}/test`)}
                      >
                        {coverage.pitchTitle}
                      </span>
                      <span className="text-sm text-muted-foreground">
                        {coverage.coverageScore?.toFixed(0)}%
                      </span>
                    </div>
                    <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                      <div
                        className={cn(
                          'h-full rounded-full transition-all',
                          getProgressColor(coverage.coverageScore || 0)
                        )}
                        style={{ width: `${coverage.coverageScore || 0}%` }}
                      />
                    </div>
                    <div className="flex gap-4 mt-1">
                      <span className="text-xs text-muted-foreground">
                        {coverage.totalTestCases} tests
                      </span>
                      <span className="text-xs text-green-500">
                        {coverage.passedRuns} passed
                      </span>
                      {coverage.failedRuns && coverage.failedRuns > 0 && (
                        <span className="text-xs text-red-500">
                          {coverage.failedRuns} failed
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-muted-foreground text-center">
                No pitch coverage data available
              </p>
            )}
          </CardContent>
        </Card>

        {/* Bug Summary */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0">
            <CardTitle>Bug Summary</CardTitle>
            <Button variant="ghost" size="sm" onClick={() => navigate('/qa/bugs')}>
              View All
            </Button>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-3 gap-4 mb-4">
              <div className="text-center">
                <p className="text-2xl font-bold">{totalBugs}</p>
                <p className="text-xs text-muted-foreground">Total</p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-bold text-red-500">{openBugs}</p>
                <p className="text-xs text-muted-foreground">Open</p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-bold text-green-500">{dashboard.resolvedBugs || 0}</p>
                <p className="text-xs text-muted-foreground">Resolved</p>
              </div>
            </div>

            <h4 className="text-sm font-semibold mb-2">By Severity</h4>
            <div className="space-y-2">
              {[
                { severity: 'BLOCKER', count: dashboard.blockerBugs || 0 },
                { severity: 'CRITICAL', count: dashboard.criticalBugs || 0 },
                { severity: 'MAJOR', count: dashboard.majorBugs || 0 },
                { severity: 'MINOR', count: dashboard.minorBugs || 0 },
                { severity: 'TRIVIAL', count: dashboard.trivialBugs || 0 },
              ].map(({ severity, count }) => (
                <div key={severity} className="flex items-center gap-2">
                  <Badge
                    variant={
                      ['BLOCKER', 'CRITICAL'].includes(severity)
                        ? 'destructive'
                        : severity === 'MAJOR'
                        ? 'secondary'
                        : 'outline'
                    }
                    className="w-20 justify-center text-xs"
                  >
                    {severity}
                  </Badge>
                  <div className="flex-1 h-1.5 bg-secondary rounded-full overflow-hidden">
                    <div
                      className="h-full bg-primary rounded-full transition-all"
                      style={{ width: `${totalBugs > 0 ? (count / totalBugs) * 100 : 0}%` }}
                    />
                  </div>
                  <span className="text-sm min-w-[24px] text-right">{count}</span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Test Case Status Summary */}
        <Card>
          <CardHeader>
            <CardTitle>Test Case Status</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-sm">Draft</span>
                <Badge variant="outline">{dashboard.draftTestCases || 0}</Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Ready</span>
                <Badge variant="secondary">{dashboard.readyTestCases || 0}</Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Approved</span>
                <Badge variant="default" className="bg-green-500 hover:bg-green-600">
                  {dashboard.approvedTestCases || 0}
                </Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Deprecated</span>
                <Badge variant="secondary" className="bg-yellow-500 hover:bg-yellow-600 text-white">
                  {dashboard.deprecatedTestCases || 0}
                </Badge>
              </div>
              {dashboard.aiGeneratedTestCases > 0 && (
                <div className="flex justify-between items-center">
                  <span className="text-sm">AI Generated</span>
                  <Badge variant="secondary" className="bg-purple-500 hover:bg-purple-600 text-white">
                    {dashboard.aiGeneratedTestCases}
                  </Badge>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        {/* Test Run Summary */}
        <Card>
          <CardHeader>
            <CardTitle>Test Run Summary</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-sm">Total Runs</span>
                <Badge variant="outline">{dashboard.totalRuns || 0}</Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Passed</span>
                <Badge variant="default" className="bg-green-500 hover:bg-green-600">
                  {dashboard.passedRuns || 0}
                </Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Failed</span>
                <Badge variant="destructive">{dashboard.failedRuns || 0}</Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Blocked</span>
                <Badge variant="secondary" className="bg-yellow-500 hover:bg-yellow-600 text-white">
                  {dashboard.blockedRuns || 0}
                </Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Skipped</span>
                <Badge variant="outline">{dashboard.skippedRuns || 0}</Badge>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-sm">Pending</span>
                <Badge variant="secondary">{dashboard.pendingRuns || 0}</Badge>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default CycleQADashboardPage;
