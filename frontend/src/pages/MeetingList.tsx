import { useEffect, useState } from 'react';
import {
  Plus,
  Pencil,
  Trash2,
  Paperclip,
  Loader2,
  CalendarDays,
} from 'lucide-react';
import dayjs from 'dayjs';
import { meetingService } from '../services/meetingService';
import { pitchService } from '../services/pitchService';
import { Meeting, Pitch, CreateMeetingRequest, MeetingType } from '../types';
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
  const [loading, setLoading] = useState(true);
  const [dialog, setDialog] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [docsDialog, setDocsDialog] = useState<{ open: boolean; meeting: Meeting | null }>({ open: false, meeting: null });

  const [formData, setFormData] = useState<CreateMeetingRequest>({
    pitchId: undefined,
    type: 'STANDUP',
    dateHeld: dayjs().format('YYYY-MM-DD'),
    dorReady: false,
    dodReady: false,
    notes: '',
  });
  const [meetingDate, setMeetingDate] = useState<string>(dayjs().format('YYYY-MM-DD'));

  useEffect(() => {
    const abortController = new AbortController();
    loadData();
    return () => abortController.abort();
  }, []);

  const loadData = async () => {
    try {
      const [meetingsRes, pitchesRes] = await Promise.all([
        meetingService.getAll(),
        pitchService.getAll(),
      ]);
      setMeetings(meetingsRes.data);
      setPitches(pitchesRes.data);
    } catch (error: any) {
      if (error.name !== 'CanceledError') {
        console.error('Failed to load data:', error);
      }
    } finally {
      setLoading(false);
    }
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
        <h1 className="text-2xl font-bold text-foreground">
          Meetings
        </h1>
        <Button onClick={() => handleOpenDialog()} className="w-full sm:w-auto">
          <Plus className="h-4 w-4 mr-2" />
          New Meeting
        </Button>
      </div>

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
                    <TableHead className="text-center">DOR</TableHead>
                    <TableHead className="text-center">DOD</TableHead>
                    <TableHead>Notes</TableHead>
                    <TableHead className="text-center">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {meetings.map((meeting) => (
                    <TableRow key={meeting.id}>
                      <TableCell>{new Date(meeting.dateHeld).toLocaleDateString()}</TableCell>
                      <TableCell>
                        <Badge variant={getMeetingTypeBadgeVariant(meeting.type)}>
                          {formatMeetingType(meeting.type)}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {meeting.pitchTitle || '-'}
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
                      <TableCell className="text-muted-foreground max-w-[200px] truncate">
                        {meeting.notes || '-'}
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
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>{editId ? 'Edit Meeting' : 'New Meeting'}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
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
