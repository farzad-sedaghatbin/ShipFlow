import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useTranslation } from "react-i18next";
import { Plus, FileText } from "lucide-react";
import WikiTree from "../components/wiki/WikiTree";
import { useBreadcrumbLabel } from "../contexts";
import {
  wikiService,
  type CreateWikiPageRequest,
} from "../services/wikiService";

// ─── Validation schema ────────────────────────────────────────────────────────

const createPageSchema = z.object({
  title: z.string().min(1, "Title is required"),
});

type CreatePageForm = z.infer<typeof createPageSchema>;

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function WikiSpace() {
  const { spaceId } = useParams<{ spaceId: string }>();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  // null = create a top-level page; a number = create a child of that page.
  const [parentId, setParentId] = useState<number | null>(null);

  const numSpaceId = Number(spaceId);

  function openCreateDialog(parent: number | null) {
    setParentId(parent);
    setDialogOpen(true);
  }

  function closeCreateDialog() {
    setDialogOpen(false);
    setParentId(null);
    reset();
  }

  const { data: space, isLoading: spaceLoading } = useQuery({
    queryKey: ["wiki-space", numSpaceId],
    queryFn: () => wikiService.getSpace(numSpaceId).then((r) => r.data),
    enabled: !!numSpaceId,
  });

  // Show the space name (not "#N") in the global breadcrumb.
  useBreadcrumbLabel(spaceId ? `/wiki/${spaceId}` : undefined, space?.name);

  const { data: tree, isLoading: treeLoading } = useQuery({
    queryKey: ["wiki-tree", numSpaceId],
    queryFn: () => wikiService.getSpaceTree(numSpaceId).then((r) => r.data),
    enabled: !!numSpaceId,
  });

  const createPageMutation = useMutation({
    mutationFn: (req: CreateWikiPageRequest) => wikiService.createPage(req),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ["wiki-tree", numSpaceId] });
      setDialogOpen(false);
      setParentId(null);
      reset();
      navigate(`/wiki/${numSpaceId}/${res.data.id}`);
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CreatePageForm>({
    resolver: zodResolver(createPageSchema),
  });

  function onSubmit(data: CreatePageForm) {
    createPageMutation.mutate({
      spaceId: numSpaceId,
      title: data.title,
      parentId: parentId ?? undefined,
    });
  }

  if (spaceLoading) {
    return (
      <div className="p-8 text-sm text-muted-foreground animate-pulse">
        Loading…
      </div>
    );
  }

  if (!space) {
    return <div className="p-8 text-sm text-destructive">Space not found.</div>;
  }

  return (
    <div className="flex h-full">
      {/* Left: tree sidebar */}
      <aside className="w-64 shrink-0 border-r border-border p-4 space-y-3 overflow-y-auto">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-sm truncate">{space.name}</h2>
          <button
            onClick={() => openCreateDialog(null)}
            className="text-muted-foreground hover:text-primary transition-colors"
            aria-label={t("wiki.createPage")}
          >
            <Plus className="w-4 h-4" />
          </button>
        </div>
        {treeLoading ? (
          <div className="text-xs text-muted-foreground animate-pulse">
            {t("wiki.pages")}…
          </div>
        ) : (
          <WikiTree
            spaceId={numSpaceId}
            nodes={tree ?? []}
            onAddChild={(pid) => openCreateDialog(pid)}
          />
        )}
      </aside>

      {/* Right: page list */}
      <main className="flex-1 p-8 overflow-y-auto">
        <div className="max-w-2xl mx-auto space-y-6">
          <div className="flex items-center justify-between">
            <h1 className="text-2xl font-semibold">{space.name}</h1>
            <button
              onClick={() => openCreateDialog(null)}
              className="flex items-center gap-2 px-4 py-2 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition-colors"
            >
              <Plus className="w-4 h-4" />
              {t("wiki.createPage")}
            </button>
          </div>

          <div className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
            {t("wiki.pages")}
          </div>

          {tree && tree.length === 0 && (
            <p className="text-sm text-muted-foreground">{t("wiki.noPages")}</p>
          )}
          {tree && tree.length > 0 && (
            <ul className="divide-y divide-border rounded-lg border overflow-hidden">
              {tree.map((node) => (
                <li key={node.id}>
                  <button
                    onClick={() => navigate(`/wiki/${numSpaceId}/${node.id}`)}
                    className="flex items-center gap-3 w-full text-left px-4 py-3 hover:bg-muted/50 transition-colors"
                  >
                    <FileText className="w-4 h-4 text-muted-foreground flex-none" />
                    <span className="font-medium">{node.title}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </main>

      {/* Create Page Dialog */}
      {dialogOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <div className="bg-background rounded-xl shadow-xl w-full max-w-md p-6 space-y-4">
            <h2 className="text-lg font-semibold">
              {parentId ? t("wiki.addSubpage") : t("wiki.createPage")}
            </h2>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
              <div>
                <label className="block text-sm font-medium mb-1">
                  {t("wiki.pageTitle")} *
                </label>
                <input
                  {...register("title")}
                  autoFocus
                  className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                  placeholder="Getting Started"
                />
                {errors.title && (
                  <p className="text-xs text-destructive mt-1">
                    {errors.title.message}
                  </p>
                )}
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={closeCreateDialog}
                  className="px-4 py-2 text-sm rounded-md border border-input hover:bg-muted transition-colors"
                >
                  {t("wiki.cancel")}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || createPageMutation.isPending}
                  className="px-4 py-2 text-sm rounded-md bg-primary text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
                >
                  {createPageMutation.isPending
                    ? t("wiki.saving")
                    : t("wiki.createPage")}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
