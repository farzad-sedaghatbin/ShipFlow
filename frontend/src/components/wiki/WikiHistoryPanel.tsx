import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { GitCompareArrows } from "lucide-react";
import { wikiService, type WikiRevisionDTO } from "../../services/wikiService";
import WikiCompareRestoreDialog from "./WikiCompareRestoreDialog";

interface WikiHistoryPanelProps {
  pageId: number;
}

export default function WikiHistoryPanel({ pageId }: WikiHistoryPanelProps) {
  const { t } = useTranslation();
  const [comparing, setComparing] = useState<WikiRevisionDTO | null>(null);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["wiki-page-history", pageId],
    queryFn: () => wikiService.getPageHistory(pageId).then((r) => r.data),
    enabled: !!pageId,
  });

  if (isLoading) {
    return (
      <div className="text-sm text-muted-foreground px-2 py-2 animate-pulse">
        {t("wiki.history")}…
      </div>
    );
  }

  if (isError) {
    return (
      <div className="text-sm text-destructive px-2 py-2">
        {t("wiki.loadHistoryError")}
      </div>
    );
  }

  // Newest first; the highest revision number is the current page content, so it
  // is shown as "Current" with no compare action (comparing it to itself is a no-op).
  const revisions = [...(data ?? [])].sort((a, b) => b.revision - a.revision);
  const latestRevision = revisions.length > 0 ? revisions[0].revision : null;

  return (
    <div className="space-y-1">
      <p className="font-semibold text-sm text-foreground mb-2">
        {t("wiki.history")}
      </p>
      {revisions.length === 0 ? (
        <p className="text-sm text-muted-foreground">—</p>
      ) : (
        <ul className="divide-y divide-border rounded border">
          {revisions.map((rev) => {
            const isCurrent = rev.revision === latestRevision;
            return (
              <li
                key={rev.revision}
                className="flex items-center justify-between gap-4 px-3 py-2 text-sm"
              >
                <div className="flex flex-col">
                  <span className="font-medium flex items-center gap-2">
                    {t("wiki.revision")} #{rev.revision} — {rev.title}
                    {isCurrent && (
                      <span className="text-[10px] uppercase tracking-wide font-semibold px-1.5 py-0.5 rounded bg-primary/10 text-primary">
                        {t("wiki.currentBadge")}
                      </span>
                    )}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {new Date(rev.timestamp).toLocaleString()} · editor #{rev.editorId}
                  </span>
                </div>
                {isCurrent ? (
                  <span className="text-xs text-muted-foreground px-2 py-1">
                    {t("wiki.currentVersionLabel")}
                  </span>
                ) : (
                  <button
                    onClick={() => setComparing(rev)}
                    className="flex items-center gap-1 text-xs px-2 py-1 rounded border border-border hover:bg-muted transition-colors"
                  >
                    <GitCompareArrows className="w-3 h-3" />
                    {t("wiki.compareRestore")}
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      )}

      <WikiCompareRestoreDialog
        pageId={pageId}
        revision={comparing}
        onClose={() => setComparing(null)}
      />
    </div>
  );
}
