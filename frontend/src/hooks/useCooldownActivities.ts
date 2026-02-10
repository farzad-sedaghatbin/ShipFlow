import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useToast } from '../contexts';
import {
  cooldownActivityService,
  CreateCooldownActivityRequest,
  UpdateCooldownActivityRequest,
} from '../services/cooldownActivityService';

// Query keys
export const cooldownKeys = {
  all: ['cooldown'] as const,
  activities: (cycleId: number) => ['cooldown', 'activities', cycleId] as const,
  summary: (cycleId: number) => ['cooldown', 'summary', cycleId] as const,
};

/**
 * Hook to fetch cooldown activities for a cycle
 */
export function useCooldownActivities(cycleId: number | null) {
  return useQuery({
    queryKey: cooldownKeys.activities(cycleId!),
    queryFn: async () => {
      const response = await cooldownActivityService.getActivitiesByCycle(cycleId!);
      return response.data;
    },
    enabled: cycleId !== null,
    staleTime: 30000, // 30 seconds
  });
}

/**
 * Hook to fetch cooldown summary for a cycle
 */
export function useCooldownSummary(cycleId: number | null) {
  return useQuery({
    queryKey: cooldownKeys.summary(cycleId!),
    queryFn: async () => {
      const response = await cooldownActivityService.getCycleSummary(cycleId!);
      return response.data;
    },
    enabled: cycleId !== null,
    staleTime: 30000,
  });
}

/**
 * Hook to create a cooldown activity
 */
export function useCreateCooldownActivity() {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: (data: CreateCooldownActivityRequest) =>
      cooldownActivityService.createActivity(data),
    onSuccess: (_, variables) => {
      // Invalidate and refetch activities and summary for the cycle
      queryClient.invalidateQueries({ queryKey: cooldownKeys.activities(variables.cycleId) });
      queryClient.invalidateQueries({ queryKey: cooldownKeys.summary(variables.cycleId) });
      showSuccess(t('cooldownActivity.createSuccess'));
    },
    onError: (error: any) => {
      showError(error.response?.data?.message || t('cooldownActivity.createError'));
    },
  });
}

/**
 * Hook to update a cooldown activity
 */
export function useUpdateCooldownActivity() {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateCooldownActivityRequest; cycleId: number }) =>
      cooldownActivityService.updateActivity(id, data),
    onSuccess: (_, variables) => {
      // Invalidate and refetch activities and summary for the cycle
      queryClient.invalidateQueries({ queryKey: cooldownKeys.activities(variables.cycleId) });
      queryClient.invalidateQueries({ queryKey: cooldownKeys.summary(variables.cycleId) });
      showSuccess(t('cooldownActivity.updateSuccess'));
    },
    onError: (error: any) => {
      showError(error.response?.data?.message || t('cooldownActivity.updateError'));
    },
  });
}

/**
 * Hook to delete a cooldown activity
 */
export function useDeleteCooldownActivity() {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useToast();
  const { t } = useTranslation();

  return useMutation({
    mutationFn: ({ id }: { id: number; cycleId: number }) =>
      cooldownActivityService.deleteActivity(id),
    onSuccess: (_, variables) => {
      // Invalidate and refetch activities and summary for the cycle
      queryClient.invalidateQueries({ queryKey: cooldownKeys.activities(variables.cycleId) });
      queryClient.invalidateQueries({ queryKey: cooldownKeys.summary(variables.cycleId) });
      showSuccess(t('cooldownActivity.deleteSuccess'));
    },
    onError: (error: any) => {
      showError(error.response?.data?.message || t('cooldownActivity.deleteError'));
    },
  });
}
