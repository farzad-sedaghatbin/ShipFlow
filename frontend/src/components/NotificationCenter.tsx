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
import { ScrollArea } from '@/components/ui/scroll-area';
import { cn } from '@/lib/utils';

export const NotificationCenter: React.FC = () => {
  const { i18n } = useTranslation();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<DashboardNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);

  // Stable callback: called whenever the SSE stream delivers a new notification
  const handleSseNotification = useCallback(() => {
    loadUnreadCount();
    // Reload the full list only if the dropdown is open to avoid background noise
    if (open) {
      loadNotifications();
    }
  }, [open]); // eslint-disable-line react-hooks/exhaustive-deps

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
  }, []);

  const loadNotifications = async () => {
    try {
      setLoading(true);
      const data = await dashboardNotificationApi.getAllNotifications();
      setNotifications(data);
    } catch (error) {
      console.error('Failed to load notifications:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadUnreadCount = async () => {
    try {
      const count = await dashboardNotificationApi.getUnreadCount();
      setUnreadCount(count);
    } catch (error) {
      console.error('Failed to load unread count:', error);
    }
  };

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
        return 'border-l-destructive bg-destructive/5';
      case 'WARNING':
        return 'border-l-amber-500 bg-amber-500/5';
      case 'INFO':
      default:
        return 'border-l-blue-500 bg-blue-500/5';
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

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return formatLocalizedDate(date, i18n.language);
  };

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button 
          variant="ghost" 
          size="icon" 
          className="relative"
          aria-label={`Notifications${unreadCount > 0 ? ` (${unreadCount} unread)` : ''}`}
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
        <DropdownMenuLabel className="flex items-center justify-between">
          <span>Notifications</span>
          {unreadCount > 0 && (
            <Button
              variant="ghost"
              size="sm"
              onClick={handleMarkAllAsRead}
              className="h-7 text-xs"
            >
              <CheckCheck className="h-3 w-3 mr-1" />
              Mark all read
            </Button>
          )}
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        
        {loading ? (
          <div className="p-4 text-center text-muted-foreground">
            Loading notifications...
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-8 text-center">
            <BellOff className="h-12 w-12 mx-auto mb-2 text-muted-foreground/50" />
            <p className="text-sm text-muted-foreground">No notifications</p>
          </div>
        ) : (
          <ScrollArea className="h-[400px]">
            <div className="space-y-1 p-1">
              {notifications.map((notification) => (
                <div
                  key={notification.id}
                  onClick={() => handleNotificationClick(notification)}
                  className={cn(
                    'p-3 rounded-lg border-l-4 transition-colors cursor-pointer hover:bg-muted/50',
                    getSeverityColor(notification.severity),
                    !notification.isRead && 'bg-primary/5'
                  )}
                >
                  <div className="flex items-start gap-2">
                    <div className="mt-0.5">
                      {getTypeIcon(notification.type, notification.severity)}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2 mb-1">
                        <h4 className={cn(
                          'text-sm font-semibold truncate',
                          !notification.isRead && 'text-foreground'
                        )}>
                          {notification.title}
                        </h4>
                        <div className="flex items-center gap-1">
                          {!notification.isRead && (
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-6 w-6"
                              onClick={(e) => handleMarkAsRead(notification.id, e)}
                              title="Mark as read"
                            >
                              <Check className="h-3 w-3" />
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-6 w-6 text-muted-foreground hover:text-destructive"
                            onClick={(e) => handleDelete(notification.id, e)}
                            title="Delete"
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
          </ScrollArea>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

export default NotificationCenter;
