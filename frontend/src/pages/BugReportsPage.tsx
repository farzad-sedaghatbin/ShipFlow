import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useNavigate, useSearchParams, Link as RouterLink } from 'react-router-dom';
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
  ChevronsLeft,
  ChevronsRight,
  MessageSquare,
  LayoutList,
  Kanban,
  Check,
  Info,
  Link,
  BookmarkPlus,
  Bookmark,
  BookmarkX,
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
import { useProject, useAuth, useToast } from '../contexts';
import { useListLoader } from '../hooks';
import { BugReport, BugStats, BugStatus, BugSeverity, Cycle, Pitch, Release, Person, getPageTotal } from '../types';
import BugReportModal from '../components/BugReportModal';
import BugKanbanBoard from '../components/BugKanbanBoard';
import { BugViewDialog } from '../components/BugViewDialog';
import { BugReportsSkeleton } from '../components/Skeletons';

const FILTER_KEYS = ['q', 'status', 'severity', 'assignee', 'cycle', 'pitch', 'release', 'exclude', 'sortBy', 'sortOrder', 'page', 'size'];

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
const NAMED_BUG_FILTERS_KEY = 'shipflow.namedBugFilters';

// Derive overview stats from a bug list, mirroring the backend's BugReportService.getBugStats
// semantics exactly. Used when a filter (e.g. release) is applied client-side and the server
// stats endpoint can't see it — keeps the overview cards in sync with the visible table.
function computeBugStats(bugs: BugReport[]): BugStats {
  return {
    total: bugs.length,
    open: bugs.filter((b) => b.status === 'OPEN').length,
    inProgress: bugs.filter((b) => b.status === 'IN_PROGRESS').length,
    resolved: bugs.filter((b) => b.status === 'RESOLVED' || b.status === 'VERIFIED' || b.status === 'CLOSED').length,
    critical: bugs.filter((b) => b.severity === 'CRITICAL' || b.severity === 'BLOCKER').length,
  };
}

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

function readSavedBugFilter<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(BUG_FILTER_KEY);
    const parsed = raw ? JSON.parse(raw) : null;
    return parsed?.[key] ?? fallback;
  } catch {
    return fallback;
  }
}

type SavedBugFilter = {
  id: string;
  name: string;
  searchQuery: string;
  statusFilter: BugStatus[];
  severityFilter: BugSeverity[];
  assigneeFilter?: number;
  excludeMode: boolean;
  sortBy: 'createdAt' | 'severity' | 'status' | 'title';
  sortOrder: 'asc' | 'desc';
};

