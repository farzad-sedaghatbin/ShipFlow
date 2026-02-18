import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  ArrowLeft,
  Beaker,
  Brain,
  Building2,
  CheckCircle2,
  ChevronRight,
  Code2,
  Copy,
  FileCode2,
  GitBranch,
  History,
  Loader2,
  MessageSquare,
  RefreshCw,
  Search,
  Send,
  Server,
  Smartphone,
  Sparkles,
  Globe,
  AlertTriangle,
  Clock,
  Package,
  Layers,
  CheckSquare,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Alert, AlertDescription, AlertTitle } from '../components/ui/alert';
import { Badge } from '../components/ui/badge';
import { Checkbox } from '../components/ui/checkbox';
import { Input } from '../components/ui/input';
import { ScrollArea } from '../components/ui/scroll-area';
import { Separator } from '../components/ui/separator';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '../components/ui/accordion';
import { useToast } from '../contexts';
import { pitchService } from '../services/pitchService';
import { githubService } from '../services/githubService';
import { wiseArchitectureService } from '../services/wiseArchitectureService';
import { Pitch } from '../types';
import { GitHubRepository } from '../types/github';
import {
  WiseArchitectureResponse,
  DetectStacksResponse,
  FollowUpResponse,
  TechStackType,
  STACK_DISPLAY_NAMES,
  DetectedStack,
} from '../types/wiseArchitecture';
import { Markdown } from '../components/ui/markdown';

// Custom debounce hook for performance optimization
function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedValue(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);

  return debouncedValue;
}

// Step definitions
type Step = 'pitch' | 'repositories' | 'stacks' | 'solution' | 'chat';

const STEPS: { key: Step; titleKey: string; descKey: string }[] = [
  { key: 'pitch', titleKey: 'wiseArchitecture.steps.pitch', descKey: 'wiseArchitecture.steps.pitchDesc' },
  { key: 'repositories', titleKey: 'wiseArchitecture.steps.repositories', descKey: 'wiseArchitecture.steps.repositoriesDesc' },
  { key: 'stacks', titleKey: 'wiseArchitecture.steps.stacks', descKey: 'wiseArchitecture.steps.stacksDesc' },
  { key: 'solution', titleKey: 'wiseArchitecture.steps.solution', descKey: 'wiseArchitecture.steps.solutionDesc' },
  { key: 'chat', titleKey: 'wiseArchitecture.steps.chat', descKey: 'wiseArchitecture.steps.chatDesc' },
];

const getStackIcon = (stack: TechStackType) => {
  if (stack.startsWith('MOBILE_')) return Smartphone;
  if (stack.startsWith('BACKEND_')) return Server;
  if (stack.startsWith('WEB_')) return Globe;
  return Code2;
};

const getStackCategory = (stack: TechStackType): string => {
  if (stack.startsWith('MOBILE_')) return 'Mobile';
  if (stack.startsWith('BACKEND_')) return 'Backend';
  if (stack.startsWith('WEB_')) return 'Web';
  return 'Other';
};

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  copilotPrompt?: string;
  timestamp: Date;
}

