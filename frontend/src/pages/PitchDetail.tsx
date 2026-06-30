import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import dayjs from 'dayjs';
import { safeParseId } from '../utils/validation';
import { pitchService } from '../services/pitchService';
import { epicService } from '../services/epicService';
import { workLogService } from '../services/workLogService';
import { meetingService } from '../services/meetingService';
import { taskService } from '../services/taskService';
import { documentService, UploadedDocument } from '../services/documentService';
import { organizationSettingsService } from '../services/organizationSettingsService';
import { Pitch, Epic, Meeting, CreateWorkLogForSelfRequest, CreateMeetingRequest, MeetingType, PitchStatus, MeetingChecklistItem, Task, WorkLogPersonSummary } from '../types';
import { MeetingTypeConfig } from '../types/organizationSettings';
import { CustomFieldsSection } from '../components/CustomFieldsSection';
import ProgressBar from '../components/ProgressBar';
import RiskInsightsCard from '../components/RiskInsightsCard';
import { PitchDetailSkeleton } from '../components/Skeletons';
import { QAFloatingButton } from '../components/QAFloatingButton';
import { NotesList } from '../components/NotesList';
import { EntityHistoryDialog } from '../components/EntityHistoryDialog';
import { useToast, useProject, useAuth, useBreadcrumbLabel } from '../contexts';
import { MoveToProjectDialog } from '../components/MoveToProjectDialog';
import { getUserFriendlyError } from '../utils/errorMessages';
import { Button } from '../components/ui/button';
import { FolderInput } from 'lucide-react';
import {
  PitchHeader,
  PitchStatsRow,
  PitchTeamCapacity,
  PitchShapingSection,
  PitchDocumentsSection,
  PitchTasksSection,
  PitchWorkLogsSection,
  PitchMeetingsSection,
} from '../components/pitchDetail';
import type { ShapeUpFields } from '../components/pitchDetail/PitchShapingSection';

