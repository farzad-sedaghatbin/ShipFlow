import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Mail, Loader2 } from 'lucide-react';
import { Button } from '../ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Separator } from '../ui/separator';
import { Switch } from '../ui/switch';
import { OrganizationSettings } from '../../types/organizationSettings';
import { organizationSettingsService } from '../../services/organizationSettingsService';
import { useToast } from '../../contexts';

interface EmailSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
}

export function EmailSettingsTab({ formData, setFormData }: EmailSettingsTabProps) {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [sendingTestEmail, setSendingTestEmail] = useState(false);

  const handleSendTestEmail = async () => {
    setSendingTestEmail(true);
    try {
      const response = await organizationSettingsService.sendTestEmail();
      if (response.data.error) {
        showToast(t('emailSettings.emailNotConfigured'), 'error');
      } else {
        showToast(t('emailSettings.testEmailSent'), 'success');
      }
    } catch {
      showToast(t('emailSettings.emailNotConfigured'), 'error');
    } finally {
      setSendingTestEmail(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Mail className="h-5 w-5" />
          {t('emailSettings.title')}
        </CardTitle>
        <CardDescription>{t('emailSettings.enabled')}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* Enable toggle */}
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="email-notifications-enabled">{t('emailSettings.enabled')}</Label>
          </div>
          <Switch
            id="email-notifications-enabled"
            checked={formData.emailNotificationsEnabled ?? true}
            onCheckedChange={(checked) =>
              setFormData({ ...formData, emailNotificationsEnabled: checked })
            }
          />
        </div>

        <Separator />

        {/* SMTP Host */}
        <div className="space-y-2">
          <Label htmlFor="smtp-host">{t('emailSettings.smtpHost')}</Label>
          <Input
            id="smtp-host"
            value={formData.smtpHost ?? ''}
            onChange={(e) => setFormData({ ...formData, smtpHost: e.target.value })}
            placeholder="smtp.example.com"
          />
        </div>

        {/* SMTP Port */}
        <div className="space-y-2">
          <Label htmlFor="smtp-port">{t('emailSettings.smtpPort')}</Label>
          <Input
            id="smtp-port"
            type="number"
            value={formData.smtpPort ?? 587}
            onChange={(e) =>
              setFormData({ ...formData, smtpPort: parseInt(e.target.value, 10) || 587 })
            }
          />
        </div>

        {/* SMTP Username */}
        <div className="space-y-2">
          <Label htmlFor="smtp-username">{t('emailSettings.smtpUsername')}</Label>
          <Input
            id="smtp-username"
            value={formData.smtpUsername ?? ''}
            onChange={(e) => setFormData({ ...formData, smtpUsername: e.target.value })}
            placeholder="user@example.com"
          />
        </div>

        {/* From address */}
        <div className="space-y-2">
          <Label htmlFor="smtp-from">{t('emailSettings.smtpFrom')}</Label>
          <Input
            id="smtp-from"
            value={formData.smtpFrom ?? 'noreply@shipflow.dev'}
            onChange={(e) => setFormData({ ...formData, smtpFrom: e.target.value })}
            placeholder="noreply@shipflow.dev"
          />
        </div>

        {/* TLS toggle */}
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="smtp-tls">{t('emailSettings.tlsEnabled')}</Label>
          </div>
          <Switch
            id="smtp-tls"
            checked={formData.smtpTlsEnabled ?? true}
            onCheckedChange={(checked) =>
              setFormData({ ...formData, smtpTlsEnabled: checked })
            }
          />
        </div>

        {/* Password note */}
        <p className="text-sm text-muted-foreground italic">
          {t('emailSettings.smtpPasswordNote')}
        </p>

        <Separator />

        {/* Test email button */}
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            onClick={handleSendTestEmail}
            disabled={sendingTestEmail}
          >
            {sendingTestEmail ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                {t('emailSettings.sendTestEmail')}
              </>
            ) : (
              <>
                <Mail className="h-4 w-4 mr-2" />
                {t('emailSettings.sendTestEmail')}
              </>
            )}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
