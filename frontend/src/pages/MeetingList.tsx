import { useEffect, useState } from 'react';
import {
  Plus,
  Pencil,
  Trash2,
  Paperclip,
  Loader2,
  CalendarDays,
  Filter,
  X,
  UserPlus,
} from 'lucide-react';
import dayjs from 'dayjs';
import { meetingService, PageResponse } from '../services/meetingService';
import { pitchService } from '../services/pitchService';
import { retroService } from '../services/retroService';
import { personService } from '../services/personService';
import { Meeting, Pitch, CreateMeetingRequest, MeetingType, MeetingAction, ActionStatus, Retrospective, Person } from '../types';
import { QAFloatingButton } from '../components/QAFloatingButton';
import { MeetingDocumentsDialog } from '../components/MeetingDocumentsDialog';
import EmptyState from '../components/EmptyState';
import { EmptyMeetingsIllustration } from '../components/illustrations';
import { useToast } from '../contexts';


import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
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

const meetingTypes: MeetingType[] = ['SHAPING', 'BETTING', 'KICKOFF', 'STANDUP', 'DEMO', 'RETROSPECTIVE', 'HILL_CHART_REVIEW'];

export default function MeetingList() {
  const { showSuccess, showError } = useToast();
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [retrospectives, setRetrospectives] = useState<Retrospective[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialog, setDialog] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [docsDialog, setDocsDialog] = useState<{ open: boolean; meeting: Meeting | null }>({ open: false, meeting: null });
  
  // Pagination state
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  // Filter state
  const [showFilters, setShowFilters] = useState(false);
  const [filters, setFilters] = useState<{
    types: MeetingType[];
    startDate: string;
    endDate: string;
    dorReady?: boolean;
    dodReady?: boolean;
  }>({
    types: [],
    startDate: '',
    endDate: '',
  });

  const [formData, setFormData] = useState<CreateMeetingRequest>({
    pitchId: undefined,
    type: 'STANDUP',
    dateHeld: dayjs().format('YYYY-MM-DD'),
    dorReady: false,
    dodReady: false,
    notes: '',
    decisions: '',
    attendees: '',
    actions: [],
  });
  const [meetingDate, setMeetingDate] = useState<string>(dayjs().format('YYYY-MM-DD'));

  useEffect(() => {
    const abortController = new AbortController();
    loadData();
    return () => abortController.abort();
  }, [page, size, filters]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [pitchesRes, retrospectivesRes, personsRes] = await Promise.all([
        pitchService.getAll(),
        retroService.getByProject(1).catch(() => ({ data: [] })), // Get retrospectives for project 1 (or current project)
        personService.getAll(true), // Get active persons
      ]);
      setPitches(pitchesRes.data);
      setRetrospectives(retrospectivesRes.data || []);
      setPersons(personsRes);
      
      // Load meetings with pagination and filters
      const hasFilters = filters.types.length > 0 || filters.startDate || filters.endDate || 
                        filters.dorReady !== undefined || filters.dodReady !== undefined;
      
      const meetingsRes = hasFilters
        ? await meetingService.getWithFilters({
            types: filters.types.length > 0 ? filters.types : undefined,
            startDate: filters.startDate || undefined,
            endDate: filters.endDate || undefined,
            dorReady: filters.dorReady,
            dodReady: filters.dodReady,
            page,
            size,
            sortBy: 'dateHeld',
            sortOrder: 'desc',
          })
        : await meetingService.getPaginated({
            page,
            size,
            sortBy: 'dateHeld',
            sortOrder: 'desc',
          });
      
      setMeetings(meetingsRes.data.content);
      setTotalPages(meetingsRes.data.totalPages);
      setTotalElements(meetingsRes.data.totalElements);
    } catch (error: any) {
      if (error.name !== 'CanceledError') {
        console.error('Failed to load data:', error);
        showError('Failed to load meetings');
      }
    } finally {
      setLoading(false);
    }
  };

  const applyFilters = () => {
    setPage(0); // Reset to first page when filters change
  };

  const clearFilters = () => {
    setFilters({
      types: [],
      startDate: '',
      endDate: '',
    });
    setPage(0);
  };

  const handleOpenDialog = (meeting?: Meeting) => {
    if (meeting) {
      setEditId(meeting.id);
      setFormData({
        pitchId: meeting.pitchId,
        type: meeting.type,
        dateHeld: meeting.dateHeld,
        dorReady: meeting.dorReady,
        dodReady: meeting.dodReady,
        notes: meeting.notes || '',
        decisions: meeting.decisions || '',
        attendees: meeting.attendees || '',
        retrospectiveId: meeting.retrospectiveId,
        actions: meeting.actions || [],
      });
      setMeetingDate(meeting.dateHeld);
    } else {
      setEditId(null);
      setFormData({
        pitchId: undefined,
        type: 'STANDUP',
        dateHeld: dayjs().format('YYYY-MM-DD'),
        dorReady: false,
        dodReady: false,
        notes: '',
        decisions: '',
        attendees: '',
        actions: [],
      });
      setMeetingDate(dayjs().format('YYYY-MM-DD'));
    }
    setDialog(true);
  };

  const handleSubmit = async () => {
    if (!meetingDate) return;
    try {
      const data = {
        ...formData,
        dateHeld: meetingDate,
      };
      if (editId) {
        await meetingService.update(editId, data);
        showSuccess('Meeting updated');
      } else {
        await meetingService.create(data);
        showSuccess('Meeting created');
      }
      setDialog(false);
      loadData();
    } catch (error) {
      showError('Failed to save meeting');
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await meetingService.delete(id);
      showSuccess('Meeting deleted');
      loadData();
    } catch (error) {
      showError('Failed to delete meeting');
    }
  };

  const addAction = () => {
    setFormData({
      ...formData,
      actions: [
        ...(formData.actions || []),
        {
          description: '',
          status: 'OPEN' as ActionStatus,
          assignedToId: undefined,
          dueDate: undefined,
          notes: '',
        },
      ],
    });
  };

  const updateAction = (index: number, field: keyof MeetingAction, value: any) => {
    const updatedActions = [...(formData.actions || [])];
    updatedActions[index] = { ...updatedActions[index], [field]: value };
    setFormData({ ...formData, actions: updatedActions });
  };

  const removeAction = (index: number) => {
    const updatedActions = formData.actions?.filter((_, i) => i !== index) || [];
    setFormData({ ...formData, actions: updatedActions });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-48">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  const getMeetingTypeBadgeVariant = (type: MeetingType): 'default' | 'secondary' | 'success' | 'warning' | 'info' | 'outline' => {
    const variants: Record<MeetingType, 'default' | 'secondary' | 'success' | 'warning' | 'info' | 'outline'> = {
      SHAPING: 'secondary',
      BETTING: 'warning',
      KICKOFF: 'default',
      STANDUP: 'outline',
      DEMO: 'success',
      RETROSPECTIVE: 'info',
      HILL_CHART_REVIEW: 'default',
    };
    return variants[type] || 'outline';
  };

  const formatMeetingType = (type: MeetingType) => {
    return type.replace(/_/g, ' ');
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="flex flex-col">
          <h1 className="text-2xl font-bold text-foreground">
            Meetings
          </h1>
          <p className="text-sm text-muted-foreground">
            {totalElements} total meetings
          </p>
        </div>
        <div className="flex gap-2 w-full sm:w-auto">
          <Button 
            variant="outline" 
            onClick={() => setShowFilters(!showFilters)}
            className="flex-1 sm:flex-none"
          >
            <Filter className="h-4 w-4 mr-2" />
            Filters
          </Button>
          <Button onClick={() => handleOpenDialog()} className="flex-1 sm:flex-none">
            <Plus className="h-4 w-4 mr-2" />
            New Meeting
          </Button>
        </div>
      </div>

      {/* Filters */}
      {showFilters && (
        <Card>
          <CardContent className="pt-6">
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label>Meeting Types</Label>
                  <Select
                    value={filters.types.length > 0 ? filters.types[0] : 'all'}
                    onValueChange={(value) => {
                      if (value === 'all') {
                        setFilters({ ...filters, types: [] });
                      } else {
                        setFilters({ ...filters, types: [value as MeetingType] });
                      }
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="All types" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All Types</SelectItem>
                      {meetingTypes.map((type) => (
                        <SelectItem key={type} value={type}>
                          {formatMeetingType(type)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Start Date</Label>
                  <Input
                    type="date"
                    value={filters.startDate}
                    onChange={(e) => setFilters({ ...filters, startDate: e.target.value })}
                  />
                </div>
                <div className="space-y-2">
                  <Label>End Date</Label>
                  <Input
                    type="date"
                    value={filters.endDate}
                    onChange={(e) => setFilters({ ...filters, endDate: e.target.value })}
                  />
                </div>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label>DOR Ready</Label>
                  <Select
                    value={filters.dorReady === undefined ? 'all' : filters.dorReady ? 'yes' : 'no'}
                    onValueChange={(value) => {
                      setFilters({
                        ...filters,
                        dorReady: value === 'all' ? undefined : value === 'yes',
                      });
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All</SelectItem>
                      <SelectItem value="yes">Yes</SelectItem>
                      <SelectItem value="no">No</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>DOD Ready</Label>
                  <Select
                    value={filters.dodReady === undefined ? 'all' : filters.dodReady ? 'yes' : 'no'}
                    onValueChange={(value) => {
                      setFilters({
                        ...filters,
                        dodReady: value === 'all' ? undefined : value === 'yes',
                      });
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">All</SelectItem>
                      <SelectItem value="yes">Yes</SelectItem>
                      <SelectItem value="no">No</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="flex gap-2">
                <Button onClick={applyFilters}>Apply Filters</Button>
                <Button variant="outline" onClick={clearFilters}>
                  <X className="h-4 w-4 mr-2" />
                  Clear
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {meetingTypes.slice(0, 4).map((type) => (
          <Card key={type}>
            <CardContent className="pt-6 text-center">
              <Badge variant={getMeetingTypeBadgeVariant(type)} className="mb-2">
                {formatMeetingType(type)}
              </Badge>
              <p className="text-3xl font-bold text-foreground">
                {meetings.filter((m) => m.type === type).length}
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Meetings Table */}
      <Card>
        <CardContent className="pt-6">
          {meetings.length === 0 ? (
            <EmptyState
              illustration={<EmptyMeetingsIllustration />}
              title="No meetings yet"
              description="Schedule your first meeting to track shaping sessions, demos, and retrospectives"
              action={{
                label: 'Schedule Meeting',
                onClick: () => handleOpenDialog(),
                startIcon: <Plus className="h-4 w-4" />,
              }}
              size="medium"
            />
          ) : (
            <div className="rounded-md border border-border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Pitch</TableHead>
                    <TableHead>Attendees</TableHead>
                    <TableHead className="text-center">Actions</TableHead>
                    <TableHead className="text-center">DOR</TableHead>
                    <TableHead className="text-center">DOD</TableHead>
                    <TableHead className="text-center">Operations</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {meetings.map((meeting) => (
                    <TableRow key={meeting.id}>
                      <TableCell className="font-medium">
                        {new Date(meeting.dateHeld).toLocaleDateString()}
                      </TableCell>
                      <TableCell>
                        <Badge variant={getMeetingTypeBadgeVariant(meeting.type)}>
                          {formatMeetingType(meeting.type)}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {meeting.pitchTitle || '-'}
                      </TableCell>
                      <TableCell className="text-muted-foreground max-w-[150px] truncate">
                        {meeting.attendees || '-'}
                      </TableCell>
                      <TableCell className="text-center">
                        {meeting.actions && meeting.actions.length > 0 ? (
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger>
                                <Badge variant="outline">
                                  {meeting.actions.length} {meeting.actions.length === 1 ? 'action' : 'actions'}
                                </Badge>
                              </TooltipTrigger>
                              <TooltipContent>
                                <div className="text-xs space-y-1">
                                  {meeting.actions.map((action, idx) => (
                                    <div key={idx}>• {action.description.substring(0, 50)}...</div>
                                  ))}
                                </div>
                              </TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                        ) : (
                          <span className="text-muted-foreground text-sm">-</span>
                        )}
                      </TableCell>
                      <TableCell className="text-center">
                        <Badge
                          variant={meeting.dorReady ? 'success' : 'outline'}
                        >
                          {meeting.dorReady ? 'Yes' : 'No'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-center">
                        <Badge
                          variant={meeting.dodReady ? 'success' : 'outline'}
                        >
                          {meeting.dodReady ? 'Yes' : 'No'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center justify-center gap-1">
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Button 
                                  variant="ghost" 
                                  size="icon-sm"
                                  onClick={() => setDocsDialog({ open: true, meeting })}
                                >
                                  <Paperclip className="h-4 w-4" />
                                </Button>
                              </TooltipTrigger>
                              <TooltipContent>Documents</TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Button 
                                  variant="ghost" 
                                  size="icon-sm"
                                  onClick={() => handleOpenDialog(meeting)}
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
                                  size="icon-sm"
                                  className="text-destructive hover:text-destructive hover:bg-destructive/10"
                                  onClick={() => handleDelete(meeting.id)}
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
                </TableBody>
              </Table>
            </div>
          )}
          
          {/* Pagination */}
          {meetings.length > 0 && totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <div className="text-sm text-muted-foreground">
                Showing {page * size + 1} to {Math.min((page + 1) * size, totalElements)} of {totalElements} meetings
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                >
                  Previous
                </Button>
                <div className="text-sm text-muted-foreground">
                  Page {page + 1} of {totalPages}
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Meeting Documents Dialog */}
      {docsDialog.meeting && (
        <MeetingDocumentsDialog
          open={docsDialog.open}
          onClose={() => setDocsDialog({ open: false, meeting: null })}
          meetingId={docsDialog.meeting.id}
          meetingType={docsDialog.meeting.type}
          meetingDate={docsDialog.meeting.dateHeld}
        />
      )}

      {/* Meeting Dialog */}
      <Dialog open={dialog} onOpenChange={setDialog}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{editId ? 'Edit Meeting' : 'New Meeting'}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            {/* Basic Info */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="meeting-type">Type *</Label>
                <Select
                  value={formData.type}
                  onValueChange={(value) => setFormData({ ...formData, type: value as MeetingType })}
                >
                  <SelectTrigger id="meeting-type">
                    <SelectValue placeholder="Select type" />
                  </SelectTrigger>
                  <SelectContent>
                    {meetingTypes.map((type) => (
                      <SelectItem key={type} value={type}>
                        {formatMeetingType(type)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="meeting-date">Date *</Label>
                <div className="relative">
                  <CalendarDays className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="meeting-date"
                    type="date"
                    value={meetingDate}
                    onChange={(e) => setMeetingDate(e.target.value)}
                    className="pl-9"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Pitch & Retrospective */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="meeting-pitch">Pitch (optional)</Label>
                <Select
                  value={formData.pitchId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ ...formData, pitchId: value === 'none' ? undefined : parseInt(value) })}
                >
                  <SelectTrigger id="meeting-pitch">
                    <SelectValue placeholder="Select pitch" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
                    {pitches.map((p) => (
                      <SelectItem key={p.id} value={p.id.toString()}>
                        {p.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="meeting-retro">Retrospective (optional)</Label>
                <Select
                  value={formData.retrospectiveId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ ...formData, retrospectiveId: value === 'none' ? undefined : parseInt(value) })}
                >
                  <SelectTrigger id="meeting-retro">
                    <SelectValue placeholder="Select retrospective" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
                    {retrospectives.map((r) => (
                      <SelectItem key={r.id} value={r.id.toString()}>
                        {r.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* DOR & DOD */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="dor-ready">DOR Ready</Label>
                <Select
                  value={formData.dorReady ? 'yes' : 'no'}
                  onValueChange={(value) => setFormData({ ...formData, dorReady: value === 'yes' })}
                >
                  <SelectTrigger id="dor-ready">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="yes">Yes</SelectItem>
                    <SelectItem value="no">No</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="dod-ready">DOD Ready</Label>
                <Select
                  value={formData.dodReady ? 'yes' : 'no'}
                  onValueChange={(value) => setFormData({ ...formData, dodReady: value === 'yes' })}
                >
                  <SelectTrigger id="dod-ready">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="yes">Yes</SelectItem>
                    <SelectItem value="no">No</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Attendees */}
            <div className="space-y-2">
              <Label htmlFor="meeting-attendees">Attendees</Label>
              <Input
                id="meeting-attendees"
                value={formData.attendees || ''}
                onChange={(e) => setFormData({ ...formData, attendees: e.target.value })}
                placeholder="e.g., John Doe, Jane Smith, Bob Johnson"
              />
              <p className="text-xs text-muted-foreground">Comma-separated names</p>
            </div>

            {/* Notes */}
            <div className="space-y-2">
              <Label htmlFor="meeting-notes">Notes</Label>
              <Textarea
                id="meeting-notes"
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                rows={3}
                placeholder="Add meeting notes..."
              />
            </div>

            {/* Decisions */}
            <div className="space-y-2">
              <Label htmlFor="meeting-decisions">Key Decisions</Label>
              <Textarea
                id="meeting-decisions"
                value={formData.decisions || ''}
                onChange={(e) => setFormData({ ...formData, decisions: e.target.value })}
                rows={3}
                placeholder="Record key decisions made during the meeting..."
              />
            </div>

            {/* Action Items */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <Label>Action Items</Label>
                <Button type="button" variant="outline" size="sm" onClick={addAction}>
                  <Plus className="h-4 w-4 mr-1" />
                  Add Action
                </Button>
              </div>
              
              {formData.actions && formData.actions.length > 0 ? (
                <div className="space-y-3">
                  {formData.actions.map((action, index) => (
                    <Card key={index} className="p-4">
                      <div className="space-y-3">
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1 space-y-2">
                            <Input
                              placeholder="Action description *"
                              value={action.description}
                              onChange={(e) => updateAction(index, 'description', e.target.value)}
                            />
                          </div>
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon-sm"
                            onClick={() => removeAction(index)}
                            className="text-destructive"
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        </div>
                        
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                          <div className="space-y-1">
                            <Label className="text-xs">Assignee</Label>
                            <Select
                              value={action.assignedToId?.toString() || 'none'}
                              onValueChange={(value) => 
                                updateAction(index, 'assignedToId', value === 'none' ? undefined : parseInt(value))
                              }
                            >
                              <SelectTrigger className="h-9">
                                <SelectValue placeholder="Unassigned" />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="none">Unassigned</SelectItem>
                                {persons.map((p) => (
                                  <SelectItem key={p.id} value={p.id.toString()}>
                                    {p.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                          
                          <div className="space-y-1">
                            <Label className="text-xs">Status</Label>
                            <Select
                              value={action.status}
                              onValueChange={(value) => updateAction(index, 'status', value as ActionStatus)}
                            >
                              <SelectTrigger className="h-9">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="OPEN">Open</SelectItem>
                                <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
                                <SelectItem value="COMPLETED">Completed</SelectItem>
                                <SelectItem value="CANCELLED">Cancelled</SelectItem>
                              </SelectContent>
                            </Select>
                          </div>
                          
                          <div className="space-y-1">
                            <Label className="text-xs">Due Date</Label>
                            <Input
                              type="date"
                              className="h-9"
                              value={action.dueDate || ''}
                              onChange={(e) => updateAction(index, 'dueDate', e.target.value || undefined)}
                            />
                          </div>
                        </div>
                        
                        <div className="space-y-1">
                          <Label className="text-xs">Notes</Label>
                          <Input
                            placeholder="Additional notes (optional)"
                            className="h-9"
                            value={action.notes || ''}
                            onChange={(e) => updateAction(index, 'notes', e.target.value)}
                          />
                        </div>
                      </div>
                    </Card>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground text-center py-4">
                  No action items yet. Click "Add Action" to create one.
                </p>
              )}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialog(false)}>
              Cancel
            </Button>
            <Button onClick={handleSubmit}>
              {editId ? 'Update' : 'Create'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <QAFloatingButton contextType="meeting" />
    </div>
  );
}
