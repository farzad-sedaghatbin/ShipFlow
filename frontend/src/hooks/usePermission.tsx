import { useCallback, useEffect, useState } from 'react';
import { ResourceType, PermissionType, permissionService } from '@/services/permissionService';
import { useAuth } from '@/contexts/AuthContext';

interface PermissionCache {
  [key: string]: boolean;
}

/**
 * Hook to check if the current user has a specific permission.
 * 
 * Uses backend RBAC system to enforce fine-grained permissions.
 * Results are cached to minimize API calls.
 * 
 * @example
 * ```tsx
 * const { hasPermission, loading } = usePermission();
 * 
 * if (loading) return <Skeleton />;
 * 
 * return (
 *   <>
 *     {hasPermission('PITCH', 'CREATE') && (
 *       <Button onClick={openCreateDialog}>Create Pitch</Button>
 *     )}
 *     {hasPermission('PITCH', 'DELETE') && (
 *       <Button onClick={deletePitch}>Delete</Button>
 *     )}
 *   </>
 * );
 * ```
 */
export function usePermission() {
  const { user } = useAuth();
  const [cache, setCache] = useState<PermissionCache>({});
  const [loading, setLoading] = useState(false);

  /**
   * Check if user has permission for a resource and action.
   * Results are cached for the session.
   */
  const hasPermission = useCallback(
    async (resource: ResourceType, permission: PermissionType): Promise<boolean> => {
      if (!user) {
        return false;
      }

      const cacheKey = `${resource}:${permission}`;

      // Return cached result if available
      if (cacheKey in cache) {
        return cache[cacheKey];
      }

      try {
        setLoading(true);
        const result = await permissionService.hasPermission(resource, permission);
        
        // Update cache
        setCache(prev => ({ ...prev, [cacheKey]: result }));
        
        return result;
      } catch (error) {
        console.error('Permission check failed:', error);
        // Fail closed - deny permission on error
        return false;
      } finally {
        setLoading(false);
      }
    },
    [user, cache]
  );

  /**
   * Synchronous check using cached permissions.
   * Returns false if permission hasn't been checked yet.
   * Prefer async hasPermission() for initial checks.
   */
  const hasPermissionSync = useCallback(
    (resource: ResourceType, permission: PermissionType): boolean => {
      if (!user) return false;
      
      const cacheKey = `${resource}:${permission}`;
      return cache[cacheKey] ?? false;
    },
    [user, cache]
  );

  /**
   * Check if user has ANY of the provided permissions.
   */
  const hasAnyPermission = useCallback(
    async (checks: Array<{ resource: ResourceType; permission: PermissionType }>): Promise<boolean> => {
      const results = await Promise.all(
        checks.map(({ resource, permission }) => hasPermission(resource, permission))
      );
      return results.some(result => result === true);
    },
    [hasPermission]
  );

  /**
   * Check if user has ALL of the provided permissions.
   */
  const hasAllPermissions = useCallback(
    async (checks: Array<{ resource: ResourceType; permission: PermissionType }>): Promise<boolean> => {
      const results = await Promise.all(
        checks.map(({ resource, permission }) => hasPermission(resource, permission))
      );
      return results.every(result => result === true);
    },
    [hasPermission]
  );

  /**
   * Clear permission cache (e.g., on logout or role change).
   */
  const clearCache = useCallback(() => {
    setCache({});
  }, []);

  // Clear cache when user changes or logs out
  useEffect(() => {
    if (!user) {
      clearCache();
    }
  }, [user, clearCache]);

  return {
    hasPermission,
    hasPermissionSync,
    hasAnyPermission,
    hasAllPermissions,
    clearCache,
    loading,
    user
  };
}

/**
 * Higher-order component to conditionally render based on permission.
 * 
 * @example
 * ```tsx
 * <PermissionGate resource="PITCH" permission="DELETE">
 *   <Button onClick={deletePitch}>Delete</Button>
 * </PermissionGate>
 * ```
 */
export function PermissionGate({
  resource,
  permission,
  children,
  fallback = null
}: {
  resource: ResourceType;
  permission: PermissionType;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}) {
  const [hasAccess, setHasAccess] = useState<boolean | null>(null);
  const { hasPermission } = usePermission();

  useEffect(() => {
    let mounted = true;

    const checkPermission = async () => {
      const result = await hasPermission(resource, permission);
      if (mounted) {
        setHasAccess(result);
      }
    };

    checkPermission();

    return () => {
      mounted = false;
    };
  }, [resource, permission, hasPermission]);

  // Loading state
  if (hasAccess === null) {
    return <>{fallback}</>;
  }

  return hasAccess ? <>{children}</> : <>{fallback}</>;
}
