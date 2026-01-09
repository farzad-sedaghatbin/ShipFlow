import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Plus,
  ChevronDown,
  ChevronUp,
  Loader2,
  X,
  FileUp,
} from 'lucide-react';
import { pitchService } from '../services/pitchService';
import { cycleService } from '../services/cycleService';
import { teamService } from '../services/teamService';
import { documentService } from '../services/documentService';
import { Pitch, Cycle, Team, PitchStatus, CreatePitchRequest } from '../types';
import StatusChip from '../components/StatusChip';
import ProgressBar from '../components/ProgressBar';
import EmptyState from '../components/EmptyState';
import { EmptyPitchesIllustration } from '../components/illustrations';
import { useProject, useToast } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';
import LoadingButton from '../components/LoadingButton';

import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '../components/ui/collapsible';

const statusColumns: PitchStatus[] = ['PENDING', 'SHAPED', 'STARTED', 'IN_PROGRESS', 'TESTING', 'DONE'];

export default function PitchBoard() {
  const { currentProject, isAllProjectsSelected } = useProject();
  const { showSuccess, showError } = useToast();
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [createDialog, setCreateDialog] = useState(false);
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [pendingDocuments, setPendingDocuments] = useState<File[]>([]);
  const [showDocUpload, setShowDocUpload] = useState(false);
  const [newPitch, setNewPitch] = useState<CreatePitchRequest>({
    title: '',
    description: '',
    appetiteDays: 6,
    cycleId: 0,
    teamId: undefined,
    status: 'PENDING',
  });

  useEffect(() => {
    const abortController = new AbortController();
    loadInitialData();
    return () => abortController.abort();
  }, [currentProject, isAllProjectsSelected]);

  useEffect(() => {
    if (selectedCycle) {
      loadPitches(parseInt(selectedCycle));
    }
  }, [selectedCycle]);

  const loadInitialData = async () => {
    try {
      let cyclesPromise;
      if (isAllProjectsSelected) {
        cyclesPromise = cycleService.getActive();
      } else if (currentProject) {
        cyclesPromise = cycleService.getActiveByProject(currentProject.id);
      } else {
        cyclesPromise = Promise.resolve({ data: [] });
      }
      
      const [cyclesRes, teamsRes] = await Promise.all([
        cyclesPromise,
        teamService.getAll(),
      ]);
      setCycles(cyclesRes.data);
      setTeams(teamsRes.data);
      if (cyclesRes.data.length > 0) {
        setSelectedCycle(cyclesRes.data[0].id.toString());
      } else {
        setSelectedCycle('');
        setPitches([]);
      }
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadPitches = async (cycleId: number) => {
    try {
      const response = await pitchService.getByCycleId(cycleId);
      setPitches(response.data);
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to load pitches'));
    }
  };

  const handleStatusChange = async (pitchId: number, newStatus: string) => {
    try {
      await pitchService.updateStatus(pitchId, newStatus as PitchStatus);
      if (selectedCycle) {
        loadPitches(parseInt(selectedCycle));
      }
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to update status'));
    }
  };

  // Validate pitch form
  const validatePitchForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!newPitch.title.trim()) {
      errors.title = 'Pitch title is required';
    } else if (newPitch.title.trim().length < 3) {
      errors.title = 'Pitch title must be at least 3 characters';
    }

    if (!newPitch.appetiteDays || newPitch.appetiteDays < 1) {
      errors.appetiteDays = 'Appetite must be at least 1 day';
    } else if (newPitch.appetiteDays > 42) {
      errors.appetiteDays = 'Appetite cannot exceed 6 weeks (42 days)';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreatePitch = async () => {
    if (!validatePitchForm()) {
      return;
    }

    try {
      setSaving(true);
      const response = await pitchService.create({
        ...newPitch,
        cycleId: parseInt(selectedCycle),
      });
      const createdPitch = response.data;
      
      // Upload pending documents if any
      if (pendingDocuments.length > 0 && createdPitch.id) {
        for (const file of pendingDocuments) {
          try {
            await documentService.uploadForPitch(createdPitch.id, file);
          } catch (docError) {
            showError('Some documents failed to upload');
          }
        }
      }
      
      showSuccess('Pitch created successfully!');
      setCreateDialog(false);
      setNewPitch({
        title: '',
        description: '',
        appetiteDays: 6,
        cycleId: 0,
        teamId: undefined,
        status: 'PENDING',
      });
      setFieldErrors({});
      setPendingDocuments([]);
      setShowDocUpload(false);
      if (selectedCycle) {
        loadPitches(parseInt(selectedCycle));
      }
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to create pitch'));
    } finally {
      setSaving(false);
    }
  };

  const handleCloseDialog = () => {
    setCreateDialog(false);
    setFieldErrors({});
    setNewPitch({
      title: '',
      description: '',
      appetiteDays: 6,
      cycleId: 0,
      teamId: undefined,
      status: 'PENDING',
    });
    setPendingDocuments([]);
    setShowDocUpload(false);
  };

  const handlePendingFileSelect = (files: FileList) => {
    setPendingDocuments(prev => [...prev, ...Array.from(files)]);
  };

  const handleRemovePendingDoc = (index: number) => {
    setPendingDocuments(prev => prev.filter((_, i) => i !== index));
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  const getPitchesByStatus = (status: PitchStatus) =>
    pitches.filter((p) => p.status === status);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Pitch Board</h1>
          <p className="text-sm text-muted-foreground">
            {isAllProjectsSelected ? 'All projects' : currentProject?.name}
          </p>
        </div>
        <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
          <Select value={selectedCycle} onValueChange={setSelectedCycle}>
            <SelectTrigger className="w-full sm:w-[200px]">
              <SelectValue placeholder="Select cycle" />
            </SelectTrigger>
            <SelectContent>
              {cycles.map((cycle) => (
                <SelectItem key={cycle.id} value={cycle.id.toString()}>
                  {cycle.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button
            onClick={() => setCreateDialog(true)}
            disabled={!selectedCycle}
            data-tour="new-pitch-btn"
            size="sm"
          >
            <Plus className="h-4 w-4 mr-2" />
            New Pitch
          </Button>
        </div>
      </div>

      {/* Board */}
      {!selectedCycle ? (
        <Card>
          <CardContent className="py-12">
            <EmptyState
              illustration={<EmptyPitchesIllustration />}
              title="No cycle selected"
              description="Please select a cycle from the dropdown above to view and manage pitches"
              size="medium"
            />
          </CardContent>
        </Card>
      ) : (
        <div className="flex gap-4 overflow-x-auto pb-4" data-tour="pitch-board">
          {statusColumns.map((status) => (
            <div key={status} className="min-w-[300px] flex-shrink-0">
              {/* Column Header */}
              <div className="flex items-center justify-between mb-3">
                <StatusChip status={status} size="medium" />
                <Badge variant="outline">{getPitchesByStatus(status).length}</Badge>
              </div>

              {/* Column Content */}
              <div className="space-y-3">
                {getPitchesByStatus(status).map((pitch) => (
                  <Card
                    key={pitch.id}
                    className="hover:shadow-lg hover:border-primary/50 transition-all cursor-pointer"
                  >
                    <CardContent className="p-4">
                      <Link
                        to={`/pitches/${pitch.id}`}
                        className="font-semibold text-foreground hover:text-primary transition-colors"
                      >
                        {pitch.title}
                      </Link>
                      <p className="text-sm text-muted-foreground mb-3 mt-1">
                        {pitch.teamName || 'Unassigned'} • {pitch.appetiteDays}d
                      </p>
                      <ProgressBar
                        value={pitch.progressPercentage || 0}
                        label={`${pitch.totalHoursSpent?.toFixed(1) || 0}h / ${pitch.appetiteHours?.toFixed(0) || 0}h`}
                      />
                      <div className="mt-3">
                        <Select
                          value={pitch.status}
                          onValueChange={(value) => handleStatusChange(pitch.id, value)}
                        >
                          <SelectTrigger className="h-8 text-xs">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            {statusColumns.map((s) => (
                              <SelectItem key={s} value={s}>
                                {s.replace('_', ' ')}
                              </SelectItem>
                            ))}
                            <SelectItem value="COOLDOWN">COOLDOWN</SelectItem>
                            <SelectItem value="CANCELLED">CANCELLED</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                    </CardContent>
                  </Card>
                ))}
                {getPitchesByStatus(status).length === 0 && (
                  <Card className="opacity-60 border-dashed">
                    <CardContent className="py-6">
                      <EmptyState
                        title="No pitches"
                        description={`Drag pitches here or create a new ${status.toLowerCase().replace('_', ' ')} pitch`}
                        size="small"
                        compact
                      />
                    </CardContent>
                  </Card>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Pitch Dialog */}
      <Dialog open={createDialog} onOpenChange={(open) => !open && handleCloseDialog()}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>Create New Pitch</DialogTitle>
            <DialogDescription>
              Add a new pitch to the current cycle
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            {/* Title */}
            <div className="space-y-2">
              <Label htmlFor="pitch-title">Title *</Label>
              <Input
                id="pitch-title"
                value={newPitch.title}
                onChange={(e) => {
                  setNewPitch({ ...newPitch, title: e.target.value });
                  setFieldErrors((prev) => ({ ...prev, title: '' }));
                }}
                placeholder="Give your pitch a clear, descriptive title"
                className={fieldErrors.title ? 'border-destructive' : ''}
              />
              {fieldErrors.title && (
                <p className="text-xs text-destructive">{fieldErrors.title}</p>
              )}
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label htmlFor="pitch-description">Description</Label>
              <Textarea
                id="pitch-description"
                value={newPitch.description}
                onChange={(e) => setNewPitch({ ...newPitch, description: e.target.value })}
                placeholder="Describe the problem and proposed solution"
                rows={3}
              />
            </div>

            {/* Appetite & Team */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="pitch-appetite">Appetite (days) *</Label>
                <Input
                  id="pitch-appetite"
                  type="number"
                  value={newPitch.appetiteDays}
                  onChange={(e) => {
                    setNewPitch({ ...newPitch, appetiteDays: parseInt(e.target.value) || 0 });
                    setFieldErrors((prev) => ({ ...prev, appetiteDays: '' }));
                  }}
                  min={1}
                  max={42}
                  className={fieldErrors.appetiteDays ? 'border-destructive' : ''}
                />
                {fieldErrors.appetiteDays ? (
                  <p className="text-xs text-destructive">{fieldErrors.appetiteDays}</p>
                ) : (
                  <p className="text-xs text-muted-foreground">1-42 days</p>
                )}
              </div>
              <div className="space-y-2">
                <Label>Team</Label>
                <Select
                  value={newPitch.teamId?.toString() || 'unassigned'}
                  onValueChange={(value) => 
                    setNewPitch({ ...newPitch, teamId: value === 'unassigned' ? undefined : parseInt(value) })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select team" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="unassigned">Unassigned</SelectItem>
                    {teams.map((team) => (
                      <SelectItem key={team.id} value={team.id.toString()}>
                        {team.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Documents */}
            <Collapsible open={showDocUpload} onOpenChange={setShowDocUpload}>
              <CollapsibleTrigger asChild>
                <Button variant="ghost" size="sm" className="w-full justify-start">
                  {showDocUpload ? (
                    <ChevronUp className="h-4 w-4 mr-2" />
                  ) : (
                    <ChevronDown className="h-4 w-4 mr-2" />
                  )}
                  {showDocUpload ? 'Hide' : 'Add'} Documents
                </Button>
              </CollapsibleTrigger>
              <CollapsibleContent className="mt-2">
                <p className="text-sm text-muted-foreground mb-2">
                  Attach documents (PDF, Word, Text) to be indexed for Q&A
                </p>
                <div
                  className="border-2 border-dashed border-border rounded-lg p-6 text-center cursor-pointer hover:border-primary hover:bg-muted/50 transition-colors"
                  onClick={() => document.getElementById('pitch-doc-upload')?.click()}
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => {
                    e.preventDefault();
                    if (e.dataTransfer.files.length > 0) {
                      handlePendingFileSelect(e.dataTransfer.files);
                    }
                  }}
                >
                  <input
                    id="pitch-doc-upload"
                    type="file"
                    hidden
                    multiple
                    accept=".pdf,.doc,.docx,.txt,.md"
                    onChange={(e) => e.target.files && handlePendingFileSelect(e.target.files)}
                  />
                  <FileUp className="h-8 w-8 mx-auto text-muted-foreground mb-2" />
                  <p className="text-sm text-muted-foreground">
                    Drop files here or click to select
                  </p>
                </div>
                {pendingDocuments.length > 0 && (
                  <div className="flex flex-wrap gap-1 mt-2">
                    {pendingDocuments.map((file, index) => (
                      <Badge key={index} variant="secondary" className="gap-1">
                        {file.name}
                        <button
                          onClick={() => handleRemovePendingDoc(index)}
                          className="hover:text-destructive"
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
          <DialogFooter>
            <Button variant="outline" onClick={handleCloseDialog} disabled={saving}>
              Cancel
            </Button>
            <LoadingButton
              onClick={handleCreatePitch}
              loading={saving}
              loadingText="Creating..."
            >
              Create
            </LoadingButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
