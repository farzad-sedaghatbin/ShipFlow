import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Paperclip,
  UploadCloud,
  Trash2,
  Download,
  FileText,
  Image,
  File,
  Loader2,
  ChevronDown,
  ChevronUp,
  Eye,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { TaskAttachment } from '../types';
import { taskAttachmentService } from '../services/taskAttachmentService';
import { useAuth } from '../contexts';

export const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
export const ALLOWED_TYPES = [
  'image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml',
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'application/msword',
  'text/plain', 'text/markdown',
  'text/html', 'text/csv',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/json',
  'application/zip', 'application/x-zip-compressed',
];

/** Extensions accepted even when the browser reports no / a generic MIME type (common for .xlsx, .html, .csv). */
const ALLOWED_EXTENSIONS = ['html', 'htm', 'csv', 'xls', 'xlsx', 'json', 'zip', 'md', 'txt', 'pdf', 'doc', 'docx'];

/** Accept list for a file input: the allowed MIME types plus extensions browsers often leave untyped. */
export const ATTACHMENT_ACCEPT = [...ALLOWED_TYPES, ...ALLOWED_EXTENSIONS.map((e) => `.${e}`)].join(',');

/** Returns the i18n key of the reason a file can't be attached, or null when it's fine. */
export function attachmentRejection(file: File): string | null {
  if (file.size > MAX_FILE_SIZE) return 'attachments.tooLarge';
  const ext = file.name.includes('.') ? file.name.split('.').pop()!.toLowerCase() : '';
  if (!ALLOWED_TYPES.includes(file.type) && !ALLOWED_EXTENSIONS.includes(ext)) return 'attachments.unsupportedType';
  return null;
}

