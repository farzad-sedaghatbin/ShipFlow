import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import dayjs from 'dayjs';
import { safeParseId } from '../utils/validation';
import {
  Plus,
  Trash2,
  ChevronDown,
  ChevronUp,
  X,
  AlertTriangle,
  Lightbulb,
  Ban,
  Link2,
  Target,
  Edit2,
  Save,
  Loader2,
} from 'lucide-react';
import { pitchService } from '../services/pitchService';
import { workLogService } from '../services/workLogService';
import { meetingService } from '../services/meetingService';
import { personService } from '../services/personService';
import { documentService, UploadedDocument } from '../services/documentService';
import { Pitch, WorkLog, Meeting, Person, CreateWorkLogRequest, CreateMeetingRequest, MeetingType, PitchStatus } from '../types';
import StatusChip from '../components/StatusChip';
import ProgressBar from '../components/ProgressBar';
import RiskInsightsCard from '../components/RiskInsightsCard';
import { PitchDetailSkeleton } from '../components/Skeletons';
import { QAFloatingButton } from '../components/QAFloatingButton';
import { NotesList } from '../components/NotesList';
import { DocumentDropZone } from '../components/DocumentDropZone';
import { useToast } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';
import { cn } from '../lib/utils';

import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Badge } from '../components/ui/badge';
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
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '../components/ui/collapsible';

// Shape Up field editing interface
interface ShapeUpFields {
  problemStatement: string;
  solution: string;
  rabbitHoles: string;
  risks: string;
  noGos: string;
  wireframeLinks: string;
}

