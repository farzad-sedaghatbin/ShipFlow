import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Check, Loader2, X } from 'lucide-react';
import { agentActionService, AgentActionProposal, AgentActionStatus } from '../services/agentActionService';
import { useToast } from '../contexts';
import { Badge } from './ui/badge';
import { Button } from './ui/button';

const STATUS_KEYS: Record<AgentActionStatus, string> = {
  PROPOSED: 'proposed',
  CONFIRMED: 'confirmed',
  EXECUTED: 'executed',
  FAILED: 'failed',
  CANCELLED: 'cancelled',
  EXPIRED: 'expired',
};

const STATUS_VARIANT: Record<AgentActionStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  PROPOSED: 'outline',
  CONFIRMED: 'secondary',
  EXECUTED: 'default',
  FAILED: 'destructive',
  CANCELLED: 'secondary',
  EXPIRED: 'secondary',
};

interface AgentProposalsProps {
  proposals: AgentActionProposal[];
  /** Called with the updated proposal after it was applied or dismissed. */
  onUpdated: (proposal: AgentActionProposal) => void;
}

/**
 * The changes the ShipFlow Agent proposed in a comment. Nothing is applied until the person who
 * asked clicks Apply; everyone else sees the same list read-only. The line shown for each change is
 * the server's rendering of the real tool and arguments, so what you confirm is what runs.
 */
export default function AgentProposals({ proposals, onUpdated }: AgentProposalsProps) {
  const { t } = useTranslation();
  const { showError } = useToast();
  const [busyId, setBusyId] = useState<number | null>(null);

  const act = async (proposal: AgentActionProposal, action: 'confirm' | 'cancel') => {
    setBusyId(proposal.id);
    try {
      const res =
        action === 'confirm'
          ? await agentActionService.confirm(proposal.id)
          : await agentActionService.cancel(proposal.id);
      onUpdated(res.data);
    } catch (error) {
      console.error(`Agent proposal ${action} failed:`, error);
      showError(t(action === 'confirm' ? 'comments.proposals.confirmFailed' : 'comments.proposals.cancelFailed'));
    } finally {
      setBusyId(null);
    }
  };

  if (proposals.length === 0) return null;

  return (
    <div className="mt-2 space-y-2 rounded-md border bg-background p-3" data-testid="agent-proposals">
      <p className="text-xs font-medium text-muted-foreground">{t('comments.proposals.title')}</p>
      <ul className="space-y-2">
        {proposals.map((p) => (
          <li key={p.id} className="space-y-1" data-testid={`agent-proposal-${p.id}`}>
            <div className="flex flex-wrap items-center gap-2">
              <code className="break-all rounded bg-muted px-1.5 py-0.5 text-xs">{p.label}</code>
              <Badge variant={STATUS_VARIANT[p.status]} className="px-2 py-0 text-xs">
                {t(`comments.proposals.status.${STATUS_KEYS[p.status]}`)}
              </Badge>
            </div>
            {p.summary && <p className="text-xs text-muted-foreground">{p.summary}</p>}
            {p.resultMessage && (p.status === 'EXECUTED' || p.status === 'FAILED') && (
              <p className={`text-xs ${p.status === 'FAILED' ? 'text-destructive' : 'text-muted-foreground'}`}>
                {p.resultMessage}
                {p.status === 'EXECUTED' && ` — ${t('comments.proposals.refreshHint')}`}
              </p>
            )}
            {p.canConfirm && (
              <div className="flex gap-2 pt-1">
                <Button size="sm" disabled={busyId !== null} onClick={() => act(p, 'confirm')}>
                  {busyId === p.id ? (
                    <Loader2 className="mr-1 h-3.5 w-3.5 animate-spin" aria-hidden="true" />
                  ) : (
                    <Check className="mr-1 h-3.5 w-3.5" aria-hidden="true" />
                  )}
                  {t('comments.proposals.apply')}
                </Button>
                <Button size="sm" variant="outline" disabled={busyId !== null} onClick={() => act(p, 'cancel')}>
                  <X className="mr-1 h-3.5 w-3.5" aria-hidden="true" />
                  {t('comments.proposals.dismiss')}
                </Button>
              </div>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
