import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ShieldCheck } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { systemService } from '@/services/systemService';

/**
 * Compact indicator shown in the app header when ShipFlow is running in
 * air-gapped AI mode. Renders nothing unless the backend reports
 * `enabled: true`, so it adds no visual noise in normal deployments.
 */
export function AirGappedBadge() {
  const { t } = useTranslation();

  const { data } = useQuery({
    queryKey: ['system', 'air-gapped'],
    queryFn: () => systemService.getAirGappedStatus(),
    // Deployment-level config: rarely changes within a session.
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
  });

  if (!data?.enabled) {
    return null;
  }

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <Badge
          variant="success"
          className="gap-1 cursor-default"
          data-testid="air-gapped-badge"
        >
          <ShieldCheck className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">{t('airGapped.badge')}</span>
        </Badge>
      </TooltipTrigger>
      <TooltipContent>{t('airGapped.tooltip')}</TooltipContent>
    </Tooltip>
  );
}

export default AirGappedBadge;