export default function PitchDetail() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = safeParseId(idParam);
  const { showSuccess, showError } = useToast();
  const [pitch, setPitch] = useState<Pitch | null>(null);
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [documents, setDocuments] = useState<UploadedDocument[]>([]);
  const [loading, setLoading] = useState(true);
  const [, setSaving] = useState(false);
  
  // Shape Up editing state
  const [editingShapeUp, setEditingShapeUp] = useState(false);
  const [savingShapeUp, setSavingShapeUp] = useState(false);
  const [shapeUpFields, setShapeUpFields] = useState<ShapeUpFields>({
    problemStatement: '',
    solution: '',
    rabbitHoles: '',
    risks: '',
    noGos: '',
    wireframeLinks: '',
  });

  const [workLogDialog, setWorkLogDialog] = useState(false);
  const [meetingDialog, setMeetingDialog] = useState(false);
  const [meetingPendingDocs, setMeetingPendingDocs] = useState<File[]>([]);
  const [showMeetingDocUpload, setShowMeetingDocUpload] = useState(false);
  const [newWorkLog, setNewWorkLog] = useState<CreateWorkLogRequest>({
    personId: 0,
    pitchId: 0,
    date: dayjs().format('YYYY-MM-DD'),
    hoursSpent: 0,
    note: '',
  });
  const [workLogDate, setWorkLogDate] = useState<string>(dayjs().format('YYYY-MM-DD'));
  const [newMeeting, setNewMeeting] = useState<CreateMeetingRequest>({
    pitchId: 0,
    type: 'STANDUP',
    dateHeld: dayjs().format('YYYY-MM-DD'),
    dorReady: false,
    dodReady: false,
    notes: '',
  });
  const [meetingDate, setMeetingDate] = useState<string>(dayjs().format('YYYY-MM-DD'));

  useEffect(() => {
    const abortController = new AbortController();
    if (id) {
      loadData(id);
    }
    return () => abortController.abort();
  }, [id]);

  const loadData = async (pitchId: number) => {
    try {
      const [pitchRes, workLogsRes, meetingsRes, personsData, docsRes] = await Promise.all([
        pitchService.getById(pitchId),
        workLogService.getByPitchId(pitchId),
        meetingService.getByPitchId(pitchId),
        personService.getAll(true),
        documentService.getDocumentsForPitch(pitchId),
      ]);
      const pitchData = pitchRes.data;
      setPitch(pitchData);
      setWorkLogs(workLogsRes.data);
      setMeetings(meetingsRes.data);
      setPersons(personsData);
      setDocuments(docsRes.data);
      
      // Sync Shape Up fields
      setShapeUpFields({
        problemStatement: pitchData.problemStatement || '',
        solution: pitchData.solution || '',
        rabbitHoles: pitchData.rabbitHoles || '',
        risks: pitchData.risks || '',
        noGos: pitchData.noGos || '',
        wireframeLinks: pitchData.wireframeLinks || '',
      });
    } catch (error) {
      console.error('Failed to load pitch:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDocumentDeleted = (docId: number) => {
    setDocuments(prev => prev.filter(d => d.id !== docId));
  };

  const handleStatusChange = async (newStatus: PitchStatus) => {
    if (!pitch) return;
    try {
      await pitchService.updateStatus(pitch.id, newStatus);
      showSuccess('Status updated successfully!');
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to update status'));
    }
  };

  // Save Shape Up fields
  const handleSaveShapeUp = async () => {
    if (!pitch) return;
    try {
      setSavingShapeUp(true);
      await pitchService.update(pitch.id, {
        title: pitch.title,
        description: pitch.description,
        appetiteDays: pitch.appetiteDays,
        cycleId: pitch.cycleId,
        teamId: pitch.teamId,
        status: pitch.status,
        ...shapeUpFields,
      });
      showSuccess('Shape Up details saved successfully!');
      setEditingShapeUp(false);
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to save Shape Up details'));
    } finally {
      setSavingShapeUp(false);
    }
  };

  // Cancel editing and reset fields
  const handleCancelShapeUpEdit = () => {
    if (pitch) {
      setShapeUpFields({
        problemStatement: pitch.problemStatement || '',
        solution: pitch.solution || '',
        rabbitHoles: pitch.rabbitHoles || '',
        risks: pitch.risks || '',
        noGos: pitch.noGos || '',
        wireframeLinks: pitch.wireframeLinks || '',
      });
    }
    setEditingShapeUp(false);
  };

  // Check if pitch has any Shape Up content
  const hasShapeUpContent = pitch && (
    pitch.problemStatement || pitch.solution || pitch.rabbitHoles || 
    pitch.risks || pitch.noGos || pitch.wireframeLinks
  );

  const handleCreateWorkLog = async () => {
    if (!pitch || !workLogDate) return;
    try {
      setSaving(true);
      await workLogService.create({
        ...newWorkLog,
        pitchId: pitch.id,
        date: workLogDate,
      });
      showSuccess('Work log created successfully!');
      setWorkLogDialog(false);
      setNewWorkLog({
        personId: 0,
        pitchId: 0,
        date: dayjs().format('YYYY-MM-DD'),
        hoursSpent: 0,
        note: '',
      });
      setWorkLogDate(dayjs().format('YYYY-MM-DD'));
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to create work log'));
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteWorkLog = async (workLogId: number) => {
    if (!pitch) return;
    try {
      await workLogService.delete(workLogId);
      showSuccess('Work log deleted successfully!');
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to delete work log'));
    }
  };

  const handleCreateMeeting = async () => {
    if (!pitch || !meetingDate) return;
    try {
      setSaving(true);
      const response = await meetingService.create({
        ...newMeeting,
        pitchId: pitch.id,
        dateHeld: meetingDate,
      });
      const createdMeeting = response.data;
      
      // Upload pending documents if any
      if (meetingPendingDocs.length > 0 && createdMeeting.id) {
        for (const file of meetingPendingDocs) {
          try {
            await documentService.uploadForMeeting(createdMeeting.id, file);
          } catch (docError) {
            showError('Some meeting documents failed to upload');
          }
        }
      }
      
      showSuccess('Meeting created successfully!');
      setMeetingDialog(false);
      setNewMeeting({
        pitchId: 0,
        type: 'STANDUP',
        dateHeld: dayjs().format('YYYY-MM-DD'),
        dorReady: false,
        dodReady: false,
        notes: '',
      });
      setMeetingDate(dayjs().format('YYYY-MM-DD'));
      setMeetingPendingDocs([]);
      setShowMeetingDocUpload(false);
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to create meeting'));
    } finally {
      setSaving(false);
    }
  };

  const handleMeetingPendingFileSelect = (files: FileList) => {
    setMeetingPendingDocs(prev => [...prev, ...Array.from(files)]);
  };

  const handleRemoveMeetingPendingDoc = (index: number) => {
    setMeetingPendingDocs(prev => prev.filter((_, i) => i !== index));
  };

  if (id === null) {
    return (
      <div className="p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <span className="text-sm">Invalid pitch ID</span>
        </div>
      </div>
    );
  }

  if (loading) {
    return <PitchDetailSkeleton />;
  }

  if (!pitch) {
    return (
      <div>
        <p className="text-muted-foreground">Pitch not found</p>
        <Button variant="link" asChild className="px-0">
          <Link to="/pitches">Back to Pitches</Link>
        </Button>
      </div>
    );
  }

  const totalHours = workLogs.reduce((sum, wl) => sum + wl.hoursSpent, 0);

  return (
    <div>
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-stretch md:items-start gap-4 mb-8">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            {pitch.title}
          </h1>
          <p className="text-muted-foreground mb-1">
            {pitch.teamName || 'Unassigned'} • {pitch.cycleName}
          </p>
          {pitch.description && (
            <p className="text-muted-foreground mt-4">
              {pitch.description}
            </p>
          )}
        </div>
        <div className="flex gap-2 items-center flex-wrap">
          <Button variant="outline" size="sm" asChild>
            <Link to={`/pitches/${pitch.id}/hill-chart`}>Hill Chart</Link>
          </Button>
          <StatusChip status={pitch.status} size="medium" />
          <Select
            value={pitch.status}
            onValueChange={(value) => handleStatusChange(value as PitchStatus)}
          >
            <SelectTrigger className="w-[150px]">
              <SelectValue placeholder="Status" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="PENDING">Pending</SelectItem>
              <SelectItem value="STARTED">Started</SelectItem>
              <SelectItem value="IN_PROGRESS">In Progress</SelectItem>
              <SelectItem value="TESTING">Testing</SelectItem>
              <SelectItem value="DONE">Done</SelectItem>
              <SelectItem value="COOLDOWN">Cooldown</SelectItem>
              <SelectItem value="CANCELLED">Cancelled</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground mb-1">Appetite</p>
            <p className="text-3xl font-bold">{pitch.appetiteDays} days</p>
            <p className="text-sm text-muted-foreground">
              ({pitch.appetiteHours?.toFixed(0)} hours)
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground mb-1">Actual Time</p>
            <p
              className={cn(
                'text-3xl font-bold',
                totalHours > (pitch.appetiteHours || 0)
                  ? 'text-destructive'
                  : 'text-success'
              )}
            >
              {totalHours.toFixed(1)}h
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground mb-1">Progress</p>
            <p className="text-3xl font-bold">
              {pitch.progressPercentage?.toFixed(0) || 0}%
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground mb-1">Work Entries</p>
            <p className="text-3xl font-bold">{workLogs.length}</p>
          </CardContent>
        </Card>
      </div>

      {/* Progress Bar */}
      <div className="mb-6">
        <ProgressBar
          value={pitch.progressPercentage || 0}
          label="Budget Progress"
          color={(pitch.progressPercentage || 0) > 100 ? 'error' : 'primary'}
        />
      </div>

      {/* Shape Up Narrative Section */}
      <Card className="mb-6">
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle className="flex items-center gap-2">
              <Target className="h-5 w-5 text-primary" />
              Shape Up Details
            </CardTitle>
            {!editingShapeUp ? (
              <Button variant="outline" size="sm" onClick={() => setEditingShapeUp(true)}>
                <Edit2 className="h-4 w-4 mr-1" />
                Edit
              </Button>
            ) : (
              <div className="flex gap-2">
                <Button variant="outline" size="sm" onClick={handleCancelShapeUpEdit} disabled={savingShapeUp}>
                  Cancel
                </Button>
                <Button size="sm" onClick={handleSaveShapeUp} disabled={savingShapeUp}>
                  {savingShapeUp ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Save className="h-4 w-4 mr-1" />}
                  Save
                </Button>
              </div>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {editingShapeUp ? (
            // Edit Mode
            <div className="space-y-4">
              {/* Problem Statement */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-orange-500" />
                  Problem Statement
                </Label>
                <Textarea
                  value={shapeUpFields.problemStatement}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, problemStatement: e.target.value }))}
                  placeholder="What problem are we solving? Why does this matter?"
                  rows={3}
                />
              </div>

              {/* Solution */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <Lightbulb className="h-4 w-4 text-yellow-500" />
                  Solution (Hatched)
                </Label>
                <Textarea
                  value={shapeUpFields.solution}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, solution: e.target.value }))}
                  placeholder="The proposed solution and approach."
                  rows={4}
                />
              </div>

              {/* Rabbit Holes */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <Ban className="h-4 w-4 text-red-500" />
                  Rabbit Holes
                </Label>
                <Textarea
                  value={shapeUpFields.rabbitHoles}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, rabbitHoles: e.target.value }))}
                  placeholder="Edge cases to avoid, potential time sinks."
                  rows={3}
                />
              </div>

              {/* Risks */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-amber-500" />
                  Risks & Unknowns
                </Label>
                <Textarea
                  value={shapeUpFields.risks}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, risks: e.target.value }))}
                  placeholder="Known risks, technical challenges, or unknowns."
                  rows={3}
                />
              </div>

              {/* No-Gos */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <X className="h-4 w-4 text-red-500" />
                  No-Gos (Out of Scope)
                </Label>
                <Textarea
                  value={shapeUpFields.noGos}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, noGos: e.target.value }))}
                  placeholder="Things explicitly NOT being built."
                  rows={2}
                />
              </div>

              {/* Wireframe Links */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <Link2 className="h-4 w-4 text-blue-500" />
                  Wireframe / Prototype Links
                </Label>
                <Textarea
                  value={shapeUpFields.wireframeLinks}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, wireframeLinks: e.target.value }))}
                  placeholder="Links to Figma, wireframes, mockups (one per line)"
                  rows={2}
                />
              </div>
            </div>
          ) : hasShapeUpContent ? (
            // Display Mode with content
            <div className="space-y-6">
              {pitch.problemStatement && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <AlertTriangle className="h-4 w-4 text-orange-500" />
                    Problem Statement
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.problemStatement}</p>
                </div>
              )}

              {pitch.solution && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <Lightbulb className="h-4 w-4 text-yellow-500" />
                    Solution
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.solution}</p>
                </div>
              )}

              {pitch.rabbitHoles && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <Ban className="h-4 w-4 text-red-500" />
                    Rabbit Holes
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.rabbitHoles}</p>
                </div>
              )}

              {pitch.risks && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <AlertTriangle className="h-4 w-4 text-amber-500" />
                    Risks & Unknowns
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.risks}</p>
                </div>
              )}

              {pitch.noGos && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <X className="h-4 w-4 text-red-500" />
                    No-Gos
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.noGos}</p>
                </div>
              )}

              {pitch.wireframeLinks && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <Link2 className="h-4 w-4 text-blue-500" />
                    Wireframe Links
                  </h4>
                  <div className="space-y-1">
                    {pitch.wireframeLinks.split('\n').map((link, idx) => {
                      const trimmedLink = link.trim();
                      if (!trimmedLink) return null;
                      const isUrl = trimmedLink.startsWith('http://') || trimmedLink.startsWith('https://');
                      return (
                        <p key={idx}>
                          {isUrl ? (
                            <a 
                              href={trimmedLink} 
                              target="_blank" 
                              rel="noopener noreferrer"
                              className="text-primary hover:underline"
                            >
                              {trimmedLink}
                            </a>
                          ) : (
                            <span className="text-muted-foreground">{trimmedLink}</span>
                          )}
                        </p>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          ) : (
            // Empty state
            <div className="text-center py-8">
              <Target className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
              <p className="text-muted-foreground mb-4">
                No Shape Up details defined yet. Add problem statement, solution, rabbit holes, and risks to fully shape this pitch.
              </p>
              <Button variant="outline" onClick={() => setEditingShapeUp(true)}>
                <Edit2 className="h-4 w-4 mr-2" />
                Add Shape Up Details
              </Button>
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-6">
        {/* Risk Analysis - Full Width */}
        <RiskInsightsCard pitchId={pitch.id} />

        {/* Documents Section */}
        <Card>
          <CardHeader>
            <CardTitle>Documents</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground mb-4">
              Upload documents (PDF, DOCX, TXT) to add their content to the knowledge base for Q&A.
            </p>
            <DocumentDropZone
              entityType="PITCH"
              entityId={pitch.id}
              existingDocuments={documents}
              onDocumentDeleted={handleDocumentDeleted}
              onUploadComplete={() => loadData(pitch.id)}
            />
          </CardContent>
        </Card>

        {/* Notes Section */}
        <NotesList 
          contextType="pitch" 
          contextId={pitch.id} 
          title="Pitch Notes"
        />

        {/* Work Logs and Meetings - Two columns on desktop */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Work Logs */}
          <Card>
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>Work Logs</CardTitle>
                <Button size="sm" onClick={() => setWorkLogDialog(true)}>
                  <Plus className="h-4 w-4 mr-1" />
                  Add
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {workLogs.length === 0 ? (
                <p className="text-muted-foreground">No work logs yet</p>
              ) : (
                <div className="space-y-1">
                  {workLogs.map((wl, index) => (
                    <div key={wl.id}>
                      <div className="flex items-start justify-between py-3">
                        <div className="flex-1 pr-4">
                          <div className="flex justify-between items-center mb-1">
                            <span className="font-medium">{wl.personName}</span>
                            <Badge variant="secondary">{wl.hoursSpent}h</Badge>
                          </div>
                          <p className="text-sm text-muted-foreground">
                            {new Date(wl.date).toLocaleDateString()}
                            {wl.note && ` • ${wl.note}`}
                          </p>
                        </div>
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          onClick={() => handleDeleteWorkLog(wl.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                      {index < workLogs.length - 1 && (
                        <div className="border-b border-border" />
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Meetings */}
          <Card>
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>Meetings</CardTitle>
                <Button size="sm" onClick={() => setMeetingDialog(true)}>
                  <Plus className="h-4 w-4 mr-1" />
                  Add
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {meetings.length === 0 ? (
                <p className="text-muted-foreground">No meetings yet</p>
              ) : (
                <div className="space-y-1">
                  {meetings.map((m, index) => (
                    <div key={m.id}>
                      <div className="py-3">
                        <div className="flex gap-2 items-center mb-2">
                          <Badge variant="outline">{m.type}</Badge>
                          <span className="text-sm text-muted-foreground">
                            {new Date(m.dateHeld).toLocaleDateString()}
                          </span>
                        </div>
                        <div className="flex gap-2 mb-2">
                          <Badge
                            variant={m.dorReady ? 'success' : 'outline'}
                            className={cn(!m.dorReady && 'text-muted-foreground')}
                          >
                            DOR
                          </Badge>
                          <Badge
                            variant={m.dodReady ? 'success' : 'outline'}
                            className={cn(!m.dodReady && 'text-muted-foreground')}
                          >
                            DOD
                          </Badge>
                        </div>
                        {m.notes && (
                          <p className="text-sm text-muted-foreground">{m.notes}</p>
                        )}
                      </div>
                      {index < meetings.length - 1 && (
                        <div className="border-b border-border" />
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Add Work Log Dialog */}
      <Dialog open={workLogDialog} onOpenChange={setWorkLogDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Add Work Log</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="person">Person *</Label>
              <Select
                value={newWorkLog.personId ? String(newWorkLog.personId) : ''}
                onValueChange={(value) =>
                  setNewWorkLog({ ...newWorkLog, personId: parseInt(value) })
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select person" />
                </SelectTrigger>
                <SelectContent>
                  {persons.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.name} ({p.email || 'no email'})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="worklog-date">Date *</Label>
                <Input
                  id="worklog-date"
                  type="date"
                  value={workLogDate}
                  onChange={(e) => setWorkLogDate(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="hours">Hours *</Label>
                <Input
                  id="hours"
                  type="number"
                  value={newWorkLog.hoursSpent || ''}
                  onChange={(e) =>
                    setNewWorkLog({
                      ...newWorkLog,
                      hoursSpent: parseFloat(e.target.value) || 0,
                    })
                  }
                  min={0.25}
                  step={0.25}
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="note">Note</Label>
              <Textarea
                id="note"
                value={newWorkLog.note}
                onChange={(e) =>
                  setNewWorkLog({ ...newWorkLog, note: e.target.value })
                }
                rows={2}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setWorkLogDialog(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleCreateWorkLog}
              disabled={!newWorkLog.personId || !newWorkLog.hoursSpent}
            >
              Add
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Add Meeting Dialog */}
      <Dialog open={meetingDialog} onOpenChange={setMeetingDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Add Meeting</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="meeting-type">Type *</Label>
                <Select
                  value={newMeeting.type}
                  onValueChange={(value) =>
                    setNewMeeting({ ...newMeeting, type: value as MeetingType })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select type" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="SHAPING">Shaping</SelectItem>
                    <SelectItem value="BETTING">Betting</SelectItem>
                    <SelectItem value="KICKOFF">Kickoff</SelectItem>
                    <SelectItem value="STANDUP">Standup</SelectItem>
                    <SelectItem value="DEMO">Demo</SelectItem>
                    <SelectItem value="RETROSPECTIVE">Retrospective</SelectItem>
                    <SelectItem value="HILL_CHART_REVIEW">Hill Chart Review</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="meeting-date">Date *</Label>
                <Input
                  id="meeting-date"
                  type="date"
                  value={meetingDate}
                  onChange={(e) => setMeetingDate(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="dor">DOR Ready</Label>
              <Select
                value={newMeeting.dorReady ? 'yes' : 'no'}
                onValueChange={(value) =>
                  setNewMeeting({ ...newMeeting, dorReady: value === 'yes' })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="yes">Yes</SelectItem>
                  <SelectItem value="no">No</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="dod">DOD Ready</Label>
              <Select
                value={newMeeting.dodReady ? 'yes' : 'no'}
                onValueChange={(value) =>
                  setNewMeeting({ ...newMeeting, dodReady: value === 'yes' })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="yes">Yes</SelectItem>
                  <SelectItem value="no">No</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="meeting-notes">Notes</Label>
              <Textarea
                id="meeting-notes"
                value={newMeeting.notes}
                onChange={(e) =>
                  setNewMeeting({ ...newMeeting, notes: e.target.value })
                }
                rows={2}
              />
            </div>
            <div>
              <Collapsible
                open={showMeetingDocUpload}
                onOpenChange={setShowMeetingDocUpload}
              >
                <CollapsibleTrigger asChild>
                  <Button variant="ghost" size="sm" className="p-0">
                    {showMeetingDocUpload ? (
                      <ChevronUp className="h-4 w-4 mr-1" />
                    ) : (
                      <ChevronDown className="h-4 w-4 mr-1" />
                    )}
                    {showMeetingDocUpload ? 'Hide' : 'Add'} Documents (MOM, etc.)
                  </Button>
                </CollapsibleTrigger>
                <CollapsibleContent className="mt-3">
                  <p className="text-sm text-muted-foreground mb-2">
                    Attach meeting documents (PDF, Word, Text) to be indexed for Q&A
                  </p>
                  <div
                    className="border-2 border-dashed border-border rounded-md p-4 text-center cursor-pointer hover:border-primary hover:bg-accent transition-colors"
                    onClick={() =>
                      document.getElementById('meeting-doc-upload')?.click()
                    }
                    onDragOver={(e) => e.preventDefault()}
                    onDrop={(e) => {
                      e.preventDefault();
                      if (e.dataTransfer.files.length > 0) {
                        handleMeetingPendingFileSelect(e.dataTransfer.files);
                      }
                    }}
                  >
                    <input
                      id="meeting-doc-upload"
                      type="file"
                      hidden
                      multiple
                      accept=".pdf,.doc,.docx,.txt,.md"
                      onChange={(e) =>
                        e.target.files &&
                        handleMeetingPendingFileSelect(e.target.files)
                      }
                    />
                    <p className="text-muted-foreground">
                      Drop files here or click to select
                    </p>
                  </div>
                  {meetingPendingDocs.length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-1">
                      {meetingPendingDocs.map((file, index) => (
                        <Badge
                          key={index}
                          variant="secondary"
                          className="pr-1 gap-1"
                        >
                          {file.name}
                          <button
                            type="button"
                            onClick={() => handleRemoveMeetingPendingDoc(index)}
                            className="ml-1 rounded-full hover:bg-muted p-0.5"
                          >
                            <X className="h-3 w-3" />
                          </button>
                        </Badge>
                      ))}
                    </div>
                  )}
                </CollapsibleContent>
              </Collapsible>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setMeetingDialog(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreateMeeting}>Add</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Q&A Floating Button */}
      <QAFloatingButton
        contextType="pitch"
        contextId={pitch.id}
        contextName={pitch.title}
        cycleId={pitch.cycleId}
        teamId={pitch.teamId}
      />
    </div>
  );
}
