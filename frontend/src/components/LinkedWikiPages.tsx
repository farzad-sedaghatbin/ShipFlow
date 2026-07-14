import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { BookOpen, ExternalLink, Loader2, Trash2 } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import {
  wikiLinkService,
  LinkedWikiPageDTO,
  WikiLinkEntityType,
} from '../services/wikiLinkService';
import { wikiService, WikiPageSearchDTO } from '../services/wikiService';
import { useDebounce } from '../hooks/useDebounce';
import { getUserFriendlyError } from '../utils/errorMessages';

interface LinkedWikiPagesProps {
  entityType: WikiLinkEntityType;
  entityId: number;
}

export default function LinkedWikiPages({ entityType, entityId }: LinkedWikiPagesProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [searchQuery, setSearchQuery] = useState('');
  const debouncedQuery = useDebounce(searchQuery, 300);
  const [unlinkTarget, setUnlinkTarget] = useState<LinkedWikiPageDTO | null>(null);

  const queryKey = ['wiki-links', entityType, entityId];

  const { data: linkedPages = [], isLoading } = useQuery<LinkedWikiPageDTO[]>({
    queryKey,
    queryFn: () =>
      wikiLinkService.listLinkedWikiPages(entityType, entityId).then((r) => r.data),
  });

  const { data: searchResults = [], isFetching: isSearching } = useQuery<WikiPageSearchDTO[]>({
    queryKey: ['wiki-page-search', debouncedQuery],
    queryFn: () => wikiService.searchPages(debouncedQuery).then((r) => r.data),
    enabled: debouncedQuery.trim().length > 0,
  });

  const linkedPageIds = new Set(linkedPages.map((p) => p.wikiPageId));
  const availableResults = searchResults.filter((p) => !linkedPageIds.has(p.id));

  const linkMutation = useMutation({
    mutationFn: (wikiPageId: number) =>
      wikiLinkService.linkWikiPage(entityType, entityId, wikiPageId).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setSearchQuery('');
      toast.success(t('wikiLinks.linked'));
    },
    onError: (err: unknown) => {
      toast.error(getUserFriendlyError(err, t('wikiLinks.linkError')));
    },
  });

  const unlinkMutation = useMutation({
    mutationFn: (wikiPageId: number) =>
      wikiLinkService.unlinkWikiPage(entityType, entityId, wikiPageId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      toast.success(t('wikiLinks.unlinked'));
    },
    onError: (err: unknown) => {
      toast.error(getUserFriendlyError(err, t('wikiLinks.unlinkError')));
    },
    onSettled: () => setUnlinkTarget(null),
  });

  function openWikiPage(page: LinkedWikiPageDTO) {
    navigate(`/wiki/${page.spaceId}/${page.wikiPageId}`);
  }

  return (
    <Card data-tour="linked-wiki-pages">
      <CardHeader>
        <CardTitle className="flex items-center gap-2 text-sm font-semibold">
          <BookOpen className="h-4 w-4" />
          {t('wikiLinks.title')}
          {linkedPages.length > 0 && (
            <span className="rounded-full bg-muted px-2 py-0.5 text-xs">
              {linkedPages.length}
            </span>
          )}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <p className="text-sm text-muted-foreground">{t('wikiLinks.description')}</p>

        {isLoading ? (
          <div className="flex justify-center py-4">
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        ) : linkedPages.length === 0 ? (
          <p className="py-2 text-center text-sm text-muted-foreground">
            {t('wikiLinks.empty')}
          </p>
        ) : (
          <ul className="divide-y divide-border rounded-md border">
            {linkedPages.map((page) => (
              <li key={page.linkId} className="flex items-center gap-3 px-3 py-2.5">
                <BookOpen className="h-4 w-4 shrink-0 text-muted-foreground" />
                <div className="min-w-0 flex-1">
                  <button
                    type="button"
                    className="truncate text-left text-sm font-medium hover:underline"
                    onClick={() => openWikiPage(page)}
                  >
                    {page.title}
                  </button>
                  <p className="text-xs text-muted-foreground">
                    {page.spaceName && `${page.spaceName} · `}
                    {t('wikiLinks.linkedAgo', {
                      time: formatDistanceToNow(new Date(page.linkedAt), { addSuffix: true }),
                    })}
                    {page.linkedByName && ` · ${page.linkedByName}`}
                  </p>
                </div>
                <div className="flex shrink-0 items-center gap-1">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7"
                    title={t('wikiLinks.openPage')}
                    onClick={() => openWikiPage(page)}
                  >
                    <ExternalLink className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7 text-destructive hover:text-destructive"
                    title={t('wikiLinks.unlink')}
                    onClick={() => setUnlinkTarget(page)}
                    disabled={unlinkMutation.isPending}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}

        <div className="space-y-1.5">
          <Input
            placeholder={t('wikiLinks.searchPlaceholder')}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          {debouncedQuery.trim().length > 0 && (
            <div className="max-h-40 divide-y overflow-y-auto rounded-md border">
              {isSearching ? (
                <div className="flex justify-center py-3">
                  <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                </div>
              ) : availableResults.length === 0 ? (
                <p className="px-3 py-2 text-sm text-muted-foreground">
                  {t('wikiLinks.noResults')}
                </p>
              ) : (
                availableResults.map((page) => (
                  <button
                    key={page.id}
                    type="button"
                    className="flex w-full items-center justify-between px-3 py-2 text-left text-sm hover:bg-muted disabled:opacity-50"
                    onClick={() => linkMutation.mutate(page.id)}
                    disabled={linkMutation.isPending}
                  >
                    <span className="truncate">{page.title}</span>
                    <span className="shrink-0 text-xs text-muted-foreground">
                      {t('wikiLinks.addLink')}
                    </span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>
      </CardContent>

      <ConfirmDialog
        open={!!unlinkTarget}
        onOpenChange={(open) => !open && setUnlinkTarget(null)}
        title={t('wikiLinks.unlinkConfirmTitle')}
        description={t('wikiLinks.unlinkConfirmDescription', {
          title: unlinkTarget?.title ?? '',
        })}
        confirmLabel={t('wikiLinks.unlink')}
        cancelLabel={t('common.cancel')}
        onConfirm={() => unlinkTarget && unlinkMutation.mutate(unlinkTarget.wikiPageId)}
        variant="destructive"
        loading={unlinkMutation.isPending}
      />
    </Card>
  );
}
