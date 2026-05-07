import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2 } from 'lucide-react';
import { formatLocalizedDate } from '../../utils/dateLocalization';
import { LocalizedDateInput } from '../LocalizedDateInput';
import { WorkLog, CreateWorkLogForSelfRequest } from '../../types';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Badge } from '../ui/badge';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';

interface PitchWorkLogsSectionProps {
  workLogs: WorkLog[];
  workLogTotalElements: number;
  workLogPageSize: number;
  workLogDialog: boolean;
  newWorkLog: CreateWorkLogForSelfRequest;
  workLogDate: string;
  language: string;
  onSetWorkLogDialog: (open: boolean) => void;
  onCreateWorkLog: () => void;
  onDeleteWorkLog: (workLogId: number) => void;
  onNewWorkLogChange: (wl: CreateWorkLogForSelfRequest) => void;
  onWorkLogDateChange: (date: string) => void;
}

export function PitchWorkLogsSection({
  workLogs,
  workLogTotalElements,
  workLogPageSize,
  workLogDialog,
  newWorkLog,
  workLogDate,
  language,
  onSetWorkLogDialog,
  onCreateWorkLog,
  onDeleteWorkLog,
  onNewWorkLogChange,
  onWorkLogDateChange,
}: PitchWorkLogsSectionProps) {
  const { t } = useTranslation();

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle>{t('pitchDetailPage.workLogs')}</CardTitle>
            <Button size="sm" onClick={() => onSetWorkLogDialog(true)}>
              <Plus className="h-4 w-4 mr-1" />
              {t('pitchDetailPage.add')}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {workLogs.length === 0 ? (
            <p className="text-muted-foreground">{t('pitchDetailPage.noWorkLogs')}</p>
          ) : (
            <div className="space-y-1">
              {workLogs.map((wl, index) => (
                <div key={wl.id}>
                  <div className="flex items-start justify-between py-3">
                    <div className="flex-1 pr-4">
                      <div className="flex justify-between items-center mb-1">
                        <span className="font-medium">{wl.personName}</span>
                        <Badge variant="secondary">{wl.hoursSpent}h</Badge>
                      </div>
                      <p className="text-sm text-muted-foreground">
                        {formatLocalizedDate(new Date(wl.date), language)}
                        {wl.note && ` • ${wl.note}`}
                      </p>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      onClick={() => onDeleteWorkLog(wl.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                  {index < workLogs.length - 1 && (
                    <div className="border-b border-border" />
                  )}
                </div>
              ))}
              {workLogTotalElements > workLogPageSize && (
                <div className="pt-3 text-center">
                  <Link to="/time/logs" className="text-sm text-primary hover:underline">
                    {t('pitchDetailPage.viewAllWorkLogs', { total: workLogTotalElements, defaultValue: 'View all {{total}} work logs →' })}
                  </Link>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add Work Log Dialog */}
      <Dialog open={workLogDialog} onOpenChange={onSetWorkLogDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('pitchDetailPage.addWorkLog')}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="worklog-date">{t('pitchDetailPage.date')} *</Label>
                <LocalizedDateInput
                  id="worklog-date"
                  value={workLogDate}
                  onChange={onWorkLogDateChange}
                  aria-required="true"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="hours">{t('pitchDetailPage.hours')} *</Label>
                <Input
                  id="hours"
                  type="number"
                  value={newWorkLog.hoursSpent || ''}
                  onChange={(e) =>
                    onNewWorkLogChange({
                      ...newWorkLog,
                      hoursSpent: parseFloat(e.target.value) || 0,
                    })
                  }
                  min={0.25}
                  step={0.25}
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="note">{t('pitchDetailPage.note')}</Label>
              <Textarea
                id="note"
                value={newWorkLog.note}
                onChange={(e) =>
                  onNewWorkLogChange({ ...newWorkLog, note: e.target.value })
                }
                rows={2}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => onSetWorkLogDialog(false)}>
              {t('pitchDetailPage.cancel')}
            </Button>
            <Button
              onClick={onCreateWorkLog}
              disabled={!newWorkLog.hoursSpent}
            >
              {t('pitchDetailPage.add')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
