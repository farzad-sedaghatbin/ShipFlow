import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { licenseService } from '../services/licenseService';

const LICENSE_STATUS_QUERY_KEY = ['license-status'];

/**
 * Loads the current licence status. Open to any authenticated user — the
 * backend's GET /license/status endpoint has no role restriction, so both
 * the Licence settings tab (status section) and LicenseBanner rely on this
 * same hook/query key.
 */
export function useLicenseStatus() {
  return useQuery({
    queryKey: LICENSE_STATUS_QUERY_KEY,
    queryFn: () => licenseService.getStatus().then((r) => r.data),
  });
}

/**
 * Uploads a new licence file's content (admin only — enforced server-side).
 * Invalidates the cached status on success so the tab/banner refresh.
 */
export function useUploadLicense() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (content: string) => licenseService.uploadLicense(content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LICENSE_STATUS_QUERY_KEY });
    },
  });
}

/**
 * Removes the currently installed licence (admin only — enforced
 * server-side). Invalidates the cached status on success.
 */
export function useRemoveLicense() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => licenseService.removeLicense(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LICENSE_STATUS_QUERY_KEY });
    },
  });
}
