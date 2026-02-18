import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
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
  SlidersHorizontal,
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
import { cn } from '../lib/utils';
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
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '../components/ui/popover';
import { Checkbox } from '../components/ui/checkbox';
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

const statusColumns: PitchStatus[] = ['IDEA', 'DRAFT', 'SHAPED', 'PENDING', 'STARTED', 'IN_PROGRESS', 'TESTING', 'DONE'];

export default function PitchBoard() {
  const { t } = useTranslation();
  const { currentProject, isAllProjectsSelected } = useProject();
  const { showSuccess, showError } = useToast();
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<string>('');
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'title' | 'appetite' | 'team'>('title');
  const [visibleColumns, setVisibleColumns] = useState<Set<PitchStatus>>(() => {
    try {
      const stored = localStorage.getItem('pitchBoard.visibleColumns');
      if (stored) return new Set(JSON.parse(stored) as PitchStatus[]);
    } catch {}
    return new Set(statusColumns);
  });
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
    appetiteDays: undefined,
    cycleId: undefined,
    teamId: undefined,
    status: 'IDEA',
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
    if (selectedCycle === 'all') {
      loadAllPitches();
    } else if (selectedCycle) {
      loadPitches(parseInt(selectedCycle));
    }
  }, [selectedCycle]);

  const loadInitialData = async () => {
    try {
      let cyclesPromise;
      if (isAllProjectsSelected) {
        cyclesPromise = cycleService.getMyActiveCycles();
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
      showError(getUserFriendlyError(error, t('pitchBoard.errors.loadFailed')));
    }
  };

  const loadAllPitches = async () => {
    try {
      const response = await pitchService.getAll();
      setPitches(response.data);
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchBoard.errors.loadFailed')));
    }
  };

  const toggleColumnVisibility = (status: PitchStatus) => {
    setVisibleColumns(prev => {
      const next = new Set(prev);
      if (next.has(status)) {
        next.delete(status);
      } else {
        next.add(status);
      }
      try {
        localStorage.setItem('pitchBoard.visibleColumns', JSON.stringify([...next]));
      } catch {}
      return next;
    });
  };

  const handleStatusChange = async (pitchId: number, newStatus: string) => {
    try {
      await pitchService.updateStatus(pitchId, newStatus as PitchStatus);
      if (selectedCycle === 'all') {
        loadAllPitches();
      } else if (selectedCycle) {
        loadPitches(parseInt(selectedCycle));
      }
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchBoard.errors.updateStatusFailed')));
    }
  };

  // Validate pitch form - requirements depend on target status
  const validatePitchForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!newPitch.title.trim()) {
      errors.title = t('pitchBoard.pitchTitleRequired');
    } else if (newPitch.title.trim().length < 3) {
      errors.title = t('pitchBoard.pitchTitleMinLength');
    }

    // Appetite is only required for SHAPED status and above
    const requiresAppetite = ['SHAPED', 'PENDING', 'STARTED', 'IN_PROGRESS', 'TESTING', 'DONE'].includes(newPitch.status || 'IDEA');
    if (requiresAppetite) {
      if (!newPitch.appetiteDays || newPitch.appetiteDays < 1) {
        errors.appetiteDays = t('pitchBoard.appetiteMin');
      } else if (newPitch.appetiteDays > 42) {
        errors.appetiteDays = t('pitchBoard.appetiteMax');
      }
    }

    // Cycle is only required for PENDING status and above
    const requiresCycle = ['PENDING', 'STARTED', 'IN_PROGRESS', 'TESTING', 'DONE'].includes(newPitch.status || 'IDEA');
    if (requiresCycle && !selectedCycle) {
      errors.cycle = t('pitchBoard.cycleRequired');
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
      const requiresCycle = ['PENDING', 'STARTED', 'IN_PROGRESS', 'TESTING', 'DONE'].includes(newPitch.status || 'IDEA');
      
      const response = await pitchService.create({
        ...newPitch,
        cycleId: requiresCycle && selectedCycle ? parseInt(selectedCycle) : undefined,
        appetiteDays: newPitch.appetiteDays || undefined,
      });
      const createdPitch = response.data;
      
      // Link extracted document if exists
      if (extractedDocumentId && createdPitch.id) {
        try {
          await documentService.linkDocumentToPitch(extractedDocumentId, createdPitch.id);
          showSuccess(t('pitchBoard.pitchCreatedWithDoc'));
        } catch (docError) {
          console.error('Document linking error:', docError);
          showError(t('pitchBoard.docLinkFailed'));
        }
      }
      
      // Upload any additional pending documents
      if (pendingDocuments.length > 0 && createdPitch.id) {
        showSuccess(t('pitchBoard.pitchCreatedUploading'));
        for (const file of pendingDocuments) {
          try {
            await documentService.uploadForPitch(createdPitch.id, file);
          } catch (docError) {
            console.error('Document upload error:', docError);
            showError(t('pitchBoard.docUploadFailed', { name: file.name }));
          }
        }
        showSuccess(t('pitchBoard.pitchCreatedWithDocs', { count: pendingDocuments.length }));
      } else if (!extractedDocumentId) {
        showSuccess(t('pitchBoard.pitchCreated'));
      }
      
      setCreateDialog(false);
      setNewPitch({
        title: '',
        description: '',
        appetiteDays: undefined,
        cycleId: undefined,
        teamId: undefined,
        status: 'IDEA',
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
      
      if (selectedCycle === 'all') {
        loadAllPitches();
      } else if (selectedCycle) {
        loadPitches(parseInt(selectedCycle));
      }
    } catch (error) {
      showError(getUserFriendlyError(error, t('pitchBoard.errors.createFailed')));
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
        showSuccess(t('pitchBoard.dataExtracted'));
      } else {
        setExtractedDocumentName('');
        setExtractedDocumentId(null);
        showError(extracted.errorMessage || t('pitchBoard.extractionFailed'));
      }
    } catch (error) {
      setExtractedDocumentName('');
      setExtractedDocumentId(null);
      showError(getUserFriendlyError(error, t('pitchBoard.extractionError')));
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
            <h1 className="text-2xl font-bold text-foreground">{t('pitchBoard.title')}</h1>
            <p className="text-sm text-muted-foreground">
              {isAllProjectsSelected ? t('pitchBoard.allProjects') : currentProject?.name}
            </p>
          </div>
          <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
            <Select value={selectedCycle} onValueChange={setSelectedCycle}>
              <SelectTrigger className="w-full sm:w-[200px]">
                <SelectValue placeholder={t('pitchBoard.selectCycle')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('pitchBoard.allCycles')}</SelectItem>
                {cycles.map((cycle) => (
                  <SelectItem key={cycle.id} value={cycle.id.toString()}>
                    {cycle.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Popover>
              <PopoverTrigger asChild>
                <Button variant="outline" size="sm">
                  <SlidersHorizontal className="h-4 w-4 mr-2" />
                  {t('pitchBoard.columns')}
                </Button>
              </PopoverTrigger>
              <PopoverContent className="w-48 p-3" align="end">
                <div className="space-y-2">
                  {statusColumns.map((status) => (
                    <div key={status} className="flex items-center gap-2">
                      <Checkbox
                        id={`col-${status}`}
                        checked={visibleColumns.has(status)}
                        onCheckedChange={() => toggleColumnVisibility(status)}
                      />
                      <label htmlFor={`col-${status}`} className="text-sm cursor-pointer">
                        {status.replace('_', ' ')}
                      </label>
                    </div>
                  ))}
                </div>
              </PopoverContent>
            </Popover>
            <Button
              onClick={() => setCreateDialog(true)}
              disabled={!selectedCycle || selectedCycle === 'all'}
              data-tour="new-pitch-btn"
              size="sm"
            >
              <Plus className="h-4 w-4 mr-2" />
              {t('pitchBoard.newPitch')}
            </Button>
          </div>
        </div>

        {/* Search and Sort Controls */}
        {selectedCycle && (
          <div className="flex flex-col sm:flex-row gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder={t('pitchBoard.searchPlaceholder')}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <Select value={sortBy} onValueChange={(value: any) => setSortBy(value)}>
              <SelectTrigger className="w-full sm:w-[200px]">
                <ArrowUpDown className="h-4 w-4 mr-2" />
                <SelectValue placeholder={t('pitchBoard.sortBy')} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="title">{t('pitchBoard.sortTitle')}</SelectItem>
                <SelectItem value="appetite">{t('pitchBoard.sortAppetite')}</SelectItem>
                <SelectItem value="team">{t('pitchBoard.sortTeam')}</SelectItem>
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
              title={t('pitchBoard.noCycleSelected')}
              description={t('pitchBoard.noCycleDescription')}
              size="medium"
            />
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4" data-tour="pitch-board">
          {statusColumns.filter(s => visibleColumns.has(s)).map((status) => (
            <div key={status} className="min-w-0">
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
                      {selectedCycle === 'all' && pitch.cycleName && (
                        <Badge variant="outline" className="text-xs mt-1 mb-0 px-1 py-0">
                          {pitch.cycleName}
                        </Badge>
                      )}
                      <p className="text-sm text-muted-foreground mb-3 mt-1">
                        {pitch.teamName || t('pitchBoard.unassigned')} • {pitch.appetiteDays}d
                      </p>
                      {pitch.busiestPerson && (
                        <div className="text-xs text-muted-foreground mb-2 flex items-center gap-1">
                          <span className="font-medium">👤 {pitch.busiestPerson.personName}</span>
                          <span className={cn(
                            pitch.busiestPerson.isOverBudget ? 'text-destructive' :
                            pitch.busiestPerson.utilizationPercent > 80 ? 'text-orange-500' :
                            'text-green-600'
                          )}>
                            ({pitch.busiestPerson.utilizationPercent?.toFixed(0)}%)
                          </span>
                        </div>
                      )}
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
                        title={t('pitchBoard.noPitches')}
                        description={t('pitchBoard.noPitchesDescription', { status: status.toLowerCase().replace('_', ' ') })}
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
        <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{t('pitchBoard.createNewPitch')}</DialogTitle>
            <DialogDescription>
              {t('pitchBoard.createDescription')}
            </DialogDescription>
          </DialogHeader>
          
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-3">
              <TabsTrigger value="basic">{t('pitchBoard.basicInfo')}</TabsTrigger>
              <TabsTrigger value="shaping">
                <Target className="h-4 w-4 mr-1" />
                {t('pitchBoard.shapeUp')}
              </TabsTrigger>
              <TabsTrigger value="documents">
                <FileUp className="h-4 w-4 mr-1" />
                {t('pitchBoard.documents')}
              </TabsTrigger>
            </TabsList>
            
            {/* Basic Info Tab */}
            <TabsContent value="basic" className="space-y-4 mt-4">
              {/* Title */}
              <div className="space-y-2">
                <Label htmlFor="pitch-title">{t('pitchBoard.pitchTitle')} *</Label>
                <Input
                  id="pitch-title"
                  value={newPitch.title}
                  onChange={(e) => {
                    setNewPitch({ ...newPitch, title: e.target.value });
                    setFieldErrors((prev) => ({ ...prev, title: '' }));
                  }}
                  placeholder={t('pitchBoard.pitchTitlePlaceholder')}
                  className={fieldErrors.title ? 'border-destructive' : ''}
                />
                {fieldErrors.title && (
                  <p className="text-xs text-destructive">{fieldErrors.title}</p>
                )}
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label htmlFor="pitch-description">{t('pitchBoard.description')}</Label>
                <Textarea
                  id="pitch-description"
                  value={newPitch.description}
                  onChange={(e) => setNewPitch({ ...newPitch, description: e.target.value })}
                  placeholder={t('pitchBoard.descriptionPlaceholder')}
                  rows={3}
                />
              </div>

              {/* Appetite & Team */}
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="pitch-appetite">{t('pitchBoard.appetite')} *</Label>
                  <Input
                    id="pitch-appetite"
                    type="number"
                    value={newPitch.appetiteDays}
                    onChange={(e) => {
                      const value = e.target.value === '' ? 0 : parseInt(e.target.value);
                      setNewPitch({ ...newPitch, appetiteDays: value });
                      setFieldErrors((prev) => ({ ...prev, appetiteDays: '' }));
                    }}
                    min={1}
                    max={42}
                    className={fieldErrors.appetiteDays ? 'border-destructive' : ''}
                  />
                  {fieldErrors.appetiteDays ? (
                    <p className="text-xs text-destructive">{fieldErrors.appetiteDays}</p>
                  ) : (
                    <p className="text-xs text-muted-foreground">{t('pitchBoard.appetiteDays')}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label>{t('pitchBoard.team')}</Label>
                  <Select
                    value={newPitch.teamId?.toString() || 'unassigned'}
                    onValueChange={(value) => 
                      setNewPitch({ ...newPitch, teamId: value === 'unassigned' ? undefined : parseInt(value) })
                    }
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={t('pitchBoard.selectTeam')} />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="unassigned">{t('pitchBoard.unassigned')}</SelectItem>
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
                  {t('pitchBoard.shapeUpMethodology')}
                </p>
              </div>

              {/* Problem Statement */}
              <div className="space-y-2">
                <Label htmlFor="pitch-problem" className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-orange-500" />
                  {t('pitchBoard.problemStatement')}
                </Label>
                <Textarea
                  id="pitch-problem"
                  value={newPitch.problemStatement || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, problemStatement: e.target.value })}
                  placeholder={t('pitchBoard.problemPlaceholder')}
                  rows={3}
                />
              </div>

              {/* Solution */}
              <div className="space-y-2">
                <Label htmlFor="pitch-solution" className="flex items-center gap-2">
                  <Lightbulb className="h-4 w-4 text-yellow-500" />
                  {t('pitchBoard.solution')}
                </Label>
                <Textarea
                  id="pitch-solution"
                  value={newPitch.solution || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, solution: e.target.value })}
                  placeholder={t('pitchBoard.solutionPlaceholder')}
                  rows={4}
                />
              </div>

              {/* Rabbit Holes */}
              <div className="space-y-2">
                <Label htmlFor="pitch-rabbitholes" className="flex items-center gap-2">
                  <Ban className="h-4 w-4 text-red-500" />
                  {t('pitchBoard.rabbitHoles')}
                </Label>
                <Textarea
                  id="pitch-rabbitholes"
                  value={newPitch.rabbitHoles || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, rabbitHoles: e.target.value })}
                  placeholder={t('pitchBoard.rabbitHolesPlaceholder')}
                  rows={3}
                />
              </div>

              {/* Risks */}
              <div className="space-y-2">
                <Label htmlFor="pitch-risks" className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-amber-500" />
                  {t('pitchBoard.risks')}
                </Label>
                <Textarea
                  id="pitch-risks"
                  value={newPitch.risks || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, risks: e.target.value })}
                  placeholder={t('pitchBoard.risksPlaceholder')}
                  rows={3}
                />
              </div>

              {/* No-Gos */}
              <div className="space-y-2">
                <Label htmlFor="pitch-nogos" className="flex items-center gap-2">
                  <X className="h-4 w-4 text-red-500" />
                  {t('pitchBoard.noGos')}
                </Label>
                <Textarea
                  id="pitch-nogos"
                  value={newPitch.noGos || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, noGos: e.target.value })}
                  placeholder={t('pitchBoard.noGosPlaceholder')}
                  rows={2}
                />
              </div>

              {/* Wireframe Links */}
              <div className="space-y-2">
                <Label htmlFor="pitch-wireframes" className="flex items-center gap-2">
                  <Link2 className="h-4 w-4 text-blue-500" />
                  {t('pitchBoard.wireframeLinks')}
                </Label>
                <Textarea
                  id="pitch-wireframes"
                  value={newPitch.wireframeLinks || ''}
                  onChange={(e) => setNewPitch({ ...newPitch, wireframeLinks: e.target.value })}
                  placeholder={t('pitchBoard.wireframeLinksPlaceholder')}
                  rows={3}
                />
                <p className="text-xs text-muted-foreground">{t('pitchBoard.multipleLinks')}</p>
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
                      <p className="text-sm font-medium text-green-900 dark:text-green-100">{t('pitchBoard.documentExtracted')}</p>
                      <p className="text-xs text-green-700 dark:text-green-300">{extractedDocumentName}</p>
                    </div>
                    <Badge variant="outline" className="bg-green-100 dark:bg-green-900 text-green-700 dark:text-green-300 border-green-300 dark:border-green-700">
                      {t('pitchBoard.processed')}
                    </Badge>
                  </div>
                  <p className="text-xs text-green-600 dark:text-green-400 mt-2">
                    {t('pitchBoard.reviewExtracted')}
                  </p>
                </div>
              )}

              {/* AI Extraction Section */}
              <div className="bg-gradient-to-r from-purple-50 to-blue-50 dark:from-purple-950/30 dark:to-blue-950/30 rounded-lg p-4 border border-purple-200 dark:border-purple-800">
                <div className="flex items-center gap-2 mb-2">
                  <Sparkles className="h-5 w-5 text-purple-500" />
                  <h4 className="font-semibold">{t('pitchBoard.aiExtraction')}</h4>
                </div>
                <p className="text-sm text-muted-foreground mb-3">
                  {t('pitchBoard.aiExtractionDescription')}
                  {extractedDocumentName && ` ${t('pitchBoard.replaceExtracted')}`}
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
                      <p className="text-sm text-purple-600 dark:text-purple-400">{t('pitchBoard.extracting')}</p>
                    </>
                  ) : (
                    <>
                      <Sparkles className="h-8 w-8 mx-auto text-purple-400 mb-2" />
                      <p className="text-sm text-muted-foreground">
                        {t('pitchBoard.dropDocument')}
                      </p>
                    </>
                  )}
                </div>
              </div>

              {/* Regular Document Upload */}
              <div className="space-y-2">
                <h4 className="font-medium">{t('pitchBoard.supportingDocuments')}</h4>
                <p className="text-sm text-muted-foreground">
                  {t('pitchBoard.supportingDescription')}
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
                    {t('pitchBoard.dropFiles')}
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
              {t('pitchBoard.cancel')}
            </Button>
            <LoadingButton
              onClick={handleCreatePitch}
              loading={saving}
              loadingText={t('pitchBoard.creating')}
              disabled={extracting}
            >
              {t('pitchBoard.createPitch')}
            </LoadingButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
