import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Users, UserCheck, UserX } from 'lucide-react';
import { teamService } from '../services/teamService';
import { Team, TeamAssignment, TeamMemberRole } from '../types';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import { Separator } from '../components/ui/separator';
import EmptyState from '../components/EmptyState';
import { TeamsSkeleton } from '../components/Skeletons';
import { cn } from '../lib/utils';

const ROLE_CLASS_NAMES: Record<TeamMemberRole, string> = {
  BACKEND: 'bg-blue-500/10 text-blue-700 dark:text-blue-400 border-blue-500/20',
  FRONTEND: 'bg-purple-500/10 text-purple-700 dark:text-purple-400 border-purple-500/20',
  MOBILE: 'bg-indigo-500/10 text-indigo-700 dark:text-indigo-400 border-indigo-500/20',
  QA: 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/20',
  DESIGNER: 'bg-green-500/10 text-green-700 dark:text-green-400 border-green-500/20',
  FULLSTACK: 'bg-cyan-500/10 text-cyan-700 dark:text-cyan-400 border-cyan-500/20',
  TECH_LEAD: 'bg-red-500/10 text-red-700 dark:text-red-400 border-red-500/20',
  PRODUCT_MANAGER: 'bg-gray-500/10 text-gray-700 dark:text-gray-400 border-gray-500/20',
};

export default function TeamDetailPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const teamId = Number(id);

  const { data: team, isLoading, isError } = useQuery<Team>({
    queryKey: ['team', teamId],
    queryFn: async () => {
      const res = await teamService.getById(teamId);
      return res.data;
    },
    enabled: Number.isFinite(teamId),
  });

  if (isLoading) {
    return <TeamsSkeleton />;
  }

  if (isError || !team) {
    return (
      <div>
        <Button asChild variant="ghost" size="sm" className="mb-4 gap-2">
          <Link to="/teams">
            <ArrowLeft className="h-4 w-4" />
            {t('teamDetail.backToTeams')}
          </Link>
        </Button>
        <Card>
          <CardContent className="py-8">
            <EmptyState
              icon={Users}
              title={t('teamDetail.notFound')}
              description={t('teamDetail.notFoundDescription')}
              size="medium"
            />
          </CardContent>
        </Card>
      </div>
    );
  }

  const assignments: TeamAssignment[] = team.assignments ?? [];
  const activeAssignments = assignments.filter((a) => a.isActive);
  const memberCount = assignments.length;
  const activeCount = activeAssignments.length;

  return (
    <div>
      <Button asChild variant="ghost" size="sm" className="mb-4 gap-2">
        <Link to="/teams">
          <ArrowLeft className="h-4 w-4" />
          {t('teamDetail.backToTeams')}
        </Link>
      </Button>

      {/* Header */}
      <div className="flex flex-col gap-1 mb-6">
        <div className="flex items-center gap-3">
          <h1 className="text-3xl font-bold tracking-tight">{team.name}</h1>
          {team.isArchived && (
            <Badge variant="secondary" className="font-normal">
              {t('teamDetail.archived')}
            </Badge>
          )}
        </div>
        <p className="text-sm text-muted-foreground">
          {t('teamDetail.membersSummary', { active: activeCount, total: memberCount })}
        </p>
      </div>

      {/* Workload stats */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-6">
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground mb-1">{t('teamDetail.totalMembers')}</p>
            <p className="text-4xl font-bold">{memberCount}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground mb-1">{t('teamDetail.activeMembers')}</p>
            <p className="text-4xl font-bold text-emerald-600 dark:text-emerald-400">{activeCount}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground mb-1">{t('teamDetail.inactiveMembers')}</p>
            <p className="text-4xl font-bold text-muted-foreground">{memberCount - activeCount}</p>
          </CardContent>
        </Card>
      </div>

      {/* Members + workload */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Users className="h-4 w-4 text-violet-500" />
            {t('teamDetail.membersAndWorkload')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {assignments.length === 0 ? (
            <EmptyState
              icon={Users}
              title={t('teamDetail.noMembers')}
              description={t('teamDetail.noMembersDescription')}
              size="small"
              compact
            />
          ) : (
            <div className="space-y-1">
              {assignments.map((assignment, index) => (
                <div key={assignment.id}>
                  <div className="flex items-center justify-between p-3 rounded-lg">
                    <div className="flex items-center gap-3">
                      <Avatar className="h-8 w-8">
                        <AvatarFallback className="text-xs">
                          {(assignment.personName || 'U').charAt(0)}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <p className="text-sm font-medium">
                          {assignment.personName || t('teamDetail.unknownPerson')}
                        </p>
                        <Badge
                          variant="outline"
                          className={cn('mt-1 text-xs', ROLE_CLASS_NAMES[assignment.role])}
                        >
                          {assignment.role.replace('_', ' ')}
                        </Badge>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      {assignment.effectiveHoursPerDay != null && (
                        <Badge variant="secondary" className="text-xs">
                          {t('teamDetail.hoursPerDay', { hours: assignment.effectiveHoursPerDay })}
                        </Badge>
                      )}
                      {assignment.isActive ? (
                        <Badge
                          variant="outline"
                          className="text-xs gap-1 bg-emerald-500/10 text-emerald-700 dark:text-emerald-400 border-emerald-500/20"
                        >
                          <UserCheck className="h-3 w-3" />
                          {t('teamDetail.active')}
                        </Badge>
                      ) : (
                        <Badge variant="outline" className="text-xs gap-1 text-muted-foreground">
                          <UserX className="h-3 w-3" />
                          {t('teamDetail.inactive')}
                        </Badge>
                      )}
                    </div>
                  </div>
                  {index < assignments.length - 1 && <Separator />}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
