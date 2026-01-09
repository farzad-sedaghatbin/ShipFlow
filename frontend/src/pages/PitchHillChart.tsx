import React, { useState, useEffect } from 'react';
import { Plus, AlertCircle, Info, X } from 'lucide-react';
import { HillChart } from '../components/HillChart';
import { hillChartApi } from '../services/hillChartApi';
import { HillChartPoint, CreateHillChartPointRequest, UpdateHillChartPointRequest } from '../types';
import { useParams } from 'react-router-dom';
import { safeParseId } from '../utils/validation';
import { HillChartSkeleton } from '../components/Skeletons';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import { Slider } from '../components/ui/slider';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '../components/ui/dialog';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '../components/ui/alert-dialog';

export const PitchHillChart: React.FC = () => {
  const { pitchId: pitchIdParam } = useParams<{ pitchId: string }>();
  const pitchId = safeParseId(pitchIdParam);
  const [points, setPoints] = useState<HillChartPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingPoint, setEditingPoint] = useState<HillChartPoint | null>(null);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; point: HillChartPoint | null }>({
    open: false,
    point: null,
  });
  const [formData, setFormData] = useState({
    scope: '',
    description: '',
    position: 0,
  });

  useEffect(() => {
    const abortController = new AbortController();
    loadHillChartPoints();
    return () => abortController.abort();
  }, [pitchId]);

  const loadHillChartPoints = async () => {
    if (!pitchId) return;
    
    try {
      setLoading(true);
      const data = await hillChartApi.getHillChartPointsByPitch(pitchId);
      setPoints(data);
      setError(null);
    } catch (err) {
      setError('Failed to load hill chart data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleAddPoint = () => {
    setEditingPoint(null);
    setFormData({
      scope: '',
      description: '',
      position: 0,
    });
    setDialogOpen(true);
  };

  const handleEditPoint = (point: HillChartPoint) => {
    setEditingPoint(point);
    setFormData({
      scope: point.scope,
      description: point.description,
      position: point.position,
    });
    setDialogOpen(true);
  };

  const handleDeletePoint = async () => {
    if (!deleteDialog.point) return;

    try {
      await hillChartApi.deleteHillChartPoint(deleteDialog.point.id);
      setDeleteDialog({ open: false, point: null });
      await loadHillChartPoints();
    } catch (err) {
      setError('Failed to delete point');
      console.error(err);
    }
  };

  const handleSave = async () => {
    if (!pitchId) return;

    try {
      if (editingPoint) {
        const request: UpdateHillChartPointRequest = {
          scope: formData.scope,
          description: formData.description,
          position: formData.position,
        };
        await hillChartApi.updateHillChartPoint(editingPoint.id, request);
      } else {
        const request: CreateHillChartPointRequest = {
          pitchId: pitchId,
          scope: formData.scope,
          description: formData.description,
          position: formData.position,
        };
        await hillChartApi.createHillChartPoint(request);
      }
      
      setDialogOpen(false);
      await loadHillChartPoints();
    } catch (err) {
      setError('Failed to save point');
      console.error(err);
    }
  };

  const handlePositionUpdate = async (point: HillChartPoint, newPosition: number) => {
    try {
      const request: UpdateHillChartPointRequest = {
        position: newPosition,
      };
      await hillChartApi.updateHillChartPoint(point.id, request);
      
      // Optimistically update local state
      setPoints(prevPoints =>
        prevPoints.map(p =>
          p.id === point.id ? { ...p, position: newPosition } : p
        )
      );
    } catch (err) {
      setError('Failed to update position');
      console.error(err);
      // Reload to get correct state
      await loadHillChartPoints();
    }
  };

  if (pitchId === null) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">Invalid pitch ID</span>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto p-6">
        <HillChartSkeleton />
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Progress Hill Chart</h1>
        <Button onClick={handleAddPoint} className="gap-2">
          <Plus className="h-4 w-4" />
          Add Scope
        </Button>
      </div>

      {error && (
        <div className="flex items-center justify-between gap-2 p-3 mb-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <div className="flex items-center gap-2">
            <AlertCircle className="h-4 w-4" />
            <span className="text-sm">{error}</span>
          </div>
          <button onClick={() => setError(null)}>
            <X className="h-4 w-4" />
          </button>
        </div>
      )}

      <div className="flex items-center gap-2 p-3 mb-6 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-500">
        <Info className="h-4 w-4 shrink-0" />
        <p className="text-sm">
          <strong>Hill Chart Guide:</strong> Drag points to update progress.
          Left side (0-50%) = Figuring things out. Right side (50-100%) = Making it happen.
        </p>
      </div>

      {points.length === 0 ? (
        <div className="flex items-center gap-2 p-4 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-500 animate-in fade-in duration-500">
          <Info className="h-4 w-4" />
          <span className="text-sm">No scopes added yet. Click "Add Scope" to start tracking progress on the hill chart.</span>
        </div>
      ) : (
        <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
          <HillChart
            points={points}
            onPointUpdate={handlePositionUpdate}
            onPointEdit={handleEditPoint}
            onPointDelete={(point) => setDeleteDialog({ open: true, point })}
            animateEntrance
          />
        </div>
      )}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editingPoint ? 'Edit Scope' : 'Add Scope'}</DialogTitle>
          </DialogHeader>
          
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="scope">Scope Name *</Label>
              <Input
                id="scope"
                value={formData.scope}
                onChange={(e) => setFormData({ ...formData, scope: e.target.value })}
                placeholder="e.g., User Authentication"
              />
            </div>
            
            <div className="space-y-2">
              <Label htmlFor="description">Description *</Label>
              <Textarea
                id="description"
                rows={3}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="What needs to be done?"
              />
            </div>

            <div className="space-y-3">
              <Label>Initial Position: {formData.position}%</Label>
              <Slider
                value={[formData.position]}
                onValueChange={([value]) => setFormData({ ...formData, position: value })}
                min={0}
                max={100}
                step={1}
              />
              <div className="flex justify-between text-xs text-muted-foreground">
                <span>0%</span>
                <span>25%</span>
                <span>50%</span>
                <span>75%</span>
                <span>100%</span>
              </div>
              <p className="text-xs text-muted-foreground">
                {formData.position < 50 
                  ? '🔍 Figuring things out (Uphill)'
                  : '🚀 Making it happen (Downhill)'}
              </p>
            </div>
          </div>
          
          <DialogFooter>
            <Button variant="ghost" onClick={() => setDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleSave}
              disabled={!formData.scope || !formData.description}
            >
              {editingPoint ? 'Update' : 'Add'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <AlertDialog 
        open={deleteDialog.open} 
        onOpenChange={(open) => !open && setDeleteDialog({ open: false, point: null })}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Hill Chart Point</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete "{deleteDialog.point?.scope}"? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeletePoint}
              className="bg-red-500 hover:bg-red-600"
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};
