import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  DndContext,
  DragEndEvent,
  DragStartEvent,
  DragOverlay,
  useSensor,
  useSensors,
  PointerSensor,
  useDroppable,
  useDraggable,
} from '@dnd-kit/core';
import {
  Plus,
  RefreshCw,
  Dices,
  GripVertical,
  Clock,
  Users,
  ClipboardList,
  XCircle,
  BarChart3,
} from 'lucide-react';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Alert, AlertDescription } from '../components/ui/alert';
import { Skeleton } from '../components/ui/skeleton';
import { Separator } from '../components/ui/separator';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { bettingService } from '../services/bettingService';
import { cycleService } from '../services/cycleService';
import { BettingTable, BettingSlot, Pitch, Cycle, TeamTrack } from '../types';
import { useProject, useToast } from '../contexts';
import EmptyState from '../components/EmptyState';
import { getUserFriendlyError } from '../utils/errorMessages';
import { cn } from '../lib/utils';

// Draggable Pitch Card Component
function DraggablePitchCard({ pitch, isDragging, t }: { pitch: Pitch; isDragging?: boolean; t: any }) {
  const { attributes, listeners, setNodeRef, transform } = useDraggable({
    id: `pitch-${pitch.id}`,
    data: { pitch, type: 'pitch' },
  });

  const style = transform
    ? {
        transform: `translate3d(${transform.x}px, ${transform.y}px, 0)`,
        opacity: isDragging ? 0.5 : 1,
      }
    : undefined;

  const getAppetiteLabel = (days: number) => {
    if (days <= 7) return t('bettingTablePage.smallBatch');
    if (days <= 14) return t('bettingTablePage.mediumBatch');
    if (days <= 28) return t('bettingTablePage.bigBatch');
    return t('bettingTablePage.fullCycle');
  };

  const getAppetiteVariant = (days: number): 'default' | 'secondary' | 'destructive' | 'outline' => {
    if (days <= 7) return 'default';
    if (days <= 14) return 'secondary';
    if (days <= 28) return 'outline';
    return 'destructive';
  };

  return (
    <Card
      ref={setNodeRef}
      style={style}
      {...listeners}
      {...attributes}
      className="cursor-grab hover:shadow-lg active:cursor-grabbing transition-shadow mb-3"
    >
      <CardContent className="py-3 px-4">
        <div className="flex items-start gap-2">
          <GripVertical className="h-4 w-4 text-muted-foreground mt-0.5 flex-shrink-0" />
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-sm truncate mb-1">{pitch.title}</p>
            <div className="flex items-center gap-1.5">
              <Badge variant={getAppetiteVariant(pitch.appetiteDays)} className="text-xs h-5">
                {pitch.appetiteDays} {t('common.days')}
              </Badge>
              <span className="text-xs text-muted-foreground">
                {getAppetiteLabel(pitch.appetiteDays)}
              </span>
            </div>
            {pitch.projectKey && (
              <p className="text-xs text-muted-foreground mt-1">{pitch.projectKey}</p>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

// Droppable Slot Component
function DroppableSlot({
  slot,
  onRemovePitch,
  cycleStartDate,
  cycleEndDate,
  t,
}: {
  slot: BettingSlot;
  onRemovePitch: (slotId: number) => void;
  cycleStartDate: string;
  cycleEndDate: string;
  t: any;
}) {
  const { isOver, setNodeRef } = useDroppable({
    id: `slot-${slot.id}`,
    data: { slot, type: 'slot' },
  });

  const slotDays = slot.durationWeeks ? slot.durationWeeks * 7 : 0;
  const isOccupied = !!slot.pitchId;

  // Calculate visual width as percentage of cycle
  const cycleStart = new Date(cycleStartDate);
  const cycleEnd = new Date(cycleEndDate);
  const slotStart = new Date(slot.startDate);
  const slotEnd = new Date(slot.endDate);
  
  const cycleDays = Math.ceil((cycleEnd.getTime() - cycleStart.getTime()) / (1000 * 60 * 60 * 24));
  const slotStartOffset = Math.ceil((slotStart.getTime() - cycleStart.getTime()) / (1000 * 60 * 60 * 24));
  const slotDuration = Math.ceil((slotEnd.getTime() - slotStart.getTime()) / (1000 * 60 * 60 * 24));
  
  const leftPercent = (slotStartOffset / cycleDays) * 100;
  const widthPercent = (slotDuration / cycleDays) * 100;

  return (
    <div
      ref={setNodeRef}
      className="absolute p-0.5"
      style={{
        left: `${leftPercent}%`,
        width: `${widthPercent}%`,
        height: 80,
      }}
    >
      <div
        className={cn(
          'h-full p-2 rounded flex flex-col transition-all overflow-hidden',
          isOver
            ? 'border-2 border-dashed border-primary bg-accent'
            : isOccupied
            ? 'border border-green-300 bg-green-50 dark:bg-green-950/20'
            : 'border border-border bg-background'
        )}
      >
        {isOccupied ? (
          <>
            <div className="flex justify-between items-start">
              <p className="text-xs font-semibold truncate">{slot.pitchTitle}</p>
              <button
                onClick={() => onRemovePitch(slot.id)}
                className="p-0.5 ml-1 hover:opacity-70 flex-shrink-0"
              >
                <XCircle className="h-4 w-4 text-destructive" />
              </button>
            </div>
            <Badge variant="default" className="mt-auto self-start h-4 text-[0.65rem]">
              {slot.pitchAppetiteDays} {t('common.days')}
            </Badge>
          </>
        ) : (
          <div className="h-full flex flex-col items-center justify-center text-muted-foreground">
            <p className="text-xs">{t('bettingTablePage.dropPitchHere')}</p>
            <p className="text-xs opacity-60">{t('bettingTablePage.daysAvailable', { days: slotDays })}</p>
          </div>
        )}
      </div>
    </div>
  );
}

// Team Track Row Component
function TeamTrackRow({
  track,
  cycleStartDate,
  cycleEndDate,
  onRemovePitch,
  t,
}: {
  track: TeamTrack;
  cycleStartDate: string;
  cycleEndDate: string;
  onRemovePitch: (slotId: number) => void;
  t: any;
}) {
  return (
    <div className="mb-4">
      <div className="flex items-center gap-2 mb-2">
        <Users className="h-4 w-4 text-primary" />
        <span className="font-semibold text-sm">{track.teamName}</span>
        <Badge
          variant={track.availableCapacityWeeks > 0 ? 'outline' : 'secondary'}
          className="ml-auto"
        >
          {t('bettingTablePage.weeksUsed', { used: track.usedCapacityWeeks, total: track.totalCapacityWeeks })}
        </Badge>
      </div>
      <div className="relative h-[90px] bg-muted/50 rounded border border-border">
        {track.slots.length > 0 ? (
          track.slots.map((slot) => (
            <DroppableSlot
              key={slot.id}
              slot={slot}
              onRemovePitch={onRemovePitch}
              cycleStartDate={cycleStartDate}
              cycleEndDate={cycleEndDate}
              t={t}
            />
          ))
        ) : (
          <div className="h-full flex items-center justify-center text-muted-foreground">
            <p className="text-sm">{t('bettingTablePage.noSlotsConfigured')}</p>
          </div>
        )}
      </div>
    </div>
  );
}

// Main Betting Table Page
export default function BettingTablePage() {
  const { t, i18n } = useTranslation();
  const { currentProject, isAllProjectsSelected } = useProject();
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();
  
  const [bettingTable, setBettingTable] = useState<BettingTable | null>(null);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [generatingSlots, setGeneratingSlots] = useState(false);
  const [activePitch, setActivePitch] = useState<Pitch | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    })
  );

  useEffect(() => {
    loadCycles();
  }, [currentProject, isAllProjectsSelected]);

  useEffect(() => {
    if (selectedCycle) {
      loadBettingTable(Number(selectedCycle));
    }
  }, [selectedCycle]);

  const loadCycles = async () => {
    try {
      let cyclesPromise;
      if (isAllProjectsSelected) {
        cyclesPromise = cycleService.getMyActiveCycles();
      } else if (currentProject) {
        cyclesPromise = cycleService.getActiveByProject(currentProject.id);
      } else {
        cyclesPromise = Promise.resolve({ data: [] });
      }

      const response = await cyclesPromise;
      setCycles(response.data);
      if (response.data.length > 0) {
        setSelectedCycle(String(response.data[0].id));
      } else {
        setSelectedCycle('');
        setBettingTable(null);
      }
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to load cycles'));
    } finally {
      setLoading(false);
    }
  };

  const loadBettingTable = async (cycleId: number) => {
    try {
      setLoading(true);
      const response = await bettingService.getBettingTable(cycleId);
      setBettingTable(response.data);
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to load betting table'));
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateSlots = async () => {
    if (!selectedCycle) return;
    
    try {
      setGeneratingSlots(true);
      await bettingService.generateSlots(Number(selectedCycle));
      await loadBettingTable(Number(selectedCycle));
      showSuccess(t('bettingTablePage.slotsGeneratedSuccess'));
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to generate slots'));
    } finally {
      setGeneratingSlots(false);
    }
  };

  const handleDragStart = (event: DragStartEvent) => {
    const { active } = event;
    const pitch = (active.data.current as any)?.pitch;
    if (pitch) {
      setActivePitch(pitch);
    }
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event;
    setActivePitch(null);

    if (!over) return;

    const pitch = (active.data.current as any)?.pitch as Pitch;
    const slot = (over.data.current as any)?.slot as BettingSlot;

    if (!pitch || !slot) return;

    // Check if slot already has a pitch
    if (slot.pitchId) {
      showError(t('bettingTablePage.slotAlreadyOccupied'));
      return;
    }

    try {
      // Check if pitch fits
      const canFit = await bettingService.canPitchFitInSlot(slot.id, pitch.id);
      if (!canFit.data) {
        showError(t('bettingTablePage.pitchDoesntFit', { title: pitch.title, days: pitch.appetiteDays }));
        return;
      }

      await bettingService.assignPitchToSlot(slot.id, pitch.id);
      await loadBettingTable(Number(selectedCycle));
      showSuccess(t('bettingTablePage.pitchAssignedSuccess', { title: pitch.title, team: slot.teamName }));
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to assign pitch'));
    }
  };

  const handleRemovePitch = async (slotId: number) => {
    try {
      await bettingService.removePitchFromSlot(slotId);
      await loadBettingTable(Number(selectedCycle));
      showSuccess(t('bettingTablePage.pitchRemovedSuccess'));
    } catch (error) {
      showError(getUserFriendlyError(error, 'Failed to remove pitch'));
    }
  };

  if (loading && !bettingTable) {
    return (
      <div className="p-6">
        <Skeleton className="h-14 mb-4" />
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
          <div className="md:col-span-3">
            <Skeleton className="h-[400px]" />
          </div>
          <div className="md:col-span-9">
            <Skeleton className="h-[400px]" />
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6" data-tour="betting-table">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
        <div className="flex items-center gap-3">
          <Dices className="h-8 w-8 text-primary" />
          <div>
            <h1 className="text-xl font-bold">{t('bettingTablePage.title')}</h1>
            <p className="text-sm text-muted-foreground">
              {t('bettingTablePage.subtitle')}
            </p>
          </div>
        </div>
        
        <div className="flex flex-col sm:flex-row gap-2">
          <Select value={selectedCycle} onValueChange={setSelectedCycle}>
            <SelectTrigger className="w-full sm:w-[200px]">
              <SelectValue placeholder={t('bettingTablePage.selectCycle')} />
            </SelectTrigger>
            <SelectContent>
              {cycles.map((cycle) => (
                <SelectItem key={cycle.id} value={String(cycle.id)}>
                  {cycle.name} {cycle.projectKey && `(${cycle.projectKey})`}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          
          <Button
            variant="outline"
            size="sm"
            onClick={() => selectedCycle && loadBettingTable(Number(selectedCycle))}
            disabled={!selectedCycle}
          >
            <RefreshCw className="h-4 w-4 mr-2" />
            {t('bettingTablePage.refresh')}
          </Button>

          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate(`/betting/comparison?cycleId=${selectedCycle}`)}
            disabled={!selectedCycle}
          >
            <BarChart3 className="h-4 w-4 mr-2" />
            {t('bettingTablePage.pitchComparison')}
          </Button>
          
          <Button
            onClick={handleGenerateSlots}
            disabled={!selectedCycle || generatingSlots}
          >
            <Plus className="h-4 w-4 mr-2" />
            {t('bettingTablePage.generateSlots')}
          </Button>
        </div>
      </div>

      {!selectedCycle || cycles.length === 0 ? (
        <EmptyState
          title={t('bettingTablePage.noActiveCycles')}
          description={t('bettingTablePage.createCycleFirst')}
          icon={Dices}
        />
      ) : !bettingTable ? (
        <div className="flex justify-center py-8">
          <div className="w-48 h-1 bg-primary/20 rounded animate-pulse" />
        </div>
      ) : (
        <DndContext
          sensors={sensors}
          onDragStart={handleDragStart}
          onDragEnd={handleDragEnd}
        >
          <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
            {/* Left Column - Shaped Pitches */}
            <div className="md:col-span-3">
              <Card className="h-[calc(100vh-220px)] overflow-auto">
                <CardContent className="p-4">
                  <div className="flex items-center gap-2 mb-4">
                    <ClipboardList className="h-5 w-5 text-primary" />
                    <h2 className="font-semibold">{t('bettingTablePage.shapedWork')}</h2>
                    <Badge variant="secondary" className="ml-auto">
                      {bettingTable.shapedPitches.length}
                    </Badge>
                  </div>
                  
                  <Alert className="mb-4">
                    <AlertDescription className="text-xs">
                      {t('bettingTablePage.dragInstructions')}
                    </AlertDescription>
                  </Alert>

                  {bettingTable.shapedPitches.length === 0 ? (
                    <div className="text-center py-8 text-muted-foreground">
                      <ClipboardList className="h-12 w-12 mx-auto opacity-30 mb-2" />
                      <p className="text-sm">{t('bettingTablePage.noPitchesAvailable')}</p>
                      <p className="text-xs">{t('bettingTablePage.markPitchesAsShaped')}</p>
                    </div>
                  ) : (
                    bettingTable.shapedPitches.map((pitch) => (
                      <DraggablePitchCard key={pitch.id} pitch={pitch} t={t} />
                    ))
                  )}
                </CardContent>
              </Card>
            </div>

            {/* Main Area - Team Tracks */}
            <div className="md:col-span-9">
              <Card className="h-[calc(100vh-220px)] overflow-auto">
                <CardContent className="p-4">
                  {/* Cycle Info Header */}
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h2 className="font-semibold text-lg">{bettingTable.cycleName}</h2>
                      <div className="flex items-center gap-4 mt-1">
                        <span className="text-xs text-muted-foreground flex items-center gap-1">
                          <Clock className="h-3 w-3" />
                          {formatLocalizedDate(new Date(bettingTable.cycleStartDate), i18n.language)} - {formatLocalizedDate(new Date(bettingTable.cycleEndDate), i18n.language)}
                        </span>
                        <span className="text-xs text-muted-foreground">
                          {bettingTable.cycleDurationWeeks} {t('bettingTablePage.weeks')}
                        </span>
                      </div>
                    </div>
                    
                    <div className="flex gap-2">
                      <Badge variant="outline">
                        {bettingTable.totalAssignedPitches} {t('bettingTablePage.assigned')}
                      </Badge>
                      <Badge variant="secondary">
                        {bettingTable.usedCapacityWeeks}/{bettingTable.totalCapacityWeeks} {t('bettingTablePage.weeks')}
                      </Badge>
                    </div>
                  </div>

                  <Separator className="mb-4" />

                  {/* Timeline Header */}
                  <div className="mb-4">
                    <div className="flex justify-between px-2 mb-1">
                      {Array.from({ length: bettingTable.cycleDurationWeeks + 1 }).map((_, i) => (
                        <span key={i} className="text-xs text-muted-foreground">
                          Week {i}
                        </span>
                      ))}
                    </div>
                    <div className="relative h-1 bg-muted rounded">
                      {Array.from({ length: bettingTable.cycleDurationWeeks }).map((_, i) => (
                        <div
                          key={i}
                          className="absolute w-px h-2 bg-muted-foreground/40 -top-0.5"
                          style={{ left: `${(i / bettingTable.cycleDurationWeeks) * 100}%` }}
                        />
                      ))}
                    </div>
                  </div>

                  {/* Team Tracks */}
                  {bettingTable.teamTracks.length === 0 ? (
                    <div className="text-center py-16 text-muted-foreground">
                      <Users className="h-16 w-16 mx-auto opacity-30 mb-4" />
                      <h3 className="font-semibold text-lg mb-2">{t('bettingTablePage.noTeamsConfigured')}</h3>
                      <p className="text-sm mb-4">
                        {t('bettingTablePage.createTeamsFirst')}
                      </p>
                      <Button onClick={handleGenerateSlots} disabled={generatingSlots}>
                        <Plus className="h-4 w-4 mr-2" />
                        {t('bettingTablePage.generateSlots')}
                      </Button>
                    </div>
                  ) : (
                    bettingTable.teamTracks.map((track) => (
                      <TeamTrackRow
                        key={track.teamId}
                        track={track}
                        cycleStartDate={bettingTable.cycleStartDate}
                        cycleEndDate={bettingTable.cycleEndDate}
                        onRemovePitch={handleRemovePitch}
                        t={t}
                      />
                    ))
                  )}
                </CardContent>
              </Card>
            </div>
          </div>

          {/* Drag Overlay */}
          <DragOverlay>
            {activePitch ? (
              <Card className="w-[250px] shadow-lg opacity-90">
                <CardContent className="py-3 px-4">
                  <p className="font-semibold text-sm">{activePitch.title}</p>
                  <Badge variant="secondary" className="mt-1">
                    {activePitch.appetiteDays} days
                  </Badge>
                </CardContent>
              </Card>
            ) : null}
          </DragOverlay>
        </DndContext>
      )}
    </div>
  );
}
