import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { cn } from '@/lib/utils';
import { Pitch } from '../../types';
import StatusChip from '../StatusChip';
import EmptyState from '../EmptyState';
import { EmptyPitchesIllustration } from '../illustrations';

interface RecentPitchesWidgetProps {
  pitches: Pitch[];
}

export function RecentPitchesWidget({ pitches }: RecentPitchesWidgetProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex justify-between items-center mb-3">
          <h2 className="text-lg font-semibold text-foreground">{t('dashboard.recentPitches')}</h2>
          <Button variant="ghost" size="sm" asChild>
            <Link to="/pitches">{t('common.viewAll')}</Link>
          </Button>
        </div>
        {pitches.length === 0 ? (
          <EmptyState
            illustration={<EmptyPitchesIllustration width={160} height={120} />}
            title={t('dashboard.noPitches')}
            description={t('dashboard.noPitchesDescription')}
            size="small"
            compact
            action={{
              label: t('dashboard.createPitch'),
              onClick: () => window.location.href = '/pitches/new',
              startIcon: <Plus className="w-4 h-4 mr-1" />,
            }}
          />
        ) : (
          <div className="space-y-2">
            {pitches.map((pitch) => (
              <Link
                key={pitch.id}
                to={`/pitches/${pitch.id}`}
                className="block p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors"
              >
                <div className="flex justify-between items-center mb-1">
                  <span className="font-semibold text-foreground">{pitch.title}</span>
                  <StatusChip status={pitch.status} />
                </div>
                <div className="flex justify-between items-center text-sm text-muted-foreground">
                  <span>{pitch.teamName || t('common.unassigned')} • {pitch.appetiteDays} {t('common.days')}</span>
                  <span>{pitch.progressPercentage?.toFixed(0) || 0}%</span>
                </div>
                <Progress
                  value={Math.min(pitch.progressPercentage || 0, 100)}
                  className={cn(
                    'h-1 mt-1',
                    (pitch.progressPercentage || 0) > 100 && '[&>div]:bg-destructive'
                  )}
                />
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
