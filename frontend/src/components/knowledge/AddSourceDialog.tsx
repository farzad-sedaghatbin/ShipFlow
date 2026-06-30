import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { FileText, Link as LinkIcon, Github, Lock } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { cn } from '@/lib/utils';
import { knowledgeService } from '../../services/knowledgeService';
import type {
  CreateKnowledgeSourceRequest,
  KnowledgeProviderType,
} from '../../types/knowledge';
import { useToast } from '../../contexts';

interface Props {
  onClose: () => void;
}

type ProviderChoice = {
  type: KnowledgeProviderType;
  enabled: boolean;
  icon: React.ElementType;
};

const PROVIDERS: ProviderChoice[] = [
  { type: 'FILE_UPLOAD', enabled: true, icon: FileText },
  { type: 'URL', enabled: true, icon: LinkIcon },
  { type: 'GITHUB', enabled: false, icon: Github },
  { type: 'CONFLUENCE', enabled: false, icon: Lock },
  { type: 'NOTION', enabled: false, icon: Lock },
  { type: 'GOOGLE_DRIVE', enabled: false, icon: Lock },
];

export function AddSourceDialog({ onClose }: Props) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const { showToast } = useToast();

  const [providerType, setProviderType] =
    useState<KnowledgeProviderType>('FILE_UPLOAD');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [url, setUrl] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);

  const create = useMutation({
    mutationFn: async () => {
      const meta: CreateKnowledgeSourceRequest = {
        name: name.trim(),
        description: description.trim() || undefined,
        providerType,
        scope: 'ORG',
        config: providerType === 'URL' ? { url: url.trim() } : {},
      };

      if (providerType === 'FILE_UPLOAD') {
        if (!file) {
          setFileError(t('knowledgeCenter.fileRequired'));
          throw new Error('file required');
        }
        return (await knowledgeService.createWithFile(meta, file)).data;
      }
      return (await knowledgeService.create(meta)).data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['knowledge'] });
      onClose();
    },
    onError: (err) => {
      if ((err as Error).message !== 'file required') {
        showToast(
          (err as Error).message ?? t('common.error'),
          'error'
        );
      }
    },
  });

  const canSubmit =
    name.trim().length > 0 &&
    (providerType !== 'URL' || url.trim().length > 0) &&
    (providerType !== 'FILE_UPLOAD' || file != null);

  return (
    <Dialog open onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{t('knowledgeCenter.addSource')}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-2">
          <div>
            <Label className="mb-2 block text-sm font-medium">
              {t('knowledgeCenter.providerType')}
            </Label>
            <div className="grid grid-cols-2 gap-2">
              {PROVIDERS.map(({ type, enabled, icon: Icon }) => {
                const active = providerType === type;
                return (
                  <button
                    key={type}
                    type="button"
                    disabled={!enabled}
                    onClick={() => enabled && setProviderType(type)}
                    className={cn(
                      'flex items-center gap-2 rounded-md border px-3 py-2 text-sm text-start transition-colors',
                      active && enabled
                        ? 'border-primary/40 bg-primary/10 text-primary'
                        : 'border-border bg-background hover:bg-accent',
                      !enabled && 'cursor-not-allowed opacity-60'
                    )}
                  >
                    <Icon className="h-4 w-4 flex-shrink-0" />
                    <span className="flex-1 truncate">
                      {t(`knowledgeCenter.provider.${type}`)}
                    </span>
                    {!enabled && (
                      <span className="text-xs text-muted-foreground">
                        {t('knowledgeCenter.comingSoon')}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          <div>
            <Label htmlFor="ks-name" className="mb-1 block text-sm font-medium">
              {t('knowledgeCenter.name')}
            </Label>
            <Input
              id="ks-name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t('knowledgeCenter.namePlaceholder')}
            />
          </div>

          <div>
            <Label
              htmlFor="ks-desc"
              className="mb-1 block text-sm font-medium"
            >
              {t('knowledgeCenter.description')}
            </Label>
            <Textarea
              id="ks-desc"
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('knowledgeCenter.descriptionPlaceholder')}
            />
          </div>

          {providerType === 'URL' && (
            <div>
              <Label htmlFor="ks-url" className="mb-1 block text-sm font-medium">
                {t('knowledgeCenter.url')}
              </Label>
              <Input
                id="ks-url"
                type="url"
                value={url}
                onChange={(e) => setUrl(e.target.value)}
                placeholder="https://example.com/docs"
              />
            </div>
          )}

          {providerType === 'FILE_UPLOAD' && (
            <div>
              <Label htmlFor="ks-file" className="mb-1 block text-sm font-medium">
                {t('knowledgeCenter.file')}
              </Label>
              <Input
                id="ks-file"
                type="file"
                onChange={(e) => {
                  setFileError(null);
                  setFile(e.target.files?.[0] ?? null);
                }}
              />
              {fileError && (
                <p className="mt-1 text-xs text-destructive">{fileError}</p>
              )}
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="ghost" onClick={onClose} disabled={create.isPending}>
            {t('knowledgeCenter.cancel')}
          </Button>
          <Button
            onClick={() => create.mutate()}
            disabled={!canSubmit || create.isPending}
          >
            {t('knowledgeCenter.create')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
