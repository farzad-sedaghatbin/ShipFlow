import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { LocalizedDateInput } from '../components/LocalizedDateInput';
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
  History,
} from 'lucide-react';
import { pitchService } from '../services/pitchService';
import { workLogService } from '../services/workLogService';
import { meetingService } from '../services/meetingService';
import { documentService, UploadedDocument } from '../services/documentService';
import { organizationSettingsService } from '../services/organizationSettingsService';
import { Pitch, WorkLog, Meeting, CreateWorkLogForSelfRequest, CreateMeetingRequest, MeetingType, PitchStatus, MeetingChecklistItem } from '../types';
import { MeetingTypeConfig } from '../types/organizationSettings';
import StatusChip from '../components/StatusChip';
import ProgressBar from '../components/ProgressBar';
import RiskInsightsCard from '../components/RiskInsightsCard';
import { PitchDetailSkeleton } from '../components/Skeletons';
import { QAFloatingButton } from '../components/QAFloatingButton';
import { NotesList } from '../components/NotesList';
import { DocumentDropZone } from '../components/DocumentDropZone';
import { SoftDeleteButton } from '../components/SoftDeleteButton';
import { EntityHistoryDialog } from '../components/EntityHistoryDialog';
import { useToast } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';
import { cn } from '../lib/utils';

