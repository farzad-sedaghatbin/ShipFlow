import React, { useState, useEffect, useRef } from 'react';
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
  Loader2,
  MessageSquare,
  RefreshCw,
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

  // Step 3: Stack detection and selection
  const [detectedStacks, setDetectedStacks] = useState<DetectedStack[]>([]);
  const [selectedStacks, setSelectedStacks] = useState<TechStackType[]>([]);
  const [detectingStacks, setDetectingStacks] = useState(false);

  // Step 4: Solution
  const [solution, setSolution] = useState<WiseArchitectureResponse | null>(null);
  const [generatingSolution, setGeneratingSolution] = useState(false);

  // Step 5: Chat
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [sendingMessage, setSendingMessage] = useState(false);

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
    setSelectedRepoIds(prev =>
      prev.includes(repoId)
        ? prev.filter(id => id !== repoId)
        : [...prev, repoId]
    );
  };

  const handleDetectStacks = async () => {
    if (selectedRepoIds.length === 0) {
      setError(t('wiseArchitecture.selectAtLeastOneRepo'));
      return;
    }

    if (!selectedPitch) {
      setError(t('wiseArchitecture.selectPitchFirst'));
      return;
    }

    setDetectingStacks(true);
    setError(null);

    try {
      const response: DetectStacksResponse = await wiseArchitectureService.detectStacks({
        pitchId: selectedPitch.id,
        repositoryIds: selectedRepoIds,
      });
      setDetectedStacks(response.detectedStacks);
      
      // Pre-select stacks with high confidence
      const highConfidenceStacks = response.detectedStacks
        .filter(s => s.confidence >= 70)
        .map(s => s.stackType);
      setSelectedStacks(highConfidenceStacks);
      
      setCurrentStep('stacks');
    } catch (err: any) {
      setError(err.response?.data?.message || t('wiseArchitecture.stackDetectionFailed'));
    } finally {
      setDetectingStacks(false);
    }
  };

  const handleStackToggle = (stack: TechStackType) => {
    setSelectedStacks(prev =>
      prev.includes(stack)
        ? prev.filter(s => s !== stack)
        : [...prev, stack]
    );
  };

  const handleGenerateSolution = async () => {
    if (!selectedPitch || selectedStacks.length === 0) {
      setError(t('wiseArchitecture.selectPitchAndStacks'));
      return;
    }

    setGeneratingSolution(true);
    setError(null);

    try {
      const response: WiseArchitectureResponse = await wiseArchitectureService.analyze({
        pitchId: selectedPitch.id,
        repositoryIds: selectedRepoIds,
        selectedStacks,
      });
      
      setSolution(response);
      setSessionId(response.sessionId);
      setCurrentStep('solution');
    } catch (err: any) {
      setError(err.response?.data?.message || t('wiseArchitecture.solutionGenerationFailed'));
    } finally {
      setGeneratingSolution(false);
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
        {currentStep !== 'pitch' && (
          <Button variant="outline" size="sm" onClick={handleStartOver}>
            <RefreshCw className="h-4 w-4 mr-2" />
            {t('wiseArchitecture.startOver')}
          </Button>
        )}
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
                  <ScrollArea className="h-[300px]">
                    <div className="space-y-2">
                      {repositories.map(repo => (
                        <div
                          key={repo.id}
                          className={`flex items-center gap-3 p-3 rounded-lg border cursor-pointer transition-colors ${
                            selectedRepoIds.includes(repo.id)
                              ? 'border-primary bg-primary/5'
                              : 'border-border hover:bg-muted/50'
                          }`}
                          onClick={() => handleRepoToggle(repo.id)}
                        >
                          <Checkbox
                            checked={selectedRepoIds.includes(repo.id)}
                            onCheckedChange={() => handleRepoToggle(repo.id)}
                          />
                          <Building2 className="h-4 w-4 text-muted-foreground" />
                          <div className="flex-1 min-w-0">
                            <p className="font-medium truncate">{repo.fullName}</p>
                            <p className="text-xs text-muted-foreground">
                              {repo.defaultBranch}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </ScrollArea>
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
                        {t('wiseArchitecture.detectingStacks')}
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
                {/* Group stacks by category */}
                {['Mobile', 'Backend', 'Web'].map(category => {
                  const categoryStacks = detectedStacks.filter(
                    s => getStackCategory(s.stackType) === category
                  );
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
                        {t('wiseArchitecture.generating')}
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
                {Object.entries(solution.solutions).map(([stack, stackSolution]) => {
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
                          {/* Overview */}
                          <p className="text-sm">{stackSolution.architectureOverview}</p>

                          {/* Reusable Services */}
                          {stackSolution.reusableServices.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <Package className="h-4 w-4" />
                                {t('wiseArchitecture.reusableServices')}
                              </h5>
                              <div className="space-y-2">
                                {stackSolution.reusableServices.map((service, idx) => (
                                  <div key={idx} className="p-2 bg-muted/50 rounded-lg">
                                    <p className="font-medium text-sm">{service.serviceName}</p>
                                    <p className="text-xs text-muted-foreground">{service.description}</p>
                                    <p className="text-xs text-primary">{service.filePath}</p>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Recommended Libraries */}
                          {stackSolution.recommendedLibraries.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <Sparkles className="h-4 w-4" />
                                {t('wiseArchitecture.recommendedLibraries')}
                              </h5>
                              <div className="flex flex-wrap gap-2">
                                {stackSolution.recommendedLibraries.map((lib, idx) => (
                                  <Badge key={idx} variant="outline" className="py-1">
                                    <span className="font-medium">{lib.name}</span>
                                    <span className="mx-1">-</span>
                                    <span className="text-muted-foreground">{lib.purpose}</span>
                                  </Badge>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Implementation Steps */}
                          {stackSolution.implementationSteps.length > 0 && (
                            <div>
                              <h5 className="text-sm font-medium mb-2 flex items-center gap-2">
                                <CheckSquare className="h-4 w-4" />
                                {t('wiseArchitecture.implementationSteps')}
                              </h5>
                              <div className="space-y-2">
                                {stackSolution.implementationSteps.map((step, idx) => (
                                  <div key={idx} className="flex gap-3 p-2 bg-muted/30 rounded-lg">
                                    <div className="w-6 h-6 rounded-full bg-primary/10 text-primary flex items-center justify-center text-sm font-medium flex-shrink-0">
                                      {step.stepNumber}
                                    </div>
                                    <div className="flex-1">
                                      <p className="text-sm font-medium">{step.title}</p>
                                      <p className="text-xs text-muted-foreground">{step.description}</p>
                                      {step.estimatedHours && (
                                        <p className="text-xs text-muted-foreground">
                                          ~{step.estimatedHours}h
                                        </p>
                                      )}
                                    </div>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Best Practices */}
                          {stackSolution.bestPractices.length > 0 && (
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
                          <p className="text-sm whitespace-pre-wrap">{message.content}</p>
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
