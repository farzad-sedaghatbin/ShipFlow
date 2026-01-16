import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { safeParseId } from '../utils/validation';
import { AlertTriangle, Zap, XCircle, CheckCircle, ArrowLeft, Loader2 } from 'lucide-react';
import { circuitBreakerService } from '../services/circuitBreakerService';
import { CircuitBreaker } from '../types/circuit-breaker';
import { useToast } from '../contexts';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Textarea } from '../components/ui/textarea';
import { Slider } from '../components/ui/slider';

export default function CircuitBreakerMonitor() {
  const { t } = useTranslation();
  const { id: idParam } = useParams<{ id: string }>();
  const cycleId = safeParseId(idParam);
  const { showSuccess, showError } = useToast();
  // i18n ready
  if (false) console.log(t('circuitBreaker.title'));

  const [overflows, setOverflows] = useState<CircuitBreaker[]>([]);
  const [triggered, setTriggered] = useState<CircuitBreaker[]>([]);
  const [loading, setLoading] = useState(true);
  const [threshold, setThreshold] = useState(80);

  const [killDialog, setKillDialog] = useState<{ open: boolean; pitch: CircuitBreaker | null }>({
    open: false,
    pitch: null
  });
  const [killReason, setKillReason] = useState('');

  const [triggerDialog, setTriggerDialog] = useState<{ open: boolean; pitch: CircuitBreaker | null }>({
    open: false,
    pitch: null
  });
  const [triggerReason, setTriggerReason] = useState('');

  useEffect(() => {
    if (cycleId) {
      loadData();
    }
  }, [cycleId, threshold]);

  const loadData = async () => {
    if (!cycleId) return;

    try {
      const [overflowRes, triggeredRes] = await Promise.all([
        circuitBreakerService.detectOverflow(cycleId, threshold),
        circuitBreakerService.getTriggered(cycleId)
      ]);

      setOverflows(overflowRes.data);
      setTriggered(triggeredRes.data);
    } catch (error) {
      showError('Failed to load circuit breaker data');
    } finally {
      setLoading(false);
    }
  };

  const handleTrigger = async () => {
    if (!triggerDialog.pitch || !triggerReason.trim()) return;

    try {
      await circuitBreakerService.trigger(triggerDialog.pitch.pitchId, triggerReason);
      showSuccess('Circuit breaker triggered');
      setTriggerDialog({ open: false, pitch: null });
      setTriggerReason('');
      loadData();
    } catch (error) {
      showError('Failed to trigger circuit breaker');
    }
  };

  const handleKill = async () => {
    if (!killDialog.pitch || !killReason.trim()) return;

    try {
      await circuitBreakerService.kill(killDialog.pitch.pitchId, killReason);
      showSuccess('Pitch permanently cancelled due to overflow');
      setKillDialog({ open: false, pitch: null });
      setKillReason('');
      loadData();
    } catch (error) {
      showError('Failed to kill pitch');
    }
  };

  const getSeverityColor = (utilization: number) => {
    if (utilization >= 100) return 'text-red-600 bg-red-50 border-red-200';
    if (utilization >= 90) return 'text-orange-600 bg-orange-50 border-orange-200';
    if (utilization >= 80) return 'text-yellow-600 bg-yellow-50 border-yellow-200';
    return 'text-blue-600 bg-blue-50 border-blue-200';
  };

  const getProgressColor = (utilization: number) => {
    if (utilization >= 100) return 'bg-red-500';
    if (utilization >= 90) return 'bg-orange-500';
    if (utilization >= 80) return 'bg-yellow-500';
    return 'bg-blue-500';
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link to={`/cycles/${cycleId}`}>
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-5 w-5" />
            </Button>
          </Link>
          <div>
            <h1 className="text-3xl font-bold flex items-center gap-2">
              <Zap className="h-8 w-8 text-yellow-500" />
              Circuit Breaker Monitor
            </h1>
            <p className="text-muted-foreground">
              Shape Up safety valve - detect and kill overflowing pitches
            </p>
          </div>
        </div>
      </div>

      {/* Threshold Control */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Detection Threshold</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4">
            <span className="text-sm font-medium min-w-[100px]">
              {threshold}% of appetite
            </span>
            <Slider
              value={[threshold]}
              onValueChange={(values) => setThreshold(values[0])}
              min={50}
              max={150}
              step={5}
              className="flex-1"
            />
          </div>
          <p className="text-xs text-muted-foreground mt-2">
            Shows pitches that have consumed {threshold}% or more of their time budget
          </p>
        </CardContent>
      </Card>

      {/* Triggered Circuit Breakers */}
      {triggered.length > 0 && (
        <Card className="border-red-200 bg-red-50/50">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-red-700">
              <AlertTriangle className="h-5 w-5" />
              Active Circuit Breakers ({triggered.length})
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {triggered.map((cb) => (
              <Card key={cb.pitchId} className="border-red-300">
                <CardContent className="pt-4">
                  <div className="space-y-3">
                    <div className="flex items-start justify-between">
                      <div>
                        <Link
                          to={`/pitches/${cb.pitchId}`}
                          className="text-lg font-semibold hover:underline"
                        >
                          {cb.pitchTitle}
                        </Link>
                        <p className="text-sm text-red-600 mt-1">{cb.circuitBreakerReason}</p>
                      </div>
                      <Badge variant="destructive" className="gap-1">
                        <Zap className="h-3 w-3" />
                        TRIGGERED
                      </Badge>
                    </div>
                    <div className="grid grid-cols-3 gap-4 text-sm">
                      <div>
                        <span className="text-muted-foreground">Appetite:</span>
                        <span className="ml-2 font-medium">{cb.appetiteDays}d</span>
                      </div>
                      <div>
                        <span className="text-muted-foreground">Spent:</span>
                        <span className="ml-2 font-medium text-red-600">
                          {cb.daysSpent.toFixed(1)}d
                        </span>
                      </div>
                      <div>
                        <span className="text-muted-foreground">Overflow:</span>
                        <span className="ml-2 font-medium text-red-600">
                          +{cb.overflowPercentage.toFixed(0)}%
                        </span>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Overflow Detection */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 text-yellow-600" />
            Overflow Detection ({overflows.length} pitches)
          </CardTitle>
        </CardHeader>
        <CardContent>
          {overflows.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <CheckCircle className="h-12 w-12 mx-auto mb-3 text-green-500" />
              <p>No overflowing pitches detected</p>
              <p className="text-sm mt-1">All pitches are within their time budget</p>
            </div>
          ) : (
            <div className="space-y-4">
              {overflows.map((cb) => (
                <Card
                  key={cb.pitchId}
                  className={`border ${getSeverityColor(cb.utilizationPercentage)}`}
                >
                  <CardContent className="pt-4">
                    <div className="space-y-3">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <Link
                            to={`/pitches/${cb.pitchId}`}
                            className="text-lg font-semibold hover:underline"
                          >
                            {cb.pitchTitle}
                          </Link>
                          <div className="flex items-center gap-4 mt-2 text-sm">
                            <span className="text-muted-foreground">
                              Appetite: <span className="font-medium">{cb.appetiteDays}d</span>
                            </span>
                            <span className="text-muted-foreground">
                              Spent:{' '}
                              <span className="font-medium">{cb.daysSpent.toFixed(1)}d</span>
                            </span>
                            <span className="text-muted-foreground">
                              ({cb.hoursSpent.toFixed(1)}h)
                            </span>
                          </div>
                        </div>
                        <Badge variant={cb.isCircuitBreakerTriggered ? 'destructive' : 'secondary'}>
                          {cb.status}
                        </Badge>
                      </div>

                      <div className="space-y-1">
                        <div className="flex items-center justify-between text-sm">
                          <span className="font-medium">
                            {cb.utilizationPercentage.toFixed(0)}% utilized
                          </span>
                          {cb.overflowPercentage > 0 && (
                            <span className="text-red-600 font-medium">
                              +{cb.overflowPercentage.toFixed(0)}% overflow
                            </span>
                          )}
                        </div>
                        <div className="relative h-2 bg-gray-200 rounded-full overflow-hidden">
                          <div
                            className={`h-full ${getProgressColor(
                              cb.utilizationPercentage
                            )} rounded-full transition-all`}
                            style={{ width: `${Math.min(cb.utilizationPercentage, 100)}%` }}
                          />
                        </div>
                      </div>

                      {!cb.isCircuitBreakerTriggered && (
                        <div className="flex gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setTriggerDialog({ open: true, pitch: cb });
                              setTriggerReason(
                                `Exceeded time budget: ${cb.daysSpent.toFixed(1)}d spent vs ${cb.appetiteDays
                                }d appetite`
                              );
                            }}
                          >
                            <Zap className="mr-1 h-4 w-4" />
                            Trigger Circuit Breaker
                          </Button>
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => {
                              setKillDialog({ open: true, pitch: cb });
                              setKillReason(
                                `Work won't fit in cycle - ${cb.overflowPercentage.toFixed(
                                  0
                                )}% over budget`
                              );
                            }}
                          >
                            <XCircle className="mr-1 h-4 w-4" />
                            Kill Pitch
                          </Button>
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Trigger Dialog */}
      <Dialog open={triggerDialog.open} onOpenChange={(open) => !open && setTriggerDialog({ open: false, pitch: null })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Trigger Circuit Breaker</DialogTitle>
            <DialogDescription>
              Flag "{triggerDialog.pitch?.pitchTitle}" as overflowing its time budget
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">Reason</label>
              <Textarea
                value={triggerReason}
                onChange={(e) => setTriggerReason(e.target.value)}
                placeholder={t('circuitBreaker.flagPlaceholder')}
                className="mt-1"
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setTriggerDialog({ open: false, pitch: null })}>
              Cancel
            </Button>
            <Button onClick={handleTrigger} disabled={!triggerReason.trim()}>
              <Zap className="mr-1 h-4 w-4" />
              Trigger
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Kill Dialog */}
      <Dialog open={killDialog.open} onOpenChange={(open) => !open && setKillDialog({ open: false, pitch: null })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle className="text-red-600">Kill Pitch</DialogTitle>
            <DialogDescription>
              Permanently stop work on "{killDialog.pitch?.pitchTitle}" due to overflow.
              This will cancel the pitch.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="bg-red-50 border border-red-200 rounded-md p-3 text-sm text-red-800">
              <AlertTriangle className="h-4 w-4 inline mr-2" />
              This action enforces Shape Up's fixed time, variable scope principle.
              Work that won't fit should be killed rather than extending timelines.
            </div>
            <div>
              <label className="text-sm font-medium">Reason</label>
              <Textarea
                value={killReason}
                onChange={(e) => setKillReason(e.target.value)}
                placeholder={t('circuitBreaker.killPlaceholder')}
                className="mt-1"
                rows={3}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setKillDialog({ open: false, pitch: null })}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleKill} disabled={!killReason.trim()}>
              <XCircle className="mr-1 h-4 w-4" />
              Kill Pitch
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