import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Badge } from '../components/ui/badge';
import { Checkbox } from '../components/ui/checkbox';
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
  const { t, i18n } = useTranslation();
  const { id: idParam } = useParams<{ id: string }>();
  const id = safeParseId(idParam);
  const { showSuccess, showError } = useToast();
  const [pitch, setPitch] = useState<Pitch | null>(null);
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [documents, setDocuments] = useState<UploadedDocument[]>([]);
  const [meetingTypeConfigs, setMeetingTypeConfigs] = useState<MeetingTypeConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [, setSaving] = useState(false);
  const [historyDialogOpen, setHistoryDialogOpen] = useState(false);
  
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
  const [viewMeetingDialog, setViewMeetingDialog] = useState(false);
  const [viewMeeting, setViewMeeting] = useState<Meeting | null>(null);
  const [meetingPendingDocs, setMeetingPendingDocs] = useState<File[]>([]);
  const [showMeetingDocUpload, setShowMeetingDocUpload] = useState(false);
  const [newWorkLog, setNewWorkLog] = useState<CreateWorkLogForSelfRequest>({
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
    dorItems: [],
    dodItems: [],
    notes: '',
  });
  const [meetingDate, setMeetingDate] = useState<string>(dayjs().format('YYYY-MM-DD'));

  // Get meeting type display name from configurations or fallback to formatted name
  const getMeetingTypeDisplayName = (type: MeetingType): string => {
    const config = meetingTypeConfigs.find(mt => mt.name.toLowerCase() === type.toLowerCase());
    return config?.displayName || type.replace(/_/g, ' ');
  };

  useEffect(() => {
    const abortController = new AbortController();
    if (id) {
      loadData(id);
    }
    return () => abortController.abort();
  }, [id]);

  const loadData = async (pitchId: number) => {
    try {
      const [pitchRes, workLogsRes, meetingsRes, docsRes, orgSettingsRes] = await Promise.all([
        pitchService.getById(pitchId),
        workLogService.getByPitchId(pitchId),
        meetingService.getByPitchId(pitchId),
        documentService.getDocumentsForPitch(pitchId),
        organizationSettingsService.getSettings(),
      ]);
      const pitchData = pitchRes.data;
      setPitch(pitchData);
      setWorkLogs(workLogsRes.data);
      setMeetings(meetingsRes.data);
      setDocuments(docsRes.data);
      setMeetingTypeConfigs(orgSettingsRes.data.meetingTypes || []);
      
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
      showSuccess(t('pitchDetailPage.statusUpdated'));
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchDetailPage.statusUpdateFailed')));
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
      showSuccess(t('pitchDetailPage.shapeUpSaved'));
      setEditingShapeUp(false);
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchDetailPage.saveFailed')));
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
      await workLogService.createMy({
        ...newWorkLog,
        pitchId: pitch.id,
        date: workLogDate,
      });
      showSuccess(t('pitchDetailPage.workLogAdded'));
      setWorkLogDialog(false);
      setNewWorkLog({
        pitchId: 0,
        date: dayjs().format('YYYY-MM-DD'),
        hoursSpent: 0,
        note: '',
      });
      setWorkLogDate(dayjs().format('YYYY-MM-DD'));
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchDetailPage.workLogFailed')));
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteWorkLog = async (workLogId: number) => {
    if (!pitch) return;
    try {
      await workLogService.delete(workLogId);
      showSuccess(t('pitchDetailPage.workLogDeleted'));
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchDetailPage.workLogDeleteFailed')));
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
            showError(t('pitchDetailPage.documentUploadFailed'));
          }
        }
      }
      
      showSuccess(t('pitchDetailPage.meetingScheduled'));
      setMeetingDialog(false);
      setNewMeeting({
        pitchId: 0,
        type: 'STANDUP',
        dateHeld: dayjs().format('YYYY-MM-DD'),
        dorReady: false,
        dodReady: false,
        dorItems: [],
        dodItems: [],
        notes: '',
      });
      setMeetingDate(dayjs().format('YYYY-MM-DD'));
      setMeetingPendingDocs([]);
      setShowMeetingDocUpload(false);
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchDetailPage.meetingFailed')));
    } finally {
      setSaving(false);
    }
  };

  const handleViewMeeting = async (meetingId: number) => {
    try {
      const response = await meetingService.getByIdForView(meetingId);
      setViewMeeting(response.data);
      setViewMeetingDialog(true);
    } catch (error) {
      console.error('Failed to load meeting for view:', error);
      showError(t('pitchDetailPage.error'));
    }
  };

  const handleMeetingPendingFileSelect = (files: FileList) => {
    setMeetingPendingDocs(prev => [...prev, ...Array.from(files)]);
  };

  const handleRemoveMeetingPendingDoc = (index: number) => {
    setMeetingPendingDocs(prev => prev.filter((_, i) => i !== index));
  };

  // Helper function to map checklist items from config to meeting format
  const mapChecklistItems = (items?: typeof meetingTypeConfigs[0]['dorItems']): MeetingChecklistItem[] => {
    return items?.map((item, index) => ({
      id: item.id ?? index + 1,
      name: item.name,
      description: item.description || '',
      isRequired: item.isRequired,
      isCompleted: false,
    })) || [];
  };

  const handleMeetingTypeChange = (type: MeetingType) => {
    const config = meetingTypeConfigs.find(c => c.name === type);
    const dorItems = mapChecklistItems(config?.dorItems);
    const dodItems = mapChecklistItems(config?.dodItems);

    setNewMeeting(prev => ({
      ...prev,
      type,
      dorItems,
      dodItems,
      dorReady: dorItems.length === 0 || !dorItems.some(item => item.isRequired),
      dodReady: dodItems.length === 0 || !dodItems.some(item => item.isRequired),
    }));
  };

  const resetMeetingForm = () => {
    const defaultType = 'STANDUP';
    const config = meetingTypeConfigs.find(c => c.name === defaultType);
    const dorItems = mapChecklistItems(config?.dorItems);
    const dodItems = mapChecklistItems(config?.dodItems);

    setNewMeeting({
      pitchId: pitch?.id || 0,
      type: defaultType,
      dateHeld: dayjs().format('YYYY-MM-DD'),
      dorReady: dorItems.length === 0 || !dorItems.some(item => item.isRequired),
      dodReady: dodItems.length === 0 || !dodItems.some(item => item.isRequired),
      dorItems,
      dodItems,
      notes: '',
    });
    setMeetingDate(dayjs().format('YYYY-MM-DD'));
    setMeetingPendingDocs([]);
    setShowMeetingDocUpload(false);
  };

  const handleOpenMeetingDialog = () => {
    resetMeetingForm();
    setMeetingDialog(true);
  };

  /**
   * Toggle the completion status of a checklist item.
   * @param listType - 'dor' or 'dod' to specify which checklist
   * @param itemId - The unique ID of the item to toggle
   */
  const toggleChecklistItem = (listType: 'dor' | 'dod', itemId: number) => {
    const items = listType === 'dor' ? [...(newMeeting.dorItems || [])] : [...(newMeeting.dodItems || [])];
    const itemIndex = items.findIndex(i => i.id === itemId);
    if (itemIndex >= 0) {
      items[itemIndex] = { ...items[itemIndex], isCompleted: !items[itemIndex].isCompleted };
      
      // Check if all required items are completed
      const allRequiredCompleted = items.filter(i => i.isRequired).every(i => i.isCompleted);
      
      if (listType === 'dor') {
        setNewMeeting(prev => ({ ...prev, dorItems: items, dorReady: allRequiredCompleted }));
      } else {
        setNewMeeting(prev => ({ ...prev, dodItems: items, dodReady: allRequiredCompleted }));
      }
    }
  };

  if (id === null) {
    return (
      <div className="p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <span className="text-sm">{t('pitchDetailPage.invalidPitchId')}</span>
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
        <p className="text-muted-foreground">{t('pitchDetailPage.pitchNotFound')}</p>
        <Button variant="link" asChild className="px-0">
          <Link to="/pitches">{t('pitchDetailPage.backToPitches')}</Link>
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
            {pitch.teamName || t('common.unassigned')} • {pitch.cycleName}
          </p>
          {pitch.description && (
            <p className="text-muted-foreground mt-4">
              {pitch.description}
            </p>
          )}
        </div>
        <div className="flex gap-2 items-center flex-wrap">
          <Button variant="outline" size="sm" asChild>
            <Link to={`/pitches/${pitch.id}/hill-chart`}>{t('pitchDetailPage.hillChart')}</Link>
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setHistoryDialogOpen(true)}
          >
            <History className="h-4 w-4 mr-2" />
            {t('history.viewHistory')}
          </Button>
          <SoftDeleteButton
            entityType="pitch"
            entityId={pitch.id}
            entityTitle={pitch.title}
            onSuccess={() => {
              // Navigate back to pitches list after successful deletion
              window.location.href = '/pitches';
            }}
            variant="outline"
            size="sm"
          />
          <StatusChip status={pitch.status} size="medium" />
          <Select
            value={pitch.status}
            onValueChange={(value) => handleStatusChange(value as PitchStatus)}
          >
            <SelectTrigger className="w-[150px]">
              <SelectValue placeholder={t('pitchDetailPage.status')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="PENDING">{t('status.pending')}</SelectItem>
              <SelectItem value="STARTED">{t('status.started')}</SelectItem>
              <SelectItem value="IN_PROGRESS">{t('status.inProgress')}</SelectItem>
              <SelectItem value="TESTING">{t('status.testing')}</SelectItem>
              <SelectItem value="DONE">{t('status.done')}</SelectItem>
              <SelectItem value="COOLDOWN">{t('status.cooldown')}</SelectItem>
              <SelectItem value="CANCELLED">{t('status.cancelled')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground mb-1">{t('pitchDetailPage.appetite')}</p>
            <p className="text-3xl font-bold">{pitch.appetiteDays} {t('common.days')}</p>
            <p className="text-sm text-muted-foreground">
              ({pitch.appetiteHours?.toFixed(0)} hours)
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground mb-1">{t('pitchDetailPage.actualHours')}</p>
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
            <p className="text-sm text-muted-foreground mb-1">{t('dashboard.progress')}</p>
            <p className="text-3xl font-bold">
              {pitch.progressPercentage?.toFixed(0) || 0}%
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground mb-1">{t('pitchDetailPage.workLogs')}</p>
            <p className="text-3xl font-bold">{workLogs.length}</p>
          </CardContent>
        </Card>
      </div>

      {/* Progress Bar */}
      <div className="mb-6">
        <ProgressBar
          value={pitch.progressPercentage || 0}
          label={t('dashboard.budgetProgress')}
          color={(pitch.progressPercentage || 0) > 100 ? 'error' : 'primary'}
        />
      </div>

      {/* Shape Up Narrative Section */}
      <Card className="mb-6">
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle className="flex items-center gap-2">
              <Target className="h-5 w-5 text-primary" />
              {t('pitchDetailPage.shapeUpDetails')}
            </CardTitle>
            {!editingShapeUp ? (
              <Button variant="outline" size="sm" onClick={() => setEditingShapeUp(true)}>
                <Edit2 className="h-4 w-4 mr-1" />
                {t('pitchDetailPage.editShapeUp')}
              </Button>
            ) : (
              <div className="flex gap-2">
                <Button variant="outline" size="sm" onClick={handleCancelShapeUpEdit} disabled={savingShapeUp}>
                  {t('pitchDetailPage.cancelEdit')}
                </Button>
                <Button size="sm" onClick={handleSaveShapeUp} disabled={savingShapeUp}>
                  {savingShapeUp ? <Loader2 className="h-4 w-4 mr-1 animate-spin" /> : <Save className="h-4 w-4 mr-1" />}
                  {savingShapeUp ? t('pitchDetailPage.saving') : t('pitchDetailPage.saveShapeUp')}
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
                  {t('pitchDetailPage.problemStatement')}
                </Label>
                <Textarea
                  value={shapeUpFields.problemStatement}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, problemStatement: e.target.value }))}
                  placeholder={t('pitchDetailPage.problemPlaceholder')}
                  rows={3}
                />
              </div>

              {/* Solution */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <Lightbulb className="h-4 w-4 text-yellow-500" />
                  {t('pitchDetailPage.solution')}
                </Label>
                <Textarea
                  value={shapeUpFields.solution}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, solution: e.target.value }))}
                  placeholder={t('pitchDetailPage.solutionPlaceholder')}
                  rows={4}
                />
              </div>

              {/* Rabbit Holes */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <Ban className="h-4 w-4 text-red-500" />
                  {t('pitchDetailPage.rabbitHoles')}
                </Label>
                <Textarea
                  value={shapeUpFields.rabbitHoles}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, rabbitHoles: e.target.value }))}
                  placeholder={t('pitchDetailPage.rabbitHolesPlaceholder')}
                  rows={3}
                />
              </div>

              {/* Risks */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-amber-500" />
                  {t('pitchDetailPage.risks')}
                </Label>
                <Textarea
                  value={shapeUpFields.risks}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, risks: e.target.value }))}
                  placeholder={t('pitchDetailPage.risksPlaceholder')}
                  rows={3}
                />
              </div>

              {/* No-Gos */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <X className="h-4 w-4 text-red-500" />
                  {t('pitchDetailPage.noGos')}
                </Label>
                <Textarea
                  value={shapeUpFields.noGos}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, noGos: e.target.value }))}
                  placeholder={t('pitchDetailPage.noGosPlaceholder')}
                  rows={2}
                />
              </div>

              {/* Wireframe Links */}
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  <Link2 className="h-4 w-4 text-blue-500" />
                  {t('pitchDetailPage.wireframeLinks')}
                </Label>
                <Textarea
                  value={shapeUpFields.wireframeLinks}
                  onChange={(e) => setShapeUpFields(prev => ({ ...prev, wireframeLinks: e.target.value }))}
                  placeholder={t('pitchDetailPage.wireframeLinksPlaceholder')}
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
                    {t('pitchDetailPage.problemStatement')}
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.problemStatement}</p>
                </div>
              )}

              {pitch.solution && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <Lightbulb className="h-4 w-4 text-yellow-500" />
                    {t('pitchDetailPage.solution')}
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.solution}</p>
                </div>
              )}

              {pitch.rabbitHoles && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <Ban className="h-4 w-4 text-red-500" />
                    {t('pitchDetailPage.rabbitHoles')}
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.rabbitHoles}</p>
                </div>
              )}

              {pitch.risks && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <AlertTriangle className="h-4 w-4 text-amber-500" />
                    {t('pitchDetailPage.risks')}
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.risks}</p>
                </div>
              )}

              {pitch.noGos && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <X className="h-4 w-4 text-red-500" />
                    {t('pitchDetailPage.noGos')}
                  </h4>
                  <p className="text-muted-foreground whitespace-pre-wrap">{pitch.noGos}</p>
                </div>
              )}

              {pitch.wireframeLinks && (
                <div>
                  <h4 className="font-semibold flex items-center gap-2 mb-2">
                    <Link2 className="h-4 w-4 text-blue-500" />
                    {t('pitchDetailPage.wireframeLinks')}
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
                {t('pitchDetailPage.noShapeUpDetails')}
              </p>
              <Button variant="outline" onClick={() => setEditingShapeUp(true)}>
                <Edit2 className="h-4 w-4 mr-2" />
                {t('pitchDetailPage.addShapeUpDetails')}
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
            <CardTitle>{t('pitchDetailPage.documents')}</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground mb-4">
              {t('pitchDetailPage.documentsDescription')}
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
          title={t('pitchDetailPage.notes')}
        />

        {/* Work Logs and Meetings - Two columns on desktop */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Work Logs */}
          <Card>
            <CardHeader>
              <div className="flex justify-between items-center">
                <CardTitle>{t('pitchDetailPage.workLogs')}</CardTitle>
                <Button size="sm" onClick={() => setWorkLogDialog(true)}>
                  <Plus className="h-4 w-4 mr-1" />
                  {t('pitchDetailPage.add')}
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {workLogs.length === 0 ? (
                <p className="text-muted-foreground">{t('pitchDetailPage.noWorkLogs')}</p>
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
                            {formatLocalizedDate(new Date(wl.date), i18n.language)}
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
                <CardTitle>{t('pitchDetailPage.meetings')}</CardTitle>
                <Button size="sm" onClick={handleOpenMeetingDialog}>
                  <Plus className="h-4 w-4 mr-1" />
                  {t('pitchDetailPage.add')}
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {meetings.length === 0 ? (
                <p className="text-muted-foreground">{t('pitchDetailPage.noMeetings')}</p>
              ) : (
                <div className="space-y-1">
                  {[...meetings].sort((a, b) => new Date(b.dateHeld).getTime() - new Date(a.dateHeld).getTime()).map((m, index) => (
                    <div key={m.id}>
                      <div className="py-3">
                        <div className="flex gap-2 items-center mb-2">
                          <Badge 
                            variant="outline"
                            className="cursor-pointer hover:opacity-80"
                            onClick={() => handleViewMeeting(m.id)}
                          >
                            {getMeetingTypeDisplayName(m.type)}
                          </Badge>
                          <span className="text-sm text-muted-foreground">
                            {formatLocalizedDate(new Date(m.dateHeld), i18n.language)}
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
            <DialogTitle>{t('pitchDetailPage.addWorkLog')}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="worklog-date">{t('pitchDetailPage.date')} *</Label>
                <LocalizedDateInput
                  id="worklog-date"
                  value={workLogDate}
                  onChange={setWorkLogDate}
                  aria-required="true"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="hours">{t('pitchDetailPage.hours')} *</Label>
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
              <Label htmlFor="note">{t('pitchDetailPage.note')}</Label>
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
              {t('pitchDetailPage.cancel')}
            </Button>
            <Button
              onClick={handleCreateWorkLog}
              disabled={!newWorkLog.hoursSpent}
            >
              {t('pitchDetailPage.add')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Add Meeting Dialog */}
      <Dialog open={meetingDialog} onOpenChange={setMeetingDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('pitchDetailPage.addMeeting')}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="meeting-type">{t('pitchDetailPage.type')} *</Label>
                <Select
                  value={newMeeting.type}
                  onValueChange={(value) => handleMeetingTypeChange(value as MeetingType)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('pitchDetailPage.selectType')} />
                  </SelectTrigger>
                  <SelectContent>
                    {meetingTypeConfigs.filter(c => c.isActive).map(config => (
                      <SelectItem key={config.name} value={config.name}>
                        {config.displayName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="meeting-date">{t('pitchDetailPage.date')} *</Label>
                <LocalizedDateInput
                  id="meeting-date"
                  value={meetingDate}
                  onChange={setMeetingDate}
                  aria-required="true"
                />
              </div>
            </div>
            
            {/* DOR Checklist */}
            {(newMeeting.dorItems && newMeeting.dorItems.length > 0) && (
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  {t('pitchDetailPage.dor')}
                  {newMeeting.dorReady ? (
                    <span className="text-xs text-green-600 font-medium">✓ {t('pitchDetailPage.ready')}</span>
                  ) : (
                    <span className="text-xs text-amber-600 font-medium">({t('pitchDetailPage.pending')})</span>
                  )}
                </Label>
                <div className="border rounded-md p-2 space-y-1 max-h-32 overflow-y-auto">
                  {newMeeting.dorItems.map((item, index) => (
                    <div key={item.id ?? index} className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        id={`dor-${item.id ?? index}`}
                        checked={item.isCompleted}
                        onChange={() => toggleChecklistItem('dor', item.id ?? index)}
                        className="h-4 w-4 rounded border-gray-300"
                      />
                      <label 
                        htmlFor={`dor-${item.id ?? index}`}
                        className={`text-sm flex-1 ${item.isCompleted ? 'line-through text-muted-foreground' : ''}`}
                      >
                        {item.name}
                        {item.isRequired && <span className="text-red-500 ml-1">*</span>}
                      </label>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* DOD Checklist */}
            {(newMeeting.dodItems && newMeeting.dodItems.length > 0) && (
              <div className="space-y-2">
                <Label className="flex items-center gap-2">
                  {t('pitchDetailPage.dod')}
                  {newMeeting.dodReady ? (
                    <span className="text-xs text-green-600 font-medium">✓ {t('pitchDetailPage.ready')}</span>
                  ) : (
                    <span className="text-xs text-amber-600 font-medium">({t('pitchDetailPage.pending')})</span>
                  )}
                </Label>
                <div className="border rounded-md p-2 space-y-1 max-h-32 overflow-y-auto">
                  {newMeeting.dodItems.map((item, index) => (
                    <div key={item.id ?? index} className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        id={`dod-${item.id ?? index}`}
                        checked={item.isCompleted}
                        onChange={() => toggleChecklistItem('dod', item.id ?? index)}
                        className="h-4 w-4 rounded border-gray-300"
                      />
                      <label 
                        htmlFor={`dod-${item.id ?? index}`}
                        className={`text-sm flex-1 ${item.isCompleted ? 'line-through text-muted-foreground' : ''}`}
                      >
                        {item.name}
                        {item.isRequired && <span className="text-red-500 ml-1">*</span>}
                      </label>
                    </div>
                  ))}
                </div>
              </div>
            )}
            
            <div className="space-y-2">
              <Label htmlFor="meeting-notes">{t('pitchDetailPage.meetingNotes')}</Label>
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
                    {showMeetingDocUpload ? t('pitchDetailPage.hideDocuments') : t('pitchDetailPage.addDocuments')} (MOM, etc.)
                  </Button>
                </CollapsibleTrigger>
                <CollapsibleContent className="mt-3">
                  <p className="text-sm text-muted-foreground mb-2">
                    {t('pitchDetailPage.meetingDocsDesc')}
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
                      {t('pitchDetailPage.dropFiles')}
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
              {t('pitchDetailPage.cancel')}
            </Button>
            <Button onClick={handleCreateMeeting}>{t('pitchDetailPage.add')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* View Meeting Dialog (Read-only, shows only completed items) */}
      {viewMeeting && (
        <Dialog open={viewMeetingDialog} onOpenChange={setViewMeetingDialog}>
          <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>{t('meetingList.dialog.viewTitle')}</DialogTitle>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              {/* Meeting Info */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.type')}</Label>
                  <div className="text-sm font-medium">{getMeetingTypeDisplayName(viewMeeting.type)}</div>
                </div>
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.date')}</Label>
                  <div className="text-sm">{formatLocalizedDate(new Date(viewMeeting.dateHeld), i18n.language)}</div>
                </div>
              </div>

              {viewMeeting.attendees && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.attendees')}</Label>
                  <div className="text-sm whitespace-pre-wrap">{viewMeeting.attendees}</div>
                </div>
              )}

              {/* DOR Items (only completed) */}
              {viewMeeting.dorItems && viewMeeting.dorItems.length > 0 && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.dor')}</Label>
                  <div className="space-y-2">
                    {viewMeeting.dorItems.map((item, index) => (
                      <div key={index} className="flex items-start gap-2 text-sm">
                        <Checkbox checked disabled className="mt-0.5" />
                        <div className="flex-1">
                          <div className="font-medium">{item.name}</div>
                          {item.description && (
                            <div className="text-muted-foreground text-xs mt-1">{item.description}</div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* DOD Items (only completed) */}
              {viewMeeting.dodItems && viewMeeting.dodItems.length > 0 && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.dod')}</Label>
                  <div className="space-y-2">
                    {viewMeeting.dodItems.map((item, index) => (
                      <div key={index} className="flex items-start gap-2 text-sm">
                        <Checkbox checked disabled className="mt-0.5" />
                        <div className="flex-1">
                          <div className="font-medium">{item.name}</div>
                          {item.description && (
                            <div className="text-muted-foreground text-xs mt-1">{item.description}</div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {viewMeeting.notes && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.notes')}</Label>
                  <div className="text-sm whitespace-pre-wrap bg-muted p-3 rounded-md">{viewMeeting.notes}</div>
                </div>
              )}

              {viewMeeting.decisions && (
                <div className="space-y-2">
                  <Label>{t('meetingList.dialog.decisions')}</Label>
                  <div className="text-sm whitespace-pre-wrap bg-muted p-3 rounded-md">{viewMeeting.decisions}</div>
                </div>
              )}

              {/* Action Items */}
              {viewMeeting.actions && viewMeeting.actions.length > 0 && (
                <div className="space-y-2">
                  <Label>{t('meetingList.actionItems.title')}</Label>
                  <div className="space-y-2">
                    {viewMeeting.actions.map((action, index) => (
                      <Card key={index}>
                        <CardContent className="p-4">
                          <div className="flex flex-col gap-2">
                            <div className="flex items-start justify-between gap-2">
                              <div className="text-sm flex-1">{action.description}</div>
                              <Badge variant={action.status === 'COMPLETED' ? 'success' : action.status === 'IN_PROGRESS' ? 'warning' : 'outline'}>
                                {action.status === 'COMPLETED' ? t('meetingList.actionItems.completed') : 
                                 action.status === 'IN_PROGRESS' ? t('meetingList.actionItems.inProgress') : 
                                 t('meetingList.actionItems.open')}
                              </Badge>
                            </div>
                            {action.assignedToName && (
                              <div className="text-xs text-muted-foreground">
                                {t('meetingList.actionItems.assignedTo')}: {action.assignedToName}
                              </div>
                            )}
                            {action.dueDate && (
                              <div className="text-xs text-muted-foreground">
                                {t('meetingList.actionItems.dueDate')}: {formatLocalizedDate(new Date(action.dueDate), i18n.language)}
                              </div>
                            )}
                            {action.notes && (
                              <div className="text-xs text-muted-foreground mt-1">{action.notes}</div>
                            )}
                          </div>
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setViewMeetingDialog(false)}>
                {t('meetingList.dialog.close')}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      )}

      {/* History Dialog */}
      <EntityHistoryDialog
        open={historyDialogOpen}
        onOpenChange={setHistoryDialogOpen}
        entityName={t('pitchDetailPage.pitch')}
        entityId={String(pitch.id)}
        fetchHistory={async (page, size) => {
          const response = await pitchService.getHistory(pitch.id, page, size);
          return response.data;
        }}
      />

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
