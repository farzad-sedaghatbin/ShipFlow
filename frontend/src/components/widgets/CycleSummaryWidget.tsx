import { CycleSummaryPanel } from '../CycleSummaryPanel';

interface CycleSummaryWidgetProps {
  cycleId?: number;
}

export function CycleSummaryWidget({ cycleId }: CycleSummaryWidgetProps) {
  if (!cycleId) {
    return null;
  }

  return <CycleSummaryPanel cycleId={cycleId} />;
}
