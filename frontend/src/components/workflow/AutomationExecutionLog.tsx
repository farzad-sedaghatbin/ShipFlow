import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { CheckCircle2, XCircle, MinusCircle, X } from 'lucide-react';
import { automationService } from '../../services/automationService';
import type { WorkflowAutomationExecution } from '../../types/automation';
import { formatLocalizedDate } from '../../utils/dateLocalization';
import { useTranslation as useI18n } from 'react-i18next';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '../ui/sheet';

interface Props {
  // When automationId is set, rendered as a sheet for a single rule.
  automationId?: number;
  automationName?: string;
  // When executions is set, rendered inline (project-level view).
  executions?: WorkflowAutomationExecution[];
  onClose?: () => void;
}

const StatusIcon = ({ status }: { status: string }) => {
  if (status === 'SUCCESS') return <CheckCircle2 className="h-4 w-4 text-green-500" />;
  if (status === 'FAILURE') return <XCircle className="h-4 w-4 text-destructive" />;
  return <MinusCircle className="h-4 w-4 text-muted-foreground" />;
};

function ExecutionTable({ items }: { items: WorkflowAutomationExecution[] }) {
  const { i18n } = useI18n();
  return (
    <div className="overflow-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b text-left text-muted-foreground">
            <th className="pb-2 pr-4 font-medium">Status</th>
            <th className="pb-2 pr-4 font-medium">Trigger</th>
            <th className="pb-2 pr-4 font-medium">Message</th>
            <th className="pb-2 font-medium">When</th>
          </tr>
        </thead>
        <tbody>
          {items.map(exec => (
            <tr key={exec.id} className="border-b last:border-0">
              <td className="py-2 pr-4">
                <span className="flex items-center gap-1">
                  <StatusIcon status={exec.status} />
                  <Badge
                    variant={exec.status === 'SUCCESS' ? 'success' : exec.status === 'FAILURE' ? 'destructive' : 'secondary'}
                    className="text-xs"
                  >
                    {exec.status}
                  </Badge>
                </span>
              </td>
              <td className="py-2 pr-4 text-xs text-muted-foreground">
                {exec.triggerEventType.replace(/_/g, ' ')}
              </td>
              <td className="py-2 pr-4 text-xs max-w-[200px] truncate" title={exec.resultMessage}>
                {exec.resultMessage}
              </td>
              <td className="py-2 text-xs text-muted-foreground">
                {formatLocalizedDate(exec.executedAt, i18n.language)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function AutomationExecutionLog({
  automationId,
  automationName,
  executions: inlineExecutions,
  onClose,
}: Props) {
  const { t } = useTranslation();
  const [executions, setExecutions] = useState<WorkflowAutomationExecution[]>(inlineExecutions ?? []);
  const [loading, setLoading] = useState(!!automationId);

  useEffect(() => {
    if (!automationId) return;
    automationService.getExecutionLog(automationId, 50)
      .then(r => setExecutions(r.data))
      .finally(() => setLoading(false));
  }, [automationId]);

  // Inline mode (project-level tab)
  if (!automationId) {
    return (
      <div>
        {(inlineExecutions ?? []).length === 0 ? (
          <p className="text-sm text-muted-foreground text-center py-8">{t('automation.noExecutions')}</p>
        ) : (
          <ExecutionTable items={inlineExecutions ?? []} />
        )}
      </div>
    );
  }

  // Sheet mode (single-rule log)
  return (
    <Sheet open onOpenChange={onClose}>
      <SheetContent className="w-full sm:max-w-2xl overflow-y-auto">
        <SheetHeader className="flex flex-row items-center justify-between">
          <SheetTitle>{t('automation.executionLogTitle', { name: automationName })}</SheetTitle>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </SheetHeader>
        <div className="mt-6">
          {loading ? (
            <div className="space-y-2">
              {[1, 2, 3].map(i => <div key={i} className="h-10 rounded bg-muted animate-pulse" />)}
            </div>
          ) : executions.length === 0 ? (
            <p className="text-sm text-muted-foreground text-center py-8">{t('automation.noExecutions')}</p>
          ) : (
            <ExecutionTable items={executions} />
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
