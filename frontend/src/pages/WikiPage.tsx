import { lazy, Suspense, useEffect, useRef, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useTranslation } from "react-i18next";
import {
  Edit2,
  Paperclip,
  ChevronDown,
  ChevronUp,
  FileDown,
  Link as LinkIcon,
  Menu,
} from "lucide-react";
import { Sheet, SheetContent } from "@/components/ui/sheet";
import WikiTree from "../components/wiki/WikiTree";
import WikiBreadcrumbs from "../components/wiki/WikiBreadcrumbs";
import WikiTableOfContents from "../components/wiki/WikiTableOfContents";
import WikiHistoryPanel from "../components/wiki/WikiHistoryPanel";
import WikiAttachmentItem from "../components/wiki/WikiAttachmentItem";
import WikiLinkedReferences from "../components/wiki/WikiLinkedReferences";
import WikiInsertLinkDialog from "../components/wiki/WikiInsertLinkDialog";
import type { WikiEditorHandle } from "../components/wiki/WikiEditor";
import { extractPlainText } from "../components/wiki/wikiTokens";
import Comments from "../components/Comments";
import { useBreadcrumbLabel } from "../contexts";
import { usePresence } from "../hooks/usePresence";
import { PresenceAvatarStack } from "../components/PresenceAvatarStack";
import { ConflictDialog } from "../components/ConflictDialog";
import { getConflictBody, type OptimisticLockConflictBody } from "../utils/conflictError";
import {
  wikiService,
  type WikiPageDTO,
  type WikiPageSearchDTO,
  type WikiTreeNodeDTO,
} from "../services/wikiService";

