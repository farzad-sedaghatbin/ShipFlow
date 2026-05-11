import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { History, Pencil, Check, X } from 'lucide-react';
import { Pitch, PitchStatus } from '../../types';
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

interface PitchHeaderProps {
  pitch: Pitch;
  onStatusChange: (newStatus: PitchStatus) => void;
  onHistoryOpen: () => void;
  onTitleSave?: (newTitle: string) => Promise<void>;
}

export function PitchHeader({ pitch, onStatusChange, onHistoryOpen, onTitleSave }: PitchHeaderProps) {
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
    } finally {
      setSavingTitle(false);
    }
  };

  const handleTitleCancel = () => {
    setTitleDraft(pitch.title);
    setEditingTitle(false);
  };

  const handleTitleKeyDown = (e: React.KeyboardEvent) => {
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
            <Button variant="ghost" size="icon" onClick={handleTitleSave} disabled={savingTitle}>
              <Check className="h-5 w-5" />
            </Button>
            <Button variant="ghost" size="icon" onMouseDown={(e) => e.preventDefault()} onClick={handleTitleCancel} disabled={savingTitle}>
              <X className="h-5 w-5" />
            </Button>
          </div>
        ) : (
          <div className="group flex items-center gap-2">
            <h1
              className="text-3xl font-bold tracking-tight cursor-pointer"
              onClick={() => onTitleSave && setEditingTitle(true)}
              title={onTitleSave ? t('pitchDetailPage.clickToEditTitle') : undefined}
            >
              {pitch.title}
            </h1>
            {onTitleSave && (
              <button
                onClick={() => setEditingTitle(true)}
                className="opacity-0 group-hover:opacity-100 transition-opacity text-muted-foreground hover:text-foreground"
              >
                <Pencil className="h-4 w-4" />
              </button>
            )}
          </div>
        )}
        <p className="text-muted-foreground mb-1">
          {pitch.teamName || t('common.unassigned')} • {pitch.cycleName}
        </p>
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
