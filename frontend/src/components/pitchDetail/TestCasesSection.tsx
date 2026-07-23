import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { ClipboardList, Loader2, Sparkles } from 'lucide-react';
import { cn } from '../../lib/utils';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import qaTestManagementService from '../../services/qaTestManagementService';
import { TestCase } from '../../types';

interface TestCasesSectionProps {
  pitchId?: number;
  taskId?: number;
}

export function TestCasesSection({ pitchId, taskId }: TestCasesSectionProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const { data: testCases = [], isLoading } = useQuery<TestCase[]>({
    queryKey: taskId ? ['test-cases', 'task', taskId] : ['test-cases', 'pitch', pitchId],
    queryFn: () =>
      taskId
        ? qaTestManagementService.getTestCasesByTask(taskId).then((r) => r.data)
        : qaTestManagementService.getTestCasesByPitch(pitchId!).then((r) => r.data),
    enabled: taskId != null || pitchId != null,
  });

  const addOrViewHref = taskId ? `/qa/test-cases/new?taskId=${taskId}` : `/pitches/${pitchId}/test`;

  return (
    <Card data-tour={taskId ? undefined : 'pitch-test-cases'}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0">
        <CardTitle className="flex items-center gap-2 text-sm font-semibold">
          <ClipboardList className="h-4 w-4" />
          {t('testCasesSection.title')}
          {testCases.length > 0 && (
            <span className="rounded-full bg-muted px-2 py-0.5 text-xs">{testCases.length}</span>
          )}
        </CardTitle>
        <Button variant="outline" size="sm" onClick={() => navigate(addOrViewHref)}>
          {taskId ? t('testCasesSection.addNew') : t('testCasesSection.viewAll')}
        </Button>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="flex justify-center py-4">
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        ) : testCases.length === 0 ? (
          <div className="py-4 text-center">
            <p className="mb-3 text-sm text-muted-foreground">
              {taskId ? t('testCasesSection.emptyTask') : t('testCasesSection.empty')}
            </p>
            <Button variant="outline" size="sm" onClick={() => navigate(addOrViewHref)}>
              {taskId ? null : <Sparkles className="mr-2 h-4 w-4" />}
              {taskId ? t('testCasesSection.addNew') : t('testCasesSection.generateOrAdd')}
            </Button>
          </div>
        ) : (
          <ul className="divide-y divide-border rounded-md border">
            {testCases.slice(0, 5).map((tc) => (
              <li
                key={tc.id}
                className="flex cursor-pointer items-center justify-between gap-3 px-3 py-2.5 hover:bg-muted/50"
                onClick={() => navigate(`/qa/test-cases/${tc.id}`)}
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-medium text-muted-foreground">{tc.testCaseKey}</span>
                    <span className="truncate text-sm font-medium">{tc.title}</span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {tc.totalRuns && tc.totalRuns > 0
                      ? t('testCasesSection.passRate', {
                          rate: tc.passRate?.toFixed(0),
                          passed: tc.passedRuns,
                          total: tc.totalRuns,
                        })
                      : t('testCasesSection.notExecuted')}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-1.5">
                  <Badge variant="outline">{tc.type}</Badge>
                  <Badge
                    variant={
                      tc.status === 'APPROVED' ? 'default' : tc.status === 'READY' ? 'secondary' : 'outline'
                    }
                    className={cn(tc.status === 'APPROVED' && 'border-green-200 bg-green-100 text-green-800')}
                  >
                    {tc.status}
                  </Badge>
                </div>
              </li>
            ))}
          </ul>
        )}
        {testCases.length > 5 && (
          <p className="mt-2 text-center text-xs text-muted-foreground">
            {taskId
              ? t('testCasesSection.andMoreTask', { count: testCases.length - 5 })
              : t('testCasesSection.andMore', { count: testCases.length - 5 })}
          </p>
        )}
      </CardContent>
    </Card>
  );
}
