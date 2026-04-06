import { useTranslation } from 'react-i18next';
import { UploadedDocument } from '../../services/documentService';
import { DocumentDropZone } from '../DocumentDropZone';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';

interface PitchDocumentsSectionProps {
  pitchId: number;
  documents: UploadedDocument[];
  onDocumentDeleted: (docId: number) => void;
  onUploadComplete: () => void;
}

export function PitchDocumentsSection({
  pitchId,
  documents,
  onDocumentDeleted,
  onUploadComplete,
}: PitchDocumentsSectionProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardHeader>
        <CardTitle>{t('pitchDetailPage.documents')}</CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground mb-4">
          {t('pitchDetailPage.documentsDescription')}
        </p>
        <DocumentDropZone
          entityType="PITCH"
          entityId={pitchId}
          existingDocuments={documents}
          onDocumentDeleted={onDocumentDeleted}
          onUploadComplete={onUploadComplete}
        />
      </CardContent>
    </Card>
  );
}
