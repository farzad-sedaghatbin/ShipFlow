import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  Save,
  Loader2,
  AlertCircle,
  CheckCircle,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { safeParseId } from '../utils/validation';
import qaTestManagementService from '../services/qaTestManagementService';
import { TestCase, TestRunStatus, CreateTestRunRequest } from '../types';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';

const testRunStatuses: TestRunStatus[] = ['PENDING', 'RUNNING', 'PASSED', 'FAILED', 'BLOCKED', 'SKIPPED'];

const TestRunPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { id: idParam } = useParams<{ id: string }>();
  const id = safeParseId(idParam);

  const [testCase, setTestCase] = useState<TestCase | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const [formData, setFormData] = useState<CreateTestRunRequest>({
    testCaseId: id || 0,
    status: 'PENDING',
    notes: '',
    actualResult: '',
    durationSeconds: undefined,
    environment: '',
    buildVersion: '',
  });

  useEffect(() => {
    loadTestCase();
  }, [id]);

  const loadTestCase = async () => {
    setLoading(true);
    try {
      const response = await qaTestManagementService.getTestCaseById(id!);
      setTestCase(response.data);
    } catch (err) {
      setError(t('testRun.loadFailed'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    try {
      await qaTestManagementService.createTestRun({
        ...formData,
        testCaseId: id!,
      });
      setSuccess(true);
      setTimeout(() => navigate(`/qa/test-cases/${id}`), 1500);
    } catch (err: any) {
      setError(err.response?.data?.message || t('testRun.recordFailed'));
    } finally {
      setSaving(false);
    }
  };

  if (id === null) {
    return (
      <div className="p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">{t('testRun.invalidId')}</span>
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

  if (!testCase) {
    return (
      <div>
        <Button variant="ghost" onClick={() => navigate('/qa/test-cases')} className="mb-4 gap-2">
          <ArrowLeft className="h-4 w-4" />
          {t('common.back')}
        </Button>
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">{t('testRun.notFound')}</span>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex items-center gap-4 mb-6">
        <Button variant="ghost" onClick={() => navigate(`/qa/test-cases/${id}`)} className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          {t('common.back')}
        </Button>
        <h1 className="text-2xl font-bold">{t('testRun.title')}</h1>
      </div>

      {error && (
        <div className="flex items-center gap-2 p-3 mb-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      {success && (
        <div className="flex items-center gap-2 p-3 mb-4 rounded-lg bg-green-500/10 border border-green-500/20 text-green-500">
          <CheckCircle className="h-4 w-4" />
          <span className="text-sm">{t('testRun.recordSuccess')}</span>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>{t('testRun.testCaseDetails')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <h3 className="font-semibold text-lg">{testCase.title}</h3>
              <p className="text-sm text-muted-foreground mt-1">{testCase.description}</p>
            </div>

            {testCase.preconditions && (
              <div>
                <Label className="text-muted-foreground">{t('testRun.preconditions')}</Label>
                <div className="mt-1 p-3 rounded-lg bg-muted border border-border">
                  <pre className="whitespace-pre-wrap text-sm font-mono">{testCase.preconditions}</pre>
                </div>
              </div>
            )}

            <div>
              <Label className="text-muted-foreground">{t('testRun.testSteps')}</Label>
              <div className="mt-1 p-3 rounded-lg bg-primary text-primary-foreground">
                <pre className="whitespace-pre-wrap text-sm font-mono">
                  {testCase.steps || t('testRun.noSteps')}
                </pre>
              </div>
            </div>

            <div>
              <Label className="text-muted-foreground">{t('testRun.expectedResult')}</Label>
              <div className="mt-1 p-3 rounded-lg bg-green-500/10 border border-green-500/20">
                <pre className="whitespace-pre-wrap text-sm font-mono text-green-600 dark:text-green-400">
                  {testCase.expectedResult || t('testRun.noExpectedResult')}
                </pre>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>{t('testRun.recordTitle')}</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label>{t('testRun.status')} *</Label>
                <Select
                  value={formData.status}
                  onValueChange={(value) => setFormData({ ...formData, status: value as TestRunStatus })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('testRun.selectStatus')} />
                  </SelectTrigger>
                  <SelectContent>
                    {testRunStatuses.map((status) => (
                      <SelectItem key={status} value={status}>
                        {status}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>{t('testRun.actualResult')}</Label>
                <Textarea
                  rows={4}
                  placeholder={t('testRun.actualResultPlaceholder')}
                  value={formData.actualResult}
                  onChange={(e) => setFormData({ ...formData, actualResult: e.target.value })}
                />
              </div>

              <div className="space-y-2">
                <Label>{t('testRun.duration')}</Label>
                <Input
                  type="number"
                  value={formData.durationSeconds || ''}
                  onChange={(e) => setFormData({ ...formData, durationSeconds: e.target.value ? Number(e.target.value) : undefined })}
                />
              </div>

              <div className="relative">
                <div className="absolute inset-0 flex items-center">
                  <span className="w-full border-t border-border" />
                </div>
                <div className="relative flex justify-center text-xs uppercase">
                  <span className="bg-card px-2 text-muted-foreground">{t('testRun.environmentDetails')}</span>
                </div>
              </div>

              <div className="space-y-2">
                <Label>{t('testRun.environment')}</Label>
                <Input
                  placeholder={t('testRun.environmentPlaceholder')}
                  value={formData.environment}
                  onChange={(e) => setFormData({ ...formData, environment: e.target.value })}
                />
                <p className="text-xs text-muted-foreground">{t('testRun.environmentHint')}</p>
              </div>

              <div className="space-y-2">
                <Label>{t('testRun.buildVersion')}</Label>
                <Input
                  placeholder={t('testRun.buildPlaceholder')}
                  value={formData.buildVersion}
                  onChange={(e) => setFormData({ ...formData, buildVersion: e.target.value })}
                />
                <p className="text-xs text-muted-foreground">{t('testRun.buildHint')}</p>
              </div>

              <div className="space-y-2">
                <Label>{t('testRun.notes')}</Label>
                <Textarea
                  rows={3}
                  placeholder={t('testRun.notesPlaceholder')}
                  value={formData.notes}
                  onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                />
              </div>

              <div className="flex justify-end gap-2 pt-4">
                <Button 
                  type="button" 
                  variant="ghost" 
                  onClick={() => navigate(`/qa/test-cases/${id}`)}
                >
                  {t('common.cancel')}
                </Button>
                <Button
                  type="submit"
                  disabled={saving || success}
                  className="gap-2"
                >
                  {saving ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Save className="h-4 w-4" />
                  )}
                  {saving ? t('testRun.recording') : t('testRun.recordButton')}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default TestRunPage;
