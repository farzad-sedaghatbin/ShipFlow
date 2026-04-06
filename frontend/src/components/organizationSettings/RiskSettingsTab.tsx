import { useTranslation } from 'react-i18next';
import { AlertTriangle } from 'lucide-react';
import { Alert, AlertDescription } from '../ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { OrganizationSettings, RiskThresholds } from '../../types/organizationSettings';

interface RiskSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
}

export function RiskSettingsTab({ formData, setFormData }: RiskSettingsTabProps) {
  const { t } = useTranslation();

  const updateRiskThreshold = (field: keyof RiskThresholds, value: number) => {
    setFormData({
      ...formData,
      riskThresholds: {
        ...formData.riskThresholds!,
        [field]: value,
      },
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <AlertTriangle className="h-5 w-5" />
          {t('organizationSettings.riskThresholds')}
        </CardTitle>
        <CardDescription>
          {t('organizationSettings.riskThresholdsDesc')}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="space-y-4">
          <div className="flex items-center justify-between p-4 rounded-lg bg-green-50 dark:bg-green-950/20 border border-green-200 dark:border-green-900">
            <div>
              <div className="font-medium text-green-900 dark:text-green-100">{t('organizationSettings.lowRisk')}</div>
              <div className="text-sm text-green-700 dark:text-green-300">
                {t('organizationSettings.lowRiskRange', { max: formData.riskThresholds?.lowMax || 30 })}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="lowMax" className="text-xs">{t('organizationSettings.maxScore')}</Label>
              <Input
                id="lowMax"
                type="number"
                min="0"
                max="99"
                value={formData.riskThresholds?.lowMax || 30}
                onChange={(e) => updateRiskThreshold('lowMax', parseInt(e.target.value))}
                className="w-20"
              />
            </div>
          </div>

          <div className="flex items-center justify-between p-4 rounded-lg bg-yellow-50 dark:bg-yellow-950/20 border border-yellow-200 dark:border-yellow-900">
            <div>
              <div className="font-medium text-yellow-900 dark:text-yellow-100">{t('organizationSettings.mediumRisk')}</div>
              <div className="text-sm text-yellow-700 dark:text-yellow-300">
                {t('organizationSettings.mediumRiskRange', { min: (formData.riskThresholds?.lowMax || 30) + 1, max: formData.riskThresholds?.mediumMax || 60 })}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="mediumMax" className="text-xs">{t('organizationSettings.maxScore')}</Label>
              <Input
                id="mediumMax"
                type="number"
                min="0"
                max="99"
                value={formData.riskThresholds?.mediumMax || 60}
                onChange={(e) => updateRiskThreshold('mediumMax', parseInt(e.target.value))}
                className="w-20"
              />
            </div>
          </div>

          <div className="flex items-center justify-between p-4 rounded-lg bg-orange-50 dark:bg-orange-950/20 border border-orange-200 dark:border-orange-900">
            <div>
              <div className="font-medium text-orange-900 dark:text-orange-100">{t('organizationSettings.highRisk')}</div>
              <div className="text-sm text-orange-700 dark:text-orange-300">
                {t('organizationSettings.highRiskRange', { min: (formData.riskThresholds?.mediumMax || 60) + 1, max: formData.riskThresholds?.highMax || 85 })}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="highMax" className="text-xs">{t('organizationSettings.maxScore')}</Label>
              <Input
                id="highMax"
                type="number"
                min="0"
                max="99"
                value={formData.riskThresholds?.highMax || 85}
                onChange={(e) => updateRiskThreshold('highMax', parseInt(e.target.value))}
                className="w-20"
              />
            </div>
          </div>

          <div className="p-4 rounded-lg bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900">
            <div className="font-medium text-red-900 dark:text-red-100">{t('organizationSettings.criticalRisk')}</div>
            <div className="text-sm text-red-700 dark:text-red-300">
              {t('organizationSettings.criticalRiskRange', { min: (formData.riskThresholds?.highMax || 85) + 1 })}
            </div>
          </div>
        </div>

        <Alert>
          <AlertTriangle className="h-4 w-4" />
          <AlertDescription>
            {t('organizationSettings.thresholdsChangeNote')}
          </AlertDescription>
        </Alert>
      </CardContent>
    </Card>
  );
}
