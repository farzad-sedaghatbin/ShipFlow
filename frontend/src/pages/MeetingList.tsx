import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Plus,
  Pencil,
  Trash2,
  Paperclip,
  Loader2,
  CalendarDays,
  Filter,
  X,
} from 'lucide-react';
import dayjs from 'dayjs';
import { meetingService } from '../services/meetingService';
import { pitchService } from '../services/pitchService';
import { retroService } from '../services/retroService';
import { personService } from '../services/personService';
import { Meeting, Pitch, CreateMeetingRequest, MeetingType, MeetingAction, ActionStatus, Retrospective, Person } from '../types';
import { QAFloatingButton } from '../components/QAFloatingButton';
import { MeetingDocumentsDialog } from '../components/MeetingDocumentsDialog';
import EmptyState from '../components/EmptyState';
import { EmptyMeetingsIllustration } from '../components/illustrations';
import { useToast, useProject } from '../contexts';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { LocalizedDateInput } from '../components/LocalizedDateInput';


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
  const { t, i18n } = useTranslation();
  const { showSuccess, showError } = useToast();
  const { currentProject } = useProject();
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
  const [size] = useState(20);
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
  }, [page, size, filters, currentProject]);

  const loadData = async () => {
    try {
      setLoading(true);
      const [pitchesRes, retrospectivesRes, personsRes] = await Promise.all([
        pitchService.getAll(),
        currentProject 
          ? retroService.getByProject(currentProject.id).catch(() => ({ data: [] }))
          : Promise.resolve({ data: [] }),
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
        showSuccess(t('meetingList.toast.updateSuccess'));
      } else {
        await meetingService.create(data);
        showSuccess(t('meetingList.toast.createSuccess'));
      }
      setDialog(false);
      loadData();
    } catch (error) {
      showError(t('meetingList.toast.saveFailed'));
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await meetingService.delete(id);
      showSuccess(t('meetingList.toast.deleteSuccess'));
      loadData();
    } catch (error) {
      showError(t('meetingList.toast.deleteFailed'));
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
            {t('meetingList.title')}
          </h1>
          <p className="text-sm text-muted-foreground">
            {t('meetingList.totalMeetings', { count: totalElements })}
          </p>
        </div>
        <div className="flex gap-2 w-full sm:w-auto">
          <Button 
            variant="outline" 
            onClick={() => setShowFilters(!showFilters)}
            className="flex-1 sm:flex-none"
          >
            <Filter className="h-4 w-4 mr-2" />
            {t('meetingList.filtersButton')}
          </Button>
          <Button onClick={() => handleOpenDialog()} className="flex-1 sm:flex-none">
            <Plus className="h-4 w-4 mr-2" />
            {t('meetingList.newMeeting')}
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
                  <Label>{t('meetingList.filters.meetingTypes')}</Label>
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
                      <SelectValue placeholder={t('meetingList.filters.allTypes')} />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="all">{t('meetingList.filters.allTypes')}</SelectItem>
                      {meetingTypes.map((type) => (
                        <SelectItem key={type} value={type}>
                          {formatMeetingType(type)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>{t('meetingList.filters.startDate')}</Label>
                  <LocalizedDateInput
                    value={filters.startDate}
                    onChange={(value) => setFilters({ ...filters, startDate: value })}
                  />
                </div>
                <div className="space-y-2">
                  <Label>{t('meetingList.filters.endDate')}</Label>
                  <LocalizedDateInput
                    value={filters.endDate}
                    onChange={(value) => setFilters({ ...filters, endDate: value })}
                  />
                </div>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="space-y-2">
                  <Label>{t('meetingList.filters.dorReady')}</Label>
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
                      <SelectItem value="all">{t('meetingList.filters.all')}</SelectItem>
                      <SelectItem value="yes">{t('meetingList.filters.yes')}</SelectItem>
                      <SelectItem value="no">{t('meetingList.filters.no')}</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>{t('meetingList.filters.dodReady')}</Label>
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
                      <SelectItem value="all">{t('meetingList.filters.all')}</SelectItem>
                      <SelectItem value="yes">{t('meetingList.filters.yes')}</SelectItem>
                      <SelectItem value="no">{t('meetingList.filters.no')}</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="flex gap-2">
                <Button onClick={applyFilters}>{t('meetingList.filters.apply')}</Button>
                <Button variant="outline" onClick={clearFilters}>
                  <X className="h-4 w-4 mr-2" />
                  {t('meetingList.filters.clear')}
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
              title={t('meetingList.empty.title')}
              description={t('meetingList.empty.description')}
              action={{
                label: t('meetingList.empty.action'),
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
                    <TableHead>{t('meetingList.table.date')}</TableHead>
                    <TableHead>{t('meetingList.table.type')}</TableHead>
                    <TableHead>{t('meetingList.table.pitch')}</TableHead>
                    <TableHead>{t('meetingList.table.attendees')}</TableHead>
                    <TableHead className="text-center">{t('meetingList.table.actions')}</TableHead>
                    <TableHead className="text-center">{t('meetingList.table.dor')}</TableHead>
                    <TableHead className="text-center">{t('meetingList.table.dod')}</TableHead>
                    <TableHead className="text-center">{t('meetingList.table.operations')}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {meetings.map((meeting) => (
                    <TableRow key={meeting.id}>
                      <TableCell className="font-medium">
                        {formatLocalizedDate(meeting.dateHeld, i18n.language)}
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
                                  {t('meetingList.table.actionCount', { count: meeting.actions.length })}
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
                          {meeting.dorReady ? t('meetingList.table.yes') : t('meetingList.table.no')}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-center">
                        <Badge
                          variant={meeting.dodReady ? 'success' : 'outline'}
                        >
                          {meeting.dodReady ? t('meetingList.table.yes') : t('meetingList.table.no')}
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
                              <TooltipContent>{t('meetingList.table.documentsTooltip')}</TooltipContent>
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
                              <TooltipContent>{t('meetingList.table.editTooltip')}</TooltipContent>
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
                              <TooltipContent>{t('meetingList.table.deleteTooltip')}</TooltipContent>
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
                {t('meetingList.pagination.showing', {
                  from: page * size + 1,
                  to: Math.min((page + 1) * size, totalElements),
                  total: totalElements
                })}
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0}
                >
                  {t('meetingList.pagination.previous')}
                </Button>
                <div className="text-sm text-muted-foreground">
                  {t('meetingList.pagination.page', { current: page + 1, total: totalPages })}
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1}
                >
                  {t('meetingList.pagination.next')}
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
            <DialogTitle>{editId ? t('meetingList.dialog.editTitle') : t('meetingList.dialog.newTitle')}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            {/* Basic Info */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="meeting-type">{t('meetingList.dialog.type')} *</Label>
                <Select
                  value={formData.type}
                  onValueChange={(value) => setFormData({ ...formData, type: value as MeetingType })}
                >
                  <SelectTrigger id="meeting-type">
                    <SelectValue placeholder={t('meetingList.dialog.selectType')} />
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
                <Label htmlFor="meeting-date">{t('meetingList.dialog.date')} *</Label>
                <div className="relative">
                  <CalendarDays className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground z-10 pointer-events-none" />
                  <LocalizedDateInput
                    id="meeting-date"
                    value={meetingDate}
                    onChange={(value) => setMeetingDate(value)}
                    className="pl-9"
                    aria-required="true"
                  />
                </div>
              </div>
            </div>

            {/* Pitch & Retrospective */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="meeting-pitch">{t('meetingList.dialog.pitch')}</Label>
                <Select
                  value={formData.pitchId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ ...formData, pitchId: value === 'none' ? undefined : parseInt(value) })}
                >
                  <SelectTrigger id="meeting-pitch">
                    <SelectValue placeholder={t('meetingList.dialog.selectPitch')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">{t('meetingList.dialog.none')}</SelectItem>
                    {pitches.map((p) => (
                      <SelectItem key={p.id} value={p.id.toString()}>
                        {p.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="meeting-retro">{t('meetingList.dialog.retrospective')}</Label>
                <Select
                  value={formData.retrospectiveId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ ...formData, retrospectiveId: value === 'none' ? undefined : parseInt(value) })}
                >
                  <SelectTrigger id="meeting-retro">
                    <SelectValue placeholder={t('meetingList.dialog.selectRetrospective')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">{t('meetingList.dialog.none')}</SelectItem>
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
                <Label htmlFor="dor-ready">{t('meetingList.dialog.dorReady')}</Label>
                <Select
                  value={formData.dorReady ? 'yes' : 'no'}
                  onValueChange={(value) => setFormData({ ...formData, dorReady: value === 'yes' })}
                >
                  <SelectTrigger id="dor-ready">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="yes">{t('meetingList.dialog.yes')}</SelectItem>
                    <SelectItem value="no">{t('meetingList.dialog.no')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="dod-ready">{t('meetingList.dialog.dodReady')}</Label>
                <Select
                  value={formData.dodReady ? 'yes' : 'no'}
                  onValueChange={(value) => setFormData({ ...formData, dodReady: value === 'yes' })}
                >
                  <SelectTrigger id="dod-ready">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="yes">{t('meetingList.dialog.yes')}</SelectItem>
                    <SelectItem value="no">{t('meetingList.dialog.no')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Attendees */}
            <div className="space-y-2">
              <Label htmlFor="meeting-attendees">{t('meetingList.dialog.attendees')}</Label>
              <Input
                id="meeting-attendees"
                value={formData.attendees || ''}
                onChange={(e) => setFormData({ ...formData, attendees: e.target.value })}
                placeholder={t('meetingList.dialog.attendeesPlaceholder')}
              />
              <p className="text-xs text-muted-foreground">{t('meetingList.dialog.commaSeparated')}</p>
            </div>

            {/* Notes */}
            <div className="space-y-2">
              <Label htmlFor="meeting-notes">{t('meetingList.dialog.notes')}</Label>
              <Textarea
                id="meeting-notes"
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                rows={3}
                placeholder={t('meetingList.dialog.notesPlaceholder')}
              />
            </div>

            {/* Decisions */}
            <div className="space-y-2">
              <Label htmlFor="meeting-decisions">{t('meetingList.dialog.decisions')}</Label>
              <Textarea
                id="meeting-decisions"
                value={formData.decisions || ''}
                onChange={(e) => setFormData({ ...formData, decisions: e.target.value })}
                rows={3}
                placeholder={t('meetingList.dialog.decisionsPlaceholder')}
              />
            </div>

            {/* Action Items */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <Label>{t('meetingList.actionItems.label')}</Label>
                <Button type="button" variant="outline" size="sm" onClick={addAction}>
                  <Plus className="h-4 w-4 mr-1" />
                  {t('meetingList.actionItems.add')}
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
                              placeholder={t('meetingList.actionItems.descriptionPlaceholder')}
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
                            <Label className="text-xs">{t('meetingList.actionItems.assignee')}</Label>
                            <Select
                              value={action.assignedToId?.toString() || 'none'}
                              onValueChange={(value) => 
                                updateAction(index, 'assignedToId', value === 'none' ? undefined : parseInt(value))
                              }
                            >
                              <SelectTrigger className="h-9">
                                <SelectValue placeholder={t('meetingList.actionItems.unassigned')} />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="none">{t('meetingList.actionItems.unassigned')}</SelectItem>
                                {persons.map((p) => (
                                  <SelectItem key={p.id} value={p.id.toString()}>
                                    {p.name}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                          
                          <div className="space-y-1">
                            <Label className="text-xs">{t('meetingList.actionItems.status')}</Label>
                            <Select
                              value={action.status}
                              onValueChange={(value) => updateAction(index, 'status', value as ActionStatus)}
                            >
                              <SelectTrigger className="h-9">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="OPEN">{t('meetingList.actionItems.statusOpen')}</SelectItem>
                                <SelectItem value="IN_PROGRESS">{t('meetingList.actionItems.statusInProgress')}</SelectItem>
                                <SelectItem value="COMPLETED">{t('meetingList.actionItems.statusCompleted')}</SelectItem>
                                <SelectItem value="CANCELLED">{t('meetingList.actionItems.statusCancelled')}</SelectItem>
                              </SelectContent>
                            </Select>
                          </div>
                          
                          <div className="space-y-1">
                            <Label className="text-xs">{t('meetingList.actionItems.dueDate')}</Label>
                            <LocalizedDateInput
                              className="h-9"
                              value={action.dueDate || ''}
                              onChange={(value) => updateAction(index, 'dueDate', value || undefined)}
                            />
                          </div>
                        </div>
                        
                        <div className="space-y-1">
                          <Label className="text-xs">{t('meetingList.actionItems.notes')}</Label>
                          <Input
                            placeholder={t('meetingList.actionItems.notesPlaceholder')}
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
                  {t('meetingList.actionItems.empty')}
                </p>
              )}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialog(false)}>
              {t('meetingList.dialog.cancel')}
            </Button>
            <Button onClick={handleSubmit}>
              {editId ? t('meetingList.dialog.update') : t('meetingList.dialog.create')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <QAFloatingButton contextType="meeting" />
    </div>
  );
}
