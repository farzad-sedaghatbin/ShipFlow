import { useTranslation } from 'react-i18next';
import { Bug } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Separator } from '../ui/separator';
import { Switch } from '../ui/switch';
import { OrganizationSettings } from '../../types/organizationSettings';

interface BugSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
}

export function BugSettingsTab({ formData, setFormData }: BugSettingsTabProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Bug className="h-5 w-5" />
          {t('organizationSettings.bugTracking')}
        </CardTitle>
        <CardDescription>{t('organizationSettings.bugTrackingDesc')}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div>
          <h3 className="font-semibold mb-3">{t('organizationSettings.bugStatuses')}</h3>
          <div className="space-y-2">
            {formData.bugStatuses?.map((status, index) => (
              <div key={index} className="flex items-center gap-3 p-3 rounded-lg border">
                <Input
                  type="color"
                  value={status.color}
                  onChange={(e) => {
                    const updated = [...(formData.bugStatuses || [])];
                    updated[index] = { ...updated[index], color: e.target.value };
                    setFormData({ ...formData, bugStatuses: updated });
                  }}
                  className="w-12 h-10 p-1 cursor-pointer"
                />
                <div className="flex-1 space-y-1">
                  <Input
                    value={status.name}
                    onChange={(e) => {
                      const updated = [...(formData.bugStatuses || [])];
                      updated[index] = { ...updated[index], name: e.target.value };
                      setFormData({ ...formData, bugStatuses: updated });
                    }}
                    className="font-medium"
                    placeholder={t('organizationSettings.statusName')}
                  />
                  <Input
                    value={status.description}
                    onChange={(e) => {
                      const updated = [...(formData.bugStatuses || [])];
                      updated[index] = { ...updated[index], description: e.target.value };
                      setFormData({ ...formData, bugStatuses: updated });
                    }}
                    className="text-sm"
                    placeholder={t('organizationSettings.description')}
                  />
                </div>
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-1">
                    <Switch
                      checked={status.isActive}
                      onCheckedChange={(checked) => {
                        const updated = [...(formData.bugStatuses || [])];
                        updated[index] = { ...updated[index], isActive: checked };
                        setFormData({ ...formData, bugStatuses: updated });
                      }}
                    />
                    <Label className="text-xs">{t('organizationSettings.active')}</Label>
                  </div>
                  <div className="flex items-center gap-1">
                    <Switch
                      checked={status.isClosed}
                      onCheckedChange={(checked) => {
                        const updated = [...(formData.bugStatuses || [])];
                        updated[index] = { ...updated[index], isClosed: checked };
                        setFormData({ ...formData, bugStatuses: updated });
                      }}
                    />
                    <Label className="text-xs">{t('organizationSettings.closed')}</Label>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <Separator />

        <div>
          <h3 className="font-semibold mb-3">{t('organizationSettings.severityLevels')}</h3>
          <div className="space-y-2">
            {formData.severityLevels?.map((severity, index) => (
              <div key={index} className="flex items-center gap-3 p-3 rounded-lg border">
                <Input
                  type="color"
                  value={severity.color}
                  onChange={(e) => {
                    const updated = [...(formData.severityLevels || [])];
                    updated[index] = { ...updated[index], color: e.target.value };
                    setFormData({ ...formData, severityLevels: updated });
                  }}
                  className="w-12 h-10 p-1 cursor-pointer"
                />
                <div className="flex-1 space-y-1">
                  <Input
                    value={severity.name}
                    onChange={(e) => {
                      const updated = [...(formData.severityLevels || [])];
                      updated[index] = { ...updated[index], name: e.target.value };
                      setFormData({ ...formData, severityLevels: updated });
                    }}
                    className="font-medium"
                    placeholder={t('organizationSettings.severityName')}
                  />
                  <Input
                    value={severity.description}
                    onChange={(e) => {
                      const updated = [...(formData.severityLevels || [])];
                      updated[index] = { ...updated[index], description: e.target.value };
                      setFormData({ ...formData, severityLevels: updated });
                    }}
                    className="text-sm"
                    placeholder={t('organizationSettings.description')}
                  />
                </div>
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-1">
                    <Label className="text-xs">{t('organizationSettings.priority')}:</Label>
                    <Input
                      type="number"
                      min="1"
                      max="10"
                      value={severity.priority}
                      onChange={(e) => {
                        const updated = [...(formData.severityLevels || [])];
                        updated[index] = { ...updated[index], priority: parseInt(e.target.value) };
                        setFormData({ ...formData, severityLevels: updated });
                      }}
                      className="w-16"
                    />
                  </div>
                  <div className="flex items-center gap-1">
                    <Switch
                      checked={severity.isActive}
                      onCheckedChange={(checked) => {
                        const updated = [...(formData.severityLevels || [])];
                        updated[index] = { ...updated[index], isActive: checked };
                        setFormData({ ...formData, severityLevels: updated });
                      }}
                    />
                    <Label className="text-xs">{t('organizationSettings.active')}</Label>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
