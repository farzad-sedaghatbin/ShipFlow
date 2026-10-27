/**
 * Pure geometry helpers for the roadmap: bar extents, dependency connectors and
 * date-conflict detection. Kept free of React so they can be unit tested.
 */

const DAY_MS = 1000 * 60 * 60 * 24;

export interface BarExtent {
  /** 0..1 fraction of the timeline width where the bar starts */
  left: number;
  /** 0..1 fraction of the timeline width the bar covers */
  width: number;
}

/** Fractional position/width of a bar on the timeline (same math the bars use). */
export function getBarExtent(
  startDate: Date | null,
  endDate: Date | null,
  timelineStart: Date,
  timelineEnd: Date
): BarExtent | null {
  if (!startDate && !endDate) return null;
  const totalDays = Math.ceil((timelineEnd.getTime() - timelineStart.getTime()) / DAY_MS);
  if (totalDays <= 0) return null;
  const start = startDate || endDate!;
  const end = endDate || startDate!;
  const startOffset = Math.ceil((start.getTime() - timelineStart.getTime()) / DAY_MS);
  const duration = Math.ceil((end.getTime() - start.getTime()) / DAY_MS) + 1;
  const left = Math.max(0, startOffset / totalDays);
  const width = Math.min(duration / totalDays, 1 - left);
  return { left, width: Math.max(width, 0.02) };
}

/** True when the dependent (target) epic starts before the blocking (source) epic ends. */
export function hasDependencyConflict(sourceEnd?: string | null, targetStart?: string | null): boolean {
  if (!sourceEnd || !targetStart) return false;
  const end = new Date(sourceEnd).getTime();
  const start = new Date(targetStart).getTime();
  if (Number.isNaN(end) || Number.isNaN(start)) return false;
  return start < end;
}

/**
 * Elbow connector path from (x1,y1) to (x2,y2). Leaves the source horizontally,
 * drops/rises in the gap, and enters the target horizontally. If the target lies
 * before the source's end, it routes around so the arrow still enters from the left.
 */
export function buildConnectorPath(x1: number, y1: number, x2: number, y2: number): string {
  const gap = 8;
  if (x2 - x1 >= gap * 2) {
    const midX = x1 + (x2 - x1) / 2;
    return `M ${x1} ${y1} H ${midX} V ${y2} H ${x2}`;
  }
  const outX = x1 + gap;
  const inX = Math.max(x2 - gap, 0);
  const midY = (y1 + y2) / 2;
  return `M ${x1} ${y1} H ${outX} V ${midY} H ${inX} V ${y2} H ${x2}`;
}

export interface EpicLike {
  id: number;
  startDate?: string;
  endDate?: string;
}

export interface DependencyEdge {
  id: number;
  sourceEpicId: number;
  targetEpicId: number;
}

export interface ConnectorGeometry {
  id: number;
  path: string;
  conflict: boolean;
  /** arrow tip */
  x: number;
  y: number;
}

/**
 * Compute connectors between epics. `rowCenters` maps epicId -> vertical center in px
 * (only visible epic rows are present); `width` is the timeline lane width in px.
 * Edges are skipped when either epic is not visible or has no dates.
 */
export function computeConnectors(
  deps: DependencyEdge[],
  epics: Map<number, EpicLike>,
  rowCenters: Map<number, number>,
  timelineStart: Date,
  timelineEnd: Date,
  width: number
): ConnectorGeometry[] {
  const result: ConnectorGeometry[] = [];
  for (const d of deps) {
    const src = epics.get(d.sourceEpicId);
    const tgt = epics.get(d.targetEpicId);
    const y1 = rowCenters.get(d.sourceEpicId);
    const y2 = rowCenters.get(d.targetEpicId);
    if (!src || !tgt || y1 === undefined || y2 === undefined) continue;
    const srcExt = getBarExtent(
      src.startDate ? new Date(src.startDate) : null,
      src.endDate ? new Date(src.endDate) : null,
      timelineStart,
      timelineEnd
    );
    const tgtExt = getBarExtent(
      tgt.startDate ? new Date(tgt.startDate) : null,
      tgt.endDate ? new Date(tgt.endDate) : null,
      timelineStart,
      timelineEnd
    );
    if (!srcExt || !tgtExt) continue;
    const x1 = (srcExt.left + srcExt.width) * width;
    const x2 = tgtExt.left * width;
    result.push({
      id: d.id,
      path: buildConnectorPath(x1, y1, x2, y2),
      conflict: hasDependencyConflict(src.endDate, tgt.startDate),
      x: x2,
      y: y2,
    });
  }
  return result;
}
