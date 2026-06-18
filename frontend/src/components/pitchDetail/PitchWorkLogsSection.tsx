import { useState, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2, ChevronDown, ChevronRight, Loader2 } from 'lucide-react';
import { formatLocalizedDate } from '../../utils/dateLocalization';
import { LocalizedDateInput } from '../LocalizedDateInput';
import { WorkLog, WorkLogPersonSummary, CreateWorkLogForSelfRequest } from '../../types';
import { workLogService } from '../../services/workLogService';
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

interface PersonEntriesState {
  loading: boolean;
  entries: WorkLog[];
}

interface PitchWorkLogsSectionProps {
  pitchId: number;
  personSummaries: WorkLogPersonSummary[];
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
  pitchId,
  personSummaries,
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
  const [expandedPersons, setExpandedPersons] = useState<Set<number>>(new Set());
  const [personEntries, setPersonEntries] = useState<Map<number, PersonEntriesState>>(new Map());

  const loadPersonEntries = useCallback(async (personId: number) => {
    setPersonEntries(prev => new Map(prev).set(personId, { loading: true, entries: [] }));
    try {
      const res = await workLogService.getByPitchIdAndPersonId(pitchId, personId, 0, 100);
      setPersonEntries(prev => new Map(prev).set(personId, { loading: false, entries: res.data.content }));
    } catch {
      setPersonEntries(prev => new Map(prev).set(personId, { loading: false, entries: [] }));
    }
  }, [pitchId]);

  const togglePerson = useCallback((personId: number) => {
    setExpandedPersons(prev => {
      const next = new Set(prev);
      if (next.has(personId)) {
        next.delete(personId);
      } else {
        next.add(personId);
        if (!personEntries.has(personId)) {
          loadPersonEntries(personId);
        }
      }
      return next;
    });
  }, [personEntries, loadPersonEntries]);

  const handleDelete = useCallback((workLogId: number, personId: number) => {
    // Optimistically remove from local entries before parent refreshes summaries
    setPersonEntries(prev => {
      const state = prev.get(personId);
      if (!state) return prev;
      return new Map(prev).set(personId, {
        ...state,
        entries: state.entries.filter(e => e.id !== workLogId),
      });
    });
    onDeleteWorkLog(workLogId);
  }, [onDeleteWorkLog]);

  const totalEntryCount = personSummaries.reduce((sum, s) => sum + s.entryCount, 0);

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
          {personSummaries.length === 0 ? (
            <p className="text-muted-foreground">{t('pitchDetailPage.noWorkLogs')}</p>
          ) : (
            <div className="space-y-0">
              {personSummaries.map((summary, gi) => {
                const isExpanded = expandedPersons.has(summary.personId);
                const entriesState = personEntries.get(summary.personId);
                return (
                  <div key={summary.personId}>
                    {/* Person summary row */}
                    <button
                      className="w-full flex items-center justify-between py-3 text-left hover:bg-muted/40 rounded px-1 -mx-1 transition-colors"
                      onClick={() => togglePerson(summary.personId)}
                    >
                      <div className="flex items-center gap-2">
                        {isExpanded
                          ? <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                          : <ChevronRight className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                        }
                        <span className="font-medium text-sm">{summary.personName}</span>
                        <span className="text-xs text-muted-foreground">
                          {summary.entryCount} {summary.entryCount === 1 ? 'entry' : 'entries'}
                        </span>
                      </div>
                      <Badge variant="secondary" className="ml-2 shrink-0">
                        {summary.totalHours % 1 === 0 ? summary.totalHours : summary.totalHours.toFixed(2)}h
                      </Badge>
                    </button>

                    {/* Expanded individual entries */}
                    {isExpanded && (
                      <div className="ml-5 border-l border-border pl-3 pb-1 space-y-0">
                        {entriesState?.loading ? (
                          <div className="flex items-center gap-2 py-2 text-xs text-muted-foreground">
                            <Loader2 className="h-3.5 w-3.5 animate-spin" />
                            Loading entries…
                          </div>
                        ) : (entriesState?.entries ?? []).map((wl, wi) => (
                          <div key={wl.id}>
                            <div className="flex items-start justify-between py-2">
                              <div className="flex-1 pr-2">
                                <p className="text-xs text-muted-foreground">
                                  {formatLocalizedDate(new Date(wl.date), language)}
                                  {wl.note && (
                                    <span className="ml-1">• {wl.note}</span>
                                  )}
                                </p>
                              </div>
                              <div className="flex items-center gap-1 shrink-0">
                                <span className="text-xs font-medium text-muted-foreground">{wl.hoursSpent}h</span>
                                <Button
                                  variant="ghost"
                                  size="icon-sm"
                                  onClick={() => handleDelete(wl.id, summary.personId)}
                                >
                                  <Trash2 className="h-3.5 w-3.5" />
                                </Button>
                              </div>
                            </div>
                            {wi < (entriesState?.entries.length ?? 0) - 1 && (
                              <div className="border-b border-border/50" />
                            )}
                          </div>
                        ))}
                      </div>
                    )}

                    {gi < personSummaries.length - 1 && (
                      <div className="border-b border-border" />
                    )}
                  </div>
                );
              })}

              {totalEntryCount > 10 && (
                <div className="pt-3 text-center">
                  <Link to="/time/logs" className="text-sm text-primary hover:underline">
                    {t('pitchDetailPage.viewAllWorkLogs', { total: totalEntryCount, defaultValue: 'View all {{total}} work logs →' })}
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
