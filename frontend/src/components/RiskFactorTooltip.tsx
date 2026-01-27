import { useState } from 'react';
import { AlertCircle, Loader2 } from 'lucide-react';
import { riskService, PitchRiskDTO, formatRiskCategory } from '../services/riskService';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';

interface RiskFactorTooltipProps {
  pitchId: number;
  pitchTitle: string;
  topRiskPreview: string;
  children: React.ReactNode;
}

export default function RiskFactorTooltip({
  pitchId,
  pitchTitle,
  topRiskPreview,
  children,
}: RiskFactorTooltipProps) {
  const [riskData, setRiskData] = useState<PitchRiskDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadRiskFactors = async () => {
    if (riskData || loading) return; // Already loaded or loading
    
    try {
      setLoading(true);
      setError(null);
      const data = await riskService.analyzePitchRisk(pitchId);
      setRiskData(data);
    } catch (err) {
      setError('Failed to load risk factors');
      console.error('Failed to load risk factors:', err);
    } finally {
      setLoading(false);
    }
  };

  const topFactors = riskData?.riskFactors
    ?.slice()
    .sort((a, b) => (b.impactLevel * b.probability) - (a.impactLevel * a.probability))
    .slice(0, 3) || [];

  return (
    <Tooltip>
      <TooltipTrigger asChild onMouseEnter={loadRiskFactors}>
        {children}
      </TooltipTrigger>
      <TooltipContent className="max-w-sm p-3">
        <div>
          <p className="font-semibold text-sm mb-2">{pitchTitle}</p>
          
          {loading && (
            <div className="flex items-center gap-2 text-xs text-muted-foreground py-2">
              <Loader2 className="w-3 h-3 animate-spin" />
              <span>Loading risk factors...</span>
            </div>
          )}
          
          {error && (
            <div className="flex items-center gap-2 text-xs text-destructive py-2">
              <AlertCircle className="w-3 h-3" />
              <span>{error}</span>
            </div>
          )}
          
          {!loading && !error && topFactors.length === 0 && (
            <p className="text-xs text-muted-foreground py-1">{topRiskPreview}</p>
          )}
          
          {!loading && !error && topFactors.length > 0 && (
            <div className="space-y-2">
              <p className="text-xs font-medium text-muted-foreground">Top Risk Factors:</p>
              {topFactors.map((factor, index) => (
                <div key={index} className="space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-medium">
                      {formatRiskCategory(factor.category)}
                    </span>
                    <div className="flex items-center gap-1">
                      <span className="text-[10px] text-muted-foreground">Impact:</span>
                      <span className={cn(
                        'text-xs font-semibold',
                        factor.impactLevel >= 7 ? 'text-destructive' :
                        factor.impactLevel >= 5 ? 'text-amber-500' :
                        'text-muted-foreground'
                      )}>
                        {factor.impactLevel}/10
                      </span>
                    </div>
                  </div>
                  <p className="text-xs text-muted-foreground leading-tight">
                    {factor.description}
                  </p>
                </div>
              ))}
              
              {riskData?.aiEnabled && (
                <p className="text-[10px] text-muted-foreground/70 pt-1 border-t">
                  AI-powered analysis
                </p>
              )}
            </div>
          )}
        </div>
      </TooltipContent>
    </Tooltip>
  );
}
