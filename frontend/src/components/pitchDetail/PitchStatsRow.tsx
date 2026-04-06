import { useTranslation } from 'react-i18next';
import { Pitch } from '../../types';
import { Card, CardContent } from '../ui/card';
import { cn } from '../../lib/utils';

interface PitchStatsRowProps {
  pitch: Pitch;
  totalHours: number;
  workLogTotalElements: number;
}

export function PitchStatsRow({ pitch, totalHours, workLogTotalElements }: PitchStatsRowProps) {
  const { t } = useTranslation();

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 mb-8">
      <Card>
        <CardContent className="pt-6">
          <p className="text-sm text-muted-foreground mb-1">{t('pitchDetailPage.appetite')}</p>
          <p className="text-3xl font-bold">{pitch.appetiteDays} {t('common.days')}</p>
          <p className="text-sm text-muted-foreground">
            ({pitch.appetiteHours?.toFixed(0)} hours)
          </p>
        </CardContent>
      </Card>
      <Card>
        <CardContent className="pt-6">
          <p className="text-sm text-muted-foreground mb-1">{t('pitchDetailPage.actualHours')}</p>
          <p
            className={cn(
              'text-3xl font-bold',
              totalHours > (pitch.appetiteHours || 0)
                ? 'text-destructive'
                : 'text-success'
            )}
          >
            {totalHours.toFixed(1)}h
          </p>
        </CardContent>
      </Card>
      <Card>
        <CardContent className="pt-6">
          <p className="text-sm text-muted-foreground mb-1">{t('dashboard.progress')}</p>
          <p className="text-3xl font-bold">
            {pitch.progressPercentage?.toFixed(0) || 0}%
          </p>
        </CardContent>
      </Card>
      <Card>
        <CardContent className="pt-6">
          <p className="text-sm text-muted-foreground mb-1">{t('pitchDetailPage.workLogs')}</p>
          <p className="text-3xl font-bold">{workLogTotalElements}</p>
        </CardContent>
      </Card>
    </div>
  );
}
