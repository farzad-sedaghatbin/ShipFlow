import React, { useState, useCallback } from 'react';
import {
  Upload,
  FileText,
  FileType,
  File,
  Trash2,
  CheckCircle,
  XCircle,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';
import { documentService, UploadedDocument, DocumentUploadResponse } from '../services/documentService';
import { Card } from './ui/card';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Progress } from './ui/progress';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from './ui/collapsible';
import { cn } from '../lib/utils';

interface DocumentDropZoneProps {
  entityType: 'PITCH' | 'MEETING' | 'CYCLE';
  entityId: number;
  onUploadComplete?: (doc: DocumentUploadResponse) => void;
  existingDocuments?: UploadedDocument[];
  onDocumentDeleted?: (docId: number) => void;
}

interface UploadingFile {
  file: File;
  progress: number;
  status: 'uploading' | 'success' | 'error';
  response?: DocumentUploadResponse;
  error?: string;
}

export const DocumentDropZone: React.FC<DocumentDropZoneProps> = ({
  entityType,
  entityId,
  onUploadComplete,
  existingDocuments = [],
  onDocumentDeleted,
}) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const [uploadingFiles, setUploadingFiles] = useState<UploadingFile[]>([]);
  const [showExtracted, setShowExtracted] = useState<number | null>(null);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);
  }, []);

  const handleDrop = useCallback(async (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragOver(false);

    const files = Array.from(e.dataTransfer.files);
    uploadFiles(files);
  }, [entityType, entityId]);

  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files ? Array.from(e.target.files) : [];
    uploadFiles(files);
    e.target.value = ''; // Reset input
  }, [entityType, entityId]);

  const uploadFiles = async (files: File[]) => {
    const validFiles = files.filter(file => {
      const ext = file.name.split('.').pop()?.toLowerCase();
      return ['pdf', 'docx', 'doc', 'txt', 'md'].includes(ext || '');
    });

    if (validFiles.length === 0) {
      return;
    }

    for (const file of validFiles) {
      setUploadingFiles(prev => [...prev, {
        file,
        progress: 0,
        status: 'uploading',
      }]);

      try {
        let response;
        switch (entityType) {
          case 'PITCH':
            response = await documentService.uploadForPitch(entityId, file);
            break;
          case 'MEETING':
            response = await documentService.uploadForMeeting(entityId, file);
            break;
          case 'CYCLE':
            response = await documentService.uploadForCycle(entityId, file);
            break;
        }

        setUploadingFiles(prev => prev.map(uf => 
          uf.file === file 
            ? { ...uf, status: 'success', progress: 100, response: response.data }
            : uf
        ));

        if (onUploadComplete && response.data) {
          onUploadComplete(response.data);
        }
      } catch (error: any) {
        setUploadingFiles(prev => prev.map(uf =>
          uf.file === file
            ? { ...uf, status: 'error', error: error.message || 'Upload failed' }
            : uf
        ));
      }
    }
  };

  const handleDelete = async (docId: number) => {
    try {
      await documentService.deleteDocument(docId);
      if (onDocumentDeleted) {
        onDocumentDeleted(docId);
      }
    } catch (error) {
      console.error('Failed to delete document:', error);
    }
  };

  const getFileIcon = (fileType: string) => {
    switch (fileType.toLowerCase()) {
      case 'pdf':
        return <FileType className="h-5 w-5 text-red-500" />;
      case 'docx':
      case 'doc':
        return <FileText className="h-5 w-5 text-blue-500" />;
      default:
        return <File className="h-5 w-5" />;
    }
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div>
      {/* Drop Zone */}
      <label
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={cn(
          "flex flex-col items-center justify-center p-6 border-2 border-dashed rounded-lg cursor-pointer transition-all",
          "hover:border-primary hover:bg-primary/5",
          "focus-within:outline-none focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2",
          isDragOver 
            ? "border-primary bg-primary/10" 
            : "border-border bg-background"
        )}
      >
        <input
          type="file"
          className="sr-only"
          multiple
          accept=".pdf,.docx,.doc,.txt,.md"
          onChange={handleFileSelect}
        />
        <Upload className="h-12 w-12 text-muted-foreground mb-2" />
        <p className="text-sm text-foreground">
          Drag and drop documents here, or click to select
        </p>
        <p className="text-xs text-muted-foreground mt-1">
          Supported formats: PDF, DOCX, DOC, TXT, MD (max 10MB)
        </p>
      </label>

      {/* Uploading Files */}
      {uploadingFiles.length > 0 && (
        <div className="mt-4 space-y-2">
          {uploadingFiles.map((uf, index) => (
            <div key={index} className="flex items-center gap-3 p-3 bg-muted/50 rounded-lg">
              <div className="shrink-0">
                {uf.status === 'success' ? (
                  <CheckCircle className="h-5 w-5 text-green-500" />
                ) : uf.status === 'error' ? (
                  <XCircle className="h-5 w-5 text-red-500" />
                ) : (
                  getFileIcon(uf.file.name.split('.').pop() || '')
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{uf.file.name}</p>
                {uf.status === 'uploading' && (
                  <div className="mt-1">
                    <Progress value={undefined} className="h-1" />
                  </div>
                )}
                {uf.status === 'error' && (
                  <p className="text-xs text-red-500 mt-1">{uf.error}</p>
                )}
                {uf.status === 'success' && uf.response?.textExtracted && (
                  <p className="text-xs text-green-500 mt-1">Text extracted successfully</p>
                )}
                {uf.status === 'success' && !uf.response?.textExtracted && (
                  <p className="text-xs text-yellow-500 mt-1">Uploaded (no text extracted)</p>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Existing Documents */}
      {existingDocuments.length > 0 && (
        <div className="mt-4">
          <h4 className="text-sm font-medium mb-2">
            Uploaded Documents ({existingDocuments.length})
          </h4>
          <div className="space-y-2">
            {existingDocuments.map((doc) => (
              <Collapsible
                key={doc.id}
                open={showExtracted === doc.id}
                onOpenChange={(open) => setShowExtracted(open ? doc.id : null)}
              >
                <div className="flex items-center gap-3 p-3 bg-muted/30 rounded-lg">
                  <div className="shrink-0">
                    {getFileIcon(doc.fileType)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">{doc.originalFileName}</p>
                    <div className="flex flex-wrap items-center gap-2 mt-1">
                      <span className="text-xs text-muted-foreground">
                        {formatFileSize(doc.fileSize)}
                      </span>
                      {doc.textExtracted && (
                        <Badge variant="outline" className="text-xs bg-green-500/10 text-green-500 border-green-500/30">
                          Text Extracted
                        </Badge>
                      )}
                      {doc.indexedForQA && (
                        <Badge variant="outline" className="text-xs bg-primary/10 text-primary border-primary/30">
                          In Knowledge Base
                        </Badge>
                      )}
                    </div>
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    {doc.textExtracted && doc.extractedText && (
                      <CollapsibleTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                          {showExtracted === doc.id ? (
                            <ChevronUp className="h-4 w-4" />
                          ) : (
                            <ChevronDown className="h-4 w-4" />
                          )}
                        </Button>
                      </CollapsibleTrigger>
                    )}
                    <Button 
                      variant="ghost" 
                      size="icon" 
                      className="h-8 w-8 text-red-500 hover:text-red-600 hover:bg-red-500/10"
                      onClick={() => handleDelete(doc.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
                <CollapsibleContent>
                  <Card className="mt-2 ml-8 mr-2 p-4 max-h-[200px] overflow-auto">
                    <p className="text-sm whitespace-pre-wrap">
                      {doc.extractedText?.substring(0, 2000)}
                      {(doc.extractedText?.length || 0) > 2000 && '...'}
                    </p>
                  </Card>
                </CollapsibleContent>
              </Collapsible>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default DocumentDropZone;
