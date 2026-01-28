import { useEffect, useState } from 'react';
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
  Merge,
  Loader2,
} from 'lucide-react';
import { retroService } from '../services/retroService';

import { usePermission } from '../hooks/usePermission';
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
import { useToast } from '../contexts';

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

  const { hasPermissionSync } = usePermission();
  const canManageRetro = hasPermissionSync('RETROSPECTIVE', 'MANAGE');
  const isReadOnly = retro?.status === 'CLOSED';

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
    if (id) {
      loadData(id);
    }
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
    } catch (error) {
      showError(t('retroBoardPage.saveFailed'));
    }
  };

  const getItemsByColumn = (columnType: RetroColumnType) => {
    return items
      .filter((item) => item.columnType === columnType && !item.mergedIntoId)
      .sort((a, b) => (b.voteCount || 0) - (a.voteCount || 0)); // Sort by votes descending
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
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 min-h-[500px]">
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
                {getItemsByColumn(column.type).map((item) => (
                  <Card
                    key={item.id}
                    className={cn(
                      'relative',
                      item.mergedItemIds && item.mergedItemIds.length > 0 && 'border-l-4 border-l-blue-500'
                    )}
                  >
                    <CardContent className="p-3">
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
                          <p className="text-sm">{item.content}</p>

                          {/* Show merged items indicator */}
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
                            <div className="flex items-center gap-2">
                              {/* Vote button */}
                              {!isReadOnly && retro?.status === 'OPEN' && (
                                <Tooltip>
                                  <TooltipTrigger asChild>
                                    <Button
                                      variant="ghost"
                                      size="icon"
                                      className={cn(
                                        'h-8 w-8 relative',
                                        item.hasVoted && 'text-primary'
                                      )}
                                      onClick={() => handleVote(item.id)}
                                    >
                                      <ThumbsUp className={cn('h-4 w-4', item.hasVoted && 'fill-current')} />
                                      {(item.voteCount || 0) > 0 && (
                                        <span className="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-primary text-[10px] text-primary-foreground">
                                          {item.voteCount}
                                        </span>
                                      )}
                                    </Button>
                                  </TooltipTrigger>
                                  <TooltipContent>
                                    {item.hasVoted ? t('common.delete') : t('retroBoardPage.vote')}
                                  </TooltipContent>
                                </Tooltip>
                              )}
                              {isReadOnly && item.voteCount > 0 && (
                                <Badge variant="secondary" className="gap-1">
                                  <ThumbsUp className="h-3 w-3" />
                                  {item.voteCount}
                                </Badge>
                              )}
                              <span className="text-xs text-muted-foreground">
                                {item.authorName || 'Anonymous'}
                              </span>
                            </div>
                            {!isReadOnly && (
                              <div className="flex items-center">
                                {/* Merge button (admin only) */}
                                {canManageRetro && retro?.status === 'OPEN' && (
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <Button
                                        variant="ghost"
                                        size="icon"
                                        className="h-8 w-8"
                                        onClick={() => openMergeDialog(item)}
                                      >
                                        <Merge className="h-4 w-4" />
                                      </Button>
                                    </TooltipTrigger>
                                    <TooltipContent>{t('retroBoardPage.merge')}</TooltipContent>
                                  </Tooltip>
                                )}
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8"
                                  onClick={() => setEditingItem({ id: item.id, content: item.content })}
                                >
                                  <Pencil className="h-4 w-4" />
                                </Button>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8 text-destructive hover:text-destructive"
                                  onClick={() => handleDeleteItem(item.id)}
                                >
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </CardContent>
                  </Card>
                ))}
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
                        {targetItem.voteCount || 0} votes • by {targetItem.authorName || 'Anonymous'}
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
      </div>
    </TooltipProvider>
  );
}
