import { CycleSignalsPanel } from '../CycleSignalsPanel';

interface CycleSignalsWidgetProps {
  cycleId?: number;
  projectId?: number;
}

export function CycleSignalsWidget({ cycleId, projectId }: CycleSignalsWidgetProps) {
  if (!projectId) {
    return null;
  }

  return <CycleSignalsPanel cycleId={cycleId} projectId={projectId} />;
}
