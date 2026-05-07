import { useTranslation } from 'react-i18next';
import { Globe } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { OrganizationSettings } from '../../types/organizationSettings';

const TIME_ZONES = [
  'UTC',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'Europe/London',
  'Europe/Paris',
  'Asia/Tokyo',
  'Asia/Shanghai',
  'Australia/Sydney',
];

const DATE_FORMATS = [
  { value: 'MM/DD/YYYY', label: 'MM/DD/YYYY (US)' },
  { value: 'DD/MM/YYYY', label: 'DD/MM/YYYY (Europe)' },
  { value: 'YYYY-MM-DD', label: 'YYYY-MM-DD (ISO)' },
];

interface GeneralSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
}

export function GeneralSettingsTab({ formData, setFormData }: GeneralSettingsTabProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Globe className="h-5 w-5" />
          {t('organizationSettings.generalInfo')}
        </CardTitle>
        <CardDescription>{t('organizationSettings.generalDesc')}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="space-y-2">
          <Label htmlFor="orgName">{t('organizationSettings.organizationName')}</Label>
          <Input
            id="orgName"
            value={formData.organizationName || ''}
            onChange={(e) => setFormData({ ...formData, organizationName: e.target.value })}
            placeholder={t('organizationSettings.organizationNamePlaceholder')}
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-2">
            <Label htmlFor="timezone">{t('organizationSettings.timeZone')}</Label>
            <select
              id="timezone"
              value={formData.timeZone || 'UTC'}
              onChange={(e) => setFormData({ ...formData, timeZone: e.target.value })}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {TIME_ZONES.map((tz) => (
                <option key={tz} value={tz}>
                  {tz}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="dateFormat">{t('organizationSettings.dateFormat')}</Label>
            <select
              id="dateFormat"
              value={formData.dateFormat || 'MM/DD/YYYY'}
              onChange={(e) => setFormData({ ...formData, dateFormat: e.target.value })}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {DATE_FORMATS.map((fmt) => (
                <option key={fmt.value} value={fmt.value}>
                  {fmt.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
