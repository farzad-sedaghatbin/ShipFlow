import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import BugViewDialog from '../components/BugViewDialog';
import qaTestManagementService from '../services/qaTestManagementService';
import { BugReport } from '../types';

/**
 * Standalone page for viewing a bug report by ID.
 * Renders the existing BugViewDialog as a full-screen modal.
 * Used for deep-linking from global search and external references.
 */
export default function BugReportDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [bug, setBug] = useState<BugReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError(null);
    qaTestManagementService.getBugReportById(Number(id))
      .then((res) => setBug(res.data))
      .catch(() => setError(t('globalSearch.bugNotFound', 'Bug report not found')))
      .finally(() => setLoading(false));
  }, [id, t]);

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
    <BugViewDialog
      bug={bug}
      open={true}
      onOpenChange={(open) => {
        if (!open) {
          navigate('/qa/bug-reports');
        }
      }}
    />
  );
}
