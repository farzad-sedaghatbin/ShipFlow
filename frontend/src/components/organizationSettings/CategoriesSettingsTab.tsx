import { useTranslation } from 'react-i18next';
import { Tags } from 'lucide-react';
import { Badge } from '../ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Separator } from '../ui/separator';
import { OrganizationSettings } from '../../types/organizationSettings';

interface CategoriesSettingsTabProps {
  formData: Partial<OrganizationSettings>;
}

export function CategoriesSettingsTab({ formData }: CategoriesSettingsTabProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Tags className="h-5 w-5" />
          {t('organizationSettings.taskPitchCategories')}
        </CardTitle>
        <CardDescription>{t('organizationSettings.categoriesDesc')}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div>
          <h3 className="font-semibold mb-3">{t('organizationSettings.taskCategories')}</h3>
          <div className="space-y-2">
            {formData.taskCategories?.map((category, index) => (
              <div key={index} className="flex items-center gap-3 p-3 rounded-lg border">
                <div
                  className="w-4 h-4 rounded"
                  style={{ backgroundColor: category.color }}
                />
                <div className="flex-1">
                  <div className="font-medium">{category.name}</div>
                  <div className="text-xs text-muted-foreground">{category.description}</div>
                </div>
                <Badge variant={category.isActive ? 'default' : 'secondary'}>
                  {category.isActive ? t('common.active') : t('common.inactive')}
                </Badge>
              </div>
            ))}
          </div>
        </div>

        <Separator />

        <div>
          <h3 className="font-semibold mb-3">{t('organizationSettings.pitchCategories')}</h3>
          <div className="space-y-2">
            {formData.pitchCategories?.map((category, index) => (
              <div key={index} className="flex items-center gap-3 p-3 rounded-lg border">
                <div
                  className="w-4 h-4 rounded"
                  style={{ backgroundColor: category.color }}
                />
                <div className="flex-1">
                  <div className="font-medium">{category.name}</div>
                  <div className="text-xs text-muted-foreground">{category.description}</div>
                </div>
                <Badge variant={category.isActive ? 'default' : 'secondary'}>
                  {category.isActive ? t('common.active') : t('common.inactive')}
                </Badge>
              </div>
            ))}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
