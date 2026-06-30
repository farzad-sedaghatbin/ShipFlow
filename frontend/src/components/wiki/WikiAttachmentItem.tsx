import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Download, Trash2, FileText, ImageIcon, Loader2 } from "lucide-react";
import { wikiService, type WikiAttachmentDTO } from "../../services/wikiService";

interface WikiAttachmentItemProps {
  attachment: WikiAttachmentDTO;
  onDelete: (attId: number) => void;
  deleting: boolean;
}

/**
 * A single wiki attachment row. Image attachments get an inline thumbnail
 * preview (fetched through the authenticated client as a blob → object URL).
 * Downloads also go through the authenticated client — never a raw `<a href>`,
 * which would hit the JWT-secured endpoint without a token and 401.
 */
export default function WikiAttachmentItem({
  attachment,
  onDelete,
  deleting,
}: WikiAttachmentItemProps) {
  const { t } = useTranslation();
  const isImage = attachment.contentType?.startsWith("image/");
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [previewError, setPreviewError] = useState(false);
  const [downloading, setDownloading] = useState(false);

  // Fetch the image bytes once for an inline preview; revoke the object URL on
  // unmount (or when the attachment changes) to avoid leaking blob memory.
  useEffect(() => {
    if (!isImage) return;
    let revoked = false;
    let url: string | null = null;
    wikiService
      .fetchAttachmentObjectUrl(attachment.id)
      .then((u) => {
        if (revoked) {
          window.URL.revokeObjectURL(u);
          return;
        }
        url = u;
        setPreviewUrl(u);
      })
      .catch(() => setPreviewError(true));
    return () => {
      revoked = true;
      if (url) window.URL.revokeObjectURL(url);
    };
  }, [attachment.id, isImage]);

  async function handleDownload() {
    setDownloading(true);
    try {
      await wikiService.downloadAttachment(attachment.id, attachment.fileName);
    } finally {
      setDownloading(false);
    }
  }

  return (
    <li className="flex items-center gap-3 px-3 py-2 text-sm">
      {/* Thumbnail / icon */}
      <div className="shrink-0">
        {isImage && previewUrl ? (
          <button
            type="button"
            onClick={() => window.open(previewUrl, "_blank", "noopener")}
            title={t("wiki.openImage")}
            className="block"
          >
            <img
              src={previewUrl}
              alt={attachment.fileName}
              className="h-10 w-10 rounded object-cover border border-border hover:opacity-80 transition-opacity"
            />
          </button>
        ) : isImage && !previewError ? (
          <div className="h-10 w-10 rounded border border-border flex items-center justify-center bg-muted">
            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
          </div>
        ) : isImage ? (
          <div className="h-10 w-10 rounded border border-border flex items-center justify-center bg-muted">
            <ImageIcon className="h-4 w-4 text-muted-foreground" />
          </div>
        ) : (
          <div className="h-10 w-10 rounded border border-border flex items-center justify-center bg-muted">
            <FileText className="h-4 w-4 text-muted-foreground" />
          </div>
        )}
      </div>

      {/* Name + size */}
      <div className="flex-1 min-w-0">
        <p className="truncate">{attachment.fileName}</p>
        <p className="text-xs text-muted-foreground">
          {(attachment.fileSize / 1024).toFixed(1)} KB
        </p>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-1 shrink-0">
        <button
          onClick={handleDownload}
          disabled={downloading}
          className="text-muted-foreground hover:text-foreground transition-colors disabled:opacity-50 p-1"
          aria-label={t("wiki.download")}
          title={t("wiki.download")}
        >
          {downloading ? (
            <Loader2 className="w-4 h-4 animate-spin" />
          ) : (
            <Download className="w-4 h-4" />
          )}
        </button>
        <button
          onClick={() => onDelete(attachment.id)}
          disabled={deleting}
          className="text-muted-foreground hover:text-destructive transition-colors disabled:opacity-50 p-1"
          aria-label={t("wiki.deleteAttachment")}
          title={t("wiki.deleteAttachment")}
        >
          <Trash2 className="w-4 h-4" />
        </button>
      </div>
    </li>
  );
}
