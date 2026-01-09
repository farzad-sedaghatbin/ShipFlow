import { useEffect, useState } from 'react';
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
import { CycleReport, Cycle } from '../types';
import StatusChip from '../components/StatusChip';
import EmptyState from '../components/EmptyState';
import { EmptyReportsIllustration } from '../components/illustrations';
import { cn } from '../lib/utils';

const COLORS = ['#2563eb', '#7c3aed', '#10b981', '#f59e0b', '#ef4444', '#6b7280'];

export default function Reports() {
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<string>('');
  const [report, setReport] = useState<CycleReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [reportLoading, setReportLoading] = useState(false);

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
      const response = await reportService.getCycleReport(cycleId);
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
        { name: 'Completed', value: report.completedPitches },
        { name: 'In Progress', value: report.inProgressPitches },
        { name: 'Pending', value: report.totalPitches - report.completedPitches - report.inProgressPitches },
      ].filter((d) => d.value > 0)
    : [];

  const memberChartData = report?.memberReports.map((m) => ({
    name: m.memberName.split(' ')[0],
    hours: m.totalHours,
  })) || [];

  return (
    <div>
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-8">
        <h1 className="text-2xl font-bold">Reports</h1>
        <div className="flex flex-col sm:flex-row gap-4 flex-wrap">
          <Select value={selectedCycle} onValueChange={setSelectedCycle}>
            <SelectTrigger className="w-full sm:w-[200px]">
              <SelectValue placeholder="Select cycle" />
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
              Export PDF
            </Button>
            <Button
              variant="outline"
              onClick={handleExportCsv}
              disabled={!selectedCycle}
              size="sm"
            >
              <Sheet className="h-4 w-4 mr-2" />
              Export CSV
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
              title="Select a cycle to view reports"
              description="Choose a cycle from the dropdown above to see detailed analytics and export options"
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
                <p className="text-sm text-muted-foreground">Total Pitches</p>
                <p className="text-3xl font-bold">{report.totalPitches}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">Completed</p>
                <p className="text-3xl font-bold text-green-600">{report.completedPitches}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">In Progress</p>
                <p className="text-3xl font-bold text-primary">{report.inProgressPitches}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">Appetite (h)</p>
                <p className="text-3xl font-bold">{report.totalAppetiteHours.toFixed(0)}</p>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="pt-6 text-center">
                <p className="text-sm text-muted-foreground">Actual (h)</p>
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
                <p className="text-sm text-muted-foreground">Efficiency</p>
                <p className={cn(
                  'text-3xl font-bold',
                  report.efficiencyPercentage > 100 ? 'text-destructive' : 'text-green-600'
                )}>
                  {report.efficiencyPercentage.toFixed(0)}%
                </p>
              </CardContent>
            </Card>
          </div>

          {/* Out-of-Scope Work Statistics */}
          <Card className="mb-8">
            <CardHeader>
              <CardTitle>Out-of-Scope Work (Tasks)</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-6">
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">Total Tasks</p>
                  <p className="text-2xl font-bold">{report.totalTasks}</p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">Completed Tasks</p>
                  <p className="text-2xl font-bold text-green-600">{report.completedTasks}</p>
                </div>
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">Est. Hours</p>
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
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
            <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle>Appetite vs Actual Hours by Pitch</CardTitle>
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
                <CardTitle>Pitch Status Distribution</CardTitle>
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

          {/* Member Hours Chart */}
          <Card className="mb-8">
            <CardHeader>
              <CardTitle>Hours by Team Member</CardTitle>
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
              <CardTitle>Pitch Details</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="border rounded-lg overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Pitch</TableHead>
                      <TableHead>Team</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead className="text-right">Appetite (days)</TableHead>
                      <TableHead className="text-right">Appetite (h)</TableHead>
                      <TableHead className="text-right">Actual (h)</TableHead>
                      <TableHead className="text-right">Variance (h)</TableHead>
                      <TableHead className="text-center">Over Budget</TableHead>
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
                            {pitch.isOverBudget ? 'Yes' : 'No'}
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
              <CardTitle>Member Work Summary</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="border rounded-lg overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Member</TableHead>
                      <TableHead>Role</TableHead>
                      <TableHead>Team</TableHead>
                      <TableHead className="text-right">Total Hours</TableHead>
                      <TableHead className="text-right">Work Days</TableHead>
                      <TableHead className="text-right">Avg Hours/Day</TableHead>
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
