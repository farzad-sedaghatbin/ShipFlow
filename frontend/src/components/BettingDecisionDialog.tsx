import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { 
  Dialog, 
  DialogContent, 
  DialogHeader, 
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from './ui/dialog';
import { Button } from './ui/button';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import { 
  Select, 
  SelectContent, 
  SelectItem, 
  SelectTrigger, 
  SelectValue 
} from './ui/select';
import { Slider } from './ui/slider';
import { Badge } from './ui/badge';
import { Separator } from './ui/separator';
import { 
  Check, 
  X, 
  Clock, 
  Pencil, 
  AlertTriangle,
  Loader2 
} from 'lucide-react';
import { Pitch, Cycle } from '../types';
import { 
  bettingDecisionService, 
  BettingDecisionDTO,
  RecordBettingDecisionRequest 
} from '../services/bettingDecisionService';
import { cn } from '../lib/utils';

type DecisionType = 'COMMITTED' | 'REJECTED' | 'DEFERRED' | 'NEEDS_SHAPING';

interface BettingDecisionDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  pitch: Pitch | null;
  cycleId: number;
  futureCycles?: Cycle[];
  existingDecision?: BettingDecisionDTO | null;
  onDecisionRecorded: () => void;
}

const decisionConfig: Record<DecisionType, { 
  icon: typeof Check; 
  color: string; 
  bgColor: string;
  label: string;
  description: string;
}> = {
  COMMITTED: {
    icon: Check,
    color: 'text-green-600',
    bgColor: 'bg-green-50 dark:bg-green-950/30 border-green-200 dark:border-green-800',
    label: 'Commit',
    description: 'Bet on this pitch for the current cycle',
  },
  REJECTED: {
    icon: X,
    color: 'text-red-600',
    bgColor: 'bg-red-50 dark:bg-red-950/30 border-red-200 dark:border-red-800',
    label: 'Reject',
    description: "Not right for this cycle (won't be reconsidered)",
  },
  DEFERRED: {
    icon: Clock,
    color: 'text-amber-600',
    bgColor: 'bg-amber-50 dark:bg-amber-950/30 border-amber-200 dark:border-amber-800',
    label: 'Defer',
    description: 'Interesting but not now - reconsider later',
  },
  NEEDS_SHAPING: {
    icon: Pencil,
    color: 'text-blue-600',
    bgColor: 'bg-blue-50 dark:bg-blue-950/30 border-blue-200 dark:border-blue-800',
    label: 'Needs Shaping',
    description: 'Good idea but needs more work before betting',
  },
};

