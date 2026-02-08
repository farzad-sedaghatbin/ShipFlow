import { useCallback, useEffect, useState, useRef } from 'react';
import { ResourceType, PermissionType, permissionService } from '@/services/permissionService';
import { useAuth } from '@/contexts/AuthContext';

interface PermissionCache {
  [key: string]: boolean;
}

interface PendingRequest {
  promise: Promise<boolean>;
  resolve: (value: boolean) => void;
  reject: (error: any) => void;
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
  const cacheRef = useRef<PermissionCache>({});
  const pendingRequestsRef = useRef<Map<string, PendingRequest>>(new Map());
  const [loading, setLoading] = useState(false);

  /**
   * Check if user has permission for a resource and action.
   * Results are cached for the session.
   * Handles concurrent requests to prevent race conditions.
   */
  const hasPermission = useCallback(
    async (resource: ResourceType, permission: PermissionType): Promise<boolean> => {
      if (!user) {
        return false;
      }

      const cacheKey = `${resource}:${permission}`;

      // Return cached result if available
      if (cacheKey in cacheRef.current) {
        return cacheRef.current[cacheKey];
      }

      // If there's already a pending request for this permission, return that promise
      const existingRequest = pendingRequestsRef.current.get(cacheKey);
      if (existingRequest) {
        return existingRequest.promise;
      }

      // Create a new request
      let resolve: (value: boolean) => void;
      let reject: (error: any) => void;
      
      const promise = new Promise<boolean>((res, rej) => {
        resolve = res;
        reject = rej;
      });

      const pendingRequest: PendingRequest = {
        promise,
        resolve: resolve!,
        reject: reject!
      };

      // Store the pending request
      pendingRequestsRef.current.set(cacheKey, pendingRequest);

      try {
        setLoading(true);
        const result = await permissionService.hasPermission(resource, permission);
        
        // Update cache safely - check if key still doesn't exist to prevent overwriting
        if (!(cacheKey in cacheRef.current)) {
          cacheRef.current[cacheKey] = result;
        }
        
        // Resolve the pending request
        pendingRequest.resolve(result);
        
        return result;
      } catch (error) {
        console.error('Permission check failed:', error);
        
        // Reject the pending request
        pendingRequest.reject(error);
        
        // Fail closed - deny permission on error
        return false;
      } finally {
        // Clean up the pending request
        pendingRequestsRef.current.delete(cacheKey);
        setLoading(false);
      }
    },
    [user?.userId] // Only depend on user ID, not entire user object
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
      return cacheRef.current[cacheKey] ?? false;
    },
    [user?.userId] // Only depend on user ID, not entire user object
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
    cacheRef.current = {};
    // Also clear any pending requests
    pendingRequestsRef.current.clear();
  }, []);

  // Clear cache when user changes or logs out
  useEffect(() => {
    if (!user) {
      clearCache();
    }
  }, [user?.userId, clearCache]);

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
  const checkKeyRef = useRef<string>('');

  useEffect(() => {
    let mounted = true;
    const checkKey = `${resource}:${permission}`;
    
    // Avoid unnecessary re-checks if same resource:permission
    if (checkKeyRef.current === checkKey) {
      return;
    }
    
    checkKeyRef.current = checkKey;
    setHasAccess(null); // Reset to loading state

    const checkPermission = async () => {
      const result = await hasPermission(resource, permission);
      if (mounted && checkKeyRef.current === checkKey) {
        setHasAccess(result);
      }
    };

    checkPermission();

    return () => {
      mounted = false;
    };
  }, [resource, permission, hasPermission]); // hasPermission is now stable, safe to include

  // Loading state
  if (hasAccess === null) {
    return <>{fallback}</>;
  }

  return hasAccess ? <>{children}</> : <>{fallback}</>;
}
