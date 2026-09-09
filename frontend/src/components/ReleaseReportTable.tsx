import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { cycleService } from '../services/cycleService';
import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Rocket } from 'lucide-react';
import { ReleaseStatus } from '../types';
import { formatLocalizedDate } from '../utils/dateLocalization';

interface ReleaseReportTableProps {
  projectId: number;
}

// Backend statuses are SNAKE_CASE (e.g. "IN_PROGRESS"); the releases.status.* i18n
// keys are camelCase (e.g. "inProgress", matching ReleaseListPage.tsx's usage) —
// a plain toLowerCase() would miss the underscore and look up a key that doesn't
// exist in fa.json.
function toCamelCase(value: string): string {
  return value.toLowerCase().replace(/_([a-z])/g, (_, letter: string) => letter.toUpperCase());
}

// Mirrors ReleaseListPage.tsx's getStatusBadgeVariant so release status badges
// stay visually consistent across the app.
const getStatusBadgeVariant = (
  status: string,
): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (status as ReleaseStatus) {
    case 'RELEASED': return 'default';
    case 'IN_PROGRESS': return 'secondary';
    case 'STAGING': return 'secondary';
    case 'PLANNING': return 'outline';
    case 'CANCELLED': return 'destructive';
    default: return 'outline';
  }
};

export function ReleaseReportTable({ projectId }: ReleaseReportTableProps) {
  const { t, i18n } = useTranslation();

  const { data, isLoading } = useQuery({
    queryKey: ['release-report', projectId],
    queryFn: () => cycleService.getReleaseReport(projectId).then((r) => r.data),
    enabled: projectId > 0,
  });

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="flex items-center gap-2 text-base">
          <Rocket className="h-4 w-4 text-primary" />
          {t('scrumReports.releaseReport.title')}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-64 w-full" />
        ) : !data || data.length === 0 ? (
          <div className="flex h-64 items-center justify-center text-sm text-muted-foreground">
            {t('scrumReports.releaseReport.noData')}
          </div>
        ) : (
          <div className="border rounded-lg overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('scrumReports.releaseReport.release')}</TableHead>
                  <TableHead>{t('scrumReports.releaseReport.status')}</TableHead>
                  <TableHead className="text-end">{t('scrumReports.releaseReport.tasks')}</TableHead>
                  <TableHead className="text-end">{t('scrumReports.releaseReport.points')}</TableHead>
                  <TableHead>{t('scrumReports.releaseReport.targetDate')}</TableHead>
                  <TableHead>{t('scrumReports.releaseReport.releaseDate')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.map((release) => (
                  <TableRow key={release.releaseId}>
                    <TableCell className="font-medium">
                      {release.releaseName}
                      {release.version && (
                        <span className="ms-2 text-xs text-muted-foreground">{release.version}</span>
                      )}
                    </TableCell>
                    <TableCell>
                      <Badge variant={getStatusBadgeVariant(release.status)}>
                        {t(`releases.status.${toCamelCase(release.status)}`)}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-end">
                      {release.completedTaskCount} / {release.taskCount}
                    </TableCell>
                    <TableCell className="text-end">
                      {release.completedPoints} / {release.plannedPoints}
                    </TableCell>
                    <TableCell>
                      {release.targetDate
                        ? formatLocalizedDate(release.targetDate, i18n.language)
                        : t('scrumReports.releaseReport.noDate')}
                    </TableCell>
                    <TableCell>
                      {release.releaseDate
                        ? formatLocalizedDate(release.releaseDate, i18n.language)
                        : t('scrumReports.releaseReport.noDate')}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}

export default ReleaseReportTable;
