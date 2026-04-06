import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { History } from 'lucide-react';
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
}

export function PitchHeader({ pitch, onStatusChange, onHistoryOpen }: PitchHeaderProps) {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col md:flex-row justify-between items-stretch md:items-start gap-4 mb-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">
          {pitch.title}
        </h1>
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
