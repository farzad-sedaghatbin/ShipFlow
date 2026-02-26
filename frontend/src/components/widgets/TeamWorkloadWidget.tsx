import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Users } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { teamService } from '../../services/teamService';
import { Team } from '../../types';
import { STALE_TIMES, queryKeys } from '../../lib/queryClient';

export function TeamWorkloadWidget() {
  const { t } = useTranslation();

  const { data: teams = [], isLoading: loading } = useQuery({
    queryKey: queryKeys.teams.lists(),
    queryFn: async () => {
      const res = await teamService.getAll();
      const allTeams: Team[] = res.data || [];
      return allTeams.slice(0, 5);
    },
    staleTime: STALE_TIMES.reference,
  });

  if (loading) {
    return (
      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base flex items-center gap-2">
            <Users className="w-4 h-4 text-violet-500" />
            {t('widgets.teamWorkload')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-sm text-muted-foreground">{t('widgets.loading')}</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          <Users className="w-4 h-4 text-violet-500" />
          {t('widgets.teamWorkload')}
        </CardTitle>
      </CardHeader>
      <CardContent>
        {teams.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t('widgets.noTeamData')}</p>
        ) : (
          <div className="space-y-2">
            {teams.map((team) => {
              const memberCount = team.assignments?.length || 0;
              const activeCount = team.assignments?.filter((a) => a.isActive).length || 0;
              return (
                <Link
                  key={team.id}
                  to={`/teams/${team.id}`}
                  className="flex items-center justify-between p-2 rounded-md bg-muted/50 hover:bg-muted transition-colors"
                >
                  <span className="text-sm font-medium text-foreground">{team.name}</span>
                  <div className="flex items-center gap-1.5">
                    <Badge variant="secondary" className="text-xs">
                      {activeCount}/{memberCount} members
                    </Badge>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
