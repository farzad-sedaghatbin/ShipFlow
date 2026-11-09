import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { useNotificationStream } from '../hooks/useNotificationStream';
import {
  Bell,
  BellOff,
  Check,
  CheckCheck,
  Trash2,
  AlertTriangle,
  Info,
  XCircle,
  MessageSquare,
} from 'lucide-react';
import { dashboardNotificationApi } from '../services/dashboardApi';
import { DashboardNotification } from '../types/dashboard';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
  DropdownMenuLabel,
} from '@/components/ui/dropdown-menu';
import { cn } from '@/lib/utils';

export const NotificationCenter: React.FC = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<DashboardNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  // "Clear all" deletes everything and cannot be undone, so it takes a second click to confirm.
  const [confirmClear, setConfirmClear] = useState(false);

  const loadNotifications = useCallback(async () => {
    try {
      setLoading(true);
      const data = await dashboardNotificationApi.getAllNotifications();
      setNotifications(data);
    } catch (error) {
      console.error('Failed to load notifications:', error);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadUnreadCount = useCallback(async () => {
    try {
      const count = await dashboardNotificationApi.getUnreadCount();
      setUnreadCount(count);
    } catch (error) {
      console.error('Failed to load unread count:', error);
    }
  }, []);

  // Stable callback: routes SSE events — notification events update the bell,
  // all other named events are forwarded as browser CustomEvents for other components to handle.
  const handleSseNotification = useCallback((eventName: string, payload: unknown) => {
    if (eventName === 'notification') {
      loadUnreadCount();
      if (open) loadNotifications();
    } else {
      window.dispatchEvent(new CustomEvent(eventName, { detail: payload }));
    }
  }, [open, loadUnreadCount, loadNotifications]);

  // Subscribe to the real-time SSE stream — instant push replaces the 30 s polling
  useNotificationStream(handleSseNotification);

  useEffect(() => {
    loadNotifications();
    loadUnreadCount();

    // Fallback polling at 60 s in case the SSE stream is unavailable (proxy, firewall, etc.)
    const interval = setInterval(() => {
      loadUnreadCount();
    }, 60_000);

    return () => clearInterval(interval);
  }, [loadNotifications, loadUnreadCount]);

  const handleMarkAsRead = async (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await dashboardNotificationApi.markAsRead(id);
      await Promise.all([loadNotifications(), loadUnreadCount()]);
    } catch (error) {
      console.error('Failed to mark as read:', error);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await dashboardNotificationApi.markAllAsRead();
      await Promise.all([loadNotifications(), loadUnreadCount()]);
    } catch (error) {
      console.error('Failed to mark all as read:', error);
    }
  };

  const handleClearAll = async () => {
    if (!confirmClear) {
      setConfirmClear(true);
      return;
    }
    try {
      await dashboardNotificationApi.clearAll();
      setNotifications([]);
      setUnreadCount(0);
    } catch (error) {
      console.error('Failed to clear notifications:', error);
    } finally {
      setConfirmClear(false);
    }
  };

  // The confirmation must not linger: reset it when the menu closes or after a few seconds.
  useEffect(() => {
    if (!confirmClear) return;
    const timer = setTimeout(() => setConfirmClear(false), 4000);
    return () => clearTimeout(timer);
  }, [confirmClear]);

  useEffect(() => {
    if (!open) setConfirmClear(false);
  }, [open]);

  const handleDelete = async (id: number, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await dashboardNotificationApi.deleteNotification(id);
      await Promise.all([loadNotifications(), loadUnreadCount()]);
    } catch (error) {
      console.error('Failed to delete notification:', error);
    }
  };

  const handleNotificationClick = async (notification: DashboardNotification) => {
    if (!notification.isRead) {
      await dashboardNotificationApi.markAsRead(notification.id);
      await Promise.all([loadNotifications(), loadUnreadCount()]);
    }
    
    if (notification.actionUrl) {
      navigate(notification.actionUrl);
      setOpen(false);
    }
  };

  /** Returns a type-specific icon that overrides the severity icon for known notification types. */
  const getTypeIcon = (type: DashboardNotification['type'], severity: DashboardNotification['severity']) => {
    if (type === 'COMMENT_MENTION') {
      return <MessageSquare className="h-4 w-4 text-violet-500" />;
    }
    // Fall back to severity-based icon
    switch (severity) {
      case 'CRITICAL':
      case 'ERROR':
        return <XCircle className="h-4 w-4 text-destructive" />;
      case 'WARNING':
        return <AlertTriangle className="h-4 w-4 text-amber-500" />;
      case 'INFO':
      default:
        return <Info className="h-4 w-4 text-blue-500" />;
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
      case 'ERROR':
        return 'border-l-destructive';
      case 'WARNING':
        return 'border-l-amber-500';
      case 'INFO':
      default:
        return 'border-l-blue-500';
    }
  };

  const getTypeLabel = (type: string) => {
    return type.replace(/_/g, ' ').toLowerCase()
      .replace(/\b\w/g, l => l.toUpperCase());
  };

  const formatTime = (dateStr: string) => {
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return t('notificationCenter.justNow');
    if (diffMins < 60) return t('notificationCenter.minutesAgo', { count: diffMins });
    if (diffHours < 24) return t('notificationCenter.hoursAgo', { count: diffHours });
    if (diffDays < 7) return t('notificationCenter.daysAgo', { count: diffDays });
    return formatLocalizedDate(date, i18n.language);
  };

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button 
          variant="ghost" 
          size="icon" 
          className="relative"
          aria-label={unreadCount > 0
            ? t('notificationCenter.ariaUnread', { count: unreadCount })
            : t('notificationCenter.title')}
        >
          {unreadCount > 0 ? (
            <Bell className="h-5 w-5" />
          ) : (
            <BellOff className="h-5 w-5" />
          )}
          {unreadCount > 0 && (
            <Badge 
              variant="destructive" 
              className="absolute -top-1 -right-1 h-5 w-5 p-0 flex items-center justify-center text-xs"
            >
              {unreadCount > 99 ? '99+' : unreadCount}
            </Badge>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-[calc(100vw-2rem)] sm:w-96 max-w-96">
        <DropdownMenuLabel className="flex items-center justify-between gap-2">
          <span>{t('notificationCenter.title')}</span>
          <div className="flex items-center gap-1">
            {unreadCount > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={handleMarkAllAsRead}
                className="h-7 text-xs"
              >
                <CheckCheck className="h-3 w-3 mr-1" />
                {t('notificationCenter.markAllRead')}
              </Button>
            )}
            {notifications.length > 0 && (
              <Button
                variant="ghost"
                size="sm"
                onClick={handleClearAll}
                className={cn(
                  'h-7 text-xs',
                  confirmClear
                    ? 'text-destructive hover:text-destructive font-semibold'
                    : 'text-muted-foreground hover:text-destructive'
                )}
              >
                <Trash2 className="h-3 w-3 mr-1" />
                {confirmClear ? t('notificationCenter.clearAllConfirm') : t('notificationCenter.clearAll')}
              </Button>
            )}
          </div>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        
        {loading ? (
          <div className="p-4 text-center text-muted-foreground">
            {t('notificationCenter.loading')}
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-8 text-center">
            <BellOff className="h-12 w-12 mx-auto mb-2 text-muted-foreground/50" />
            <p className="text-sm text-muted-foreground">{t('notificationCenter.empty')}</p>
          </div>
        ) : (
          <div className="max-h-[400px] overflow-y-auto overflow-x-hidden">
            <div className="space-y-1 p-1">
              {notifications.map((notification) => (
                <div
                  key={notification.id}
                  onClick={() => handleNotificationClick(notification)}
                  className={cn(
                    'p-3 rounded-lg border-l-4 transition-colors cursor-pointer min-w-0',
                    getSeverityColor(notification.severity),
                    notification.isRead
                      ? 'opacity-60 hover:opacity-100 hover:bg-muted/50'
                      : 'bg-primary/10 hover:bg-primary/15'
                  )}
                >
                  <div className="flex items-start gap-2">
                    {!notification.isRead && (
                      <span
                        className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary"
                        aria-label={t('notificationCenter.unread')}
                      />
                    )}
                    <div className="mt-0.5">
                      {getTypeIcon(notification.type, notification.severity)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2 mb-1">
                        <h4 className={cn(
                          'text-sm truncate',
                          notification.isRead ? 'font-normal' : 'font-semibold text-foreground'
                        )}>
                          {notification.title}
                        </h4>
                        <div className="flex shrink-0 items-center gap-1">
                          {!notification.isRead && (
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-6 w-6"
                              onClick={(e) => handleMarkAsRead(notification.id, e)}
                              title={t('notificationCenter.markRead')}
                            >
                              <Check className="h-3 w-3" />
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-6 w-6 text-muted-foreground hover:text-destructive"
                            onClick={(e) => handleDelete(notification.id, e)}
                            title={t('notificationCenter.delete')}
                            aria-label={t('notificationCenter.delete')}
                          >
                            <Trash2 className="h-3 w-3" />
                          </Button>
                        </div>
                      </div>
                      {notification.message && (
                        <p className="text-xs text-muted-foreground line-clamp-2 mb-1">
                          {notification.message}
                        </p>
                      )}
                      <div className="flex items-center justify-between">
                        <Badge variant="outline" className="text-xs">
                          {getTypeLabel(notification.type)}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          {formatTime(notification.createdAt)}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default NotificationCenter;
