import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { cycleService } from '../services/cycleService';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ClipboardList, PlusCircle } from 'lucide-react';
import { TaskStatus } from '../types';
import { cn } from '@/lib/utils';

interface SprintReportCardProps {
  cycleId: number;
  cycleName: string;
}

// Mirrors the status→color/label mapping used by KanbanBoard.tsx's KANBAN_COLUMNS,
// kept in the canonical TaskStatus order so status breakdowns stay visually consistent
// across the app.
const STATUS_CONFIG: { status: TaskStatus; labelKey: string; color: string }[] = [
  { status: 'BACKLOG', labelKey: 'backlogPage.statusOptions.backlog', color: 'bg-gray-500' },
  { status: 'TODO', labelKey: 'backlogPage.statusOptions.todo', color: 'bg-blue-500' },
  { status: 'IN_PROGRESS', labelKey: 'backlogPage.statusOptions.inProgress', color: 'bg-yellow-500' },
  { status: 'BLOCKED', labelKey: 'backlogPage.statusOptions.blocked', color: 'bg-red-500' },
  { status: 'IN_REVIEW', labelKey: 'backlogPage.statusOptions.inReview', color: 'bg-purple-500' },
  { status: 'DONE', labelKey: 'backlogPage.statusOptions.done', color: 'bg-green-500' },
  { status: 'CANCELLED', labelKey: 'backlogPage.statusOptions.cancelled', color: 'bg-slate-400' },
];

export function SprintReportCard({ cycleId, cycleName }: SprintReportCardProps) {
  const { t } = useTranslation();

  const { data, isLoading } = useQuery({
    queryKey: ['sprint-report', cycleId],
    queryFn: () => cycleService.getSprintReport(cycleId).then((r) => r.data),
    enabled: cycleId > 0,
  });

  const maxStatusCount = data
    ? Math.max(1, ...STATUS_CONFIG.map((s) => data.taskCountByStatus[s.status] ?? 0))
    : 1;

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-base">
          <ClipboardList className="h-4 w-4 text-primary" />
          {t('scrumReports.sprintReport.title')} — {cycleName}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-64 w-full" />
        ) : !data ? (
          <div className="flex h-64 items-center justify-center text-sm text-muted-foreground">
            {t('scrumReports.sprintReport.noData')}
          </div>
        ) : (
          <div className="space-y-6">
            {/* Completion rate */}
            <div className="flex items-center gap-6">
              <div>
                <p className="text-xs text-muted-foreground">
                  {t('scrumReports.sprintReport.completionRate')}
                </p>
                <p className="text-3xl font-bold">{(data.completionRate * 100).toFixed(0)}%</p>
              </div>
              <div className="flex gap-4 text-sm">
                <div>
                  <p className="text-xs text-muted-foreground">
                    {t('scrumReports.sprintReport.planned')}
                  </p>
                  <p className="font-medium">{data.plannedPoints}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">
                    {t('scrumReports.sprintReport.completed')}
                  </p>
                  <p className="font-medium">{data.completedPoints}</p>
                </div>
              </div>
            </div>

            {/* Status breakdown */}
            <div>
              <p className="text-xs font-medium text-muted-foreground mb-2">
                {t('scrumReports.sprintReport.byStatus')}
              </p>
              <div className="space-y-1.5">
                {STATUS_CONFIG.map(({ status, labelKey, color }) => {
                  const count = data.taskCountByStatus[status] ?? 0;
                  return (
                    <div key={status} className="flex items-center gap-2 text-xs">
                      <span className="w-24 shrink-0 truncate text-muted-foreground">
                        {t(labelKey)}
                      </span>
                      <div className="flex-1 h-2 rounded-full bg-muted overflow-hidden">
                        <div
                          className={cn('h-full rounded-full', color)}
                          style={{ width: `${(count / maxStatusCount) * 100}%` }}
                        />
                      </div>
                      <span className="w-6 shrink-0 text-end font-mono">{count}</span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Scope added callout */}
            {data.scopeAddedTaskCount > 0 && (
              <div className="flex items-start gap-2 rounded-md border border-amber-500/30 bg-amber-500/10 p-3 text-sm">
                <PlusCircle className="h-4 w-4 text-amber-600 shrink-0 mt-0.5" />
                <span>
                  {t('scrumReports.sprintReport.scopeAdded', {
                    count: data.scopeAddedTaskCount,
                    points: data.scopeAddedPoints,
                  })}
                </span>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default SprintReportCard;
