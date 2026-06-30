import { useTranslation } from 'react-i18next';
import { Users } from 'lucide-react';
import { Pitch } from '../../types';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { cn } from '../../lib/utils';

interface PitchTeamCapacityProps {
  pitch: Pitch;
}

export function PitchTeamCapacity({ pitch }: PitchTeamCapacityProps) {
  const { t } = useTranslation();

  if (!pitch.teamId || !pitch.teamMemberCount || pitch.teamMemberCount <= 0) {
    return null;
  }

  return (
    <Card className="mb-6">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Users className="h-5 w-5 text-primary" />
          {t('pitchDetailPage.teamCapacity')}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <p className="text-sm text-muted-foreground mb-1">{t('pitchDetailPage.teamMembers')}</p>
            <p className="text-2xl font-bold">{pitch.teamMemberCount}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground mb-1">{t('pitchDetailPage.budgetPersonDays')}</p>
            <p className="text-2xl font-bold">{pitch.totalBudgetPersonDays?.toFixed(1) || 0}</p>
          </div>
          <div>
            <p className="text-sm text-muted-foreground mb-1">{t('pitchDetailPage.budgetUtilization')}</p>
            <p className={cn(
              'text-2xl font-bold',
              (pitch.budgetUtilizationPercent || 0) > 100 ? 'text-destructive' :
              (pitch.budgetUtilizationPercent || 0) > 80 ? 'text-orange-500' :
              'text-success'
            )}>
              {pitch.budgetUtilizationPercent?.toFixed(1) || 0}%
            </p>
          </div>
        </div>

        {pitch.busiestPerson && (
          <div className="mt-6 pt-6 border-t">
            <p className="text-sm text-muted-foreground mb-3">{t('pitchDetailPage.busiestPerson')}</p>
            <div className="bg-muted/50 rounded-lg p-4">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <p className="font-semibold">{pitch.busiestPerson.personName}</p>
                  {pitch.busiestPerson.role && (
                    <p className="text-sm text-muted-foreground">{pitch.busiestPerson.role}</p>
                  )}
                </div>
                <div className={cn(
                  'px-3 py-1 rounded-full text-sm font-medium',
                  pitch.busiestPerson.isOverBudget ? 'bg-destructive/10 text-destructive' :
                  pitch.busiestPerson.utilizationPercent > 80 ? 'bg-orange-500/10 text-orange-500' :
                  'bg-green-500/10 text-green-500'
                )}>
                  {pitch.busiestPerson.utilizationPercent?.toFixed(0)}% {t('pitchDetailPage.utilizationPercent')}
                </div>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                <div>
                  <p className="text-muted-foreground">{t('pitchDetailPage.hoursPerDay')}</p>
                  <p className="font-medium">{pitch.busiestPerson.hoursPerDay}h</p>
                </div>
                <div>
                  <p className="text-muted-foreground">{t('pitchDetailPage.capacitySource')}</p>
                  <p className="font-medium capitalize">{pitch.busiestPerson.capacitySource}</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Budget</p>
                  <p className="font-medium">{pitch.busiestPerson.totalBudgetHours?.toFixed(0)}h</p>
                </div>
                <div>
                  <p className="text-muted-foreground">Spent</p>
                  <p className="font-medium">{pitch.busiestPerson.hoursSpent?.toFixed(1)}h</p>
                </div>
              </div>
              {pitch.busiestPerson.isOverBudget && (
                <div className="mt-3 p-2 bg-destructive/10 rounded text-sm text-destructive">
                  ⚠️ {t('pitchDetailPage.overBudget')} - This person has exceeded their individual budget
                </div>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
