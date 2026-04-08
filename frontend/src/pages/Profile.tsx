import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  User,
  Pencil,
  Save,
  X,
  Lock,
  Eye,
  EyeOff,
  Camera,
  Loader2,
  Bell,
} from 'lucide-react';
import { useToast } from '../contexts';
import api from '../services/api';
import { UserProfile, UpdateProfileRequest, ChangePasswordRequest, NotificationUserMapping } from '../types';

import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import { Badge } from '../components/ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../components/ui/avatar';
import { Separator } from '../components/ui/separator';
import { Alert, AlertDescription } from '../components/ui/alert';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';

export default function Profile() {
  const { t, i18n } = useTranslation();
  const { showToast } = useToast();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editForm, setEditForm] = useState<UpdateProfileRequest>({});
  
  // Password change dialog
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [passwordForm, setPasswordForm] = useState<ChangePasswordRequest>({
    currentPassword: '',
    newPassword: '',
  });
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);

  // Notification mapping state
  const [notificationMappings, setNotificationMappings] = useState<NotificationUserMapping[]>([]);
  const [slackId, setSlackId] = useState('');
  const [teamsId, setTeamsId] = useState('');
  const [savingMapping, setSavingMapping] = useState<string | null>(null);

  useEffect(() => {
    const abortController = new AbortController();
    fetchProfile();
    fetchNotificationMappings();
    return () => abortController.abort();
  }, []);

  const fetchProfile = async () => {
    try {
      const response = await api.get<UserProfile>('/users/me/profile');
      setProfile(response.data);
      setEditForm({
        email: response.data.email,
        avatarUrl: response.data.avatarUrl,
        bio: response.data.bio,
        skills: response.data.skills,
        department: response.data.department,
      });
    } catch (error) {
      showToast(t('profile.loadFailed'), 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveProfile = async () => {
    setSaving(true);
    try {
      const response = await api.put<UserProfile>('/users/me/profile', editForm);
      setProfile(response.data);
      setEditing(false);
      showToast(t('profilePage.profileUpdated'), 'success');
    } catch (error) {
      showToast(t('profilePage.failedToLoad'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleChangePassword = async () => {
    if (passwordForm.newPassword !== confirmPassword) {
      showToast(t('profilePage.passwordMismatch'), 'error');
      return;
    }
    if (passwordForm.newPassword.length < 6) {
      showToast(t('profilePage.passwordMinLength'), 'error');
      return;
    }

    setChangingPassword(true);
    try {
      await api.put(`/users/${profile?.id}/password`, passwordForm);
      showToast(t('profilePage.passwordChanged'), 'success');
      setPasswordDialogOpen(false);
      setPasswordForm({ currentPassword: '', newPassword: '' });
      setConfirmPassword('');
    } catch (error) {
      // Error is handled by interceptor
    } finally {
      setChangingPassword(false);
    }
  };

  const fetchNotificationMappings = async () => {
    try {
      const response = await api.get<NotificationUserMapping[]>('/users/me/notification-mappings');
      setNotificationMappings(response.data);
      const slack = response.data.find((m) => m.providerName === 'slack');
      const teams = response.data.find((m) => m.providerName === 'teams');
      if (slack) setSlackId(slack.externalUserId);
      if (teams) setTeamsId(teams.externalUserId);
    } catch {
      // Non-critical — profile still works without mappings
    }
  };

  const handleSaveMapping = async (providerName: string, externalUserId: string) => {
    setSavingMapping(providerName);
    try {
      if (externalUserId.trim()) {
        await api.put('/users/me/notification-mappings', { providerName, externalUserId: externalUserId.trim() });
        showToast(t('profilePage.notificationIdSaved'), 'success');
      } else {
        await api.delete(`/users/me/notification-mappings/${providerName}`);
        showToast(t('profilePage.notificationIdRemoved'), 'success');
      }
      await fetchNotificationMappings();
    } catch {
      showToast(t('profilePage.notificationIdSaveFailed'), 'error');
    } finally {
      setSavingMapping(null);
    }
  };

  const getRoleClasses = (role: string) => {
    const classes: Record<string, string> = {
      ADMIN: 'bg-red-500/10 text-red-400 border-red-500/20',
      PROJECT_MANAGER: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
      PRODUCT: 'bg-purple-500/10 text-purple-400 border-purple-500/20',
      DEVELOPER: 'bg-green-500/10 text-green-400 border-green-500/20',
      QA: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    };
    return classes[role] || '';
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!profile) {
    return (
      <Alert variant="destructive">
        <AlertDescription>{t('profilePage.failedToLoad')}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-foreground">{t('profilePage.title')}</h1>

      <div className="grid md:grid-cols-3 gap-6">
        {/* Profile Card */}
        <div className="space-y-4">
          <Card>
            <CardContent className="pt-6 text-center">
              <Avatar className="h-28 w-28 mx-auto mb-4">
                <AvatarImage src={profile.avatarUrl} />
                <AvatarFallback className="text-3xl">
                  {profile.personName?.charAt(0) || profile.username.charAt(0)}
                </AvatarFallback>
              </Avatar>
              
              {editing && (
                <div className="mb-4">
                  <Label htmlFor="avatar-url" className="sr-only">{t('profilePage.avatarUrl')}</Label>
                  <div className="relative">
                    <Camera className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="avatar-url"
                      placeholder={t('profilePage.avatarUrl')}
                      value={editForm.avatarUrl || ''}
                      onChange={(e) => setEditForm({ ...editForm, avatarUrl: e.target.value })}
                      className="pl-10"
                    />
                  </div>
                </div>
              )}
              
              <h2 className="text-xl font-semibold text-foreground">
                {profile.personName || profile.username}
              </h2>
              <p className="text-sm text-muted-foreground mb-3">@{profile.username}</p>
              <Badge variant="outline" className={getRoleClasses(profile.role)}>
                {profile.role.replace('_', ' ')}
              </Badge>
              {profile.department && (
                <p className="text-sm text-muted-foreground mt-3">{profile.department}</p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base flex items-center gap-2">
                <Lock className="h-4 w-4" />
                {t('profilePage.security')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Button 
                variant="outline" 
                className="w-full"
                onClick={() => setPasswordDialogOpen(true)}
              >
                {t('profilePage.changePassword')}
              </Button>
            </CardContent>
          </Card>
        </div>

        {/* Profile Details */}
        <div className="md:col-span-2">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>{t('profilePage.profileDetails')}</CardTitle>
              {!editing ? (
                <Button variant="outline" size="sm" onClick={() => setEditing(true)}>
                  <Pencil className="h-4 w-4 mr-2" />
                  {t('profilePage.editProfile')}
                </Button>
              ) : (
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setEditing(false);
                      setEditForm({
                        email: profile.email,
                        avatarUrl: profile.avatarUrl,
                        bio: profile.bio,
                        skills: profile.skills,
                        department: profile.department,
                      });
                    }}
                  >
                    <X className="h-4 w-4 mr-2" />
                    {t('common.cancel')}
                  </Button>
                  <Button size="sm" onClick={handleSaveProfile} disabled={saving}>
                    {saving ? (
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    ) : (
                      <Save className="h-4 w-4 mr-2" />
                    )}
                    {t('common.save')}
                  </Button>
                </div>
              )}
            </CardHeader>
            <CardContent className="space-y-6">
              <Separator />
              
              <div className="grid sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="username">{t('profilePage.username')}</Label>
                  <div className="relative">
                    <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                    <Input
                      id="username"
                      value={profile.username}
                      disabled
                      className="pl-10"
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">{t('profilePage.email')}</Label>
                  <Input
                    id="email"
                    type="email"
                    value={editing ? editForm.email || '' : profile.email || ''}
                    onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                    disabled={!editing}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="department">{t('profilePage.department')}</Label>
                  <Input
                    id="department"
                    value={editing ? editForm.department || '' : profile.department || ''}
                    onChange={(e) => setEditForm({ ...editForm, department: e.target.value })}
                    disabled={!editing}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="role">{t('profilePage.role')}</Label>
                  <Input
                    id="role"
                    value={profile.role.replace('_', ' ')}
                    disabled
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="skills">{t('profilePage.skills')}</Label>
                <Input
                  id="skills"
                  value={editing ? editForm.skills || '' : profile.skills || ''}
                  onChange={(e) => setEditForm({ ...editForm, skills: e.target.value })}
                  disabled={!editing}
                  placeholder={t('profilePage.skillsPlaceholder')}
                />
                <p className="text-xs text-muted-foreground">{t('profilePage.skillsHelp')}</p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="bio">{t('profilePage.bio')}</Label>
                <Textarea
                  id="bio"
                  rows={4}
                  value={editing ? editForm.bio || '' : profile.bio || ''}
                  onChange={(e) => setEditForm({ ...editForm, bio: e.target.value })}
                  disabled={!editing}
                  placeholder={t('profilePage.bioPlaceholder')}
                />
              </div>

              <Separator />

              <p className="text-sm text-muted-foreground">
                {t('profilePage.memberSince')}: {formatLocalizedDate(new Date(profile.createdAt), i18n.language)}
                {profile.updatedAt && (
                  <> • {t('profilePage.lastUpdated')}: {formatLocalizedDate(new Date(profile.updatedAt), i18n.language)}</>
                )}
              </p>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Notification IDs Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Bell className="h-5 w-5" />
            {t('profilePage.notificationIds')}
          </CardTitle>
          <p className="text-sm text-muted-foreground">{t('profilePage.notificationIdsDesc')}</p>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="slack-member-id">{t('profilePage.slackMemberId')}</Label>
              <div className="flex gap-2">
                <Input
                  id="slack-member-id"
                  value={slackId}
                  onChange={(e) => setSlackId(e.target.value)}
                  placeholder="U0123456789"
                />
                <Button
                  size="sm"
                  onClick={() => handleSaveMapping('slack', slackId)}
                  disabled={savingMapping === 'slack'}
                >
                  {savingMapping === 'slack' ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">{t('profilePage.slackMemberIdHelp')}</p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="teams-member-id">{t('profilePage.teamsMemberId')}</Label>
              <div className="flex gap-2">
                <Input
                  id="teams-member-id"
                  value={teamsId}
                  onChange={(e) => setTeamsId(e.target.value)}
                  placeholder="user@company.com"
                />
                <Button
                  size="sm"
                  onClick={() => handleSaveMapping('teams', teamsId)}
                  disabled={savingMapping === 'teams'}
                >
                  {savingMapping === 'teams' ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">{t('profilePage.teamsMemberIdHelp')}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Change Password Dialog */}
      <Dialog open={passwordDialogOpen} onOpenChange={setPasswordDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('profilePage.changePassword')}</DialogTitle>
            <DialogDescription>
              {t('profilePage.changePasswordDesc')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="current-password">{t('profilePage.currentPassword')}</Label>
              <div className="relative">
                <Input
                  id="current-password"
                  type={showCurrentPassword ? 'text' : 'password'}
                  value={passwordForm.currentPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, currentPassword: e.target.value })}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="absolute right-0 top-0 h-full px-3"
                  onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                >
                  {showCurrentPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </Button>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="new-password">{t('profilePage.newPassword')}</Label>
              <div className="relative">
                <Input
                  id="new-password"
                  type={showNewPassword ? 'text' : 'password'}
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm({ ...passwordForm, newPassword: e.target.value })}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="absolute right-0 top-0 h-full px-3"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                >
                  {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">{t('profilePage.minChars')}</p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirm-password">{t('profilePage.confirmPassword')}</Label>
              <Input
                id="confirm-password"
                type={showNewPassword ? 'text' : 'password'}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className={confirmPassword !== '' && confirmPassword !== passwordForm.newPassword ? 'border-destructive' : ''}
              />
              {confirmPassword !== '' && confirmPassword !== passwordForm.newPassword && (
                <p className="text-xs text-destructive">{t('profilePage.passwordsDoNotMatch')}</p>
              )}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setPasswordDialogOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button
              onClick={handleChangePassword}
              disabled={changingPassword || !passwordForm.currentPassword || !passwordForm.newPassword || !confirmPassword}
            >
              {changingPassword ? t('profilePage.changing') : t('profilePage.changePassword')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
