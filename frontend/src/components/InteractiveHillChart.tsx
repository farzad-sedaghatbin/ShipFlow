import React, { useState, useCallback } from 'react';
import { 
  Clock, 
  BarChart3, 
  ChevronDown, 
  TrendingUp,
  MessageSquare,
  Info,
} from 'lucide-react';
import { HillChart } from './HillChart';
import { HillChartPoint } from '../types';
import { 
  hillChartApi, 
  UpdateHillChartPositionRequest,
  HillChartHistoryDTO,
  ConfidenceAnalysisDTO,
} from '../services/hillChartApi';
import { useToast } from '../contexts';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Badge } from './ui/badge';
import { Textarea } from './ui/textarea';
import { Slider } from './ui/slider';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from './ui/dialog';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import {
  Collapsible,
  CollapsibleContent,
} from './ui/collapsible';
import { cn } from '../lib/utils';

interface InteractiveHillChartProps {
  points: HillChartPoint[];
  pitchId: number;
  onPointsChange?: () => void;
  readonly?: boolean;
}

export const InteractiveHillChart: React.FC<InteractiveHillChartProps> = ({
  points: initialPoints,
  pitchId,
  onPointsChange,
  readonly = false,
}) => {
  const { showToast } = useToast();
  const [points, setPoints] = useState<HillChartPoint[]>(initialPoints);
  const [draggedPoint, setDraggedPoint] = useState<HillChartPoint | null>(null);
  const [newPosition, setNewPosition] = useState<number>(0);
  const [confidence, setConfidence] = useState<number>(70);
  const [note, setNote] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  
  // History and analysis state
  const [showHistory, setShowHistory] = useState(false);
  const [history, setHistory] = useState<HillChartHistoryDTO[]>([]);
  const [analysis, setAnalysis] = useState<ConfidenceAnalysisDTO | null>(null);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // Sync with prop changes
  React.useEffect(() => {
    setPoints(initialPoints);
  }, [initialPoints]);

  const handlePointUpdate = useCallback((point: HillChartPoint, position: number) => {
    // Update local state immediately for responsive feedback
    setPoints(prev => prev.map(p => 
      p.id === point.id ? { ...p, position } : p
    ));
    
    // Open dialog to confirm and add confidence
    setDraggedPoint(point);
    setNewPosition(position);
    setDialogOpen(true);
  }, []);

  const handleSavePosition = async () => {
    if (!draggedPoint) return;

    try {
      setSaving(true);
      const request: UpdateHillChartPositionRequest = {
        newPosition,
        confidenceLevel: confidence,
        note: note.trim() || undefined,
      };

      await hillChartApi.updatePositionWithHistory(draggedPoint.id, request);
      showToast('Position updated with confidence tracking', 'success');
      
      // Reset and close
      setDialogOpen(false);
      setDraggedPoint(null);
      setNote('');
      setConfidence(70);
      onPointsChange?.();
    } catch (err) {
      showToast('Failed to update position', 'error');
      // Revert local change
      setPoints(initialPoints);
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  const handleCancelMove = () => {
    // Revert to original position
    setPoints(initialPoints);
    setDialogOpen(false);
    setDraggedPoint(null);
    setNote('');
  };

  const loadHistory = async () => {
    if (showHistory && history.length > 0) {
      setShowHistory(false);
      return;
    }

    try {
      setLoadingHistory(true);
      const [historyData, analysisData] = await Promise.all([
        hillChartApi.getPitchHistory(pitchId),
        hillChartApi.getConfidenceAnalysis(pitchId),
      ]);
      setHistory(historyData);
      setAnalysis(analysisData);
      setShowHistory(true);
    } catch (err) {
      showToast('Failed to load history', 'error');
      console.error(err);
    } finally {
      setLoadingHistory(false);
    }
  };

  const getConfidenceBgColor = (level: number): string => {
    if (level >= 80) return 'bg-green-500/10 text-green-500';
    if (level >= 60) return 'bg-amber-500/10 text-amber-500';
    return 'bg-red-500/10 text-red-500';
  };

  const getAccuracyBadgeStyle = (delta: number): string => {
    if (delta > 10) return 'bg-amber-500/10 text-amber-500';
    if (delta < -10) return 'bg-blue-500/10 text-blue-500';
    return 'bg-green-500/10 text-green-500';
  };

  return (
    <div data-tour="interactive-hill-chart">
      <HillChart
        points={points}
        onPointUpdate={readonly ? undefined : handlePointUpdate}
        readonly={readonly}
      />

      {!readonly && (
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mt-4 gap-2">
          <p className="text-xs text-muted-foreground">
            <span className="hidden sm:inline">Drag points to update progress • Confidence is tracked for analysis</span>
            <span className="sm:hidden">Tap & drag to update progress</span>
          </p>
          
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={loadHistory}
                  disabled={loadingHistory}
                  className="gap-2 min-h-[44px] touch-manipulation"
                >
                  {showHistory ? (
                    <ChevronDown className="h-4 w-4" />
                  ) : (
                    <Clock className="h-4 w-4" />
                  )}
                  {loadingHistory ? 'Loading...' : showHistory ? 'Hide History' : 'View History'}
                </Button>
              </TooltipTrigger>
              <TooltipContent>
                <p>View position history and confidence analysis</p>
              </TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>
      )}

      {/* History and Analysis Section */}
      <Collapsible open={showHistory}>
        <CollapsibleContent>
          <Card className="mt-4 border border-border">
            <CardContent className="p-4">
              {analysis && (
                <div className="mb-6">
                  <h4 className="text-base font-semibold mb-3 flex items-center gap-2">
                    <BarChart3 className="h-4 w-4" />
                    Confidence Analysis
                  </h4>
                  
                  <div className="flex flex-wrap gap-2 mb-4">
                    <Badge variant="outline" className="gap-1">
                      <TrendingUp className="h-3 w-3" />
                      Avg Confidence: {analysis.averageConfidence.toFixed(0)}%
                    </Badge>
                    <Badge variant="outline" className="bg-blue-500/10 text-blue-500">
                      Actual Progress: {analysis.actualProgress.toFixed(0)}%
                    </Badge>
                    <Badge className={getAccuracyBadgeStyle(analysis.confidenceDelta)}>
                      {analysis.accuracyAssessment.replace('_', ' ')}
                    </Badge>
                  </div>

                  {analysis.insights.length > 0 && (
                    <div className="bg-blue-500/10 border border-blue-500/20 rounded-lg p-3 mb-4">
                      <div className="flex items-center gap-2 mb-2">
                        <Info className="h-4 w-4 text-blue-500" />
                        <span className="text-sm font-semibold text-blue-500">Insights</span>
                      </div>
                      <ul className="space-y-1 pl-6 list-disc">
                        {analysis.insights.map((insight, i) => (
                          <li key={i} className="text-sm text-muted-foreground">{insight}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}

              <h4 className="text-base font-semibold mb-3 flex items-center gap-2">
                <Clock className="h-4 w-4" />
                Position History
              </h4>
              
              {history.length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  No position changes recorded yet.
                </p>
              ) : (
                <div className="space-y-2">
                  {history.slice(0, 10).map((entry, index) => (
                    <div key={entry.id}>
                      <div className="py-2">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-sm font-medium">{entry.scope}</span>
                          <span className="text-sm text-muted-foreground">
                            {entry.previousPosition}% → {entry.newPosition}%
                          </span>
                          {entry.confidenceLevel && (
                            <Badge 
                              variant="secondary"
                              className={cn("text-xs", getConfidenceBgColor(entry.confidenceLevel))}
                            >
                              {entry.confidenceLevel}% confident
                            </Badge>
                          )}
                        </div>
                        <p className="text-xs text-muted-foreground mt-1">
                          {entry.userName && `by ${entry.userName} • `}
                          {new Date(entry.createdAt).toLocaleString()}
                          {entry.note && ` • "${entry.note}"`}
                        </p>
                      </div>
                      {index < Math.min(history.length, 10) - 1 && (
                        <div className="border-t border-border" />
                      )}
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </CollapsibleContent>
      </Collapsible>

      {/* Confidence Dialog */}
      <Dialog open={dialogOpen} onOpenChange={(open) => !open && handleCancelMove()}>
        <DialogContent className="sm:max-w-md w-[calc(100vw-2rem)] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Confirm Position Update</DialogTitle>
          </DialogHeader>
          
          {draggedPoint && (
            <div className="space-y-4">
              <div>
                <p className="text-sm">
                  Moving <strong>{draggedPoint.scope}</strong> to position <strong>{newPosition}%</strong>
                </p>
                <p className="text-sm text-muted-foreground mt-1">
                  {newPosition < 50 ? '🔍 Figuring things out' : '🚀 Making it happen'}
                </p>
              </div>

              <div className="space-y-3">
                <label className="text-sm font-medium">
                  How confident are you about this position?
                </label>
                <div className="flex items-center gap-3 py-2">
                  <span className="text-xs text-red-500 font-medium">Low</span>
                  <Slider
                    value={[confidence]}
                    onValueChange={([value]) => setConfidence(value)}
                    min={0}
                    max={100}
                    step={1}
                    className={cn(
                      "flex-1",
                      confidence >= 80 && "[&_[data-slider-track]]:bg-green-500/20 [&_[data-slider-range]]:bg-green-500 [&_[data-slider-thumb]]:border-green-500",
                      confidence >= 60 && confidence < 80 && "[&_[data-slider-track]]:bg-amber-500/20 [&_[data-slider-range]]:bg-amber-500 [&_[data-slider-thumb]]:border-amber-500",
                      confidence < 60 && "[&_[data-slider-track]]:bg-red-500/20 [&_[data-slider-range]]:bg-red-500 [&_[data-slider-thumb]]:border-red-500"
                    )}
                  />
                  <span className="text-xs text-green-500 font-medium">High</span>
                </div>
                <div className="flex justify-center">
                  <Badge className={getConfidenceBgColor(confidence)}>
                    {confidence}%
                  </Badge>
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium flex items-center gap-2">
                  <MessageSquare className="h-4 w-4" />
                  Note (optional)
                </label>
                <Textarea
                  placeholder="Any context about this update..."
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  rows={3}
                  className="min-h-[80px] touch-manipulation"
                />
              </div>
            </div>
          )}
          
          <DialogFooter className="flex-col sm:flex-row gap-2">
            <Button 
              variant="ghost" 
              onClick={handleCancelMove}
              className="w-full sm:w-auto min-h-[44px] touch-manipulation"
            >
              Cancel
            </Button>
            <Button 
              onClick={handleSavePosition} 
              disabled={saving}
              className="w-full sm:w-auto min-h-[44px] touch-manipulation"
            >
              {saving ? 'Saving...' : 'Save Position'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default InteractiveHillChart;
