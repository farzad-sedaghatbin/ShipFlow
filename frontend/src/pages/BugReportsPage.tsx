import React, { useState, useEffect, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate, formatLocalizedDateTime } from '../utils/dateLocalization';
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
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Checkbox } from '../components/ui/checkbox';
import { Switch } from '../components/ui/switch';
import { Label } from '../components/ui/label';
import { Alert, AlertDescription } from '../components/ui/alert';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import qaTestManagementService from '../services/qaTestManagementService';
import { cycleService } from '../services/cycleService';
import { pitchService } from '../services/pitchService';
import { useProject } from '../contexts';
import { BugReport, BugStatus, BugSeverity, Cycle, Pitch } from '../types';
import BugReportModal from '../components/BugReportModal';

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

const BugReportsPage: React.FC = () => {
  const { t, i18n } = useTranslation();
  const { currentProject, isAllProjectsSelected, isKanbanProject } = useProject();
  const [bugReports, setBugReports] = useState<BugReport[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<BugStatus[]>([]);
  const [severityFilter, setSeverityFilter] = useState<BugSeverity[]>([]);
  const [assigneeFilter, setAssigneeFilter] = useState<number[]>([]);
  const [cycleFilter, setCycleFilter] = useState<number | undefined>(undefined);
  const [pitchFilter, setPitchFilter] = useState<number | undefined>(undefined);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [excludeMode, setExcludeMode] = useState(false);
  const [sortBy, setSortBy] = useState<'createdAt' | 'severity' | 'status' | 'title'>('createdAt');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('desc');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedBug, setSelectedBug] = useState<BugReport | null>(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const [severityDropdownOpen, setSeverityDropdownOpen] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'kanban'>('list');
  const [updatingBugId, setUpdatingBugId] = useState<number | null>(null);

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
    setCycleFilter(undefined);
    setPitchFilter(undefined);
    setPage(0); // Reset to first page when project changes
  }, [currentProject?.id, isAllProjectsSelected]);

  useEffect(() => {
    loadBugReports();
  }, [page, rowsPerPage, sortBy, sortOrder, statusFilter, severityFilter, assigneeFilter, excludeMode, cycleFilter, pitchFilter, currentProject?.id, isAllProjectsSelected]);

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

  const loadBugReports = async () => {
    setLoading(true);
    setError(null);
    try {
      let response;
      // Always use the filter endpoint for consistent server-side filtering
      const projectId = isAllProjectsSelected ? undefined : currentProject?.id;
      
      response = await qaTestManagementService.getBugReportsWithFilters(
        projectId,
        cycleFilter,
        pitchFilter,
        statusFilter.length > 0 ? statusFilter : undefined,
        severityFilter.length > 0 ? severityFilter : undefined,
        assigneeFilter.length > 0 ? assigneeFilter : undefined,
        excludeMode,
        page,
        rowsPerPage,
        sortBy,
        sortOrder
      );
      
      setBugReports(response.data.content);
      setTotalElements(response.data.totalElements);
    } catch (err) {
      setError(t('bugReports.loadFailed'));
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm(t('bugReports.confirmDelete'))) return;
    try {
      await qaTestManagementService.deleteBugReport(id);
      setBugReports(bugReports.filter((b) => b.id !== id));
    } catch (err) {
      setError(t('bugReports.deleteFailed'));
    }
  };

  const handleCreateOrUpdate = async (data: any) => {
    try {
      if (selectedBug) {
        console.log('Updating bug:', selectedBug.id, 'with data:', data);
        const response = await qaTestManagementService.updateBugReport(selectedBug.id, data);
        console.log('Update response:', response.data);
        setBugReports(bugReports.map((b) => (b.id === selectedBug.id ? response.data : b)));
      } else {
        console.log('Creating bug with data:', data);
        const response = await qaTestManagementService.createBugReport(data);
        console.log('Create response:', response.data);
        setBugReports([response.data, ...bugReports]);
      }
      setModalOpen(false);
      setSelectedBug(null);
    } catch (err) {
      console.error('Error in handleCreateOrUpdate:', err);
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

  // Kanban columns by status
  const kanbanColumns: BugStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED'];
  
  const bugsByStatus = useMemo(() => {
    const grouped: Record<BugStatus, BugReport[]> = {
      OPEN: [],
      IN_PROGRESS: [],
      RESOLVED: [],
      VERIFIED: [],
      CLOSED: [],
      REOPENED: [],
      WONT_FIX: [],
      DUPLICATE: [],
    };
    bugReports.forEach(bug => {
      grouped[bug.status].push(bug);
    });
    return grouped;
  }, [bugReports]);

  const getStatCounts = () => ({
    total: totalElements,
    open: bugReports.filter((b) => b.status === 'OPEN').length,
    inProgress: bugReports.filter((b) => b.status === 'IN_PROGRESS').length,
    resolved: bugReports.filter((b) => ['RESOLVED', 'VERIFIED', 'CLOSED'].includes(b.status)).length,
    critical: bugReports.filter((b) => ['CRITICAL', 'BLOCKER'].includes(b.severity)).length,
  });

  const stats = getStatCounts();
  const totalPages = Math.ceil(totalElements / rowsPerPage);

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h1 className="text-3xl font-bold tracking-tight">{t('bugReports.title')}</h1>
        <TooltipProvider>
          <Tooltip>
            <TooltipTrigger asChild>
              <span>
                <Button 
                  onClick={openCreateModal} 
                  className="w-full sm:w-auto"
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
      </div>

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
      <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-5 gap-4">
        <Card>
          <CardContent className="text-center py-4">
            <p className="text-2xl font-bold">{stats.total}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.total')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-4">
            <p className="text-2xl font-bold text-destructive">{stats.open}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.open')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-4">
            <p className="text-2xl font-bold text-primary">{stats.inProgress}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.inProgress')}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-4">
            <p className="text-2xl font-bold text-success">{stats.resolved}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.resolved')}</p>
          </CardContent>
        </Card>
        <Card className="col-span-2 sm:col-span-1">
          <CardContent className="text-center py-4">
            <p className="text-2xl font-bold text-destructive">{stats.critical}</p>
            <p className="text-xs text-muted-foreground">{t('bugReports.stats.criticalBlocker')}</p>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <div className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-4 items-end">
          {/* Search */}
          <div className="relative lg:col-span-2">
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

          {/* Status Filter */}
          <div className="relative">
            <Label className="text-xs mb-1 block">{t('bugReports.filters.status')}</Label>
            <Button
              variant="outline"
              className="w-full justify-between"
              onClick={() => setStatusDropdownOpen(!statusDropdownOpen)}
            >
              {statusFilter.length > 0 ? t('bugReports.filters.itemsSelected', { count: statusFilter.length }) : t('bugReports.filters.allStatus')}
            </Button>
            {statusDropdownOpen && (
              <div className="absolute z-50 mt-1 w-full bg-popover border rounded-md shadow-md p-2 space-y-1">
                {(Object.keys(statusBadgeVariants) as BugStatus[]).map((status) => (
                  <div
                    key={status}
                    className="flex items-center gap-2 px-2 py-1.5 hover:bg-accent rounded cursor-pointer"
                    onClick={() => toggleStatusFilter(status)}
                  >
                    <Checkbox checked={statusFilter.includes(status)} />
                    <span className="text-sm">{status.replace('_', ' ')}</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Severity Filter */}
          <div className="relative">
            <Label className="text-xs mb-1 block">{t('bugReports.filters.severity')}</Label>
            <Button
              variant="outline"
              className="w-full justify-between"
              onClick={() => setSeverityDropdownOpen(!severityDropdownOpen)}
            >
              {severityFilter.length > 0 ? t('bugReports.filters.itemsSelected', { count: severityFilter.length }) : t('bugReports.filters.allSeverity')}
            </Button>
            {severityDropdownOpen && (
              <div className="absolute z-50 mt-1 w-full bg-popover border rounded-md shadow-md p-2 space-y-1">
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

          {/* Exclude Mode + Clear */}
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Switch
                id="exclude-mode"
                checked={excludeMode}
                onCheckedChange={setExcludeMode}
              />
              <Label htmlFor="exclude-mode" className="text-sm">{t('bugReports.filters.exclude')}</Label>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                setStatusFilter([]);
                setSeverityFilter([]);
                setAssigneeFilter([]);
                setCycleFilter(undefined);
                setPitchFilter(undefined);
                setExcludeMode(false);
              }}
            >
              {t('bugReports.filters.clear')}
            </Button>
          </div>

          {/* Sort */}
          <div className="flex gap-2">
            <div className="flex-1">
              <Label className="text-xs mb-1 block">{t('bugReports.filters.sortBy')}</Label>
              <Select value={sortBy} onValueChange={(v) => setSortBy(v as typeof sortBy)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="createdAt">{t('bugReports.sort.createdDate')}</SelectItem>
                  <SelectItem value="severity">{t('bugReports.sort.severity')}</SelectItem>
                  <SelectItem value="status">{t('bugReports.sort.status')}</SelectItem>
                  <SelectItem value="title">{t('bugReports.sort.title')}</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="self-end">
              <Button
                variant="outline"
                size="icon"
                onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
              >
                {sortOrder === 'asc' ? <ArrowUp className="h-4 w-4" /> : <ArrowDown className="h-4 w-4" />}
              </Button>
            </div>
          </div>
        </div>

        {/* Cycle and Pitch Filters Row - Hidden for Kanban projects (Shape Up concepts) */}
        {!isKanbanProject && (
          <div className="flex flex-wrap gap-4">
            <div className="min-w-[180px]">
              <Label className="text-xs mb-1 block">{t('bugReports.filters.cycle')}</Label>
              <Select
                value={cycleFilter?.toString() ?? 'all'}
                onValueChange={(value) => setCycleFilter(value === 'all' ? undefined : parseInt(value))}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('bugReports.filters.allCycles')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">{t('bugReports.filters.allCycles')}</SelectItem>
                  {filteredCycles.map((cycle) => (
                    <SelectItem key={cycle.id} value={cycle.id.toString()}>
                      {cycle.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="min-w-[180px]">
              <Label className="text-xs mb-1 block">{t('bugReports.filters.pitch')}</Label>
              <Select
                value={pitchFilter?.toString() ?? 'all'}
                onValueChange={(value) => setPitchFilter(value === 'all' ? undefined : parseInt(value))}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('bugReports.filters.allPitches')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">{t('bugReports.filters.allPitches')}</SelectItem>
                  {filteredPitches.map((pitch) => (
                    <SelectItem key={pitch.id} value={pitch.id.toString()}>
                      {pitch.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        )}
      </div>

      {/* Bug Reports - List View */}
      {viewMode === 'list' && (
        <Card>
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
                    <Select
                      value={bug.severity}
                      onValueChange={(value) => handleInlineUpdate(bug.id, 'severity', value)}
                      disabled={updatingBugId === bug.id}
                    >
                      <SelectTrigger className="h-7 w-[110px] border-0 bg-transparent hover:bg-muted/50 focus:ring-0 p-1">
                        <Badge variant={severityBadgeVariants[bug.severity]} className="cursor-pointer">
                          {updatingBugId === bug.id ? <Loader2 className="h-3 w-3 animate-spin mr-1" /> : null}
                          {bug.severity}
                        </Badge>
                      </SelectTrigger>
                      <SelectContent>
                        {(['TRIVIAL', 'MINOR', 'MAJOR', 'CRITICAL', 'BLOCKER'] as BugSeverity[]).map((severity) => (
                          <SelectItem key={severity} value={severity}>
                            <Badge variant={severityBadgeVariants[severity]}>{severity}</Badge>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </TableCell>
                  <TableCell>
                    <Select
                      value={bug.status}
                      onValueChange={(value) => handleInlineUpdate(bug.id, 'status', value)}
                      disabled={updatingBugId === bug.id}
                    >
                      <SelectTrigger className="h-7 w-[130px] border-0 bg-transparent hover:bg-muted/50 focus:ring-0 p-1">
                        <Badge variant={statusBadgeVariants[bug.status]} className="cursor-pointer">
                          {updatingBugId === bug.id ? <Loader2 className="h-3 w-3 animate-spin mr-1" /> : null}
                          {bug.status.replace('_', ' ')}
                        </Badge>
                      </SelectTrigger>
                      <SelectContent>
                        {(['OPEN', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED', 'REOPENED', 'WONT_FIX', 'DUPLICATE'] as BugStatus[]).map((status) => (
                          <SelectItem key={status} value={status}>
                            <Badge variant={statusBadgeVariants[status]}>{status.replace('_', ' ')}</Badge>
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
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
                              onClick={() => openDetailModal(bug)}
                            >
                              <Eye className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('bugReports.actions.viewDetails')}</TooltipContent>
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
                              onClick={() => handleDelete(bug.id)}
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
                  <TableCell colSpan={9} className="text-center py-8">
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

        {/* Pagination */}
        <div className="flex items-center justify-between px-4 py-3 border-t">
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">{t('bugReports.pagination.rowsPerPage')}</span>
            <Select value={rowsPerPage.toString()} onValueChange={handleChangeRowsPerPage}>
              <SelectTrigger className="w-[70px] h-8">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="5">5</SelectItem>
                <SelectItem value="10">10</SelectItem>
                <SelectItem value="25">25</SelectItem>
                <SelectItem value="50">50</SelectItem>
              </SelectContent>
            </Select>
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
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {kanbanColumns.map((status) => (
            <div key={status} className="bg-muted/30 rounded-lg p-3 min-h-[400px]">
              <div className="flex items-center justify-between mb-3">
                <h3 className="font-semibold flex items-center gap-2">
                  <Badge variant={statusBadgeVariants[status]}>{status.replace('_', ' ')}</Badge>
                  <span className="text-muted-foreground text-sm">({bugsByStatus[status].length})</span>
                </h3>
              </div>
              <div className="space-y-2">
                {bugsByStatus[status].map((bug) => (
                  <Card
                    key={bug.id}
                    className="cursor-pointer hover:shadow-md transition-shadow"
                    onClick={() => openDetailModal(bug)}
                  >
                    <CardContent className="p-3 space-y-2">
                      <div className="flex items-center gap-2">
                        <Bug className="h-3 w-3 text-destructive shrink-0" />
                        <span className="text-xs text-muted-foreground font-mono">{bug.bugKey}</span>
                      </div>
                      <p 
                        className="text-sm font-medium line-clamp-2"
                        dir={detectTextDirection(bug.title)}
                      >
                        {bug.title}
                      </p>
                      <div className="flex items-center justify-between">
                        <Select
                          value={bug.severity}
                          onValueChange={(value) => {
                            handleInlineUpdate(bug.id, 'severity', value);
                          }}
                          disabled={updatingBugId === bug.id}
                        >
                          <SelectTrigger 
                            className="h-6 w-auto border-0 bg-transparent p-0 focus:ring-0"
                            onClick={(e) => e.stopPropagation()}
                          >
                            <Badge variant={severityBadgeVariants[bug.severity]} className="text-[0.65rem]">
                              {updatingBugId === bug.id ? <Loader2 className="h-2 w-2 animate-spin mr-1" /> : null}
                              {bug.severity}
                            </Badge>
                          </SelectTrigger>
                          <SelectContent>
                            {(['TRIVIAL', 'MINOR', 'MAJOR', 'CRITICAL', 'BLOCKER'] as BugSeverity[]).map((sev) => (
                              <SelectItem key={sev} value={sev}>
                                <Badge variant={severityBadgeVariants[sev]} className="text-[0.65rem]">{sev}</Badge>
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                        {bug.assigneeName && (
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Avatar className="h-5 w-5">
                                  <AvatarFallback className="text-[0.6rem]">
                                    {bug.assigneeName.charAt(0)}
                                  </AvatarFallback>
                                </Avatar>
                              </TooltipTrigger>
                              <TooltipContent>{bug.assigneeName}</TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                        )}
                      </div>
                      {(bug.commentCount ?? 0) > 0 && (
                        <div className="flex items-center gap-1 text-xs text-muted-foreground">
                          <MessageSquare className="h-3 w-3" />
                          <span>{bug.commentCount}</span>
                        </div>
                      )}
                      {/* Quick status change */}
                      <Select
                        value={bug.status}
                        onValueChange={(value) => handleInlineUpdate(bug.id, 'status', value)}
                        disabled={updatingBugId === bug.id}
                      >
                        <SelectTrigger 
                          className="h-6 text-xs border-dashed"
                          onClick={(e) => e.stopPropagation()}
                        >
                          <span className="text-muted-foreground">{t('bugReports.kanban.moveTo')}</span>
                        </SelectTrigger>
                        <SelectContent>
                          {(['OPEN', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED'] as BugStatus[]).filter(s => s !== bug.status).map((s) => (
                            <SelectItem key={s} value={s}>
                              <Badge variant={statusBadgeVariants[s]} className="text-[0.65rem]">{s.replace('_', ' ')}</Badge>
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </CardContent>
                  </Card>
                ))}
                {bugsByStatus[status].length === 0 && (
                  <div className="text-center py-8 text-muted-foreground text-sm">
                    {t('bugReports.kanban.empty')}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create/Edit Modal */}
      <BugReportModal
        open={modalOpen}
        onClose={handleModalClose}
        onSubmit={handleCreateOrUpdate}
        bugReport={selectedBug || undefined}
        cycleId={!isAllProjectsSelected && currentProject ? filteredCycles[0]?.id : undefined}
        pitchId={!isAllProjectsSelected && currentProject ? filteredPitches[0]?.id : undefined}
      />

      {/* Bug Detail Dialog */}
      <Dialog open={detailModalOpen} onOpenChange={setDetailModalOpen}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-3">
              <Bug className="h-5 w-5 text-destructive" />
              <div>
                <div className="text-lg">{selectedBug?.bugKey}</div>
                <div className="text-base font-normal text-muted-foreground">{selectedBug?.title}</div>
              </div>
            </DialogTitle>
          </DialogHeader>
          {selectedBug && (
            <div className="space-y-4">
              {/* Status and Severity badges */}
              <div className="flex gap-2">
                <Badge variant={severityBadgeVariants[selectedBug.severity]}>
                  {selectedBug.severity}
                </Badge>
                <Badge variant={statusBadgeVariants[selectedBug.status]}>
                  {selectedBug.status.replace('_', ' ')}
                </Badge>
              </div>

              {/* Description */}
              <div>
                <h4 className="text-sm font-semibold mb-2">{t('bugReports.detail.description')}</h4>
                <div className="border rounded-md p-3 bg-muted/30">
                  <p className="text-sm whitespace-pre-wrap">
                    {selectedBug.description || t('bugReports.detail.noDescription')}
                  </p>
                </div>
              </div>

              {/* Steps to Reproduce */}
              <div>
                <h4 className="text-sm font-semibold mb-2">{t('bugReports.detail.stepsToReproduce')}</h4>
                <div className="border rounded-md p-3 bg-muted/30">
                  <p className="text-sm whitespace-pre-wrap">
                    {selectedBug.stepsToReproduce || t('bugReports.detail.notProvided')}
                  </p>
                </div>
              </div>

              {/* Expected / Actual Behavior */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <h4 className="text-sm font-semibold mb-2">{t('bugReports.detail.expectedBehavior')}</h4>
                  <div className="border rounded-md p-3 bg-muted/30">
                    <p className="text-sm">{selectedBug.expectedBehavior || t('bugReports.detail.notProvided')}</p>
                  </div>
                </div>
                <div>
                  <h4 className="text-sm font-semibold mb-2">{t('bugReports.detail.actualBehavior')}</h4>
                  <div className="border rounded-md p-3 bg-muted/30">
                    <p className="text-sm">{selectedBug.actualBehavior || t('bugReports.detail.notProvided')}</p>
                  </div>
                </div>
              </div>

              {/* Environment */}
              <div className="text-sm text-muted-foreground">
                <strong>{t('bugReports.detail.environment')}</strong> {selectedBug.environment || '-'}
              </div>

              {/* Tags */}
              {selectedBug.tags && (
                <div>
                  <h4 className="text-sm font-semibold mb-2">{t('bugReports.detail.tags')}</h4>
                  <div className="flex flex-wrap gap-2">
                    {selectedBug.tags.split(',').map((tag: string) => (
                      <Badge key={tag} variant="secondary">{tag.trim()}</Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Metadata */}
              <div className="flex flex-col sm:flex-row gap-4 text-sm text-muted-foreground">
                <div>
                  <strong>{t('bugReports.detail.reporter')}</strong> {selectedBug.reporterName || '-'}
                </div>
                <div>
                  <strong>{t('bugReports.detail.assignee')}</strong> {selectedBug.assigneeName || t('bugReports.unassigned')}
                </div>
                <div>
                  <strong>{t('bugReports.detail.created')}</strong> {formatLocalizedDateTime(new Date(selectedBug.createdAt), i18n.language)}
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setDetailModalOpen(false)}>
              {t('bugReports.actions.close')}
            </Button>
            <Button
              onClick={() => {
                setDetailModalOpen(false);
                if (selectedBug) openEditModal(selectedBug);
              }}
            >
              {t('bugReports.actions.editBug')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default BugReportsPage;
