import { useTranslation } from 'react-i18next';
import { Palette } from 'lucide-react';
import { Alert, AlertDescription } from '../ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { OrganizationSettings, ColorSettings } from '../../types/organizationSettings';

const DEFAULT_COLORS: ColorSettings = {
  appetiteHours: '#3B82F6',
  actualHours: '#10B981',
  overBudget: '#EF4444',
  underBudget: '#22C55E',
};

interface ColorsSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
}

export function ColorsSettingsTab({ formData, setFormData }: ColorsSettingsTabProps) {
  const { t } = useTranslation();

  const colorFields = [
    {
      id: 'appetiteColor',
      key: 'appetiteHours' as keyof ColorSettings,
      labelKey: 'organizationSettings.appetiteHoursColor',
      descKey: 'organizationSettings.appetiteHoursColorDesc',
    },
    {
      id: 'actualColor',
      key: 'actualHours' as keyof ColorSettings,
      labelKey: 'organizationSettings.actualHoursColor',
      descKey: 'organizationSettings.actualHoursColorDesc',
    },
    {
      id: 'overBudgetColor',
      key: 'overBudget' as keyof ColorSettings,
      labelKey: 'organizationSettings.overBudgetColor',
      descKey: 'organizationSettings.overBudgetColorDesc',
    },
    {
      id: 'underBudgetColor',
      key: 'underBudget' as keyof ColorSettings,
      labelKey: 'organizationSettings.underBudgetColor',
      descKey: 'organizationSettings.underBudgetColorDesc',
    },
  ];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Palette className="h-5 w-5" />
          {t('organizationSettings.colorConfiguration')}
        </CardTitle>
        <CardDescription>
          {t('organizationSettings.colorsDesc')}
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {colorFields.map(({ id, key, labelKey, descKey }) => (
            <div key={id} className="space-y-2">
              <Label htmlFor={id}>{t(labelKey)}</Label>
              <div className="flex gap-2 items-center">
                <Input
                  id={id}
                  type="color"
                  value={formData.colors?.[key] || DEFAULT_COLORS[key]}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      colors: { ...formData.colors!, [key]: e.target.value },
                    })
                  }
                  className="w-20 h-10"
                />
                <span className="text-sm text-muted-foreground">
                  {formData.colors?.[key] || DEFAULT_COLORS[key]}
                </span>
              </div>
              <p className="text-xs text-muted-foreground">{t(descKey)}</p>
            </div>
          ))}
        </div>

        <Alert>
          <Palette className="h-4 w-4" />
          <AlertDescription>
            {t('organizationSettings.colorsApplyNote')}
          </AlertDescription>
        </Alert>
      </CardContent>
    </Card>
  );
}
