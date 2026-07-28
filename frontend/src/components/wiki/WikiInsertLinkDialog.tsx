import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Search, Link as LinkIcon, Loader2 } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import {
  wikiService,
  type WikiPageSearchDTO,
} from "../../services/wikiService";

/**
 * Page picker for inserting an internal `[[pageId]]` link token into the wiki
 * editor. Searches pages within the current space and calls `onInsert(pageId)`
 * with the chosen page; the parent inserts the actual token text.
 */
interface WikiInsertLinkDialogProps {
  spaceId: number;
  open: boolean;
  onClose: () => void;
  onInsert: (page: WikiPageSearchDTO) => void;
}

export default function WikiInsertLinkDialog({
  spaceId,
  open,
  onClose,
  onInsert,
}: WikiInsertLinkDialogProps) {
  const { t } = useTranslation();
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<WikiPageSearchDTO[]>([]);
  const [loading, setLoading] = useState(false);

  // Reset state each time the dialog opens.
  useEffect(() => {
    if (open) {
      setQuery("");
      setResults([]);
    }
  }, [open]);

  // Debounced search scoped to the current space.
  useEffect(() => {
    if (!open) return;
    let cancelled = false;
    const handle = setTimeout(async () => {
      setLoading(true);
      try {
        const res = await wikiService.searchPages(query, spaceId);
        if (!cancelled) setResults(res.data);
      } catch {
        if (!cancelled) setResults([]);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }, 200);
    return () => {
      cancelled = true;
      clearTimeout(handle);
    };
  }, [query, spaceId, open]);

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <LinkIcon className="w-4 h-4" />
            {t("wiki.insertPageLink")}
          </DialogTitle>
        </DialogHeader>
        <p className="text-xs text-muted-foreground">
          {t("wiki.insertPageLinkHint")}
        </p>
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            autoFocus
            placeholder={t("wiki.searchPlaceholder")}
            className="w-full rounded-md border border-input bg-background pl-9 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>

        <div className="max-h-72 overflow-y-auto rounded-md border border-border divide-y divide-border">
          {loading && (
            <div className="flex items-center justify-center py-6 text-muted-foreground">
              <Loader2 className="w-4 h-4 animate-spin" />
            </div>
          )}
          {!loading && results.length === 0 && (
            <p className="px-3 py-6 text-center text-sm text-muted-foreground">
              {t("wiki.noResults")}
            </p>
          )}
          {!loading &&
            results.map((page) => (
              <button
                key={page.id}
                type="button"
                onClick={() => onInsert(page)}
                className="w-full text-left px-3 py-2 hover:bg-muted transition-colors"
              >
                <span className="text-sm font-medium block truncate">
                  {page.title}
                </span>
                {page.excerpt && (
                  <span className="text-xs text-muted-foreground block truncate">
                    {page.excerpt}
                  </span>
                )}
              </button>
            ))}
        </div>

        <div className="flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm rounded-md border border-input hover:bg-muted transition-colors"
          >
            {t("wiki.cancel")}
          </button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
