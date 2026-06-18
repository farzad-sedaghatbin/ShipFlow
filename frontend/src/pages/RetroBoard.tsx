import { useEffect, useRef, useState, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { safeParseId } from '../utils/validation';
import {
  ArrowLeft,
  Plus,
  Trash2,
  Pencil,
  Check,
  X,
  Lock,
  Play,
  Square,
  ThumbsUp,
  ThumbsDown,
  Merge,
  Loader2,
  Rocket,
  LayoutTemplate,
  Timer,
  TimerOff,
  CheckCircle2,
} from 'lucide-react';
import { retroService } from '../services/retroService';
import { ActOnRetroItemsDialog } from '../components/ActOnRetroItemsDialog';
import { RetroSummaryPanel } from '../components/RetroSummaryPanel';

import { useAuth } from '../contexts/AuthContext';
import { Retrospective, RetroItem, RetroColumnType, RetroStatus } from '../types';
import { cn } from '../lib/utils';

import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Textarea } from '../components/ui/textarea';
import { Checkbox } from '../components/ui/checkbox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { useToast } from '../contexts';

// Retro template definitions
interface RetroTemplate {
  key: string;
  titleKey: string;
  columns: { type: RetroColumnType; emoji: string; color: string; titleKey: string }[];
}

const RETRO_TEMPLATES: RetroTemplate[] = [
  {
    key: 'went_well',
    titleKey: 'retroBoardTemplates.templateWentWell',
    columns: [
      { type: 'WENT_WELL', emoji: '✅', color: 'border-green-500', titleKey: 'retroBoardPage.wentWell' },
      { type: 'DID_NOT_GO_WELL', emoji: '⚠️', color: 'border-orange-500', titleKey: 'retroBoardPage.didNotGoWell' },
      { type: 'ACTIONS', emoji: '🧾', color: 'border-purple-500', titleKey: 'retroBoardPage.actions' },
    ],
  },
  {
    key: 'start_stop_continue',
    titleKey: 'retroBoardTemplates.templateStartStopContinue',
    columns: [
      { type: 'WENT_WELL', emoji: '▶️', color: 'border-green-500', titleKey: 'retroBoardPage.wentWell' },
      { type: 'DID_NOT_GO_WELL', emoji: '⏹️', color: 'border-red-500', titleKey: 'retroBoardPage.didNotGoWell' },
      { type: 'TRY_NEXT', emoji: '🔄', color: 'border-blue-500', titleKey: 'retroBoardPage.tryNext' },
    ],
  },
  {
    key: '4ls',
    titleKey: 'retroBoardTemplates.template4Ls',
    columns: [
      { type: 'WENT_WELL', emoji: '❤️', color: 'border-green-500', titleKey: 'retroBoardPage.wentWell' },
      { type: 'TRY_NEXT', emoji: '📚', color: 'border-blue-500', titleKey: 'retroBoardPage.tryNext' },
      { type: 'DID_NOT_GO_WELL', emoji: '😕', color: 'border-orange-500', titleKey: 'retroBoardPage.didNotGoWell' },
      { type: 'ACTIONS', emoji: '💭', color: 'border-purple-500', titleKey: 'retroBoardPage.actions' },
    ],
  },
];

const columns: { type: RetroColumnType; title: string; emoji: string; color: string }[] = [
  { type: 'WENT_WELL', title: '', emoji: '✅', color: 'border-green-500' },
  { type: 'DID_NOT_GO_WELL', title: '', emoji: '⚠️', color: 'border-orange-500' },
  { type: 'TRY_NEXT', title: '', emoji: '🚀', color: 'border-blue-500' },
  { type: 'ACTIONS', title: '', emoji: '🧾', color: 'border-purple-500' },
];

const statusConfig: Record<RetroStatus, { label: string; variant: 'default' | 'success' | 'secondary' }> = {
  DRAFT: { label: '', variant: 'secondary' },
  OPEN: { label: '', variant: 'default' },
  CLOSED: { label: '', variant: 'success' },
};

