import { useCallback, useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Download, Film, Image as ImageIcon, Loader2, Paperclip, Trash2, UploadCloud } from 'lucide-react';
import {
  documentService,
  PlanningEntityType,
  UploadedDocument,
} from '../services/documentService';
import { useToast } from '../contexts';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from './ui/dialog';

export const PLANNING_IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'];
export const PLANNING_VIDEO_EXTENSIONS = ['mp4', 'webm', 'mov', 'avi'];
export const PLANNING_MAX_BYTES = 20 * 1024 * 1024; // keep in sync with PlanningAttachmentService.MAX_BYTES
const ACCEPT = [...PLANNING_IMAGE_EXTENSIONS, ...PLANNING_VIDEO_EXTENSIONS].map((e) => `.${e}`).join(',');

const extensionOf = (name: string) => name.split('.').pop()?.toLowerCase() ?? '';

/** i18n key of the reason a file can't be attached, or null when it's fine. */
export function planningAttachmentRejection(file: File): string | null {
  const ext = extensionOf(file.name);
  if (![...PLANNING_IMAGE_EXTENSIONS, ...PLANNING_VIDEO_EXTENSIONS].includes(ext)) {
    return 'planningAttachments.unsupportedType';
  }
  if (file.size > PLANNING_MAX_BYTES) return 'planningAttachments.tooLarge';
  return null;
}

interface PlanningAttachmentsProps {
  entityType: PlanningEntityType;
  entityId: number;
}

/**
 * Images and short clips on an epic or initiative (which otherwise only have a text description).
 * Files are fetched as authenticated blobs because a bare <img src> can't send the auth header.
 */
