import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { CooldownActivityDTO } from '../services/cooldownActivityService';
import { useToast } from '../contexts';
import { safeParseId } from '../utils/validation';
import CooldownActivitiesView from '../components/CooldownActivitiesView';
import {
  useCooldownActivities,
  useCooldownSummary,
  useDeleteCooldownActivity,
} from '../hooks/useCooldownActivities';

/**
 * Container component for Cooldown Activities page
 * Handles business logic, data fetching, and state management
 */
export default function CooldownActivitiesPage() {
  const { t } = useTranslation();
  const { cycleId: cycleIdParam } = useParams<{ cycleId: string }>();
  const cycleId = safeParseId(cycleIdParam);
  const { showError } = useToast();

  // React Query hooks for data fetching
  const { data: activities = [], isLoading: activitiesLoading, error: activitiesError } = useCooldownActivities(cycleId);
  const { data: summary = null, isLoading: summaryLoading } = useCooldownSummary(cycleId);
  const deleteMutation = useDeleteCooldownActivity();

  // UI state
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedActivity, setSelectedActivity] = useState<CooldownActivityDTO | undefined>(undefined);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [activityToDelete, setActivityToDelete] = useState<CooldownActivityDTO | null>(null);
  const [filterType, setFilterType] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');

  // Compute loading state
  const loading = activitiesLoading || summaryLoading;

  // Handle query errors
  if (activitiesError) {
    showError((activitiesError as any).response?.data?.message || t('cooldownActivity.loadError'));
  }

  const handleCreate = () => {
    setSelectedActivity(undefined);
    setDialogOpen(true);
  };

  const handleEdit = (activity: CooldownActivityDTO) => {
    setSelectedActivity(activity);
    setDialogOpen(true);
  };

  const handleDeleteClick = (activity: CooldownActivityDTO) => {
    setActivityToDelete(activity);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = () => {
    if (!activityToDelete || !cycleId) return;

    deleteMutation.mutate(
      { id: activityToDelete.id, cycleId },
      {
        onSuccess: () => {
          setDeleteDialogOpen(false);
          setActivityToDelete(null);
        },
      }
    );
  };

  const filteredActivities = activities.filter((activity) => {
    if (filterType !== 'all' && activity.activityType !== filterType) return false;
    if (filterStatus !== 'all' && activity.status !== filterStatus) return false;
    return true;
  });

  // Render presentational component
  return (
    <CooldownActivitiesView
      loading={loading}
      cycleId={cycleId}
      activities={activities}
      summary={summary}
      dialogOpen={dialogOpen}
      selectedActivity={selectedActivity}
      deleteDialogOpen={deleteDialogOpen}
      activityToDelete={activityToDelete}
      filterType={filterType}
      filterStatus={filterStatus}
      filteredActivities={filteredActivities}
      onCreateClick={handleCreate}
      onEditClick={handleEdit}
      onDeleteClick={handleDeleteClick}
      onDeleteConfirm={handleDeleteConfirm}
      onDialogClose={() => setDialogOpen(false)}
      onDialogSave={() => setDialogOpen(false)}
      onFilterTypeChange={setFilterType}
      onFilterStatusChange={setFilterStatus}
      onDeleteDialogChange={setDeleteDialogOpen}
    />
  );
}