export default function PitchDetail() {
  const { t, i18n } = useTranslation();
  const { id: idParam } = useParams<{ id: string }>();
  const id = safeParseId(idParam);
  const { showSuccess, showError } = useToast();
  const { currentProject } = useProject();
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const [moveDialogOpen, setMoveDialogOpen] = useState(false);
  const [pitch, setPitch] = useState<Pitch | null>(null);
  const [epics, setEpics] = useState<Epic[]>([]);
  const [workLogPersonSummaries, setWorkLogPersonSummaries] = useState<WorkLogPersonSummary[]>([]);
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [documents, setDocuments] = useState<UploadedDocument[]>([]);
  const [meetingTypeConfigs, setMeetingTypeConfigs] = useState<MeetingTypeConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [, setSaving] = useState(false);
  const [historyDialogOpen, setHistoryDialogOpen] = useState(false);

  // Show the pitch title (not "Pitch #N") in the global breadcrumb.
  useBreadcrumbLabel(idParam ? `/pitches/${idParam}` : undefined, pitch?.title);

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
    appetiteDays: undefined,
  });

  const [workLogDialog, setWorkLogDialog] = useState(false);
  const [meetingDialog, setMeetingDialog] = useState(false);
  const [viewMeetingDialog, setViewMeetingDialog] = useState(false);
  const [viewMeeting, setViewMeeting] = useState<Meeting | null>(null);
  const [meetingPendingDocs, setMeetingPendingDocs] = useState<File[]>([]);
  const [showMeetingDocUpload, setShowMeetingDocUpload] = useState(false);

  // AI extraction state
  const [extracting, setExtracting] = useState(false);
  const [extractedDocumentName, setExtractedDocumentName] = useState('');
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
      loadWorkLogs(id);
    }
    return () => abortController.abort();
  }, [id]);

  useEffect(() => {
    if (currentProject?.id) {
      epicService.getByProject(currentProject.id)
        .then(res => setEpics(res.data))
        .catch(error => {
          console.error('Failed to load epics:', error);
        });
    }
  }, [currentProject?.id]);

  const loadData = async (pitchId: number) => {
    try {
      const [pitchRes, meetingsRes, docsRes, orgSettingsRes, tasksRes] = await Promise.all([
        pitchService.getById(pitchId),
        meetingService.getByPitchId(pitchId),
        documentService.getDocumentsForPitch(pitchId),
        organizationSettingsService.getMeetingTypes().catch(() => ({ data: [] })),
        taskService.getByPitchId(pitchId).catch(() => ({ data: [] })),
      ]);
      const pitchData = pitchRes.data;
      setPitch(pitchData);
      setMeetings(meetingsRes.data);
      setDocuments(docsRes.data);
      setTasks(Array.isArray(tasksRes.data) ? tasksRes.data : []);
      setMeetingTypeConfigs(Array.isArray(orgSettingsRes.data) ? orgSettingsRes.data : []);

      // Sync Shape Up fields
      setShapeUpFields({
        problemStatement: pitchData.problemStatement || '',
        solution: pitchData.solution || '',
        rabbitHoles: pitchData.rabbitHoles || '',
        risks: pitchData.risks || '',
        noGos: pitchData.noGos || '',
        wireframeLinks: pitchData.wireframeLinks || '',
        appetiteDays: pitchData.appetiteDays ?? undefined,
      });
    } catch (error) {
      console.error('Failed to load pitch:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadWorkLogs = async (pitchId: number) => {
    try {
      const res = await workLogService.getPersonSummaryByPitchId(pitchId);
      setWorkLogPersonSummaries(res.data);
    } catch (error) {
      console.error('Failed to load work log summaries:', error);
    }
  };

  const handleDocumentDeleted = (docId: number) => {
    setDocuments(prev => prev.filter(d => d.id !== docId));
  };

  const handleTitleSave = async (newTitle: string) => {
    if (!pitch) return;
    try {
      await pitchService.update(pitch.id, {
        title: newTitle,
        description: pitch.description,
        cycleId: pitch.cycleId,
        teamId: pitch.teamId,
        epicId: pitch.epicId,
        status: pitch.status,
        targetReleaseId: pitch.targetReleaseId,
        priority: pitch.priority,
        problemStatement: pitch.problemStatement,
        solution: pitch.solution,
        rabbitHoles: pitch.rabbitHoles,
        risks: pitch.risks,
        noGos: pitch.noGos,
        wireframeLinks: pitch.wireframeLinks,
        appetiteDays: pitch.appetiteDays,
      });
      showSuccess(t('pitchDetailPage.titleUpdated'));
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchDetailPage.titleUpdateFailed')));
      throw error; // let PitchHeader know save failed so it stays in edit mode
    }
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

  const handleEpicChange = async (epicId: number | null) => {
    if (!pitch) return;
    try {
      if (epicId) {
        await pitchService.linkToEpic(pitch.id, epicId);
      } else {
        await pitchService.unlinkFromEpic(pitch.id);
      }
      showSuccess(t('pitchDetailPage.epicUpdated'));
      loadData(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchDetailPage.epicUpdateFailed')));
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
        cycleId: pitch.cycleId,
        teamId: pitch.teamId,
        epicId: pitch.epicId,
        status: pitch.status,
        targetReleaseId: pitch.targetReleaseId,
        priority: pitch.priority,
        ...shapeUpFields,
        // appetiteDays comes from shapeUpFields (overrides nothing above)
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
        appetiteDays: pitch.appetiteDays ?? undefined,
      });
    }
    setExtractedDocumentName('');
    setEditingShapeUp(false);
  };

  // Extract Shape Up fields from uploaded document using AI
  const handleExtractFromDocument = async (file: File) => {
    if (!pitch) return;
    try {
      setExtracting(true);
      const response = await documentService.extractPitchData(file, pitch.id, true);
      const extracted = response.data;

      if (extracted.extractionSuccessful) {
        setExtractedDocumentName(file.name);
        // Apply extracted data to form fields (only fills empty fields by default)
        setShapeUpFields(prev => ({
          problemStatement: extracted.problemStatement || prev.problemStatement,
          solution: extracted.solution || prev.solution,
          rabbitHoles: extracted.rabbitHoles || prev.rabbitHoles,
          risks: extracted.risks || prev.risks,
          noGos: extracted.noGos || prev.noGos,
          wireframeLinks: extracted.wireframeLinks || prev.wireframeLinks,
          appetiteDays: extracted.appetiteDays ?? prev.appetiteDays,
        }));
        // Enter edit mode so user can review/modify the extracted fields
        if (!editingShapeUp) {
          setEditingShapeUp(true);
        }
        // Refresh only the documents list — do NOT call loadData() here because
        // that would re-sync shapeUpFields from the server (still empty) and
        // overwrite the extracted data we just applied to the form.
        documentService.getDocumentsForPitch(pitch.id).then(res => setDocuments(res.data));
        showSuccess(t('pitchDetailPage.dataExtracted'));
      } else {
        setExtractedDocumentName('');
        showError(extracted.errorMessage || t('pitchDetailPage.extractionFailed'));
      }
    } catch (error) {
      setExtractedDocumentName('');
      showError(getUserFriendlyError(error, t('pitchDetailPage.extractionError')));
    } finally {
      setExtracting(false);
    }
  };

  // Check if pitch has any Shape Up content
  const hasShapeUpContent = Boolean(
    pitch && (pitch.problemStatement || pitch.solution || pitch.rabbitHoles ||
    pitch.risks || pitch.noGos || pitch.wireframeLinks)
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
      loadWorkLogs(pitch.id);
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchDetailPage.workLogFailed')));
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteWorkLog = async (workLogId: number, ownerPersonId: number) => {
    if (!pitch) return;
    try {
      const isOwn = user?.personId != null && user.personId === ownerPersonId;
      if (isOwn) {
        await workLogService.deleteMy(workLogId);
      } else {
        await workLogService.delete(workLogId);
      }
      showSuccess(t('pitchDetailPage.workLogDeleted'));
      loadData(pitch.id);
      loadWorkLogs(pitch.id);
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

  // Use server-computed totalHoursSpent from the pitch DTO (accurate across all pages)
  const totalHours = pitch?.totalHoursSpent ?? 0;

  return (
    <div>
      <PitchHeader
        pitch={pitch}
        epics={epics}
        onStatusChange={handleStatusChange}
        onHistoryOpen={() => setHistoryDialogOpen(true)}
        onTitleSave={handleTitleSave}
        onEpicChange={handleEpicChange}
      />

      {isAdmin && (
        <div className="flex justify-end mb-2">
          <Button variant="outline" size="sm" onClick={() => setMoveDialogOpen(true)}>
            <FolderInput className="h-4 w-4 mr-2" />
            {t('moveToProject.confirm')}
          </Button>
        </div>
      )}

      <PitchStatsRow
        pitch={pitch}
        totalHours={totalHours}
        workLogTotalElements={workLogPersonSummaries.reduce((sum, s) => sum + s.entryCount, 0)}
      />

      <PitchTeamCapacity pitch={pitch} />

      {/* Progress Bar */}
      <div className="mb-6">
        <ProgressBar
          value={pitch.progressPercentage || 0}
          label={t('dashboard.budgetProgress')}
          color={(pitch.progressPercentage || 0) > 100 ? 'error' : 'primary'}
        />
      </div>

      <PitchShapingSection
        pitch={pitch}
        editingShapeUp={editingShapeUp}
        savingShapeUp={savingShapeUp}
        shapeUpFields={shapeUpFields}
        extracting={extracting}
        extractedDocumentName={extractedDocumentName}
        hasShapeUpContent={hasShapeUpContent}
        onSetEditingShapeUp={setEditingShapeUp}
        onSaveShapeUp={handleSaveShapeUp}
        onCancelShapeUpEdit={handleCancelShapeUpEdit}
        onExtractFromDocument={handleExtractFromDocument}
        onShapeUpFieldChange={setShapeUpFields}
      />

      <div className="grid grid-cols-1 gap-6">
        {/* Risk Analysis - Full Width */}
        <RiskInsightsCard pitchId={pitch.id} />

        <PitchDocumentsSection
          pitchId={pitch.id}
          documents={documents}
          onDocumentDeleted={handleDocumentDeleted}
          onUploadComplete={() => loadData(pitch.id)}
        />

        <CustomFieldsSection
          entityType="PITCH"
          entityId={pitch.id}
          projectId={pitch.projectId}
        />

        <NotesList
          contextType="pitch"
          contextId={pitch.id}
          title={t('pitchDetailPage.notes')}
        />

        <PitchTasksSection
          tasks={tasks}
          pitchId={pitch.id}
          cycleId={pitch.cycleId}
          onTaskCreated={(task) => setTasks(prev => [task, ...prev])}
        />

        {/* Work Logs and Meetings - Two columns on desktop */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <PitchWorkLogsSection
            pitchId={pitch.id}
            personSummaries={workLogPersonSummaries}
            workLogDialog={workLogDialog}
            newWorkLog={newWorkLog}
            workLogDate={workLogDate}
            language={i18n.language}
            onSetWorkLogDialog={setWorkLogDialog}
            onCreateWorkLog={handleCreateWorkLog}
            onDeleteWorkLog={handleDeleteWorkLog}
            onNewWorkLogChange={setNewWorkLog}
            onWorkLogDateChange={setWorkLogDate}
          />

          <PitchMeetingsSection
            meetings={meetings}
            meetingDialog={meetingDialog}
            viewMeetingDialog={viewMeetingDialog}
            viewMeeting={viewMeeting}
            newMeeting={newMeeting}
            meetingDate={meetingDate}
            meetingPendingDocs={meetingPendingDocs}
            showMeetingDocUpload={showMeetingDocUpload}
            meetingTypeConfigs={meetingTypeConfigs}
            language={i18n.language}
            getMeetingTypeDisplayName={getMeetingTypeDisplayName}
            onSetMeetingDialog={setMeetingDialog}
            onSetViewMeetingDialog={setViewMeetingDialog}
            onOpenMeetingDialog={handleOpenMeetingDialog}
            onCreateMeeting={handleCreateMeeting}
            onViewMeeting={handleViewMeeting}
            onMeetingTypeChange={handleMeetingTypeChange}
            onNewMeetingChange={setNewMeeting}
            onMeetingDateChange={setMeetingDate}
            onMeetingPendingFileSelect={handleMeetingPendingFileSelect}
            onRemoveMeetingPendingDoc={handleRemoveMeetingPendingDoc}
            onSetShowMeetingDocUpload={setShowMeetingDocUpload}
            onToggleChecklistItem={toggleChecklistItem}
          />
        </div>
      </div>

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

      {isAdmin && (
        <MoveToProjectDialog
          open={moveDialogOpen}
          onOpenChange={setMoveDialogOpen}
          entityType="pitch"
          entityId={pitch.id}
          entityTitle={pitch.title}
          currentProjectId={pitch.projectId ?? undefined}
          onSuccess={() => window.location.reload()}
        />
      )}
    </div>
  );
}
