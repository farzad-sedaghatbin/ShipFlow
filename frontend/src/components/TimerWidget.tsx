import React, { useState, useEffect } from 'react';
import { Square, X, Timer as TimerIcon, Clock } from 'lucide-react';
import timerService, { WorkLogTimer } from '../services/timerService';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { Alert, AlertDescription } from './ui/alert';
import { Badge } from './ui/badge';

interface TimerWidgetProps {
  onTimerStopped?: () => void;
}

const TimerWidget: React.FC<TimerWidgetProps> = ({ onTimerStopped }) => {
  const [activeTimer, setActiveTimer] = useState<WorkLogTimer | null>(null);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmDialog, setConfirmDialog] = useState<'stop' | 'cancel' | null>(null);

  // Load active timer on mount
  useEffect(() => {
    loadActiveTimer();
  }, []);

  // Update elapsed time every second
  useEffect(() => {
    if (!activeTimer) return;

    const interval = setInterval(() => {
      setElapsedSeconds((prev) => prev + 1);
    }, 1000);

    return () => clearInterval(interval);
  }, [activeTimer]);

  const loadActiveTimer = async () => {
    try {
      const timer = await timerService.getActiveTimer();
      if (timer) {
        setActiveTimer(timer);
        setElapsedSeconds(timer.elapsedSeconds);
      }
    } catch (err) {
      console.error('Failed to load active timer:', err);
    }
  };

  const handleStopTimer = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await timerService.stopTimer();
      setActiveTimer(null);
      setElapsedSeconds(0);
      setConfirmDialog(null);
      if (onTimerStopped) {
        onTimerStopped();
      }
      console.log(`Timer stopped. Created work log with ${response.hoursSpent} hours`);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to stop timer');
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
      setElapsedSeconds(0);
      setConfirmDialog(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to cancel timer');
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

  return (
    <>
      <Card className="fixed bottom-6 right-6 w-80 z-50 shadow-lg border-2 border-primary">
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <TimerIcon className="h-5 w-5 text-primary" />
              <CardTitle className="text-lg">Active Timer</CardTitle>
            </div>
            <Badge variant="default" className="gap-1">
              <Clock className="h-3 w-3" />
              Running
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {/* Timer Display */}
          <div className="text-center py-4 bg-muted rounded-lg">
            <div className="text-4xl font-bold text-primary tabular-nums">
              {formatTime(elapsedSeconds)}
            </div>
            <div className="text-sm text-muted-foreground mt-1">
              {formatHours(elapsedSeconds)} hours
            </div>
          </div>

          {/* Work Item */}
          <div>
            <div className="text-xs text-muted-foreground mb-1">
              {activeTimer.taskId ? 'Task' : 'Pitch'}:
            </div>
            <div className="font-medium">
              {activeTimer.taskTitle || activeTimer.pitchTitle}
            </div>
            {activeTimer.note && (
              <>
                <div className="text-xs text-muted-foreground mt-2 mb-1">
                  Note:
                </div>
                <div className="text-sm text-muted-foreground">
                  {activeTimer.note}
                </div>
              </>
            )}
          </div>

          {/* Actions */}
          <div className="flex gap-2">
            <Button
              className="flex-1"
              onClick={() => setConfirmDialog('stop')}
              disabled={loading}
            >
              <Square className="h-4 w-4 mr-2" />
              Stop & Log
            </Button>
            <Button
              variant="destructive"
              size="icon"
              onClick={() => setConfirmDialog('cancel')}
              disabled={loading}
              title="Cancel timer without logging"
            >
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* Error */}
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
        </CardContent>
      </Card>

      {/* Stop Confirmation Dialog */}
      <Dialog open={confirmDialog === 'stop'} onOpenChange={() => setConfirmDialog(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Stop Timer</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p>
              Stop the timer and create a work log entry for{' '}
              <strong>{formatHours(elapsedSeconds)} hours</strong> (rounded to nearest 0.25 hours)?
            </p>
            <p className="text-sm text-muted-foreground">
              This will be logged against: {activeTimer.taskTitle || activeTimer.pitchTitle}
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmDialog(null)} disabled={loading}>
              Cancel
            </Button>
            <Button onClick={handleStopTimer} disabled={loading}>
              Stop & Log Time
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Cancel Confirmation Dialog */}
      <Dialog open={confirmDialog === 'cancel'} onOpenChange={() => setConfirmDialog(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Cancel Timer</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p>Cancel the timer without creating a work log entry?</p>
            <p className="text-sm text-muted-foreground">
              {formatTime(elapsedSeconds)} of tracked time will be discarded.
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfirmDialog(null)} disabled={loading}>
              Keep Timer
            </Button>
            <Button variant="destructive" onClick={handleCancelTimer} disabled={loading}>
              Discard Timer
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default TimerWidget;