export default function PlanningAttachments({ entityType, entityId }: PlanningAttachmentsProps) {
  const { t } = useTranslation();
  const { showSuccess, showError } = useToast();
  const inputRef = useRef<HTMLInputElement>(null);
  const [attachments, setAttachments] = useState<UploadedDocument[]>([]);
  const [thumbs, setThumbs] = useState<Record<number, string>>({});
  const thumbsRef = useRef<Record<number, string>>({});
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [preview, setPreview] = useState<{ doc: UploadedDocument; url: string } | null>(null);

  const load = useCallback(async () => {
    try {
      const res = await documentService.getPlanningAttachments(entityType, entityId);
      setAttachments(res.data);
    } catch (error) {
      console.error('Failed to load attachments:', error);
    }
  }, [entityType, entityId]);

  useEffect(() => {
    load();
  }, [load]);

  // Fetch a blob URL for each image we haven't loaded yet.
  useEffect(() => {
    let cancelled = false;
    attachments
      .filter((a) => PLANNING_IMAGE_EXTENSIONS.includes(a.fileType.toLowerCase()) && !thumbsRef.current[a.id])
      .forEach(async (a) => {
        try {
          const url = await documentService.fetchAttachmentBlobUrl(a.id);
          if (cancelled) {
            URL.revokeObjectURL(url);
            return;
          }
          thumbsRef.current = { ...thumbsRef.current, [a.id]: url };
          setThumbs(thumbsRef.current);
        } catch (error) {
          console.error('Failed to load attachment preview:', error);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [attachments]);

  // Release blob URLs on unmount.
  useEffect(
    () => () => {
      Object.values(thumbsRef.current).forEach((u) => URL.revokeObjectURL(u));
    },
    [],
  );

  const upload = async (files: File[]) => {
    if (files.length === 0) return;
    setUploading(true);
    let ok = 0;
    for (const file of files) {
      const rejection = planningAttachmentRejection(file);
      if (rejection) {
        showError(`${file.name}: ${t(rejection)}`);
        continue;
      }
      try {
        await documentService.uploadPlanningAttachment(entityType, entityId, file);
        ok += 1;
      } catch (error) {
        console.error('Attachment upload failed:', error);
        showError(`${file.name}: ${t('planningAttachments.uploadFailed')}`);
      }
    }
    setUploading(false);
    if (ok > 0) {
      showSuccess(t('planningAttachments.uploaded', { count: ok }));
      await load();
    }
  };

  const remove = async (doc: UploadedDocument) => {
    try {
      await documentService.deletePlanningAttachment(entityType, entityId, doc.id);
      if (thumbsRef.current[doc.id]) URL.revokeObjectURL(thumbsRef.current[doc.id]);
      const { [doc.id]: _gone, ...rest } = thumbsRef.current;
      thumbsRef.current = rest;
      setThumbs(rest);
      setAttachments((prev) => prev.filter((a) => a.id !== doc.id));
    } catch (error) {
      console.error('Failed to delete attachment:', error);
      showError(t('planningAttachments.deleteFailed'));
    }
  };

  const openPreview = async (doc: UploadedDocument) => {
    try {
      const url = thumbsRef.current[doc.id] ?? (await documentService.fetchAttachmentBlobUrl(doc.id));
      setPreview({ doc, url });
    } catch (error) {
      console.error('Failed to open attachment:', error);
      showError(t('planningAttachments.openFailed'));
    }
  };

  const isImage = (doc: UploadedDocument) => PLANNING_IMAGE_EXTENSIONS.includes(doc.fileType.toLowerCase());

  return (
    <Card data-testid="planning-attachments">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Paperclip className="h-5 w-5" />
          {t('planningAttachments.title')} ({attachments.length})
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div
          role="button"
          tabIndex={0}
          data-testid="planning-attachments-dropzone"
          onClick={() => inputRef.current?.click()}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              inputRef.current?.click();
            }
          }}
          onDragOver={(e) => {
            e.preventDefault();
            setDragOver(true);
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={(e) => {
            e.preventDefault();
            setDragOver(false);
            upload(Array.from(e.dataTransfer.files));
          }}
          className={`flex cursor-pointer flex-col items-center gap-1 rounded-lg border-2 border-dashed p-6 text-center text-sm text-muted-foreground transition-colors ${
            dragOver ? 'border-primary bg-primary/5' : 'hover:bg-muted/40'
          }`}
        >
          {uploading ? <Loader2 className="h-6 w-6 animate-spin" /> : <UploadCloud className="h-6 w-6" />}
          <span>{t('planningAttachments.dropHint')}</span>
          <span className="text-xs">{t('planningAttachments.limits')}</span>
          <input
            ref={inputRef}
            type="file"
            multiple
            accept={ACCEPT}
            className="hidden"
            data-testid="planning-attachments-input"
            onChange={(e) => {
              upload(Array.from(e.target.files ?? []));
              e.target.value = '';
            }}
          />
        </div>

        {attachments.length === 0 ? (
          <p className="text-sm text-muted-foreground">{t('planningAttachments.empty')}</p>
        ) : (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            {attachments.map((doc) => (
              <li key={doc.id} className="group relative overflow-hidden rounded-lg border bg-muted/30">
                <button
                  type="button"
                  onClick={() => openPreview(doc)}
                  className="flex aspect-video w-full items-center justify-center overflow-hidden"
                  aria-label={doc.originalFileName}
                >
                  {isImage(doc) && thumbs[doc.id] ? (
                    <img src={thumbs[doc.id]} alt={doc.originalFileName} className="h-full w-full object-cover" />
                  ) : isImage(doc) ? (
                    <ImageIcon className="h-8 w-8 text-muted-foreground" />
                  ) : (
                    <Film className="h-8 w-8 text-muted-foreground" />
                  )}
                </button>
                <div className="flex items-center gap-1 p-2">
                  <span className="min-w-0 flex-1 truncate text-xs" title={doc.originalFileName}>
                    {doc.originalFileName}
                  </span>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7"
                    onClick={() => documentService.downloadAttachment(doc.id, doc.originalFileName)}
                    aria-label={t('planningAttachments.download')}
                    title={t('planningAttachments.download')}
                  >
                    <Download className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7"
                    onClick={() => remove(doc)}
                    aria-label={t('planningAttachments.remove')}
                    title={t('planningAttachments.remove')}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </CardContent>

      <Dialog
        open={preview !== null}
        onOpenChange={(open) => {
          if (!open) {
            // A freshly fetched (non-thumbnail) blob is ours to release.
            if (preview && !thumbsRef.current[preview.doc.id]) URL.revokeObjectURL(preview.url);
            setPreview(null);
          }
        }}
      >
        <DialogContent className="max-w-4xl">
          <DialogHeader>
            <DialogTitle className="truncate">{preview?.doc.originalFileName}</DialogTitle>
          </DialogHeader>
          {preview &&
            (isImage(preview.doc) ? (
              <img src={preview.url} alt={preview.doc.originalFileName} className="max-h-[70vh] w-full object-contain" />
            ) : (
              <video src={preview.url} controls className="max-h-[70vh] w-full" />
            ))}
        </DialogContent>
      </Dialog>
    </Card>
  );
}
