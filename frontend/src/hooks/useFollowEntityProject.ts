import { useEffect } from 'react';
import { useProject } from '../contexts/ProjectContext';

/**
 * Switch this tab's project context to the project of the entity a detail page is showing.
 * Call it with the loaded entity's `projectId` (undefined while it's still loading). See
 * `followEntityProject` in ProjectContext for exactly when it switches and when it doesn't.
 */
export function useFollowEntityProject(projectId: number | null | undefined) {
  const { followEntityProject } = useProject();
  useEffect(() => {
    followEntityProject(projectId);
  }, [projectId, followEntityProject]);
}
