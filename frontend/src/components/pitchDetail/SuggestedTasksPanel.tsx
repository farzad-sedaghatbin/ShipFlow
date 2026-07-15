import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { Sparkles, Loader2, Wand2, RotateCcw, CheckCircle2, AlertCircle, Palette } from 'lucide-react';
import { toast } from 'sonner';
import { Task, TaskSuggestion, Discipline } from '../../types';
import { pitchTaskSuggestionService } from '../../services/pitchTaskSuggestionService';
import { taskService } from '../../services/taskService';
import { getUserFriendlyError } from '../../utils/errorMessages';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { Checkbox } from '../ui/checkbox';
import { Alert, AlertDescription } from '../ui/alert';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogFooter } from '../ui/dialog';

interface SuggestedTasksPanelProps {
  pitchId: number;
  cycleId?: number | null;
  onTasksCreated: (tasks: Task[]) => void;
}

type PanelState = 'idle' | 'loading' | 'result' | 'error';

const DISCIPLINE_KEY: Record<Discipline, string> = {
  DESIGN: 'suggestedTasksPanel.disciplineDesign',
  BACKEND: 'suggestedTasksPanel.disciplineBackend',
  MOBILE: 'suggestedTasksPanel.disciplineMobile',
  QA: 'suggestedTasksPanel.disciplineQa',
};