function loadNamedFilters(userId: number | undefined): SavedBugFilter[] {
  try {
    const raw = localStorage.getItem(`${NAMED_BUG_FILTERS_KEY}.${userId ?? 'guest'}`);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function persistNamedFilters(userId: number | undefined, filters: SavedBugFilter[]): void {
  try {
    localStorage.setItem(`${NAMED_BUG_FILTERS_KEY}.${userId ?? 'guest'}`, JSON.stringify(filters));
  } catch {}
}

const BugReportsPage: React.FC = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { currentProject, isAllProjectsSelected, isKanbanProject, isSwitchingProject, notifyProjectSwitchComplete } = useProject();
  const { user } = useAuth();
  const { showToast } = useToast();
  const [bugReports, setBugReports] = useState<BugReport[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [bugStats, setBugStats] = useState<BugStats>({ total: 0, open: 0, inProgress: 0, resolved: 0, critical: 0 });
  const { loading, refreshing, startLoad, finishLoad, resetInitial } = useListLoader();
  const [error, setError] = useState<string | null>(null);

  // Filter state — URL params are the primary source of truth (shareable, back-button safe).
  // When there are no URL params (fresh navigation or new session), fall back to localStorage
  // so the user's last view is restored automatically.
  const [searchQuery, setSearchQuery] = useState(() => searchParams.get('q') ?? readSavedBugFilter('searchQuery', ''));
  const [debouncedSearch, setDebouncedSearch] = useState(() => searchParams.get('q') ?? readSavedBugFilter('searchQuery', ''));
  const [statusFilter, setStatusFilter] = useState<BugStatus[]>(() =>
    searchParams.getAll('status').length > 0 ? searchParams.getAll('status') as BugStatus[] : readSavedBugFilter('statusFilter', [])
  );
  const [severityFilter, setSeverityFilter] = useState<BugSeverity[]>(() =>
    searchParams.getAll('severity').length > 0 ? searchParams.getAll('severity') as BugSeverity[] : readSavedBugFilter('severityFilter', [])
  );
  const [assigneeFilter, setAssigneeFilter] = useState<number | undefined>(() => {
    const v = searchParams.get('assignee'); return v ? parseInt(v) : readSavedBugFilter('assigneeFilter', undefined);
  });
  const [cycleFilter, setCycleFilter] = useState<number | undefined>(() => {
    const v = searchParams.get('cycle'); return v ? parseInt(v) : undefined;
  });
  const [pitchFilter, setPitchFilter] = useState<number | undefined>(() => {
    const v = searchParams.get('pitch'); return v ? parseInt(v) : undefined;
  });
  const [releaseFilter, setReleaseFilter] = useState<number | undefined>(() => {
    const v = searchParams.get('release'); return v ? parseInt(v) : undefined;
  });
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [releases, setReleases] = useState<Release[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [excludeMode, setExcludeMode] = useState(() =>
    searchParams.has('exclude') ? searchParams.get('exclude') === 'true' : readSavedBugFilter('excludeMode', false)
  );
  const [sortBy, setSortBy] = useState<'createdAt' | 'severity' | 'status' | 'title'>(
    () => (searchParams.get('sortBy') as 'createdAt' | 'severity' | 'status' | 'title') ?? readSavedBugFilter('sortBy', 'createdAt')
  );
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>(
    () => (searchParams.get('sortOrder') as 'asc' | 'desc') ?? readSavedBugFilter('sortOrder', 'desc')
  );
  const [page, setPage] = useState(() => parseInt(searchParams.get('page') ?? '0'));
  const [rowsPerPage, setRowsPerPage] = useState(() =>
    parseInt(searchParams.get('size') ?? String(readSavedBugFilter('rowsPerPage', 10)))
  );
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedBug, setSelectedBug] = useState<BugReport | null>(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const [severityDropdownOpen, setSeverityDropdownOpen] = useState(false);
  const [viewMode, setViewMode] = useState<'list' | 'kanban'>(() => readSavedBugFilter('viewMode', 'list'));
  const [updatingBugId, setUpdatingBugId] = useState<number | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [bugToDelete, setBugToDelete] = useState<number | null>(null);

  // Saved named filters (per user, stored in localStorage)
  const [savedFilters, setSavedFilters] = useState<SavedBugFilter[]>(() => loadNamedFilters(user?.userId));
  const [savedFilterPanelOpen, setSavedFilterPanelOpen] = useState(false);
  const [savedFilterName, setSavedFilterName] = useState('');
  const savedFilterPanelRef = useRef<HTMLDivElement>(null);

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

  // Keep URL in sync with filter state so the address bar is always shareable
  // and browser Back/Forward restores the exact filter view.
  useEffect(() => {
    setSearchParams(prev => {
      const next = new URLSearchParams();
      // Carry through non-filter params that may be set by other code
      prev.forEach((v, k) => { if (!FILTER_KEYS.includes(k)) next.set(k, v); });
      if (searchQuery) next.set('q', searchQuery);
      statusFilter.forEach(s => next.append('status', s));
      severityFilter.forEach(s => next.append('severity', s));
      if (assigneeFilter !== undefined) next.set('assignee', String(assigneeFilter));
      if (cycleFilter !== undefined) next.set('cycle', String(cycleFilter));
      if (pitchFilter !== undefined) next.set('pitch', String(pitchFilter));
      if (releaseFilter !== undefined) next.set('release', String(releaseFilter));
      if (excludeMode) next.set('exclude', 'true');
      if (sortBy !== 'createdAt') next.set('sortBy', sortBy);
      if (sortOrder !== 'desc') next.set('sortOrder', sortOrder);
      if (page > 0) next.set('page', String(page));
      if (rowsPerPage !== 10) next.set('size', String(rowsPerPage));
      return next;
    }, { replace: true });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, severityFilter, assigneeFilter, cycleFilter, pitchFilter, releaseFilter, excludeMode, sortBy, sortOrder, page, rowsPerPage, searchQuery]);

  // Reset cycle and pitch filters when project changes to ensure clean filtering
  useEffect(() => {
    resetInitial(); // force full skeleton on next project load
    setCycleFilter(undefined);
    setPitchFilter(undefined);
    setReleaseFilter(undefined);
    setPage(0);
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
    startLoad();
    setError(null);
    try {
      const projectId = isAllProjectsSelected ? undefined : currentProject?.id;
      const activeStatuses = statusFilter.length > 0 ? statusFilter : undefined;
      const activeSeverities = severityFilter.length > 0 ? severityFilter : undefined;
      const activeAssignees = assigneeFilter !== undefined ? [assigneeFilter] : undefined;

      // When release filter is active, fetch a large page so client-side
      // filtering covers all records (no reliable backend support yet).
      const effectivePage = releaseFilter !== undefined ? 0 : page;
      const effectiveSize = releaseFilter !== undefined ? 1000 : rowsPerPage;

      // Fire page data and aggregate stats in parallel
      const [response, statsResponse] = await Promise.all([
        qaTestManagementService.getBugReportsWithFilters(
          projectId, cycleFilter, pitchFilter,
          activeStatuses, activeSeverities, activeAssignees,
          excludeMode, effectivePage, effectiveSize,
          sortBy, sortOrder, debouncedSearch || undefined
        ),
        qaTestManagementService.getBugStats(
          projectId, cycleFilter, pitchFilter,
          activeStatuses, activeSeverities, activeAssignees,
          excludeMode, debouncedSearch || undefined
        ),
      ]);

      let bugData = response.data.content;
      if (releaseFilter !== undefined) {
        bugData = bugData.filter(bug => bug.targetReleaseId === releaseFilter);
      }

      setBugReports(bugData);
      setTotalElements(releaseFilter !== undefined ? bugData.length : getPageTotal(response.data));
      // The release filter is applied client-side, so the server stats endpoint can't account
      // for it — recompute the overview cards from the filtered set to keep them in sync.
      setBugStats(releaseFilter !== undefined ? computeBugStats(bugData) : statsResponse.data);
    } catch (err) {
      setError(t('bugReports.loadFailed'));
      console.error(err);
    } finally {
      finishLoad();
      notifyProjectSwitchComplete();
    }
  };

  // Close saved-filter panel on outside click
  useEffect(() => {
    if (!savedFilterPanelOpen) return;
    const handler = (e: MouseEvent) => {
      if (savedFilterPanelRef.current && !savedFilterPanelRef.current.contains(e.target as Node)) {
        setSavedFilterPanelOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [savedFilterPanelOpen]);

  const handleSaveFilter = () => {
    const name = savedFilterName.trim();
    if (!name) return;
    const entry: SavedBugFilter = {
      id: String(Date.now()),
      name,
      searchQuery,
      statusFilter,
      severityFilter,
      assigneeFilter,
      excludeMode,
      sortBy,
      sortOrder,
    };
    const next = [entry, ...savedFilters];
    setSavedFilters(next);
    persistNamedFilters(user?.userId, next);
    setSavedFilterName('');
    showToast(t('bugReports.savedFilters.saved', 'Filter saved'), 'success');
  };

  const handleApplyFilter = (f: SavedBugFilter) => {
    setSearchQuery(f.searchQuery);
    setDebouncedSearch(f.searchQuery);
    setStatusFilter(f.statusFilter);
    setSeverityFilter(f.severityFilter);
    setAssigneeFilter(f.assigneeFilter);
    setExcludeMode(f.excludeMode);
    setSortBy(f.sortBy);
    setSortOrder(f.sortOrder);
    setPage(0);
    setSavedFilterPanelOpen(false);
  };

  const handleDeleteSavedFilter = (id: string) => {
    const next = savedFilters.filter(f => f.id !== id);
    setSavedFilters(next);
    persistNamedFilters(user?.userId, next);
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
    setPage(0);
  };

  const toggleSeverityFilter = (severity: BugSeverity) => {
    setSeverityFilter((prev) =>
      prev.includes(severity) ? prev.filter((s) => s !== severity) : [...prev, severity]
    );
    setPage(0);
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

  const stats = bugStats;
  const totalPages = Math.ceil(totalElements / rowsPerPage);

  if (loading || isSwitchingProject) {
    return <BugReportsSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Thin refresh progress bar — no page blink, just a subtle indicator */}
      <div className={`fixed top-0 left-0 right-0 z-50 h-0.5 overflow-hidden transition-opacity duration-300 ${refreshing ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}>
        <div className={`h-full w-full bg-primary ${refreshing ? 'animate-page-progress' : ''}`} />
      </div>

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

          {/* Copy shareable link */}
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button variant="outline" size="icon" className="h-9 w-9"
                  onClick={() => {
                    navigator.clipboard.writeText(window.location.href);
                    showToast(t('bugReports.linkCopied', 'Link copied to clipboard'), 'success');
                  }}>
                  <Link className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('bugReports.copyLink', 'Copy shareable link')}</TooltipContent>
            </Tooltip>
          </TooltipProvider>

          {/* Save / load named filters */}
          <div className="relative" ref={savedFilterPanelRef}>
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    variant="outline"
                    size="icon"
                    className="h-9 w-9"
                    onClick={() => setSavedFilterPanelOpen(v => !v)}
                  >
                    <Bookmark className="h-4 w-4" />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>{t('bugReports.savedFilters.title', 'Saved filters')}</TooltipContent>
              </Tooltip>
            </TooltipProvider>

            {savedFilterPanelOpen && (
              <div className="absolute right-0 top-full mt-1 z-50 w-72 bg-popover border rounded-md shadow-lg p-3 space-y-3">
                {/* Save current filter */}
                <div className="space-y-1.5">
                  <p className="text-xs font-medium text-muted-foreground">{t('bugReports.savedFilters.saveCurrent', 'Save current filters')}</p>
                  <div className="flex gap-1.5">
                    <input
                      type="text"
                      className="flex-1 h-8 rounded-md border border-input bg-background px-2 text-sm focus:outline-none focus:ring-1 focus:ring-ring"
                      placeholder={t('bugReports.savedFilters.namePlaceholder', 'Filter name...')}
                      value={savedFilterName}
                      onChange={e => setSavedFilterName(e.target.value)}
                      onKeyDown={e => { if (e.key === 'Enter') handleSaveFilter(); }}
                    />
                    <Button size="sm" className="h-8 px-2" onClick={handleSaveFilter} disabled={!savedFilterName.trim()}>
                      <BookmarkPlus className="h-4 w-4" />
                    </Button>
                  </div>
                </div>

                {/* Saved filters list */}
                {savedFilters.length > 0 ? (
                  <div className="space-y-1 border-t pt-2">
                    <p className="text-xs font-medium text-muted-foreground">{t('bugReports.savedFilters.title', 'Saved filters')}</p>
                    {savedFilters.map(f => (
                      <div key={f.id} className="flex items-center gap-1 group">
                        <button
                          type="button"
                          className="flex-1 text-left text-sm px-2 py-1 rounded hover:bg-accent truncate"
                          onClick={() => handleApplyFilter(f)}
                        >
                          {f.name}
                        </button>
                        <button
                          type="button"
                          className="shrink-0 p-1 rounded text-muted-foreground hover:text-destructive opacity-0 group-hover:opacity-100 transition-opacity"
                          onClick={() => handleDeleteSavedFilter(f.id)}
                          title={t('common.delete', 'Delete')}
                        >
                          <BookmarkX className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-muted-foreground border-t pt-2">{t('bugReports.savedFilters.none', 'No saved filters yet')}</p>
                )}
              </div>
            )}
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
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <div className="flex items-center gap-1.5">
                    <Switch
                      id="exclude-mode"
                      checked={excludeMode}
                      onCheckedChange={setExcludeMode}
                    />
                    <Label htmlFor="exclude-mode" className="text-sm cursor-pointer">
                      {t('bugReports.filters.excludeLabel')}
                    </Label>
                  </div>
                </TooltipTrigger>
                <TooltipContent side="top" className="max-w-xs">
                  {t('bugReports.filters.excludeTooltip')}
                </TooltipContent>
              </Tooltip>
            </TooltipProvider>
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
        <Card className={`transition-opacity duration-200 ${refreshing ? 'opacity-60' : 'opacity-100'}`}>
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
                      <Button asChild variant="ghost" size="icon-sm" title={t('bugReports.actions.openFullPage', 'Open full page')}>
                        <RouterLink to={`/qa/bug-reports/${bug.id}`}>
                          <Eye className="h-4 w-4" />
                        </RouterLink>
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
                              asChild
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                            >
                              <RouterLink to={`/qa/bug-reports/${bug.id}`}>
                                <Eye className="h-4 w-4" />
                              </RouterLink>
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
          <div className="flex items-center gap-3">
            <span className="text-sm text-muted-foreground">
              {totalElements === 0
                ? t('bugReports.pagination.empty', 'No results')
                : t('bugReports.pagination.rangeText', {
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
                onClick={() => handleChangePage(0)}
                disabled={page === 0}
                aria-label={t('bugReports.pagination.first', 'First page')}
                title={t('bugReports.pagination.first', 'First page')}
              >
                <ChevronsLeft className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                onClick={() => handleChangePage(page - 1)}
                disabled={page === 0}
                aria-label={t('bugReports.pagination.previous', 'Previous page')}
                title={t('bugReports.pagination.previous', 'Previous page')}
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
                    onClick={() => handleChangePage(item)}
                    aria-label={t('bugReports.pagination.goToPage', 'Go to page {{page}}', { page: item + 1 })}
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
                onClick={() => handleChangePage(page + 1)}
                disabled={page >= totalPages - 1}
                aria-label={t('bugReports.pagination.next', 'Next page')}
                title={t('bugReports.pagination.next', 'Next page')}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
              <Button
                variant="outline"
                size="icon"
                className="h-8 w-8"
                onClick={() => handleChangePage(totalPages - 1)}
                disabled={page >= totalPages - 1}
                aria-label={t('bugReports.pagination.last', 'Last page')}
                title={t('bugReports.pagination.last', 'Last page')}
              >
                <ChevronsRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </div>
      </Card>
      )}

      {/* Bug Reports - Kanban View */}
      {viewMode === 'kanban' && (
        <div className={`transition-opacity duration-200 ${refreshing ? 'opacity-60' : 'opacity-100'}`}>
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
        </div>
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
        onOpenFullPage={(bug) => {
          setDetailModalOpen(false);
          navigate(`/qa/bug-reports/${bug.id}`);
        }}
        onUpdate={(updated) => setSelectedBug(updated)}
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
