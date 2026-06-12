import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2, ChevronDown, ChevronRight } from 'lucide-react';
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

interface PersonGroup {
  personName: string;
  totalHours: number;
  logs: WorkLog[];
}

function groupByPerson(workLogs: WorkLog[]): PersonGroup[] {
  const map = new Map<string, PersonGroup>();
  for (const wl of workLogs) {
    const name = wl.personName || '—';
    if (!map.has(name)) {
      map.set(name, { personName: name, totalHours: 0, logs: [] });
    }
    const g = map.get(name)!;
    g.totalHours += wl.hoursSpent || 0;
    g.logs.push(wl);
  }
  return Array.from(map.values()).sort((a, b) => b.totalHours - a.totalHours);
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
  const [expandedPersons, setExpandedPersons] = useState<Set<string>>(new Set());

  const togglePerson = (name: string) => {
    setExpandedPersons(prev => {
      const next = new Set(prev);
      next.has(name) ? next.delete(name) : next.add(name);
      return next;
    });
  };

  const groups = groupByPerson(workLogs);

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
          {groups.length === 0 ? (
            <p className="text-muted-foreground">{t('pitchDetailPage.noWorkLogs')}</p>
          ) : (
            <div className="space-y-0">
              {groups.map((group, gi) => {
                const isExpanded = expandedPersons.has(group.personName);
                return (
                  <div key={group.personName}>
                    {/* Person summary row */}
                    <button
                      className="w-full flex items-center justify-between py-3 text-left hover:bg-muted/40 rounded px-1 -mx-1 transition-colors"
                      onClick={() => togglePerson(group.personName)}
                    >
                      <div className="flex items-center gap-2">
                        {isExpanded
                          ? <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                          : <ChevronRight className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                        }
                        <span className="font-medium text-sm">{group.personName}</span>
                        <span className="text-xs text-muted-foreground">
                          {group.logs.length} {group.logs.length === 1 ? 'entry' : 'entries'}
                        </span>
                      </div>
                      <Badge variant="secondary" className="ml-2 shrink-0">
                        {group.totalHours % 1 === 0 ? group.totalHours : group.totalHours.toFixed(2)}h
                      </Badge>
                    </button>

                    {/* Expanded individual entries */}
                    {isExpanded && (
                      <div className="ml-5 border-l border-border pl-3 pb-1 space-y-0">
                        {group.logs.map((wl, wi) => (
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
                                  onClick={() => onDeleteWorkLog(wl.id)}
                                >
                                  <Trash2 className="h-3.5 w-3.5" />
                                </Button>
                              </div>
                            </div>
                            {wi < group.logs.length - 1 && (
                              <div className="border-b border-border/50" />
                            )}
                          </div>
                        ))}
                      </div>
                    )}

                    {gi < groups.length - 1 && (
                      <div className="border-b border-border" />
                    )}
                  </div>
                );
              })}

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
