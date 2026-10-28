import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Paperclip, X } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import { ATTACHMENT_ACCEPT, attachmentRejection, fileIcon, formatBytes } from '../TaskAttachments';

interface PendingAttachmentsProps {
  files: File[];
  onChange: (files: File[]) => void;
}

/**
 * File picker for a task that doesn't exist yet: files are held here and uploaded by the caller
 * once the task has been created (attachments need a task id). Same type/size rules as
 * {@link TaskAttachments}.
 */
export function PendingAttachments({ files, onChange }: PendingAttachmentsProps) {
  const { t } = useTranslation();
  const inputRef = useRef<HTMLInputElement>(null);

  const addFiles = (list: FileList | null) => {
    if (!list) return;
    const accepted: File[] = [];
    for (const file of Array.from(list)) {
      const rejection = attachmentRejection(file);
      if (rejection) {
        toast.error(`${file.name}: ${t(rejection)}`);
      } else {
        accepted.push(file);
      }
    }
    if (accepted.length > 0) onChange([...files, ...accepted]);
  };

  return (
    <div className="grid gap-2">
      <Label>{t('attachments.title')}</Label>
      <div
        className="flex flex-col items-center justify-center gap-1 rounded-md border border-dashed p-4 text-center text-sm text-muted-foreground cursor-pointer hover:bg-muted/50"
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault();
          addFiles(e.dataTransfer.files);
        }}
      >
        <Paperclip className="h-4 w-4" />
        <span>{t('attachments.dropHint')}</span>
        <span className="text-xs">{t('attachments.allowedTypes')}</span>
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          accept={ATTACHMENT_ACCEPT}
          aria-label={t('attachments.title')}
          onChange={(e) => {
            addFiles(e.target.files);
            e.target.value = '';
          }}
        />
      </div>
      {files.length > 0 && (
        <ul className="divide-y rounded-md border">
          {files.map((file, index) => (
            <li key={`${file.name}-${index}`} className="flex items-center gap-2 px-3 py-2 text-sm">
              {fileIcon(file.type)}
              <span className="min-w-0 flex-1 truncate">{file.name}</span>
              <span className="text-xs text-muted-foreground">{formatBytes(file.size)}</span>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="h-7 w-7"
                aria-label={t('attachments.remove')}
                onClick={() => onChange(files.filter((_, i) => i !== index))}
              >
                <X className="h-4 w-4" />
              </Button>
            </li>
          ))}
        </ul>
      )}
      {files.length > 0 && (
        <p className="text-xs text-muted-foreground">{t('attachments.uploadAfterCreate')}</p>
      )}
    </div>
  );
}
