import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { detectTextDirection } from '../utils/rtlDetection';
import {
  Bug,
  Plus,
  Search,
  Pencil,
  Trash2,
  Eye,
  ArrowUp,
  ArrowDown,
  Loader2,
  X,
  ChevronLeft,
  ChevronRight,
  MessageSquare,
  LayoutList,
  Kanban,
  Check,
  Info,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { Combobox } from '../components/ui/combobox';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { Checkbox } from '../components/ui/checkbox';
import { Switch } from '../components/ui/switch';
import { Label } from '../components/ui/label';
import { Alert, AlertDescription } from '../components/ui/alert';
import { MobileCardView, ResponsiveTable } from '../components/ui/mobile-card-view';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import { ConfirmDialog } from '../components/ui/confirm-dialog';
import qaTestManagementService from '../services/qaTestManagementService';
import { cycleService } from '../services/cycleService';
import { pitchService } from '../services/pitchService';
import { releaseService } from '../services/releaseService';
import { personService } from '../services/personService';
import { useProject } from '../contexts';
import { BugReport, BugStatus, BugSeverity, Cycle, Pitch, Release, Person, getPageTotal } from '../types';
import BugReportModal from '../components/BugReportModal';
import BugKanbanBoard from '../components/BugKanbanBoard';
import { BugViewDialog } from '../components/BugViewDialog';
import { BugReportsSkeleton } from '../components/Skeletons';

const severityBadgeVariants: Record<BugSeverity, 'default' | 'secondary' | 'info' | 'warning' | 'destructive'> = {
  TRIVIAL: 'secondary',
  MINOR: 'info',
  MAJOR: 'warning',
  CRITICAL: 'destructive',
  BLOCKER: 'destructive',
};

const statusBadgeVariants: Record<BugStatus, 'default' | 'secondary' | 'info' | 'warning' | 'destructive' | 'success'> = {
  OPEN: 'destructive',
  IN_PROGRESS: 'default',
  RESOLVED: 'success',
  VERIFIED: 'success',
  CLOSED: 'secondary',
  REOPENED: 'warning',
  WONT_FIX: 'secondary',
  DUPLICATE: 'secondary',
};

const BUG_FILTER_KEY = 'shipflow.bugFilters';

function readSavedBugFilter<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(BUG_FILTER_KEY);
    const parsed = raw ? JSON.parse(raw) : null;
    return parsed?.[key] ?? fallback;
  } catch {
    return fallback;
  }
}

