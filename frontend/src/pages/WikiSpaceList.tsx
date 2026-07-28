import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useTranslation } from "react-i18next";
import { BookOpenText, Plus, Trash2 } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import {
  wikiService,
  type CreateWikiSpaceRequest,
} from "../services/wikiService";

// ─── Validation schema ────────────────────────────────────────────────────────

const createSpaceSchema = z.object({
  name: z.string().min(1, "Name is required"),
  spaceKey: z
    .string()
    .min(1, "Space key is required")
    .max(10, "Max 10 characters")
    .regex(/^[A-Z0-9]+$/, "Uppercase alphanumeric only (e.g. ENG, PROD)"),
  description: z.string().optional(),
});

type CreateSpaceForm = z.infer<typeof createSpaceSchema>;

// Derive a valid space key (uppercase alphanumeric, max 10) from a space name.
export function deriveSpaceKey(name: string): string {
  return (name || "")
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "")
    .slice(0, 10);
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function WikiSpaceList() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<number | null>(null);

  const {
    data: spaces,
    isLoading,
    isError,
  } = useQuery({
    queryKey: ["wiki-spaces"],
    queryFn: () => wikiService.getSpaces().then((r) => r.data),
  });

  const createMutation = useMutation({
    mutationFn: (req: CreateWikiSpaceRequest) => wikiService.createSpace(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-spaces"] });
      setDialogOpen(false);
      setKeyEdited(false);
      reset();
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => wikiService.deleteSpace(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-spaces"] });
      setDeleteTarget(null);
    },
  });

  const {
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<CreateSpaceForm>({
    resolver: zodResolver(createSpaceSchema),
  });

  // Auto-derive the space key from the name until the user edits the key manually.
  const [keyEdited, setKeyEdited] = useState(false);
  const nameValue = watch("name");
  useEffect(() => {
    if (!keyEdited) {
      setValue("spaceKey", deriveSpaceKey(nameValue ?? ""), {
        shouldValidate: false,
      });
    }
  }, [nameValue, keyEdited, setValue]);

  const spaceKeyField = register("spaceKey");

  function closeDialog() {
    setDialogOpen(false);
    setKeyEdited(false);
    reset();
  }

  function onSubmit(data: CreateSpaceForm) {
    createMutation.mutate(data);
  }

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <BookOpenText className="w-6 h-6 text-muted-foreground" />
          <div>
            <h1 className="text-2xl font-semibold">{t("wiki.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("wiki.subtitle")}</p>
          </div>
        </div>
        <button
          onClick={() => setDialogOpen(true)}
          className="flex items-center gap-2 px-4 py-2 rounded-md bg-primary text-primary-foreground text-sm font-medium hover:bg-primary/90 transition-colors"
        >
          <Plus className="w-4 h-4" />
          {t("wiki.createSpace")}
        </button>
      </div>

      {/* List */}
      {isLoading && (
        <div className="text-sm text-muted-foreground animate-pulse py-4">
          {t("wiki.spaces")}…
        </div>
      )}
      {isError && (
        <div className="text-sm text-destructive py-4">
          {t("wiki.loadSpacesError")}
        </div>
      )}
      {!isLoading && !isError && spaces && spaces.length === 0 && (
        <p className="text-sm text-muted-foreground">{t("wiki.noSpaces")}</p>
      )}
      {spaces && spaces.length > 0 && (
        <ul className="divide-y divide-border rounded-lg border overflow-hidden">
          {spaces.map((space) => (
            <li
              key={space.id}
              className="flex items-center justify-between gap-4 px-4 py-3 hover:bg-muted/50 transition-colors"
            >
              <button
                onClick={() => navigate(`/wiki/${space.id}`)}
                className="flex items-start gap-3 flex-1 text-left"
              >
                <div className="flex-none w-10 h-10 rounded-md bg-primary/10 flex items-center justify-center text-primary font-bold text-sm">
                  {space.spaceKey.slice(0, 2)}
                </div>
                <div>
                  <p className="font-medium">{space.name}</p>
                  <p className="text-xs text-muted-foreground">{space.spaceKey}</p>
                </div>
              </button>
              <button
                onClick={() => setDeleteTarget(space.id)}
                className="text-muted-foreground hover:text-destructive transition-colors"
                aria-label={t("wiki.delete")}
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </li>
          ))}
        </ul>
      )}

      {/* Create Space Dialog — shared Dialog component, so it inherits the
          app-wide mobile fullscreen fallback instead of the fixed max-w-md
          card the hand-rolled overlay used to render flush against the
          screen edges on narrow viewports. */}
      <Dialog open={dialogOpen} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t("wiki.createSpace")}</DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
            <div>
              <label className="block text-sm font-medium mb-1">
                {t("wiki.spaceName")} *
              </label>
              <input
                {...register("name")}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="Engineering"
              />
              {errors.name && (
                <p className="text-xs text-destructive mt-1">
                  {errors.name.message}
                </p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">
                {t("wiki.spaceKey")} *
              </label>
              <input
                {...spaceKeyField}
                onChange={(e) => {
                  setKeyEdited(true);
                  spaceKeyField.onChange(e);
                }}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary uppercase"
                placeholder="ENG"
              />
              <p className="text-xs text-muted-foreground mt-1">
                {t("wiki.spaceKeyHint")}
              </p>
              {errors.spaceKey && (
                <p className="text-xs text-destructive mt-1">
                  {errors.spaceKey.message}
                </p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">
                {t("wiki.description")}
              </label>
              <input
                {...register("description")}
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                placeholder="…"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={closeDialog}
                className="px-4 py-2 text-sm rounded-md border border-input hover:bg-muted transition-colors"
              >
                {t("wiki.cancel")}
              </button>
              <button
                type="submit"
                disabled={isSubmitting || createMutation.isPending}
                className="px-4 py-2 text-sm rounded-md bg-primary text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
              >
                {createMutation.isPending ? t("wiki.saving") : t("wiki.createSpace")}
              </button>
            </div>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation — shared AlertDialog, consistent with every
          other destructive-action confirmation in the app. */}
      <AlertDialog open={deleteTarget !== null} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t("wiki.delete")}</AlertDialogTitle>
            <AlertDialogDescription>{t("wiki.deleteSpaceConfirm")}</AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t("wiki.cancel")}</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => deleteTarget !== null && deleteMutation.mutate(deleteTarget)}
              disabled={deleteMutation.isPending}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              {t("wiki.delete")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
