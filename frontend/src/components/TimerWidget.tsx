import { useState, useEffect, useRef, forwardRef, useImperativeHandle } from 'react';
import { Square, X, Timer as TimerIcon, Clock, MinusCircle, Maximize2, Pause, Play, AlertTriangle } from 'lucide-react';
import { Input } from './ui/input';
import { toast } from 'sonner';
import { useTranslation } from 'react-i18next';
import timerService, { WorkLogTimer } from '../services/timerService';
import { workLogService } from '../services/workLogService';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Textarea } from './ui/textarea';
import { Label } from './ui/label';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Alert, AlertDescription } from './ui/alert';
import { Badge } from './ui/badge';

export interface TimerWidgetHandle {
  focusAndExpand: () => void;
}

interface TimerWidgetProps {
  onTimerStopped?: () => void;
}

const TimerWidget = forwardRef<TimerWidgetHandle, TimerWidgetProps>(({ onTimerStopped }, ref) => {
  const { t } = useTranslation();
  const [activeTimer, setActiveTimer] = useState<WorkLogTimer | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmDialog, setConfirmDialog] = useState<'stop' | 'cancel' | null>(null);
  const [isMinimized, setIsMinimized] = useState(false);
  const [workLogNote, setWorkLogNote] = useState('');
  const [customHours, setCustomHours] = useState('');

  const LONG_RUNNING_THRESHOLD_SECS = 8 * 3600; // auto-stop at 8 h
  const autoStoppedRef = useRef(false);
  const cardRef = useRef<HTMLDivElement>(null);

  // Drift-free elapsed: store server snapshot + wall-clock reference
  const baseElapsedRef = useRef<number>(0);
  const mountTimestampRef = useRef<number>(Date.now());
  const [displayedElapsed, setDisplayedElapsed] = useState(0);

  // Expose focusAndExpand so external components can scroll + un-minimize the widget
  useImperativeHandle(ref, () => ({
    focusAndExpand: () => {
      setIsMinimized(false);
      setTimeout(() => {
        cardRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
        cardRef.current?.focus();
      }, 50);
    },
  }));

  // Load active timer on mount
  useEffect(() => {
    loadActiveTimer();
  }, []);

  // Ticker: only count wall-clock delta when timer is RUNNING
  useEffect(() => {
    if (!activeTimer) return;

    if (activeTimer.status === 'PAUSED') {
      // Show frozen elapsed when paused — no interval needed
      setDisplayedElapsed(baseElapsedRef.current);
      return;
    }

    // RUNNING: display = baseElapsed + (Date.now() - mountTimestamp) / 1000
    const tick = () => {
      const deltaSecs = Math.floor((Date.now() - mountTimestampRef.current) / 1000);
      setDisplayedElapsed(baseElapsedRef.current + deltaSecs);
    };

    tick(); // immediate first render
    const interval = setInterval(tick, 1000);
    return () => clearInterval(interval);
  }, [activeTimer]);

  // Auto-stop: when elapsed reaches 8 h, log exactly 8 h and close the timer
  useEffect(() => {
    if (
      displayedElapsed >= LONG_RUNNING_THRESHOLD_SECS &&
      activeTimer &&
      !autoStoppedRef.current &&
      !loading
    ) {
      autoStoppedRef.current = true;
      (async () => {
        try {
          await workLogService.createMy({
            pitchId: activeTimer.pitchId,
            taskId: activeTimer.taskId,
            date: new Date().toISOString().split('T')[0],
            hoursSpent: 8,
            note: activeTimer.note?.trim() || undefined,
          });
          await timerService.cancelTimer();
          setActiveTimer(null);
          setDisplayedElapsed(0);
          toast.info(t('timerWidget.autoStopped'));
          if (onTimerStopped) onTimerStopped();
        } catch {
          autoStoppedRef.current = false; // allow retry
        }
      })();
    }
  }, [displayedElapsed, activeTimer, loading]);

  const applyTimerSnapshot = (timer: WorkLogTimer) => {
    setActiveTimer(timer);
    baseElapsedRef.current = timer.elapsedSeconds;
    mountTimestampRef.current = Date.now();
    setDisplayedElapsed(timer.elapsedSeconds);
    setWorkLogNote(timer.note || '');
  };

  const loadActiveTimer = async () => {
    try {
      const timer = await timerService.getActiveTimer();
      if (timer) {
        applyTimerSnapshot(timer);
      }
    } catch (err) {
      console.error('Failed to load active timer:', err);
    }
  };

  const handleOpenStopDialog = () => {
    setWorkLogNote(activeTimer?.note || '');
    // Pre-fill with computed hours so user can correct if timer ran too long
    const computed = Math.round((displayedElapsed / 3600) * 4) / 4;
    setCustomHours(computed.toFixed(2));
    setConfirmDialog('stop');
  };

  const handleStopTimer = async () => {
    if (!activeTimer) return;

    try {
      setLoading(true);
      setError(null);

      // Use user-edited hours if valid, otherwise fall back to timer-computed
      const parsed = parseFloat(customHours);
      const hours = !isNaN(parsed) && parsed > 0
        ? Math.round(parsed * 4) / 4  // snap to nearest 0.25
        : Math.round((displayedElapsed / 3600) * 4) / 4;

      // Create work log with the custom note
      await workLogService.createMy({
        pitchId: activeTimer.pitchId,
        taskId: activeTimer.taskId,
        date: new Date().toISOString().split('T')[0],
        hoursSpent: hours,
        note: workLogNote.trim() || undefined,
      });

      // Cancel the timer (no work log created by timer)
      await timerService.cancelTimer();

      setActiveTimer(null);
      setDisplayedElapsed(0);
      setWorkLogNote('');
      setConfirmDialog(null);
      if (onTimerStopped) {
        onTimerStopped();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || t('errors.stopTimerFailed'));
    } finally {
      setLoading(false);
    }
  };

  const handleCancelTimer = async () => {
    try {
      setLoading(true);
      setError(null);
      await timerService.cancelTimer();
      setActiveTimer(null);
      setDisplayedElapsed(0);
      setConfirmDialog(null);
    } catch (err: any) {
      setError(err.response?.data?.message || t('errors.cancelTimerFailed'));
    } finally {
      setLoading(false);
    }
  };

  const handlePauseTimer = async () => {
    try {
      setLoading(true);
      setError(null);
      const updated = await timerService.pauseTimer();
      applyTimerSnapshot(updated);
    } catch (err: any) {
      setError(err.response?.data?.message || t('timerWidget.pauseFailed'));
    } finally {
      setLoading(false);
    }
  };

  const handleResumeTimer = async () => {
    try {
      setLoading(true);
      setError(null);
      const updated = await timerService.resumeTimer();
      applyTimerSnapshot(updated);
    } catch (err: any) {
      setError(err.response?.data?.message || t('timerWidget.resumeFailed'));
    } finally {
      setLoading(false);
    }
  };

  const formatTime = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  const formatHours = (seconds: number): string => {
    const hours = seconds / 3600;
    return hours.toFixed(2);
  };

  if (!activeTimer) {
    return null;
  }

  const isPaused = activeTimer.status === 'PAUSED';

  return (
    <>
      <Card ref={cardRef} tabIndex={-1} className="fixed bottom-6 right-6 w-80 z-50 shadow-lg border-2 border-primary focus:outline-none focus:ring-2 focus:ring-ring">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <TimerIcon className="h-5 w-5 text-primary" />
              <CardTitle className="text-lg">{t('timerWidget.activeTimer')}</CardTitle>
            </div>
            <div className="flex items-center gap-2">
              <Badge variant={isPaused ? 'secondary' : 'default'} className="gap-1">
                <Clock className="h-3 w-3" />
                {isPaused ? t('timerWidget.paused') : t('timerWidget.running')}
              </Badge>
              <Button
                variant="ghost"
                size="icon"
                className="h-6 w-6"
                onClick={() => setIsMinimized(!isMinimized)}
                title={isMinimized ? t('timerWidget.expand') : t('timerWidget.minimize')}
                aria-label={isMinimized ? t('timerWidget.expand') : t('timerWidget.minimize')}
              >
                {isMinimized ? <Maximize2 className="h-4 w-4" aria-hidden="true" /> : <MinusCircle className="h-4 w-4" aria-hidden="true" />}
              </Button>
            </div>
          </div>
        </CardHeader>
        {!isMinimized && (
        <CardContent className="space-y-4">
          {/* Long-running warning */}
          {displayedElapsed > LONG_RUNNING_THRESHOLD_SECS && (
            <Alert variant="destructive" className="py-2">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription className="text-xs">
                {t('timerWidget.longRunningWarning')}
              </AlertDescription>
            </Alert>
          )}

          {/* Timer Display */}
          <div className={`text-center py-4 bg-muted rounded-lg${isPaused ? ' opacity-70' : ''}`}>
            <div className="text-4xl font-bold text-primary tabular-nums">
              {formatTime(displayedElapsed)}
            </div>
            <div className="text-sm text-muted-foreground mt-1">
              {formatHours(displayedElapsed)} {t('timerWidget.hours')}
              {isPaused && (
                <span className="ml-2 text-xs text-muted-foreground">({t('timerWidget.paused')})</span>
              )}
            </div>
          </div>

          {/* Work Item */}
          <div>
            <div className="text-xs text-muted-foreground mb-1">
              {activeTimer.taskId ? t('timerWidget.task') : t('timerWidget.pitch')}:
            </div>
            <div className="font-medium">
              {activeTimer.taskTitle || activeTimer.pitchTitle}
            </div>
            {activeTimer.note && (
              <>
                <div className="text-xs text-muted-foreground mt-2 mb-1">
                  {t('timerWidget.note')}:
                </div>
                <div className="text-sm text-muted-foreground">
                  {activeTimer.note}
                </div>
              </>
            )}
          </div>

          {/* Actions */}
          <div className="flex gap-2">
            {isPaused ? (
              <Button
                variant="outline"
                size="icon"
                onClick={handleResumeTimer}
                disabled={loading}
                title={t('timerWidget.resume')}
                aria-label={t('timerWidget.resume')}
              >
                <Play className="h-4 w-4" aria-hidden="true" />
              </Button>
            ) : (
              <Button
                variant="outline"
                size="icon"
                onClick={handlePauseTimer}
                disabled={loading}
                title={t('timerWidget.pause')}
                aria-label={t('timerWidget.pause')}
              >
                <Pause className="h-4 w-4" aria-hidden="true" />
              </Button>
            )}
            <Button
              className="flex-1"
              onClick={handleOpenStopDialog}
              disabled={loading}
            >
              <Square className="h-4 w-4 mr-2" />
              {t('timerWidget.stopAndLog')}
            </Button>
            <Button
              variant="destructive"
              size="icon"
              onClick={() => setConfirmDialog('cancel')}
              disabled={loading}
              title={t('timerWidget.cancelTitle')}
              aria-label={t('timerWidget.cancelTitle')}
            >
              <X className="h-4 w-4" aria-hidden="true" />
            </Button>
          </div>

          {/* Error */}
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
        </CardContent>
        )}
      </Card>

      {/* Stop Confirmation Dialog */}
      <Dialog open={confirmDialog === 'stop'} onOpenChange={() => setConfirmDialog(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>{t('timerWidget.stopDialog.title')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <p className="text-sm text-muted-foreground">
                {t('timerWidget.stopDialog.for')}: <strong>{activeTimer?.taskTitle || activeTimer?.pitchTitle}</strong>
              </p>
              <p className="text-xs text-muted-foreground mt-0.5">
                {t('timerWidget.stopDialog.timerRan')}: {formatTime(displayedElapsed)}
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="hours-input">{t('timerWidget.stopDialog.hoursToLog')}</Label>
              <div className="flex items-center gap-2">
                <Input
                  id="hours-input"
                  type="number"
                  min="0.25"
                  max="24"
                  step="0.25"
                  value={customHours}
                  onChange={(e) => setCustomHours(e.target.value)}
                  className="w-28"
                />
                <span className="text-sm text-muted-foreground">{t('timerWidget.hours')}</span>
              </div>
              <p className="text-xs text-muted-foreground">{t('timerWidget.stopDialog.hoursHint')}</p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="worklog-note">{t('timerWidget.stopDialog.notes')}</Label>
              <Textarea
                id="worklog-note"
                placeholder={t('timerWidget.stopDialog.notesPlaceholder')}
                value={workLogNote}
                onChange={(e) => setWorkLogNote(e.target.value)}
                rows={4}
                className="resize-none"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmDialog(null)} disabled={loading}>
              {t('timerWidget.stopDialog.cancel')}
            </Button>
            <Button onClick={handleStopTimer} disabled={loading}>
              {t('timerWidget.stopDialog.confirm')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Cancel Confirmation Dialog */}
      <Dialog open={confirmDialog === 'cancel'} onOpenChange={() => setConfirmDialog(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('timerWidget.cancelDialog.title')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p>{t('timerWidget.cancelDialog.message')}</p>
            <p className="text-sm text-muted-foreground">
              {formatTime(displayedElapsed)} {t('timerWidget.cancelDialog.discardMessage')}
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmDialog(null)} disabled={loading}>
              {t('timerWidget.cancelDialog.keep')}
            </Button>
            <Button variant="destructive" onClick={handleCancelTimer} disabled={loading}>
              {t('timerWidget.cancelDialog.discard')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
});

TimerWidget.displayName = 'TimerWidget';

export default TimerWidget;