export default function RetroBoard() {
  const { t, i18n } = useTranslation();
  const { id: idParam } = useParams<{ id: string }>();
  const id = safeParseId(idParam);
  const { showSuccess, showError } = useToast();
  const [retro, setRetro] = useState<Retrospective | null>(null);
  const [items, setItems] = useState<RetroItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [newItemContent, setNewItemContent] = useState<Record<RetroColumnType, string>>({
    WENT_WELL: '',
    DID_NOT_GO_WELL: '',
    TRY_NEXT: '',
    ACTIONS: '',
  });
  const [isAnonymous, setIsAnonymous] = useState<Record<RetroColumnType, boolean>>({
    WENT_WELL: false,
    DID_NOT_GO_WELL: false,
    TRY_NEXT: false,
    ACTIONS: false,
  });
  const [editingItem, setEditingItem] = useState<{ id: number; content: string } | null>(null);
  const [mergeDialog, setMergeDialog] = useState<{ open: boolean; sourceItem: RetroItem | null; columnType: RetroColumnType | null }>({
    open: false,
    sourceItem: null,
    columnType: null,
  });
  const [actOnItemsDialog, setActOnItemsDialog] = useState(false);
  const [templateConfirmDialog, setTemplateConfirmDialog] = useState<RetroTemplate | null>(null);
  const [discussingItemId, setDiscussingItemId] = useState<number | null>(null);
  const [discussTimeLeft, setDiscussTimeLeft] = useState(0);
  const discussTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const { user } = useAuth();
  const canManageRetro = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const isReadOnly = retro?.status === 'CLOSED';
  const canEditItem = (item: RetroItem) =>
    canManageRetro || (item.authorId != null && item.authorId === user?.userId);

  // Memoize actionable items to avoid repeated filtering
  const actionableItems = useMemo(() => {
    return items.filter(i => (i.columnType === 'TRY_NEXT' || i.columnType === 'ACTIONS') && !i.mergedIntoId);
  }, [items]);

  const getColumnTitle = (type: RetroColumnType) => {
    const map: Record<RetroColumnType, string> = {
      WENT_WELL: t('retroBoardPage.wentWell'),
      DID_NOT_GO_WELL: t('retroBoardPage.didNotGoWell'),
      TRY_NEXT: t('retroBoardPage.tryNext'),
      ACTIONS: t('retroBoardPage.actions'),
    };
    return map[type];
  };

  const getStatusLabel = (status: RetroStatus) => {
    const map: Record<RetroStatus, string> = {
      DRAFT: `📝 ${t('retroBoardPage.draft')}`,
      OPEN: `🟢 ${t('retroBoardPage.open')}`,
      CLOSED: `✅ ${t('retroBoardPage.closed')}`,
    };
    return map[status];
  };

  useEffect(() => {
    if (!id) return;
    loadData(id);
  }, [id]);

  // Live updates via SSE — refetch items when the server broadcasts a board change for this retro.
  useEffect(() => {
    if (!id) return;
    const handler = (e: Event) => {
      const detail = (e as CustomEvent<{ retroId: number }>).detail;
      if (detail?.retroId === Number(id)) {
        retroService.getItems(id).then((res) => setItems(res.data)).catch(() => {});
      }
    };
    window.addEventListener('retro-updated', handler);
    return () => window.removeEventListener('retro-updated', handler);
  }, [id]);

  const loadData = async (retroId: number) => {
    try {
      const [retroRes, itemsRes] = await Promise.all([
        retroService.getById(retroId),
        retroService.getItems(retroId),
      ]);
      setRetro(retroRes.data);
      setItems(itemsRes.data);
    } catch (error: any) {
      console.error('Failed to load retro:', error);
      if (error.response?.status === 400 || error.response?.status === 403) {
        showError(t('retroBoardPage.retrosDisabled'));
      }
    } finally {
      setLoading(false);
    }
  };

  const handleAddItem = async (columnType: RetroColumnType) => {
    const content = newItemContent[columnType].trim();
    if (!content || !retro) return;

    try {
      const res = await retroService.createItem({
        content,
        columnType,
        retrospectiveId: retro.id,
        isAnonymous: isAnonymous[columnType],
      });
      setItems([...items, res.data]);
      setNewItemContent({ ...newItemContent, [columnType]: '' });
      setIsAnonymous({ ...isAnonymous, [columnType]: false });
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
  };

  const handleUpdateItem = async () => {
    if (!editingItem) return;

    try {
      await retroService.updateItem(editingItem.id, editingItem.content);
      setItems(items.map((item) => (item.id === editingItem.id ? { ...item, content: editingItem.content } : item)));
      setEditingItem(null);
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
  };

  const handleDeleteItem = async (itemId: number) => {
    try {
      await retroService.deleteItem(itemId);
      setItems(items.filter((item) => item.id !== itemId));
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
  };

  const handleVote = async (itemId: number) => {
    try {
      const res = await retroService.toggleVote(itemId);
      setItems(items.map((item) => (item.id === itemId ? res.data : item)));
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
  };

  const handleDislike = async (itemId: number) => {
    try {
      const res = await retroService.toggleDislike(itemId);
      setItems(items.map((item) => (item.id === itemId ? res.data : item)));
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
  };

  const startDiscussion = (itemId: number, seconds = 300) => {
    if (discussTimerRef.current) clearInterval(discussTimerRef.current);
    setDiscussingItemId(itemId);
    setDiscussTimeLeft(seconds);
    discussTimerRef.current = setInterval(() => {
      setDiscussTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(discussTimerRef.current!);
          discussTimerRef.current = null;
          setDiscussingItemId((currentId) => {
            if (currentId != null) {
              retroService.markDiscussed(currentId, true)
                .then((res) => setItems((cur) => cur.map((i) => i.id === currentId ? res.data : i)))
                .catch(() => {});
            }
            return null;
          });
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const handleMarkDiscussed = async (itemId: number, discussed: boolean) => {
    try {
      const res = await retroService.markDiscussed(itemId, discussed);
      setItems(items.map((item) => (item.id === itemId ? res.data : item)));
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
  };

  const stopDiscussion = (markAsDone = true) => {
    const itemId = discussingItemId;
    if (discussTimerRef.current) clearInterval(discussTimerRef.current);
    discussTimerRef.current = null;
    setDiscussingItemId(null);
    setDiscussTimeLeft(0);
    if (markAsDone && itemId != null) {
      handleMarkDiscussed(itemId, true);
    }
  };

  // Clean up discuss timer on unmount
  useEffect(() => () => { if (discussTimerRef.current) clearInterval(discussTimerRef.current); }, []);

  const handleMerge = async (targetItemId: number) => {
    if (!mergeDialog.sourceItem) return;
    try {
      const res = await retroService.mergeItems(targetItemId, mergeDialog.sourceItem.id);
      // Update target item and mark source as merged
      setItems(items.map((item) => {
        if (item.id === targetItemId) return res.data;
        if (item.id === mergeDialog.sourceItem?.id) return { ...item, mergedIntoId: targetItemId };
        return item;
      }));
      setMergeDialog({ open: false, sourceItem: null, columnType: null });
      showSuccess(t('retroBoardPage.itemsMerged'));
    } catch (error: any) {
      showError(error.response?.data?.message || t('retroBoardPage.mergeFailed'));
    }
  };

  const openMergeDialog = (sourceItem: RetroItem) => {
    setMergeDialog({ open: true, sourceItem, columnType: sourceItem.columnType });
  };

  const handleSelectTemplate = (template: RetroTemplate) => {
    if (items.length > 0) {
      setTemplateConfirmDialog(template);
    } else {
      applyTemplate(template);
    }
  };

  const applyTemplate = async (_template: RetroTemplate) => {
    if (!retro) return;
    try {
      // Delete all existing items
      await Promise.all(items.map((item) => retroService.deleteItem(item.id)));
      setItems([]);
      showSuccess(t('retroBoardTemplates.templateApplied'));
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
    setTemplateConfirmDialog(null);
  };

  const handleOpenRetro = async () => {
    if (!retro) return;
    try {
      const res = await retroService.open(retro.id);
      setRetro(res.data);
      showSuccess(t('retroBoardPage.retroOpened'));
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
  };

  const handleCloseRetro = async () => {
    if (!retro) return;
    try {
      const res = await retroService.close(retro.id);
      setRetro(res.data);
      showSuccess(t('retroBoardPage.retroClosed'));
      // After closing, offer to act on action items (use memoized value)
      if (actionableItems.length > 0) {
        setActOnItemsDialog(true);
      }
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
  };

  const getItemsByColumn = (columnType: RetroColumnType) => {
    return items
      .filter((item) => item.columnType === columnType && !item.mergedIntoId)
      .sort((a, b) => (b.voteCount || 0) - (a.voteCount || 0));
  };

  const getMaxVotesInColumn = (columnType: RetroColumnType) => {
    const col = items.filter((i) => i.columnType === columnType && !i.mergedIntoId);
    return col.length ? Math.max(...col.map((i) => i.voteCount || 0), 1) : 1;
  };

  const formatTime = (s: number) => {
    const m = Math.floor(s / 60).toString().padStart(2, '0');
    const sec = (s % 60).toString().padStart(2, '0');
    return `${m}:${sec}`;
  };

  const getMergeTargets = (columnType: RetroColumnType, excludeId: number) => {
    return items.filter(
      (item) => item.columnType === columnType && !item.mergedIntoId && item.id !== excludeId
    );
  };

  if (id === null) {
    return (
      <div className="p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <span className="text-sm">{t('pitchDetailPage.invalidPitchId')}</span>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!retro) {
    return (
      <div className="space-y-4">
        <p className="text-muted-foreground">{t('pitchDetailPage.pitchNotFound')}</p>
        <Button variant="outline" asChild>
          <Link to="/retros">
            <ArrowLeft className="mr-2 h-4 w-4" />
            {t('retroBoardPage.backToList')}
          </Link>
        </Button>
      </div>
    );
  }

  return (
    <TooltipProvider>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col gap-4 sm:flex-row sm:justify-between sm:items-start">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <Button variant="ghost" size="sm" asChild>
                <Link to="/retros">
                  <ArrowLeft className="mr-1 h-4 w-4" />
                  {t('retroBoardPage.backToList')}
                </Link>
              </Button>
              <h1 className="text-2xl font-bold">{retro.title}</h1>
              <Badge variant={statusConfig[retro.status].variant}>
                {getStatusLabel(retro.status)}
              </Badge>
              {isReadOnly && (
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Lock className="h-4 w-4 text-muted-foreground" />
                  </TooltipTrigger>
                  <TooltipContent>
                    {t('retroBoardPage.readOnly')}
                  </TooltipContent>
                </Tooltip>
              )}
            </div>
            <p className="text-sm text-muted-foreground">
              {t('retroListPage.cycle')}: {retro.cycleName} • {t('backlogPage.created')}: {formatLocalizedDate(new Date(retro.createdAt), i18n.language)}
              {retro.closedAt && ` • ${t('backlogPage.closed')}: ${formatLocalizedDate(new Date(retro.closedAt), i18n.language)}`}
            </p>
            {retro.notes && (
              <p className="text-sm italic text-muted-foreground">{retro.notes}</p>
            )}
          </div>
          <div className="flex gap-2">
            {!isReadOnly && (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="outline" size="sm">
                    <LayoutTemplate className="mr-2 h-4 w-4" />
                    {t('retroBoardTemplates.useTemplate')}
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  {RETRO_TEMPLATES.map((template) => (
                    <DropdownMenuItem
                      key={template.key}
                      onClick={() => handleSelectTemplate(template)}
                    >
                      {t(template.titleKey)}
                    </DropdownMenuItem>
                  ))}
                </DropdownMenuContent>
              </DropdownMenu>
            )}
            {retro.status === 'DRAFT' && (
              <Button variant="default" className="bg-green-600 hover:bg-green-700" onClick={handleOpenRetro}>
                <Play className="mr-2 h-4 w-4" />
                {t('retroBoardPage.openRetro')}
              </Button>
            )}
            {retro.status === 'OPEN' && canManageRetro && (
              <Button variant="default" className="bg-amber-600 hover:bg-amber-700" onClick={handleCloseRetro}>
                <Square className="mr-2 h-4 w-4" />
                {t('retroBoardPage.closeRetro')}
              </Button>
            )}
            {retro.status === 'CLOSED' && actionableItems.length > 0 && (
              <Button variant="default" onClick={() => setActOnItemsDialog(true)}>
                <Rocket className="mr-2 h-4 w-4" />
                {t('retroBoardPage.actOnItems', 'Act on Items')}
              </Button>
            )}
          </div>
        </div>

        {isReadOnly && (
          <div className="rounded-md border border-blue-200 bg-blue-50 dark:bg-blue-950/30 dark:border-blue-800 p-4">
            <p className="text-sm text-blue-800 dark:text-blue-200">
              {t('retroBoardPage.confirmCloseDesc')}
            </p>
          </div>
        )}

        {/* Retro Board */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 min-h-[300px] md:min-h-[500px]">
          {columns.map((column) => (
            <div
              key={column.type}
              className={cn(
                'rounded-lg border bg-card p-4 border-t-4 flex flex-col',
                column.color
              )}
            >
              {/* Column Header */}
              <h3 className="text-lg font-semibold mb-4">
                {column.emoji} {getColumnTitle(column.type)}
              </h3>

              {/* Items */}
              <div className="flex-1 space-y-2 mb-4">
                {getItemsByColumn(column.type).map((item) => {
                  const isDiscussing = discussingItemId === item.id;
                  const fillPct = Math.round(((item.voteCount || 0) / getMaxVotesInColumn(column.type)) * 100);
                  const timeIsUp = isDiscussing && discussTimeLeft === 0;
                  const isDiscussed = !!item.discussed;
                  return (
                    <Card
                      key={item.id}
                      className={cn(
                        'relative overflow-hidden transition-shadow',
                        item.mergedItemIds && item.mergedItemIds.length > 0 && 'border-l-4 border-l-blue-500',
                        isDiscussing && 'ring-2 ring-amber-400',
                        isDiscussed && !isDiscussing && 'opacity-75'
                      )}
                    >
                      {/* Vote fill bar */}
                      {fillPct > 0 && (
                        <div
                          className="absolute inset-0 bg-primary/8 transition-all duration-500"
                          style={{ width: `${fillPct}%` }}
                        />
                      )}

                      {/* Discuss countdown overlay */}
                      {isDiscussing && (
                        <div className={cn(
                          'absolute top-1 right-1 flex items-center gap-1 rounded px-1.5 py-0.5 text-xs font-mono font-bold z-10',
                          timeIsUp
                            ? 'bg-red-500 text-white animate-pulse'
                            : 'bg-amber-400 text-amber-900'
                        )}>
                          <Timer className="h-3 w-3" />
                          {timeIsUp ? t('retroBoardPage.timeUp') : formatTime(discussTimeLeft)}
                        </div>
                      )}

                      <CardContent className="p-3 relative">
                        {editingItem?.id === item.id ? (
                          <div className="space-y-2">
                            <Textarea
                              value={editingItem.content}
                              onChange={(e) => setEditingItem({ ...editingItem, content: e.target.value })}
                              className="min-h-[60px]"
                              autoFocus
                            />
                            <div className="flex justify-end gap-1">
                              <Button variant="ghost" size="icon" className="h-8 w-8" onClick={handleUpdateItem}>
                                <Check className="h-4 w-4" />
                              </Button>
                              <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => setEditingItem(null)}>
                                <X className="h-4 w-4" />
                              </Button>
                            </div>
                          </div>
                        ) : (
                          <div className="space-y-2">
                            <div className="flex items-start gap-2">
                              <p className={cn('text-sm flex-1', isDiscussed && 'line-through text-muted-foreground')}>{item.content}</p>
                              {isDiscussed && (
                                <Badge variant="success" className="shrink-0 gap-1 text-xs px-1.5 py-0">
                                  <CheckCircle2 className="h-3 w-3" />
                                  {t('retroBoardPage.discussed')}
                                </Badge>
                              )}
                            </div>

                            {item.mergedItemIds && item.mergedItemIds.length > 0 && (
                              <Tooltip>
                                <TooltipTrigger asChild>
                                  <Badge variant="info" className="text-xs cursor-help">
                                    +{item.mergedItemIds.length} {t('retroBoardPage.merge').toLowerCase()}
                                  </Badge>
                                </TooltipTrigger>
                                <TooltipContent className="max-w-xs">
                                  <div className="space-y-1">
                                    <p className="font-semibold text-xs">{t('retroBoardPage.merge')}:</p>
                                    {items
                                      .filter((i) => item.mergedItemIds?.includes(i.id))
                                      .map((mergedItem) => (
                                        <p key={mergedItem.id} className="text-xs">
                                          • {mergedItem.content.substring(0, 50)}...
                                        </p>
                                      ))}
                                  </div>
                                </TooltipContent>
                              </Tooltip>
                            )}

                            <div className="flex items-center justify-between">
                              {/* Left: reactions + author */}
                              <div className="flex items-center gap-1">
                                {/* Like */}
                                {!isReadOnly && retro?.status === 'OPEN' && (
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <Button
                                        variant="ghost"
                                        size="icon"
                                        className={cn('h-7 w-7 relative', item.hasVoted && 'text-primary')}
                                        onClick={() => handleVote(item.id)}
                                      >
                                        <ThumbsUp className={cn('h-3.5 w-3.5', item.hasVoted && 'fill-current')} />
                                        {(item.voteCount || 0) > 0 && (
                                          <span className="absolute -top-1 -right-1 flex h-3.5 w-3.5 items-center justify-center rounded-full bg-primary text-[9px] text-primary-foreground font-bold">
                                            {item.voteCount}
                                          </span>
                                        )}
                                      </Button>
                                    </TooltipTrigger>
                                    <TooltipContent>{item.hasVoted ? t('common.delete') : t('retroBoardPage.vote')}</TooltipContent>
                                  </Tooltip>
                                )}
                                {/* Dislike */}
                                {!isReadOnly && retro?.status === 'OPEN' && (
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <Button
                                        variant="ghost"
                                        size="icon"
                                        className={cn('h-7 w-7 relative', item.hasDisliked && 'text-destructive')}
                                        onClick={() => handleDislike(item.id)}
                                      >
                                        <ThumbsDown className={cn('h-3.5 w-3.5', item.hasDisliked && 'fill-current')} />
                                        {(item.dislikeCount || 0) > 0 && (
                                          <span className="absolute -top-1 -right-1 flex h-3.5 w-3.5 items-center justify-center rounded-full bg-destructive text-[9px] text-destructive-foreground font-bold">
                                            {item.dislikeCount}
                                          </span>
                                        )}
                                      </Button>
                                    </TooltipTrigger>
                                    <TooltipContent>{item.hasDisliked ? t('common.delete') : t('retroBoardPage.dislike')}</TooltipContent>
                                  </Tooltip>
                                )}
                                {isReadOnly && (item.voteCount > 0 || item.dislikeCount > 0) && (
                                  <div className="flex items-center gap-1">
                                    {item.voteCount > 0 && (
                                      <Badge variant="secondary" className="gap-1 text-xs px-1.5 py-0">
                                        <ThumbsUp className="h-3 w-3" />{item.voteCount}
                                      </Badge>
                                    )}
                                    {item.dislikeCount > 0 && (
                                      <Badge variant="destructive" className="gap-1 text-xs px-1.5 py-0">
                                        <ThumbsDown className="h-3 w-3" />{item.dislikeCount}
                                      </Badge>
                                    )}
                                  </div>
                                )}
                                <span className="text-xs text-muted-foreground ml-1">
                                  {item.authorName || t('common.anonymous')}
                                </span>
                              </div>

                              {/* Right: discuss + merge + edit/delete */}
                              {!isReadOnly && (
                                <div className="flex items-center">
                                  {/* Mark as discussed toggle */}
                                  {retro?.status === 'OPEN' && (
                                    <Tooltip>
                                      <TooltipTrigger asChild>
                                        <Button
                                          variant="ghost"
                                          size="icon"
                                          className={cn('h-7 w-7', isDiscussed && 'text-green-500')}
                                          onClick={() => handleMarkDiscussed(item.id, !isDiscussed)}
                                        >
                                          <CheckCircle2 className={cn('h-3.5 w-3.5', isDiscussed && 'fill-current')} />
                                        </Button>
                                      </TooltipTrigger>
                                      <TooltipContent>
                                        {isDiscussed ? t('retroBoardPage.unmarkDiscussed') : t('retroBoardPage.markDiscussed')}
                                      </TooltipContent>
                                    </Tooltip>
                                  )}
                                  {/* Discuss timer */}
                                  {retro?.status === 'OPEN' && (
                                    <Tooltip>
                                      <TooltipTrigger asChild>
                                        <Button
                                          variant="ghost"
                                          size="icon"
                                          className={cn('h-7 w-7', isDiscussing && 'text-amber-500')}
                                          onClick={() => isDiscussing ? stopDiscussion() : startDiscussion(item.id)}
                                        >
                                          {isDiscussing ? <TimerOff className="h-3.5 w-3.5" /> : <Timer className="h-3.5 w-3.5" />}
                                        </Button>
                                      </TooltipTrigger>
                                      <TooltipContent>
                                        {isDiscussing ? t('retroBoardPage.stopDiscussion') : t('retroBoardPage.discussItem')}
                                      </TooltipContent>
                                    </Tooltip>
                                  )}
                                  {/* Merge (admin/manager only) */}
                                  {canManageRetro && retro?.status === 'OPEN' && (
                                    <Tooltip>
                                      <TooltipTrigger asChild>
                                        <Button
                                          variant="ghost"
                                          size="icon"
                                          className="h-7 w-7"
                                          onClick={() => openMergeDialog(item)}
                                        >
                                          <Merge className="h-3.5 w-3.5" />
                                        </Button>
                                      </TooltipTrigger>
                                      <TooltipContent>{t('retroBoardPage.merge')}</TooltipContent>
                                    </Tooltip>
                                  )}
                                  {canEditItem(item) && (
                                    <>
                                      <Button
                                        variant="ghost"
                                        size="icon"
                                        className="h-7 w-7"
                                        onClick={() => setEditingItem({ id: item.id, content: item.content })}
                                      >
                                        <Pencil className="h-3.5 w-3.5" />
                                      </Button>
                                      <Button
                                        variant="ghost"
                                        size="icon"
                                        className="h-7 w-7 text-destructive hover:text-destructive"
                                        onClick={() => handleDeleteItem(item.id)}
                                      >
                                        <Trash2 className="h-3.5 w-3.5" />
                                      </Button>
                                    </>
                                  )}
                                </div>
                              )}
                            </div>
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  );
                })}
              </div>

              {/* Add New Item */}
              {!isReadOnly && (
                <div className="border-t pt-3 space-y-2">
                  <Textarea
                    placeholder={`${t('retroBoardPage.addItem')} ${getColumnTitle(column.type).toLowerCase()}...`}
                    value={newItemContent[column.type]}
                    onChange={(e) => setNewItemContent({ ...newItemContent, [column.type]: e.target.value })}
                    className="min-h-[60px] resize-none"
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.shiftKey) {
                        e.preventDefault();
                        handleAddItem(column.type);
                      }
                    }}
                  />
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-2">
                      <Checkbox
                        id={`anonymous-${column.type}`}
                        checked={isAnonymous[column.type]}
                        onCheckedChange={(checked) =>
                          setIsAnonymous({ ...isAnonymous, [column.type]: checked as boolean })
                        }
                      />
                      <label
                        htmlFor={`anonymous-${column.type}`}
                        className="text-sm text-muted-foreground cursor-pointer"
                      >
                        {t('retroBoardPage.anonymous')}
                      </label>
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleAddItem(column.type)}
                      disabled={!newItemContent[column.type].trim()}
                    >
                      <Plus className="mr-1 h-4 w-4" />
                      {t('retroBoardPage.addItem')}
                    </Button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>

        {/* AI Retrospective Summary */}
        {retro && (
          <RetroSummaryPanel
            cycleId={retro.cycleId}
            retroStatus={retro.status}
          />
        )}

        {/* Merge Dialog */}
        <Dialog
          open={mergeDialog.open}
          onOpenChange={(open) => {
            if (!open) setMergeDialog({ open: false, sourceItem: null, columnType: null });
          }}
        >
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>{t('retroBoardPage.merge')}</DialogTitle>
              <DialogDescription>
                {t('retroBoardPage.selectItemToMerge')} "{mergeDialog.sourceItem?.content?.substring(0, 50)}...":
              </DialogDescription>
            </DialogHeader>
            <div className="max-h-[300px] overflow-y-auto">
              {mergeDialog.sourceItem && mergeDialog.columnType && (
                <div className="space-y-1">
                  {getMergeTargets(mergeDialog.columnType, mergeDialog.sourceItem.id).map((targetItem) => (
                    <button
                      key={targetItem.id}
                      onClick={() => handleMerge(targetItem.id)}
                      className="w-full text-left p-3 rounded-md hover:bg-accent transition-colors"
                    >
                      <p className="text-sm">{targetItem.content}</p>
                      <p className="text-xs text-muted-foreground mt-1">
                        {t('common.votesByAuthor', { votes: targetItem.voteCount || 0, author: targetItem.authorName || t('common.anonymous') })}
                      </p>
                    </button>
                  ))}
                  {getMergeTargets(mergeDialog.columnType, mergeDialog.sourceItem.id).length === 0 && (
                    <p className="text-center text-muted-foreground py-4">
                      {t('retroBoardPage.selectItemToMerge')}
                    </p>
                  )}
                </div>
              )}
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setMergeDialog({ open: false, sourceItem: null, columnType: null })}
              >
                {t('common.cancel')}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Act on Items Dialog */}
        {retro && (
          <ActOnRetroItemsDialog
            open={actOnItemsDialog}
            onOpenChange={setActOnItemsDialog}
            retroId={retro.id}
            retroTitle={retro.title}
            projectId={retro.projectId}
            items={items}
            onActionComplete={() => {
              // Refresh items to show acted-on status
              retroService.getItems(retro.id).then((res) => setItems(res.data));
            }}
          />
        )}

        {/* Template Confirmation Dialog */}
        <Dialog
          open={!!templateConfirmDialog}
          onOpenChange={(open) => { if (!open) setTemplateConfirmDialog(null); }}
        >
          <DialogContent className="max-w-md">
            <DialogHeader>
              <DialogTitle>{t('retroBoardTemplates.templateConfirmReplace')}</DialogTitle>
              <DialogDescription>
                {t('retroBoardTemplates.templateConfirmReplaceDesc')}
              </DialogDescription>
            </DialogHeader>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setTemplateConfirmDialog(null)}
              >
                {t('retroBoardTemplates.templateConfirmNo')}
              </Button>
              <Button
                variant="destructive"
                onClick={() => { if (templateConfirmDialog) applyTemplate(templateConfirmDialog); }}
              >
                {t('retroBoardTemplates.templateConfirmYes')}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </TooltipProvider>
  );
}
