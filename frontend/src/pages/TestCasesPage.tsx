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
  ChevronLeft,
  ChevronRight,
  ChevronsLeft,
  ChevronsRight,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { Label } from '../components/ui/label';
import { Checkbox } from '../components/ui/checkbox';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogDescription,
} from '../components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import { Combobox } from '../components/ui/combobox';
import { cn } from '../lib/utils';
import qaTestManagementService from '../services/qaTestManagementService';
import { SoftDeleteButton } from '../components/SoftDeleteButton';
import { cycleService } from '../services/cycleService';
import { pitchService } from '../services/pitchService';
import { useProject, useToast } from '../contexts';
import { useListLoader } from '../hooks';
import { TestCasesSkeleton } from '../components/Skeletons';
import {
  TestCase,
  TestCaseStatus,
  TestCaseType,
  TestCasePriority,
  TestRunStatus,
  Cycle,
  Pitch,
} from '../types';

const TEST_RUN_STATUSES: TestRunStatus[] = ['PASSED', 'FAILED', 'BLOCKED', 'SKIPPED', 'RUNNING', 'PENDING'];

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

// Build a compact list of 0-indexed page numbers to render, inserting 'gap' markers
// for elided ranges. Always shows the first and last page plus the current ±1.
function buildPageList(current: number, totalPages: number): (number | 'gap')[] {
  if (totalPages <= 7) return Array.from({ length: totalPages }, (_, i) => i);
  const pages: (number | 'gap')[] = [0];
  const start = Math.max(1, current - 1);
  const end = Math.min(totalPages - 2, current + 1);
  if (start > 1) pages.push('gap');
  for (let i = start; i <= end; i++) pages.push(i);
  if (end < totalPages - 2) pages.push('gap');
  pages.push(totalPages - 1);
  return pages;
}

const TestCasesPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { currentProject, isAllProjectsSelected, isKanbanProject, isSwitchingProject, notifyProjectSwitchComplete } = useProject();
  const { showToast } = useToast();
  const { loading, refreshing, startLoad, finishLoad, resetInitial } = useListLoader();
  // Bulk test execution (Zephyr-style): multi-select test cases and record a run for each at once.
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [bulkDialogOpen, setBulkDialogOpen] = useState(false);
  const [bulkStatus, setBulkStatus] = useState<TestRunStatus>('PASSED');
  const [bulkEnvironment, setBulkEnvironment] = useState('');
  const [bulkSubmitting, setBulkSubmitting] = useState(false);
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<TestCaseStatus | 'all'>('all');
  const [typeFilter, setTypeFilter] = useState<TestCaseType | 'all'>('all');
  const [priorityFilter, setPriorityFilter] = useState<TestCasePriority | 'all'>('all');
  const [cycleFilter, setCycleFilter] = useState<number | 'all'>('all');
  const [pitchFilter, setPitchFilter] = useState<number | 'all'>('all');
  const [sourceFilter, setSourceFilter] = useState<'all' | 'manual' | 'ai'>('all');
  const [creatorFilter, setCreatorFilter] = useState<number | 'all'>('all');
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);

  // Distinct creators present in the loaded test cases — drives the "Created by" filter without a
  // separate user-list fetch. The creator filter is applied client-side so these options stay stable.
  const creatorOptions = useMemo(() => {
    const map = new Map<number, string>();
    testCases.forEach((tc) => {
      if (tc.createdById != null) map.set(tc.createdById, tc.createdByName || `#${tc.createdById}`);
    });
    return Array.from(map, ([id, name]) => ({ id, name }));
  }, [testCases]);

  // Filter cycles by current project
  const filteredCycles = useMemo(() => {
    if (isAllProjectsSelected) return cycles;
    return cycles.filter(c => c.projectId === currentProject?.id);
  }, [cycles, currentProject, isAllProjectsSelected]);

  // Filter pitches by current project's cycles
  const filteredPitches = useMemo(() => {
    if (isAllProjectsSelected) return pitches;
    const projectCycleIds = new Set(filteredCycles.map(c => c.id));
    return pitches.filter(p => p.cycleId !== undefined && projectCycleIds.has(p.cycleId));
  }, [pitches, filteredCycles, isAllProjectsSelected]);

  const totalPages = Math.ceil(totalElements / rowsPerPage);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchQuery);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  // Reset cycle and pitch filters when project changes to ensure clean filtering
  useEffect(() => {
    resetInitial();
    setCycleFilter('all');
    setPitchFilter('all');
    setPage(0);
  }, [currentProject?.id, isAllProjectsSelected]);

  useEffect(() => {
    loadTestCases();
  }, [statusFilter, typeFilter, priorityFilter, cycleFilter, pitchFilter, sourceFilter, currentProject?.id, page, rowsPerPage, debouncedSearch]);

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
    startLoad();
    setError(null);
    try {
      const response = await qaTestManagementService.getTestCasesWithFilters(
        cycleFilter !== 'all' ? cycleFilter : undefined,
        pitchFilter !== 'all' ? pitchFilter : undefined,
        statusFilter !== 'all' ? [statusFilter] : undefined,
        typeFilter !== 'all' ? [typeFilter] : undefined,
        priorityFilter !== 'all' ? [priorityFilter] : undefined,
        page,
        rowsPerPage,
        'createdAt',
        'desc',
        undefined,
        sourceFilter === 'ai' ? true : sourceFilter === 'manual' ? false : undefined,
        isAllProjectsSelected ? undefined : currentProject?.id
      );

      // Handle both paginated (Page<TestCase>) and plain array responses
      const data = response.data;
      if (data && typeof data === 'object' && 'content' in data) {
        const pageData = data as { content: TestCase[]; totalElements: number };
        setTestCases(pageData.content);
        setTotalElements(pageData.totalElements);
      } else {
        // Fallback: plain array (shouldn't happen with updated service, but be safe)
        const arr = data as unknown as TestCase[];
        setTestCases(arr);
        setTotalElements(arr.length);
      }
    } catch (err) {
      setError(t('testCases.loadFailed'));
      console.error(err);
    } finally {
      finishLoad();
      notifyProjectSwitchComplete();
    }
  };

  // Client-side search + creator filtering on the loaded set
  const filteredTestCases = testCases.filter((tc) => {
    if (creatorFilter !== 'all' && tc.createdById !== creatorFilter) return false;
    if (!debouncedSearch) return true;
    const q = debouncedSearch.toLowerCase();
    return (
      tc.title.toLowerCase().includes(q) ||
      tc.testCaseKey.toLowerCase().includes(q) ||
      (tc.description?.toLowerCase().includes(q) ?? false)
    );
  });

  const toggleSelected = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const allVisibleSelected = filteredTestCases.length > 0 && filteredTestCases.every((tc) => selectedIds.has(tc.id));
  const toggleSelectAllVisible = () => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (allVisibleSelected) {
        filteredTestCases.forEach((tc) => next.delete(tc.id));
      } else {
        filteredTestCases.forEach((tc) => next.add(tc.id));
      }
      return next;
    });
  };

  const handleBulkRecord = async () => {
    setBulkSubmitting(true);
    try {
      await qaTestManagementService.recordTestRunsBulk({
        testCaseIds: Array.from(selectedIds),
        status: bulkStatus,
        environment: bulkEnvironment.trim() || undefined,
      });
      showToast(t('testCases.bulkRun.success', { count: selectedIds.size }), 'success');
      setBulkDialogOpen(false);
      setSelectedIds(new Set());
      setBulkEnvironment('');
      loadTestCases();
    } catch (err) {
      console.error(err);
      showToast(t('testCases.bulkRun.failed', 'Failed to record test runs'), 'error');
    } finally {
      setBulkSubmitting(false);
    }
  };

  // Stats derived from current page
  const approvedCount = testCases.filter((tc) => tc.status === 'APPROVED').length;
  const readyCount = testCases.filter((tc) => tc.status === 'READY').length;
  const aiGeneratedCount = testCases.filter((tc) => tc.aiGenerated).length;

  if (loading || isSwitchingProject) {
    return <TestCasesSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Thin refresh progress bar */}
      <div className={`fixed top-0 left-0 right-0 z-50 h-0.5 overflow-hidden transition-opacity duration-300 ${refreshing ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}>
        <div className={`h-full w-full bg-primary ${refreshing ? 'animate-page-progress' : ''}`} />
      </div>

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
            <p className="text-3xl font-bold">{totalElements}</p>
            <p className="text-sm text-muted-foreground">{t('testCases.totalTestCases')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-3xl font-bold text-green-600 dark:text-green-400">
              {approvedCount}
            </p>
            <p className="text-sm text-muted-foreground">{t('testCases.approved')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-3xl font-bold text-blue-600 dark:text-blue-400">
              {readyCount}
            </p>
            <p className="text-sm text-muted-foreground">{t('testCases.ready')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-3xl font-bold text-cyan-600 dark:text-cyan-400">
              {aiGeneratedCount}
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

        {/* Status Combobox */}
        <div className="min-w-[140px]">
          <Combobox
            options={[
              { value: 'all', label: t('testCases.allStatus') },
              ...(['DRAFT', 'READY', 'APPROVED', 'DEPRECATED', 'ARCHIVED'] as TestCaseStatus[]).map((s) => ({
                value: s,
                label: t(`testCases.statusValues.${s}`),
              })),
            ]}
            value={statusFilter}
            onValueChange={(v) => { setStatusFilter(v as TestCaseStatus | 'all'); setPage(0); }}
            placeholder={t('testCases.status')}
            searchPlaceholder={t('testCases.searchStatus')}
          />
        </div>

        {/* Type Combobox */}
        <div className="min-w-[140px]">
          <Combobox
            options={[
              { value: 'all', label: t('testCases.allTypes') },
              ...(['FUNCTIONAL', 'INTEGRATION', 'UNIT', 'E2E', 'REGRESSION', 'SMOKE'] as TestCaseType[]).map((tp) => ({
                value: tp,
                label: t(`testCases.typeValues.${tp}`),
              })),
            ]}
            value={typeFilter}
            onValueChange={(v) => { setTypeFilter(v as TestCaseType | 'all'); setPage(0); }}
            placeholder={t('testCases.type')}
            searchPlaceholder={t('testCases.searchType')}
          />
        </div>

        {/* Priority Combobox */}
        <div className="min-w-[140px]">
          <Combobox
            options={[
              { value: 'all', label: t('testCases.allPriority') },
              ...(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as TestCasePriority[]).map((p) => ({
                value: p,
                label: t(`testCases.priorityValues.${p}`),
              })),
            ]}
            value={priorityFilter}
            onValueChange={(v) => { setPriorityFilter(v as TestCasePriority | 'all'); setPage(0); }}
            placeholder={t('testCases.priority')}
            searchPlaceholder={t('testCases.searchPriority')}
          />
        </div>

        {/* Source Combobox — manual vs AI-generated */}
        <div className="min-w-[150px]">
          <Combobox
            options={[
              { value: 'all', label: t('testCases.allSources', 'All sources') },
              { value: 'manual', label: t('testCases.sourceManual', 'Manual') },
              { value: 'ai', label: t('testCases.sourceAi', 'AI-generated') },
            ]}
            value={sourceFilter}
            onValueChange={(v) => { setSourceFilter(v as 'all' | 'manual' | 'ai'); setPage(0); }}
            placeholder={t('testCases.source', 'Source')}
            searchPlaceholder={t('testCases.source', 'Source')}
          />
        </div>

        {/* Created-by Combobox */}
        <div className="min-w-[160px]">
          <Combobox
            options={[
              { value: 'all', label: t('testCases.allCreators', 'All creators') },
              ...creatorOptions.map((c) => ({ value: c.id.toString(), label: c.name })),
            ]}
            value={creatorFilter.toString()}
            onValueChange={(v) => { setCreatorFilter(v === 'all' ? 'all' : parseInt(v)); setPage(0); }}
            placeholder={t('testCases.createdBy', 'Created by')}
            searchPlaceholder={t('testCases.createdBy', 'Created by')}
          />
        </div>

        {/* Hide cycle and pitch filters for Kanban projects - Shape Up concepts */}
        {!isKanbanProject && (
          <>
            {/* Cycle Combobox */}
            <div className="min-w-[180px]">
              <Combobox
                options={[
                  { value: 'all', label: t('testCases.allCycles') },
                  ...filteredCycles.map((cycle) => ({
                    value: cycle.id.toString(),
                    label: cycle.name,
                  })),
                ]}
                value={cycleFilter.toString()}
                onValueChange={(v) => { setCycleFilter(v === 'all' ? 'all' : parseInt(v)); setPage(0); }}
                placeholder={t('testCases.cycle')}
                searchPlaceholder={t('testCases.searchCycle')}
              />
            </div>

            {/* Pitch Combobox */}
            <div className="min-w-[180px]">
              <Combobox
                options={[
                  { value: 'all', label: t('testCases.allPitches') },
                  ...filteredPitches.map((pitch) => ({
                    value: pitch.id.toString(),
                    label: pitch.title,
                  })),
                ]}
                value={pitchFilter.toString()}
                onValueChange={(v) => { setPitchFilter(v === 'all' ? 'all' : parseInt(v)); setPage(0); }}
                placeholder={t('testCases.pitch')}
                searchPlaceholder={t('testCases.searchPitch')}
              />
            </div>
          </>
        )}
      </div>

      {/* Bulk action bar — appears when test cases are selected */}
      {selectedIds.size > 0 && (
        <div className="flex items-center justify-between gap-3 rounded-md border bg-accent/40 px-4 py-2">
          <span className="text-sm font-medium">
            {t('testCases.bulkRun.selectedCount', { count: selectedIds.size })}
          </span>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" onClick={() => setSelectedIds(new Set())}>
              {t('testCases.bulkRun.clear', 'Clear')}
            </Button>
            <Button size="sm" onClick={() => setBulkDialogOpen(true)}>
              <Play className="h-4 w-4 mr-2" />
              {t('testCases.bulkRun.recordRuns', 'Record runs')}
            </Button>
          </div>
        </div>
      )}

      {/* Test Cases Table */}
      <Card className={`transition-opacity duration-200 ${refreshing ? 'opacity-60' : 'opacity-100'}`}>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[40px]">
                <Checkbox
                  checked={allVisibleSelected}
                  onCheckedChange={toggleSelectAllVisible}
                  aria-label={t('testCases.bulkRun.selectAll', 'Select all')}
                />
              </TableHead>
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
              <TableRow key={tc.id} data-state={selectedIds.has(tc.id) ? 'selected' : undefined}>
                <TableCell>
                  <Checkbox
                    checked={selectedIds.has(tc.id)}
                    onCheckedChange={() => toggleSelected(tc.id)}
                    aria-label={t('testCases.bulkRun.selectOne', 'Select test case')}
                  />
                </TableCell>
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
                        setTestCases(prev => prev.filter((testCase) => testCase.id !== tc.id));
                        setTotalElements(prev => Math.max(0, prev - 1));
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
                <TableCell colSpan={9} className="text-center py-8">
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

        {/* Pagination */}
        <div className="flex items-center justify-between px-4 py-3 border-t">
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">{t('testCases.pagination.rowsPerPage')}</span>
            <Combobox
              options={[
                { value: '5', label: '5' },
                { value: '10', label: '10' },
                { value: '25', label: '25' },
                { value: '50', label: '50' },
              ]}
              value={rowsPerPage.toString()}
              onValueChange={(v) => { setRowsPerPage(parseInt(v, 10)); setPage(0); }}
              triggerClassName="w-[70px] h-8"
            />
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm text-muted-foreground">
              {totalElements === 0
                ? t('testCases.pagination.empty')
                : t('testCases.pagination.rangeText', {
                    start: page * rowsPerPage + 1,
                    end: Math.min((page + 1) * rowsPerPage, totalElements),
                    total: totalElements,
                  })}
            </span>
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                onClick={() => setPage(0)}
                disabled={page === 0}
                aria-label={t('testCases.pagination.first')}
                title={t('testCases.pagination.first')}
              >
                <ChevronsLeft className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                aria-label={t('testCases.pagination.previous')}
                title={t('testCases.pagination.previous')}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              {totalPages > 1 && buildPageList(page, totalPages).map((item, idx) =>
                item === 'gap' ? (
                  <span key={`gap-${idx}`} className="px-1 text-sm text-muted-foreground select-none">…</span>
                ) : (
                  <Button
                    key={item}
                    variant={item === page ? 'default' : 'outline'}
                    size="icon"
                    className="h-8 w-8 text-xs"
                    onClick={() => setPage(item)}
                    aria-label={t('testCases.pagination.goToPage', { page: item + 1 })}
                    aria-current={item === page ? 'page' : undefined}
                  >
                    {item + 1}
                  </Button>
                )
              )}
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                aria-label={t('testCases.pagination.next')}
                title={t('testCases.pagination.next')}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                onClick={() => setPage(totalPages - 1)}
                disabled={page >= totalPages - 1}
                aria-label={t('testCases.pagination.last')}
                title={t('testCases.pagination.last')}
              >
                <ChevronsRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </Card>

      {/* Bulk record-runs dialog */}
      <Dialog open={bulkDialogOpen} onOpenChange={setBulkDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('testCases.bulkRun.title', 'Record test runs')}</DialogTitle>
            <DialogDescription>
              {t('testCases.bulkRun.description', { count: selectedIds.size })}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-1">
              <Label>{t('testCases.bulkRun.status', 'Result')}</Label>
              <Combobox
                options={TEST_RUN_STATUSES.map((s) => ({ value: s, label: t(`testRuns.statusValues.${s}`, s) }))}
                value={bulkStatus}
                onValueChange={(v) => setBulkStatus(v as TestRunStatus)}
                placeholder={t('testCases.bulkRun.status', 'Result')}
                searchPlaceholder={t('testCases.bulkRun.status', 'Result')}
              />
            </div>
            <div className="space-y-1">
              <Label>{t('testCases.bulkRun.environment', 'Environment (optional)')}</Label>
              <Input
                value={bulkEnvironment}
                onChange={(e) => setBulkEnvironment(e.target.value)}
                placeholder={t('testCases.bulkRun.environmentPlaceholder', 'e.g. Chrome / staging')}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setBulkDialogOpen(false)} disabled={bulkSubmitting}>
              {t('common.cancel', 'Cancel')}
            </Button>
            <Button onClick={handleBulkRecord} disabled={bulkSubmitting || selectedIds.size === 0}>
              {bulkSubmitting
                ? t('testCases.bulkRun.recording', 'Recording…')
                : t('testCases.bulkRun.recordRuns', 'Record runs')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default TestCasesPage;
