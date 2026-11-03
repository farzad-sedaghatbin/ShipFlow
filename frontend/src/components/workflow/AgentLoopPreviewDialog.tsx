import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Loader2, Sparkles } from 'lucide-react';
import { automationService } from '../../services/automationService';
import type { AgentLoopPreview, WorkflowAutomation } from '../../types/automation';
import { getUserFriendlyError } from '../../utils/errorMessages';
import { Alert, AlertDescription } from '../ui/alert';
import { Button } from '../ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';

interface Props {
  automation: WorkflowAutomation;
  onClose: () => void;
}

/**
 * Shows what an agent loop would send right now — generated from the project's current data, sent
 * nowhere — so a rule can be checked before it starts posting to a channel.
 */
export default function AgentLoopPreviewDialog({ automation, onClose }: Props) {
  const { t } = useTranslation();
  const [preview, setPreview] = useState<AgentLoopPreview | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    automationService
      .preview(automation.id)
      .then((res) => {
        if (!cancelled) setPreview(res.data);
      })
      .catch((err) => {
        if (!cancelled) setError(getUserFriendlyError(err));
      });
    return () => {
      cancelled = true;
    };
  }, [automation.id]);

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-h-[90vh] max-w-xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{t('automation.preview.title', { name: automation.name })}</DialogTitle>
          <DialogDescription>{t('automation.preview.desc')}</DialogDescription>
        </DialogHeader>

        {!preview && !error && (
          <div className="flex items-center gap-2 py-6 text-sm text-muted-foreground" role="status">
            <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
            {t('automation.preview.loading')}
          </div>
        )}

        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {preview && (
          <div className="space-y-3" data-testid="agent-loop-preview">
            {!preview.wouldSend && (
              <Alert>
                <AlertDescription>{t('automation.preview.wouldSkip')}</AlertDescription>
              </Alert>
            )}
            <p className="font-medium">{preview.title}</p>
            {preview.summary && (
              <div className="rounded-md bg-muted p-3 text-sm">
                <p className="mb-1 flex items-center gap-1 text-xs font-medium text-muted-foreground">
                  <Sparkles className="h-3 w-3" aria-hidden="true" />
                  {t('automation.preview.agentSummary')}
                </p>
                {preview.summary}
              </div>
            )}
            <pre className="whitespace-pre-wrap break-words rounded-md border p-3 text-xs">{preview.facts}</pre>
            <p className="text-xs text-muted-foreground">{t('automation.preview.factsNote')}</p>
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={onClose}>
            {t('common.close')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
