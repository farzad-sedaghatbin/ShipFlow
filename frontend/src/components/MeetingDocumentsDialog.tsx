import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { FileText } from 'lucide-react';
import { DocumentDropZone } from './DocumentDropZone';
import { documentService, UploadedDocument } from '../services/documentService';
import { Button } from './ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from './ui/dialog';

interface MeetingDocumentsDialogProps {
  open: boolean;
  onClose: () => void;
  meetingId: number;
  meetingType: string;
  meetingDate: string;
}

export const MeetingDocumentsDialog: React.FC<MeetingDocumentsDialogProps> = ({
  open,
  onClose,
  meetingId,
  meetingType,
  meetingDate,
}) => {
  const { t } = useTranslation();
  const [documents, setDocuments] = useState<UploadedDocument[]>([]);

  useEffect(() => {
    if (open && meetingId) {
      loadDocuments();
    }
  }, [open, meetingId]);

  const loadDocuments = async () => {
    try {
      const response = await documentService.getDocumentsForMeeting(meetingId);
      setDocuments(response.data);
    } catch (error) {
      console.error('Failed to load documents:', error);
    }
  };

  const handleDocumentDeleted = (docId: number) => {
    setDocuments(prev => prev.filter(d => d.id !== docId));
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            {t('meetingDocuments.title', { type: meetingType, date: meetingDate })}
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">
            {t('meetingDocuments.description')}
          </p>
          
          <DocumentDropZone
            entityType="MEETING"
            entityId={meetingId}
            existingDocuments={documents}
            onDocumentDeleted={handleDocumentDeleted}
            onUploadComplete={loadDocuments}
          />
        </div>
        
        <DialogFooter>
          <Button variant="ghost" onClick={onClose}>
            {t('meetingDocuments.close')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default MeetingDocumentsDialog;
