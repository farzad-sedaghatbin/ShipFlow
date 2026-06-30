import { useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link as LinkIcon, AtSign, FileX } from "lucide-react";
import { useNavigate } from "react-router-dom";
import {
  type WikiPageLinkDTO,
  type WikiPageDTO,
} from "../../services/wikiService";
import commentService, { MentionUser } from "../../services/commentService";
import {
  parseMentions,
  MentionLink,
} from "../mentions/mentionUtils";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "../ui/tooltip";

/**
 * Renders the "Linked pages" and "Mentions" affordances derived from a wiki
 * page's resolved `pageLinks` (from the DTO) and the @mentions parsed from its
 * plain text. This is the chosen rendering strategy for `[[pageId]]` links and
 * @mentions: BlockNote's read-only view renders from its own JSON schema, so a
 * clean, testable affordance below the content is preferred over fragile inline
 * custom-content injection. See the page view for where it is mounted.
 */
interface WikiLinkedReferencesProps {
  page: WikiPageDTO;
  /** Plain text extracted from the page body, used to parse @mentions. */
  plainText: string;
}

export default function WikiLinkedReferences({
  page,
  plainText,
}: WikiLinkedReferencesProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const pageLinks: WikiPageLinkDTO[] = page.pageLinks ?? [];
  const mentions = useMemo(() => parseMentions(plainText), [plainText]);

  // Lazy cache of fetched mention users, mirroring the Comments pattern.
  const [mentionUserCache, setMentionUserCache] = useState<
    Map<string, MentionUser>
  >(new Map());

  const fetchMentionUser = useCallback(
    async (name: string): Promise<MentionUser | null> => {
      if (mentionUserCache.has(name)) {
        return mentionUserCache.get(name) || null;
      }
      try {
        const response = await commentService.getUserByDisplayName(name);
        const user = response.data;
        setMentionUserCache((prev) => new Map(prev).set(name, user));
        return user;
      } catch {
        return null;
      }
    },
    [mentionUserCache]
  );

  if (pageLinks.length === 0 && mentions.length === 0) return null;

  return (
    <section className="border-t border-border pt-4 space-y-4 no-print">
      {pageLinks.length > 0 && (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold flex items-center gap-2">
            <LinkIcon className="w-4 h-4" />
            {t("wiki.linkedPages")}
          </h3>
          <ul className="flex flex-wrap gap-2">
            {pageLinks.map((link) =>
              link.exists ? (
                <li key={link.pageId}>
                  <button
                    type="button"
                    onClick={() => navigate(link.url)}
                    className="inline-flex items-center gap-1.5 rounded-full border border-input bg-muted/40 px-3 py-1 text-xs font-medium text-foreground hover:bg-muted transition-colors"
                  >
                    <LinkIcon className="w-3 h-3 text-muted-foreground" />
                    {link.title || t("wiki.pageFallback", { id: link.pageId })}
                  </button>
                </li>
              ) : (
                <li key={link.pageId}>
                  <TooltipProvider>
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <span className="inline-flex items-center gap-1.5 rounded-full border border-dashed border-destructive/40 bg-destructive/5 px-3 py-1 text-xs font-medium text-muted-foreground line-through cursor-not-allowed">
                          <FileX className="w-3 h-3 text-destructive/70" />
                          {t("wiki.pageFallback", { id: link.pageId })}
                        </span>
                      </TooltipTrigger>
                      <TooltipContent>
                        {t("wiki.missingPageTooltip")}
                      </TooltipContent>
                    </Tooltip>
                  </TooltipProvider>
                </li>
              )
            )}
          </ul>
        </div>
      )}

      {mentions.length > 0 && (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold flex items-center gap-2">
            <AtSign className="w-4 h-4" />
            {t("wiki.mentions")}
          </h3>
          <ul className="flex flex-wrap gap-2">
            {mentions.map((m) => (
              <li
                key={m.name}
                className="inline-flex items-center rounded-full border border-input bg-muted/40 px-3 py-1 text-xs"
              >
                <MentionLink
                  name={m.name}
                  cachedUser={mentionUserCache.get(m.name)}
                  onFetchUser={fetchMentionUser}
                />
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
