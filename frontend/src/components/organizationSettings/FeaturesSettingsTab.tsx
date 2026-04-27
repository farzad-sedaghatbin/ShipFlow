import { useTranslation } from 'react-i18next';
import { Sparkles, Beaker } from 'lucide-react';
import { Badge } from '../ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Label } from '../ui/label';
import { Separator } from '../ui/separator';
import { Switch } from '../ui/switch';
import { OrganizationSettings } from '../../types/organizationSettings';
import { formatLocalizedDateTime } from '../../utils/dateLocalization';

interface FeaturesSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
  settings: OrganizationSettings | null;
}

export function FeaturesSettingsTab({ formData, setFormData, settings }: FeaturesSettingsTabProps) {
  const { t, i18n } = useTranslation();

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Sparkles className="h-5 w-5" />
            {t('organizationSettings.featureToggles')}
          </CardTitle>
          <CardDescription>{t('organizationSettings.featuresDesc')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-muted-foreground" />
                <Label htmlFor="ai-features">{t('organizationSettings.aiFeatures')}</Label>
              </div>
              <p className="text-sm text-muted-foreground">
                {t('organizationSettings.aiFeaturesDesc')}
              </p>
            </div>
            <Switch
              id="ai-features"
              checked={formData.enableAIFeatures ?? true}
              onCheckedChange={(checked) =>
                setFormData({ ...formData, enableAIFeatures: checked })
              }
            />
          </div>

          <Separator />

          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <div className="flex items-center gap-2">
                <Beaker className="h-4 w-4 text-muted-foreground" />
                <Label htmlFor="wise-architecture">{t('organizationSettings.wiseArchitecture')}</Label>
                <Badge variant="secondary" className="text-xs">
                  {t('common.experimental')}
                </Badge>
              </div>
              <p className="text-sm text-muted-foreground">
                {t('organizationSettings.wiseArchitectureDesc')}
              </p>
            </div>
            <Switch
              id="wise-architecture"
              checked={formData.enableWiseArchitecture ?? false}
              onCheckedChange={(checked) =>
                setFormData({ ...formData, enableWiseArchitecture: checked })
              }
              disabled={!(formData.enableAIFeatures ?? true)}
            />
          </div>
        </CardContent>
      </Card>

      {settings && (
        <Card>
          <CardContent className="pt-6">
            <p className="text-sm text-muted-foreground">
              {t('organizationSettings.lastUpdated', {
                date: formatLocalizedDateTime(new Date(settings.updatedAt), i18n.language),
              })}{' '}
              by {settings.updatedBy}
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
