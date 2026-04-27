import { useTranslation } from 'react-i18next';
import { Calendar, Clock, Sparkles } from 'lucide-react';
import { Alert, AlertDescription } from '../ui/alert';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { OrganizationSettings } from '../../types/organizationSettings';

interface CycleSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
}

export function CycleSettingsTab({ formData, setFormData }: CycleSettingsTabProps) {
  const { t } = useTranslation();

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="h-5 w-5" />
            {t('organizationSettings.cycleConfiguration')}
          </CardTitle>
          <CardDescription>{t('organizationSettings.cycleDesc')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <Label htmlFor="cycleLength">{t('organizationSettings.defaultCycleLength')}</Label>
              <Input
                id="cycleLength"
                type="number"
                min="1"
                max="12"
                value={formData.defaultCycleLengthWeeks || 6}
                onChange={(e) =>
                  setFormData({ ...formData, defaultCycleLengthWeeks: parseInt(e.target.value) })
                }
              />
              <p className="text-xs text-muted-foreground">
                {t('organizationSettings.defaultCycleLengthNote')}
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="cooldownLength">{t('organizationSettings.defaultCooldown')}</Label>
              <Input
                id="cooldownLength"
                type="number"
                min="0"
                max="4"
                value={formData.defaultCooldownWeeks || 2}
                onChange={(e) =>
                  setFormData({ ...formData, defaultCooldownWeeks: parseInt(e.target.value) })
                }
              />
              <p className="text-xs text-muted-foreground">
                {t('organizationSettings.defaultCooldownNote')}
              </p>
            </div>
          </div>

          <Alert>
            <Clock className="h-4 w-4" />
            <AlertDescription>
              {t('organizationSettings.cycleDefaultsNote')}
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>

      {/* Capacity Configuration */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5" />
            {t('organizationSettings.capacityConfiguration', 'Capacity Configuration')}
          </CardTitle>
          <CardDescription>
            {t('organizationSettings.capacityDesc', 'Configure default work hours and working days. Teams and individuals can override these values.')}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-2">
              <Label htmlFor="hoursPerDay">{t('organizationSettings.defaultHoursPerDay', 'Default Hours Per Day')}</Label>
              <Input
                id="hoursPerDay"
                type="number"
                min="1"
                max="24"
                step="0.5"
                value={formData.defaultHoursPerDay || 8}
                onChange={(e) =>
                  setFormData({ ...formData, defaultHoursPerDay: parseFloat(e.target.value) })
                }
              />
              <p className="text-xs text-muted-foreground">
                {t('organizationSettings.defaultHoursPerDayNote', 'Standard working hours per day. Teams and individuals can override this setting.')}
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="workingDaysPerWeek">{t('organizationSettings.defaultWorkingDaysPerWeek', 'Default Working Days Per Week')}</Label>
              <Input
                id="workingDaysPerWeek"
                type="number"
                min="1"
                max="7"
                value={formData.defaultWorkingDaysPerWeek || 5}
                onChange={(e) =>
                  setFormData({ ...formData, defaultWorkingDaysPerWeek: parseInt(e.target.value) })
                }
              />
              <p className="text-xs text-muted-foreground">
                {t('organizationSettings.defaultWorkingDaysPerWeekNote', 'Used to convert weeks to days for appetite calculations. E.g., 2 weeks = 10 working days (if 5 days/week).')}
              </p>
            </div>
          </div>

          <Alert>
            <Sparkles className="h-4 w-4" />
            <AlertDescription>
              {t('organizationSettings.capacityInheritanceNote', 'Capacity inheritance: Organization → Team → Person → Team Assignment. More specific settings override general ones.')}
            </AlertDescription>
          </Alert>
        </CardContent>
      </Card>
    </div>
  );
}
