import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
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
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { Checkbox } from './ui/checkbox';
import { Badge } from './ui/badge';
import { Alert, AlertDescription } from './ui/alert';
import { Separator } from './ui/separator';
import { 
  Rocket, 
  Loader2, 
  CheckCircle2,
  Circle,
  Lightbulb,
  ListTodo,
  ExternalLink
} from 'lucide-react';
import { RetroItem, Cycle, Pitch, RetroColumnType } from '../types';
import { retroService, ConvertToPitchRequest } from '../services/retroService';
import { cycleService } from '../services/cycleService';
import { cn } from '../lib/utils';

interface ConvertRetroToPitchDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  retroId: number;
  retroTitle: string;
  projectId: number;
  items: RetroItem[];
  onConversionComplete?: (pitch: Pitch) => void;
}

const columnTypeLabels: Record<RetroColumnType, { label: string; icon: typeof Lightbulb }> = {
  WENT_WELL: { label: 'Went Well', icon: CheckCircle2 },
  DID_NOT_GO_WELL: { label: 'Did Not Go Well', icon: Circle },
  TRY_NEXT: { label: 'Try Next', icon: Rocket },
  ACTIONS: { label: 'Actions', icon: ListTodo },
};

export function ConvertRetroToPitchDialog({
  open,
  onOpenChange,
  retroId,
  retroTitle,
  projectId,
  items,
  onConversionComplete,
}: ConvertRetroToPitchDialogProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [selectedItemIds, setSelectedItemIds] = useState<number[]>([]);
  const [customTitle, setCustomTitle] = useState('');
  const [additionalNotes, setAdditionalNotes] = useState('');
  const [appetiteDays, setAppetiteDays] = useState<number>(1);
  const [targetCycleId, setTargetCycleId] = useState<string>('');
  const [availableCycles, setAvailableCycles] = useState<Cycle[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdPitch, setCreatedPitch] = useState<Pitch | null>(null);

  // Get actionable items (TRY_NEXT and ACTIONS that aren't merged)
  const actionableItems = items.filter(
    (item) =>
      (item.columnType === 'TRY_NEXT' || item.columnType === 'ACTIONS') &&
      !item.mergedIntoId
  );

  // Load available cycles
  useEffect(() => {
    if (open && projectId) {
      cycleService.getByProject(projectId).then((res) => {
        // Filter to upcoming or active cycles
        const now = new Date();
        const futureCycles = res.data.filter((cycle) => {
          const endDate = new Date(cycle.endDate);
          return endDate >= now;
        });
        setAvailableCycles(futureCycles);
        
        // Auto-select first available cycle
        if (futureCycles.length > 0 && !targetCycleId) {
          setTargetCycleId(futureCycles[0].id.toString());
        }
      });
    }
  }, [open, projectId]);

  // Pre-select all actionable items by default
  useEffect(() => {
    if (open && actionableItems.length > 0) {
      setSelectedItemIds(actionableItems.map((item) => item.id));
    }
  }, [open, items]);

  // Generate default title
  useEffect(() => {
    if (open && !customTitle) {
      setCustomTitle(`Improvements from: ${retroTitle}`);
    }
  }, [open, retroTitle]);

  const handleItemToggle = (itemId: number) => {
    setSelectedItemIds((prev) =>
      prev.includes(itemId)
        ? prev.filter((id) => id !== itemId)
        : [...prev, itemId]
    );
  };

  const handleSelectAll = () => {
    if (selectedItemIds.length === actionableItems.length) {
      setSelectedItemIds([]);
    } else {
      setSelectedItemIds(actionableItems.map((item) => item.id));
    }
  };

  const handleSubmit = async () => {
    if (selectedItemIds.length === 0) {
      setError(t('retroBoard.convertToPitch.noItemsSelected', 'Please select at least one item to convert'));
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const request: ConvertToPitchRequest = {
        retroItemIds: selectedItemIds,
        targetCycleId: targetCycleId ? Number(targetCycleId) : undefined,
        customTitle: customTitle.trim() || undefined,
        additionalNotes: additionalNotes.trim() || undefined,
        appetiteDays,
      };

      const response = await retroService.convertToPitchDraft(retroId, request);
      setCreatedPitch(response.data);
      onConversionComplete?.(response.data);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to convert to pitch';
      setError(errorMessage);
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewPitch = () => {
    if (createdPitch) {
      navigate(`/pitches/${createdPitch.id}`);
      onOpenChange(false);
    }
  };

  const handleClose = () => {
    onOpenChange(false);
    // Reset state after animation
    setTimeout(() => {
      setCreatedPitch(null);
      setSelectedItemIds([]);
      setCustomTitle('');
      setAdditionalNotes('');
      setAppetiteDays(1);
      setError(null);
    }, 200);
  };

  // Success view after pitch is created
  if (createdPitch) {
    return (
      <Dialog open={open} onOpenChange={handleClose}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-green-600">
              <CheckCircle2 className="h-5 w-5" />
              {t('retroBoard.convertToPitch.success', 'Pitch Draft Created!')}
            </DialogTitle>
            <DialogDescription>
              {t('retroBoard.convertToPitch.successDesc', 'Your retro insights have been converted to a pitch draft.')}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="rounded-lg border bg-muted/50 p-4">
              <h4 className="font-medium">{createdPitch.title}</h4>
              <p className="text-sm text-muted-foreground mt-1">
                {t('retroBoard.convertToPitch.appetite', 'Appetite')}: {createdPitch.appetiteDays} {t('common.days', 'days')}
              </p>
              <p className="text-sm text-muted-foreground">
                {t('retroBoard.convertToPitch.cycle', 'Cycle')}: {createdPitch.cycleName}
              </p>
            </div>

            <Alert>
              <Lightbulb className="h-4 w-4" />
              <AlertDescription>
                {t('retroBoard.convertToPitch.nextSteps', 
                  'The pitch is now a draft. You can add more details, rabbitHoles, and risks before betting.')}
              </AlertDescription>
            </Alert>
          </div>

          <DialogFooter className="gap-2">
            <Button variant="outline" onClick={handleClose}>
              {t('common.close', 'Close')}
            </Button>
            <Button onClick={handleViewPitch}>
              <ExternalLink className="mr-2 h-4 w-4" />
              {t('retroBoard.convertToPitch.viewPitch', 'View Pitch')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Rocket className="h-5 w-5 text-primary" />
            {t('retroBoard.convertToPitch.title', 'Convert to Pitch Draft')}
          </DialogTitle>
          <DialogDescription>
            {t('retroBoard.convertToPitch.description', 
              'Turn your retro action items into a pitch for the next cycle. Selected items will be marked as acted on.')}
          </DialogDescription>
        </DialogHeader>

        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <div className="space-y-6 py-4">
          {/* Item Selection */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <Label className="text-base font-medium">
                {t('retroBoard.convertToPitch.selectItems', 'Select Items to Convert')}
              </Label>
              <Button
                variant="ghost"
                size="sm"
                onClick={handleSelectAll}
                className="text-xs"
              >
                {selectedItemIds.length === actionableItems.length
                  ? t('common.deselectAll', 'Deselect All')
                  : t('common.selectAll', 'Select All')}
              </Button>
            </div>

            {actionableItems.length === 0 ? (
              <Alert>
                <AlertDescription>
                  {t('retroBoard.convertToPitch.noActionItems', 
                    'No action items found. Add items to the "Try Next" or "Actions" columns first.')}
                </AlertDescription>
              </Alert>
            ) : (
              <div className="space-y-2 max-h-48 overflow-y-auto rounded-lg border p-3">
                {actionableItems.map((item) => {
                  const config = columnTypeLabels[item.columnType];
                  const Icon = config.icon;
                  return (
                    <div
                      key={item.id}
                      className={cn(
                        'flex items-start gap-3 p-2 rounded-md transition-colors cursor-pointer',
                        selectedItemIds.includes(item.id)
                          ? 'bg-primary/10 border border-primary/20'
                          : 'hover:bg-muted'
                      )}
                      onClick={() => handleItemToggle(item.id)}
                    >
                      <Checkbox
                        checked={selectedItemIds.includes(item.id)}
                        onCheckedChange={() => handleItemToggle(item.id)}
                        className="mt-0.5"
                      />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm">{item.content}</p>
                        <div className="flex items-center gap-2 mt-1">
                          <Badge variant="outline" className="text-xs">
                            <Icon className="h-3 w-3 mr-1" />
                            {config.label}
                          </Badge>
                          {item.voteCount > 0 && (
                            <Badge variant="secondary" className="text-xs">
                              {item.voteCount} {t('common.votes', 'votes')}
                            </Badge>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
            <p className="text-xs text-muted-foreground">
              {selectedItemIds.length} {t('retroBoard.convertToPitch.itemsSelected', 'items selected')}
            </p>
          </div>

          <Separator />

          {/* Pitch Details */}
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="pitch-title">
                {t('retroBoard.convertToPitch.pitchTitle', 'Pitch Title')}
              </Label>
              <Input
                id="pitch-title"
                value={customTitle}
                onChange={(e) => setCustomTitle(e.target.value)}
                placeholder={t('retroBoard.convertToPitch.titlePlaceholder', 'Enter a title for the pitch')}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="target-cycle">
                  {t('retroBoard.convertToPitch.targetCycle', 'Target Cycle')}
                </Label>
                <Select value={targetCycleId} onValueChange={setTargetCycleId}>
                  <SelectTrigger id="target-cycle">
                    <SelectValue placeholder={t('retroBoard.convertToPitch.selectCycle', 'Select a cycle')} />
                  </SelectTrigger>
                  <SelectContent>
                    {availableCycles.map((cycle) => (
                      <SelectItem key={cycle.id} value={cycle.id.toString()}>
                        {cycle.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="appetite">
                  {t('retroBoard.convertToPitch.appetite', 'Appetite')} ({t('common.days', 'days')})
                </Label>
                <Select
                  value={appetiteDays.toString()}
                  onValueChange={(v) => setAppetiteDays(Number(v))}
                >
                  <SelectTrigger id="appetite">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {[1, 2, 3, 5, 10, 15, 20, 30].map((days) => (
                      <SelectItem key={days} value={days.toString()}>
                        {days} {t('common.days', 'days')}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="additional-notes">
                {t('retroBoard.convertToPitch.additionalNotes', 'Additional Notes')} ({t('common.optional', 'Optional')})
              </Label>
              <Textarea
                id="additional-notes"
                value={additionalNotes}
                onChange={(e) => setAdditionalNotes(e.target.value)}
                placeholder={t('retroBoard.convertToPitch.notesPlaceholder', 
                  'Add any additional context or requirements...')}
                rows={3}
              />
            </div>
          </div>
        </div>

        <DialogFooter className="gap-2">
          <Button variant="outline" onClick={handleClose} disabled={submitting}>
            {t('common.cancel', 'Cancel')}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={submitting || selectedItemIds.length === 0 || actionableItems.length === 0}
          >
            {submitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                {t('common.creating', 'Creating...')}
              </>
            ) : (
              <>
                <Rocket className="mr-2 h-4 w-4" />
                {t('retroBoard.convertToPitch.create', 'Create Pitch Draft')}
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export default ConvertRetroToPitchDialog;
