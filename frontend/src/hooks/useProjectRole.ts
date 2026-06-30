import { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { projectService } from '../services/projectService';
import type { ProjectRole } from '../types';

interface UseProjectRoleResult {
  role: ProjectRole | null;
  isManager: boolean;
  isContributor: boolean;
  isViewer: boolean;
  loading: boolean;
}

export function useProjectRole(projectId?: number): UseProjectRoleResult {
  const { user } = useAuth();
  const [role, setRole] = useState<ProjectRole | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!projectId) {
      setLoading(false);
      return;
    }

    if (user?.role === 'ADMIN') {
      setRole('MANAGER');
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    projectService
      .getMyRole(projectId)
      .then((r) => {
        if (!cancelled) setRole(r);
      })
      .catch(() => {
        if (!cancelled) setRole(null);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [projectId, user?.role]);

  return {
    role,
    isManager: role === 'MANAGER',
    isContributor: role === 'CONTRIBUTOR' || role === 'MANAGER',
    isViewer: role !== null,
    loading,
  };
}
