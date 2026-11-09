import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';
import { Mail, Loader2 } from 'lucide-react';
import { Button } from '../ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Separator } from '../ui/separator';
import { organizationSettingsService } from '../../services/organizationSettingsService';
import { useToast } from '../../contexts';

/**
 * How email is set up on this server. Email is configured through the server's SMTP environment
 * variables (the person running ShipFlow sets them and restarts it) — there is nothing to edit here,
 * so this shows what the server is really using and lets an admin send a test message.
 */
export function EmailSettingsTab() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [sendingTestEmail, setSendingTestEmail] = useState(false);

  const { data: status, isLoading, isError } = useQuery({
    queryKey: ['email-status'],
    queryFn: async () => (await organizationSettingsService.getEmailStatus()).data,
  });

  const handleSendTestEmail = async () => {
    setSendingTestEmail(true);
    try {
      const response = await organizationSettingsService.sendTestEmail();
      if (response.data.error) {
        showToast(t('emailSettings.testEmailFailed', { reason: response.data.error }), 'error');
      } else {
        showToast(t('emailSettings.testEmailSent'), 'success');
      }
    } catch {
      showToast(t('emailSettings.testEmailRequestFailed'), 'error');
    } finally {
      setSendingTestEmail(false);
    }
  };

  const notSet = t('emailSettings.notSet');
  const rows: Array<[string, string]> = status
    ? [
        [t('emailSettings.smtpHost'), status.host ?? notSet],
        [t('emailSettings.smtpPort'), status.port != null ? String(status.port) : notSet],
        [t('emailSettings.smtpUsername'), status.username ?? notSet],
        [t('emailSettings.smtpFrom'), status.from ?? notSet],
        [t('emailSettings.replyTo'), status.replyTo ?? notSet],
        [t('emailSettings.linkBase'), status.baseUrl ?? notSet],
        [t('emailSettings.encryption'), status.starttlsRequired ? t('emailSettings.starttlsRequired') : t('emailSettings.starttlsOptional')],
      ]
    : [];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Mail className="h-5 w-5" />
          {t('emailSettings.title')}
        </CardTitle>
        <CardDescription>{t('emailSettings.serverManagedNote')}</CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {isLoading && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Loader2 className="h-4 w-4 animate-spin" />
            {t('emailSettings.loading')}
          </div>
        )}
        {isError && <p className="text-sm text-destructive">{t('emailSettings.statusLoadFailed')}</p>}

        {status && (
          <>
            <p
              className={status.configured ? 'text-sm font-medium text-green-600' : 'text-sm font-medium text-amber-600'}
              data-testid="email-status"
            >
              {status.configured ? t('emailSettings.statusOn') : t('emailSettings.statusOff')}
            </p>

            {status.configured && (
              <dl className="grid grid-cols-1 gap-x-6 gap-y-3 sm:grid-cols-[max-content_1fr]">
                {rows.map(([label, value]) => (
                  <div key={label} className="contents">
                    <dt className="text-sm text-muted-foreground">{label}</dt>
                    <dd className="break-all text-sm">{value}</dd>
                  </div>
                ))}
              </dl>
            )}

            <p className="text-sm text-muted-foreground italic">{t('emailSettings.smtpPasswordNote')}</p>
          </>
        )}

        <Separator />

        <div className="flex items-center gap-3">
          <Button variant="outline" onClick={handleSendTestEmail} disabled={sendingTestEmail || (status && !status.configured)}>
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
