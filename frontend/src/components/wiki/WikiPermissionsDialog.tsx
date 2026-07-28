import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Users,
  Loader2,
  Trash2,
  Search,
  Shield,
  UserRound,
} from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog";
import {
  wikiPermissionService,
  type WikiSpacePermissionDTO,
  type WikiGranteeType,
  type WikiPermissionLevel,
} from "../../services/wikiPermissionService";
import { commentService, type MentionUser } from "../../services/commentService";
import { useToast } from "../../contexts";

/**
 * "Manage Access" modal for a wiki space.
 *
 * Lists current grants (user or role + level), lets a manager add a new grant
 * by searching for a teammate or picking a role, and revoke any existing grant.
 * Backed by {@link wikiPermissionService} (REST) via React Query.
 */
interface WikiPermissionsDialogProps {
  spaceId: number;
  spaceName: string;
  open: boolean;
  onClose: () => void;
}

// Mirrors the backend UserRole enum.
const ROLES = ["ADMIN", "MANAGER", "MEMBER", "READONLY"] as const;

export default function WikiPermissionsDialog({
  spaceId,
  spaceName,
  open,
  onClose,
}: WikiPermissionsDialogProps) {
  const { t } = useTranslation();
  const { showSuccess, showError } = useToast();
  const queryClient = useQueryClient();

  const [granteeType, setGranteeType] = useState<WikiGranteeType>("USER");
  const [level, setLevel] = useState<WikiPermissionLevel>("READ");
  const [userQuery, setUserQuery] = useState("");
  const [selectedUser, setSelectedUser] = useState<MentionUser | null>(null);
  const [selectedRole, setSelectedRole] = useState<string>(ROLES[2]);

  // Reset the add-form whenever the dialog (re)opens.
  useEffect(() => {
    if (open) {
      setGranteeType("USER");
      setLevel("READ");
      setUserQuery("");
      setSelectedUser(null);
      setSelectedRole(ROLES[2]);
    }
  }, [open]);

  const permissionsQuery = useQuery({
    queryKey: ["wiki-permissions", spaceId],
    queryFn: () => wikiPermissionService.list(spaceId).then((r) => r.data),
    enabled: open && !!spaceId,
  });

  // Debounced user search (only when adding a USER grant and nothing picked yet).
  const [userResults, setUserResults] = useState<MentionUser[]>([]);
  const [searching, setSearching] = useState(false);
  useEffect(() => {
    if (!open || granteeType !== "USER" || selectedUser) {
      setUserResults([]);
      return;
    }
    const q = userQuery.trim();
    if (q.length < 2) {
      setUserResults([]);
      return;
    }
    let cancelled = false;
    setSearching(true);
    const handle = setTimeout(async () => {
      try {
        const res = await commentService.searchUsersForMention(q);
        if (!cancelled) setUserResults(res.data);
      } catch {
        if (!cancelled) setUserResults([]);
      } finally {
        if (!cancelled) setSearching(false);
      }
    }, 200);
    return () => {
      cancelled = true;
      clearTimeout(handle);
    };
  }, [open, granteeType, selectedUser, userQuery]);

  // Map userId → display name so USER grants render a name, not a raw id.
  const userIdsInGrants = useMemo(() => {
    const grants = permissionsQuery.data ?? [];
    return grants
      .filter((p) => p.granteeType === "USER")
      .map((p) => Number(p.granteeRef))
      .filter((n) => Number.isFinite(n));
  }, [permissionsQuery.data]);

  const granteeNamesQuery = useQuery({
    queryKey: ["wiki-permission-grantee-names", spaceId, userIdsInGrants],
    enabled: open && userIdsInGrants.length > 0,
    queryFn: async () => {
      const names: Record<number, string> = {};
      // Resolve each granted user's display name (best-effort).
      await Promise.all(
        userIdsInGrants.map(async (id) => {
          try {
            const res = await commentService.searchUsersForMention(String(id));
            const match = res.data.find((u) => u.id === id);
            if (match) names[id] = match.displayName || match.username;
          } catch {
            // ignore — falls back to "User #id"
          }
        })
      );
      return names;
    },
  });

  const grantMutation = useMutation({
    mutationFn: () => {
      const granteeRef =
        granteeType === "USER" ? String(selectedUser?.id) : selectedRole;
      return wikiPermissionService.grant(spaceId, {
        granteeType,
        granteeRef,
        level,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-permissions", spaceId] });
      showSuccess(t("wiki.share.grantSuccess"));
      setSelectedUser(null);
      setUserQuery("");
    },
    onError: () => showError(t("wiki.share.grantError")),
  });

  const revokeMutation = useMutation({
    mutationFn: (permId: number) => wikiPermissionService.revoke(permId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-permissions", spaceId] });
      showSuccess(t("wiki.share.revokeSuccess"));
    },
    onError: () => showError(t("wiki.share.revokeError")),
  });

  const canSubmit =
    granteeType === "USER" ? !!selectedUser : !!selectedRole;

  function granteeLabel(p: WikiSpacePermissionDTO): string {
    if (p.granteeType === "ROLE") {
      return t(`wiki.share.role.${p.granteeRef}`, { defaultValue: p.granteeRef });
    }
    const id = Number(p.granteeRef);
    const name = granteeNamesQuery.data?.[id];
    return name ?? t("wiki.share.userFallback", { id });
  }

  return (
    <Dialog open={open} onOpenChange={(next) => !next && onClose()}>
      <DialogContent className="sm:max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Users className="w-4 h-4" />
            {t("wiki.share.title")}
          </DialogTitle>
          <DialogDescription>
            {t("wiki.share.subtitle", { space: spaceName })}
          </DialogDescription>
        </DialogHeader>

        {/* Current grants */}
        <div className="space-y-2">
          <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
            {t("wiki.share.currentAccess")}
          </div>
          <div className="rounded-md border border-border divide-y divide-border max-h-56 overflow-y-auto">
            {permissionsQuery.isLoading && (
              <div className="flex items-center justify-center py-6 text-muted-foreground">
                <Loader2 className="w-4 h-4 animate-spin" />
              </div>
            )}
            {permissionsQuery.isError && (
              <p className="px-3 py-6 text-center text-sm text-destructive">
                {t("wiki.share.loadError")}
              </p>
            )}
            {permissionsQuery.data && permissionsQuery.data.length === 0 && (
              <p className="px-3 py-6 text-center text-sm text-muted-foreground">
                {t("wiki.share.empty")}
              </p>
            )}
            {permissionsQuery.data?.map((p) => (
              <div
                key={p.id}
                className="flex items-center justify-between gap-3 px-3 py-2"
              >
                <div className="flex items-center gap-2 min-w-0">
                  {p.granteeType === "ROLE" ? (
                    <Shield className="w-4 h-4 text-muted-foreground flex-none" />
                  ) : (
                    <UserRound className="w-4 h-4 text-muted-foreground flex-none" />
                  )}
                  <span className="text-sm font-medium truncate">
                    {granteeLabel(p)}
                  </span>
                  <span className="text-[10px] uppercase tracking-wide px-1.5 py-0.5 rounded bg-muted text-muted-foreground">
                    {p.granteeType === "ROLE"
                      ? t("wiki.share.granteeRole")
                      : t("wiki.share.granteeUser")}
                  </span>
                </div>
                <div className="flex items-center gap-2 flex-none">
                  <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-primary/10 text-primary">
                    {p.level === "WRITE"
                      ? t("wiki.share.levelWrite")
                      : t("wiki.share.levelRead")}
                  </span>
                  <button
                    type="button"
                    onClick={() => revokeMutation.mutate(p.id)}
                    disabled={revokeMutation.isPending}
                    aria-label={t("wiki.share.revoke")}
                    title={t("wiki.share.revoke")}
                    className="text-muted-foreground hover:text-destructive transition-colors disabled:opacity-50"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Add grant */}
        <div className="space-y-3 rounded-md border border-border p-3">
          <div className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
            {t("wiki.share.addAccess")}
          </div>

          {/* Grantee type toggle */}
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setGranteeType("USER")}
              className={`flex-1 text-sm rounded-md border px-3 py-1.5 transition-colors ${
                granteeType === "USER"
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-input hover:bg-muted"
              }`}
            >
              {t("wiki.share.granteeUser")}
            </button>
            <button
              type="button"
              onClick={() => setGranteeType("ROLE")}
              className={`flex-1 text-sm rounded-md border px-3 py-1.5 transition-colors ${
                granteeType === "ROLE"
                  ? "border-primary bg-primary/10 text-primary"
                  : "border-input hover:bg-muted"
              }`}
            >
              {t("wiki.share.granteeRole")}
            </button>
          </div>

          {/* USER picker */}
          {granteeType === "USER" && (
            <div className="space-y-2">
              {selectedUser ? (
                <div className="flex items-center justify-between rounded-md border border-input px-3 py-2">
                  <span className="text-sm font-medium truncate">
                    {selectedUser.displayName || selectedUser.username}
                  </span>
                  <button
                    type="button"
                    onClick={() => setSelectedUser(null)}
                    className="text-xs text-muted-foreground hover:text-foreground"
                  >
                    {t("wiki.share.change")}
                  </button>
                </div>
              ) : (
                <>
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <input
                      value={userQuery}
                      onChange={(e) => setUserQuery(e.target.value)}
                      placeholder={t("wiki.share.searchUserPlaceholder")}
                      className="w-full rounded-md border border-input bg-background pl-9 pr-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>
                  {userQuery.trim().length >= 2 && (
                    <div className="rounded-md border border-border divide-y divide-border max-h-40 overflow-y-auto">
                      {searching && (
                        <div className="flex items-center justify-center py-4 text-muted-foreground">
                          <Loader2 className="w-4 h-4 animate-spin" />
                        </div>
                      )}
                      {!searching && userResults.length === 0 && (
                        <p className="px-3 py-4 text-center text-sm text-muted-foreground">
                          {t("wiki.share.noUsers")}
                        </p>
                      )}
                      {!searching &&
                        userResults.map((u) => (
                          <button
                            key={u.id}
                            type="button"
                            onClick={() => {
                              setSelectedUser(u);
                              setUserResults([]);
                            }}
                            className="w-full text-left px-3 py-2 hover:bg-muted transition-colors"
                          >
                            <span className="text-sm font-medium block truncate">
                              {u.displayName || u.username}
                            </span>
                            {u.email && (
                              <span className="text-xs text-muted-foreground block truncate">
                                {u.email}
                              </span>
                            )}
                          </button>
                        ))}
                    </div>
                  )}
                </>
              )}
            </div>
          )}

          {/* ROLE picker */}
          {granteeType === "ROLE" && (
            <select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
              className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              {ROLES.map((r) => (
                <option key={r} value={r}>
                  {t(`wiki.share.role.${r}`, { defaultValue: r })}
                </option>
              ))}
            </select>
          )}

          {/* Level picker */}
          <div className="flex items-center gap-2">
            <label className="text-sm text-muted-foreground">
              {t("wiki.share.accessLevel")}
            </label>
            <select
              value={level}
              onChange={(e) => setLevel(e.target.value as WikiPermissionLevel)}
              className="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="READ">{t("wiki.share.levelRead")}</option>
              <option value="WRITE">{t("wiki.share.levelWrite")}</option>
            </select>
          </div>

          <button
            type="button"
            onClick={() => grantMutation.mutate()}
            disabled={!canSubmit || grantMutation.isPending}
            className="w-full px-4 py-2 text-sm rounded-md bg-primary text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
          >
            {grantMutation.isPending
              ? t("wiki.share.granting")
              : t("wiki.share.grant")}
          </button>
        </div>

        <div className="flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm rounded-md border border-input hover:bg-muted transition-colors"
          >
            {t("wiki.share.done")}
          </button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
