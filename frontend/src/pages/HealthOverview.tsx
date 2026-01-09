import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Loader2, Info, AlertCircle } from 'lucide-react';
import { useProject } from '../contexts';
import { CycleHealthSummary } from '../components/CycleHealthSummary';
import { 
  pitchHealthService, 
  CycleHealthSummaryDTO 
} from '../services/pitchHealthService';
import { cycleService } from '../services/cycleService';
import { Cycle } from '../types';
import { Card, CardContent } from '../components/ui/card';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../components/ui/tabs';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Label } from '../components/ui/label';

export const HealthOverview: React.FC = () => {
  const navigate = useNavigate();
  const { currentProject } = useProject();
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [selectedCycleId, setSelectedCycleId] = useState<string>('');
  const [allCycleHealth, setAllCycleHealth] = useState<CycleHealthSummaryDTO[]>([]);
  const [tabValue, setTabValue] = useState('all');
  const [loading, setLoading] = useState(true);
  const [healthLoading, setHealthLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Load cycles for the selected project
  useEffect(() => {
    const loadCycles = async () => {
      try {
        setLoading(true);
        const response = await cycleService.getAll();
        const projectCycles = currentProject
          ? response.data.filter((c: Cycle) => c.projectId === currentProject.id)
          : response.data;
        setCycles(projectCycles);
        
        // Select the first active cycle by default
        const activeCycle = projectCycles.find((c: Cycle) => c.isActive);
        if (activeCycle) {
          setSelectedCycleId(String(activeCycle.id));
        } else if (projectCycles.length > 0) {
          setSelectedCycleId(String(projectCycles[0].id));
        }
      } catch (err) {
        setError('Failed to load cycles');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    loadCycles();
  }, [currentProject]);

  // Load all active cycle health for overview tab (fast endpoint)
  useEffect(() => {
    const loadAllHealth = async () => {
      if (tabValue === 'all') {
        try {
          setHealthLoading(true);
          // Use the fast endpoint (no AI) for quick initial load
          const health = await pitchHealthService.getAllActiveCycleHealth();
          const filtered = currentProject
            ? health.filter(h => h.projectName === currentProject.name)
            : health;
          setAllCycleHealth(filtered);
        } catch (err) {
          console.error('Failed to load all cycle health:', err);
        } finally {
          setHealthLoading(false);
        }
      }
    };
    loadAllHealth();
  }, [tabValue, currentProject]);

  const handlePitchClick = (pitchId: number) => {
    navigate(`/pitches/${pitchId}`);
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[200px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-2">Pitch Health Overview</h1>
        <p className="text-muted-foreground">
          A simplified view for stakeholders showing pitch status, risk levels, and progress at a glance.
        </p>
      </div>

      {error && (
        <div className="flex items-center gap-2 p-3 mb-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      <Tabs value={tabValue} onValueChange={setTabValue} className="mb-6">
        <TabsList>
          <TabsTrigger value="all">All Active Cycles</TabsTrigger>
          <TabsTrigger value="specific">Specific Cycle</TabsTrigger>
        </TabsList>

        <TabsContent value="all" className="mt-4">
          <div data-tour="hill-chart-section">
            {healthLoading ? (
              <div className="flex justify-center items-center min-h-[200px]">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
              </div>
            ) : allCycleHealth.length === 0 ? (
              <div className="flex items-center gap-2 p-4 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-500">
                <Info className="h-4 w-4" />
                <span className="text-sm">No active cycles found</span>
              </div>
            ) : (
              <div className="space-y-6">
                {allCycleHealth.map((cycleHealth) => (
                  <CycleHealthSummary 
                    key={cycleHealth.cycleId}
                    cycleId={cycleHealth.cycleId} 
                    onPitchClick={handlePitchClick}
                  />
                ))}
              </div>
            )}
          </div>
        </TabsContent>

        <TabsContent value="specific" className="mt-4">
          <div className="mb-6">
            <Label className="mb-2 block">Select Cycle</Label>
            <Select
              value={selectedCycleId}
              onValueChange={setSelectedCycleId}
            >
              <SelectTrigger className="w-[300px]">
                <SelectValue placeholder="Select a cycle" />
              </SelectTrigger>
              <SelectContent>
                {cycles.map((cycle) => (
                  <SelectItem key={cycle.id} value={String(cycle.id)}>
                    {cycle.name} {cycle.isActive && '(Active)'}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {selectedCycleId ? (
            <CycleHealthSummary 
              cycleId={parseInt(selectedCycleId)} 
              onPitchClick={handlePitchClick}
            />
          ) : (
            <div className="flex items-center gap-2 p-4 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-500">
              <Info className="h-4 w-4" />
              <span className="text-sm">Select a cycle to view health summary</span>
            </div>
          )}
        </TabsContent>
      </Tabs>

      <div className="border-t border-border my-8" />

      {/* Legend */}
      <Card>
        <CardContent className="p-4">
          <h3 className="text-sm font-semibold mb-3">Understanding Health Indicators</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded-full bg-green-500" />
              <span className="text-sm"><strong>Healthy (Low Risk)</strong> - On track</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded-full bg-amber-500" />
              <span className="text-sm"><strong>At Risk (Medium)</strong> - Monitor closely</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded-full bg-red-500" />
              <span className="text-sm"><strong>High Risk</strong> - Needs attention</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-4 h-4 rounded-full bg-red-700" />
              <span className="text-sm"><strong>Critical</strong> - Immediate action needed</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default HealthOverview;
