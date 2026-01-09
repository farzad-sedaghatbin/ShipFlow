import React, { useState, useEffect } from 'react';
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

  useEffect(() => {
    loadBugReports();
  }, [page, rowsPerPage, sortBy, sortOrder, statusFilter, severityFilter, assigneeFilter, excludeMode, cycleFilter, pitchFilter]);

  useEffect(() => {
    loadCyclesAndPitches();
  }, []);

  const loadCyclesAndPitches = async () => {
    try {
      const [cyclesRes, pitchesRes] = await Promise.all([
        cycleService.getAll(),
        pitchService.getAll(),
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
      if (statusFilter.length > 0 || severityFilter.length > 0 || assigneeFilter.length > 0 || cycleFilter || pitchFilter) {
        response = await qaTestManagementService.getBugReportsWithFilters(
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
      } else {
        response = await qaTestManagementService.getAllBugReports(page, rowsPerPage, sortBy, sortOrder);
      }
      setBugReports(response.data.content);
      setTotalElements(response.data.totalElements);
    } catch (err) {
      setError('Failed to load bug reports');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this bug report?')) return;
    try {
      await qaTestManagementService.deleteBugReport(id);
      setBugReports(bugReports.filter((b) => b.id !== id));
    } catch (err) {
      setError('Failed to delete bug report');
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
      }
      setModalOpen(false);
      setSelectedBug(null);
    } catch (err) {
      setError('Failed to save bug report');
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
        <h1 className="text-3xl font-bold tracking-tight">Bug Reports</h1>
        <Button onClick={openCreateModal} className="w-full sm:w-auto">
          <Plus className="h-4 w-4 mr-2" />
          Report Bug
        </Button>
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
            <p className="text-xs text-muted-foreground">Total</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-4">
            <p className="text-2xl font-bold text-destructive">{stats.open}</p>
            <p className="text-xs text-muted-foreground">Open</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-4">
            <p className="text-2xl font-bold text-primary">{stats.inProgress}</p>
            <p className="text-xs text-muted-foreground">In Progress</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="text-center py-4">
            <p className="text-2xl font-bold text-success">{stats.resolved}</p>
            <p className="text-xs text-muted-foreground">Resolved</p>
          </CardContent>
        </Card>
        <Card className="col-span-2 sm:col-span-1">
          <CardContent className="text-center py-4">
            <p className="text-2xl font-bold text-destructive">{stats.critical}</p>
            <p className="text-xs text-muted-foreground">Critical/Blocker</p>
          </CardContent>
        </Card>
      </div>

      {/* Filters */}
      <div className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-4 items-end">
          {/* Search */}
          <div className="relative lg:col-span-2">
            <Label htmlFor="bugs-search" className="sr-only">Search bugs</Label>
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" aria-hidden="true" />
            <Input
              id="bugs-search"
              type="search"
              placeholder="Search bugs..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-9"
              aria-label="Search bugs"
            />
          </div>

          {/* Status Filter */}
          <div className="relative">
            <Label className="text-xs mb-1 block">Status</Label>
            <Button
              variant="outline"
              className="w-full justify-between"
              onClick={() => setStatusDropdownOpen(!statusDropdownOpen)}
            >
              {statusFilter.length > 0 ? `${statusFilter.length} selected` : 'All Status'}
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
            <Label className="text-xs mb-1 block">Severity</Label>
            <Button
              variant="outline"
              className="w-full justify-between"
              onClick={() => setSeverityDropdownOpen(!severityDropdownOpen)}
            >
              {severityFilter.length > 0 ? `${severityFilter.length} selected` : 'All Severity'}
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
              <Label htmlFor="exclude-mode" className="text-sm">Exclude</Label>
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
              Clear
            </Button>
          </div>

          {/* Sort */}
          <div className="flex gap-2">
            <div className="flex-1">
              <Label className="text-xs mb-1 block">Sort By</Label>
              <Select value={sortBy} onValueChange={(v) => setSortBy(v as typeof sortBy)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="createdAt">Created Date</SelectItem>
                  <SelectItem value="severity">Severity</SelectItem>
                  <SelectItem value="status">Status</SelectItem>
                  <SelectItem value="title">Title</SelectItem>
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

        {/* Cycle and Pitch Filters Row */}
        <div className="flex flex-wrap gap-4">
          <div className="min-w-[180px]">
            <Label className="text-xs mb-1 block">Cycle</Label>
            <Select
              value={cycleFilter?.toString() ?? 'all'}
              onValueChange={(value) => setCycleFilter(value === 'all' ? undefined : parseInt(value))}
            >
              <SelectTrigger>
                <SelectValue placeholder="All Cycles" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Cycles</SelectItem>
                {cycles.map((cycle) => (
                  <SelectItem key={cycle.id} value={cycle.id.toString()}>
                    {cycle.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="min-w-[180px]">
            <Label className="text-xs mb-1 block">Pitch</Label>
            <Select
              value={pitchFilter?.toString() ?? 'all'}
              onValueChange={(value) => setPitchFilter(value === 'all' ? undefined : parseInt(value))}
            >
              <SelectTrigger>
                <SelectValue placeholder="All Pitches" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Pitches</SelectItem>
                {pitches.map((pitch) => (
                  <SelectItem key={pitch.id} value={pitch.id.toString()}>
                    {pitch.title}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      {/* Bug Reports Table */}
      <Card>
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Key</TableHead>
                <TableHead
                  className="cursor-pointer select-none"
                  onClick={() => handleSort('title')}
                >
                  <div className="flex items-center gap-1">
                    Title
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
                    Severity
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
                    Status
                    {sortBy === 'status' && (
                      sortOrder === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                    )}
                  </div>
                </TableHead>
                <TableHead>Pitch</TableHead>
                <TableHead>Assignee</TableHead>
                <TableHead>Reporter</TableHead>
                <TableHead
                  className="cursor-pointer select-none"
                  onClick={() => handleSort('createdAt')}
                >
                  <div className="flex items-center gap-1">
                    Created
                    {sortBy === 'createdAt' && (
                      sortOrder === 'asc' ? <ArrowUp className="h-3 w-3" /> : <ArrowDown className="h-3 w-3" />
                    )}
                  </div>
                </TableHead>
                <TableHead className="text-right">Actions</TableHead>
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
                    <Badge variant={severityBadgeVariants[bug.severity]}>{bug.severity}</Badge>
                  </TableCell>
                  <TableCell>
                    <Badge variant={statusBadgeVariants[bug.status]}>
                      {bug.status.replace('_', ' ')}
                    </Badge>
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
                      <span className="text-muted-foreground">Unassigned</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <span className="text-muted-foreground">{bug.reporterName || '-'}</span>
                  </TableCell>
                  <TableCell>
                    <span className="text-muted-foreground">
                      {new Date(bug.createdAt).toLocaleDateString()}
                    </span>
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
                          <TooltipContent>View Details</TooltipContent>
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
                          <TooltipContent>Edit</TooltipContent>
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
                          <TooltipContent>Delete</TooltipContent>
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
                        ? 'No bugs match the filters'
                        : 'No bugs reported yet. Report one if you find an issue!'}
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
            <span className="text-sm text-muted-foreground">Rows per page:</span>
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
              {page * rowsPerPage + 1}-{Math.min((page + 1) * rowsPerPage, totalElements)} of {totalElements}
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

      {/* Create/Edit Modal */}
      <BugReportModal
        open={modalOpen}
        onClose={() => {
          setModalOpen(false);
          setSelectedBug(null);
        }}
        onSubmit={handleCreateOrUpdate}
        bugReport={selectedBug || undefined}
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
                <h4 className="text-sm font-semibold mb-2">Description</h4>
                <div className="border rounded-md p-3 bg-muted/30">
                  <p className="text-sm whitespace-pre-wrap">
                    {selectedBug.description || 'No description provided'}
                  </p>
                </div>
              </div>

              {/* Steps to Reproduce */}
              <div>
                <h4 className="text-sm font-semibold mb-2">Steps to Reproduce</h4>
                <div className="border rounded-md p-3 bg-muted/30">
                  <p className="text-sm whitespace-pre-wrap">
                    {selectedBug.stepsToReproduce || 'Not provided'}
                  </p>
                </div>
              </div>

              {/* Expected / Actual Behavior */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <h4 className="text-sm font-semibold mb-2">Expected Behavior</h4>
                  <div className="border rounded-md p-3 bg-muted/30">
                    <p className="text-sm">{selectedBug.expectedBehavior || 'Not provided'}</p>
                  </div>
                </div>
                <div>
                  <h4 className="text-sm font-semibold mb-2">Actual Behavior</h4>
                  <div className="border rounded-md p-3 bg-muted/30">
                    <p className="text-sm">{selectedBug.actualBehavior || 'Not provided'}</p>
                  </div>
                </div>
              </div>

              {/* Environment */}
              <div className="text-sm text-muted-foreground">
                <strong>Environment:</strong> {selectedBug.environment || '-'}
              </div>

              {/* Tags */}
              {selectedBug.tags && (
                <div>
                  <h4 className="text-sm font-semibold mb-2">Tags</h4>
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
                  <strong>Reporter:</strong> {selectedBug.reporterName || '-'}
                </div>
                <div>
                  <strong>Assignee:</strong> {selectedBug.assigneeName || 'Unassigned'}
                </div>
                <div>
                  <strong>Created:</strong> {new Date(selectedBug.createdAt).toLocaleString()}
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setDetailModalOpen(false)}>
              Close
            </Button>
            <Button
              onClick={() => {
                setDetailModalOpen(false);
                if (selectedBug) openEditModal(selectedBug);
              }}
            >
              Edit Bug
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default BugReportsPage;