// BlockNote editor is lazy-loaded so tests can mock it and it doesn't bloat the initial bundle
const WikiEditor = lazy(() => import("../components/wiki/WikiEditor"));

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function WikiPage() {
  const { spaceId, pageId } = useParams<{ spaceId: string; pageId: string }>();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const numSpaceId = Number(spaceId);
  const numPageId = Number(pageId);

  const [editMode, setEditMode] = useState(false);
  const [draftContent, setDraftContent] = useState<string | null>(null);
  const [historyOpen, setHistoryOpen] = useState(false);
  // Add-subpage dialog: null = closed; a number = creating a child of that page.
  const [childParent, setChildParent] = useState<number | null>(null);
  const [childTitle, setChildTitle] = useState("");
  // Insert-page-link picker (edit mode only).
  const [insertLinkOpen, setInsertLinkOpen] = useState(false);
  // Below `lg` the page tree sidebar is hidden (it doesn't fit alongside
  // readable content on a phone/tablet); this drives a Sheet drawer instead.
  const [mobileTreeOpen, setMobileTreeOpen] = useState(false);
  // A newer version was saved by someone else while this user is mid-edit —
  // shown as a non-blocking heads-up; the real reconciliation happens via the
  // 409 conflict dialog when they actually try to save.
  const [wikiStale, setWikiStale] = useState(false);
  // 409 optimistic-lock conflict on the page save.
  const [wikiConflict, setWikiConflict] = useState<OptimisticLockConflictBody<WikiPageDTO> | null>(null);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const editorRef = useRef<WikiEditorHandle>(null);

  // ── Data fetching ──────────────────────────────────────────────────────────

  const { data: space } = useQuery({
    queryKey: ["wiki-space", numSpaceId],
    queryFn: () => wikiService.getSpace(numSpaceId).then((r) => r.data),
    enabled: !!numSpaceId,
  });

  const { data: tree } = useQuery({
    queryKey: ["wiki-tree", numSpaceId],
    queryFn: () => wikiService.getSpaceTree(numSpaceId).then((r) => r.data),
    enabled: !!numSpaceId,
  });

  const {
    data: page,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["wiki-page", numPageId],
    queryFn: () => wikiService.getPage(numPageId).then((r) => r.data),
    enabled: !!numPageId,
  });

  const { data: attachments } = useQuery({
    queryKey: ["wiki-attachments", numPageId],
    queryFn: () => wikiService.getAttachments(numPageId).then((r) => r.data),
    enabled: !!numPageId,
  });

  // Resolve both breadcrumb crumbs by name: the parent space and this page.
  useBreadcrumbLabel(spaceId ? `/wiki/${spaceId}` : undefined, space?.name);
  useBreadcrumbLabel(
    spaceId && pageId ? `/wiki/${spaceId}/${pageId}` : undefined,
    page?.title,
  );

  // Live presence — who else is viewing this page (v1.13.0 S64)
  const { viewers } = usePresence("WIKI_PAGE", page?.id);

  // Live refresh — silently reload when not editing; just flag "stale" when
  // editing so we don't clobber an in-progress edit (mirrors RetroBoard's
  // 'retro-updated' pattern).
  useEffect(() => {
    if (!numPageId) return;
    const handler = (e: Event) => {
      const detail = (e as CustomEvent<{ pageId: number }>).detail;
      if (detail?.pageId !== numPageId) return;
      if (editMode) {
        setWikiStale(true);
      } else {
        queryClient.invalidateQueries({ queryKey: ["wiki-page", numPageId] });
      }
    };
    window.addEventListener("wiki-updated", handler);
    return () => window.removeEventListener("wiki-updated", handler);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [numPageId, editMode]);

  // ── Mutations ──────────────────────────────────────────────────────────────

  const saveMutation = useMutation({
    mutationFn: (vars: { content: string; expectedVersion?: number }) =>
      wikiService.updatePage(numPageId, {
        title: page?.title,
        content: vars.content,
        expectedVersion: vars.expectedVersion,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-page", numPageId] });
      setEditMode(false);
      setDraftContent(null);
      setWikiStale(false);
    },
    onError: (error) => {
      const conflict = getConflictBody<WikiPageDTO>(error);
      if (conflict) {
        setWikiConflict(conflict);
      }
      // Non-conflict errors fall through to the global axios interceptor's toast.
    },
  });

  // Conflict dialog resolution: overwrite the server with our local edit,
  // retrying with the version the server told us is current.
  function handleKeepMyEdits() {
    if (draftContent === null || !wikiConflict) return;
    saveMutation.mutate(
      { content: draftContent, expectedVersion: wikiConflict.currentVersion },
      { onSuccess: () => setWikiConflict(null) }
    );
  }

  // Conflict dialog resolution: discard our local edit and adopt the server's
  // current version — the 409 body's `current` payload is already the fresh
  // WikiPageDTO, so no extra fetch is needed.
  function handleDiscardMyEdits() {
    if (!wikiConflict) return;
    queryClient.setQueryData(["wiki-page", numPageId], wikiConflict.current);
    setEditMode(false);
    setDraftContent(null);
    setWikiStale(false);
    setWikiConflict(null);
  }

  const uploadMutation = useMutation({
    mutationFn: (file: File) => wikiService.uploadAttachment(numPageId, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-attachments", numPageId] });
    },
  });

  const deleteAttachmentMutation = useMutation({
    mutationFn: (attId: number) => wikiService.deleteAttachment(attId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-attachments", numPageId] });
    },
  });

  const createChildMutation = useMutation({
    mutationFn: (title: string) =>
      wikiService.createPage({
        spaceId: numSpaceId,
        title,
        parentId: childParent ?? undefined,
      }),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ["wiki-tree", numSpaceId] });
      setChildParent(null);
      setChildTitle("");
      navigate(`/wiki/${numSpaceId}/${res.data.id}`);
    },
  });

  // ── Handlers ───────────────────────────────────────────────────────────────

  function handleSave() {
    if (draftContent !== null) {
      saveMutation.mutate({ content: draftContent, expectedVersion: page?.version });
    } else {
      setEditMode(false);
    }
  }

  function handleExportPdf() {
    // Render the page content into an isolated window and print THAT, rather than
    // toggling visibility on the live app (which is fragile: app scroll/overflow
    // containers can clip the print area, leaving an empty preview). This builds
    // a clean standalone document with the app's styles inlined, so the print →
    // Save as PDF preview always shows the real content with selectable text.
    const src = document.getElementById("wiki-print-area");
    if (!src) {
      window.print();
      return;
    }

    // Clone the content, drop anything marked no-print, and force the editor to
    // a light color scheme so the PDF is readable regardless of the app theme.
    const clone = src.cloneNode(true) as HTMLElement;
    clone.querySelectorAll(".no-print").forEach((el) => el.remove());
    clone.querySelectorAll(".bn-container").forEach((el) => {
      el.classList.remove("dark");
      el.classList.add("light");
      el.setAttribute("data-color-scheme", "light");
    });

    // Inline all same-origin stylesheet rules (Tailwind + BlockNote) so the
    // isolated window renders with the same styling.
    let css = "";
    for (const sheet of Array.from(document.styleSheets)) {
      try {
        for (const rule of Array.from(sheet.cssRules)) css += rule.cssText + "\n";
      } catch {
        // Cross-origin stylesheet (e.g. fonts) — not readable, skip it.
      }
    }

    const title = (page?.title ?? "Wiki").replace(/[<>&]/g, (c) =>
      c === "<" ? "&lt;" : c === ">" ? "&gt;" : "&amp;"
    );
    const win = window.open("", "_blank", "width=900,height=1200");
    if (!win) {
      // Popup blocked — fall back to printing the page in place.
      window.print();
      return;
    }
    win.document.open();
    win.document.write(
      `<!doctype html><html><head><meta charset="utf-8"><title>${title}</title>` +
        `<style>${css}\n` +
        `html,body{background:#fff;color:#111;margin:0;padding:32px;}` +
        `*{color:#111 !important;background-color:transparent !important;box-shadow:none !important;}` +
        `body{background:#fff !important;}` +
        // The inlined app CSS carries the wiki @media print rule that hides
        // everything except #wiki-print-area (which doesn't exist here); undo it
        // so the isolated document is fully visible when printed.
        `@media print{body,body *{visibility:visible !important;}}` +
        `@page{margin:0.75in;}</style></head>` +
        `<body>${clone.innerHTML}</body></html>`
    );
    win.document.close();
    win.focus();
    // Give the cloned content + fonts a moment to lay out, then print.
    setTimeout(() => {
      win.print();
    }, 400);
  }

  function handleInsertPageLink(target: WikiPageSearchDTO) {
    // Insert the `[[pageId]]` token at the cursor; the backend resolves it to a
    // WikiPageLinkDTO on save and the view renders it under "Linked pages".
    editorRef.current?.insertText(`[[${target.id}]]`);
    setInsertLinkOpen(false);
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) uploadMutation.mutate(file);
    // Reset so same file can be re-selected
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  // ── Ancestry computation (flat walk from tree) ─────────────────────────────

  function findAncestors(): WikiPageDTO[] {
    if (!tree || !numPageId) return [];

    // Walk the tree depth-first; collect the path from root to the target node.
    // WikiTreeNodeDTO carries id + title — enough to build breadcrumb links.
    // We cast to WikiPageDTO for the ancestors prop type (only id + title used).
    function walk(
      nodes: WikiTreeNodeDTO[],
      targetId: number,
      path: WikiTreeNodeDTO[]
    ): WikiTreeNodeDTO[] | null {
      for (const node of nodes) {
        const current = [...path, node];
        if (node.id === targetId) return current;
        if (node.children.length > 0) {
          const found = walk(node.children, targetId, current);
          if (found) return found;
        }
      }
      return null;
    }

    const fullPath = walk(tree, numPageId, []);
    if (!fullPath || fullPath.length <= 1) return [];
    // Drop the last element (current page itself); return the ancestor chain.
    return fullPath.slice(0, -1).map((n) => ({
      id: n.id,
      title: n.title,
      slug: n.slug,
      spaceId: numSpaceId,
      parentId: null,
      content: null,
      contentText: null,
      position: n.position,
      createdBy: 0,
      createdAt: "",
      updatedAt: "",
      pageLinks: [],
    }));
  }

  // ── Render ─────────────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="p-8 text-sm text-muted-foreground animate-pulse">
        Loading…
      </div>
    );
  }

  if (isError || !page) {
    return <div className="p-8 text-sm text-destructive">Page not found.</div>;
  }

  const displayContent = draftContent ?? page.content;

  return (
    <div className="flex h-full">
      {/* Left: tree sidebar — hidden below `lg` (a fixed 256px column leaves
          almost no room for content on a phone/tablet); a Sheet drawer below
          gives mobile users the same navigation via a menu button instead. */}
      {space && (
        <aside className="hidden lg:block w-64 shrink-0 border-r border-border p-4 overflow-y-auto">
          <h2 className="font-semibold text-sm truncate mb-3">{space.name}</h2>
          <WikiTree
            spaceId={numSpaceId}
            nodes={tree ?? []}
            currentPageId={numPageId}
            onAddChild={(pid) => {
              setChildParent(pid);
              setChildTitle("");
            }}
          />
        </aside>
      )}

      {/* Mobile page-tree drawer (< lg only) */}
      {space && (
        <Sheet open={mobileTreeOpen} onOpenChange={setMobileTreeOpen}>
          <SheetContent side="start" className="w-72 p-4 overflow-y-auto">
            <h2 className="font-semibold text-sm truncate mb-3">{space.name}</h2>
            <WikiTree
              spaceId={numSpaceId}
              nodes={tree ?? []}
              currentPageId={numPageId}
              onAddChild={(pid) => {
                setChildParent(pid);
                setChildTitle("");
                setMobileTreeOpen(false);
              }}
            />
          </SheetContent>
        </Sheet>
      )}

      {/* Main content area */}
      <div className="flex-1 flex overflow-hidden">
        <main className="flex-1 overflow-y-auto">
          <div id="wiki-print-area" className="max-w-3xl mx-auto py-8 px-6 space-y-6">
            {/* Breadcrumbs */}
            {space && (
              <div className="no-print flex items-center gap-2">
                <button
                  onClick={() => setMobileTreeOpen(true)}
                  className="lg:hidden flex items-center justify-center h-8 w-8 shrink-0 rounded-md border border-input hover:bg-muted transition-colors"
                  aria-label={t("wiki.pageTree")}
                >
                  <Menu className="w-4 h-4" />
                </button>
                <WikiBreadcrumbs
                  space={space}
                  ancestors={findAncestors()}
                  currentTitle={page.title}
                />
              </div>
            )}

            {/* Title + toolbar — stacks on mobile; the toolbar row also wraps
                so its buttons drop to a second line instead of overflowing
                (up to 3 buttons, e.g. "Insert Page Link" / "Cancel" / "Save"
                in edit mode, comfortably exceeds a ~310px content width once
                the page-tree drawer trigger and padding are accounted for). */}
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div className="flex items-center gap-3">
                <h1 className="text-3xl font-bold leading-tight">{page.title}</h1>
                <PresenceAvatarStack viewers={viewers} />
              </div>
              <div className="flex flex-wrap items-center gap-2 sm:shrink-0 no-print">
                {editMode ? (
                  <>
                    <button
                      onClick={() => setInsertLinkOpen(true)}
                      className="flex items-center gap-1 px-3 py-1.5 text-sm rounded-md border border-input hover:bg-muted transition-colors"
                    >
                      <LinkIcon className="w-3.5 h-3.5" />
                      {t("wiki.insertPageLink")}
                    </button>
                    <button
                      onClick={() => {
                        setEditMode(false);
                        setDraftContent(null);
                      }}
                      className="flex items-center gap-1 px-3 py-1.5 text-sm rounded-md border border-input hover:bg-muted transition-colors"
                    >
                      {t("wiki.cancel")}
                    </button>
                    <button
                      onClick={handleSave}
                      disabled={saveMutation.isPending}
                      className="flex items-center gap-1 px-3 py-1.5 text-sm rounded-md bg-primary text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
                    >
                      {saveMutation.isPending ? t("wiki.saving") : t("wiki.save")}
                    </button>
                  </>
                ) : (
                  <>
                    <button
                      onClick={() => {
                        setEditMode(true);
                        setWikiStale(false);
                      }}
                      className="flex items-center gap-1 px-3 py-1.5 text-sm rounded-md border border-input hover:bg-muted transition-colors"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                      {t("wiki.edit")}
                    </button>
                    <button
                      onClick={handleExportPdf}
                      className="flex items-center gap-1 px-3 py-1.5 text-sm rounded-md border border-input hover:bg-muted transition-colors"
                    >
                      <FileDown className="w-3.5 h-3.5" />
                      {t("wiki.exportPdf")}
                    </button>
                  </>
                )}
              </div>
            </div>

            {/* Last edited */}
            <p className="text-xs text-muted-foreground">
              {t("wiki.lastEdited")}: {new Date(page.updatedAt).toLocaleString()}
            </p>

            {editMode && wikiStale && (
              <div className="no-print flex items-center justify-between gap-2 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200">
                <span>{t("wiki.staleWhileEditing")}</span>
                <button
                  type="button"
                  onClick={() => {
                    queryClient.invalidateQueries({ queryKey: ["wiki-page", numPageId] });
                    setEditMode(false);
                    setDraftContent(null);
                    setWikiStale(false);
                  }}
                  className="shrink-0 underline hover:no-underline"
                >
                  {t("wiki.reloadNow")}
                </button>
              </div>
            )}

            {/* Editor / viewer */}
            <div className="wiki-content rounded-lg border border-border min-h-[300px] overflow-hidden">
              {editMode ? (
                <Suspense
                  fallback={
                    <div className="p-4 text-sm text-muted-foreground animate-pulse">
                      Loading editor…
                    </div>
                  }
                >
                  <WikiEditor
                    ref={editorRef}
                    initialContent={page.content}
                    editable={true}
                    onChange={setDraftContent}
                  />
                </Suspense>
              ) : page.content ? (
                <Suspense
                  fallback={
                    <div className="p-4 text-sm text-muted-foreground animate-pulse">
                      Loading…
                    </div>
                  }
                >
                  <WikiEditor
                    initialContent={page.content}
                    editable={false}
                    onChange={() => undefined}
                  />
                </Suspense>
              ) : (
                <p className="p-4 text-sm text-muted-foreground italic">
                  {t("wiki.noContent")}
                </p>
              )}
            </div>

            {/* Linked pages + mentions (view mode) */}
            {!editMode && (
              <WikiLinkedReferences
                page={page}
                plainText={extractPlainText(page.content)}
              />
            )}

            {/* Attachments */}
            <section className="space-y-3 no-print">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold flex items-center gap-2">
                  <Paperclip className="w-4 h-4" />
                  {t("wiki.attachments")}
                </h3>
                <button
                  onClick={() => fileInputRef.current?.click()}
                  disabled={uploadMutation.isPending}
                  className="flex items-center gap-1 text-xs px-2 py-1 rounded border border-input hover:bg-muted transition-colors disabled:opacity-50"
                >
                  {t("wiki.uploadAttachment")}
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  className="hidden"
                  onChange={handleFileChange}
                />
              </div>
              {uploadMutation.isError && (
                <p className="text-sm text-destructive">
                  {t("wiki.uploadFailed")}
                </p>
              )}
              {attachments && attachments.length === 0 && (
                <p className="text-sm text-muted-foreground">
                  {t("wiki.noAttachments")}
                </p>
              )}
              {attachments && attachments.length > 0 && (
                <ul className="divide-y divide-border rounded border overflow-hidden">
                  {attachments.map((att) => (
                    <WikiAttachmentItem
                      key={att.id}
                      attachment={att}
                      onDelete={(id) => deleteAttachmentMutation.mutate(id)}
                      deleting={deleteAttachmentMutation.isPending}
                    />
                  ))}
                </ul>
              )}
            </section>

            {/* Comments */}
            <section className="no-print">
              <Comments entityType="wiki" entityId={numPageId} />
            </section>

            {/* History (collapsible) */}
            <section className="border-t border-border pt-4 space-y-3 no-print">
              <button
                onClick={() => setHistoryOpen((o) => !o)}
                className="flex items-center gap-2 text-sm font-semibold text-muted-foreground hover:text-foreground transition-colors"
              >
                {historyOpen ? (
                  <ChevronUp className="w-4 h-4" />
                ) : (
                  <ChevronDown className="w-4 h-4" />
                )}
                {t("wiki.history")}
              </button>
              {historyOpen && <WikiHistoryPanel pageId={numPageId} />}
            </section>
          </div>
        </main>

        {/* Right: table of contents */}
        <aside className="w-52 shrink-0 p-4 border-l border-border overflow-y-auto hidden lg:block">
          <WikiTableOfContents content={displayContent} />
        </aside>
      </div>

      {/* Insert page link picker */}
      <WikiInsertLinkDialog
        spaceId={numSpaceId}
        open={insertLinkOpen}
        onClose={() => setInsertLinkOpen(false)}
        onInsert={handleInsertPageLink}
      />

      {/* Conflict Dialog — someone else updated this page first (409) */}
      <ConflictDialog
        open={!!wikiConflict}
        onOpenChange={(open) => { if (!open) setWikiConflict(null); }}
        entityLabel={t("conflictDialog.entityLabel.wikiPage")}
        onKeepMine={handleKeepMyEdits}
        onDiscardMine={handleDiscardMyEdits}
      />

      {/* Add Subpage dialog */}
      {childParent !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-background rounded-xl shadow-xl w-full max-w-md p-6 space-y-4">
            <h2 className="text-lg font-semibold">{t("wiki.addSubpage")}</h2>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (childTitle.trim()) createChildMutation.mutate(childTitle.trim());
              }}
              className="space-y-3"
            >
              <div>
                <label className="block text-sm font-medium mb-1">
                  {t("wiki.pageTitle")} *
                </label>
                <input
                  value={childTitle}
                  onChange={(e) => setChildTitle(e.target.value)}
                  autoFocus
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                  placeholder="Getting Started"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => {
                    setChildParent(null);
                    setChildTitle("");
                  }}
                  className="px-4 py-2 text-sm rounded-md border border-input hover:bg-muted transition-colors"
                >
                  {t("wiki.cancel")}
                </button>
                <button
                  type="submit"
                  disabled={!childTitle.trim() || createChildMutation.isPending}
                  className="px-4 py-2 text-sm rounded-md bg-primary text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
                >
                  {createChildMutation.isPending
                    ? t("wiki.saving")
                    : t("wiki.addSubpage")}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