export function SuggestedTasksPanel({ pitchId, cycleId, onTasksCreated }: SuggestedTasksPanelProps) {
  const { t } = useTranslation();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [state, setState] = useState<PanelState>('idle');
  const [suggestions, setSuggestions] = useState<TaskSuggestion[]>([]);
  const [figmaContextUsed, setFigmaContextUsed] = useState(false);
  const [selectedIndices, setSelectedIndices] = useState<Set<number>>(new Set());
  const [errorMessage, setErrorMessage] = useState('');
  const [creating, setCreating] = useState(false);

  const { data: status } = useQuery({
    queryKey: ['pitch-task-suggestions-status'],
    queryFn: pitchTaskSuggestionService.getStatus,
  });

  if (!status?.available) return null;

  const resetPanel = () => {
    setState('idle');
    setSuggestions([]);
    setFigmaContextUsed(false);
    setSelectedIndices(new Set());
    setErrorMessage('');
  };

  const handleOpenChange = (open: boolean) => {
    setDialogOpen(open);
    if (!open) resetPanel();
  };

  const handleGenerate = async () => {
    setState('loading');
    setErrorMessage('');
    try {
      const response = await pitchTaskSuggestionService.generate(pitchId);
      setSuggestions(response.suggestions);
      setFigmaContextUsed(response.figmaContextUsed);
      setSelectedIndices(new Set(response.suggestions.map((_, i) => i)));
      setState('result');
    } catch (err) {
      setErrorMessage(getUserFriendlyError(err, t('suggestedTasksPanel.errorGeneric')));
      setState('error');
    }
  };

  const toggleSelect = (index: number) => {
    setSelectedIndices(prev => {
      const next = new Set(prev);
      if (next.has(index)) next.delete(index);
      else next.add(index);
      return next;
    });
  };

  const handleCreateSelected = async () => {
    if (!cycleId || selectedIndices.size === 0) return;
    const tasksToCreate = suggestions.filter((_, i) => selectedIndices.has(i));
    setCreating(true);
    try {
      const res = await taskService.bulkCreate({ pitchId, cycleId, tasks: tasksToCreate });
      onTasksCreated(res.data.createdTasks);
      if (res.data.failureCount > 0) {
        toast.error(
          t('suggestedTasksPanel.createPartialFailure', { count: res.data.failureCount })
        );
      } else {
        toast.success(t('suggestedTasksPanel.createSuccess', { count: res.data.successCount }));
      }
      setDialogOpen(false);
      resetPanel();
    } catch (err) {
      toast.error(getUserFriendlyError(err, t('suggestedTasksPanel.errorGeneric')));
    } finally {
      setCreating(false);
    }
  };

  return (
    <>
      <Button size="sm" variant="outline" onClick={() => setDialogOpen(true)}>
        <Sparkles className="h-4 w-4 mr-1 text-purple-500" />
        {t('suggestedTasksPanel.triggerButton')}
      </Button>

      <Dialog open={dialogOpen} onOpenChange={handleOpenChange}>
        <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-purple-500" />
              {t('suggestedTasksPanel.dialogTitle')}
            </DialogTitle>
            <DialogDescription>{t('suggestedTasksPanel.dialogSubtitle')}</DialogDescription>
          </DialogHeader>

          {state === 'idle' && (
            <div className="flex justify-end pt-2">
              <Button onClick={handleGenerate}>
                <Wand2 className="h-4 w-4 mr-2" />
                {t('suggestedTasksPanel.generateButton')}
              </Button>
            </div>
          )}

          {state === 'loading' && (
            <div className="flex flex-col items-center gap-3 text-center py-8" aria-live="polite">
              <Loader2 className="h-10 w-10 animate-spin text-primary" />
              <p className="text-sm text-muted-foreground">{t('suggestedTasksPanel.loading')}</p>
            </div>
          )}

          {state === 'error' && (
            <div className="space-y-4">
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{errorMessage}</AlertDescription>
              </Alert>
              <div className="flex justify-end">
                <Button variant="outline" onClick={() => setState('idle')}>
                  <RotateCcw className="h-4 w-4 mr-2" />
                  {t('common.retry', 'Retry')}
                </Button>
              </div>
            </div>
          )}

          {state === 'result' && (
            <div className="space-y-4">
              {!figmaContextUsed && (
                <Alert>
                  <Palette className="h-4 w-4" />
                  <AlertDescription>{t('suggestedTasksPanel.figmaUnavailableNote')}</AlertDescription>
                </Alert>
              )}

              {suggestions.length === 0 ? (
                <p className="text-sm text-muted-foreground py-4">
                  {t('suggestedTasksPanel.emptyState')}
                </p>
              ) : (
                <div className="max-h-[420px] overflow-y-auto pr-3">
                  <div className="space-y-2">
                    {suggestions.map((suggestion, index) => (
                      <div
                        key={index}
                        className="rounded-lg border border-border p-3 space-y-2"
                      >
                        <div className="flex items-start gap-3">
                          <Checkbox
                            checked={selectedIndices.has(index)}
                            onCheckedChange={() => toggleSelect(index)}
                            className="mt-0.5"
                          />
                          <div className="flex-1 min-w-0 space-y-1.5">
                            <div className="flex items-center gap-2 flex-wrap">
                              <span className="font-medium text-sm">{suggestion.title}</span>
                              <Badge variant={suggestion.sourceContext === 'PITCH_DESIGN' ? 'info' : 'outline'}>
                                {t(
                                  suggestion.sourceContext === 'PITCH_DESIGN'
                                    ? 'suggestedTasksPanel.sourcePitchDesign'
                                    : 'suggestedTasksPanel.sourcePitch'
                                )}
                              </Badge>
                              {suggestion.estimateHours != null && (
                                <Badge variant="secondary">
                                  {suggestion.estimateHours}h
                                </Badge>
                              )}
                            </div>
                            <p className="text-sm text-muted-foreground">{suggestion.description}</p>
                            <div className="flex items-center gap-1.5 flex-wrap">
                              {suggestion.disciplines.map(discipline => (
                                <Badge key={discipline} variant="outline" className="text-[10px] px-1.5 py-0">
                                  {t(DISCIPLINE_KEY[discipline])}
                                </Badge>
                              ))}
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              <DialogFooter>
                <Button variant="outline" onClick={handleGenerate}>
                  <RotateCcw className="h-4 w-4 mr-2" />
                  {t('suggestedTasksPanel.regenerate')}
                </Button>
                <Button
                  onClick={handleCreateSelected}
                  disabled={selectedIndices.size === 0 || !cycleId || creating}
                >
                  <CheckCircle2 className="h-4 w-4 mr-2" />
                  {creating
                    ? t('common.saving', 'Saving…')
                    : t('suggestedTasksPanel.createSelected', { count: selectedIndices.size })}
                </Button>
              </DialogFooter>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
