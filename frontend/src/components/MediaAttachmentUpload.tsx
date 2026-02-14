import React, { useState, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Upload,
  Image as ImageIcon,
  Video,
  Loader2,
  AlertCircle,
  CheckCircle,
  Trash2,
  Eye,
} from 'lucide-react';
import { documentService, UploadedDocument, DocumentUploadResponse } from '../services/documentService';
import { Button } from './ui/button';
import { Progress } from './ui/progress';
import { cn } from '../lib/utils';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from './ui/dialog';

interface MediaAttachmentUploadProps {
  bugId?: number | null;
  onAttachmentsChange?: (attachments: UploadedDocument[]) => void;
  existingAttachments?: UploadedDocument[];
  disabled?: boolean;
  compact?: boolean;
  pendingFiles?: File[];
  onPendingFilesChange?: (files: File[]) => void;
}

interface UploadingFile {
  file: File;
  progress: number;
  status: 'uploading' | 'success' | 'error';
  response?: DocumentUploadResponse;
  error?: string;
  preview?: string;
}

const ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'mp4', 'webm', 'mov', 'avi'];
const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg'];
const VIDEO_EXTENSIONS = ['mp4', 'webm', 'mov', 'avi'];
const MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

export const MediaAttachmentUpload: React.FC<MediaAttachmentUploadProps> = ({
  bugId,
  onAttachmentsChange,
  existingAttachments = [],
  disabled = false,
  compact = false,
  pendingFiles = [],
  onPendingFilesChange,
}) => {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [uploadingFiles, setUploadingFiles] = useState<UploadingFile[]>([]);
  const [attachments, setAttachments] = useState<UploadedDocument[]>(existingAttachments);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewAttachment, setPreviewAttachment] = useState<UploadedDocument | null>(null);

  const isImageFile = (fileType: string) => IMAGE_EXTENSIONS.includes(fileType.toLowerCase());
  const isVideoFile = (fileType: string) => VIDEO_EXTENSIONS.includes(fileType.toLowerCase());

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (!disabled) {
      setIsDragOver(true);
    }
  }, [disabled]);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback(async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
    
    if (disabled) return;

    const files = Array.from(e.dataTransfer.files);
    uploadFiles(files);
  }, [disabled, bugId]);

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files ? Array.from(e.target.files) : [];
    uploadFiles(files);
    e.target.value = ''; // Reset input
  }, [bugId]);

  const validateFile = (file: File): { valid: boolean; error?: string } => {
    const ext = file.name.split('.').pop()?.toLowerCase() || '';
    
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      return { 
        valid: false, 
        error: t('bugAttachments.invalidType', { types: ALLOWED_EXTENSIONS.join(', ') }) 
      };
    }
    
    if (file.size > MAX_FILE_SIZE) {
      return { 
        valid: false, 
        error: t('bugAttachments.fileTooLarge', { maxSize: '50MB' }) 
      };
    }
    
    return { valid: true };
  };

  const createPreview = (file: File): Promise<string> => {
    return new Promise((resolve) => {
      const ext = file.name.split('.').pop()?.toLowerCase() || '';
      if (IMAGE_EXTENSIONS.includes(ext)) {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result as string);
        reader.readAsDataURL(file);
      } else {
        resolve(''); // No preview for videos during upload
      }
    });
  };

  const uploadFiles = async (files: File[]) => {
    if (!bugId) {
      // If no bugId yet, add files to pending list for later upload
      const validFiles: File[] = [];
      const errors: UploadingFile[] = [];
      
      for (const file of files) {
        const validation = validateFile(file);
        
        if (!validation.valid) {
          errors.push({
            file,
            progress: 0,
            status: 'error',
            error: validation.error,
          });
        } else {
          validFiles.push(file);
        }
      }
      
      // Show errors temporarily
      if (errors.length > 0) {
        setUploadingFiles(errors);
        setTimeout(() => {
          setUploadingFiles(prev => prev.filter(uf => uf.status !== 'error'));
        }, 3000);
      }
      
      // Add valid files to pending list
      if (validFiles.length > 0 && onPendingFilesChange) {
        onPendingFilesChange([...pendingFiles, ...validFiles]);
      }
      return;
    }

    for (const file of files) {
      const validation = validateFile(file);
      
      if (!validation.valid) {
        setUploadingFiles(prev => [...prev, {
          file,
          progress: 0,
          status: 'error',
          error: validation.error,
        }]);
        continue;
      }

      const preview = await createPreview(file);
      
      setUploadingFiles(prev => [...prev, {
        file,
        progress: 0,
        status: 'uploading',
        preview,
      }]);

      try {
        const response = await documentService.uploadBugAttachment(bugId, file);

        if (response.data.errorMessage) {
          setUploadingFiles(prev => prev.map(uf =>
            uf.file === file
              ? { ...uf, status: 'error', error: response.data.errorMessage }
              : uf
          ));
        } else {
          setUploadingFiles(prev => prev.map(uf =>
            uf.file === file
              ? { ...uf, status: 'success', progress: 100, response: response.data }
              : uf
          ));

          // Add to attachments list
          if (response.data.id) {
            const newAttachment: UploadedDocument = {
              id: response.data.id,
              fileName: response.data.storagePath || response.data.fileName || '',
              originalFileName: response.data.fileName || file.name,
              fileType: response.data.fileType || '',
              fileSize: response.data.fileSize,
              storagePath: response.data.storagePath,
              textExtracted: false,
              createdAt: new Date().toISOString(),
              indexedForQA: false,
            };
            
            setAttachments(prev => {
              const updated = [...prev, newAttachment];
              onAttachmentsChange?.(updated);
              return updated;
            });
          }
        }
      } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : 'Upload failed';
        setUploadingFiles(prev => prev.map(uf =>
          uf.file === file
            ? { ...uf, status: 'error', error: errorMessage }
            : uf
        ));
      }
    }

    // Clear completed uploads after a delay
    setTimeout(() => {
      setUploadingFiles(prev => prev.filter(uf => uf.status === 'uploading'));
    }, 3000);
  };

  const handleDelete = async (attachmentId: number) => {
    try {
      await documentService.deleteBugAttachment(attachmentId);
      setAttachments(prev => {
        const updated = prev.filter(a => a.id !== attachmentId);
        onAttachmentsChange?.(updated);
        return updated;
      });
    } catch (error) {
      console.error('Failed to delete attachment:', error);
    }
  };

  const handlePreview = (attachment: UploadedDocument) => {
    setPreviewAttachment(attachment);
    setPreviewOpen(true);
  };

  const getFileIcon = (fileType: string) => {
    if (isImageFile(fileType)) {
      return <ImageIcon className="h-4 w-4" />;
    }
    if (isVideoFile(fileType)) {
      return <Video className="h-4 w-4" />;
    }
    return <ImageIcon className="h-4 w-4" />;
  };

  const getAttachmentUrl = (attachment: UploadedDocument) => {
    return `/api/documents/${attachment.id}/download`;
  };

  return (
    <div className="space-y-3">
      {/* Drop Zone */}
      <div
        className={cn(
          'relative border-2 border-dashed rounded-lg transition-colors',
          isDragOver
            ? 'border-primary bg-primary/5'
            : 'border-muted-foreground/25 hover:border-muted-foreground/50',
          disabled && 'opacity-50 cursor-not-allowed',
          compact ? 'p-3' : 'p-4'
        )}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          accept={ALLOWED_EXTENSIONS.map(ext => `.${ext}`).join(',')}
          multiple
          onChange={handleFileSelect}
          disabled={disabled}
        />
        
        <div className={cn(
          'flex flex-col items-center justify-center text-center',
          compact ? 'gap-1' : 'gap-2'
        )}>
          <div className={cn(
            'rounded-full bg-muted',
            compact ? 'p-2' : 'p-3'
          )}>
            <Upload className={cn(
              'text-muted-foreground',
              compact ? 'h-4 w-4' : 'h-5 w-5'
            )} />
          </div>
          <div>
            <p className={cn(
              'font-medium',
              compact ? 'text-xs' : 'text-sm'
            )}>
              {t('bugAttachments.dropHere')}
            </p>
            <p className={cn(
              'text-muted-foreground',
              compact ? 'text-[10px]' : 'text-xs'
            )}>
              {t('bugAttachments.supportedTypes')}
            </p>
          </div>
          <Button
            type="button"
            variant="outline"
            size={compact ? 'sm' : 'default'}
            onClick={() => fileInputRef.current?.click()}
            disabled={disabled}
            className={compact ? 'h-7 text-xs' : ''}
          >
            {t('bugAttachments.selectFiles')}
          </Button>
        </div>
      </div>

      {/* Uploading Files */}
      {uploadingFiles.length > 0 && (
        <div className="space-y-2">
          {uploadingFiles.map((uf, index) => (
            <div
              key={index}
              className={cn(
                'flex items-center gap-3 p-2 rounded-lg bg-muted/50',
                uf.status === 'error' && 'bg-destructive/10'
              )}
            >
              {uf.preview ? (
                <img
                  src={uf.preview}
                  alt=""
                  className="h-10 w-10 rounded object-cover"
                />
              ) : (
                <div className="h-10 w-10 rounded bg-muted flex items-center justify-center">
                  {getFileIcon(uf.file.name.split('.').pop() || '')}
                </div>
              )}
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{uf.file.name}</p>
                {uf.status === 'uploading' && (
                  <Progress value={uf.progress} className="h-1 mt-1" />
                )}
                {uf.status === 'error' && (
                  <p className="text-xs text-destructive flex items-center gap-1 mt-1">
                    <AlertCircle className="h-3 w-3" />
                    {uf.error}
                  </p>
                )}
                {uf.status === 'success' && (
                  <p className="text-xs text-green-600 flex items-center gap-1 mt-1">
                    <CheckCircle className="h-3 w-3" />
                    {t('bugAttachments.uploaded')}
                  </p>
                )}
              </div>
              {uf.status === 'uploading' && (
                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
              )}
            </div>
          ))}
        </div>
      )}

      {/* Pending Files (for creation mode) */}
      {!bugId && pendingFiles.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-medium text-muted-foreground">
            {t('bugAttachments.pendingUpload', { count: pendingFiles.length })}
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {pendingFiles.map((file, index) => {
              const ext = file.name.split('.').pop()?.toLowerCase() || '';
              const isImage = IMAGE_EXTENSIONS.includes(ext);
              
              return (
                <div
                  key={index}
                  className="relative group rounded-lg overflow-hidden border bg-muted/30"
                >
                  {isImage ? (
                    <img
                      src={URL.createObjectURL(file)}
                      alt={file.name}
                      className="w-full h-20 object-cover"
                    />
                  ) : (
                    <div className="w-full h-20 flex items-center justify-center bg-muted">
                      {getFileIcon(ext)}
                    </div>
                  )}
                  
                  {/* Overlay with delete action */}
                  <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                    <Button
                      type="button"
                      size="icon"
                      variant="ghost"
                      className="h-7 w-7 text-white hover:bg-destructive/50"
                      onClick={() => {
                        if (onPendingFilesChange) {
                          onPendingFilesChange(pendingFiles.filter((_, i) => i !== index));
                        }
                      }}
                      disabled={disabled}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                  
                  {/* File name */}
                  <div className="absolute bottom-0 left-0 right-0 bg-black/60 px-2 py-1">
                    <p className="text-[10px] text-white truncate">
                      {file.name}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Existing Attachments */}
      {attachments.length > 0 && (
        <div className="space-y-2">
          <p className="text-xs font-medium text-muted-foreground">
            {t('bugAttachments.attached', { count: attachments.length })}
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {attachments.map((attachment) => (
              <div
                key={attachment.id}
                className="relative group rounded-lg overflow-hidden border bg-muted/30"
              >
                {isImageFile(attachment.fileType) ? (
                  <img
                    src={getAttachmentUrl(attachment)}
                    alt={attachment.originalFileName}
                    className="w-full h-20 object-cover"
                  />
                ) : isVideoFile(attachment.fileType) ? (
                  <div className="w-full h-20 flex items-center justify-center bg-muted">
                    <Video className="h-8 w-8 text-muted-foreground" />
                  </div>
                ) : (
                  <div className="w-full h-20 flex items-center justify-center bg-muted">
                    {getFileIcon(attachment.fileType)}
                  </div>
                )}
                
                {/* Overlay with actions */}
                <div className="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                  <Button
                    type="button"
                    size="icon"
                    variant="ghost"
                    className="h-7 w-7 text-white hover:bg-white/20"
                    onClick={() => handlePreview(attachment)}
                  >
                    <Eye className="h-4 w-4" />
                  </Button>
                  <Button
                    type="button"
                    size="icon"
                    variant="ghost"
                    className="h-7 w-7 text-white hover:bg-destructive/50"
                    onClick={() => handleDelete(attachment.id)}
                    disabled={disabled}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
                
                {/* File name */}
                <div className="absolute bottom-0 left-0 right-0 bg-black/60 px-2 py-1">
                  <p className="text-[10px] text-white truncate">
                    {attachment.originalFileName}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Preview Dialog */}
      <Dialog open={previewOpen} onOpenChange={setPreviewOpen}>
        <DialogContent className="max-w-4xl max-h-[90vh]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {previewAttachment && getFileIcon(previewAttachment.fileType)}
              {previewAttachment?.originalFileName}
            </DialogTitle>
          </DialogHeader>
          <div className="flex items-center justify-center overflow-auto max-h-[70vh]">
            {previewAttachment && isImageFile(previewAttachment.fileType) && (
              <img
                src={getAttachmentUrl(previewAttachment)}
                alt={previewAttachment.originalFileName}
                className="max-w-full max-h-full object-contain"
              />
            )}
            {previewAttachment && isVideoFile(previewAttachment.fileType) && (
              <video
                src={getAttachmentUrl(previewAttachment)}
                controls
                className="max-w-full max-h-full"
              >
                {t('bugAttachments.videoNotSupported')}
              </video>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default MediaAttachmentUpload;
