import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { FileText, Plus, Pencil, Trash2, CalendarDays } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Textarea } from '../ui/textarea';
import { Badge } from '../ui/badge';
import { ConfirmDialog } from '../ui/confirm-dialog';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import EmptyState from '../EmptyState';
import { EmptyTasksIllustration } from '../illustrations';
import { LocalizedDateInput } from '../LocalizedDateInput';
import { useToast } from '../../contexts';
import { projectAgreementService } from '../../services/projectAgreementService';
import { meetingService } from '../../services/meetingService';
import { ProjectAgreement, ProjectAgreementRequest } from '../../types';
import { formatLocalizedDate } from '../../utils/dateLocalization';

interface ProjectAgreementsSectionProps {
  projectId: number;
}

interface FormState {
  title: string;
  content: string;
  agreedDate: string;
  meetingId?: number;
}

const emptyForm = (): FormState => ({
  title: '',
  content: '',
  agreedDate: dayjs().format('YYYY-MM-DD'),
  meetingId: undefined,
});

export default function ProjectAgreementsSection({ projectId }: ProjectAgreementsSectionProps) {
  const { t, i18n } = useTranslation();
  const { showSuccess, showError } = useToast();
  const queryClient = useQueryClient();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingAgreement, setEditingAgreement] = useState<ProjectAgreement | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm());
  const [deleteTarget, setDeleteTarget] = useState<ProjectAgreement | null>(null);

  const queryKey = ['project-agreements', projectId];

  const { data: agreements = [], isLoading } = useQuery<ProjectAgreement[]>({
    queryKey,
    queryFn: () => projectAgreementService.list(projectId).then((r) => r.data),
  });

  // Only fetched once the dialog is open, so a project with an empty
  // agreements list doesn't also pay for a meetings fetch up front.
  const { data: meetings = [] } = useQuery({
    queryKey: ['project-agreements-meetings', projectId],
    queryFn: () =>
      meetingService
        .getWithFilters({ projectId, size: 100, sortBy: 'dateHeld', sortOrder: 'desc' })
        .then((r) => r.data.content),
    enabled: dialogOpen,
  });

  const createMutation = useMutation({
    mutationFn: (request: ProjectAgreementRequest) => projectAgreementService.create(projectId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      showSuccess(t('projectAgreements.toast.createSuccess'));
      closeDialog();
    },
    onError: () => showError(t('projectAgreements.toast.saveFailed')),
  });

  const updateMutation = useMutation({
    mutationFn: ({ agreementId, request }: { agreementId: number; request: ProjectAgreementRequest }) =>
      projectAgreementService.update(projectId, agreementId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      showSuccess(t('projectAgreements.toast.updateSuccess'));
      closeDialog();
    },
    onError: () => showError(t('projectAgreements.toast.saveFailed')),
  });

  const deleteMutation = useMutation({
    mutationFn: (agreementId: number) => projectAgreementService.remove(projectId, agreementId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      showSuccess(t('projectAgreements.toast.deleteSuccess'));
    },
    onError: () => showError(t('projectAgreements.toast.deleteFailed')),
    onSettled: () => setDeleteTarget(null),
  });

  const openCreateDialog = () => {
    setEditingAgreement(null);
    setForm(emptyForm());
    setDialogOpen(true);
  };

  const openEditDialog = (agreement: ProjectAgreement) => {
    setEditingAgreement(agreement);
    setForm({
      title: agreement.title,
      content: agreement.content,
      agreedDate: agreement.agreedDate || dayjs().format('YYYY-MM-DD'),
      meetingId: agreement.meetingId,
    });
    setDialogOpen(true);
  };

  const closeDialog = () => {
    setDialogOpen(false);
    setEditingAgreement(null);
    setForm(emptyForm());
  };

  const handleSubmit = () => {
    if (!form.title.trim() || !form.content.trim()) return;
    const request: ProjectAgreementRequest = {
      title: form.title.trim(),
      content: form.content.trim(),
      agreedDate: form.agreedDate || undefined,
      meetingId: form.meetingId,
    };
    if (editingAgreement) {
      updateMutation.mutate({ agreementId: editingAgreement.id, request });
    } else {
      createMutation.mutate(request);
    }
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;
  const meetingTitleById = new Map(meetings.map((m) => [m.id, m]));

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2 text-lg">
            <FileText className="h-5 w-5" />
            {t('projectAgreements.title')}
            {agreements.length > 0 && (
              <Badge variant="outline">{agreements.length}</Badge>
            )}
          </CardTitle>
          <Button size="sm" onClick={openCreateDialog}>
            <Plus className="h-4 w-4 mr-2" />
            {t('projectAgreements.addAgreement')}
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="py-6 text-center text-sm text-muted-foreground">
            {t('common.loading')}
          </div>
        ) : agreements.length === 0 ? (
          <EmptyState
            illustration={<EmptyTasksIllustration />}
            title={t('projectAgreements.empty.title')}
            description={t('projectAgreements.empty.description')}
            action={{
              label: t('projectAgreements.addAgreement'),
              onClick: openCreateDialog,
              startIcon: <Plus className="h-4 w-4" />,
            }}
            size="small"
          />
        ) : (
          <div className="space-y-3">
            {agreements.map((agreement) => (
              <div
                key={agreement.id}
                className="rounded-md border border-border p-4 space-y-2"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0 flex-1">
                    <h4 className="font-medium text-sm break-words">{agreement.title}</h4>
                    <p className="text-sm text-muted-foreground whitespace-pre-wrap mt-1 break-words">
                      {agreement.content}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-1">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      title={t('projectAgreements.editTooltip')}
                      aria-label={t('projectAgreements.editTooltip')}
                      onClick={() => openEditDialog(agreement)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      className="text-destructive hover:text-destructive hover:bg-destructive/10"
                      title={t('projectAgreements.deleteTooltip')}
                      aria-label={t('projectAgreements.deleteTooltip')}
                      onClick={() => setDeleteTarget(agreement)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                  {agreement.agreedDate && (
                    <span className="flex items-center gap-1">
                      <CalendarDays className="h-3 w-3" />
                      {formatLocalizedDate(agreement.agreedDate, i18n.language)}
                    </span>
                  )}
                  {agreement.createdByName && (
                    <span>{t('projectAgreements.agreedBy', { name: agreement.createdByName })}</span>
                  )}
                  {agreement.meetingId && (
                    <Badge variant="secondary" className="text-xs">
                      {meetingTitleById.get(agreement.meetingId)
                        ? t('projectAgreements.fromMeetingType', {
                            type: meetingTitleById.get(agreement.meetingId)!.type.replace(/_/g, ' '),
                          })
                        : t('projectAgreements.fromMeeting')}
                    </Badge>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>

      {/* Add/Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={(open) => (open ? setDialogOpen(true) : closeDialog())}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>
              {editingAgreement
                ? t('projectAgreements.dialog.editTitle')
                : t('projectAgreements.dialog.newTitle')}
            </DialogTitle>
          </DialogHeader>
          <div className="grid gap-4 py-2">
            <div className="space-y-2">
              <Label htmlFor="agreement-title">{t('projectAgreements.dialog.titleLabel')} *</Label>
              <Input
                id="agreement-title"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder={t('projectAgreements.dialog.titlePlaceholder')}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="agreement-content">{t('projectAgreements.dialog.contentLabel')} *</Label>
              <Textarea
                id="agreement-content"
                value={form.content}
                onChange={(e) => setForm({ ...form, content: e.target.value })}
                rows={4}
                placeholder={t('projectAgreements.dialog.contentPlaceholder')}
              />
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="agreement-date">{t('projectAgreements.dialog.agreedDate')}</Label>
                <LocalizedDateInput
                  id="agreement-date"
                  value={form.agreedDate}
                  onChange={(value) => setForm({ ...form, agreedDate: value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="agreement-meeting">{t('projectAgreements.dialog.meeting')}</Label>
                <Select
                  value={form.meetingId?.toString() || 'none'}
                  onValueChange={(value) =>
                    setForm({ ...form, meetingId: value === 'none' ? undefined : parseInt(value) })
                  }
                >
                  <SelectTrigger id="agreement-meeting">
                    <SelectValue placeholder={t('projectAgreements.dialog.selectMeeting')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">{t('projectAgreements.dialog.noMeeting')}</SelectItem>
                    {meetings.map((m) => (
                      <SelectItem key={m.id} value={m.id.toString()}>
                        {formatLocalizedDate(m.dateHeld, i18n.language)} — {m.type.replace(/_/g, ' ')}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={closeDialog} disabled={isSaving}>
              {t('projectAgreements.dialog.cancel')}
            </Button>
            <Button
              onClick={handleSubmit}
              disabled={!form.title.trim() || !form.content.trim() || isSaving}
            >
              {editingAgreement ? t('projectAgreements.dialog.update') : t('projectAgreements.dialog.create')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title={t('projectAgreements.deleteConfirm.title')}
        description={t('projectAgreements.deleteConfirm.description', {
          title: deleteTarget?.title ?? '',
        })}
        confirmLabel={t('projectAgreements.deleteConfirm.confirm')}
        cancelLabel={t('projectAgreements.dialog.cancel')}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget.id)}
        variant="destructive"
        loading={deleteMutation.isPending}
      />
    </Card>
  );
}
