import { useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import { RotateCcw, Loader2, ArrowRight } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogDescription,
} from "../ui/dialog";
import { Button } from "../ui/button";
import { wikiService, type WikiRevisionDTO } from "../../services/wikiService";
import { blocksToLines, diffLines, hasNoChanges } from "./wikiDiff";
import { cn } from "../../lib/utils";

interface WikiCompareRestoreDialogProps {
  pageId: number;
  /** The revision the user wants to compare against / restore to. null = closed. */
  revision: WikiRevisionDTO | null;
  onClose: () => void;
  /** Called after a successful restore so the parent can refresh the page view. */
  onRestored?: () => void;
}

/**
 * Side-by-side compare view between the current page and a historical revision,
 * with changed/added/removed lines highlighted, plus a confirm-to-restore action.
 */
export default function WikiCompareRestoreDialog({
  pageId,
  revision,
  onClose,
  onRestored,
}: WikiCompareRestoreDialogProps) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const open = revision !== null;

  // Current page content — shares the cache key with WikiPage, so this is
  // usually served from cache without an extra request.
  const { data: current } = useQuery({
    queryKey: ["wiki-page", pageId],
    queryFn: () => wikiService.getPage(pageId).then((r) => r.data),
    enabled: open,
  });

  const { data: revisionPage, isLoading: revisionLoading } = useQuery({
    queryKey: ["wiki-page-revision", pageId, revision?.revision],
    queryFn: () =>
      wikiService.getPageRevision(pageId, revision!.revision).then((r) => r.data),
    enabled: open && revision !== null,
  });

  const restoreMutation = useMutation({
    mutationFn: () => wikiService.restorePage(pageId, revision!.revision),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-page", pageId] });
      queryClient.invalidateQueries({ queryKey: ["wiki-page-history", pageId] });
      onRestored?.();
      onClose();
    },
  });

  const rows = useMemo(() => {
    if (!current || !revisionPage) return [];
    return diffLines(
      blocksToLines(current.content),
      blocksToLines(revisionPage.content)
    );
  }, [current, revisionPage]);

  const titleChanged =
    !!current && !!revisionPage && current.title !== revisionPage.title;
  const noChanges = !revisionLoading && rows.length > 0 && hasNoChanges(rows) && !titleChanged;

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-5xl max-h-[88vh] flex flex-col">
        <DialogHeader>
          <DialogTitle>{t("wiki.compareTitle")}</DialogTitle>
          <DialogDescription>
            {revision
              ? t("wiki.compareSubtitle", { revision: revision.revision })
              : ""}
          </DialogDescription>
        </DialogHeader>

        {/* Column headers */}
        <div className="grid grid-cols-2 gap-px text-xs font-semibold border border-border rounded-t-md overflow-hidden">
          <div className="bg-muted px-3 py-2">
            {t("wiki.currentVersion")}
            {current && (
              <span className="ml-1 font-normal text-muted-foreground">
                · {new Date(current.updatedAt).toLocaleString()}
              </span>
            )}
          </div>
          <div className="bg-muted px-3 py-2 flex items-center gap-1">
            {t("wiki.revision")} #{revision?.revision}
            {revision && (
              <span className="font-normal text-muted-foreground">
                · {new Date(revision.timestamp).toLocaleString()} · editor #
                {revision.editorId}
              </span>
            )}
          </div>
        </div>

        {/* Title diff (if the title changed between versions) */}
        {titleChanged && current && revisionPage && (
          <div className="grid grid-cols-2 gap-px border-x border-border text-sm">
            <div className="bg-red-500/10 px-3 py-2 line-through decoration-red-500/60">
              {current.title}
            </div>
            <div className="bg-green-500/10 px-3 py-2">{revisionPage.title}</div>
          </div>
        )}

        {/* Diff body */}
        <div className="flex-1 overflow-y-auto border border-border rounded-b-md">
          {revisionLoading ? (
            <div className="flex items-center justify-center py-12 text-muted-foreground">
              <Loader2 className="h-5 w-5 animate-spin mr-2" />
              {t("wiki.loadingRevision")}
            </div>
          ) : noChanges ? (
            <div className="px-3 py-8 text-center text-sm text-muted-foreground">
              {t("wiki.noTextChanges")}
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-px font-mono text-xs">
              {rows.map((row, i) => (
                <DiffCells key={i} left={row.left} right={row.right} status={row.status} />
              ))}
            </div>
          )}
        </div>

        {/* Legend */}
        <div className="flex items-center gap-4 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <span className="inline-block w-3 h-3 rounded-sm bg-green-500/30" />
            {t("wiki.legendAdded")}
          </span>
          <span className="flex items-center gap-1">
            <span className="inline-block w-3 h-3 rounded-sm bg-red-500/30" />
            {t("wiki.legendRemoved")}
          </span>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={restoreMutation.isPending}>
            {t("wiki.cancel")}
          </Button>
          <Button
            onClick={() => restoreMutation.mutate()}
            disabled={restoreMutation.isPending || revisionLoading}
            className="gap-1"
          >
            {restoreMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <RotateCcw className="h-4 w-4" />
            )}
            {restoreMutation.isPending
              ? t("wiki.restoring")
              : t("wiki.restoreThisVersion")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// A single aligned diff row rendered as two cells (current | revision).
function DiffCells({
  left,
  right,
  status,
}: {
  left: string | null;
  right: string | null;
  status: "same" | "changed" | "added" | "removed";
}) {
  const leftHighlighted = status === "removed" || status === "changed";
  const rightHighlighted = status === "added" || status === "changed";
  return (
    <>
      <div
        className={cn(
          "px-3 py-1 whitespace-pre-wrap break-words border-b border-border/50",
          leftHighlighted && "bg-red-500/10",
          left === null && "bg-muted/30"
        )}
      >
        {left}
      </div>
      <div
        className={cn(
          "px-3 py-1 whitespace-pre-wrap break-words border-b border-border/50 flex gap-1",
          rightHighlighted && "bg-green-500/10",
          right === null && "bg-muted/30"
        )}
      >
        {status === "changed" && (
          <ArrowRight className="h-3 w-3 mt-0.5 shrink-0 text-muted-foreground" />
        )}
        <span>{right}</span>
      </div>
    </>
  );
}
