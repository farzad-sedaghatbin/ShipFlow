import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ChevronDown, ChevronRight, FileText } from "lucide-react";
import { type WikiTreeNodeDTO } from "../../services/wikiService";
import { wikiService } from "../../services/wikiService";

// ─── Types ────────────────────────────────────────────────────────────────────

export interface WikiTreeProps {
  spaceId: number;
  nodes: WikiTreeNodeDTO[];
  currentPageId?: number;
  onNavigate?: (pageId: number) => void;
}

// ─── Single node (recursive) ──────────────────────────────────────────────────

interface WikiTreeNodeProps {
  spaceId: number;
  node: WikiTreeNodeDTO;
  currentPageId?: number;
  onNavigate?: (pageId: number) => void;
  onDrop: (draggedId: number, targetParentId: number | null, newIndex: number) => void;
}

function WikiTreeNode({
  spaceId,
  node,
  currentPageId,
  onNavigate,
  onDrop,
}: WikiTreeNodeProps) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(true);
  const hasChildren = node.children && node.children.length > 0;
  const isActive = currentPageId === node.id;

  function handleClick(e: React.MouseEvent) {
    e.stopPropagation();
    navigate(`/wiki/${spaceId}/${node.id}`);
    onNavigate?.(node.id);
  }

  function handleToggle(e: React.MouseEvent) {
    e.stopPropagation();
    if (hasChildren) setOpen((o) => !o);
  }

  // ── Drag-and-drop (HTML5) ──────────────────────────────────────────────────

  function handleDragStart(e: React.DragEvent<HTMLLIElement>) {
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", String(node.id));
  }

  function handleDragOver(e: React.DragEvent<HTMLLIElement>) {
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
  }

  function handleDrop(e: React.DragEvent<HTMLLIElement>) {
    e.preventDefault();
    e.stopPropagation();
    const draggedId = Number(e.dataTransfer.getData("text/plain"));
    if (draggedId === node.id) return;
    // Drop onto a node → becomes the first child of the parent of drop target.
    // Convention: place dragged node at position 0 under the drop target's parent.
    onDrop(draggedId, node.id, 0);
  }

  return (
    <li
      data-testid={`wiki-tree-node-${node.id}`}
      draggable
      onDragStart={handleDragStart}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
      className="list-none"
    >
      <div
        className={`flex items-center gap-1 px-2 py-1 rounded cursor-pointer text-sm hover:bg-muted/60 ${
          isActive ? "bg-muted font-medium" : ""
        }`}
        onClick={handleClick}
      >
        {/* Expand / collapse toggle */}
        <button
          onClick={handleToggle}
          className="flex-none w-4 h-4 flex items-center justify-center text-muted-foreground hover:text-foreground"
          aria-label={open ? "collapse" : "expand"}
        >
          {hasChildren ? (
            open ? (
              <ChevronDown className="w-3 h-3" />
            ) : (
              <ChevronRight className="w-3 h-3" />
            )
          ) : (
            <FileText className="w-3 h-3 text-muted-foreground/50" />
          )}
        </button>
        <span className="truncate">{node.title}</span>
      </div>

      {hasChildren && open && (
        <ul className="pl-4">
          {node.children.map((child) => (
            <WikiTreeNode
              key={child.id}
              spaceId={spaceId}
              node={child}
              currentPageId={currentPageId}
              onNavigate={onNavigate}
              onDrop={onDrop}
            />
          ))}
        </ul>
      )}
    </li>
  );
}

// ─── Root component ───────────────────────────────────────────────────────────

export default function WikiTree({
  spaceId,
  nodes,
  currentPageId,
  onNavigate,
}: WikiTreeProps) {
  const queryClient = useQueryClient();

  const moveMutation = useMutation({
    mutationFn: ({
      pageId,
      newParentId,
      newIndex,
    }: {
      pageId: number;
      newParentId: number | null;
      newIndex: number;
    }) => wikiService.movePage(pageId, { newParentId, newIndex }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["wiki-tree", spaceId] });
    },
  });

  function handleDrop(
    draggedId: number,
    targetParentId: number | null,
    newIndex: number
  ) {
    moveMutation.mutate({ pageId: draggedId, newParentId: targetParentId, newIndex });
  }

  if (!nodes || nodes.length === 0) {
    return <p className="text-sm text-muted-foreground px-2 py-1">—</p>;
  }

  return (
    <ul className="space-y-0.5">
      {nodes.map((node) => (
        <WikiTreeNode
          key={node.id}
          spaceId={spaceId}
          node={node}
          currentPageId={currentPageId}
          onNavigate={onNavigate}
          onDrop={handleDrop}
        />
      ))}
    </ul>
  );
}
