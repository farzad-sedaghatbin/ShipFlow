import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  UploadCloud,
  Loader2,
  CheckCircle2,
  XCircle,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import * as CollapsiblePrimitive from '@radix-ui/react-collapsible';
import * as SelectPrimitive from '@radix-ui/react-select';
import { cn } from '@/lib/utils';
import { importService } from '../services/importService';
import { ImportJobDTO } from '../types';
import { useToast } from '../contexts';

type Step = 1 | 2 | 3;

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

export default function ImportPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showToast } = useToast();

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
  const [errorsOpen, setErrorsOpen] = useState(false);

  // ── Drag & Drop ───────────────────────────────────────────────────────────
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

  // ── Submit ────────────────────────────────────────────────────────────────
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

  // ── Reset ─────────────────────────────────────────────────────────────────
  function handleReset() {
    setFile(null);
    setProjectName('');
    setFormat('auto');
    setProjectNameError('');
    setFileError('');
    setResult(null);
    setErrorsOpen(false);
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

      {/* Stepper */}
      <div className="mb-10 flex items-center gap-2">
        <StepIndicator step={1} current={step} />
        <StepDivider done={step > 1} />
        <StepIndicator step={2} current={step} />
        <StepDivider done={step > 2} />
        <StepIndicator step={3} current={step} />
      </div>

      {/* ── Step 1: Upload ─────────────────────────────────────────────────── */}
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

      {/* ── Step 2: Importing ──────────────────────────────────────────────── */}
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

      {/* ── Step 3: Done ──────────────────────────────────────────────────── */}
      {step === 3 && result && (
        <div className="space-y-6">
          {/* Status icon + title */}
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
              <p className="text-sm text-muted-foreground">
                {t('importPage.projectCreated')}
              </p>
            )}
          </div>

          {/* Stats */}
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

          {/* Error details collapsible */}
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

          {/* Actions */}
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
              onClick={handleReset}
              className="flex-1 rounded-md border border-border px-4 py-2 text-sm font-semibold text-foreground hover:bg-accent transition-colors"
            >
              {t('importPage.importAnother')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