const WiseArchitecturePage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const chatEndRef = useRef<HTMLDivElement>(null);
  
  // AbortControllers for cancelling async operations
  const detectAbortController = useRef<AbortController | null>(null);
  const analyzeAbortController = useRef<AbortController | null>(null);

  // State
  const [currentStep, setCurrentStep] = useState<Step>('pitch');
  const [error, setError] = useState<string | null>(null);
  const [featureEnabled, setFeatureEnabled] = useState<boolean | null>(null);

  // Step 1: Pitch selection
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [selectedPitch, setSelectedPitch] = useState<Pitch | null>(null);

  // Step 2: Repository selection
  const [repositories, setRepositories] = useState<GitHubRepository[]>([]);
  const [selectedRepoIds, setSelectedRepoIds] = useState<number[]>([]);
  const [repoBranches, setRepoBranches] = useState<Record<number, string>>({});
  const [repoSearchQuery, setRepoSearchQuery] = useState('');

  // Step 3: Stack detection and selection
  const [detectedStacks, setDetectedStacks] = useState<DetectedStack[]>([]);
  const [selectedStacks, setSelectedStacks] = useState<TechStackType[]>([]);
  const [detectingStacks, setDetectingStacks] = useState(false);
  const [detectProgress, setDetectProgress] = useState<{ percent: number; message: string }>({ percent: 0, message: '' });

  // Step 4: Solution
  const [solution, setSolution] = useState<WiseArchitectureResponse | null>(null);
  const [generatingSolution, setGeneratingSolution] = useState(false);
  const [analyzeProgress, setAnalyzeProgress] = useState<{ percent: number; message: string }>({ percent: 0, message: '' });

  // Step 5: Chat
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [sendingMessage, setSendingMessage] = useState(false);

  // PERFORMANCE: Debounce repository search to avoid excessive filtering
  const debouncedRepoSearch = useDebounce(repoSearchQuery, 300);

  // PERFORMANCE: Memoize filtered repositories to avoid re-computation on every render
  const filteredRepositories = useMemo(() => {
    if (!debouncedRepoSearch) return repositories;
    const query = debouncedRepoSearch.toLowerCase();
    return repositories.filter(repo => 
      repo.fullName.toLowerCase().includes(query) ||
      repo.name.toLowerCase().includes(query)
    );
  }, [repositories, debouncedRepoSearch]);

  // PERFORMANCE: Memoize stacks grouped by category
  const stacksByCategory = useMemo(() => {
    const categories: Record<string, DetectedStack[]> = {
      Mobile: [],
      Backend: [],
      Web: [],
      Other: [],
    };
    detectedStacks.forEach(stack => {
      const category = getStackCategory(stack.stackType);
      categories[category].push(stack);
    });
    return categories;
  }, [detectedStacks]);

  // PERFORMANCE: Memoize solution stacks for accordion
  const solutionStackEntries = useMemo(() => {
    if (!solution?.solutions) return [];
    return Object.entries(solution.solutions) as [TechStackType, typeof solution.solutions[TechStackType]][];
  }, [solution]);

  // Check feature status on mount
  useEffect(() => {
    checkFeatureStatus();
    loadPitches();
    loadRepositories();
  }, []);

  // Scroll to bottom when chat messages update
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages]);

  // Cleanup: abort ongoing operations on unmount
  useEffect(() => {
    return () => {
      detectAbortController.current?.abort();
      analyzeAbortController.current?.abort();
    };
  }, []);

  const checkFeatureStatus = async () => {
    try {
      const response = await wiseArchitectureService.getStatus();
      setFeatureEnabled(response.enabled);
      if (!response.enabled) {
        setError(t('wiseArchitecture.featureDisabled'));
      }
    } catch (err) {
      console.error('Failed to check feature status', err);
      setFeatureEnabled(false);
      setError(t('wiseArchitecture.statusCheckFailed'));
    }
  };

  const loadPitches = async () => {
    try {
      const response = await pitchService.getMyPitches();
      setPitches(response.data);
    } catch (err) {
      console.error('Failed to load pitches', err);
    }
  };

  const loadRepositories = async () => {
    try {
      const repos = await githubService.getAllRepositories();
      setRepositories(repos.filter(r => r.isActive));
    } catch (err) {
      console.error('Failed to load repositories', err);
    }
  };

  const handlePitchSelect = (pitchId: string) => {
    const pitch = pitches.find(p => p.id === Number(pitchId));
    setSelectedPitch(pitch || null);
  };

  const handleRepoToggle = (repoId: number) => {
    const repo = repositories.find(r => r.id === repoId);
    setSelectedRepoIds(prev => {
      if (prev.includes(repoId)) {
        // Removing - also remove from branches
        setRepoBranches(b => {
          const { [repoId]: _, ...rest } = b;
          return rest;
        });
        return prev.filter(id => id !== repoId);
      } else {
        // Adding - initialize with default branch
        if (repo?.defaultBranch) {
          setRepoBranches(b => ({ ...b, [repoId]: repo.defaultBranch }));
        }
        return [...prev, repoId];
      }
    });
  };

  // PERFORMANCE: Memoize branch change handler
  const handleBranchChange = useCallback((repoId: number, branch: string) => {
    setRepoBranches(prev => ({ ...prev, [repoId]: branch }));
  }, []);

  // PERFORMANCE: Memoize stack toggle handler
  const handleStackToggle = useCallback((stack: TechStackType) => {
    setSelectedStacks(prev =>
      prev.includes(stack)
        ? prev.filter(s => s !== stack)
        : [...prev, stack]
    );
  }, []);

  const handleDetectStacks = async () => {
    if (selectedRepoIds.length === 0) {
      setError(t('wiseArchitecture.selectAtLeastOneRepo'));
      return;
    }

    if (!selectedPitch) {
      setError(t('wiseArchitecture.selectPitchFirst'));
      return;
    }

    // Create new AbortController for this operation
    detectAbortController.current?.abort();
    detectAbortController.current = new AbortController();

    setDetectingStacks(true);
    setError(null);
    setDetectProgress({ percent: 0, message: t('wiseArchitecture.startingDetection') });

    try {
      // Use fallback method that handles sync/async automatically
      const response: DetectStacksResponse = await wiseArchitectureService.detectStacksWithFallback(
        {
          pitchId: selectedPitch.id,
          repositoryIds: selectedRepoIds,
          repositoryBranches: repoBranches,
        },
        (progress, message) => {
          setDetectProgress({ percent: progress, message: message || '' });
        },
        detectAbortController.current.signal
      );
      setDetectedStacks(response.detectedStacks);
      
      // Pre-select stacks with high confidence
      const highConfidenceStacks = response.detectedStacks
        .filter(s => s.confidence >= 70)
        .map(s => s.stackType);
      setSelectedStacks(highConfidenceStacks);
      
      setCurrentStep('stacks');
    } catch (err: any) {
      // Ignore errors from abort
      if (err.message !== 'Operation cancelled') {
        setError(err.response?.data?.message || err.message || t('wiseArchitecture.stackDetectionFailed'));
      }
    } finally {
      setDetectingStacks(false);
      setDetectProgress({ percent: 0, message: '' });
      detectAbortController.current = null;
    }
  };

  const handleGenerateSolution = async () => {
    if (!selectedPitch || selectedStacks.length === 0) {
      setError(t('wiseArchitecture.selectPitchAndStacks'));
      return;
    }

    // Create new AbortController for this operation
    analyzeAbortController.current?.abort();
    analyzeAbortController.current = new AbortController();

    setGeneratingSolution(true);
    setError(null);
    setAnalyzeProgress({ percent: 0, message: t('wiseArchitecture.startingAnalysis') });

    try {
      // Use fallback method that handles sync/async automatically
      const response: WiseArchitectureResponse = await wiseArchitectureService.analyzeWithFallback(
        {
          pitchId: selectedPitch.id,
          repositoryIds: selectedRepoIds,
          repositoryBranches: repoBranches,
          selectedStacks,
        },
        (progress, message) => {
          setAnalyzeProgress({ percent: progress, message: message || '' });
        },
        analyzeAbortController.current.signal
      );
      
      setSolution(response);
      setSessionId(response.sessionId);
      setCurrentStep('solution');
    } catch (err: any) {
      // Ignore errors from abort
      if (err.message !== 'Operation cancelled') {
        setError(err.response?.data?.message || err.message || t('wiseArchitecture.solutionGenerationFailed'));
      }
    } finally {
      setGeneratingSolution(false);
      setAnalyzeProgress({ percent: 0, message: '' });
      analyzeAbortController.current = null;
    }
  };

  const handleSendMessage = async () => {
    if (!chatInput.trim() || !sessionId) return;

    const userMessage: ChatMessage = {
      role: 'user',
      content: chatInput.trim(),
      timestamp: new Date(),
    };
    setChatMessages(prev => [...prev, userMessage]);
    setChatInput('');
    setSendingMessage(true);

    try {
      const response: FollowUpResponse = await wiseArchitectureService.followUp({
        sessionId,
        question: userMessage.content,
      });

      const assistantMessage: ChatMessage = {
        role: 'assistant',
        content: response.answer,
        copilotPrompt: response.copilotPrompt || undefined,
        timestamp: new Date(),
      };
      setChatMessages(prev => [...prev, assistantMessage]);
    } catch (err: any) {
      const errorMessage: ChatMessage = {
        role: 'assistant',
        content: err.response?.data?.message || t('wiseArchitecture.followUpFailed'),
        timestamp: new Date(),
      };
      setChatMessages(prev => [...prev, errorMessage]);
    } finally {
      setSendingMessage(false);
    }
  };

  const handleCopyPrompt = (prompt: string) => {
    navigator.clipboard.writeText(prompt);
    showToast(t('common.success'), 'success');
  };

  const handleGoToChat = () => {
    setCurrentStep('chat');
  };

  const handleStartOver = () => {
    setCurrentStep('pitch');
    setSelectedPitch(null);
    setSelectedRepoIds([]);
    setDetectedStacks([]);
    setSelectedStacks([]);
    setSolution(null);
    setSessionId(null);
    setChatMessages([]);
    setError(null);
  };

  const canProceed = (step: Step): boolean => {
    switch (step) {
      case 'pitch':
        return selectedPitch !== null;
      case 'repositories':
        return selectedRepoIds.length > 0;
      case 'stacks':
        return selectedStacks.length > 0;
      default:
        return true;
    }
  };

  const getCurrentStepIndex = () => STEPS.findIndex(s => s.key === currentStep);

  // Feature disabled view
  if (featureEnabled === false) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
            <ArrowLeft className="h-4 w-4 mr-1" />
            {t('common.back')}
          </Button>
          <Beaker className="h-6 w-6 text-muted-foreground" />
          <h1 className="text-2xl font-bold">{t('wiseArchitecture.title')}</h1>
          <Badge variant="secondary">{t('common.experimental')}</Badge>
        </div>

        <Alert variant="destructive">
          <AlertTriangle className="h-4 w-4" />
          <AlertTitle>{t('wiseArchitecture.featureDisabledTitle')}</AlertTitle>
          <AlertDescription>
            {t('wiseArchitecture.featureDisabledDesc')}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
            <ArrowLeft className="h-4 w-4 mr-1" />
            {t('common.back')}
          </Button>
          <Beaker className="h-6 w-6 text-primary" />
          <h1 className="text-2xl font-bold">{t('wiseArchitecture.title')}</h1>
          <Badge variant="secondary">{t('common.experimental')}</Badge>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" onClick={() => navigate('/rd/advice-history')}>
            <History className="h-4 w-4 mr-2" />
            {t('wiseArchitecture.viewHistory')}
          </Button>
          {currentStep !== 'pitch' && (
            <Button variant="outline" size="sm" onClick={handleStartOver}>
              <RefreshCw className="h-4 w-4 mr-2" />
              {t('wiseArchitecture.startOver')}
            </Button>
          )}
        </div>
      </div>

      {/* Progress Steps */}
      <div className="flex items-center justify-between">
        {STEPS.map((step, index) => (
          <React.Fragment key={step.key}>
            <div
              className={`flex items-center gap-2 ${
                index <= getCurrentStepIndex()
                  ? 'text-primary'
                  : 'text-muted-foreground'
              }`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium ${
                  index < getCurrentStepIndex()
                    ? 'bg-primary text-primary-foreground'
                    : index === getCurrentStepIndex()
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-muted text-muted-foreground'
                }`}
              >
                {index < getCurrentStepIndex() ? (
                  <CheckCircle2 className="h-4 w-4" />
                ) : (
                  index + 1
                )}
              </div>
              <span className="hidden md:inline text-sm font-medium">
                {t(step.titleKey)}
              </span>
            </div>
            {index < STEPS.length - 1 && (
              <ChevronRight className="h-4 w-4 text-muted-foreground flex-shrink-0" />
            )}
          </React.Fragment>
        ))}
      </div>

      {/* Error Alert */}
      {error && (
        <Alert variant="destructive">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {/* Step Content */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-4">
          {/* Step 1: Pitch Selection */}
          {currentStep === 'pitch' && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FileCode2 className="h-5 w-5" />
                  {t('wiseArchitecture.selectPitch')}
                </CardTitle>
                <CardDescription>
                  {t('wiseArchitecture.selectPitchDesc')}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <Select
                  value={selectedPitch?.id.toString() || ''}
                  onValueChange={handlePitchSelect}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('wiseArchitecture.choosePitch')} />
                  </SelectTrigger>
                  <SelectContent>
                    {pitches.map(pitch => (
                      <SelectItem key={pitch.id} value={pitch.id.toString()}>
                        <div className="flex items-center gap-2">
                          <span>{pitch.title}</span>
                          {pitch.appetiteDays && (
                            <Badge variant="outline" className="text-xs">
                              {Math.ceil(pitch.appetiteDays / 5)}w
                            </Badge>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                {selectedPitch && (
                  <Card className="bg-muted/50">
                    <CardContent className="pt-4">
                      <h4 className="font-medium mb-2">{selectedPitch.title}</h4>
                      <p className="text-sm text-muted-foreground mb-3">
                        {selectedPitch.problemStatement || selectedPitch.description}
                      </p>
                      {selectedPitch.appetiteDays && (
                        <div className="flex items-center gap-2 text-sm">
                          <Clock className="h-4 w-4" />
                          <span>{t('wiseArchitecture.appetite')}: {Math.ceil(selectedPitch.appetiteDays / 5)} {t('common.weeks')}</span>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                )}

                <Button
                  onClick={() => setCurrentStep('repositories')}
                  disabled={!canProceed('pitch')}
                  className="w-full"
                >
                  {t('common.next')}
                  <ChevronRight className="h-4 w-4 ml-2" />
                </Button>
              </CardContent>
            </Card>
          )}

          {/* Step 2: Repository Selection */}
          {currentStep === 'repositories' && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <GitBranch className="h-5 w-5" />
                  {t('wiseArchitecture.selectRepositories')}
                </CardTitle>
                <CardDescription>
                  {t('wiseArchitecture.selectRepositoriesDesc')}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {repositories.length === 0 ? (
                  <Alert>
                    <AlertDescription>
                      {t('wiseArchitecture.noRepositories')}
                    </AlertDescription>
                  </Alert>
                ) : (
                  <>
                    <div className="relative">
                      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      <Input
                        placeholder={t('common.search')}
                        value={repoSearchQuery}
                        onChange={(e) => setRepoSearchQuery(e.target.value)}
                        className="pl-10"
                      />
                    </div>
                    <ScrollArea className="h-[350px]">
                      <div className="space-y-2">
                        {filteredRepositories.map(repo => (
                        <div
                          key={repo.id}
                          className={`p-3 rounded-lg border transition-colors ${
                            selectedRepoIds.includes(repo.id)
                              ? 'border-primary bg-primary/5'
                              : 'border-border hover:bg-muted/50'
                          }`}
                        >
                          <div 
                            className="flex items-center gap-3 cursor-pointer"
                            onClick={() => handleRepoToggle(repo.id)}
                          >
                            <Checkbox
                              checked={selectedRepoIds.includes(repo.id)}
                              onCheckedChange={() => handleRepoToggle(repo.id)}
                            />
                            <Building2 className="h-4 w-4 text-muted-foreground" />
                            <div className="flex-1 min-w-0">
                              <p className="font-medium truncate">{repo.fullName}</p>
                            </div>
                          </div>
                          {selectedRepoIds.includes(repo.id) && (
                            <div className="mt-2 ml-8 flex items-center gap-2">
                              <GitBranch className="h-3.5 w-3.5 text-muted-foreground" />
                              <Input
                                placeholder={t('wiseArchitecture.branchPlaceholder') || 'Branch name'}
                                value={repoBranches[repo.id] || ''}
                                onChange={(e) => {
                                  e.stopPropagation();
                                  handleBranchChange(repo.id, e.target.value);
                                }}
                                onClick={(e) => e.stopPropagation()}
                                className="h-8 text-sm flex-1"
                              />
                            </div>
                          )}
                        </div>
                        ))}
                      </div>
                    </ScrollArea>
                  </>
                )}

                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    onClick={() => setCurrentStep('pitch')}
                    className="flex-1"
                  >
                    {t('common.back')}
                  </Button>
                  <Button
                    onClick={handleDetectStacks}
                    disabled={!canProceed('repositories') || detectingStacks}
                    className="flex-1"
                  >
                    {detectingStacks ? (
                      <>
                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        {detectProgress.message || t('wiseArchitecture.detectingStacks')}
                        {detectProgress.percent > 0 && ` (${detectProgress.percent}%)`}
                      </>
                    ) : (
                      <>
                        {t('wiseArchitecture.detectStacks')}
                        <ChevronRight className="h-4 w-4 ml-2" />
                      </>
                    )}
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Step 3: Stack Selection */}
          {currentStep === 'stacks' && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Layers className="h-5 w-5" />
                  {t('wiseArchitecture.selectStacks')}
                </CardTitle>
                <CardDescription>
                  {t('wiseArchitecture.selectStacksDesc')}
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {/* PERFORMANCE: Use memoized stacks grouped by category */}
                {(['Mobile', 'Backend', 'Web'] as const).map(category => {
                  const categoryStacks = stacksByCategory[category];
                  if (categoryStacks.length === 0) return null;

                  return (
                    <div key={category}>
                      <h4 className="text-sm font-medium mb-2">{category}</h4>
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                        {categoryStacks.map(stack => {
                          const Icon = getStackIcon(stack.stackType);
                          return (
                            <div
                              key={stack.stackType}
                              className={`flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors ${
                                selectedStacks.includes(stack.stackType)
                                  ? 'border-primary bg-primary/5'
                                  : 'border-border hover:bg-muted/50'
                              }`}
                              onClick={() => handleStackToggle(stack.stackType)}
                            >
                              <Checkbox
                                checked={selectedStacks.includes(stack.stackType)}
                                onCheckedChange={() => handleStackToggle(stack.stackType)}
                              />
                              <Icon className="h-4 w-4 text-muted-foreground" />
                              <div className="flex-1 min-w-0">
                                <p className="font-medium text-sm">
                                  {STACK_DISPLAY_NAMES[stack.stackType]}
                                </p>
                                <p className="text-xs text-muted-foreground">
                                  {t('wiseArchitecture.confidence')}: {stack.confidence}%
                                </p>
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  );
                })}

                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    onClick={() => setCurrentStep('repositories')}
                    className="flex-1"
                  >
                    {t('common.back')}
                  </Button>
                  <Button
                    onClick={handleGenerateSolution}
                    disabled={!canProceed('stacks') || generatingSolution}
                    className="flex-1"
                  >
                    {generatingSolution ? (
                      <>
                        <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                        {analyzeProgress.message || t('wiseArchitecture.generating')}
                        {analyzeProgress.percent > 0 && ` (${analyzeProgress.percent}%)`}
                      </>
                    ) : (
                      <>
                        <Brain className="h-4 w-4 mr-2" />
                        {t('wiseArchitecture.generateSolution')}
                      </>
                    )}
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}

          {/* Step 4: Solution Display */}
          {currentStep === 'solution' && solution && (
            <div className="space-y-4">
              {/* Appetite Check */}
              <Card className={solution.appetiteCheck.passed ? 'border-green-500/50' : 'border-amber-500/50'}>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    {solution.appetiteCheck.passed ? (
                      <CheckCircle2 className="h-5 w-5 text-green-500" />
                    ) : (
                      <AlertTriangle className="h-5 w-5 text-amber-500" />
                    )}
                    {t('wiseArchitecture.appetiteCheck')}
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-2 gap-4 mb-4">
                    <div>
                      <p className="text-sm text-muted-foreground">{t('wiseArchitecture.estimated')}</p>
                      <p className="text-2xl font-bold">{Math.ceil(solution.appetiteCheck.estimatedHours / 40)}w</p>
                    </div>
                    <div>
                      <p className="text-sm text-muted-foreground">{t('wiseArchitecture.available')}</p>
                      <p className="text-2xl font-bold">{Math.ceil(solution.appetiteCheck.availableHours / 40)}w</p>
                    </div>
                  </div>
                  {solution.appetiteCheck.message && (
                    <p className="text-sm">{solution.appetiteCheck.message}</p>
                  )}
                  {!solution.appetiteCheck.passed && solution.reducedScope && (
                    <div className="mt-4 p-3 bg-amber-500/10 rounded-lg">
                      <h4 className="font-medium mb-2">{t('wiseArchitecture.reducedScope')}</h4>
                      <p className="text-sm mb-2">{solution.reducedScope.explanation}</p>
                      {solution.reducedScope.deferredItems && solution.reducedScope.deferredItems.length > 0 && (
                        <ul className="text-sm list-disc list-inside space-y-1">
                          {solution.reducedScope.deferredItems.map((item: string, idx: number) => (
                            <li key={idx} className="text-muted-foreground">{item}</li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}
                </CardContent>
              </Card>

              {/* Context Sources Warning */}
              {solution.contextSources?.warnings && solution.contextSources.warnings.length > 0 && (
                <Alert variant="default" className="border-amber-500/50 bg-amber-500/10">
                  <AlertTriangle className="h-4 w-4 text-amber-500" />
                  <AlertTitle>{t('wiseArchitecture.contextWarnings.title')}</AlertTitle>
                  <AlertDescription>
                    <ul className="mt-2 space-y-1 text-sm">
                      {solution.contextSources.warnings.map((warning, idx) => (
                        <li key={idx} className="flex items-start gap-2">
                          <span className="text-amber-500">•</span>
                          <span>{warning}</span>
                        </li>
                      ))}
                    </ul>
                    <p className="mt-2 text-xs text-muted-foreground">
                      {t('wiseArchitecture.contextWarnings.hint')}
                    </p>
                  </AlertDescription>
                </Alert>
              )}

              {/* Solutions by Stack */}
              <Accordion type="multiple" defaultValue={selectedStacks} className="space-y-2">
                {solutionStackEntries.map(([stack, stackSolution]) => {
                  const Icon = getStackIcon(stack as TechStackType);
                  return (
                    <AccordionItem key={stack} value={stack} className="border rounded-lg">
                      <AccordionTrigger className="px-4">
                        <div className="flex items-center gap-2">
                          <Icon className="h-4 w-4" />
                          <span>{STACK_DISPLAY_NAMES[stack as TechStackType]}</span>
                        </div>
                      </AccordionTrigger>
                      <AccordionContent className="px-4 pb-4">
                        <div className="space-y-4">
                          {/* Architecture Overview (markdown) */}
                          {stackSolution.architectureDetail?.summary ? (
                            <Markdown content={stackSolution.architectureDetail.summary} className="text-sm" />
                          ) : (
                            <Markdown content={stackSolution.architectureOverview} className="text-sm" />
                          )}

                          {/* Architecture Components */}
                          {stackSolution.architectureDetail?.components && stackSolution.architectureDetail.components.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <Layers className="h-4 w-4" />
                                {t('wiseArchitecture.components', 'Components')}
                              </h5>
                              <div className="space-y-2">
                                {stackSolution.architectureDetail.components.map((comp, idx) => (
                                  <div key={idx} className="p-2 bg-muted/50 rounded-lg">
                                    <p className="font-medium text-sm">{comp.name}</p>
                                    <p className="text-xs text-muted-foreground">{comp.responsibility}</p>
                                    {comp.interactsWith && comp.interactsWith.length > 0 && (
                                      <div className="flex flex-wrap gap-1 mt-1">
                                        {comp.interactsWith.map((dep, i) => (
                                          <Badge key={i} variant="secondary" className="text-xs py-0">{dep}</Badge>
                                        ))}
                                      </div>
                                    )}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* API Contracts */}
                          {stackSolution.architectureDetail?.apiContracts && stackSolution.architectureDetail.apiContracts.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <Code2 className="h-4 w-4" />
                                {t('wiseArchitecture.apiContracts', 'API Contracts')}
                              </h5>
                              <div className="space-y-2">
                                {stackSolution.architectureDetail.apiContracts.map((api, idx) => (
                                  <div key={idx} className="p-2 bg-muted/50 rounded-lg font-mono text-xs">
                                    <span className="font-semibold text-primary">{api.method}</span>{' '}
                                    <span>{api.endpoint}</span>
                                    {api.description && (
                                      <p className="text-muted-foreground mt-1 font-sans">{api.description}</p>
                                    )}
                                    {(api.requestShape || api.responseShape) && (
                                      <div className="mt-1 flex gap-4">
                                        {api.requestShape && <span className="text-muted-foreground">Req: {api.requestShape}</span>}
                                        {api.responseShape && <span className="text-muted-foreground">Res: {api.responseShape}</span>}
                                      </div>
                                    )}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Data Model */}
                          {stackSolution.architectureDetail?.dataModel && stackSolution.architectureDetail.dataModel.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <Server className="h-4 w-4" />
                                {t('wiseArchitecture.dataModel', 'Data Model')}
                              </h5>
                              <div className="space-y-2">
                                {stackSolution.architectureDetail.dataModel.map((entity, idx) => (
                                  <div key={idx} className="p-2 bg-muted/50 rounded-lg">
                                    <p className="font-medium text-sm font-mono">{entity.entityName}</p>
                                    {entity.fields && entity.fields.length > 0 && (
                                      <div className="flex flex-wrap gap-1 mt-1">
                                        {entity.fields.map((f, i) => (
                                          <Badge key={i} variant="outline" className="text-xs font-mono py-0">{f}</Badge>
                                        ))}
                                      </div>
                                    )}
                                    {entity.relationships && entity.relationships.length > 0 && (
                                      <p className="text-xs text-muted-foreground mt-1">
                                        {entity.relationships.join(' | ')}
                                      </p>
                                    )}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Config Changes */}
                          {stackSolution.architectureDetail?.configChanges && stackSolution.architectureDetail.configChanges.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2">
                                {t('wiseArchitecture.configChanges', 'Configuration Changes')}
                              </h5>
                              <div className="space-y-1">
                                {stackSolution.architectureDetail.configChanges.map((cfg, idx) => (
                                  <div key={idx} className="flex items-start gap-2 text-xs font-mono p-1 bg-muted/30 rounded">
                                    <span className="font-semibold text-primary">{cfg.key}</span>
                                    <span>=</span>
                                    <span>{cfg.value}</span>
                                    {cfg.description && (
                                      <span className="text-muted-foreground font-sans ml-2">— {cfg.description}</span>
                                    )}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Reusable Services (enriched) */}
                          {stackSolution.reusableServices?.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <Package className="h-4 w-4" />
                                {t('wiseArchitecture.reusableServices')}
                              </h5>
                              <div className="space-y-2">
                                {stackSolution.reusableServices.map((svc, idx) => (
                                  <div key={idx} className="p-2 bg-muted/50 rounded-lg">
                                    <p className="font-medium text-sm">{svc.serviceName}</p>
                                    <p className="text-xs text-muted-foreground">{svc.description}</p>
                                    <p className="text-xs text-primary font-mono">{svc.filePath}</p>
                                    {svc.howToUse && (
                                      <p className="text-xs mt-1"><span className="font-medium">Usage:</span> {svc.howToUse}</p>
                                    )}
                                    {svc.importStatement && (
                                      <code className="text-xs bg-muted px-1.5 py-0.5 rounded mt-1 block font-mono">{svc.importStatement}</code>
                                    )}
                                    {svc.methodsToCall && svc.methodsToCall.length > 0 && (
                                      <div className="flex flex-wrap gap-1 mt-1">
                                        {svc.methodsToCall.map((m, i) => (
                                          <Badge key={i} variant="secondary" className="text-xs font-mono py-0">{m}</Badge>
                                        ))}
                                      </div>
                                    )}
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Recommended Libraries (enriched) */}
                          {stackSolution.recommendedLibraries?.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <Sparkles className="h-4 w-4" />
                                {t('wiseArchitecture.recommendedLibraries')}
                              </h5>
                              <div className="flex flex-wrap gap-2">
                                {stackSolution.recommendedLibraries.map((lib, idx) => (
                                  <Badge key={idx} variant="outline" className="py-1 gap-1">
                                    <span className="font-medium">{lib.name}</span>
                                    {lib.version && <span className="text-muted-foreground text-xs">v{lib.version}</span>}
                                    <span className="mx-1">—</span>
                                    <span className="text-muted-foreground">{lib.purpose}</span>
                                    {lib.alreadyInProject && (
                                      <Badge variant="secondary" className="ml-1 text-xs py-0">
                                        {t('wiseArchitecture.alreadyInProject', 'In project')}
                                      </Badge>
                                    )}
                                    {lib.documentationUrl && (
                                      <a href={lib.documentationUrl} target="_blank" rel="noopener noreferrer"
                                        className="text-primary hover:underline text-xs ml-1">docs</a>
                                    )}
                                  </Badge>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Implementation Steps (enriched) */}
                          {stackSolution.implementationSteps?.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <CheckSquare className="h-4 w-4" />
                                {t('wiseArchitecture.implementationSteps')}
                              </h5>
                              <div className="space-y-3">
                                {stackSolution.implementationSteps.map((step, idx) => (
                                  <div key={idx} className="p-3 bg-muted/30 rounded-lg">
                                    <div className="flex gap-3">
                                      <div className="w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-sm font-medium flex-shrink-0">
                                        {step.stepNumber}
                                      </div>
                                      <div className="flex-1 space-y-2">
                                        <div className="flex items-center justify-between">
                                          <p className="text-sm font-medium">{step.title}</p>
                                          {step.estimatedHours && (
                                            <Badge variant="outline" className="text-xs">
                                              <Clock className="h-3 w-3 mr-1" />
                                              ~{step.estimatedHours}h
                                            </Badge>
                                          )}
                                        </div>
                                        <Markdown content={step.description} className="text-xs text-muted-foreground" />

                                        {/* Dependencies */}
                                        {step.dependsOnSteps && step.dependsOnSteps.length > 0 && (
                                          <p className="text-xs text-muted-foreground">
                                            {t('wiseArchitecture.dependsOn', 'Depends on:')} {step.dependsOnSteps.map(d => `Step ${d}`).join(', ')}
                                          </p>
                                        )}

                                        {/* Files to create/modify */}
                                        {((step.filesToCreate && step.filesToCreate.length > 0) ||
                                          (step.filesToModify && step.filesToModify.length > 0)) && (
                                          <div className="flex flex-wrap gap-1">
                                            {step.filesToCreate?.map((f, i) => (
                                              <Badge key={`c-${i}`} variant="default" className="text-xs font-mono py-0">
                                                + {f}
                                              </Badge>
                                            ))}
                                            {step.filesToModify?.map((f, i) => (
                                              <Badge key={`m-${i}`} variant="secondary" className="text-xs font-mono py-0">
                                                ~ {f}
                                              </Badge>
                                            ))}
                                          </div>
                                        )}

                                        {/* Method Signatures */}
                                        {step.methodSignatures && step.methodSignatures.length > 0 && (
                                          <div className="space-y-1">
                                            {step.methodSignatures.map((sig, i) => (
                                              <code key={i} className="text-xs bg-muted px-1.5 py-0.5 rounded block font-mono">
                                                {sig}
                                              </code>
                                            ))}
                                          </div>
                                        )}

                                        {/* Sub-tasks */}
                                        {step.subTasks && step.subTasks.length > 0 && (
                                          <div className="space-y-1 ml-2 border-l-2 border-primary/20 pl-2">
                                            {step.subTasks.map((sub, i) => (
                                              <div key={i} className="text-xs">
                                                <p className="font-medium">{sub.task}</p>
                                                <p className="text-muted-foreground italic">{sub.acceptanceCriteria}</p>
                                              </div>
                                            ))}
                                          </div>
                                        )}
                                      </div>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Risk Factors */}
                          {stackSolution.riskFactors && stackSolution.riskFactors.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <AlertTriangle className="h-4 w-4 text-amber-500" />
                                {t('wiseArchitecture.riskFactors', 'Risk Factors')}
                              </h5>
                              <ul className="text-sm list-disc list-inside space-y-1">
                                {stackSolution.riskFactors.map((risk, idx) => (
                                  <li key={idx} className="text-muted-foreground">{risk}</li>
                                ))}
                              </ul>
                            </div>
                          )}

                          {/* Best Practices */}
                          {stackSolution.bestPractices?.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2">{t('wiseArchitecture.bestPractices')}</h5>
                              <ul className="text-sm list-disc list-inside space-y-1">
                                {stackSolution.bestPractices.map((practice, idx) => (
                                  <li key={idx}>{practice}</li>
                                ))}
                              </ul>
                            </div>
                          )}
                        </div>
                      </AccordionContent>
                    </AccordionItem>
                  );
                })}
              </Accordion>

              <Button onClick={handleGoToChat} className="w-full">
                <MessageSquare className="h-4 w-4 mr-2" />
                {t('wiseArchitecture.askFollowUp')}
              </Button>
            </div>
          )}

          {/* Step 5: Chat */}
          {currentStep === 'chat' && (
            <Card className="flex flex-col h-[600px]">
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MessageSquare className="h-5 w-5" />
                  {t('wiseArchitecture.followUpQuestions')}
                </CardTitle>
                <CardDescription>
                  {t('wiseArchitecture.followUpQuestionsDesc')}
                </CardDescription>
              </CardHeader>
              <CardContent className="flex-1 flex flex-col min-h-0">
                <ScrollArea className="flex-1 pr-4">
                  <div className="space-y-4">
                    {chatMessages.map((message, idx) => (
                      <div
                        key={idx}
                        className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
                      >
                        <div
                          className={`max-w-[80%] p-3 rounded-lg ${
                            message.role === 'user'
                              ? 'bg-primary text-primary-foreground'
                              : 'bg-muted'
                          }`}
                        >
                          {message.role === 'assistant' ? (
                            <div className="text-sm prose prose-sm dark:prose-invert max-w-none">
                              <Markdown content={message.content} />
                            </div>
                          ) : (
                            <p className="text-sm whitespace-pre-wrap">{message.content}</p>
                          )}
                          {message.copilotPrompt && (
                            <div className="mt-3 p-2 bg-background/50 rounded border">
                              <div className="flex items-center justify-between mb-1">
                                <span className="text-xs font-medium">
                                  {t('wiseArchitecture.copilotPrompt')}
                                </span>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  className="h-6 px-2"
                                  onClick={() => handleCopyPrompt(message.copilotPrompt!)}
                                >
                                  <Copy className="h-3 w-3" />
                                </Button>
                              </div>
                              <p className="text-xs text-muted-foreground font-mono">
                                {message.copilotPrompt}
                              </p>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                    {sendingMessage && (
                      <div className="flex justify-start">
                        <div className="bg-muted p-3 rounded-lg">
                          <Loader2 className="h-4 w-4 animate-spin" />
                        </div>
                      </div>
                    )}
                    <div ref={chatEndRef} />
                  </div>
                </ScrollArea>

                <Separator className="my-4" />

                <div className="flex gap-2">
                  <Input
                    value={chatInput}
                    onChange={e => setChatInput(e.target.value)}
                    placeholder={t('wiseArchitecture.typeQuestion')}
                    onKeyDown={e => e.key === 'Enter' && !e.shiftKey && handleSendMessage()}
                    disabled={sendingMessage}
                  />
                  <Button
                    onClick={handleSendMessage}
                    disabled={!chatInput.trim() || sendingMessage}
                  >
                    <Send className="h-4 w-4" />
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Sidebar - Context Info */}
        <div className="space-y-4">
          {selectedPitch && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">{t('wiseArchitecture.selectedPitch')}</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="font-medium">{selectedPitch.title}</p>
                {selectedPitch.appetiteDays && (
                  <Badge variant="outline" className="mt-2">
                    <Clock className="h-3 w-3 mr-1" />
                    {Math.ceil(selectedPitch.appetiteDays / 5)}w
                  </Badge>
                )}
              </CardContent>
            </Card>
          )}

          {selectedRepoIds.length > 0 && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">{t('wiseArchitecture.selectedRepos')}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-1">
                  {repositories
                    .filter(r => selectedRepoIds.includes(r.id))
                    .map(repo => (
                      <div key={repo.id} className="flex items-center gap-2 text-sm">
                        <Building2 className="h-3 w-3 text-muted-foreground" />
                        <span className="truncate">{repo.fullName}</span>
                      </div>
                    ))}
                </div>
              </CardContent>
            </Card>
          )}

          {selectedStacks.length > 0 && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">{t('wiseArchitecture.selectedStacks')}</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-1">
                  {selectedStacks.map(stack => {
                    const Icon = getStackIcon(stack);
                    return (
                      <Badge key={stack} variant="secondary" className="text-xs">
                        <Icon className="h-3 w-3 mr-1" />
                        {STACK_DISPLAY_NAMES[stack]}
                      </Badge>
                    );
                  })}
                </div>
              </CardContent>
            </Card>
          )}

          {currentStep === 'solution' && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">{t('wiseArchitecture.tips')}</CardTitle>
              </CardHeader>
              <CardContent>
                <ul className="text-xs text-muted-foreground space-y-2">
                  <li>• {t('wiseArchitecture.tipReview')}</li>
                  <li>• {t('wiseArchitecture.tipAsk')}</li>
                  <li>• {t('wiseArchitecture.tipCopilot')}</li>
                </ul>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
};

export default WiseArchitecturePage;