export function fileIcon(contentType: string) {
  if (contentType.startsWith('image/')) return <Image className="h-4 w-4 text-blue-500" />;
  if (contentType === 'application/pdf') return <FileText className="h-4 w-4 text-red-500" />;
  if (contentType === 'application/json') return <FileText className="h-4 w-4 text-amber-500" />;
  if (contentType === 'application/zip' || contentType === 'application/x-zip-compressed') {
    return <FileText className="h-4 w-4 text-purple-500" />;
  }
  return <File className="h-4 w-4 text-muted-foreground" />;
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Images the browser can show inline. They render through <img> from a blob, never as a document. */
export const isPreviewable = (contentType: string) => contentType.startsWith('image/');

function ImagePreview({ taskId, attachment, onClose }: { taskId: number; attachment: TaskAttachment | null; onClose: () => void }) {
  const { t } = useTranslation();
  const [src, setSrc] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    if (!attachment) return;
    let url: string | null = null;
    let cancelled = false;
    setSrc(null);
    setFailed(false);
    taskAttachmentService.fetchBlob(taskId, attachment.id)
      .then((blob) => { if (cancelled) return; url = window.URL.createObjectURL(blob); setSrc(url); })
      .catch(() => { if (!cancelled) setFailed(true); });
    return () => { cancelled = true; if (url) window.URL.revokeObjectURL(url); };
  }, [taskId, attachment]);

  return (
    <Dialog open={!!attachment} onOpenChange={(open) => { if (!open) onClose(); }}>
      <DialogContent className="max-w-4xl">
        <DialogHeader>
          <DialogTitle className="truncate">{attachment?.fileName}</DialogTitle>
        </DialogHeader>
        <div className="flex min-h-[200px] items-center justify-center">
          {failed ? (
            <p className="text-sm text-destructive">{t('attachments.downloadError')}</p>
          ) : src ? (
            <img src={src} alt={attachment?.fileName} className="max-h-[75vh] max-w-full object-contain" />
          ) : (
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}

interface Props {
  taskId: number;
}

export default function TaskAttachments({ taskId }: Props) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [collapsed, setCollapsed] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [previewing, setPreviewing] = useState<TaskAttachment | null>(null);

  const queryKey = ['task-attachments', taskId];

  const { data: attachments = [], isLoading } = useQuery<TaskAttachment[]>({
    queryKey,
    queryFn: () => taskAttachmentService.getAttachments(taskId).then((r) => r.data),
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) =>
      taskAttachmentService
        .uploadAttachment(taskId, file, setUploadProgress)
        .then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      setUploadProgress(null);
      toast.success(t('attachments.uploaded'));
    },
    onError: (err: unknown) => {
      setUploadProgress(null);
      const msg = err instanceof Error ? err.message : t('attachments.uploadError');
      toast.error(msg);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (attachmentId: number) =>
      taskAttachmentService.deleteAttachment(taskId, attachmentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      toast.success(t('attachments.deleted'));
    },
    onError: () => toast.error(t('attachments.deleteError')),
  });

  function handleFiles(files: FileList | null) {
    if (!files || files.length === 0) return;
    const file = files[0];
    const rejection = attachmentRejection(file);
    if (rejection) {
      toast.error(t(rejection));
      return;
    }
    uploadMutation.mutate(file);
  }

  function canDelete(a: TaskAttachment) {
    return (
      user?.role === 'ADMIN' ||
      (user?.userId !== undefined && a.uploadedById === user.userId)
    );
  }

  return (
    <Card>
      <CardHeader className="p-0">
        <button
          type="button"
          className="flex w-full cursor-pointer select-none items-center justify-between p-6 text-left"
          aria-expanded={!collapsed}
          onClick={() => setCollapsed((c) => !c)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              setCollapsed((c) => !c);
            }
          }}
        >
          <CardTitle className="flex items-center gap-2 text-sm font-semibold">
            <Paperclip className="h-4 w-4" />
            {t('attachments.title')}
            {attachments.length > 0 && (
              <span className="rounded-full bg-muted px-2 py-0.5 text-xs">
                {attachments.length}
              </span>
            )}
          </CardTitle>
          {collapsed ? (
            <ChevronDown className="h-4 w-4 text-muted-foreground" />
          ) : (
            <ChevronUp className="h-4 w-4 text-muted-foreground" />
          )}
        </button>
      </CardHeader>

      {!collapsed && (
        <CardContent className="space-y-4">
          {/* Drop zone */}
          <div
            className={`flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-6 transition-colors ${
              dragging ? 'border-primary bg-primary/5' : 'border-muted-foreground/30 hover:border-primary/50'
            }`}
            onDragOver={(e) => {
              e.preventDefault();
              setDragging(true);
            }}
            onDragLeave={() => setDragging(false)}
            onDrop={(e) => {
              e.preventDefault();
              setDragging(false);
              handleFiles(e.dataTransfer.files);
            }}
            onClick={() => fileInputRef.current?.click()}
          >
            <UploadCloud className="h-7 w-7 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">
              {t('attachments.dropHint')}
            </p>
            <p className="text-xs text-muted-foreground/70">
              {t('attachments.allowedTypes')}
            </p>
            <input
              ref={fileInputRef}
              type="file"
              className="hidden"
              accept={ATTACHMENT_ACCEPT}
              onChange={(e) => handleFiles(e.target.files)}
            />
          </div>

          {/* Upload progress */}
          {uploadProgress !== null && (
            <div className="space-y-1">
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Loader2 className="h-3 w-3 animate-spin" />
                {t('attachments.uploading')} {uploadProgress}%
              </div>
              <Progress value={uploadProgress} className="h-1.5" />
            </div>
          )}

          {/* Attachment list */}
          {isLoading ? (
            <div className="flex justify-center py-4">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          ) : attachments.length === 0 ? (
            <p className="text-center text-sm text-muted-foreground py-2">
              {t('attachments.empty')}
            </p>
          ) : (
            <ul className="divide-y divide-border rounded-md border">
              {attachments.map((a) => (
                <li key={a.id} className="flex items-center gap-3 px-3 py-2.5">
                  {fileIcon(a.contentType)}
                  <div className="min-w-0 flex-1">
                    {isPreviewable(a.contentType) ? (
                      <button
                        type="button"
                        className="block max-w-full truncate text-left text-sm font-medium hover:underline"
                        onClick={() => setPreviewing(a)}
                      >
                        {a.fileName}
                      </button>
                    ) : (
                      <p className="truncate text-sm font-medium">{a.fileName}</p>
                    )}
                    <p className="text-xs text-muted-foreground">
                      {formatBytes(a.fileSize)}
                      {a.uploadedByUsername && ` · ${a.uploadedByUsername}`}
                    </p>
                  </div>
                  <div className="flex shrink-0 items-center gap-1">
                    {isPreviewable(a.contentType) && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7"
                        title={t('attachments.preview')}
                        onClick={() => setPreviewing(a)}
                      >
                        <Eye className="h-3.5 w-3.5" />
                      </Button>
                    )}
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7"
                      title={t('common.download')}
                      onClick={() =>
                        taskAttachmentService
                          .downloadAttachment(taskId, a.id, a.fileName)
                          .catch(() => toast.error(t('attachments.downloadError')))
                      }
                    >
                      <Download className="h-3.5 w-3.5" />
                    </Button>
                    {canDelete(a) && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-destructive hover:text-destructive"
                        title={t('common.delete')}
                        onClick={() => deleteMutation.mutate(a.id)}
                        disabled={deleteMutation.isPending}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      )}
      <ImagePreview taskId={taskId} attachment={previewing} onClose={() => setPreviewing(null)} />
    </Card>
  );
}
