import { useEffect, useState } from 'react';
import { Plus, Trash2, Pencil, Clock, CalendarDays, Loader2, AlertTriangle } from 'lucide-react';
import dayjs from 'dayjs';
import { workLogService } from '../services/workLogService';
import { pitchService } from '../services/pitchService';
import { cycleService } from '../services/cycleService';
import { WorkLog, Pitch, Cycle, CreateWorkLogForSelfRequest } from '../types';
import { useAuth, useToast } from '../contexts';
import EmptyState from '../components/EmptyState';
import { EmptyWorkLogsIllustration } from '../components/illustrations';
import { cn } from '../lib/utils';

import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Badge } from '../components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { Textarea } from '../components/ui/textarea';
import { Alert, AlertDescription } from '../components/ui/alert';

export default function MyWorkLogs() {
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<string>('');
  const [loading, setLoading] = useState(true);

  // Form state
  const [newWorkLog, setNewWorkLog] = useState<CreateWorkLogForSelfRequest>({
    pitchId: 0,
    date: dayjs().format('YYYY-MM-DD'),
    hoursSpent: 0,
    note: '',
  });
  const [workLogDate, setWorkLogDate] = useState<string>(dayjs().format('YYYY-MM-DD'));
  const [selectedPitchId, setSelectedPitchId] = useState<string>('');

  // Edit dialog state
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editingWorkLog, setEditingWorkLog] = useState<WorkLog | null>(null);
  const [editForm, setEditForm] = useState<CreateWorkLogForSelfRequest>({
    pitchId: 0,
    date: '',
    hoursSpent: 0,
    note: '',
  });
  const [editDate, setEditDate] = useState<string>('');
  const [editPitchId, setEditPitchId] = useState<string>('');

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedCycle) {
      const cycleId = parseInt(selectedCycle, 10);
      loadWorkLogs(cycleId);
      loadPitches(cycleId);
    }
  }, [selectedCycle]);

  const loadInitialData = async () => {
    try {
      const cyclesRes = await cycleService.getActive();
      setCycles(cyclesRes.data);
      if (cyclesRes.data.length > 0) {
        setSelectedCycle(cyclesRes.data[0].id.toString());
      }
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadWorkLogs = async (cycleId: number) => {
    try {
      const response = await workLogService.getMyByCycle(cycleId);
      setWorkLogs(response.data);
    } catch (error) {
      console.error('Failed to load work logs:', error);
    }
  };

  const loadPitches = async (cycleId: number) => {
    try {
      const response = await pitchService.getByCycleId(cycleId);
      setPitches(response.data);
    } catch (error) {
      console.error('Failed to load pitches:', error);
    }
  };

  const handleCreateWorkLog = async () => {
    if (!workLogDate || !selectedPitchId || !newWorkLog.hoursSpent) return;
    try {
      await workLogService.createMy({
        ...newWorkLog,
        pitchId: parseInt(selectedPitchId, 10),
        date: workLogDate,
      });
      setNewWorkLog({
        pitchId: 0,
        date: dayjs().format('YYYY-MM-DD'),
        hoursSpent: 0,
        note: '',
      });
      setWorkLogDate(dayjs().format('YYYY-MM-DD'));
      setSelectedPitchId('');
      showSuccess('Work log added successfully');
      if (selectedCycle) {
        loadWorkLogs(parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || error.response?.data?.error || 'Failed to add work log';
      showError(message);
    }
  };

  const handleDeleteWorkLog = async (id: number) => {
    try {
      await workLogService.deleteMy(id);
      showSuccess('Work log deleted');
      if (selectedCycle) {
        loadWorkLogs(parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to delete work log';
      showError(message);
    }
  };

  const handleEditClick = (workLog: WorkLog) => {
    setEditingWorkLog(workLog);
    setEditForm({
      pitchId: workLog.pitchId,
      date: workLog.date,
      hoursSpent: workLog.hoursSpent,
      note: workLog.note || '',
    });
    setEditDate(workLog.date);
    setEditPitchId(workLog.pitchId.toString());
    setEditDialogOpen(true);
  };

  const handleEditSave = async () => {
    if (!editingWorkLog || !editDate || !editPitchId) return;
    try {
      await workLogService.updateMy(editingWorkLog.id, {
        ...editForm,
        pitchId: parseInt(editPitchId, 10),
        date: editDate,
      });
      showSuccess('Work log updated successfully');
      setEditDialogOpen(false);
      setEditingWorkLog(null);
      if (selectedCycle) {
        loadWorkLogs(parseInt(selectedCycle, 10));
      }
    } catch (error: any) {
      const message = error.response?.data?.message || 'Failed to update work log';
      showError(message);
    }
  };

  if (!user?.personId) {
    return (
      <div>
        <h1 className="text-3xl font-bold mb-4">My Work Logs</h1>
        <Alert variant="warning">
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            Your account is not linked to a person profile. Please contact an administrator to link your account.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  const totalHours = workLogs.reduce((sum, wl) => sum + wl.hoursSpent, 0);
  const todayLogs = workLogs.filter(wl => wl.date === dayjs().format('YYYY-MM-DD'));
  const todayHours = todayLogs.reduce((sum, wl) => sum + wl.hoursSpent, 0);

  return (
    <div>
      <h1 className="text-3xl font-bold mb-1">My Work Logs</h1>
      <p className="text-sm text-muted-foreground mb-6">
        Track your daily work on assigned pitches
      </p>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 mb-2">
              <CalendarDays className="h-5 w-5 text-primary" />
              <span className="text-sm text-muted-foreground">Today</span>
            </div>
            <div className="text-3xl font-bold">{todayHours.toFixed(1)}h</div>
            <p className="text-xs text-muted-foreground">
              {todayLogs.length} log{todayLogs.length !== 1 ? 's' : ''} today
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="flex items-center gap-2 mb-2">
              <Clock className="h-5 w-5 text-secondary-foreground" />
              <span className="text-sm text-muted-foreground">This Cycle</span>
            </div>
            <div className="text-3xl font-bold">{totalHours.toFixed(1)}h</div>
            <p className="text-xs text-muted-foreground">
              {workLogs.length} total log{workLogs.length !== 1 ? 's' : ''}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Cycle Filter */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <div className="flex flex-col gap-2">
            <Label htmlFor="cycle-select">Cycle</Label>
            <Select value={selectedCycle} onValueChange={setSelectedCycle}>
              <SelectTrigger className="w-[300px]" id="cycle-select">
                <SelectValue placeholder="Select a cycle" />
              </SelectTrigger>
              <SelectContent>
                {cycles.map((cycle) => (
                  <SelectItem key={cycle.id} value={cycle.id.toString()}>
                    <div className="flex items-center gap-2">
                      {cycle.projectKey && (
                        <Badge variant="secondary" className="text-xs">
                          {cycle.projectKey}
                        </Badge>
                      )}
                      <span>{cycle.name}</span>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Quick Add Work Log Form */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Log Your Work</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-12 gap-4 items-end">
            <div className="md:col-span-4 space-y-2">
              <Label htmlFor="pitch-select">Pitch *</Label>
              <Select value={selectedPitchId} onValueChange={setSelectedPitchId}>
                <SelectTrigger id="pitch-select">
                  <SelectValue placeholder="Select a pitch" />
                </SelectTrigger>
                <SelectContent>
                  {pitches.map((p) => (
                    <SelectItem key={p.id} value={p.id.toString()}>
                      {p.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="md:col-span-2 space-y-2">
              <Label htmlFor="date-input">Date *</Label>
              <Input
                id="date-input"
                type="date"
                value={workLogDate}
                onChange={(e) => setWorkLogDate(e.target.value)}
              />
            </div>
            <div className="md:col-span-2 space-y-2">
              <Label htmlFor="hours-input">Hours *</Label>
              <Input
                id="hours-input"
                type="number"
                value={newWorkLog.hoursSpent || ''}
                onChange={(e) => setNewWorkLog({ ...newWorkLog, hoursSpent: parseFloat(e.target.value) || 0 })}
                min={0.25}
                step={0.25}
                placeholder="0.0"
              />
            </div>
            <div className="md:col-span-3 space-y-2">
              <Label htmlFor="note-input">Note (optional)</Label>
              <Input
                id="note-input"
                value={newWorkLog.note}
                onChange={(e) => setNewWorkLog({ ...newWorkLog, note: e.target.value })}
                placeholder="What did you work on?"
              />
            </div>
            <div className="md:col-span-1">
              <Button
                onClick={handleCreateWorkLog}
                disabled={!selectedPitchId || !newWorkLog.hoursSpent}
                className="w-full"
              >
                <Plus className="h-4 w-4 mr-1" />
                Add
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Work Logs Table */}
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle>My Recent Work Logs</CardTitle>
            <span className="text-sm text-muted-foreground">
              Total: <strong>{totalHours.toFixed(1)} hours</strong>
            </span>
          </div>
        </CardHeader>
        <CardContent>
          {workLogs.length === 0 ? (
            <EmptyState
              illustration={<EmptyWorkLogsIllustration />}
              title="No work logs yet"
              description="Start tracking your work by adding your first work log entry using the form above"
              size="medium"
            />
          ) : (
            <div className="border rounded-md">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Pitch</TableHead>
                    <TableHead className="text-right">Hours</TableHead>
                    <TableHead>Note</TableHead>
                    <TableHead className="text-center">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {workLogs.map((wl) => (
                    <TableRow
                      key={wl.id}
                      className={cn(
                        wl.date === dayjs().format('YYYY-MM-DD') && 'bg-muted/50'
                      )}
                    >
                      <TableCell>
                        <div className="flex items-center gap-2">
                          {new Date(wl.date).toLocaleDateString()}
                          {wl.date === dayjs().format('YYYY-MM-DD') && (
                            <Badge variant="default" className="text-xs">Today</Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>{wl.pitchTitle}</TableCell>
                      <TableCell className="text-right">{wl.hoursSpent}h</TableCell>
                      <TableCell>{wl.note || '-'}</TableCell>
                      <TableCell>
                        <div className="flex items-center justify-center gap-1">
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            onClick={() => handleEditClick(wl)}
                            aria-label={`Edit work log for ${wl.pitchTitle}`}
                          >
                            <Pencil className="h-4 w-4" aria-hidden="true" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            onClick={() => handleDeleteWorkLog(wl.id)}
                            className="text-destructive hover:text-destructive"
                            aria-label={`Delete work log for ${wl.pitchTitle}`}
                          >
                            <Trash2 className="h-4 w-4" aria-hidden="true" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>Edit Work Log</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="edit-pitch">Pitch *</Label>
              <Select value={editPitchId} onValueChange={setEditPitchId}>
                <SelectTrigger id="edit-pitch">
                  <SelectValue placeholder="Select a pitch" />
                </SelectTrigger>
                <SelectContent>
                  {pitches.map((p) => (
                    <SelectItem key={p.id} value={p.id.toString()}>
                      {p.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="edit-date">Date *</Label>
                <Input
                  id="edit-date"
                  type="date"
                  value={editDate}
                  onChange={(e) => setEditDate(e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-hours">Hours *</Label>
                <Input
                  id="edit-hours"
                  type="number"
                  value={editForm.hoursSpent || ''}
                  onChange={(e) => setEditForm({ ...editForm, hoursSpent: parseFloat(e.target.value) || 0 })}
                  min={0.25}
                  step={0.25}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="edit-note">Note</Label>
              <Textarea
                id="edit-note"
                value={editForm.note}
                onChange={(e) => setEditForm({ ...editForm, note: e.target.value })}
                rows={2}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleEditSave}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
