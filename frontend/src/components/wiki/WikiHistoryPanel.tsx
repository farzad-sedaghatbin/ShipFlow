import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { RotateCcw } from "lucide-react";
import { wikiService } from "../../services/wikiService";

interface WikiHistoryPanelProps {
  pageId: number;
}

export default function WikiHistoryPanel({ pageId }: WikiHistoryPanelProps) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();

  const { data, isLoading, isError } = useQuery({
    queryKey: ["wiki-page-history", pageId],
    queryFn: () => wikiService.getPageHistory(pageId).then((r) => r.data),
    enabled: !!pageId,
  });

  const restoreMutation = useMutation({
    mutationFn: (revision: number) =>
      wikiService.restorePage(pageId, revision),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-page", pageId] });
      queryClient.invalidateQueries({ queryKey: ["wiki-page-history", pageId] });
    },
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

  const revisions = data ?? [];

  return (
    <div className="space-y-1">
      <p className="font-semibold text-sm text-foreground mb-2">
        {t("wiki.history")}
      </p>
      {revisions.length === 0 ? (
        <p className="text-sm text-muted-foreground">—</p>
      ) : (
        <ul className="divide-y divide-border rounded border">
          {revisions.map((rev) => (
            <li
              key={rev.revision}
              className="flex items-center justify-between gap-4 px-3 py-2 text-sm"
            >
              <div className="flex flex-col">
                <span className="font-medium">
                  {t("wiki.revision")} #{rev.revision} — {rev.title}
                </span>
                <span className="text-xs text-muted-foreground">
                  {new Date(rev.timestamp).toLocaleString()} · editor #{rev.editorId}
                </span>
              </div>
              <button
                onClick={() => restoreMutation.mutate(rev.revision)}
                disabled={restoreMutation.isPending}
                className="flex items-center gap-1 text-xs px-2 py-1 rounded border border-border hover:bg-muted transition-colors disabled:opacity-50"
              >
                <RotateCcw className="w-3 h-3" />
                {restoreMutation.isPending
                  ? t("wiki.restoring")
                  : t("wiki.restore")}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
