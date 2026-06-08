import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Sparkles, Loader2, Wand2, RotateCcw, CheckCircle, AlertCircle } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Button } from './ui/button';
import { Textarea } from './ui/textarea';
import { Badge } from './ui/badge';
import { Skeleton } from './ui/skeleton';
import { Alert, AlertTitle, AlertDescription } from './ui/alert';
import { Label } from './ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { aiPitchWriterService, PitchWriterResponse } from '../services/aiPitchWriterService';

interface AIPitchWriterModalProps {
  open: boolean;
  onClose: () => void;
  onAccept: (draft: PitchWriterResponse) => void;
  projectContext?: string;
  projectId?: number;
}

type ModalState = 'input' | 'loading' | 'result' | 'error';

const MAX_PROBLEM_LENGTH = 500;

export default function AIPitchWriterModal({
  open,
  onClose,
  onAccept,
  projectContext,
  projectId,
}: AIPitchWriterModalProps) {
  const { t } = useTranslation();

  const [state, setState] = useState<ModalState>('input');
  const [problemDescription, setProblemDescription] = useState('');
  const [appetiteHint, setAppetiteHint] = useState<string>('any');
  const [result, setResult] = useState<PitchWriterResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState('');

  const handleClose = () => {
    // Only reset to input state; keep form values so the user can reopen
    onClose();
  };

  const handleOpenChange = (open: boolean) => {
    if (!open) handleClose();
  };

  const handleGenerate = async () => {
    if (!problemDescription.trim()) return;

    setState('loading');
    setErrorMessage('');

    try {
      const appetiteValue =
        appetiteHint === '14' ? 14 : appetiteHint === '28' ? 28 : undefined;

      const response = await aiPitchWriterService.generate({
        problemDescription: problemDescription.trim(),
        appetiteHint: appetiteValue,
        projectContext: projectContext,
        projectId: projectId,
      });

      setResult(response);
      setState('result');
    } catch (err: unknown) {
      const msg =
        err instanceof Error
          ? err.message
          : t('aiPitchWriter.errorTitle');
      setErrorMessage(msg);
      setState('error');
    }
  };

  const handleRetry = () => {
    setState('input');
    setResult(null);
    setErrorMessage('');
  };

  const handleAccept = () => {
    if (result) {
      onAccept(result);
      onClose();
      // Reset for next use
      setProblemDescription('');
      setAppetiteHint('any');
      setResult(null);
      setState('input');
    }
  };

  const renderInput = () => (
    <div className="space-y-5">
      {/* Problem description */}
      <div className="space-y-2">
        <Label htmlFor="ai-problem-desc">
          {t('aiPitchWriter.problemLabel')}
        </Label>
        <Textarea
          id="ai-problem-desc"
          value={problemDescription}
          onChange={(e) => {
            if (e.target.value.length <= MAX_PROBLEM_LENGTH) {
              setProblemDescription(e.target.value);
            }
          }}
          placeholder={t('aiPitchWriter.problemPlaceholder')}
          rows={5}
          aria-describedby="ai-problem-counter"
        />
        <p
          id="ai-problem-counter"
          className="text-xs text-muted-foreground text-right"
        >
          {problemDescription.length} / {MAX_PROBLEM_LENGTH}
        </p>
      </div>

      {/* Appetite selector */}
      <div className="space-y-2">
        <Label>{t('aiPitchWriter.appetiteLabel')}</Label>
        <Select value={appetiteHint} onValueChange={setAppetiteHint}>
          <SelectTrigger className="w-full sm:w-[280px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="any">{t('aiPitchWriter.appetiteAny')}</SelectItem>
            <SelectItem value="14">{t('aiPitchWriter.appetite14')}</SelectItem>
            <SelectItem value="28">{t('aiPitchWriter.appetite28')}</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Generate button */}
      <div className="flex justify-end pt-2">
        <Button
          onClick={handleGenerate}
          disabled={!problemDescription.trim()}
        >
          <Wand2 className="h-4 w-4 mr-2" />
          {t('aiPitchWriter.generateButton')}
        </Button>
      </div>
    </div>
  );

  const renderLoading = () => (
    <div className="space-y-6 py-4" aria-live="polite" aria-label={t('aiPitchWriter.generating')}>
      {/* Spinner + heading */}
      <div className="flex flex-col items-center gap-3 text-center">
        <div className="relative">
          <Sparkles className="h-8 w-8 text-purple-400 absolute -top-1 -right-1 animate-pulse" />
          <Loader2 className="h-12 w-12 animate-spin text-primary" />
        </div>
        <p className="text-base font-medium">{t('aiPitchWriter.generating')}</p>
        <p className="text-sm text-muted-foreground">{t('aiPitchWriter.generatingNote')}</p>
      </div>

      {/* Skeleton rows */}
      <div className="space-y-3 rounded-lg border border-border p-4">
        <Skeleton className="h-5 w-2/3" />
        <Skeleton className="h-4 w-full" />
        <Skeleton className="h-4 w-5/6" />
        <Skeleton className="h-4 w-3/4" />
        <div className="pt-2 space-y-2">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-4/5" />
        </div>
      </div>
    </div>
  );

  const renderResult = () => {
    if (!result) return null;

    return (
      <div className="space-y-4">
        {/* Result header */}
        <div className="flex items-center gap-2">
          <CheckCircle className="h-5 w-5 text-green-500 flex-shrink-0" />
          <h3 className="font-semibold text-base">{t('aiPitchWriter.resultTitle')}</h3>
        </div>

        {/* Draft preview card */}
        <div className="rounded-lg border border-border bg-muted/30 p-4 space-y-4 max-h-[380px] overflow-y-auto">
          {/* Title */}
          <div>
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-1">
              {t('aiPitchWriter.fieldTitle')}
            </p>
            <p className="font-semibold text-foreground">{result.title}</p>
          </div>

          {/* Appetite badge */}
          <div>
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-1">
              {t('aiPitchWriter.fieldAppetite')}
            </p>
            <Badge variant="secondary">
              {result.appetiteDays} {t('aiPitchWriter.days')}
            </Badge>
          </div>

          {/* Problem Statement */}
          <div>
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-1">
              {t('aiPitchWriter.fieldProblem')}
            </p>
            <p className="text-sm text-foreground whitespace-pre-wrap">{result.problemStatement}</p>
          </div>

          {/* Solution */}
          <div>
            <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-1">
              {t('aiPitchWriter.fieldSolution')}
            </p>
            <p className="text-sm text-foreground whitespace-pre-wrap">{result.solution}</p>
          </div>

          {/* Rabbit Holes */}
          {result.rabbitHoles && (
            <div>
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-1">
                {t('aiPitchWriter.fieldRabbitHoles')}
              </p>
              <p className="text-sm text-foreground whitespace-pre-wrap">{result.rabbitHoles}</p>
            </div>
          )}

          {/* No-Gos */}
          {result.noGos && (
            <div>
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-1">
                {t('aiPitchWriter.fieldNoGos')}
              </p>
              <p className="text-sm text-foreground whitespace-pre-wrap">{result.noGos}</p>
            </div>
          )}

          {/* Risks */}
          {result.risks && (
            <div>
              <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-1">
                {t('aiPitchWriter.fieldRisks')}
              </p>
              <p className="text-sm text-foreground whitespace-pre-wrap">{result.risks}</p>
            </div>
          )}
        </div>

        {/* Action buttons */}
        <div className="flex justify-end gap-2 pt-1">
          <Button variant="outline" onClick={handleRetry}>
            <RotateCcw className="h-4 w-4 mr-2" />
            {t('aiPitchWriter.tryAgain')}
          </Button>
          <Button onClick={handleAccept}>
            <CheckCircle className="h-4 w-4 mr-2" />
            {t('aiPitchWriter.useThisDraft')}
          </Button>
        </div>
      </div>
    );
  };

  const renderError = () => (
    <div className="space-y-4">
      <Alert variant="destructive">
        <AlertCircle className="h-4 w-4" />
        <AlertTitle>{t('aiPitchWriter.errorTitle')}</AlertTitle>
        <AlertDescription>{errorMessage}</AlertDescription>
      </Alert>
      <div className="flex justify-end">
        <Button variant="outline" onClick={handleRetry}>
          <RotateCcw className="h-4 w-4 mr-2" />
          {t('aiPitchWriter.errorRetry')}
        </Button>
      </div>
    </div>
  );

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-purple-500" />
            {t('aiPitchWriter.modalTitle')}
          </DialogTitle>
          <DialogDescription>
            {t('aiPitchWriter.modalSubtitle')}
          </DialogDescription>
        </DialogHeader>

        <div className="mt-2">
          {state === 'input' && renderInput()}
          {state === 'loading' && renderLoading()}
          {state === 'result' && renderResult()}
          {state === 'error' && renderError()}
        </div>
      </DialogContent>
    </Dialog>
  );
}
