import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { Play, Plus, Bug, Sparkles, ClipboardList, Loader2 } from 'lucide-react';
import { cn } from '../lib/utils';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../components/ui/tabs';
import { Badge } from '../components/ui/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import qaTestManagementService from '../services/qaTestManagementService';
import { TestCase, TestRun, BugReport, TestCoverage, GenerateTestCasesRequest, TestCaseSuggestion, CreateTestCaseRequest } from '../types';
import TestCoverageBar from '../components/TestCoverageBar';
import TestExecutionPanel from '../components/TestExecutionPanel';
import AISuggestionPanel from '../components/AISuggestionPanel';
import { safeParseId } from '../utils/validation';

const PitchTestPage: React.FC = () => {
  const { i18n } = useTranslation();
  const { pitchId: pitchIdParam } = useParams<{ pitchId: string }>();
  const pitchId = safeParseId(pitchIdParam);
  const navigate = useNavigate();
  const [tab, setTab] = useState('test-cases');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [testRuns, setTestRuns] = useState<TestRun[]>([]);
  const [bugReports, setBugReports] = useState<BugReport[]>([]);
  const [coverage, setCoverage] = useState<TestCoverage | null>(null);

  useEffect(() => {
    if (pitchId) {
      loadData();
    }
  }, [pitchId]);

  if (!pitchId) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Card>
          <CardContent className="p-6">
            <p className="text-center text-destructive">Invalid pitch ID</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [testCasesRes, testRunsRes, bugsRes, coverageRes] = await Promise.all([
        qaTestManagementService.getTestCasesByPitch(pitchId),
        qaTestManagementService.getTestRunsByPitch(pitchId),
        qaTestManagementService.getBugReportsByPitch(pitchId),
        qaTestManagementService.getTestCoverageByPitch(pitchId),
      ]);
      setTestCases(testCasesRes.data);
      setTestRuns(testRunsRes.data);
      setBugReports(bugsRes.data);
      setCoverage(coverageRes.data);
    } catch (err) {
      setError('Failed to load test data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAIGenerate = async (_suggestions: TestCase[]) => {
    // Reload test cases to include newly generated ones
    const response = await qaTestManagementService.getTestCasesByPitch(pitchId);
    setTestCases(response.data);
  };

  const handleGenerateTestCases = async (request: GenerateTestCasesRequest) => {
    const response = await qaTestManagementService.generateTestCases(request);
    return response.data;
  };

  const handleAcceptSuggestion = async (suggestion: TestCaseSuggestion) => {
    const createRequest: CreateTestCaseRequest = {
      pitchId,
      title: suggestion.title,
      description: suggestion.description,
      preconditions: suggestion.preconditions,
      steps: suggestion.steps,
      expectedResult: suggestion.expectedResult,
      type: (suggestion.suggestedType as any) || 'FUNCTIONAL',
      priority: (suggestion.suggestedPriority as any) || 'MEDIUM',
      tags: suggestion.suggestedTags,
      aiGenerated: true,
    };
    await qaTestManagementService.createTestCase(createRequest);
    // Refresh test cases
    handleAIGenerate([]);
  };

  const getStatusCounts = () => {
    const passed = testRuns.filter((tr) => tr.status === 'PASSED').length;
    const failed = testRuns.filter((tr) => tr.status === 'FAILED').length;
    const blocked = testRuns.filter((tr) => tr.status === 'BLOCKED').length;
    const skipped = testRuns.filter((tr) => tr.status === 'SKIPPED').length;
    return { passed, failed, blocked, skipped, total: testRuns.length };
  };

  const stats = getStatusCounts();
  const openBugs = bugReports.filter((b) => ['OPEN', 'IN_PROGRESS', 'REOPENED'].includes(b.status)).length;
  const criticalBugs = bugReports.filter((b) => ['CRITICAL', 'BLOCKER'].includes(b.severity)).length;

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold">Test Management</h1>
          <p className="text-sm text-muted-foreground">
            Pitch #{pitchId}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => navigate(`/qa/test-cases/new?pitchId=${pitchId}`)}>
            <Plus className="mr-2 h-4 w-4" />
            Add Test Case
          </Button>
          <Button variant="outline" className="text-destructive border-destructive hover:bg-destructive hover:text-destructive-foreground" onClick={() => navigate(`/qa/bugs/new?pitchId=${pitchId}`)}>
            <Bug className="mr-2 h-4 w-4" />
            Report Bug
          </Button>
        </div>
      </div>

      {error && (
        <div className="mb-4 p-4 rounded-md bg-destructive/10 border border-destructive text-destructive">
          {error}
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <div>
          {coverage && <TestCoverageBar coverage={coverage} />}
        </div>
        <Card>
          <CardContent className="pt-6">
            <h3 className="text-sm font-medium mb-4">Quick Stats</h3>
            <div className="grid grid-cols-4 gap-4">
              <div className="flex flex-col items-center">
                <span className="text-3xl font-bold">{testCases.length}</span>
                <span className="text-xs text-muted-foreground">Test Cases</span>
              </div>
              <div className="flex flex-col items-center">
                <span className="text-3xl font-bold text-green-600">{stats.passed}</span>
                <span className="text-xs text-muted-foreground">Passed</span>
              </div>
              <div className="flex flex-col items-center">
                <span className="text-3xl font-bold text-red-600">{stats.failed}</span>
                <span className="text-xs text-muted-foreground">Failed</span>
              </div>
              <div className="flex flex-col items-center">
                <span className="text-3xl font-bold text-red-600">{openBugs}</span>
                <span className="text-xs text-muted-foreground">Open Bugs</span>
              </div>
            </div>
            {criticalBugs > 0 && (
              <div className="mt-4 p-3 rounded-md bg-destructive/10 border border-destructive text-destructive text-sm">
                {criticalBugs} critical/blocker bug{criticalBugs > 1 ? 's' : ''} require immediate attention!
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Tabs */}
      <Card>
        <Tabs value={tab} onValueChange={setTab}>
          <TabsList className="w-full justify-start rounded-none border-b bg-transparent p-0">
            <TabsTrigger 
              value="test-cases" 
              className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent"
            >
              <ClipboardList className="mr-2 h-4 w-4" />
              Test Cases ({testCases.length})
            </TabsTrigger>
            <TabsTrigger 
              value="test-runs" 
              className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent"
            >
              <Play className="mr-2 h-4 w-4" />
              Test Runs ({testRuns.length})
            </TabsTrigger>
            <TabsTrigger 
              value="bugs" 
              className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent"
            >
              <Bug className="mr-2 h-4 w-4" />
              Bugs ({bugReports.length})
            </TabsTrigger>
            <TabsTrigger 
              value="ai-suggestions" 
              className="rounded-none border-b-2 border-transparent data-[state=active]:border-primary data-[state=active]:bg-transparent"
            >
              <Sparkles className="mr-2 h-4 w-4" />
              AI Suggestions
            </TabsTrigger>
          </TabsList>

          <TabsContent value="test-cases" className="p-4">
            {testCases.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-muted-foreground mb-4">
                  No test cases yet for this pitch
                </p>
                <Button
                  onClick={() => navigate(`/qa/test-cases/new?pitchId=${pitchId}`)}
                  className="mr-2"
                >
                  <Plus className="mr-2 h-4 w-4" />
                  Create Test Case
                </Button>
                <Button
                  variant="outline"
                  onClick={() => setTab('ai-suggestions')}
                >
                  <Sparkles className="mr-2 h-4 w-4" />
                  Generate with AI
                </Button>
              </div>
            ) : (
              <div className="divide-y">
                {testCases.map((tc) => (
                  <div
                    key={tc.id}
                    className="flex items-center justify-between py-3 px-2 hover:bg-muted/50 cursor-pointer rounded-md"
                    onClick={() => navigate(`/qa/test-cases/${tc.id}`)}
                  >
                    <div className="flex items-center gap-3">
                      <ClipboardList className="h-5 w-5 text-muted-foreground" />
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium">{tc.testCaseKey}</span>
                          <span className="text-sm">{tc.title}</span>
                        </div>
                        <p className="text-xs text-muted-foreground">
                          {tc.totalRuns && tc.totalRuns > 0
                            ? `Pass rate: ${tc.passRate?.toFixed(0)}% (${tc.passedRuns}/${tc.totalRuns} runs)`
                            : 'Not yet executed'}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge variant="outline">{tc.type}</Badge>
                      <Badge
                        variant={
                          tc.priority === 'CRITICAL'
                            ? 'destructive'
                            : tc.priority === 'HIGH'
                            ? 'secondary'
                            : 'outline'
                        }
                        className={cn(
                          tc.priority === 'HIGH' && 'bg-amber-100 text-amber-800 border-amber-200'
                        )}
                      >
                        {tc.priority}
                      </Badge>
                      <Badge
                        variant={
                          tc.status === 'APPROVED'
                            ? 'default'
                            : tc.status === 'READY'
                            ? 'secondary'
                            : 'outline'
                        }
                        className={cn(
                          tc.status === 'APPROVED' && 'bg-green-100 text-green-800 border-green-200'
                        )}
                      >
                        {tc.status}
                      </Badge>
                      {tc.aiGenerated && (
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <Sparkles className="h-4 w-4 text-primary" />
                            </TooltipTrigger>
                            <TooltipContent>
                              <p>AI Generated</p>
                            </TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </TabsContent>

          <TabsContent value="test-runs" className="p-4">
            <TestExecutionPanel testRuns={testRuns} />
          </TabsContent>

          <TabsContent value="bugs" className="p-4">
            {bugReports.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-muted-foreground mb-4">
                  No bugs reported for this pitch
                </p>
                <Button
                  variant="destructive"
                  onClick={() => navigate(`/qa/bugs/new?pitchId=${pitchId}`)}
                >
                  <Bug className="mr-2 h-4 w-4" />
                  Report Bug
                </Button>
              </div>
            ) : (
              <div className="divide-y">
                {bugReports.map((bug) => (
                  <div
                    key={bug.id}
                    className="flex items-center justify-between py-3 px-2 hover:bg-muted/50 cursor-pointer rounded-md"
                    onClick={() => navigate(`/qa/bugs/${bug.id}`)}
                  >
                    <div className="flex items-center gap-3">
                      <Bug className="h-5 w-5 text-destructive" />
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-medium">{bug.bugKey}</span>
                          <span className="text-sm">{bug.title}</span>
                        </div>
                        <div className="flex items-center gap-4 text-xs text-muted-foreground">
                          <span>Reported by {bug.reporterName || 'Unknown'}</span>
                          <span>{formatLocalizedDate(new Date(bug.createdAt), i18n.language)}</span>
                        </div>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge
                        variant={
                          ['CRITICAL', 'BLOCKER'].includes(bug.severity)
                            ? 'destructive'
                            : bug.severity === 'MAJOR'
                            ? 'secondary'
                            : 'outline'
                        }
                        className={cn(
                          bug.severity === 'MAJOR' && 'bg-amber-100 text-amber-800 border-amber-200'
                        )}
                      >
                        {bug.severity}
                      </Badge>
                      <Badge
                        variant={
                          bug.status === 'OPEN'
                            ? 'destructive'
                            : bug.status === 'IN_PROGRESS'
                            ? 'secondary'
                            : ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(bug.status)
                            ? 'default'
                            : 'outline'
                        }
                        className={cn(
                          ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(bug.status) && 'bg-green-100 text-green-800 border-green-200'
                        )}
                      >
                        {bug.status.replace('_', ' ')}
                      </Badge>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </TabsContent>

          <TabsContent value="ai-suggestions" className="p-4">
            <AISuggestionPanel 
              pitchId={pitchId} 
              onGenerateTestCases={handleGenerateTestCases}
              onAcceptSuggestion={handleAcceptSuggestion}
            />
          </TabsContent>
        </Tabs>
      </Card>
    </div>
  );
};

export default PitchTestPage;
