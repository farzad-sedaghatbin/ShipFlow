import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { Plus, Trash2, Clock } from 'lucide-react';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { LocalizedDateInput } from './LocalizedDateInput';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import { Badge } from './ui/badge';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { WorkLog } from '../types';
import { workLogService } from '../services/workLogService';
import { useAuth } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';

interface TaskWorkLogsSectionProps {
  taskId: number;
  /** Called after the user adds or deletes a worklog so the parent can refresh
   *  derived stats (total hours, progress bars, etc.). Optional. */
  onWorkLogsChanged?: () => void;
}

const PAGE_SIZE = 20;

/**
 * Self-contained worklog list for a single task. Renders the task's recent
 * worklogs, lets the current user add their own and delete their own. Admin
 * deletion of other users' worklogs is intentionally out of scope here — the
 * dedicated time-logs page handles that.
 */
export default function TaskWorkLogsSection({ taskId, onWorkLogsChanged }: TaskWorkLogsSectionProps) {
  const { t, i18n } = useTranslation();
  const { user } = useAuth();
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalHours, setTotalHours] = useState(0);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [date, setDate] = useState(() => new Date().toISOString().split('T')[0]);
  const [hours, setHours] = useState<number | ''>('');
  const [note, setNote] = useState('');

  const load = useCallback(async () => {
    try {
      setLoading(true);
      const response = await workLogService.getByTaskId(taskId, 0, PAGE_SIZE);
      setWorkLogs(response.data.content);
      setTotalElements(response.data.page?.totalElements ?? response.data.totalElements ?? 0);
      // Sum from the loaded page — over PAGE_SIZE entries we show a "view all"
      // link so the partial sum is acceptable for the inline display.
      setTotalHours(response.data.content.reduce((acc, wl) => acc + (wl.hoursSpent || 0), 0));
    } catch (err) {
      toast.error(getUserFriendlyError(err, t('taskWorkLogs.loadFailed')));
    } finally {
      setLoading(false);
    }
  }, [taskId, t]);

  useEffect(() => {
    load();
  }, [load]);

  const resetForm = () => {
    setDate(new Date().toISOString().split('T')[0]);
    setHours('');
    setNote('');
  };

  const handleCreate = async () => {
    if (!hours || hours <= 0) return;
    setSubmitting(true);
    try {
      await workLogService.createMy({
        taskId,
        date,
        hoursSpent: Number(hours),
        note: note.trim() || undefined,
      });
      setDialogOpen(false);
      resetForm();
      await load();
      onWorkLogsChanged?.();
      toast.success(t('taskWorkLogs.added'));
    } catch (err) {
      toast.error(getUserFriendlyError(err, t('taskWorkLogs.addFailed')));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (workLog: WorkLog) => {
    if (!window.confirm(t('taskWorkLogs.deleteConfirm'))) return;
    try {
      // Only the worklog's owner is allowed to call /worklogs/my/{id};
      // for everyone else we silently fall through to the admin endpoint,
      // which the backend will gate via @PreAuthorize.
      const isOwn = user?.personId && user.personId === workLog.personId;
      if (isOwn) {
        await workLogService.deleteMy(workLog.id);
      } else {
        await workLogService.delete(workLog.id);
      }
      await load();
      onWorkLogsChanged?.();
      toast.success(t('taskWorkLogs.deleted'));
    } catch (err) {
      toast.error(getUserFriendlyError(err, t('taskWorkLogs.deleteFailed')));
    }
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle className="text-lg flex items-center gap-2">
              <Clock className="h-5 w-5" />
              {t('taskWorkLogs.title')}
              {totalElements > 0 && (
                <Badge variant="secondary" className="ml-1">
                  {totalElements}
                </Badge>
              )}
            </CardTitle>
            <Button
              size="sm"
              onClick={() => {
                resetForm();
                setDialogOpen(true);
              }}
            >
              <Plus className="h-4 w-4 mr-1" />
              {t('taskWorkLogs.add')}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p className="text-muted-foreground text-sm">{t('common.loading')}</p>
          ) : workLogs.length === 0 ? (
            <p className="text-muted-foreground text-sm">{t('taskWorkLogs.empty')}</p>
          ) : (
            <div className="space-y-1">
              {workLogs.map((wl, index) => {
                const canDelete = user?.personId === wl.personId;
                return (
                  <div key={wl.id}>
                    <div className="flex items-start justify-between py-3">
                      <div className="flex-1 pr-4 min-w-0">
                        <div className="flex justify-between items-center mb-1 gap-2">
                          <span className="font-medium truncate">{wl.personName}</span>
                          <Badge variant="secondary">{wl.hoursSpent}h</Badge>
                        </div>
                        <p className="text-sm text-muted-foreground break-words">
                          {formatLocalizedDate(new Date(wl.date), i18n.language)}
                          {wl.note && ` • ${wl.note}`}
                        </p>
                      </div>
                      {canDelete && (
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          aria-label={t('taskWorkLogs.delete')}
                          onClick={() => handleDelete(wl)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                    {index < workLogs.length - 1 && <div className="border-b border-border" />}
                  </div>
                );
              })}
              <div className="pt-3 flex items-center justify-between text-sm">
                <span className="text-muted-foreground">
                  {t('taskWorkLogs.totalShown', {
                    hours: totalHours.toFixed(2).replace(/\.00$/, ''),
                  })}
                </span>
                {totalElements > workLogs.length && (
                  <span className="text-muted-foreground">
                    {t('taskWorkLogs.moreNotShown', { count: totalElements - workLogs.length })}
                  </span>
                )}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('taskWorkLogs.addTitle')}</DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="task-wl-date">{t('taskWorkLogs.date')} *</Label>
                <LocalizedDateInput id="task-wl-date" value={date} onChange={setDate} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="task-wl-hours">{t('taskWorkLogs.hours')} *</Label>
                <Input
                  id="task-wl-hours"
                  type="number"
                  value={hours}
                  onChange={(e) => setHours(e.target.value === '' ? '' : parseFloat(e.target.value))}
                  min={0.25}
                  step={0.25}
                  required
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="task-wl-note">{t('taskWorkLogs.note')}</Label>
              <Textarea
                id="task-wl-note"
                value={note}
                onChange={(e) => setNote(e.target.value)}
                rows={2}
                placeholder={t('taskWorkLogs.notePlaceholder')}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)} disabled={submitting}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleCreate} disabled={!hours || hours <= 0 || submitting}>
              {submitting ? t('taskWorkLogs.adding') : t('taskWorkLogs.add')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
