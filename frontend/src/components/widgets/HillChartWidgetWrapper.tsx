import { HillChartWidget } from '../HillChartWidget';

interface HillChartWidgetWrapperProps {
  projectId?: number;
  maxPoints?: number;
}

export function HillChartWidgetWrapper({ projectId, maxPoints = 5 }: HillChartWidgetWrapperProps) {
  return <HillChartWidget projectId={projectId} maxPoints={maxPoints} />;
}
