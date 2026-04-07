import { useTranslation } from 'react-i18next';
import { ShieldAlert, AlertTriangle } from 'lucide-react';
import { Alert, AlertDescription } from '../ui/alert';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Separator } from '../ui/separator';
import { OrganizationSettings, RiskWeights, RiskProfile } from '../../types/organizationSettings';
import { organizationSettingsService } from '../../services/organizationSettingsService';
import { useToast } from '../../contexts';

const DEFAULT_RISK_WEIGHTS = {
  budgetWeight: 25,
  bugsWeight: 30,
  scopeWeight: 25,
  timeWeight: 20,
};

interface WeightsSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
}

export function WeightsSettingsTab({ formData, setFormData }: WeightsSettingsTabProps) {
  const { t } = useTranslation();
  const { showToast } = useToast();

  const updateRiskWeight = (weightField: keyof RiskWeights, rawValue: number) => {
    const value = Number.isNaN(rawValue) ? 0 : rawValue;
    const currentWeights = formData.riskWeights ?? DEFAULT_RISK_WEIGHTS;
    setFormData({
      ...formData,
      riskWeights: {
        ...currentWeights,
        [weightField]: value,
      },
    });
  };

  const applyRiskProfile = async (profileName: string) => {
    try {
      const response = await organizationSettingsService.getRiskProfiles();
      const profiles: RiskProfile[] = response.data.profiles;

      if (!profiles || !Array.isArray(profiles)) {
        showToast(t('organizationSettings.failedToLoadProfiles'), 'error');
        return;
      }

      const selectedProfile = profiles.find((p) => p.name === profileName);

      if (!selectedProfile) {
        showToast(t('organizationSettings.profileNotFound', { profile: profileName }), 'error');
        return;
      }

      setFormData({
        ...formData,
        riskWeights: {
          budgetWeight: selectedProfile.budgetWeight,
          bugsWeight: selectedProfile.bugsWeight,
          scopeWeight: selectedProfile.scopeWeight,
          timeWeight: selectedProfile.timeWeight,
        },
      });
      showToast(t('organizationSettings.profileApplied', { profile: selectedProfile.displayName }), 'success');
    } catch {
      showToast(t('organizationSettings.failedToApplyProfile'), 'error');
    }
  };

  const getRiskWeightsSum = () => {
    const weights = formData.riskWeights || DEFAULT_RISK_WEIGHTS;
    return weights.budgetWeight + weights.bugsWeight + weights.scopeWeight + weights.timeWeight;
  };

  const isRiskWeightsValid = () => getRiskWeightsSum() === 100;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <ShieldAlert className="h-5 w-5" />
          {t('organizationSettings.riskWeights')}
        </CardTitle>
        <CardDescription>
          {t('organizationSettings.riskWeightsDesc')}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Preset Profiles */}
        <div className="space-y-3">
          <Label className="text-sm font-semibold">{t('organizationSettings.quickProfiles')}</Label>
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-2">
            <Button type="button" variant="outline" size="sm" onClick={() => applyRiskProfile('balanced')} className="justify-start">
              ⚖️ {t('organizationSettings.balanced')}
            </Button>
            <Button type="button" variant="outline" size="sm" onClick={() => applyRiskProfile('conservative')} className="justify-start">
              🛡️ {t('organizationSettings.conservative')}
            </Button>
            <Button type="button" variant="outline" size="sm" onClick={() => applyRiskProfile('aggressive')} className="justify-start">
              🚀 {t('organizationSettings.aggressive')}
            </Button>
            <Button type="button" variant="outline" size="sm" onClick={() => applyRiskProfile('quality_focused')} className="justify-start">
              🎯 {t('organizationSettings.qualityFocus')}
            </Button>
            <Button type="button" variant="outline" size="sm" onClick={() => applyRiskProfile('time_critical')} className="justify-start">
              ⏱️ {t('organizationSettings.timeCritical')}
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">{t('organizationSettings.profilesNote')}</p>
        </div>

        <Separator />

        {/* Weight Sliders */}
        <div className="space-y-6">
          {(
            [
              { key: 'budgetWeight' as keyof RiskWeights, emoji: '💰', labelKey: 'organizationSettings.budgetWeight', descKey: 'organizationSettings.budgetWeightDesc', default: 25 },
              { key: 'bugsWeight' as keyof RiskWeights, emoji: '🐛', labelKey: 'organizationSettings.bugsWeight', descKey: 'organizationSettings.bugsWeightDesc', default: 30 },
              { key: 'scopeWeight' as keyof RiskWeights, emoji: '📊', labelKey: 'organizationSettings.scopeWeight', descKey: 'organizationSettings.scopeWeightDesc', default: 25 },
              { key: 'timeWeight' as keyof RiskWeights, emoji: '⏰', labelKey: 'organizationSettings.timeWeight', descKey: 'organizationSettings.timeWeightDesc', default: 20 },
            ] as const
          ).map(({ key, emoji, labelKey, descKey, default: def }) => (
            <div key={key} className="space-y-3">
              <div className="flex items-center justify-between">
                <Label htmlFor={key} className="font-medium">{emoji} {t(labelKey)}</Label>
                <div className="flex items-center gap-2">
                  <Input
                    id={key}
                    type="number"
                    min="0"
                    max="100"
                    value={formData.riskWeights?.[key] ?? def}
                    onChange={(e) => updateRiskWeight(key, parseInt(e.target.value) || 0)}
                    className="w-16 text-center"
                  />
                  <span className="text-sm text-muted-foreground">%</span>
                </div>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={formData.riskWeights?.[key] ?? def}
                onChange={(e) => updateRiskWeight(key, parseInt(e.target.value))}
                className="w-full"
              />
              <p className="text-xs text-muted-foreground">{t(descKey)}</p>
            </div>
          ))}
        </div>

        {/* Validation Alert */}
        <Alert variant={isRiskWeightsValid() ? 'default' : 'destructive'}>
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            <div className="flex items-center justify-between">
              <span>{t('organizationSettings.totalWeight')}: <strong>{getRiskWeightsSum()}%</strong></span>
              {isRiskWeightsValid() ? (
                <Badge variant="default" className="bg-green-500">✓ {t('organizationSettings.valid')}</Badge>
              ) : (
                <Badge variant="destructive">{t('organizationSettings.mustEqual100')}</Badge>
              )}
            </div>
          </AlertDescription>
        </Alert>

        <Alert>
          <ShieldAlert className="h-4 w-4" />
          <AlertDescription>
            {t('organizationSettings.weightsChangeNote')}
          </AlertDescription>
        </Alert>
      </CardContent>
    </Card>
  );
}
