import type { KeyboardEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { History, Pencil, Check, X, Layers } from 'lucide-react';
import { Pitch, PitchStatus, Epic } from '../../types';
import StatusChip from '../StatusChip';
import { SoftDeleteButton } from '../SoftDeleteButton';
import { Button } from '../ui/button';
import { Markdown } from '../ui/markdown';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import { Badge } from '../ui/badge';

interface PitchHeaderProps {
  pitch: Pitch;
  epics?: Epic[];
  onStatusChange: (newStatus: PitchStatus) => void;
  onHistoryOpen: () => void;
  onTitleSave?: (newTitle: string) => Promise<void>;
  onEpicChange?: (epicId: number | null) => Promise<void>;
}

export function PitchHeader({ pitch, epics, onStatusChange, onHistoryOpen, onTitleSave, onEpicChange }: PitchHeaderProps) {
  const { t } = useTranslation();
  const [editingTitle, setEditingTitle] = useState(false);
  const [titleDraft, setTitleDraft] = useState(pitch.title);
  const [savingTitle, setSavingTitle] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setTitleDraft(pitch.title);
  }, [pitch.title]);

  useEffect(() => {
    if (editingTitle) {
      inputRef.current?.focus();
      inputRef.current?.select();
    }
  }, [editingTitle]);

  const handleTitleSave = async () => {
    const trimmed = titleDraft.trim();
    if (!trimmed || trimmed === pitch.title) {
      setTitleDraft(pitch.title);
      setEditingTitle(false);
      return;
    }
    if (!onTitleSave) return;
    try {
      setSavingTitle(true);
      await onTitleSave(trimmed);
      setEditingTitle(false);
    } catch {
      // toast already shown by caller
    } finally {
      setSavingTitle(false);
    }
  };

  const handleTitleCancel = () => {
    setTitleDraft(pitch.title);
    setEditingTitle(false);
  };

  const handleTitleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleTitleSave();
    } else if (e.key === 'Escape') {
      handleTitleCancel();
    }
  };

  return (
    <div className="flex flex-col md:flex-row justify-between items-stretch md:items-start gap-4 mb-8">
      <div>
        {editingTitle ? (
          <div className="flex items-center gap-2">
            <input
              ref={inputRef}
              type="text"
              value={titleDraft}
              onChange={(e) => setTitleDraft(e.target.value)}
              onKeyDown={handleTitleKeyDown}
              onBlur={handleTitleSave}
              disabled={savingTitle}
              className="text-3xl font-bold tracking-tight bg-transparent border-b-2 border-primary outline-none w-full min-w-0"
            />
            <Button variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={handleTitleSave} disabled={savingTitle}>
              <Check className="h-5 w-5" />
            </Button>
            <Button variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={handleTitleCancel} disabled={savingTitle}>
              <X className="h-5 w-5" />
            </Button>
          </div>
        ) : (
          <div className="group flex items-center gap-2">
            <button
              type="button"
              className="text-3xl font-bold tracking-tight cursor-pointer text-left bg-transparent border-none p-0"
              onClick={() => onTitleSave && setEditingTitle(true)}
              onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && onTitleSave) { e.preventDefault(); setEditingTitle(true); } }}
              title={onTitleSave ? t('pitchDetailPage.clickToEditTitle') : undefined}
              aria-label={onTitleSave ? t('pitchDetailPage.clickToEditTitle') : undefined}
              disabled={!onTitleSave}
            >
              {pitch.title}
            </button>
            {onTitleSave && (
              <button
                type="button"
                onClick={() => setEditingTitle(true)}
                className="opacity-0 group-hover:opacity-100 transition-opacity text-muted-foreground hover:text-foreground"
                aria-label={t('pitchDetailPage.clickToEditTitle')}
                title={t('pitchDetailPage.clickToEditTitle')}
              >
                <Pencil className="h-4 w-4" />
              </button>
            )}
          </div>
        )}
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-muted-foreground mb-1">
          <span>{pitch.teamName || t('common.unassigned')} • {pitch.cycleName}</span>
          <span className="text-muted-foreground/50">•</span>
          {onEpicChange ? (
            <span className="flex items-center gap-1">
              <Layers className="h-3.5 w-3.5" />
              <Select
                value={pitch.epicId?.toString() || '__none__'}
                onValueChange={(value) => {
                  onEpicChange(value === '__none__' ? null : parseInt(value));
                }}
              >
                <SelectTrigger className="h-6 w-auto min-w-[120px] max-w-[200px] text-xs border-dashed px-2 py-0">
                  <SelectValue placeholder={t('pitchDetailPage.selectEpic')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__none__">{t('pitchDetailPage.noEpic')}</SelectItem>
                  {epics?.map((epic) => (
                    <SelectItem key={epic.id} value={epic.id.toString()}>
                      {epic.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </span>
          ) : pitch.epicId ? (
            <Link to={`/epics/${pitch.epicId}`} className="inline-flex items-center gap-1 hover:text-foreground">
              <Layers className="h-3.5 w-3.5" />
              <Badge variant="outline" className="text-xs font-normal">
                {pitch.epicName || t('pitchDetailPage.epic')}
              </Badge>
            </Link>
          ) : (
            <span className="inline-flex items-center gap-1 text-xs italic">
              <Layers className="h-3.5 w-3.5" />
              {t('pitchDetailPage.noEpic')}
            </span>
          )}
        </div>
        {pitch.description && (
          <div className="mt-4">
            <Markdown content={pitch.description} className="text-muted-foreground" />
          </div>
        )}
      </div>
      <div className="flex gap-2 items-center flex-wrap">
        <Button variant="outline" size="sm" asChild>
          <Link to={`/pitches/${pitch.id}/hill-chart`}>{t('pitchDetailPage.hillChart')}</Link>
        </Button>
        <Button
          variant="outline"
          size="sm"
          onClick={onHistoryOpen}
        >
          <History className="h-4 w-4 mr-2" />
          {t('history.viewHistory')}
        </Button>
        <SoftDeleteButton
          entityType="pitch"
          entityId={pitch.id}
          entityTitle={pitch.title}
          onSuccess={() => {
            window.location.href = '/pitches';
          }}
          variant="outline"
          size="sm"
        />
        <StatusChip status={pitch.status} size="medium" />
        <Select
          value={pitch.status}
          onValueChange={(value) => onStatusChange(value as PitchStatus)}
        >
          <SelectTrigger className="w-full sm:w-[150px]">
            <SelectValue placeholder={t('pitchDetailPage.status')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="IDEA">{t('status.idea')}</SelectItem>
            <SelectItem value="DRAFT">{t('status.draft')}</SelectItem>
            <SelectItem value="SHAPED">{t('pitches.status.shaped')}</SelectItem>
            <SelectItem value="PENDING">{t('status.pending')}</SelectItem>
            <SelectItem value="STARTED">{t('status.started')}</SelectItem>
            <SelectItem value="IN_PROGRESS">{t('status.inProgress')}</SelectItem>
            <SelectItem value="TESTING">{t('status.testing')}</SelectItem>
            <SelectItem value="DONE">{t('status.done')}</SelectItem>
            <SelectItem value="COOLDOWN">{t('status.cooldown')}</SelectItem>
            <SelectItem value="CANCELLED">{t('status.cancelled')}</SelectItem>
          </SelectContent>
        </Select>
      </div>
    </div>
  );
}
