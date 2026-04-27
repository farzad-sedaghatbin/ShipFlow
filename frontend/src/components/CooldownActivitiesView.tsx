import { useTranslation } from 'react-i18next';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Plus, Loader2 } from 'lucide-react';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '../components/ui/alert-dialog';
import { CooldownActivityDTO, CooldownSummaryDTO } from '../services/cooldownActivityService';
import CooldownActivityDialog from '../components/CooldownActivityDialog';
import CooldownSummaryCards from '../components/CooldownSummaryCards';
import CooldownActivityFilters from '../components/CooldownActivityFilters';
import CooldownActivityTable from '../components/CooldownActivityTable';

interface CooldownActivitiesViewProps {
  // State
  loading: boolean;
  cycleId: number | null;
  activities: CooldownActivityDTO[];
  summary: CooldownSummaryDTO | null;
  dialogOpen: boolean;
  selectedActivity?: CooldownActivityDTO;
  deleteDialogOpen: boolean;
  activityToDelete: CooldownActivityDTO | null;
  filterType: string;
  filterStatus: string;
  filteredActivities: CooldownActivityDTO[];
  
  // Handlers
  onCreateClick: () => void;
  onEditClick: (activity: CooldownActivityDTO) => void;
  onDeleteClick: (activity: CooldownActivityDTO) => void;
  onDeleteConfirm: () => void;
  onDialogClose: () => void;
  onDialogSave: () => void;
  onFilterTypeChange: (value: string) => void;
  onFilterStatusChange: (value: string) => void;
  onDeleteDialogChange: (open: boolean) => void;
}

export default function CooldownActivitiesView({
  loading,
  cycleId,
  summary,
  dialogOpen,
  selectedActivity,
  deleteDialogOpen,
  activityToDelete,
  filterType,
  filterStatus,
  filteredActivities,
  onCreateClick,
  onEditClick,
  onDeleteClick,
  onDeleteConfirm,
  onDialogClose,
  onDialogSave,
  onFilterTypeChange,
  onFilterStatusChange,
  onDeleteDialogChange,
}: CooldownActivitiesViewProps) {
  const { t } = useTranslation();

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!cycleId) {
    return (
      <div className="p-6">
        <Card>
          <CardContent className="p-12 text-center">
            <p className="text-muted-foreground">{t('cooldownActivity.noCycleSelected')}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">{t('cooldownActivity.pageTitle')}</h1>
          <p className="text-muted-foreground mt-1">{t('cooldownActivity.subtitle')}</p>
        </div>
        <Button onClick={onCreateClick}>
          <Plus className="h-4 w-4 me-2" />
          {t('cooldownActivity.createActivity')}
        </Button>
      </div>

      {/* Summary Cards */}
      {summary && <CooldownSummaryCards summary={summary} />}

      {/* Filters */}
      <CooldownActivityFilters
        filterType={filterType}
        filterStatus={filterStatus}
        onTypeChange={onFilterTypeChange}
        onStatusChange={onFilterStatusChange}
      />

      {/* Activities Table */}
      <Card>
        <CardContent className="pt-6">
          <CooldownActivityTable
            activities={filteredActivities}
            onEdit={onEditClick}
            onDelete={onDeleteClick}
            onCreate={onCreateClick}
          />
        </CardContent>
      </Card>

      {/* Dialogs */}
      {cycleId && (
        <CooldownActivityDialog
          open={dialogOpen}
          onClose={onDialogClose}
          onSave={onDialogSave}
          cycleId={cycleId}
          activity={selectedActivity}
        />
      )}

      <AlertDialog open={deleteDialogOpen} onOpenChange={onDeleteDialogChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('cooldownActivity.deleteConfirmTitle')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('cooldownActivity.deleteConfirmMessage', { title: activityToDelete?.title })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common.cancel')}</AlertDialogCancel>
            <AlertDialogAction onClick={onDeleteConfirm} className="bg-red-600 hover:bg-red-700">
              {t('common.delete')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
