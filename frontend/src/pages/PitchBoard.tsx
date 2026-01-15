import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Plus,
  Loader2,
  X,
  FileUp,
  Sparkles,
  AlertTriangle,
  Target,
  Lightbulb,
  Ban,
  Link2,
  Search,
  ArrowUpDown,
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
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'title' | 'appetite' | 'team'>('title');
  const [loading, setLoading] = useState(true);
  const [createDialog, setCreateDialog] = useState(false);
  const [saving, setSaving] = useState(false);
  const [extracting, setExtracting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [pendingDocuments, setPendingDocuments] = useState<File[]>([]);
  const [extractedDocumentName, setExtractedDocumentName] = useState<string>('');
  const [extractedDocumentId, setExtractedDocumentId] = useState<number | null>(null);
  const [activeTab, setActiveTab] = useState('basic');
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

  const navigate = useNavigate();

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
      
      // Link extracted document if exists
      if (extractedDocumentId && createdPitch.id) {
        try {
          await documentService.linkDocumentToPitch(extractedDocumentId, createdPitch.id);
          showSuccess('Pitch created with document!');
        } catch (docError) {
          console.error('Document linking error:', docError);
          showError('Pitch created but failed to link document');
        }
      }
      
      // Upload any additional pending documents
      if (pendingDocuments.length > 0 && createdPitch.id) {
        showSuccess('Pitch created! Uploading additional documents...');
        for (const file of pendingDocuments) {
          try {
            await documentService.uploadForPitch(createdPitch.id, file);
          } catch (docError) {
            console.error('Document upload error:', docError);
            showError(`Failed to upload ${file.name}`);
          }
        }
        showSuccess(`Pitch created with ${pendingDocuments.length} additional document(s)!`);
      } else if (!extractedDocumentId) {
        showSuccess('Pitch created successfully!');
      }
      
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
      setExtractedDocumentId(null);
      setExtractedDocumentName('');
      
      // Navigate after all state cleanup is complete to avoid visual issues
      navigate(`/pitches/${createdPitch.id}`);
      
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
    setExtractedDocumentName('');
    setExtractedDocumentId(null);
    setActiveTab('basic');
  };

  // Extract pitch data from uploaded document using AI
  // Also adds the document to the knowledge base for Q&A
  const handleExtractFromDocument = async (file: File) => {
    try {
      setExtracting(true);
      
      // Note: pitchId will be undefined for new pitches, which is fine
      // The document will still be added to knowledge base with pitch metadata once pitch is created
      const response = await documentService.extractPitchData(file, undefined, true);
      const extracted = response.data;
      
      if (extracted.extractionSuccessful) {
        // Set document name only after successful extraction
        setExtractedDocumentName(file.name);
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
        // Store the document ID from extraction (document was already saved during extraction)
        if (extracted.documentId) {
          setExtractedDocumentId(extracted.documentId);
        }
        // Switch to Shape Up tab to show extracted data
        setActiveTab('shaping');
        showSuccess('Pitch data extracted and added to knowledge base! Review the Shape Up fields and create the pitch.');
      } else {
        setExtractedDocumentName('');
        setExtractedDocumentId(null);
        showError(extracted.errorMessage || 'Failed to extract pitch data');
      }
    } catch (error) {
      setExtractedDocumentName('');
      setExtractedDocumentId(null);
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

  const filterAndSortPitches = (pitchesList: Pitch[]) => {
    return pitchesList
      .filter(pitch => {
        if (!searchTerm) return true;
        const search = searchTerm.toLowerCase();
        return (
          pitch.title.toLowerCase().includes(search) ||
          pitch.description?.toLowerCase().includes(search) ||
          pitch.teamName?.toLowerCase().includes(search) ||
          pitch.problemStatement?.toLowerCase().includes(search)
        );
      })
      .sort((a, b) => {
        switch (sortBy) {
          case 'title':
            return a.title.localeCompare(b.title);
          case 'appetite':
            return (a.appetiteDays || 0) - (b.appetiteDays || 0);
          case 'team':
            return (a.teamName || '').localeCompare(b.teamName || '');
          default:
            return 0;
        }
      });
  };

  const getPitchesByStatus = (status: PitchStatus) =>
    filterAndSortPitches(pitches.filter((p) => p.status === status));

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4">
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

        {/* Search and Sort Controls */}
        {selectedCycle && (
          <div className="flex flex-col sm:flex-row gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search pitches by title, description, or team..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <Select value={sortBy} onValueChange={(value: any) => setSortBy(value)}>
              <SelectTrigger className="w-full sm:w-[200px]">
                <ArrowUpDown className="h-4 w-4 mr-2" />
                <SelectValue placeholder="Sort by" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="title">Title (A-Z)</SelectItem>
                <SelectItem value="appetite">Appetite (Days)</SelectItem>
                <SelectItem value="team">Team</SelectItem>
              </SelectContent>
            </Select>
          </div>
        )}
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
          
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
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
                  placeholder="Figma, wireframes, mockups (one URL per line)\n\nhttps://figma.com/...\nhttps://miro.com/..."
                  rows={3}
                />
                <p className="text-xs text-muted-foreground">Enter multiple links, one per line</p>
              </div>
            </TabsContent>

            {/* Documents Tab */}
            <TabsContent value="documents" className="space-y-4 mt-4">
              {/* Extracted Document Indicator */}
              {extractedDocumentName && (
                <div className="bg-green-50 dark:bg-green-950/30 rounded-lg p-3 border border-green-200 dark:border-green-800">
                  <div className="flex items-center gap-2">
                    <FileUp className="h-4 w-4 text-green-600 dark:text-green-400" />
                    <div className="flex-1">
                      <p className="text-sm font-medium text-green-900 dark:text-green-100">Document Extracted</p>
                      <p className="text-xs text-green-700 dark:text-green-300">{extractedDocumentName}</p>
                    </div>
                    <Badge variant="outline" className="bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 border-green-300 dark:border-green-700">
                      ✓ Processed
                    </Badge>
                  </div>
                  <p className="text-xs text-green-600 dark:text-green-400 mt-2">
                    Review the extracted data in the Shape Up tab. You can extract from another document below to replace this data.
                  </p>
                </div>
              )}

              {/* AI Extraction Section */}
              <div className="bg-gradient-to-r from-purple-50 to-blue-50 dark:from-purple-950/30 dark:to-blue-950/30 rounded-lg p-4 border border-purple-200 dark:border-purple-800">
                <div className="flex items-center gap-2 mb-2">
                  <Sparkles className="h-5 w-5 text-purple-500" />
                  <h4 className="font-semibold">AI-Powered Extraction</h4>
                </div>
                <p className="text-sm text-muted-foreground mb-3">
                  Upload a pitch document to automatically extract problem statement, solution, rabbit holes, and risks using AI.
                  {extractedDocumentName && " Upload another to replace the current extracted data."}
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
                <h4 className="font-medium">Attach Supporting Documents</h4>
                <p className="text-sm text-muted-foreground">
                  Add additional reference documents (no extraction needed). These will be available for download and indexed for Q&A.
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
                    Drop files here or click to select multiple documents
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
