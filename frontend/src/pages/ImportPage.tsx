import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  UploadCloud,
  Loader2,
  CheckCircle2,
  XCircle,
  ChevronDown,
  ChevronUp,
  Unplug,
  Plug,
} from 'lucide-react';
import * as CollapsiblePrimitive from '@radix-ui/react-collapsible';
import * as SelectPrimitive from '@radix-ui/react-select';
import { cn } from '@/lib/utils';
import { importService } from '../services/importService';
import { ImportJobDTO, LinearConnectionStatus, LinearTeam } from '../types';
import { useToast } from '../contexts';

type Step = 1 | 2 | 3;
type TabSource = 'csv' | 'linear';

const FORMAT_OPTIONS = [
  { value: 'auto', labelKey: 'importPage.formatAuto' },
  { value: 'jira', labelKey: 'importPage.formatJira' },
  { value: 'linear', labelKey: 'importPage.formatLinear' },
  { value: 'asana', labelKey: 'importPage.formatAsana' },
  { value: 'generic', labelKey: 'importPage.formatGeneric' },
] as const;

function StepIndicator({ step, current }: { step: number; current: number }) {
  const { t } = useTranslation();
  const stepKeys = ['importPage.step1', 'importPage.step2', 'importPage.step3'] as const;
  const done = current > step;
  const active = current === step;

  return (
    <div className="flex flex-col items-center gap-1">
      <div
        className={cn(
          'flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold transition-colors',
          done && 'bg-green-500 text-white',
          active && 'bg-primary text-primary-foreground',
          !done && !active && 'bg-muted text-muted-foreground'
        )}
      >
        {done ? <CheckCircle2 className="h-5 w-5" /> : step}
      </div>
      <span
        className={cn(
          'text-xs font-medium',
          active ? 'text-foreground' : 'text-muted-foreground'
        )}
      >
        {t(stepKeys[step - 1])}
      </span>
    </div>
  );
}

function StepDivider({ done }: { done: boolean }) {
  return (
    <div
      className={cn(
        'mb-6 h-0.5 flex-1 transition-colors',
        done ? 'bg-green-500' : 'bg-muted'
      )}
    />
  );
}

