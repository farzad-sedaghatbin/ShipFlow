import { useState, useEffect, useRef } from 'react';
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
  // Tracks the bugKey we've already loaded so the canonical-URL redirect below doesn't
  // trigger a redundant re-fetch when the param flips from a numeric id to the key.
  const loadedKeyRef = useRef<string | null>(null);

  const load = () => {
    if (!id) return;
    // After a legacy numeric URL is rewritten to its bugKey the param changes, but we're
    // already showing that bug — skip the refetch (and the loader flash).
    if (loadedKeyRef.current === id) return;
    setLoading(true);
    setError(null);
    // The canonical bug URL uses the human-friendly bugKey (e.g. BUG-150). Older links
    // (legacy notification actionUrls, previously-shared URLs) still carry the numeric DB
    // id, so resolve by id when the param is all digits and by key otherwise.
    const isNumericId = /^\d+$/.test(id);
    const request = isNumericId
      ? qaTestManagementService.getBugReportById(Number(id))
      : qaTestManagementService.getBugReportByKey(id);
    request
      .then((res) => {
        setBug(res.data);
        loadedKeyRef.current = res.data.bugKey;
        // Rewrite a legacy numeric URL to the bugKey so the address bar matches the key
        // shown throughout the UI.
        if (isNumericId && res.data.bugKey) {
          navigate(`/qa/bug-reports/${res.data.bugKey}`, { replace: true });
        }
      })
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
