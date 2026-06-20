import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { BugViewDialog } from '../components/BugViewDialog';
import BugReportModal from '../components/BugReportModal';
import qaTestManagementService from '../services/qaTestManagementService';
import { BugReport, CreateBugReportRequest, UpdateBugReportRequest } from '../types';

export default function BugReportDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [bug, setBug] = useState<BugReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editOpen, setEditOpen] = useState(false);

  const load = () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    qaTestManagementService.getBugReportById(Number(id))
      .then((res) => setBug(res.data))
      .catch(() => setError(t('globalSearch.bugNotFound', 'Bug report not found')))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [id]);

  const handleUpdate = async (data: CreateBugReportRequest | UpdateBugReportRequest) => {
    if (!bug) return;
    const res = await qaTestManagementService.updateBugReport(bug.id, data as UpdateBugReportRequest);
    setBug(res.data);
    setEditOpen(false);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error || !bug) {
    return (
      <div className="flex flex-col items-center justify-center h-64 gap-4">
        <p className="text-muted-foreground">{error || t('globalSearch.bugNotFound', 'Bug report not found')}</p>
        <Button variant="outline" onClick={() => navigate('/qa/bug-reports')}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          {t('globalSearch.backToBugs', 'Back to Bug Reports')}
        </Button>
      </div>
    );
  }

  return (
    <>
      <BugViewDialog
        bug={bug}
        open={true}
        onOpenChange={(open) => { if (!open) navigate('/qa/bug-reports'); }}
        onEdit={() => setEditOpen(true)}
      />
      <BugReportModal
        open={editOpen}
        onClose={() => setEditOpen(false)}
        onSubmit={handleUpdate}
        bugReport={bug}
      />
    </>
  );
}