// ── Result Card (shared between CSV step 3 and Linear import result) ─────────
function ImportResultCard({
  result,
  onReset,
  resetLabel,
}: {
  result: ImportJobDTO;
  onReset: () => void;
  resetLabel: string;
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [errorsOpen, setErrorsOpen] = useState(false);

  return (
    <div className="space-y-6">
      <div className="flex flex-col items-center gap-3 py-4 text-center">
        {result.status === 'COMPLETED' ? (
          <CheckCircle2 className="h-14 w-14 text-green-500" />
        ) : (
          <XCircle className="h-14 w-14 text-destructive" />
        )}
        <h2 className="text-xl font-bold text-foreground">
          {result.status === 'COMPLETED'
            ? t('importPage.importComplete')
            : t('importPage.importFailed')}
        </h2>
        {result.status === 'COMPLETED' && (
          <p className="text-sm text-muted-foreground">{t('importPage.projectCreated')}</p>
        )}
      </div>

      <div className="grid grid-cols-3 gap-4">
        {[
          { labelKey: 'importPage.totalRows', value: result.totalRows },
          { labelKey: 'importPage.imported', value: result.importedRows },
          { labelKey: 'importPage.failed', value: result.failedRows },
        ].map(({ labelKey, value }) => (
          <div
            key={labelKey}
            className="flex flex-col items-center gap-1 rounded-lg border border-border bg-card p-4"
          >
            <span className="text-2xl font-bold text-foreground">{value}</span>
            <span className="text-xs text-muted-foreground">{t(labelKey)}</span>
          </div>
        ))}
      </div>

      {result.failedRows > 0 && result.errorLog && (
        <CollapsiblePrimitive.Root open={errorsOpen} onOpenChange={setErrorsOpen}>
          <CollapsiblePrimitive.Trigger className="flex w-full items-center justify-between rounded-md px-1 py-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
            <span>{t('importPage.errorDetails')}</span>
            {errorsOpen ? (
              <ChevronUp className="h-4 w-4" />
            ) : (
              <ChevronDown className="h-4 w-4" />
            )}
          </CollapsiblePrimitive.Trigger>
          <CollapsiblePrimitive.Content>
            <pre className="mt-2 max-h-48 overflow-auto rounded-md bg-muted p-3 text-xs text-muted-foreground whitespace-pre-wrap break-words">
              {result.errorLog}
            </pre>
          </CollapsiblePrimitive.Content>
        </CollapsiblePrimitive.Root>
      )}

      <div className="flex flex-col gap-3 sm:flex-row">
        <button
          type="button"
          onClick={() => navigate('/projects')}
          className="flex-1 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
        >
          {t('importPage.openProject')}
        </button>
        <button
          type="button"
          onClick={onReset}
          className="flex-1 rounded-md border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent transition-colors"
        >
          {resetLabel}
        </button>
      </div>
    </div>
  );
}

// ── Linear Tab ────────────────────────────────────────────────────────────────
function LinearTab() {
  const { t } = useTranslation();
  const { showToast } = useToast();

  const [status, setStatus] = useState<LinearConnectionStatus | null>(null);
  const [statusLoading, setStatusLoading] = useState(true);

  const [teams, setTeams] = useState<LinearTeam[]>([]);
  const [teamsLoading, setTeamsLoading] = useState(false);

  const [selectedTeam, setSelectedTeam] = useState<LinearTeam | null>(null);
  const [teamSaving, setTeamSaving] = useState(false);

  const [linearProjectName, setLinearProjectName] = useState('');
  const [linearProjectType, setLinearProjectType] = useState<'KANBAN' | 'SCRUM'>('KANBAN');
  const [linearProjectNameError, setLinearProjectNameError] = useState('');

  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<ImportJobDTO | null>(null);

  const [disconnecting, setDisconnecting] = useState(false);
  const [authorizing, setAuthorizing] = useState(false);

  // Load status on mount
  useEffect(() => {
    let cancelled = false;
    setStatusLoading(true);
    importService
      .getLinearStatus()
      .then((s) => {
        if (!cancelled) setStatus(s);
      })
      .catch(() => {
        if (!cancelled) setStatus({ connected: false, configured: false, teamId: null, teamName: null });
      })
      .finally(() => {
        if (!cancelled) setStatusLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // When connected and a team is already saved in status, pre-populate selectedTeam
  useEffect(() => {
    if (status?.connected && status.teamId && status.teamName && !selectedTeam) {
      setSelectedTeam({ id: status.teamId, name: status.teamName, key: '' });
    }
  }, [status, selectedTeam]);

  // Load teams when connected and no team selected yet
  function fetchTeams() {
    setTeamsLoading(true);
    importService
      .getLinearTeams()
      .then(setTeams)
      .catch(() => setTeams([]))
      .finally(() => setTeamsLoading(false));
  }

  async function handleConnect() {
    setAuthorizing(true);
    try {
      const { authorizationUrl } = await importService.authorizeLinear(window.location.origin);
      window.location.href = authorizationUrl;
    } catch {
      showToast(t('importPage.linearErrorToast', { error: t('common.retry') }), 'error');
      setAuthorizing(false);
    }
  }

  async function handleDisconnect() {
    setDisconnecting(true);
    try {
      await importService.disconnectLinear();
      setStatus({ connected: false, configured: status?.configured ?? false, teamId: null, teamName: null });
      setSelectedTeam(null);
      setImportResult(null);
      showToast(t('importPage.linearDisconnectedToast'), 'success');
    } catch {
      showToast(t('importPage.linearErrorToast', { error: t('common.retry') }), 'error');
    } finally {
      setDisconnecting(false);
    }
  }

  async function handleSelectTeam(team: LinearTeam) {
    setTeamSaving(true);
    try {
      await importService.saveLinearTeam(team.id, team.name);
      setSelectedTeam(team);
      setStatus((prev) =>
        prev ? { ...prev, teamId: team.id, teamName: team.name } : prev
      );
    } catch {
      showToast(t('importPage.linearErrorToast', { error: t('common.retry') }), 'error');
    } finally {
      setTeamSaving(false);
    }
  }

  async function handleImport() {
    if (!linearProjectName.trim()) {
      setLinearProjectNameError(t('importPage.projectNameRequired'));
      return;
    }
    if (!selectedTeam) return;
    setLinearProjectNameError('');
    setImporting(true);
    try {
      const job = await importService.importFromLinear(
        selectedTeam.id,
        linearProjectName.trim(),
        linearProjectType
      );
      setImportResult(job);
    } catch {
      showToast(t('importPage.importError'), 'error');
    } finally {
      setImporting(false);
    }
  }

  function handleResetImport() {
    setImportResult(null);
    setLinearProjectName('');
    setLinearProjectType('KANBAN');
    setLinearProjectNameError('');
  }

  if (statusLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  // State A: not configured
  if (!status?.configured) {
    return (
      <div className="rounded-lg border border-border bg-muted/40 p-6">
        <p className="text-sm text-muted-foreground">{t('importPage.linearNotConfigured')}</p>
      </div>
    );
  }

  // State B: configured but not connected
  if (!status.connected) {
    return (
      <div className="flex flex-col items-center gap-6 py-10 text-center">
        <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted">
          <Plug className="h-8 w-8 text-muted-foreground" />
        </div>
        <div>
          <h3 className="text-base font-semibold text-foreground">{t('importPage.tabLinear')}</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            {t('importPage.linearSelectTeam')}
          </p>
        </div>
        <button
          type="button"
          onClick={handleConnect}
          disabled={authorizing}
          className="flex items-center gap-2 rounded-md bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60 transition-colors"
        >
          {authorizing && <Loader2 className="h-4 w-4 animate-spin" />}
          {t('importPage.linearConnectBtn')}
        </button>
      </div>
    );
  }

  // State C: connected — show import result if done
  if (importResult) {
    return (
      <ImportResultCard
        result={importResult}
        onReset={handleResetImport}
        resetLabel={t('importPage.importAnother')}
      />
    );
  }

  // State C: connected — show team picker or import form
  return (
    <div className="space-y-6">
      {/* Connected badge + disconnect */}
      <div className="flex items-center justify-between rounded-lg border border-green-200 bg-green-50 px-4 py-3 dark:border-green-900 dark:bg-green-950/30">
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1 rounded-full bg-green-500 px-2.5 py-0.5 text-xs font-semibold text-white">
            <CheckCircle2 className="h-3 w-3" />
            {t('importPage.linearConnectedBadge')}
          </span>
          {status.teamName && (
            <span className="text-sm text-muted-foreground">— {status.teamName}</span>
          )}
        </div>
        <button
          type="button"
          onClick={handleDisconnect}
          disabled={disconnecting}
          className="flex items-center gap-1.5 rounded-md border border-border px-3 py-1.5 text-xs font-medium text-foreground hover:bg-accent disabled:opacity-60 transition-colors"
        >
          {disconnecting ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : (
            <Unplug className="h-3.5 w-3.5" />
          )}
          {t('importPage.linearDisconnectBtn')}
        </button>
      </div>

      {/* Team picker */}
      {!selectedTeam && (
        <div className="space-y-3">
          <p className="text-sm font-medium text-foreground">{t('importPage.linearSelectTeam')}</p>
          {!teams.length && !teamsLoading && (
            <button
              type="button"
              onClick={fetchTeams}
              className="rounded-md border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-accent transition-colors"
            >
              {t('importPage.linearTeamPickerPlaceholder')}
            </button>
          )}
          {teamsLoading && (
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              {t('common.loading')}
            </div>
          )}
          {teams.length > 0 && (
            <div className="space-y-2">
              {teams.map((team) => (
                <button
                  key={team.id}
                  type="button"
                  onClick={() => handleSelectTeam(team)}
                  disabled={teamSaving}
                  className="flex w-full items-center justify-between rounded-lg border border-border px-4 py-3 text-sm font-medium text-foreground hover:border-primary hover:bg-accent disabled:cursor-not-allowed disabled:opacity-60 transition-colors"
                >
                  <span>{team.name}</span>
                  <span className="text-xs text-muted-foreground">{team.key}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Import form — once team is selected */}
      {selectedTeam && (
        <div className="space-y-5">
          {/* Change team link */}
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">
              {selectedTeam.name}
            </span>
            <button
              type="button"
              onClick={() => {
                setSelectedTeam(null);
                setTeams([]);
              }}
              className="text-xs font-medium text-primary underline-offset-2 hover:underline"
            >
              {t('importPage.linearChangeTeam')}
            </button>
          </div>

          {/* Project name */}
          <div className="space-y-1">
            <label
              htmlFor="linearProjectName"
              className="block text-sm font-medium text-foreground"
            >
              {t('importPage.linearProjectName')}
            </label>
            <input
              id="linearProjectName"
              type="text"
              value={linearProjectName}
              onChange={(e) => {
                setLinearProjectName(e.target.value);
                if (e.target.value.trim()) setLinearProjectNameError('');
              }}
              placeholder={t('importPage.linearProjectNamePlaceholder')}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
            />
            {linearProjectNameError && (
              <p className="text-xs text-destructive">{linearProjectNameError}</p>
            )}
          </div>

          {/* Project type radio */}
          <div className="space-y-2">
            <span className="block text-sm font-medium text-foreground">
              {t('importPage.linearProjectType')}
            </span>
            <div className="flex gap-6">
              {(
                [
                  { value: 'KANBAN', labelKey: 'importPage.linearProjectTypeKanban' },
                  { value: 'SCRUM', labelKey: 'importPage.linearProjectTypeScrum' },
                ] as const
              ).map(({ value, labelKey }) => (
                <label
                  key={value}
                  className="flex cursor-pointer items-center gap-2 text-sm text-foreground"
                >
                  <input
                    type="radio"
                    name="linearProjectType"
                    value={value}
                    checked={linearProjectType === value}
                    onChange={() => setLinearProjectType(value)}
                    className="accent-primary"
                  />
                  {t(labelKey)}
                </label>
              ))}
            </div>
          </div>

          {/* Import button */}
          <button
            type="button"
            onClick={handleImport}
            disabled={importing || !linearProjectName.trim()}
            className={cn(
              'flex w-full items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-semibold transition-colors',
              importing || !linearProjectName.trim()
                ? 'cursor-not-allowed bg-muted text-muted-foreground'
                : 'bg-primary text-primary-foreground hover:bg-primary/90'
            )}
          >
            {importing && <Loader2 className="h-4 w-4 animate-spin" />}
            {importing ? t('importPage.linearImporting') : t('importPage.linearImportBtn')}
          </button>
        </div>
      )}
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function ImportPage() {
  const { t } = useTranslation();
  const { showToast } = useToast();

  // ── Tab state — driven by ?tab= URL param ────────────────────────────────
  const [activeTab, setActiveTab] = useState<TabSource>(() => {
    const params = new URLSearchParams(window.location.search);
    return params.get('tab') === 'linear' ? 'linear' : 'csv';
  });

  // Handle ?linear_connected / ?linear_error URL params on mount
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const linearConnected = params.get('linear_connected');
    const linearError = params.get('linear_error');
    if (linearConnected === 'true') {
      showToast(t('importPage.linearConnectedToast'), 'success');
    }
    if (linearError) {
      showToast(t('importPage.linearErrorToast', { error: decodeURIComponent(linearError) }), 'error');
    }
    if (linearConnected || linearError) {
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, [showToast, t]);

  // ── CSV state ────────────────────────────────────────────────────────────
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [step, setStep] = useState<Step>(1);
  const [file, setFile] = useState<File | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [projectName, setProjectName] = useState('');
  const [format, setFormat] = useState('auto');
  const [projectNameError, setProjectNameError] = useState('');
  const [fileError, setFileError] = useState('');
  const [result, setResult] = useState<ImportJobDTO | null>(null);
  const [formatsOpen, setFormatsOpen] = useState(false);

  // ── Drag & Drop ──────────────────────────────────────────────────────────
  function handleDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragActive(false);
    const dropped = e.dataTransfer.files[0];
    if (dropped) validateAndSetFile(dropped);
  }

  function handleDragOver(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setDragActive(true);
  }

  function handleDragLeave() {
    setDragActive(false);
  }

  function handleFileInputChange(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = e.target.files?.[0];
    if (selected) validateAndSetFile(selected);
  }

  function validateAndSetFile(f: File) {
    if (!f.name.endsWith('.csv')) {
      setFileError(t('importPage.wrongFormat'));
      setFile(null);
    } else {
      setFileError('');
      setFile(f);
    }
  }

  // ── Submit CSV ───────────────────────────────────────────────────────────
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    let valid = true;
    if (!file) {
      setFileError(t('importPage.noFile'));
      valid = false;
    }
    if (!projectName.trim()) {
      setProjectNameError(t('importPage.projectNameRequired'));
      valid = false;
    }
    if (!valid) return;

    setProjectNameError('');
    setFileError('');
    setStep(2);

    try {
      const job = await importService.importCsv(file!, projectName.trim(), format);
      setResult(job);
      setStep(3);
    } catch {
      showToast(t('importPage.importError'), 'error');
      setStep(1);
    }
  }

  // ── Reset CSV ────────────────────────────────────────────────────────────
  function handleReset() {
    setFile(null);
    setProjectName('');
    setFormat('auto');
    setProjectNameError('');
    setFileError('');
    setResult(null);
    setFormatsOpen(false);
    setStep(1);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  const canSubmit = !!file && !!projectName.trim();

  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold tracking-tight text-foreground">
          {t('importPage.title')}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">{t('importPage.subtitle')}</p>
      </div>

      {/* Source tabs */}
      <div className="mb-8 flex gap-1 rounded-lg border border-border bg-muted/30 p-1">
        {(
          [
            { id: 'csv' as const, labelKey: 'importPage.tabCsv' },
            { id: 'linear' as const, labelKey: 'importPage.tabLinear' },
          ] as const
        ).map(({ id, labelKey }) => (
          <button
            key={id}
            type="button"
            onClick={() => setActiveTab(id)}
            className={cn(
              'flex-1 rounded-md px-4 py-2 text-sm font-medium transition-colors',
              activeTab === id
                ? 'bg-background text-foreground shadow-sm'
                : 'text-muted-foreground hover:text-foreground'
            )}
          >
            {t(labelKey)}
          </button>
        ))}
      </div>

      {/* ── CSV tab ──────────────────────────────────────────────────────── */}
      {activeTab === 'csv' && (
        <>
          {/* Stepper */}
          <div className="mb-10 flex items-center gap-2">
            <StepIndicator step={1} current={step} />
            <StepDivider done={step > 1} />
            <StepIndicator step={2} current={step} />
            <StepDivider done={step > 2} />
            <StepIndicator step={3} current={step} />
          </div>

          {/* ── Step 1: Upload ──────────────────────────────────────────── */}
          {step === 1 && (
            <form onSubmit={handleSubmit} className="space-y-6" noValidate>
              {/* Drop Zone */}
              <div>
                <input
                  type="file"
                  accept=".csv"
                  className="hidden"
                  ref={fileInputRef}
                  onChange={handleFileInputChange}
                />
                <div
                  role="button"
                  tabIndex={0}
                  onClick={() => fileInputRef.current?.click()}
                  onKeyDown={(e) => e.key === 'Enter' && fileInputRef.current?.click()}
                  onDrop={handleDrop}
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                  className={cn(
                    'flex cursor-pointer flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed px-6 py-10 transition-colors',
                    dragActive
                      ? 'border-primary bg-primary/5'
                      : 'border-border hover:border-primary/60 hover:bg-accent/40'
                  )}
                >
                  <UploadCloud
                    className={cn(
                      'h-10 w-10 transition-colors',
                      dragActive ? 'text-primary' : 'text-muted-foreground'
                    )}
                  />
                  {file ? (
                    <p className="text-sm font-medium text-foreground">
                      {t('importPage.selectedFile', { name: file.name })}
                    </p>
                  ) : (
                    <p className="text-sm text-muted-foreground">
                      {dragActive ? t('importPage.dropZoneActive') : t('importPage.dropZone')}
                    </p>
                  )}
                </div>
                {fileError && (
                  <p className="mt-1 text-xs text-destructive">{fileError}</p>
                )}
              </div>

              {/* Project Name */}
              <div className="space-y-1">
                <label
                  htmlFor="projectName"
                  className="block text-sm font-medium text-foreground"
                >
                  {t('importPage.projectName')}
                </label>
                <input
                  id="projectName"
                  type="text"
                  value={projectName}
                  onChange={(e) => {
                    setProjectName(e.target.value);
                    if (e.target.value.trim()) setProjectNameError('');
                  }}
                  placeholder={t('importPage.projectNamePlaceholder')}
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
                />
                {projectNameError && (
                  <p className="text-xs text-destructive">{projectNameError}</p>
                )}
              </div>

              {/* Format Select — Radix UI Select primitive */}
              <div className="space-y-1">
                <label className="block text-sm font-medium text-foreground">
                  {t('importPage.format')}
                </label>
                <SelectPrimitive.Root value={format} onValueChange={setFormat}>
                  <SelectPrimitive.Trigger className="flex w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring">
                    <SelectPrimitive.Value />
                    <SelectPrimitive.Icon>
                      <ChevronDown className="h-4 w-4 text-muted-foreground" />
                    </SelectPrimitive.Icon>
                  </SelectPrimitive.Trigger>
                  <SelectPrimitive.Portal>
                    <SelectPrimitive.Content
                      position="popper"
                      className="z-50 w-full min-w-[var(--radix-select-trigger-width)] overflow-hidden rounded-md border border-border bg-popover shadow-md"
                    >
                      <SelectPrimitive.Viewport className="p-1">
                        {FORMAT_OPTIONS.map((opt) => (
                          <SelectPrimitive.Item
                            key={opt.value}
                            value={opt.value}
                            className="flex cursor-pointer select-none items-center rounded-sm px-3 py-2 text-sm outline-none data-[highlighted]:bg-accent data-[highlighted]:text-accent-foreground"
                          >
                            <SelectPrimitive.ItemText>
                              {t(opt.labelKey)}
                            </SelectPrimitive.ItemText>
                          </SelectPrimitive.Item>
                        ))}
                      </SelectPrimitive.Viewport>
                    </SelectPrimitive.Content>
                  </SelectPrimitive.Portal>
                </SelectPrimitive.Root>
              </div>

              {/* Submit */}
              <button
                type="submit"
                disabled={!canSubmit}
                className={cn(
                  'w-full rounded-md px-4 py-2 text-sm font-semibold transition-colors',
                  canSubmit
                    ? 'bg-primary text-primary-foreground hover:bg-primary/90'
                    : 'cursor-not-allowed bg-muted text-muted-foreground'
                )}
              >
                {t('importPage.startImport')}
              </button>

              {/* Supported Formats collapsible */}
              <CollapsiblePrimitive.Root open={formatsOpen} onOpenChange={setFormatsOpen}>
                <CollapsiblePrimitive.Trigger className="flex w-full items-center justify-between rounded-md px-1 py-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors">
                  <span>{t('importPage.supportedFormats')}</span>
                  {formatsOpen ? (
                    <ChevronUp className="h-4 w-4" />
                  ) : (
                    <ChevronDown className="h-4 w-4" />
                  )}
                </CollapsiblePrimitive.Trigger>
                <CollapsiblePrimitive.Content>
                  <div className="mt-2 overflow-hidden rounded-lg border border-border">
                    <table className="w-full text-sm">
                      <thead className="bg-muted/50">
                        <tr>
                          <th className="px-3 py-2 text-start text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                            {t('importPage.formatTable.format')}
                          </th>
                          <th className="px-3 py-2 text-start text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                            {t('importPage.formatTable.keyColumns')}
                          </th>
                          <th className="px-3 py-2 text-start text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                            {t('importPage.formatTable.tools')}
                          </th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-border">
                        <tr>
                          <td className="px-3 py-2 font-medium">{t('importPage.formatJira')}</td>
                          <td className="px-3 py-2 text-muted-foreground">{t('importPage.jiraColumns')}</td>
                          <td className="px-3 py-2 text-muted-foreground">{t('importPage.jiraTools')}</td>
                        </tr>
                        <tr>
                          <td className="px-3 py-2 font-medium">{t('importPage.formatLinear')}</td>
                          <td className="px-3 py-2 text-muted-foreground">{t('importPage.linearColumns')}</td>
                          <td className="px-3 py-2 text-muted-foreground">{t('importPage.linearTools')}</td>
                        </tr>
                        <tr>
                          <td className="px-3 py-2 font-medium">{t('importPage.formatAsana')}</td>
                          <td className="px-3 py-2 text-muted-foreground">{t('importPage.asanaColumns')}</td>
                          <td className="px-3 py-2 text-muted-foreground">{t('importPage.asanaTools')}</td>
                        </tr>
                        <tr>
                          <td className="px-3 py-2 font-medium">{t('importPage.formatGeneric')}</td>
                          <td className="px-3 py-2 text-muted-foreground">{t('importPage.genericColumns')}</td>
                          <td className="px-3 py-2 text-muted-foreground">{t('importPage.genericTools')}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </CollapsiblePrimitive.Content>
              </CollapsiblePrimitive.Root>
            </form>
          )}

          {/* ── Step 2: Importing ────────────────────────────────────────── */}
          {step === 2 && (
            <div className="flex flex-col items-center gap-4 py-12 text-center">
              <Loader2 className="h-12 w-12 animate-spin text-primary" />
              <p className="text-lg font-semibold text-foreground">
                {t('importPage.importingFile', { fileName: file?.name ?? '' })}
              </p>
              <p className="text-sm text-muted-foreground">
                {t('importPage.importingDesc')}
              </p>
            </div>
          )}

          {/* ── Step 3: Done ─────────────────────────────────────────────── */}
          {step === 3 && result && (
            <ImportResultCard
              result={result}
              onReset={handleReset}
              resetLabel={t('importPage.importAnother')}
            />
          )}
        </>
      )}

      {/* ── Linear tab ───────────────────────────────────────────────────── */}
      {activeTab === 'linear' && <LinearTab />}
    </div>
  );
}
