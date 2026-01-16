import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { FileText, Sheet, Loader2 } from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { reportService } from '../services/reportService';
import { cycleService } from '../services/cycleService';
import { EnhancedCycleReport, Cycle } from '../types';
import StatusChip from '../components/StatusChip';
import EmptyState from '../components/EmptyState';
import { EmptyReportsIllustration } from '../components/illustrations';
import { cn } from '../lib/utils';

const COLORS = ['#2563eb', '#7c3aed', '#10b981', '#f59e0b', '#ef4444', '#6b7280'];

export default function Reports() {
  const { t } = useTranslation();
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<string>('');
  const [report, setReport] = useState<EnhancedCycleReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [reportLoading, setReportLoading] = useState(false);
  // i18n ready
  if (false) console.log(t('common.loading'));

  useEffect(() => {
    const abortController = new AbortController();
    loadCycles();
    return () => abortController.abort();
  }, []);

  useEffect(() => {
    const abortController = new AbortController();
    if (selectedCycle) {
      loadReport(Number(selectedCycle));
    }
    return () => abortController.abort();
  }, [selectedCycle]);

  const loadCycles = async () => {
    try {
      const response = await cycleService.getAll();
      setCycles(response.data);
      if (response.data.length > 0) {
        setSelectedCycle(String(response.data[0].id));
      }
    } catch (error: any) {
      if (error.name !== 'CanceledError') {
        console.error('Failed to load cycles:', error);
      }
    } finally {
      setLoading(false);
    }
  };

  const loadReport = async (cycleId: number) => {
    setReportLoading(true);
    try {
      const response = await reportService.getEnhancedCycleReport(cycleId);
      setReport(response.data);
    } catch (error) {
      console.error('Failed to load report:', error);
    } finally {
      setReportLoading(false);
    }
  };

  const handleExportCsv = async () => {
    if (!selectedCycle) return;
    try {
      const response = await reportService.exportCsv(Number(selectedCycle));
      const blob = new Blob([response.data as unknown as BlobPart], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `cycle_report_${selectedCycle}.csv`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to export CSV:', error);
    }
  };

  const handleExportPdf = async () => {
    if (!selectedCycle) return;
    try {
      const response = await reportService.exportPdf(Number(selectedCycle));
      const blob = new Blob([response.data as unknown as BlobPart], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `cycle_report_${selectedCycle}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to export PDF:', error);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  const pitchChartData = report?.pitchReports.map((p) => ({
    name: p.pitchTitle.length > 15 ? p.pitchTitle.substring(0, 15) + '...' : p.pitchTitle,
    appetite: p.appetiteHours,
    actual: p.actualHours,
  })) || [];

  const statusData = report
    ? [
        { name: t('reportsPage.completed'), value: report.completedPitches },
        { name: t('reportsPage.inProgress'), value: report.inProgressPitches },
        { name: t('reportsPage.notStarted'), value: report.notStartedPitches || 0 },
      ].filter((d) => d.value > 0)
    : [];

  const riskData = report?.riskDistribution
    ? [
        { name: t('reportsPage.lowRiskLabel'), value: report.riskDistribution.lowRiskCount, color: '#10b981' },
        { name: t('reportsPage.mediumRiskLabel'), value: report.riskDistribution.mediumRiskCount, color: '#f59e0b' },
        { name: t('reportsPage.highRiskLabel'), value: report.riskDistribution.highRiskCount, color: '#f97316' },
        { name: t('reportsPage.criticalRiskLabel'), value: report.riskDistribution.criticalRiskCount, color: '#ef4444' },
      ].filter((d) => d.value > 0)
    : [];

  const memberChartData = report?.memberReports.map((m) => ({
    name: m.memberName.split(' ')[0],
    hours: m.totalHours,
  })) || [];

  // Show loading state while cycles are being loaded
  if (loading) {
    return (
      <div>
        <h1 className="text-2xl font-bold mb-8">{t('reportsPage.title')}</h1>
        <div className="flex justify-center items-center min-h-[40vh]">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      </div>
    );
  }

  // Show empty state if no cycles exist
  if (cycles.length === 0) {
    return (
      <div>
        <h1 className="text-2xl font-bold mb-8">{t('reportsPage.title')}</h1>
        <Card>
          <CardContent className="py-12">
            <EmptyState
              illustration={<EmptyReportsIllustration />}
              title={t('reportsPage.noCyclesFound')}
              description={t('reportsPage.noCyclesDesc')}
              size="medium"
            />
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
        <h1 className="text-2xl font-bold">{t('reportsPage.title')}</h1>
        <div className="flex flex-col sm:flex-row gap-4 flex-wrap">
          <Select value={selectedCycle} onValueChange={setSelectedCycle}>
            <SelectTrigger className="w-full sm:w-[200px]">
              <SelectValue placeholder={t('reportsPage.selectCycle')} />
            </SelectTrigger>
            <SelectContent>
              {cycles.map((cycle) => (
                <SelectItem key={cycle.id} value={String(cycle.id)}>
                  {cycle.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <div className="flex gap-2 flex-wrap">
            <Button
              onClick={handleExportPdf}
              disabled={!selectedCycle}
              size="sm"
            >
              <FileText className="h-4 w-4 mr-2" />
              {t('reportsPage.exportPDF')}
            </Button>
            <Button
              variant="outline"
              onClick={handleExportCsv}
              disabled={!selectedCycle}
              size="sm"
            >
              <Sheet className="h-4 w-4 mr-2" />
              {t('reportsPage.exportCSV')}
            </Button>
          </div>
        </div>
      </div>

      {reportLoading ? (
        <div className="flex justify-center items-center min-h-[40vh]">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      ) : !report ? (
        <Card>
          <CardContent className="py-12">
            <EmptyState
              illustration={<EmptyReportsIllustration />}
              title={t('reportsPage.selectCycleToView')}
              description={t('reportsPage.selectCycleDesc')}
              size="medium"
            />
          </CardContent>
        </Card>
      ) : (
        <>
          {/* Summary Stats */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4 mb-8">
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">{t('reportsPage.totalPitches')}</p>
                <p className="text-3xl font-bold">{report.totalPitches}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">{t('reportsPage.completed')}</p>
                <p className="text-3xl font-bold text-green-600">{report.completedPitches}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">{t('reportsPage.inProgress')}</p>
                <p className="text-3xl font-bold text-primary">{report.inProgressPitches}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">{t('reportsPage.appetiteHours')}</p>
                <p className="text-3xl font-bold">{report.totalAppetiteHours.toFixed(0)}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">{t('reportsPage.actualHours')}</p>
                <p className={cn(
                  'text-3xl font-bold',
                  report.totalActualHours > report.totalAppetiteHours ? 'text-destructive' : 'text-green-600'
                )}>
                  {report.totalActualHours.toFixed(0)}
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">{t('reportsPage.efficiency')}</p>
                <p className={cn(
                  'text-3xl font-bold',
                  report.efficiencyPercentage > 100 ? 'text-destructive' : 'text-green-600'
                )}>
                  {report.efficiencyPercentage.toFixed(0)}%
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Variance Analysis */}
          <Card className="mb-8">
            <CardHeader>
              <CardTitle>{t('reportsPage.varianceAnalysis')}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-6">
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">{t('reportsPage.varianceHours')}</p>
                  <p className={cn(
                    'text-2xl font-bold',
                    report.varianceHours > 0 ? 'text-destructive' : 'text-green-600'
                  )}>
                    {report.varianceHours > 0 ? '+' : ''}{report.varianceHours.toFixed(1)}h
                  </p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">{t('reportsPage.variancePercent')}</p>
                  <p className={cn(
                    'text-2xl font-bold',
                    report.variancePercentage > 0 ? 'text-destructive' : 'text-green-600'
                  )}>
                    {report.variancePercentage > 0 ? '+' : ''}{report.variancePercentage.toFixed(1)}%
                  </p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">{t('reportsPage.teamMembers')}</p>
                  <p className="text-2xl font-bold">{report.totalTeamMembers}</p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">{t('reportsPage.avgHoursPerMember')}</p>
                  <p className="text-2xl font-bold">{report.averageHoursPerMember.toFixed(1)}h</p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Out-of-Scope Work Statistics */}
          <Card className="mb-8">
            <CardHeader>
              <CardTitle>{t('reportsPage.outOfScopeWork')}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-6">
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">{t('reportsPage.totalTasks')}</p>
                  <p className="text-2xl font-bold">{report.totalTasks}</p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">{t('reportsPage.completedTasks')}</p>
                  <p className="text-2xl font-bold text-green-600">{report.completedTasks}</p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">{t('reportsPage.estimateHours')}</p>
                  <p className="text-2xl font-bold">{report.totalTaskEstimateHours.toFixed(0)}h</p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">Actual Hours</p>
                  <p className={cn(
                    'text-2xl font-bold',
                    report.totalTaskActualHours > report.totalTaskEstimateHours ? 'text-destructive' : 'text-green-600'
                  )}>
                    {report.totalTaskActualHours.toFixed(0)}h
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Charts */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
            <Card>
              <CardHeader>
                <CardTitle>{t('reportsPage.appetiteVsActual')}</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={pitchChartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="appetite" name="Appetite (h)" fill="#2563eb" />
                    <Bar dataKey="actual" name="Actual (h)" fill="#10b981" />
                  </BarChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
            <Card>
              <CardHeader>
                <CardTitle>{t('reportsPage.pitchStatusDist')}</CardTitle>
              </CardHeader>
              <CardContent>
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={statusData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {statusData.map((_, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              </CardContent>
            </Card>
          </div>

          {/* Risk Distribution Section */}
          <Card className="mb-8">
            <CardHeader>
              <CardTitle>{t('reportsPage.riskDistribution')}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div>
                  <ResponsiveContainer width="100%" height={250}>
                    <PieChart>
                      <Pie
                        data={riskData}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                        outerRadius={80}
                        fill="#8884d8"
                        dataKey="value"
                      >
                        {riskData.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="space-y-4">
                  <div className="grid grid-cols-2 gap-4">
                    <div className="text-center p-4 bg-green-50 dark:bg-green-900/20 rounded-lg">
                      <p className="text-sm text-muted-foreground">{t('reportsPage.lowRisk')}</p>
                      <p className="text-2xl font-bold text-green-600">{report.riskDistribution?.lowRiskCount || 0}</p>
                    </div>
                    <div className="text-center p-4 bg-yellow-50 dark:bg-yellow-900/20 rounded-lg">
                      <p className="text-sm text-muted-foreground">{t('reportsPage.mediumRisk')}</p>
                      <p className="text-2xl font-bold text-yellow-600">{report.riskDistribution?.mediumRiskCount || 0}</p>
                    </div>
                    <div className="text-center p-4 bg-orange-50 dark:bg-orange-900/20 rounded-lg">
                      <p className="text-sm text-muted-foreground">{t('reportsPage.highRisk')}</p>
                      <p className="text-2xl font-bold text-orange-600">{report.riskDistribution?.highRiskCount || 0}</p>
                    </div>
                    <div className="text-center p-4 bg-red-50 dark:bg-red-900/20 rounded-lg">
                      <p className="text-sm text-muted-foreground">{t('reportsPage.criticalRisk')}</p>
                      <p className="text-2xl font-bold text-red-600">{report.riskDistribution?.criticalRiskCount || 0}</p>
                    </div>
                  </div>
                  <div className="pt-4 border-t">
                    <div className="grid grid-cols-3 gap-4 text-center">
                      <div>
                        <p className="text-xs text-muted-foreground">{t('reportsPage.avgRiskScore')}</p>
                        <p className="text-lg font-semibold">{report.riskDistribution?.averageRiskScore?.toFixed(1) || '0'}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">{t('reportsPage.minScore')}</p>
                        <p className="text-lg font-semibold">{report.riskDistribution?.minRiskScore?.toFixed(1) || '0'}</p>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground">{t('reportsPage.maxScore')}</p>
                        <p className="text-lg font-semibold">{report.riskDistribution?.maxRiskScore?.toFixed(1) || '0'}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Team Performance Section */}
          {(report.topPerformers.length > 0 || report.overBudgetPitches.length > 0) && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
              {report.topPerformers.length > 0 && (
                <Card>
                  <CardHeader>
                    <CardTitle>{t('reportsPage.topPerformers')}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <ul className="space-y-2">
                      {report.topPerformers.map((performer, index) => (
                        <li key={index} className="flex items-center gap-2">
                          <Badge variant="default">{index + 1}</Badge>
                          <span className="font-medium">{performer}</span>
                        </li>
                      ))}
                    </ul>
                  </CardContent>
                </Card>
              )}
              {report.overBudgetPitches.length > 0 && (
                <Card>
                  <CardHeader>
                    <CardTitle>{t('reportsPage.overBudgetPitches')}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <ul className="space-y-2">
                      {report.overBudgetPitches.map((pitch, index) => (
                        <li key={index} className="flex items-center gap-2">
                          <Badge variant="destructive">{t('reportsPage.overBudget')}</Badge>
                          <span className="font-medium">{pitch}</span>
                        </li>
                      ))}
                    </ul>
                  </CardContent>
                </Card>
              )}
            </div>
          )}

          {/* Member Hours Chart */}
          <Card className="mb-8">
            <CardHeader>
              <CardTitle>{t('reportsPage.hoursByMember')}</CardTitle>
            </CardHeader>
            <CardContent>
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={memberChartData} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis type="number" />
                  <YAxis dataKey="name" type="category" />
                  <Tooltip />
                  <Bar dataKey="hours" name="Hours" fill="#7c3aed" />
                </BarChart>
              </ResponsiveContainer>
            </CardContent>
          </Card>

          {/* Pitch Details Table */}
          <Card className="mb-8">
            <CardHeader>
              <CardTitle>{t('reportsPage.pitchDetails')}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="border rounded-lg overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t('reportsPage.pitch')}</TableHead>
                      <TableHead>{t('reportsPage.team')}</TableHead>
                      <TableHead>{t('reportsPage.status')}</TableHead>
                      <TableHead className="text-right">{t('reportsPage.appetiteDays')}</TableHead>
                      <TableHead className="text-right">{t('reportsPage.appetiteHours')}</TableHead>
                      <TableHead className="text-right">{t('reportsPage.actualHours')}</TableHead>
                      <TableHead className="text-right">{t('reportsPage.variance')}</TableHead>
                      <TableHead className="text-center">{t('reportsPage.isOverBudget')}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {report.pitchReports.map((pitch) => (
                      <TableRow key={pitch.pitchId}>
                        <TableCell className="font-medium">{pitch.pitchTitle}</TableCell>
                        <TableCell>{pitch.teamName}</TableCell>
                        <TableCell>
                          <StatusChip status={pitch.status} />
                        </TableCell>
                        <TableCell className="text-right">{pitch.appetiteDays}</TableCell>
                        <TableCell className="text-right">{pitch.appetiteHours.toFixed(0)}</TableCell>
                        <TableCell className="text-right">{pitch.actualHours.toFixed(1)}</TableCell>
                        <TableCell className={cn(
                          'text-right',
                          pitch.varianceHours > 0 ? 'text-destructive' : 'text-green-600'
                        )}>
                          {pitch.varianceHours > 0 ? '+' : ''}{pitch.varianceHours.toFixed(1)}
                        </TableCell>
                        <TableCell className="text-center">
                          <Badge variant={pitch.isOverBudget ? 'destructive' : 'default'}>
                            {pitch.isOverBudget ? t('reportsPage.yes') : t('reportsPage.no')}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>

          {/* Member Details Table */}
          <Card>
            <CardHeader>
              <CardTitle>{t('reportsPage.memberWorkSummary')}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="border rounded-lg overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t('reportsPage.member')}</TableHead>
                      <TableHead>{t('reportsPage.role')}</TableHead>
                      <TableHead>{t('reportsPage.team')}</TableHead>
                      <TableHead className="text-right">{t('reportsPage.totalHours')}</TableHead>
                      <TableHead className="text-right">{t('reportsPage.workDays')}</TableHead>
                      <TableHead className="text-right">{t('reportsPage.avgHoursPerDay')}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {report.memberReports.map((member) => (
                      <TableRow key={member.memberId}>
                        <TableCell className="font-medium">{member.memberName}</TableCell>
                        <TableCell>
                          <Badge variant="outline">{member.role}</Badge>
                        </TableCell>
                        <TableCell>{member.teamName}</TableCell>
                        <TableCell className="text-right">{member.totalHours.toFixed(1)}</TableCell>
                        <TableCell className="text-right">{member.workDays}</TableCell>
                        <TableCell className="text-right">{member.avgHoursPerDay.toFixed(1)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