export function BettingDecisionDialog({
  open,
  onOpenChange,
  pitch,
  cycleId,
  futureCycles = [],
  existingDecision,
  onDecisionRecorded,
}: BettingDecisionDialogProps) {
  const { t } = useTranslation();
  const [decision, setDecision] = useState<DecisionType | null>(
    existingDecision?.decision || null
  );
  const [reason, setReason] = useState(existingDecision?.reason || '');
  const [commitmentLevel, setCommitmentLevel] = useState<number>(
    existingDecision?.commitmentLevel || 80
  );
  const [deferralReason, setDeferralReason] = useState(
    existingDecision?.deferralReason || ''
  );
  const [deferredUntilCycleId, setDeferredUntilCycleId] = useState<string>(
    existingDecision?.deferredUntilCycleId?.toString() || ''
  );
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!decision || !pitch) return;

    setSubmitting(true);
    setError(null);

    try {
      // For DEFERRED decisions, use deferralReason as the reason if no general reason provided
      const effectiveReason = reason.trim() || 
        (decision === 'DEFERRED' ? deferralReason.trim() : '') || 
        undefined;
      
      const request: RecordBettingDecisionRequest = {
        cycleId,
        pitchId: pitch.id,
        decision,
        reason: effectiveReason,
        commitmentLevel: decision === 'COMMITTED' ? commitmentLevel : undefined,
        deferralReason: decision === 'DEFERRED' ? deferralReason.trim() || undefined : undefined,
        deferredUntilCycleId: decision === 'DEFERRED' && deferredUntilCycleId 
          ? Number(deferredUntilCycleId) 
          : undefined,
      };

      await bettingDecisionService.recordDecision(request);
      onDecisionRecorded();
      onOpenChange(false);
      
      // Reset form
      setDecision(null);
      setReason('');
      setCommitmentLevel(80);
      setDeferralReason('');
      setDeferredUntilCycleId('');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to record decision');
    } finally {
      setSubmitting(false);
    }
  };

  const getAppetiteLabel = (days: number) => {
    if (days <= 7) return 'Small Batch';
    if (days <= 14) return 'Medium Batch';
    if (days <= 28) return 'Big Batch';
    return 'Full Cycle';
  };

  if (!pitch) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <span>{t('bettingTablePage.decisionDialog.title')}</span>
          </DialogTitle>
          <DialogDescription>
            {t('bettingTablePage.decisionDialog.subtitle')}
          </DialogDescription>
        </DialogHeader>

        {/* Pitch Summary */}
        <div className="p-4 bg-muted/50 rounded-lg mb-4">
          <h3 className="font-semibold text-lg mb-1">{pitch.title}</h3>
          <div className="flex items-center gap-2">
            <Badge variant="outline">{pitch.appetiteDays} {t('common.days')}</Badge>
            <span className="text-sm text-muted-foreground">
              {getAppetiteLabel(pitch.appetiteDays)}
            </span>
          </div>
          {pitch.description && (
            <p className="text-sm text-muted-foreground mt-2 line-clamp-2">
              {pitch.description}
            </p>
          )}
        </div>

        {/* Decision Options */}
        <div className="space-y-3">
          <Label>{t('bettingTablePage.decisionDialog.whatDecision')}</Label>
          <div className="grid grid-cols-2 gap-2">
            {(Object.keys(decisionConfig) as DecisionType[]).map((type) => {
              const config = decisionConfig[type];
              const Icon = config.icon;
              const isSelected = decision === type;
              
              return (
                <button
                  key={type}
                  onClick={() => setDecision(type)}
                  className={cn(
                    'flex items-center gap-2 p-3 rounded-lg border-2 transition-all text-left',
                    isSelected 
                      ? config.bgColor + ' border-current'
                      : 'border-muted hover:border-muted-foreground/50'
                  )}
                >
                  <Icon className={cn('h-5 w-5', isSelected ? config.color : 'text-muted-foreground')} />
                  <div>
                    <p className={cn('font-medium text-sm', isSelected && config.color)}>
                      {config.label}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {config.description}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        </div>

        <Separator />

        {/* Decision-specific options */}
        {decision === 'COMMITTED' && (
          <div className="space-y-3">
            <Label>{t('bettingTablePage.decisionDialog.commitmentLevel')}: {commitmentLevel}%</Label>
            <Slider
              value={[commitmentLevel]}
              onValueChange={([value]) => setCommitmentLevel(value)}
              min={50}
              max={100}
              step={5}
              className="w-full"
            />
            <p className="text-xs text-muted-foreground">
              {commitmentLevel >= 90 && t('bettingTablePage.decisionDialog.commitmentHigh')}
              {commitmentLevel >= 70 && commitmentLevel < 90 && t('bettingTablePage.decisionDialog.commitmentMedium')}
              {commitmentLevel < 70 && t('bettingTablePage.decisionDialog.commitmentLow')}
            </p>
          </div>
        )}

        {decision === 'DEFERRED' && (
          <div className="space-y-3">
            <div>
              <Label>{t('bettingTablePage.decisionDialog.deferralReason')}</Label>
              <Textarea
                value={deferralReason}
                onChange={(e) => setDeferralReason(e.target.value)}
                placeholder={t('bettingTablePage.decisionDialog.deferralReasonPlaceholder')}
                className="mt-1.5"
                rows={2}
              />
            </div>
            {futureCycles.length > 0 && (
              <div>
                <Label>{t('bettingTablePage.decisionDialog.selectFutureCycle')}</Label>
                <Select 
                  value={deferredUntilCycleId} 
                  onValueChange={setDeferredUntilCycleId}
                >
                  <SelectTrigger className="mt-1.5">
                    <SelectValue placeholder={t('bettingTablePage.decisionDialog.selectFutureCyclePlaceholder')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="">No specific cycle</SelectItem>
                    {futureCycles.map((cycle) => (
                      <SelectItem key={cycle.id} value={String(cycle.id)}>
                        {cycle.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </div>
        )}

        {/* Rationale */}
        <div>
          <Label>{t('bettingTablePage.decisionDialog.decisionRationale')}</Label>
          <Textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder={t('bettingTablePage.decisionDialog.decisionRationalePlaceholder')}
            className="mt-1.5"
            rows={3}
          />
        </div>

        {error && (
          <div className="flex items-center gap-2 p-3 bg-destructive/10 text-destructive rounded-lg">
            <AlertTriangle className="h-4 w-4" />
            <span className="text-sm">{error}</span>
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t('bettingTablePage.decisionDialog.cancel')}
          </Button>
          <Button 
            onClick={handleSubmit} 
            disabled={!decision || submitting}
          >
            {submitting ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Recording...
              </>
            ) : (
              t('bettingTablePage.decisionDialog.submit')
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default BettingDecisionDialog;
