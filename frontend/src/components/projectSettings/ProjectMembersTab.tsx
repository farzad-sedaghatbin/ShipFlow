import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { UserPlus, Trash2, Loader2 } from 'lucide-react';
import { Avatar, AvatarFallback } from '../ui/avatar';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { ConfirmDialog } from '../ui/confirm-dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../ui/dialog';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { projectService } from '../../services/projectService';
import { commentService } from '../../services/commentService';
import type { ProjectMember, ProjectRole } from '../../types';
import { useToast } from '../../contexts';

const ROLES: ProjectRole[] = ['VIEWER', 'CONTRIBUTOR', 'MANAGER'];

interface Props {
  projectId: number;
  isManager: boolean;
}

export function ProjectMembersTab({ projectId, isManager }: Props) {
  const { t } = useTranslation();
  const { showToast } = useToast();

  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [removeTarget, setRemoveTarget] = useState<ProjectMember | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<{ id: number; username: string }[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [addRole, setAddRole] = useState<ProjectRole>('VIEWER');
  const [addSaving, setAddSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setMembers(await projectService.getMembers(projectId));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [projectId]);

  useEffect(() => {
    if (!searchQuery.trim()) { setSearchResults([]); return; }
    commentService
      .searchUsersForMention(searchQuery)
      .then((r) => setSearchResults(r.data.map((u) => ({ id: u.id, username: u.username }))))
      .catch(() => setSearchResults([]));
  }, [searchQuery]);

  const handleRemove = async () => {
    if (!removeTarget) return;
    try {
      await projectService.revokeAccess(projectId, removeTarget.userId);
      showToast(t('projectSettings.removeMember'), 'success');
      setRemoveTarget(null);
      load();
    } catch {
      showToast(t('common.error'), 'error');
    }
  };

  const handleRoleChange = async (member: ProjectMember, newRole: ProjectRole) => {
    try {
      await projectService.grantAccess(projectId, member.userId, newRole);
      load();
    } catch {
      showToast(t('common.error'), 'error');
    }
  };

  const handleAdd = async () => {
    if (!selectedUserId) return;
    setAddSaving(true);
    try {
      await projectService.grantAccess(projectId, selectedUserId, addRole);
      showToast(t('projectSettings.addMember'), 'success');
      setAddOpen(false);
      setSearchQuery('');
      setSelectedUserId(null);
      setAddRole('VIEWER');
      load();
    } catch {
      showToast(t('common.error'), 'error');
    } finally {
      setAddSaving(false);
    }
  };

  const initials = (username: string) =>
    username.slice(0, 2).toUpperCase();

  if (loading) {
    return (
      <div className="flex justify-center py-8">
        <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">{t('projectSettings.membersDesc')}</p>
        {isManager && (
          <Button size="sm" className="gap-2" onClick={() => setAddOpen(true)}>
            <UserPlus className="h-4 w-4" />
            {t('projectSettings.addMember')}
          </Button>
        )}
      </div>

      {members.length === 0 ? (
        <p className="text-sm text-muted-foreground py-8 text-center">
          {t('projectSettings.noMembers')}
        </p>
      ) : (
        <div className="divide-y rounded-md border">
          {members.map((m) => (
            <div key={m.userId} className="flex items-center gap-3 px-4 py-3">
              <Avatar className="h-8 w-8">
                <AvatarFallback className="text-xs">{initials(m.username)}</AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium truncate">{m.username}</p>
                {m.grantedByUsername && (
                  <p className="text-xs text-muted-foreground truncate">
                    {t('projectSettings.grantedBy')}: {m.grantedByUsername}
                  </p>
                )}
              </div>

              {isManager ? (
                <Select
                  value={m.projectRole}
                  onValueChange={(v) => handleRoleChange(m, v as ProjectRole)}
                >
                  <SelectTrigger className="w-32 h-7 text-xs">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ROLES.map((r) => (
                      <SelectItem key={r} value={r} className="text-xs">
                        {t(`projectSettings.role.${r}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              ) : (
                <Badge variant="outline" className="text-xs">
                  {t(`projectSettings.role.${m.projectRole}`)}
                </Badge>
              )}

              {isManager && (
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 text-destructive hover:text-destructive shrink-0"
                  onClick={() => setRemoveTarget(m)}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Add member dialog */}
      <Dialog open={addOpen} onOpenChange={setAddOpen}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>{t('projectSettings.addMember')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>User</Label>
              <Input
                placeholder="Search by username…"
                value={searchQuery}
                onChange={(e) => { setSearchQuery(e.target.value); setSelectedUserId(null); }}
              />
              {searchResults.length > 0 && !selectedUserId && (
                <div className="border rounded-md divide-y max-h-40 overflow-y-auto">
                  {searchResults.map((u) => (
                    <button
                      key={u.id}
                      type="button"
                      className="w-full text-left px-3 py-2 text-sm hover:bg-muted"
                      onClick={() => { setSelectedUserId(u.id); setSearchQuery(u.username); setSearchResults([]); }}
                    >
                      {u.username}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className="space-y-1.5">
              <Label>{t('projectSettings.members')}</Label>
              <Select value={addRole} onValueChange={(v) => setAddRole(v as ProjectRole)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ROLES.map((r) => (
                    <SelectItem key={r} value={r}>
                      {t(`projectSettings.role.${r}`)} — {t(`projectSettings.roleDesc.${r}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setAddOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleAdd} disabled={!selectedUserId || addSaving}>
              {addSaving && <Loader2 className="h-4 w-4 animate-spin mr-1.5" />}
              {t('projectSettings.addMember')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Remove confirmation */}
      <ConfirmDialog
        open={!!removeTarget}
        onOpenChange={(o) => !o && setRemoveTarget(null)}
        title={t('projectSettings.removeMember')}
        description={t('projectSettings.removeConfirm', { username: removeTarget?.username ?? '' })}
        onConfirm={handleRemove}
        variant="destructive"
      />
    </div>
  );
}
