import { Link } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import { type WikiSpaceDTO, type WikiPageDTO } from "../../services/wikiService";

interface WikiBreadcrumbsProps {
  space: WikiSpaceDTO;
  ancestors: WikiPageDTO[];
  currentTitle: string;
}

export default function WikiBreadcrumbs({
  space,
  ancestors,
  currentTitle,
}: WikiBreadcrumbsProps) {
  return (
    <nav aria-label="breadcrumb" className="flex items-center gap-1 text-sm text-muted-foreground flex-wrap">
      <Link
        to={`/wiki/${space.id}`}
        className="hover:text-foreground transition-colors font-medium"
      >
        {space.name}
      </Link>

      {ancestors.map((ancestor) => (
        <span key={ancestor.id} className="flex items-center gap-1">
          <ChevronRight className="w-3 h-3 flex-none" />
          <Link
            to={`/wiki/${space.id}/${ancestor.id}`}
            className="hover:text-foreground transition-colors"
          >
            {ancestor.title}
          </Link>
        </span>
      ))}

      <span className="flex items-center gap-1">
        <ChevronRight className="w-3 h-3 flex-none" />
        <span className="text-foreground font-medium">{currentTitle}</span>
      </span>
    </nav>
  );
}