const BugReportsPage: React.FC = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { currentProject, isAllProjectsSelected, isKanbanProject, isSwitchingProject, notifyProjectSwitchComplete } = useProject();
  const [bugReports, setBugReports] = useState<BugReport[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState<string>(() => readSavedBugFilter('searchQuery', ''));
  const [debouncedSearch, setDebouncedSearch] = useState<string>(() => readSavedBugFilter('searchQuery', ''));
  const [statusFilter, setStatusFilter] = useState<BugStatus[]>(() => readSavedBugFilter('statusFilter', []));
  const [severityFilter, setSeverityFilter] = useState<BugSeverity[]>(() => readSavedBugFilter('severityFilter', []));
  const [assigneeFilter, setAssigneeFilter] = useState<number | undefined>(() => readSavedBugFilter('assigneeFilter', undefined));
  const [cycleFilter, setCycleFilter] = useState<number | undefined>(undefined);
  const [pitchFilter, setPitchFilter] = useState<number | undefined>(undefined);
  const [releaseFilter, setReleaseFilter] = useState<number | undefined>(undefined);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [releases, setReleases] = useState<Release[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [excludeMode, setExcludeMode] = useState<boolean>(() => readSavedBugFilter('excludeMode', false));
  const [sortBy, setSortBy] = useState<'createdAt' | 'severity' | 'status' | 'title'>(() => readSavedBugFilter('sortBy', 'createdAt'));
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>(() => readSavedBugFilter('sortOrder', 'desc'));
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState<number>(() => readSavedBugFilter('rowsPerPage', 10));
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedBug, setSelectedBug] = useState<BugReport | null>(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const [severityDropdownOpen, setSeverityDropdownOpen] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'kanban'>(() => readSavedBugFilter('viewMode', 'list'));
  const [updatingBugId, setUpdatingBugId] = useState<number | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [bugToDelete, setBugToDelete] = useState<number | null>(null);

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

  // Reset cycle and pitch filters when project changes to ensure clean filtering
  useEffect(() => {
    setCycleFilter(undefined);
    setPitchFilter(undefined);
    setReleaseFilter(undefined);
    setPage(0); // Reset to first page when project changes
  }, [currentProject?.id, isAllProjectsSelected]);

  // Persist user-level filters across page navigations
  useEffect(() => {
    try {
      localStorage.setItem(BUG_FILTER_KEY, JSON.stringify({
        searchQuery,
        statusFilter,
        severityFilter,
        assigneeFilter,
        excludeMode,
        sortBy,
        sortOrder,
        rowsPerPage,
        viewMode,
      }));
    } catch { /* quota exceeded — ignore */ }
  }, [searchQuery, statusFilter, severityFilter, assigneeFilter, excludeMode, sortBy, sortOrder, rowsPerPage, viewMode]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(searchQuery);
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [searchQuery]);

  useEffect(() => {
    loadBugReports();
  }, [page, rowsPerPage, sortBy, sortOrder, statusFilter, severityFilter, assigneeFilter, excludeMode, cycleFilter, pitchFilter, releaseFilter, currentProject?.id, isAllProjectsSelected, debouncedSearch]);

  useEffect(() => {
    loadCyclesAndPitches();
  }, [currentProject?.id]);

  const loadCyclesAndPitches = async () => {
    try {
      const [cyclesRes, pitchesRes, personsData] = await Promise.all([
        cycleService.getMyCycles(),
        pitchService.getMyPitches(),
        personService.getAll(true),
      ]);
      setCycles(cyclesRes.data);
      setPitches(pitchesRes.data);
      setPersons(personsData);
      // Load releases for current project
      if (currentProject?.id) {
        try {
          const releasesRes = await releaseService.getByProject(currentProject.id);
          setReleases(releasesRes.data);
        } catch {
          setReleases([]);
        }
      }
    } catch (err) {
      console.error('Failed to load cycles and pitches', err);
    }
  };

  const loadBugReports = async () => {
    setLoading(true);
    setError(null);
    try {
      let response;
      // Always use the filter endpoint for consistent server-side filtering
      const projectId = isAllProjectsSelected ? undefined : currentProject?.id;
      
      // When release filter is active, fetch a large page so client-side
      // filtering covers all records (no reliable backend support yet).
      const effectivePage = releaseFilter !== undefined ? 0 : page;
      const effectiveSize = releaseFilter !== undefined ? 1000 : rowsPerPage;

      response = await qaTestManagementService.getBugReportsWithFilters(
        projectId,
        cycleFilter,
        pitchFilter,
        statusFilter.length > 0 ? statusFilter : undefined,
        severityFilter.length > 0 ? severityFilter : undefined,
        assigneeFilter !== undefined ? [assigneeFilter] : undefined,
        excludeMode,
        effectivePage,
        effectiveSize,
        sortBy,
        sortOrder,
        debouncedSearch || undefined
      );
      
      let bugData = response.data.content;
      
      // Client-side filter by release (not yet supported by backend filter endpoint)
      if (releaseFilter !== undefined) {
        bugData = bugData.filter(bug => bug.targetReleaseId === releaseFilter);
      }
      
      setBugReports(bugData);
      setTotalElements(releaseFilter !== undefined ? bugData.length : getPageTotal(response.data));
    } catch (err) {
      setError(t('bugReports.loadFailed'));
      console.error(err);
    } finally {
      setLoading(false);
      notifyProjectSwitchComplete();
    }
  };

  const openDeleteConfirm = (id: number) => {
    setBugToDelete(id);
    setDeleteConfirmOpen(true);
  };

  const handleDelete = async () => {
    if (bugToDelete === null) return;
    try {
      await qaTestManagementService.deleteBugReport(bugToDelete);
      setBugReports(bugReports.filter((b) => b.id !== bugToDelete));
      setDeleteConfirmOpen(false);
      setBugToDelete(null);
    } catch (err) {
      setError(t('bugReports.deleteFailed'));
    }
  };

  const handleCreateOrUpdate = async (data: any) => {
    try {
      if (selectedBug) {
        const response = await qaTestManagementService.updateBugReport(selectedBug.id, data);
        setBugReports(bugReports.map((b) => (b.id === selectedBug.id ? response.data : b)));
      } else {
        const response = await qaTestManagementService.createBugReport(data);
        setBugReports([response.data, ...bugReports]);
        setModalOpen(false);
        setSelectedBug(null);
        return response.data; // return so BugReportModal can upload pending attachments
      }
      setModalOpen(false);
      setSelectedBug(null);
    } catch (err) {
      setError(t('bugReports.saveFailed'));
      throw err; // Re-throw so the modal knows there was an error
    }
  };

  const openEditModal = (bug: BugReport) => {
    setSelectedBug(bug);
    setModalOpen(true);
  };

  const openCreateModal = () => {
    setSelectedBug(null);
    setModalOpen(true);
  };

  const handleModalClose = () => {
    setModalOpen(false);
    setSelectedBug(null);
  };

  const openDetailModal = (bug: BugReport) => {
    setSelectedBug(bug);
    setDetailModalOpen(true);
  };

  const handleSort = (field: 'createdAt' | 'severity' | 'status' | 'title') => {
    if (sortBy === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(field);
      setSortOrder('desc');
    }
    setPage(0);
  };

  const handleChangePage = (newPage: number) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (value: string) => {
    setRowsPerPage(parseInt(value, 10));
    setPage(0);
  };

  const toggleStatusFilter = (status: BugStatus) => {
    setStatusFilter((prev) =>
      prev.includes(status) ? prev.filter((s) => s !== status) : [...prev, status]
    );
  };

  const toggleSeverityFilter = (severity: BugSeverity) => {
    setSeverityFilter((prev) =>
      prev.includes(severity) ? prev.filter((s) => s !== severity) : [...prev, severity]
    );
  };

  // Inline update for status/severity
  const handleInlineUpdate = async (bugId: number, field: 'status' | 'severity', value: string) => {
    setUpdatingBugId(bugId);
    try {
      const bug = bugReports.find(b => b.id === bugId);
      if (!bug) return;
      
      const updateData = {
        title: bug.title,
        description: bug.description,
        severity: field === 'severity' ? value as BugSeverity : bug.severity,
        status: field === 'status' ? value as BugStatus : bug.status,
      };
      
      const response = await qaTestManagementService.updateBugReport(bugId, updateData);
      setBugReports(bugReports.map(b => b.id === bugId ? response.data : b));
    } catch (err) {
      setError(t('bugReports.saveFailed'));
      console.error('Failed to update bug:', err);
    } finally {
      setUpdatingBugId(null);
    }
  };

  const getStatCounts = () => ({
    total: totalElements,
    open: bugReports.filter((b) => b.status === 'OPEN').length,
    inProgress: bugReports.filter((b) => b.status === 'IN_PROGRESS').length,
    resolved: bugReports.filter((b) => ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(b.status)).length,
    critical: bugReports.filter((b) => ['CRITICAL', 'BLOCKER'].includes(b.severity)).length,
  });

  const stats = getStatCounts();
  const totalPages = Math.ceil(totalElements / rowsPerPage);

  if (loading || isSwitchingProject) {
    return <BugReportsSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h1 className="text-3xl font-bold tracking-tight">{t('bugReports.title')}</h1>
        
        <div className="flex items-center gap-3">
          {/* View Mode Toggle */}
          <div className="flex items-center gap-1 border rounded-md p-1">
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant={viewMode === 'list' ? 'secondary' : 'ghost'}
                    size="icon"
                    className="h-8 w-8"
                    onClick={() => setViewMode('list')}
                  >
                    <LayoutList className="h-4 w-4" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('bugReports.viewMode.list')}</TooltipContent>
              </Tooltip>
            </TooltipProvider>
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant={viewMode === 'kanban' ? 'secondary' : 'ghost'}
                    size="icon"
                    className="h-8 w-8"
                    onClick={() => setViewMode('kanban')}
                  >
                    <Kanban className="h-4 w-4" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('bugReports.viewMode.kanban')}</TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </div>

          {/* Add New Bug Button */}
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <span>
                  <Button 
                    onClick={openCreateModal} 
                    disabled={isAllProjectsSelected}
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    {t('bugReports.reportBug')}
                  </Button>
                </span>
              </TooltipTrigger>
              {isAllProjectsSelected && (
                <TooltipContent>
                  {t('bugReports.selectProjectToCreate')}
                </TooltipContent>
              )}
            </Tooltip>
          </TooltipProvider>
        </div>
      </div>

      {/* All Projects Info Alert */}
      {isAllProjectsSelected && (
        <Alert className="bg-blue-50 dark:bg-blue-950 border-blue-200 dark:border-blue-800">
          <Info className="h-4 w-4 text-blue-500" />
          <AlertDescription className="text-blue-700 dark:text-blue-300">
            {t('bugReports.allProjectsInfoMessage')}
          </AlertDescription>
        </Alert>
      )}

      {/* Error Alert */}
      {error && (
        <Alert variant="destructive">
          <AlertDescription className="flex items-center justify-between">
            {error}
            <Button variant="ghost" size="sm" onClick={() => setError(null)}>
              <X className="h-4 w-4" />
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {/* Stats Cards */}
      <div className="grid grid-cols-5 gap-3">
        <Card>
          <CardContent className="text-center py-2 px-3">
            <p className="text-xl font-bold">{stats.total}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.total')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-2 px-3">
            <p className="text-xl font-bold text-destructive">{stats.open}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.open')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-2 px-3">
            <p className="text-xl font-bold text-primary">{stats.inProgress}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.inProgress')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-2 px-3">
            <p className="text-xl font-bold text-success">{stats.resolved}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.resolved')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-2 px-3">
            <p className="text-xl font-bold text-destructive">{stats.critical}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.criticalBlocker')}</p>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <div className="space-y-2">
        {/* Row 1: Search + Sort */}
        <div className="flex flex-wrap gap-3 items-end">
          <div className="relative flex-1 min-w-[200px]">
            <Label htmlFor="bugs-search" className="sr-only">{t('bugReports.filters.searchLabel')}</Label>
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" aria-hidden="true" />
            <Input
              id="bugs-search"
              type="search"
              placeholder={t('bugReports.filters.searchPlaceholder')}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
              aria-label={t('bugReports.filters.searchAriaLabel')}
            />
          </div>
          <div className="flex items-end gap-2">
            <div className="min-w-[140px]">
              <Label className="text-xs mb-1 block">{t('bugReports.filters.sortBy')}</Label>
              <Combobox
                options={[
                  { value: 'createdAt', label: t('bugReports.sort.createdDate') },
                  { value: 'severity', label: t('bugReports.sort.severity') },
                  { value: 'status', label: t('bugReports.sort.status') },
                  { value: 'title', label: t('bugReports.sort.title') },
                ]}
                value={sortBy}
                onValueChange={(v) => setSortBy(v as typeof sortBy)}
                placeholder={t('bugReports.filters.sortBy')}
              />
            </div>
            <Button
              variant="outline"
              size="icon"
              onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
              aria-label={sortOrder === 'asc' ? 'Sort descending' : 'Sort ascending'}
            >
              {sortOrder === 'asc' ? <ArrowUp className="h-4 w-4" /> : <ArrowDown className="h-4 w-4" />}
            </Button>
          </div>
        </div>

        {/* Row 2: Filter controls */}
        <div className="flex flex-wrap gap-2 items-center">
          {/* Status multi-select */}
          <div className="relative">
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              onClick={() => setStatusDropdownOpen(!statusDropdownOpen)}
            >
              {t('bugReports.filters.status')}
              {statusFilter.length > 0 && (
                <Badge variant="secondary" className="ml-0.5 px-1.5 py-0 text-xs">{statusFilter.length}</Badge>
              )}
            </Button>
            {statusDropdownOpen && (
              <div className="absolute z-50 mt-1 min-w-[180px] bg-popover border rounded-md shadow-md p-2 space-y-1">
                {(Object.keys(statusBadgeVariants) as BugStatus[]).map((status) => (
                  <div
                    key={status}
                    className="flex items-center gap-2 px-2 py-1.5 hover:bg-accent rounded cursor-pointer"
                    onClick={() => toggleStatusFilter(status)}
                  >
                    <Checkbox checked={statusFilter.includes(status)} />
                    <span className="text-sm">{status.replace(/_/g, ' ')}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Severity multi-select */}
          <div className="relative">
            <Button
              variant="outline"
              size="sm"
              className="gap-1.5"
              onClick={() => setSeverityDropdownOpen(!severityDropdownOpen)}
            >
              {t('bugReports.filters.severity')}
              {severityFilter.length > 0 && (
                <Badge variant="secondary" className="ml-0.5 px-1.5 py-0 text-xs">{severityFilter.length}</Badge>
              )}
            </Button>
            {severityDropdownOpen && (
              <div className="absolute z-50 mt-1 min-w-[160px] bg-popover border rounded-md shadow-md p-2 space-y-1">
                {(Object.keys(severityBadgeVariants) as BugSeverity[]).map((severity) => (
                  <div
                    key={severity}
                    className="flex items-center gap-2 px-2 py-1.5 hover:bg-accent rounded cursor-pointer"
                    onClick={() => toggleSeverityFilter(severity)}
                  >
                    <Checkbox checked={severityFilter.includes(severity)} />
                    <span className="text-sm">{severity}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Assignee filter */}
          {persons.length > 0 && (
            <div className="min-w-[160px]">
              <Combobox
                options={[
                  { value: 'all', label: t('bugReports.filters.allAssignees', 'All Assignees') },
                  ...persons.map(p => ({ value: p.id.toString(), label: p.name })),
                ]}
                value={assigneeFilter?.toString() ?? 'all'}
                onValueChange={(v) => setAssigneeFilter(v === 'all' ? undefined : parseInt(v))}
                placeholder={t('bugReports.filters.allAssignees', 'All Assignees')}
                searchPlaceholder={t('bugReports.filters.searchAssignee', 'Search people...')}
              />
            </div>
          )}

          {/* Cycle + Pitch (Shape Up only) */}
          {!isKanbanProject && (
            <>
              <div className="min-w-[160px]">
                <Combobox
                  options={[
                    { value: 'all', label: t('bugReports.filters.allCycles') },
                    ...filteredCycles.map(cycle => ({ value: cycle.id.toString(), label: cycle.name })),
                  ]}
                  value={cycleFilter?.toString() ?? 'all'}
                  onValueChange={(value) => setCycleFilter(value === 'all' ? undefined : parseInt(value))}
                  placeholder={t('bugReports.filters.allCycles')}
                  searchPlaceholder="Search cycles..."
                />
              </div>
              <div className="min-w-[160px]">
                <Combobox
                  options={[
                    { value: 'all', label: t('bugReports.filters.allPitches') },
                    ...filteredPitches.map(pitch => ({ value: pitch.id.toString(), label: pitch.title })),
                  ]}
                  value={pitchFilter?.toString() ?? 'all'}
                  onValueChange={(value) => setPitchFilter(value === 'all' ? undefined : parseInt(value))}
                  placeholder={t('bugReports.filters.allPitches')}
                  searchPlaceholder="Search pitches..."
                />
              </div>
            </>
          )}

          {/* Release */}
          {releases.length > 0 && (
            <div className="min-w-[160px]">
              <Combobox
                options={[
                  { value: 'all', label: t('bugReports.filters.allReleases', 'All Releases') },
                  ...releases.map(r => ({ value: r.id.toString(), label: `${r.name} (${r.version})` })),
                ]}
                value={releaseFilter?.toString() ?? 'all'}
                onValueChange={(value) => setReleaseFilter(value === 'all' ? undefined : parseInt(value))}
                placeholder={t('bugReports.filters.allReleases', 'All Releases')}
                searchPlaceholder="Search releases..."
              />
            </div>
          )}

          {/* Exclude + Clear — pushed to the end */}
          <div className="ml-auto flex items-center gap-3">
            <div className="flex items-center gap-1.5">
              <Switch
                id="exclude-mode"
                checked={excludeMode}
                onCheckedChange={setExcludeMode}
              />
              <Label htmlFor="exclude-mode" className="text-sm cursor-pointer">
                {t('bugReports.filters.exclude')}
              </Label>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setStatusFilter([]);
                setSeverityFilter([]);
                setAssigneeFilter(undefined);
                setCycleFilter(undefined);
                setPitchFilter(undefined);
                setReleaseFilter(undefined);
                setExcludeMode(false);
              }}
            >
              {t('bugReports.filters.clear')}
            </Button>
          </div>
        </div>
      </div>

      {/* Bug Reports - List View */}
      {viewMode === 'list' && (
        <Card>
          <ResponsiveTable
            mobileContent={
              <MobileCardView
                className="p-3"
                items={bugReports.map((bug) => ({
                  key: bug.id,
                  title: (
                    <div className="flex items-center gap-2">
                      <Bug className="h-4 w-4 text-destructive flex-shrink-0" />
                      <span className="font-medium text-xs text-muted-foreground">{bug.bugKey}</span>
                      <span className="truncate">{bug.title}</span>
                    </div>
                  ),
                  subtitle: bug.pitchTitle ? `Pitch: ${bug.pitchTitle}` : undefined,
                  fields: [
                    {
                      label: t('bugReports.table.severity'),
                      value: <Badge variant={severityBadgeVariants[bug.severity]}>{bug.severity}</Badge>,
                    },
                    {
                      label: t('bugReports.table.status'),
                      value: <Badge variant={statusBadgeVariants[bug.status]}>{bug.status.replace('_', ' ')}</Badge>,
                    },
                    {
                      label: t('bugReports.table.assignee'),
                      value: bug.assigneeName || t('bugReports.unassigned'),
                    },
                    {
                      label: t('bugReports.table.created'),
                      value: formatLocalizedDate(new Date(bug.createdAt), i18n.language),
                    },
                  ],
                  actions: (
                    <>
                      <Button variant="ghost" size="icon-sm" title={t('common.openFullPage', 'Open full page')} onClick={() => navigate(`/qa/bug-reports/${bug.id}`)}>
                        <Eye className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon-sm" onClick={() => openEditModal(bug)}>
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button variant="ghost" size="icon-sm" className="text-destructive" onClick={() => openDeleteConfirm(bug.id)}>
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </>
                  ),
                  onClick: () => openDetailModal(bug),
                }))}
                emptyState={
                  <div className="text-center py-8 text-muted-foreground">
                    {searchQuery || statusFilter.length > 0 || severityFilter.length > 0
                      ? t('bugReports.emptyState.noMatches')
                      : t('bugReports.emptyState.noBugs')}
                  </div>
                }
              />
            }
          >
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('bugReports.table.key')}</TableHead>
                  <TableHead
                    className="cursor-pointer select-none"
                    onClick={() => handleSort('title')}
                  >
                    <div className="flex items-center gap-1">
                      {t('bugReports.table.title')}
                    {sortBy === 'title' && (
                      sortOrder === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                    )}
                  </div>
                </TableHead>
                <TableHead
                  className="cursor-pointer select-none"
                  onClick={() => handleSort('severity')}
                >
                  <div className="flex items-center gap-1">
                    {t('bugReports.table.severity')}
                    {sortBy === 'severity' && (
                      sortOrder === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                    )}
                  </div>
                </TableHead>
                <TableHead
                  className="cursor-pointer select-none"
                  onClick={() => handleSort('status')}
                >
                  <div className="flex items-center gap-1">
                    {t('bugReports.table.status')}
                    {sortBy === 'status' && (
                      sortOrder === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                    )}
                  </div>
                </TableHead>
                <TableHead>{t('bugReports.table.component')}</TableHead>
                <TableHead>{t('bugReports.table.pitch')}</TableHead>
                <TableHead>{t('bugReports.table.assignee')}</TableHead>
                <TableHead>{t('bugReports.table.reporter')}</TableHead>
                <TableHead
                  className="cursor-pointer select-none"
                  onClick={() => handleSort('createdAt')}
                >
                  <div className="flex items-center gap-1">
                    {t('bugReports.table.created')}
                    {sortBy === 'createdAt' && (
                      sortOrder === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                    )}
                  </div>
                </TableHead>
                <TableHead className="text-center">
                  <div className="flex items-center justify-center gap-1">
                    <MessageSquare className="h-3 w-3" />
                  </div>
                </TableHead>
                <TableHead className="text-right">{t('bugReports.table.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {bugReports.map((bug) => (
                <TableRow key={bug.id}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      <Bug className="h-4 w-4 text-destructive" />
                      <span className="font-medium">{bug.bugKey}</span>
                    </div>
                  </TableCell>
                  <TableCell>
                    <div
                      className="max-w-[280px] truncate cursor-pointer hover:text-primary"
                      onClick={() => openDetailModal(bug)}
                      dir={detectTextDirection(bug.title)}
                      style={{ textAlign: detectTextDirection(bug.title) === 'rtl' ? 'right' : 'left' }}
                    >
                      {bug.title}
                    </div>
                    {bug.tags && (
                      <div className="flex gap-1 mt-1">
                        {bug.tags.split(',').slice(0, 2).map((tag: string) => (
                          <Badge key={tag} variant="outline" className="text-[0.65rem] py-0 px-1">
                            {tag.trim()}
                          </Badge>
                        ))}
                        {bug.tags.split(',').length > 2 && (
                          <Badge variant="outline" className="text-[0.65rem] py-0 px-1">
                            +{bug.tags.split(',').length - 2}
                          </Badge>
                        )}
                      </div>
                    )}
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" className="h-auto p-0" disabled={updatingBugId === bug.id}>
                          <Badge variant={severityBadgeVariants[bug.severity]} className="cursor-pointer">
                            {updatingBugId === bug.id ? <Loader2 className="h-3 w-3 animate-spin mr-1" /> : null}
                            {bug.severity}
                          </Badge>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="start">
                        {(['TRIVIAL', 'MINOR', 'MAJOR', 'CRITICAL', 'BLOCKER'] as BugSeverity[]).map((severity) => (
                          <DropdownMenuItem
                            key={severity}
                            onClick={() => handleInlineUpdate(bug.id, 'severity', severity)}
                          >
                            <Badge variant={severityBadgeVariants[severity]} className="mr-2">
                              {severity}
                            </Badge>
                            {bug.severity === severity && <Check className="ml-auto h-4 w-4" />}
                          </DropdownMenuItem>
                        ))}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" className="h-auto p-0" disabled={updatingBugId === bug.id}>
                          <Badge variant={statusBadgeVariants[bug.status]} className="cursor-pointer">
                            {updatingBugId === bug.id ? <Loader2 className="h-3 w-3 animate-spin mr-1" /> : null}
                            {bug.status.replace('_', ' ')}
                          </Badge>
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="start">
                        {(['OPEN', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED', 'REOPENED', 'WONT_FIX', 'DUPLICATE'] as BugStatus[]).map((status) => (
                          <DropdownMenuItem
                            key={status}
                            onClick={() => handleInlineUpdate(bug.id, 'status', status)}
                          >
                            <Badge variant={statusBadgeVariants[status]} className="mr-2">
                              {status.replace('_', ' ')}
                            </Badge>
                            {bug.status === status && <Check className="ml-auto h-4 w-4" />}
                          </DropdownMenuItem>
                        ))}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                  <TableCell>
                    {bug.component ? (
                      <Badge variant="outline" className="text-xs font-normal">{bug.component}</Badge>
                    ) : (
                      <span className="text-muted-foreground">-</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <span className="text-muted-foreground">{bug.pitchTitle || '-'}</span>
                  </TableCell>
                  <TableCell>
                    {bug.assigneeName ? (
                      <div className="flex items-center gap-2">
                        <Avatar className="h-6 w-6">
                          <AvatarFallback className="text-xs">
                            {bug.assigneeName.charAt(0)}
                          </AvatarFallback>
                        </Avatar>
                        <span className="text-sm">{bug.assigneeName}</span>
                      </div>
                    ) : (
                      <span className="text-muted-foreground">{t('bugReports.unassigned')}</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <span className="text-muted-foreground">{bug.reporterName || '-'}</span>
                  </TableCell>
                  <TableCell>
                    <span className="text-muted-foreground">
                      {formatLocalizedDate(new Date(bug.createdAt), i18n.language)}
                    </span>
                  </TableCell>
                  <TableCell className="text-center">
                    {(bug.commentCount ?? 0) > 0 ? (
                      <Badge variant="secondary" className="gap-1">
                        <MessageSquare className="h-3 w-3" />
                        {bug.commentCount}
                      </Badge>
                    ) : (
                      <span className="text-muted-foreground">-</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={() => navigate(`/qa/bug-reports/${bug.id}`)}
                            >
                              <Eye className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('bugReports.actions.openFullPage', 'Open full page')}</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={() => openEditModal(bug)}
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('bugReports.actions.edit')}</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-destructive hover:text-destructive"
                              onClick={() => openDeleteConfirm(bug.id)}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('bugReports.actions.delete')}</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
              {bugReports.length === 0 && (
                <TableRow>
                  <TableCell colSpan={10} className="text-center py-8">
                    <span className="text-muted-foreground">
                      {searchQuery || statusFilter.length > 0 || severityFilter.length > 0
                        ? t('bugReports.emptyState.noMatches')
                        : t('bugReports.emptyState.noBugs')}
                    </span>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>
        </ResponsiveTable>

        {/* Pagination */}
        <div className="flex items-center justify-between px-4 py-3 border-t">
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">{t('bugReports.pagination.rowsPerPage')}</span>
            <Combobox
              options={[
                { value: '5', label: '5' },
                { value: '10', label: '10' },
                { value: '25', label: '25' },
                { value: '50', label: '50' }
              ]}
              value={rowsPerPage.toString()}
              onValueChange={handleChangeRowsPerPage}
              triggerClassName="w-[70px] h-8"
            />
          </div>
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">
              {t('bugReports.pagination.rangeText', { 
                start: page * rowsPerPage + 1, 
                end: Math.min((page + 1) * rowsPerPage, totalElements), 
                total: totalElements 
              })}
            </span>
            <div className="flex gap-1">
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                onClick={() => handleChangePage(page - 1)}
                disabled={page === 0}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                onClick={() => handleChangePage(page + 1)}
                disabled={page >= totalPages - 1}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </Card>
      )}

      {/* Bug Reports - Kanban View */}
      {viewMode === 'kanban' && (
        <BugKanbanBoard
          bugs={bugReports}
          onStatusChange={(bugId, newStatus) => handleInlineUpdate(bugId, 'status', newStatus)}
          onViewBug={openDetailModal}
          onEditBug={(bug) => {
            setSelectedBug(bug);
            setModalOpen(true);
          }}
          onDeleteBug={handleDelete}
          loading={loading}
          updatingBugId={updatingBugId}
        />
      )}

      {/* Create/Edit Modal */}
      <BugReportModal
        open={modalOpen}
        onClose={handleModalClose}
        onSubmit={handleCreateOrUpdate}
        bugReport={selectedBug || undefined}
      />

      {/* Bug Detail Dialog - uses BugViewDialog with Activity tab */}
      <BugViewDialog
        bug={selectedBug}
        open={detailModalOpen}
        onOpenChange={setDetailModalOpen}
        onEdit={(bug) => {
          setDetailModalOpen(false);
          openEditModal(bug);
        }}
      />

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteConfirmOpen}
        onOpenChange={setDeleteConfirmOpen}
        title={t('bugReports.deleteTitle')}
        description={t('bugReports.confirmDelete')}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleDelete}
        variant="destructive"
      />
    </div>
  );
};

export default BugReportsPage;
