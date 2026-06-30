import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Download, FileDown, Loader2 } from 'lucide-react';
import { useAuth, useToast } from '../../contexts';
import {
  auditService,
  AuditEntityType,
  AuditExportFormat,
} from '../../services/auditService';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';

export function AuditExportSettingsTab() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const { showToast } = useToast();

  const [entityType, setEntityType] = useState<AuditEntityType>('all');
  const [format, setFormat] = useState<AuditExportFormat>('csv');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [exporting, setExporting] = useState(false);

  // Defence in depth: the tab is only registered for admins, but never render
  // the export controls for a non-admin even if reached directly.
  if (user?.role !== 'ADMIN') {
    return null;
  }

  const handleExport = async () => {
    // Client-side guard so the obvious mistake is caught before the round trip;
    // the backend also rejects from > to with HTTP 400.
    if (from && to && from > to) {
      showToast(t('auditExport.dateRangeError'), 'error');
      return;
    }

    setExporting(true);
    try {
      await auditService.exportAuditTrail({
        entityType,
        format,
        from: from || undefined,
        to: to || undefined,
      });
    } catch {
      showToast(t('auditExport.exportFailed'), 'error');
    } finally {
      setExporting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h3 className="text-base font-semibold flex items-center gap-2">
          <FileDown className="h-4 w-4" />
          {t('auditExport.title')}
        </h3>
        <p className="text-sm text-muted-foreground mt-0.5">{t('auditExport.subtitle')}</p>
      </div>

      {/* Entity type */}
      <div className="space-y-2">
        <Label htmlFor="audit-entity-type">{t('auditExport.entityType')}</Label>
        <Select value={entityType} onValueChange={(v) => setEntityType(v as AuditEntityType)}>
          <SelectTrigger id="audit-entity-type" className="w-full sm:w-72">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="task">{t('auditExport.entityTask')}</SelectItem>
            <SelectItem value="bug">{t('auditExport.entityBug')}</SelectItem>
            <SelectItem value="pitch">{t('auditExport.entityPitch')}</SelectItem>
            <SelectItem value="testcase">{t('auditExport.entityTestCase')}</SelectItem>
            <SelectItem value="all">{t('auditExport.entityAll')}</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Date range */}
      <div className="grid gap-4 sm:grid-cols-2 sm:max-w-2xl">
        <div className="space-y-2">
          <Label htmlFor="audit-from">{t('auditExport.fromDate')}</Label>
          <Input
            id="audit-from"
            type="date"
            value={from}
            max={to || undefined}
            onChange={(e) => setFrom(e.target.value)}
          />
        </div>
        <div className="space-y-2">
          <Label htmlFor="audit-to">{t('auditExport.toDate')}</Label>
          <Input
            id="audit-to"
            type="date"
            value={to}
            min={from || undefined}
            onChange={(e) => setTo(e.target.value)}
          />
        </div>
      </div>

      {/* Format */}
      <div className="space-y-2">
        <Label htmlFor="audit-format">{t('auditExport.format')}</Label>
        <Select value={format} onValueChange={(v) => setFormat(v as AuditExportFormat)}>
          <SelectTrigger id="audit-format" className="w-full sm:w-72">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="csv">{t('auditExport.formatCsv')}</SelectItem>
            <SelectItem value="json">{t('auditExport.formatJson')}</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Helper text */}
      <p className="text-sm text-muted-foreground max-w-2xl">{t('auditExport.helperText')}</p>

      {/* Action */}
      <div>
        <Button type="button" onClick={handleExport} disabled={exporting}>
          {exporting ? (
            <Loader2 className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <Download className="mr-2 h-4 w-4" />
          )}
          {exporting ? t('auditExport.exporting') : t('auditExport.export')}
        </Button>
      </div>
    </div>
  );
}
