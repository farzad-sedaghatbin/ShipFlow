import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  CooldownActivityDTO,
  CooldownSummaryDTO,
  cooldownActivityService,
} from '../services/cooldownActivityService';
import { useToast } from '../contexts';
import { safeParseId } from '../utils/validation';
import CooldownActivitiesView from '../components/CooldownActivitiesView';

/**
 * Container component for Cooldown Activities page
 * Handles business logic, data fetching, and state management
 */
export default function CooldownActivitiesPage() {
  const { t } = useTranslation();
  const { cycleId: cycleIdParam } = useParams<{ cycleId: string }>();
  const cycleId = safeParseId(cycleIdParam);
  const { showSuccess, showError } = useToast();

  const [activities, setActivities] = useState<CooldownActivityDTO[]>([]);
  const [summary, setSummary] = useState<CooldownSummaryDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedActivity, setSelectedActivity] = useState<CooldownActivityDTO | undefined>(undefined);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [activityToDelete, setActivityToDelete] = useState<CooldownActivityDTO | null>(null);

  const [filterType, setFilterType] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');

  useEffect(() => {
    if (cycleId) {
      loadData();
    }
  }, [cycleId]);

  const loadData = async () => {
    if (!cycleId) return;

    setLoading(true);
    try {
      const [activitiesRes, summaryRes] = await Promise.all([
        cooldownActivityService.getActivitiesByCycle(cycleId),
        cooldownActivityService.getCycleSummary(cycleId),
      ]);
      setActivities(activitiesRes.data);
      setSummary(summaryRes.data);
    } catch (error: any) {
      showError(error.response?.data?.message || t('cooldownActivity.loadError'));
    } finally {
      setLoading(false);
    }
  };

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

  const handleDeleteConfirm = async () => {
    if (!activityToDelete) return;

    try {
      await cooldownActivityService.deleteActivity(activityToDelete.id);
      showSuccess(t('cooldownActivity.deleteSuccess'));
      setDeleteDialogOpen(false);
      setActivityToDelete(null);
      loadData();
    } catch (error: any) {
      showError(error.response?.data?.message || t('cooldownActivity.deleteError'));
    }
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
      onDialogSave={loadData}
      onFilterTypeChange={setFilterType}
      onFilterStatusChange={setFilterStatus}
      onDeleteDialogChange={setDeleteDialogOpen}
    />
  );
}
