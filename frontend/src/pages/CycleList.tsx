import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Plus,
  Pencil,
  Trash2,
  Play,
  Pause,
  Search,
  Calendar,
  Users,
  FileText,
} from 'lucide-react';
import { cycleService } from '../services/cycleService';
import { Cycle } from '../types';
import { useProject, useToast } from '../contexts';
import { CycleListSkeleton } from '../components/Skeletons';
import EmptyState from '../components/EmptyState';
import { EmptyCyclesIllustration } from '../components/illustrations';

import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '../components/ui/tooltip';

export default function CycleList() {
  const { currentProject, isAllProjectsSelected } = useProject();
  const { showSuccess, showError } = useToast();
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; cycle: Cycle | null }>({
    open: false,
    cycle: null,
  });
  
  // Filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [filterPhase, setFilterPhase] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');

  useEffect(() => {
    const abortController = new AbortController();
    loadCycles();
    return () => abortController.abort();
  }, [currentProject, isAllProjectsSelected]);

  const loadCycles = async () => {
    try {
      setLoading(true);
      let response;
      if (isAllProjectsSelected) {
        response = await cycleService.getAll();
      } else if (currentProject) {
        response = await cycleService.getByProject(currentProject.id);
      } else {
        response = { data: [] };
      }
      setCycles(response.data);
    } catch (error) {
      console.error('Failed to load cycles:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleActive = async (cycle: Cycle) => {
    try {
      await cycleService.toggleActive(cycle.id);
      showSuccess(cycle.isActive ? 'Cycle paused' : 'Cycle reactivated');
      loadCycles();
    } catch (error) {
      showError('Failed to update cycle status');
      console.error('Failed to toggle cycle:', error);
    }
  };

  const handleDelete = async () => {
    if (!deleteDialog.cycle) return;
    try {
      await cycleService.delete(deleteDialog.cycle.id);
      showSuccess('Cycle deleted successfully!');
      setDeleteDialog({ open: false, cycle: null });
      loadCycles();
    } catch (error) {
      showError('Failed to delete cycle');
      console.error('Failed to delete cycle:', error);
    }
  };

  if (loading) {
    return <CycleListSkeleton />;
  }

  // Apply filters
  const filteredCycles = cycles.filter((cycle) => {
    // Search filter
    if (searchTerm && !cycle.name.toLowerCase().includes(searchTerm.toLowerCase())) {
      return false;
    }
    
    // Phase filter
    if (filterPhase !== 'all' && cycle.phase !== filterPhase) {
      return false;
    }
    
    // Status filter
    if (filterStatus === 'active' && !cycle.isActive) {
      return false;
    }
    if (filterStatus === 'inactive' && cycle.isActive) {
      return false;
    }
    
    return true;
  });

  const activeCycles = filteredCycles.filter((c) => c.isActive);
  const inactiveCycles = filteredCycles.filter((c) => !c.isActive);

  const getPhaseClasses = (phase: string) => {
    switch (phase) {
      case 'BUILD': return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
      case 'SHAPING': return 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20';
      case 'BETTING': return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
      case 'COOLDOWN': return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
      default: return '';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Cycles</h1>
          <p className="text-sm text-muted-foreground">
            {isAllProjectsSelected ? 'All projects' : currentProject?.name} • {filteredCycles.length} cycle{filteredCycles.length !== 1 ? 's' : ''} found
          </p>
        </div>
        <Button asChild data-tour="new-cycle-btn">
          <Link to="/cycles/new">
            <Plus className="h-4 w-4 mr-2" />
            New Cycle
          </Link>
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            <div className="relative md:col-span-2">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search cycles..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9"
              />
            </div>
            <Select value={filterPhase} onValueChange={setFilterPhase}>
              <SelectTrigger>
                <SelectValue placeholder="All Phases" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Phases</SelectItem>
                <SelectItem value="SHAPING">Shaping</SelectItem>
                <SelectItem value="BETTING">Betting</SelectItem>
                <SelectItem value="BUILD">Build</SelectItem>
                <SelectItem value="COOLDOWN">Cooldown</SelectItem>
              </SelectContent>
            </Select>
            <Select value={filterStatus} onValueChange={setFilterStatus}>
              <SelectTrigger>
                <SelectValue placeholder="All Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="active">Active</SelectItem>
                <SelectItem value="inactive">Completed</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Active Cycles */}
      <section>
        <h2 className="text-lg font-semibold text-foreground mb-4">Active Cycles</h2>
        {activeCycles.length === 0 ? (
          <Card>
            <EmptyState
              illustration={<EmptyCyclesIllustration />}
              title="No Active Cycles"
              description="Start a new cycle to begin tracking your team's progress through the build phase."
              action={{
                label: 'Create First Cycle',
                onClick: () => window.location.href = '/cycles/new',
              }}
              size="small"
              compact
            />
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {activeCycles.map((cycle, index) => (
              <Card 
                key={cycle.id} 
                className="group hover:border-primary/50 transition-colors"
                data-tour={index === 0 ? 'cycle-card' : undefined}
              >
                <CardContent className="pt-6">
                  {/* Header */}
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold text-foreground truncate">{cycle.name}</h3>
                      <div className="flex items-center gap-1.5 text-sm text-muted-foreground mt-1">
                        <Calendar className="h-3.5 w-3.5" />
                        <span>
                          {new Date(cycle.startDate).toLocaleDateString()} - {new Date(cycle.endDate).toLocaleDateString()}
                        </span>
                      </div>
                    </div>
                    <Badge variant="outline" className={getPhaseClasses(cycle.phase)}>
                      {cycle.phase}
                    </Badge>
                  </div>

                  {/* Stats */}
                  <div className="flex gap-4 mb-4 text-sm text-muted-foreground">
                    <div className="flex items-center gap-1.5">
                      <FileText className="h-3.5 w-3.5" />
                      <span><strong className="text-foreground">{cycle.pitchCount || 0}</strong> pitches</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Users className="h-3.5 w-3.5" />
                      <span><strong className="text-foreground">{cycle.teamCount || 0}</strong> teams</span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex justify-between items-center pt-2 border-t border-border">
                    <Button variant="link" asChild className="px-0 h-auto">
                      <Link to={`/cycles/${cycle.id}`}>View Details</Link>
                    </Button>
                    <TooltipProvider>
                      <div className="flex gap-1">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              className="h-8 w-8"
                              onClick={() => handleToggleActive(cycle)}
                            >
                              <Pause className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Pause cycle</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-8 w-8" asChild>
                              <Link to={`/cycles/${cycle.id}/edit`}>
                                <Pencil className="h-4 w-4" />
                              </Link>
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Edit cycle</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              className="h-8 w-8 text-destructive hover:text-destructive"
                              onClick={() => setDeleteDialog({ open: true, cycle })}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Delete cycle</TooltipContent>
                        </Tooltip>
                      </div>
                    </TooltipProvider>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </section>

      {/* Past Cycles */}
      <section>
        <h2 className="text-lg font-semibold text-foreground mb-4">Past Cycles</h2>
        {inactiveCycles.length === 0 ? (
          <Card>
            <CardContent className="py-8 text-center">
              <p className="text-muted-foreground">No past cycles yet</p>
              <p className="text-sm text-muted-foreground/70 mt-1">
                Completed cycles will appear here
              </p>
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {inactiveCycles.map((cycle) => (
              <Card key={cycle.id} className="opacity-80 hover:opacity-100 transition-opacity">
                <CardContent className="pt-6">
                  {/* Header */}
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold text-foreground truncate">{cycle.name}</h3>
                      <div className="flex items-center gap-1.5 text-sm text-muted-foreground mt-1">
                        <Calendar className="h-3.5 w-3.5" />
                        <span>
                          {new Date(cycle.startDate).toLocaleDateString()} - {new Date(cycle.endDate).toLocaleDateString()}
                        </span>
                      </div>
                    </div>
                    <Badge variant="outline" className="bg-muted text-muted-foreground">
                      Completed
                    </Badge>
                  </div>

                  {/* Stats */}
                  <div className="flex gap-4 mb-4 text-sm text-muted-foreground">
                    <div className="flex items-center gap-1.5">
                      <FileText className="h-3.5 w-3.5" />
                      <span><strong className="text-foreground">{cycle.pitchCount || 0}</strong> pitches</span>
                    </div>
                    <div className="flex items-center gap-1.5">
                      <Users className="h-3.5 w-3.5" />
                      <span><strong className="text-foreground">{cycle.teamCount || 0}</strong> teams</span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex justify-between items-center pt-2 border-t border-border">
                    <Button variant="link" asChild className="px-0 h-auto">
                      <Link to={`/cycles/${cycle.id}`}>View Details</Link>
                    </Button>
                    <TooltipProvider>
                      <div className="flex gap-1">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              className="h-8 w-8"
                              onClick={() => handleToggleActive(cycle)}
                            >
                              <Play className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Reactivate cycle</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              className="h-8 w-8 text-destructive hover:text-destructive"
                              onClick={() => setDeleteDialog({ open: true, cycle })}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Delete cycle</TooltipContent>
                        </Tooltip>
                      </div>
                    </TooltipProvider>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </section>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialog.open} onOpenChange={(open) => !open && setDeleteDialog({ open: false, cycle: null })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Cycle</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete "{deleteDialog.cycle?.name}"? This will also delete all associated
              pitches and work logs.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog({ open: false, cycle: null })}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
