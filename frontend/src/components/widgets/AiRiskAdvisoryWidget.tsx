import CycleRiskOverview from '../CycleRiskOverview';

interface AiRiskAdvisoryWidgetProps {
  cycleId?: number;
  cycleName?: string;
  compact?: boolean;
}

export function AiRiskAdvisoryWidget({ cycleId, cycleName, compact = false }: AiRiskAdvisoryWidgetProps) {
  if (!cycleId || !cycleName) {
    return null;
  }

  return <CycleRiskOverview cycleId={cycleId} cycleName={cycleName} compact={compact} />;
}
