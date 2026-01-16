import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { LocalizedDateInput } from '../components/LocalizedDateInput';
import { Plus, Trash2, ClipboardList, Loader2 } from 'lucide-react';
import dayjs from 'dayjs';
import { workLogService } from '../services/workLogService';
import { pitchService } from '../services/pitchService';
import { personService } from '../services/personService';
import { cycleService } from '../services/cycleService';
import { WorkLog, Pitch, Person, Cycle, CreateWorkLogRequest } from '../types';
import EmptyState from '../components/EmptyState';

import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { useToast } from '../contexts';

export default function WorkLogForm() {
  const { t, i18n } = useTranslation();
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [selectedCycle, setSelectedCycle] = useState<number | ''>('');
  const [loading, setLoading] = useState(true);
  const { showSuccess, showError } = useToast();

  const [newWorkLog, setNewWorkLog] = useState<CreateWorkLogRequest>({
    personId: 0,
    pitchId: 0,
    date: dayjs().format('YYYY-MM-DD'),
    hoursSpent: 0,
    note: '',
  });
  const [workLogDate, setWorkLogDate] = useState<string>(dayjs().format('YYYY-MM-DD'));

  useEffect(() => {
    const abortController = new AbortController();
    loadInitialData();
    return () => abortController.abort();
  }, []);

  useEffect(() => {
    const abortController = new AbortController();
    if (selectedCycle) {
      loadWorkLogs(selectedCycle);
      loadPitches(selectedCycle);
    }
    return () => abortController.abort();
  }, [selectedCycle]);

  const loadInitialData = async () => {
    try {
      const [cyclesRes, personsData] = await Promise.all([
        cycleService.getActive(),
        personService.getAll(true),
      ]);
      setCycles(cyclesRes.data);
      setPersons(personsData);
      if (cyclesRes.data.length > 0) {
        setSelectedCycle(cyclesRes.data[0].id);
      }
    } catch (error) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadWorkLogs = async (cycleId: number) => {
    try {
      const response = await workLogService.getByCycleId(cycleId);
      setWorkLogs(response.data);
    } catch (error) {
      console.error('Failed to load work logs:', error);
    }
  };

  const loadPitches = async (cycleId: number) => {
    try {
      const response = await pitchService.getByCycleId(cycleId);
      setPitches(response.data);
    } catch (error) {
      console.error('Failed to load pitches:', error);
    }
  };

  const handleCreateWorkLog = async () => {
    if (!workLogDate) return;
    try {
      await workLogService.create({
        ...newWorkLog,
        date: workLogDate,
      });
      setNewWorkLog({
        personId: 0,
        pitchId: 0,
        date: dayjs().format('YYYY-MM-DD'),
        hoursSpent: 0,
        note: '',
      });
      setWorkLogDate(dayjs().format('YYYY-MM-DD'));
      showSuccess(t('workLogForm.workLogAdded'));
      if (selectedCycle) {
        loadWorkLogs(selectedCycle as number);
      }
    } catch (error) {
      showError(t('workLogForm.workLogAddFailed'));
    }
  };

  const handleDeleteWorkLog = async (id: number) => {
    try {
      await workLogService.delete(id);
      showSuccess(t('workLogForm.workLogDeleted'));
      if (selectedCycle) {
        loadWorkLogs(selectedCycle as number);
      }
    } catch (error) {
      showError(t('workLogForm.workLogDeleteFailed'));
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  const totalHours = workLogs.reduce((sum, wl) => sum + wl.hoursSpent, 0);

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-bold tracking-tight">{t('workLogForm.title')}</h1>

      {/* Filter */}
      <Card>
        <CardContent className="pt-6">
          <div className="space-y-2">
            <Label htmlFor="cycle-select">{t('workLogForm.cycle')}</Label>
            <Select
              value={selectedCycle ? String(selectedCycle) : ''}
              onValueChange={(value) => setSelectedCycle(Number(value))}
            >
              <SelectTrigger id="cycle-select" className="w-[300px]">
                <SelectValue placeholder={t('workLogForm.selectCycle')} />
              </SelectTrigger>
              <SelectContent>
                {cycles.map((cycle) => (
                  <SelectItem key={cycle.id} value={String(cycle.id)}>
                    {cycle.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Add Work Log Form */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg font-semibold">{t('workLogForm.logDailyWork')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-6 lg:grid-cols-12 gap-4 items-end">
            <div className="sm:col-span-1 md:col-span-2 lg:col-span-2 space-y-2">
              <Label htmlFor="person-select">{t('workLogForm.person')} *</Label>
              <Select
                value={newWorkLog.personId ? String(newWorkLog.personId) : ''}
                onValueChange={(value) => setNewWorkLog({ ...newWorkLog, personId: Number(value) })}
              >
                <SelectTrigger id="person-select">
                  <SelectValue placeholder={t('workLogForm.selectPerson')} />
                </SelectTrigger>
                <SelectContent>
                  {persons.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="sm:col-span-1 md:col-span-2 lg:col-span-3 space-y-2">
              <Label htmlFor="pitch-select">{t('workLogForm.pitch')} *</Label>
              <Select
                value={newWorkLog.pitchId ? String(newWorkLog.pitchId) : ''}
                onValueChange={(value) => setNewWorkLog({ ...newWorkLog, pitchId: Number(value) })}
              >
                <SelectTrigger id="pitch-select">
                  <SelectValue placeholder={t('workLogForm.selectPitch')} />
                </SelectTrigger>
                <SelectContent>
                  {pitches.map((p) => (
                    <SelectItem key={p.id} value={String(p.id)}>
                      {p.title}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="sm:col-span-1 md:col-span-1 lg:col-span-2 space-y-2">
              <Label htmlFor="date-input">{t('workLogForm.date')}</Label>
              <LocalizedDateInput
                id="date-input"
                value={workLogDate}
                onChange={setWorkLogDate}
              />
            </div>

            <div className="sm:col-span-1 md:col-span-1 lg:col-span-1 space-y-2">
              <Label htmlFor="hours-input">{t('workLogForm.hours')} *</Label>
              <Input
                id="hours-input"
                type="number"
                min={0.25}
                step={0.25}
                value={newWorkLog.hoursSpent || ''}
                onChange={(e) => setNewWorkLog({ ...newWorkLog, hoursSpent: parseFloat(e.target.value) || 0 })}
                placeholder="0"
              />
            </div>

            <div className="sm:col-span-1 md:col-span-2 lg:col-span-3 space-y-2">
              <Label htmlFor="note-input">{t('workLogForm.note')}</Label>
              <Input
                id="note-input"
                value={newWorkLog.note}
                onChange={(e) => setNewWorkLog({ ...newWorkLog, note: e.target.value })}
                placeholder={t('workLogForm.optionalNote')}
              />
            </div>

            <div className="sm:col-span-2 md:col-span-1 lg:col-span-1">
              <Button
                onClick={handleCreateWorkLog}
                disabled={!newWorkLog.personId || !newWorkLog.pitchId || !newWorkLog.hoursSpent}
                className="w-full"
              >
                <Plus className="h-4 w-4 mr-2" />
                {t('workLogForm.add')}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Work Logs Table */}
      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <CardTitle className="text-lg font-semibold">{t('workLogForm.recentWorkLogs')}</CardTitle>
            <span className="text-muted-foreground">
              {t('workLogForm.total')}: <strong className="text-foreground">{totalHours.toFixed(1)} {t('workLogForm.hoursUnit')}</strong>
            </span>
          </div>
        </CardHeader>
        <CardContent>
          {workLogs.length === 0 ? (
            <EmptyState
              icon={ClipboardList}
              title={t('workLogForm.noWorkLogs')}
              description={t('workLogForm.noWorkLogsDesc')}
              size="small"
              compact
            />
          ) : (
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>{t('workLogForm.date')}</TableHead>
                    <TableHead>{t('workLogForm.teamMember')}</TableHead>
                    <TableHead>{t('workLogForm.pitch')}</TableHead>
                    <TableHead className="text-right">{t('workLogForm.hours')}</TableHead>
                    <TableHead>{t('workLogForm.note')}</TableHead>
                    <TableHead className="text-center">{t('workLogForm.actions')}</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {workLogs.map((wl) => (
                    <TableRow key={wl.id}>
                      <TableCell>{formatLocalizedDate(new Date(wl.date), i18n.language)}</TableCell>
                      <TableCell>{wl.personName}</TableCell>
                      <TableCell>{wl.pitchTitle}</TableCell>
                      <TableCell className="text-right">{wl.hoursSpent}h</TableCell>
                      <TableCell>{wl.note || '-'}</TableCell>
                      <TableCell className="text-center">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-destructive hover:text-destructive hover:bg-destructive/10"
                          onClick={() => handleDeleteWorkLog(wl.id)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
