import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Plus,
  Search,
  Pencil,
  Play,
  Eye,
  Sparkles,
  AlertCircle,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { Label } from '../components/ui/label';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import { cn } from '../lib/utils';
import qaTestManagementService from '../services/qaTestManagementService';
import { SoftDeleteButton } from '../components/SoftDeleteButton';
import { cycleService } from '../services/cycleService';
import { pitchService } from '../services/pitchService';
import { useProject } from '../contexts';
import { TestCasesSkeleton } from '../components/Skeletons';
import {
  TestCase,
  TestCaseStatus,
  TestCaseType,
  TestCasePriority,
  Cycle,
  Pitch,
} from '../types';

const priorityVariants: Record<TestCasePriority, 'secondary' | 'default' | 'warning' | 'destructive'> = {
  LOW: 'secondary',
  MEDIUM: 'default',
  HIGH: 'warning',
  CRITICAL: 'destructive',
};

const statusVariants: Record<TestCaseStatus, string> = {
  DRAFT: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-300',
  READY: 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300',
  APPROVED: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-300',
  DEPRECATED: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-300',
  ARCHIVED: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300',
};

const TestCasesPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { currentProject, isAllProjectsSelected, isKanbanProject, isSwitchingProject, notifyProjectSwitchComplete } = useProject();
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<TestCaseStatus | 'all'>('all');
  const [typeFilter, setTypeFilter] = useState<TestCaseType | 'all'>('all');
  const [priorityFilter, setPriorityFilter] = useState<TestCasePriority | 'all'>('all');
  const [cycleFilter, setCycleFilter] = useState<number | 'all'>('all');
  const [pitchFilter, setPitchFilter] = useState<number | 'all'>('all');
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);

  // Filter cycles by current project
  const filteredCycles = useMemo(() => {
    if (isAllProjectsSelected) return cycles;
    return cycles.filter(c => c.projectId === currentProject?.id);
  }, [cycles, currentProject, isAllProjectsSelected]);

  // Filter pitches by current project's cycles
  const filteredPitches = useMemo(() => {
    if (isAllProjectsSelected) return pitches;
    const projectCycleIds = new Set(filteredCycles.map(c => c.id));
    return pitches.filter(p => projectCycleIds.has(p.cycleId));
  }, [pitches, filteredCycles, isAllProjectsSelected]);

  // Reset cycle and pitch filters when project changes to ensure clean filtering
  useEffect(() => {
    setCycleFilter('all');
    setPitchFilter('all');
  }, [currentProject?.id, isAllProjectsSelected]);

  useEffect(() => {
    loadTestCases();
  }, [statusFilter, typeFilter, priorityFilter, cycleFilter, pitchFilter, currentProject?.id]);

  useEffect(() => {
    loadCyclesAndPitches();
  }, []);

  const loadCyclesAndPitches = async () => {
    try {
      const [cyclesRes, pitchesRes] = await Promise.all([
        cycleService.getMyCycles(),
        pitchService.getMyPitches(),
      ]);
      setCycles(cyclesRes.data);
      setPitches(pitchesRes.data);
    } catch (err) {
      console.error('Failed to load cycles and pitches', err);
    }
  };

  const loadTestCases = async () => {
    setLoading(true);
    setError(null);
    try {
      let response;
      // Use filter endpoint if any filter is active
      if (
        statusFilter !== 'all' ||
        typeFilter !== 'all' ||
        priorityFilter !== 'all' ||
        cycleFilter !== 'all' ||
        pitchFilter !== 'all'
      ) {
        response = await qaTestManagementService.getTestCasesWithFilters(
          cycleFilter !== 'all' ? cycleFilter : undefined,
          pitchFilter !== 'all' ? pitchFilter : undefined,
          statusFilter !== 'all' ? [statusFilter] : undefined,
          typeFilter !== 'all' ? [typeFilter] : undefined,
          priorityFilter !== 'all' ? [priorityFilter] : undefined
        );
      } else {
        response = await qaTestManagementService.getAllTestCases();
      }
      
      let cases = response.data;
      // Filter by current project if one is selected
      if (!isAllProjectsSelected && currentProject) {
        const projectCycleIds = new Set(cycles.filter(c => c.projectId === currentProject.id).map(c => c.id));
        const projectPitchIds = new Set(pitches.filter(p => projectCycleIds.has(p.cycleId)).map(p => p.id));
        cases = cases.filter(tc => tc.pitchId && projectPitchIds.has(tc.pitchId));
      }
      
      setTestCases(cases);
    } catch (err) {
      setError(t('testCases.loadFailed'));
      console.error(err);
    } finally {
      setLoading(false);
      notifyProjectSwitchComplete();
    }
  };

  const filteredTestCases = testCases.filter((tc) => {
    // Only apply search query filter on client-side
    const matchesSearch =
      !searchQuery ||
      tc.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      tc.testCaseKey.toLowerCase().includes(searchQuery.toLowerCase()) ||
      tc.description?.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesSearch;
  });

  if (loading || isSwitchingProject) {
    return <TestCasesSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-4">
        <h1 className="text-3xl font-bold tracking-tight">{t('testCases.title')}</h1>
        <div className="flex flex-col sm:flex-row gap-2">
          <Button
            variant="outline"
            onClick={() => navigate('/qa/test-cases/generate')}
            className="w-full sm:w-auto"
          >
            <Sparkles className="mr-2 h-4 w-4" />
            {t('testCases.generateAI')}
          </Button>
          <Button
            onClick={() => navigate('/qa/test-cases/new')}
            className="w-full sm:w-auto"
          >
            <Plus className="mr-2 h-4 w-4" />
            {t('testCases.newTestCase')}
          </Button>
        </div>
      </div>

      {/* Error Alert */}
      {error && (
        <div className="flex items-center gap-2 p-4 rounded-lg bg-destructive/10 text-destructive border border-destructive/20">
          <AlertCircle className="h-4 w-4" />
          <span>{error}</span>
        </div>
      )}

      {/* Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-3xl font-bold">{testCases.length}</p>
            <p className="text-sm text-muted-foreground">{t('testCases.totalTestCases')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-3xl font-bold text-green-600 dark:text-green-400">
              {testCases.filter((tc) => tc.status === 'APPROVED').length}
            </p>
            <p className="text-sm text-muted-foreground">{t('testCases.approved')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-3xl font-bold text-blue-600 dark:text-blue-400">
              {testCases.filter((tc) => tc.status === 'READY').length}
            </p>
            <p className="text-sm text-muted-foreground">{t('testCases.ready')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-3xl font-bold text-cyan-600 dark:text-cyan-400">
              {testCases.filter((tc) => tc.aiGenerated).length}
            </p>
            <p className="text-sm text-muted-foreground">{t('testCases.aiGenerated')}</p>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-4">
        <div className="relative min-w-[300px]">
          <Label htmlFor="test-cases-search" className="sr-only">{t('testCases.searchLabel')}</Label>
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" aria-hidden="true" />
          <Input
            id="test-cases-search"
            type="search"
            placeholder={t('testCases.searchPlaceholder')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-9"
            aria-label={t('testCases.searchLabel')}
          />
        </div>
        <Select
          value={statusFilter}
          onValueChange={(value) => setStatusFilter(value as TestCaseStatus | 'all')}
        >
          <SelectTrigger className="w-[140px]">
            <SelectValue placeholder={t('testCases.status')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t('testCases.allStatus')}</SelectItem>
            {['DRAFT', 'READY', 'APPROVED', 'DEPRECATED', 'ARCHIVED'].map((status) => (
              <SelectItem key={status} value={status}>
                {t(`testCases.statusValues.${status}`)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={typeFilter}
          onValueChange={(value) => setTypeFilter(value as TestCaseType | 'all')}
        >
          <SelectTrigger className="w-[140px]">
            <SelectValue placeholder={t('testCases.type')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t('testCases.allTypes')}</SelectItem>
            {['FUNCTIONAL', 'INTEGRATION', 'UNIT', 'E2E', 'REGRESSION', 'SMOKE'].map((type) => (
              <SelectItem key={type} value={type}>
                {t(`testCases.typeValues.${type}`)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={priorityFilter}
          onValueChange={(value) => setPriorityFilter(value as TestCasePriority | 'all')}
        >
          <SelectTrigger className="w-[140px]">
            <SelectValue placeholder={t('testCases.priority')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t('testCases.allPriority')}</SelectItem>
            {['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((priority) => (
              <SelectItem key={priority} value={priority}>
                {t(`testCases.priorityValues.${priority}`)}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {/* Hide cycle and pitch filters for Kanban projects - Shape Up concepts */}
        {!isKanbanProject && (
          <>
            <Select
              value={cycleFilter.toString()}
              onValueChange={(value) => setCycleFilter(value === 'all' ? 'all' : parseInt(value))}
            >
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder={t('testCases.cycle')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('testCases.allCycles')}</SelectItem>
                {filteredCycles.map((cycle) => (
                  <SelectItem key={cycle.id} value={cycle.id.toString()}>
                    {cycle.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select
              value={pitchFilter.toString()}
              onValueChange={(value) => setPitchFilter(value === 'all' ? 'all' : parseInt(value))}
            >
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder={t('testCases.pitch')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('testCases.allPitches')}</SelectItem>
                {filteredPitches.map((pitch) => (
                  <SelectItem key={pitch.id} value={pitch.id.toString()}>
                    {pitch.title}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </>
        )}
      </div>

      {/* Test Cases Table */}
      <Card>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>{t('testCases.key')}</TableHead>
              <TableHead>{t('testCases.tableTitle')}</TableHead>
              <TableHead>{t('testCases.type')}</TableHead>
              <TableHead>{t('testCases.priority')}</TableHead>
              <TableHead>{t('testCases.status')}</TableHead>
              <TableHead>{t('testCases.pitch')}</TableHead>
              <TableHead>{t('testCases.passRate')}</TableHead>
              <TableHead className="text-right">{t('testCases.actions')}</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredTestCases.map((tc) => (
              <TableRow key={tc.id}>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <span className="font-medium">{tc.testCaseKey}</span>
                    {tc.aiGenerated && (
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger>
                            <Sparkles className="h-4 w-4 text-primary" />
                          </TooltipTrigger>
                          <TooltipContent>{t('testCases.aiGenerated')}</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                    )}
                  </div>
                </TableCell>
                <TableCell>
                  <span className="max-w-[300px] truncate block">{tc.title}</span>
                </TableCell>
                <TableCell>
                  <Badge variant="outline">{t(`testCases.typeValues.${tc.type}`)}</Badge>
                </TableCell>
                <TableCell>
                  <Badge variant={priorityVariants[tc.priority]}>{t(`testCases.priorityValues.${tc.priority}`)}</Badge>
                </TableCell>
                <TableCell>
                  <Badge className={cn('font-medium', statusVariants[tc.status])}>
                    {t(`testCases.statusValues.${tc.status}`)}
                  </Badge>
                </TableCell>
                <TableCell>
                  <span className="text-muted-foreground">{tc.pitchTitle || '-'}</span>
                </TableCell>
                <TableCell>
                  {tc.totalRuns && tc.totalRuns > 0 ? (
                    <span
                      className={cn(
                        'font-medium',
                        tc.passRate && tc.passRate >= 80
                          ? 'text-green-600 dark:text-green-400'
                          : 'text-red-600 dark:text-red-400'
                      )}
                    >
                      {tc.passRate?.toFixed(0)}% ({tc.passedRuns}/{tc.totalRuns})
                    </span>
                  ) : (
                    <span className="text-muted-foreground">{t('testCases.noRuns')}</span>
                  )}
                </TableCell>
                <TableCell className="text-right">
                  <div className="flex items-center justify-end gap-1">
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => navigate(`/qa/test-cases/${tc.id}`)}
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>{t('testCases.view')}</TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => navigate(`/qa/test-cases/${tc.id}/edit`)}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>{t('testCases.edit')}</TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-primary hover:text-primary"
                            onClick={() => navigate(`/qa/test-cases/${tc.id}/run`)}
                          >
                            <Play className="h-4 w-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>{t('testCases.runTest')}</TooltipContent>
                      </Tooltip>
                    </TooltipProvider>
                    <SoftDeleteButton
                      entityType="testCase"
                      entityId={tc.id}
                      entityTitle={tc.title}
                      onSuccess={() => {
                        setTestCases(testCases.filter((testCase) => testCase.id !== tc.id));
                      }}
                      variant="ghost"
                      size="sm"
                    />
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {filteredTestCases.length === 0 && (
              <TableRow>
                <TableCell colSpan={8} className="text-center py-8">
                  <span className="text-muted-foreground">
                    {searchQuery || statusFilter !== 'all' || typeFilter !== 'all' || priorityFilter !== 'all'
                      ? t('testCases.noMatch')
                      : t('testCases.noTestCases')}
                  </span>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>
    </div>
  );
};

export default TestCasesPage;
