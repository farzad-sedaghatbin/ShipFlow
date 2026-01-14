import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Plus,
  ChevronDown,
  ChevronUp,
  Loader2,
  X,
  FileUp,
  Sparkles,
  AlertTriangle,
  Target,
  Lightbulb,
  Ban,
  Link2,
} from 'lucide-react';
import { pitchService } from '../services/pitchService';
import { cycleService } from '../services/cycleService';
import { teamService } from '../services/teamService';
import { documentService } from '../services/documentService';
import { Pitch, Cycle, Team, PitchStatus, CreatePitchRequest, ExtractedPitchData } from '../types';
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
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '../components/ui/tabs';

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
  const [extracting, setExtracting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [pendingDocuments, setPendingDocuments] = useState<File[]>([]);
  const [showDocUpload, setShowDocUpload] = useState(false);
  const [showShapingFields, setShowShapingFields] = useState(false);
  const [newPitch, setNewPitch] = useState<CreatePitchRequest>({
    title: '',
    description: '',
    appetiteDays: 6,
    cycleId: 0,
    teamId: undefined,
    status: 'PENDING',
    // Shape Up fields
    problemStatement: '',
    solution: '',
    rabbitHoles: '',
    risks: '',
    noGos: '',
    wireframeLinks: '',
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
        problemStatement: '',
        solution: '',
        rabbitHoles: '',
        risks: '',
        noGos: '',
        wireframeLinks: '',
      });
      setFieldErrors({});
      setPendingDocuments([]);
      setShowDocUpload(false);
      setShowShapingFields(false);
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
      problemStatement: '',
      solution: '',
      rabbitHoles: '',
      risks: '',
      noGos: '',
      wireframeLinks: '',
    });
    setPendingDocuments([]);
    setShowDocUpload(false);
    setShowShapingFields(false);
  };

  // Extract pitch data from uploaded document using AI
  const handleExtractFromDocument = async (file: File) => {
    try {
      setExtracting(true);
      const response = await documentService.extractPitchData(file);
      const extracted = response.data;
      
      if (extracted.extractionSuccessful) {
        // Apply extracted data to form
        setNewPitch(prev => ({
          ...prev,
          title: extracted.title || prev.title,
          problemStatement: extracted.problemStatement || prev.problemStatement,
          solution: extracted.solution || prev.solution,
          rabbitHoles: extracted.rabbitHoles || prev.rabbitHoles,
          risks: extracted.risks || prev.risks,
          noGos: extracted.noGos || prev.noGos,
          wireframeLinks: extracted.wireframeLinks || prev.wireframeLinks,
          appetiteDays: extracted.appetiteDays || prev.appetiteDays,
        }));
        setShowShapingFields(true);
        showSuccess('Pitch data extracted successfully! Review and edit the fields below.');
      } else {
        showError(extracted.errorMessage || 'Failed to extract pitch data');
      }
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to extract pitch data from document'));
    } finally {
      setExtracting(false);
    }
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
        <DialogContent className="sm:max-w-[700px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Create New Pitch</DialogTitle>
            <DialogDescription>
              Add a new pitch to the current cycle. Use the Shape Up tab to add problem statement, solution, and risks.
            </DialogDescription>
          </DialogHeader>
          
          <Tabs defaultValue="basic" className="w-full">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="basic">Basic Info</TabsTrigger>
              <TabsTrigger value="shaping">
                <Target className="h-4 w-4 mr-1" />
                Shape Up
              </TabsTrigger>
              <TabsTrigger value="documents">
                <FileUp className="h-4 w-4 mr-1" />
                Documents
              </TabsTrigger>
            </TabsList>
            
            {/* Basic Info Tab */}
            <TabsContent value="basic" className="space-y-4 mt-4">
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
                  placeholder="Brief summary of the pitch"
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
            </TabsContent>

            {/* Shape Up Tab */}
            <TabsContent value="shaping" className="space-y-4 mt-4">
              <div className="bg-muted/50 rounded-lg p-3 mb-4">
                <p className="text-sm text-muted-foreground">
                  <Target className="h-4 w-4 inline mr-1" />
                  Shape Up methodology fields help define the pitch narrative: the problem, solution, rabbit holes to avoid, and risks.
                </p>
              </div>

              {/* Problem Statement */}
              <div className="space-y-2">
                <Label htmlFor="pitch-problem" className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-orange-500" />
                  Problem Statement
                </Label>
                <Textarea
                  id="pitch-problem"
                  value={newPitch.problemStatement || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, problemStatement: e.target.value })}
                  placeholder="What problem are we solving? Why does this matter? Who is affected?"
                  rows={3}
                />
              </div>

              {/* Solution */}
              <div className="space-y-2">
                <Label htmlFor="pitch-solution" className="flex items-center gap-2">
                  <Lightbulb className="h-4 w-4 text-yellow-500" />
                  Solution (Hatched)
                </Label>
                <Textarea
                  id="pitch-solution"
                  value={newPitch.solution || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, solution: e.target.value })}
                  placeholder="The proposed solution. Fat-marker sketch of the approach, key elements, and how it addresses the problem."
                  rows={4}
                />
              </div>

              {/* Rabbit Holes */}
              <div className="space-y-2">
                <Label htmlFor="pitch-rabbitholes" className="flex items-center gap-2">
                  <Ban className="h-4 w-4 text-red-500" />
                  Rabbit Holes
                </Label>
                <Textarea
                  id="pitch-rabbitholes"
                  value={newPitch.rabbitHoles || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, rabbitHoles: e.target.value })}
                  placeholder="Edge cases to avoid, potential time sinks, areas that could derail the project."
                  rows={3}
                />
              </div>

              {/* Risks */}
              <div className="space-y-2">
                <Label htmlFor="pitch-risks" className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-amber-500" />
                  Risks & Unknowns
                </Label>
                <Textarea
                  id="pitch-risks"
                  value={newPitch.risks || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, risks: e.target.value })}
                  placeholder="Known risks, technical challenges, dependencies, or unknowns that need investigation."
                  rows={3}
                />
              </div>

              {/* No-Gos */}
              <div className="space-y-2">
                <Label htmlFor="pitch-nogos" className="flex items-center gap-2">
                  <X className="h-4 w-4 text-red-500" />
                  No-Gos (Out of Scope)
                </Label>
                <Textarea
                  id="pitch-nogos"
                  value={newPitch.noGos || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, noGos: e.target.value })}
                  placeholder="Things explicitly NOT being built. Clear boundaries for the project."
                  rows={2}
                />
              </div>

              {/* Wireframe Links */}
              <div className="space-y-2">
                <Label htmlFor="pitch-wireframes" className="flex items-center gap-2">
                  <Link2 className="h-4 w-4 text-blue-500" />
                  Wireframe / Prototype Links
                </Label>
                <Textarea
                  id="pitch-wireframes"
                  value={newPitch.wireframeLinks || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, wireframeLinks: e.target.value })}
                  placeholder="Links to Figma, wireframes, mockups, or visual references (one per line)"
                  rows={2}
                />
              </div>
            </TabsContent>

            {/* Documents Tab */}
            <TabsContent value="documents" className="space-y-4 mt-4">
              {/* AI Extraction Section */}
              <div className="bg-gradient-to-r from-purple-50 to-blue-50 dark:from-purple-950/30 dark:to-blue-950/30 rounded-lg p-4 border border-purple-200 dark:border-purple-800">
                <div className="flex items-center gap-2 mb-2">
                  <Sparkles className="h-5 w-5 text-purple-500" />
                  <h4 className="font-semibold">AI-Powered Extraction</h4>
                </div>
                <p className="text-sm text-muted-foreground mb-3">
                  Upload a pitch document to automatically extract problem statement, solution, rabbit holes, and risks using AI.
                </p>
                <div
                  className="border-2 border-dashed border-purple-300 dark:border-purple-700 rounded-lg p-4 text-center cursor-pointer hover:border-purple-500 hover:bg-purple-50/50 dark:hover:bg-purple-950/50 transition-colors"
                  onClick={() => document.getElementById('pitch-extract-upload')?.click()}
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={(e) => {
                    e.preventDefault();
                    if (e.dataTransfer.files.length > 0) {
                      handleExtractFromDocument(e.dataTransfer.files[0]);
                    }
                  }}
                >
                  <input
                    id="pitch-extract-upload"
                    type="file"
                    hidden
                    accept=".pdf,.doc,.docx,.txt,.md"
                    onChange={(e) => e.target.files?.[0] && handleExtractFromDocument(e.target.files[0])}
                    disabled={extracting}
                  />
                  {extracting ? (
                    <>
                      <Loader2 className="h-8 w-8 mx-auto text-purple-500 animate-spin mb-2" />
                      <p className="text-sm text-purple-600 dark:text-purple-400">Extracting pitch data...</p>
                    </>
                  ) : (
                    <>
                      <Sparkles className="h-8 w-8 mx-auto text-purple-400 mb-2" />
                      <p className="text-sm text-muted-foreground">
                        Drop a pitch document or click to extract Shape Up fields
                      </p>
                    </>
                  )}
                </div>
              </div>

              {/* Regular Document Upload */}
              <div className="space-y-2">
                <h4 className="font-medium">Attach Documents for Q&A</h4>
                <p className="text-sm text-muted-foreground">
                  These documents will be indexed for the knowledge base Q&A feature.
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
              </div>
            </TabsContent>
          </Tabs>
          
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={handleCloseDialog} disabled={saving || extracting}>
              Cancel
            </Button>
            <LoadingButton
              onClick={handleCreatePitch}
              loading={saving}
              loadingText="Creating..."
              disabled={extracting}
            >
              Create Pitch
            </LoadingButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
