import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { knowledgeService } from '../services/knowledgeService';
import type { KnowledgeSource } from '../types/knowledge';

/**
 * Loads the knowledge sources for the currently selected scope. Scope is
 * driven by URL search params (`scope=org|team|project` plus `teamId` /
 * `projectId`) so deep links work and back/forward navigation preserves
 * filter state.
 */
export function useKnowledgeSources() {
  const [params] = useSearchParams();
  const scope = (params.get('scope') ?? 'org') as 'org' | 'team' | 'project';
  const teamId = params.get('teamId');
  const projectId = params.get('projectId');

  return useQuery<KnowledgeSource[]>({
    queryKey: ['knowledge', scope, teamId, projectId],
    queryFn: async () => {
      if (scope === 'team' && teamId) {
        return (await knowledgeService.listTeam(Number(teamId))).data;
      }
      if (scope === 'project' && projectId) {
        return (await knowledgeService.listProject(Number(projectId))).data;
      }
      return (await knowledgeService.listOrg()).data;
    },
    enabled:
      !(scope === 'team' && !teamId) && !(scope === 'project' && !projectId),
  });
}
