import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDateTime } from '../utils/dateLocalization';
import {
  ArrowLeft,
  Pencil,
  Play,
  Loader2,
  AlertCircle,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { safeParseId } from '../utils/validation';
import qaTestManagementService from '../services/qaTestManagementService';
import { TestCase, TestRun } from '../types';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { cn } from '../lib/utils';

const getPriorityStyle = (priority: string): string => {
  switch (priority) {
    case 'CRITICAL': return 'bg-red-500/10 text-red-500';
    case 'HIGH': return 'bg-amber-500/10 text-amber-500';
    case 'MEDIUM': return 'bg-blue-500/10 text-blue-500';
    default: return 'bg-muted text-muted-foreground';
  }
};

const getStatusStyle = (status: string): string => {
  switch (status) {
    case 'APPROVED': return 'bg-green-500/10 text-green-500';
    case 'READY': return 'bg-blue-500/10 text-blue-500';
    case 'DRAFT': return 'bg-amber-500/10 text-amber-500';
    case 'DEPRECATED': return 'bg-red-500/10 text-red-500';
    default: return 'bg-muted text-muted-foreground';
  }
};

const getRunStatusStyle = (status: string): string => {
  switch (status) {
    case 'PASSED': return 'bg-green-500/10 text-green-500';
    case 'FAILED': return 'bg-red-500/10 text-red-500';
    case 'BLOCKED': return 'bg-amber-500/10 text-amber-500';
    default: return 'bg-blue-500/10 text-blue-500';
  }
};

const TestCaseDetailPage: React.FC = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { id: idParam } = useParams<{ id: string }>();
  const id = safeParseId(idParam);

  const [testCase, setTestCase] = useState<TestCase | null>(null);
  const [testRuns, setTestRuns] = useState<TestRun[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadTestCase();
  }, [id]);

  const loadTestCase = async () => {
    if (id === null) {
      setError(t('testCaseDetail.invalidId'));
      setLoading(false);
      return;
    }
    
    setLoading(true);
    try {
      const [tcResponse, runsResponse] = await Promise.all([
        qaTestManagementService.getTestCaseById(id),
        qaTestManagementService.getTestRunsByTestCase(id),
      ]);
      setTestCase(tcResponse.data);
      setTestRuns(runsResponse.data);
    } catch (err) {
      setError(t('testCaseDetail.loadFailed'));
    } finally {
      setLoading(false);
    }
  };

  if (id === null) {
    return (
      <div className="p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">{t('testCaseDetail.invalidId')}</span>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (error || !testCase) {
    return (
      <div>
        <Button variant="ghost" onClick={() => navigate('/qa/test-cases')} className="mb-4 gap-2">
          <ArrowLeft className="h-4 w-4" />
          {t('testCaseDetail.back')}
        </Button>
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">{error || t('testCaseDetail.notFound')}</span>
        </div>
      </div>
    );
  }

  const tags = testCase.tags?.split(',').map(t => t.trim()).filter(Boolean) || [];

  return (
    <div className="p-6">
      <div className="flex items-center gap-4 mb-6 flex-wrap">
        <Button variant="ghost" onClick={() => navigate('/qa/test-cases')} className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          {t('testCaseDetail.back')}
        </Button>
        <div className="flex-1 min-w-0">
          <h1 className="text-2xl font-bold truncate">{testCase.title}</h1>
          <p className="text-sm text-muted-foreground">{t('testCaseDetail.title', { id: testCase.id })}</p>
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => navigate(`/qa/test-cases/${id}/edit`)}
            className="gap-2"
          >
            <Pencil className="h-4 w-4" />
            {t('testCaseDetail.edit')}
          </Button>
          <Button
            onClick={() => navigate(`/qa/test-cases/${id}/run`)}
            className="gap-2"
          >
            <Play className="h-4 w-4" />
            {t('testCaseDetail.runTest')}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>{t('testCaseDetail.details')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.description')}</p>
                <p className="text-sm">{testCase.description || t('testCaseDetail.noDescription')}</p>
              </div>

              <div>
                <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.preconditions')}</p>
                <div className="p-3 rounded-lg bg-muted border border-border">
                  <pre className="whitespace-pre-wrap text-sm font-mono">
                    {testCase.preconditions || t('testCaseDetail.noPreconditions')}
                  </pre>
                </div>
              </div>

              <div>
                <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.testSteps')}</p>
                <div className="p-3 rounded-lg bg-muted border border-border">
                  <pre className="whitespace-pre-wrap text-sm font-mono">
                    {testCase.steps || t('testCaseDetail.noSteps')}
                  </pre>
                </div>
              </div>

              <div>
                <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.expectedResult')}</p>
                <div className="p-3 rounded-lg bg-muted border border-border">
                  <pre className="whitespace-pre-wrap text-sm font-mono">
                    {testCase.expectedResult || t('testCaseDetail.noExpectedResult')}
                  </pre>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t('testCaseDetail.testRunHistory')}</CardTitle>
            </CardHeader>
            <CardContent>
              {testRuns.length === 0 ? (
                <p className="text-sm text-muted-foreground">{t('testCaseDetail.noRuns')}</p>
              ) : (
                <div className="space-y-3">
                  {testRuns.map((run) => (
                    <div key={run.id} className="p-3 rounded-lg border border-border">
                      <div className="flex items-center gap-2 mb-2">
                        <Badge className={cn("text-xs", getRunStatusStyle(run.status))}>
                          {run.status}
                        </Badge>
                        <span className="text-sm text-muted-foreground">
                          {run.executedAt ? formatLocalizedDateTime(new Date(run.executedAt), i18n.language) : 'Not executed'}
                        </span>
                      </div>
                      <div className="space-y-1 text-sm text-muted-foreground">
                        {run.environment && <p>Environment: {run.environment}</p>}
                        {run.buildVersion && <p>Build/Version: {run.buildVersion}</p>}
                        {run.durationSeconds && <p>Duration: {run.durationSeconds}s</p>}
                        {run.actualResult && <p>Actual Result: {run.actualResult}</p>}
                        {run.notes && <p>Notes: {run.notes}</p>}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        <div>
          <Card>
            <CardHeader>
              <CardTitle>{t('testCaseDetail.properties')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.status')}</p>
                <Badge className={cn("text-xs", getStatusStyle(testCase.status))}>
                  {testCase.status}
                </Badge>
              </div>

              <div>
                <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.type')}</p>
                <Badge variant="outline" className="text-xs">{testCase.type}</Badge>
              </div>

              <div>
                <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.priority')}</p>
                <Badge className={cn("text-xs", getPriorityStyle(testCase.priority))}>
                  {testCase.priority}
                </Badge>
              </div>

              {testCase.pitchId && (
                <div>
                  <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.pitch')}</p>
                  <p className="text-sm">{testCase.pitchTitle || t('testCaseDetail.pitchId', { id: testCase.pitchId })}</p>
                </div>
              )}

              {testCase.estimatedMinutes && (
                <div>
                  <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.estimatedTime')}</p>
                  <p className="text-sm">{t('testCaseDetail.minutes', { minutes: testCase.estimatedMinutes })}</p>
                </div>
              )}

              {tags.length > 0 && (
                <div>
                  <p className="text-sm text-muted-foreground mb-2">{t('testCaseDetail.tags')}</p>
                  <div className="flex flex-wrap gap-1">
                    {tags.map((tag, idx) => (
                      <Badge key={idx} variant="secondary" className="text-xs">{tag}</Badge>
                    ))}
                  </div>
                </div>
              )}

              <div className="border-t border-border pt-4" />

              <div>
                <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.created')}</p>
                <p className="text-sm">
                  {testCase.createdAt ? formatLocalizedDateTime(new Date(testCase.createdAt), i18n.language) : t('testCaseDetail.unknown')}
                </p>
              </div>

              {testCase.updatedAt && (
                <div>
                  <p className="text-sm text-muted-foreground mb-1">{t('testCaseDetail.lastUpdated')}</p>
                  <p className="text-sm">{formatLocalizedDateTime(new Date(testCase.updatedAt), i18n.language)}</p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default TestCaseDetailPage;
