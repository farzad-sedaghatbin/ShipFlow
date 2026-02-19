import * as React from "react"
import { cn } from "@/lib/utils"
import { useBreakpointHelpers } from "@/hooks/useBreakpoint"

/**
 * A single field in a mobile card row.
 */
export interface MobileCardField {
  label: string
  value: React.ReactNode
  /** If true, this field spans the full row width */
  fullWidth?: boolean
  /** If true, this field is hidden on mobile cards */
  hidden?: boolean
}

export interface MobileCardItem {
  /** Unique key for React rendering */
  key: string | number
  /** Primary display line (e.g. name, title) */
  title: React.ReactNode
  /** Optional subtitle */
  subtitle?: React.ReactNode
  /** Optional avatar/icon on the left */
  avatar?: React.ReactNode
  /** Fields to show in the card body */
  fields: MobileCardField[]
  /** Actions (buttons) to show at bottom of card */
  actions?: React.ReactNode
  /** Click handler for the whole card */
  onClick?: () => void
  /** Additional className for the card */
  className?: string
}

interface MobileCardViewProps {
  items: MobileCardItem[]
  /** Render when there are no items */
  emptyState?: React.ReactNode
  className?: string
}

/**
 * Renders a list of cards as a mobile-friendly alternative to tables.
 * Should be paired with the regular table view using useBreakpointHelpers().isMobile.
 */
export function MobileCardView({ items, emptyState, className }: MobileCardViewProps) {
  if (items.length === 0 && emptyState) {
    return <>{emptyState}</>
  }

  return (
    <div className={cn("space-y-3", className)}>
      {items.map((item) => (
        <div
          key={item.key}
          className={cn(
            "rounded-lg border bg-card p-4 transition-colors",
            item.onClick && "cursor-pointer hover:bg-muted/50 active:bg-muted",
            item.className
          )}
          onClick={item.onClick}
        >
          {/* Header: Avatar + Title + Subtitle */}
          <div className="flex items-start gap-3">
            {item.avatar && (
              <div className="flex-shrink-0">{item.avatar}</div>
            )}
            <div className="flex-1 min-w-0">
              <div className="font-medium text-sm leading-tight">{item.title}</div>
              {item.subtitle && (
                <div className="text-xs text-muted-foreground mt-0.5">{item.subtitle}</div>
              )}
            </div>
          </div>

          {/* Fields */}
          {item.fields.filter(f => !f.hidden).length > 0 && (
            <div className="mt-3 grid grid-cols-2 gap-x-4 gap-y-2">
              {item.fields
                .filter(f => !f.hidden)
                .map((field, idx) => (
                  <div key={idx} className={cn(field.fullWidth && "col-span-2")}>
                    <div className="text-[11px] text-muted-foreground uppercase tracking-wide">{field.label}</div>
                    <div className="text-sm mt-0.5">{field.value}</div>
                  </div>
                ))}
            </div>
          )}

          {/* Actions */}
          {item.actions && (
            <div className="mt-3 pt-3 border-t flex items-center gap-2 justify-end">
              {item.actions}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}

/**
 * Wrapper that shows table on desktop and MobileCardView on mobile.
 * Children is the desktop table, mobileCards is the mobile card list.
 */
interface ResponsiveTableProps {
  children: React.ReactNode
  mobileContent: React.ReactNode
  className?: string
}

export function ResponsiveTable({ children, mobileContent, className }: ResponsiveTableProps) {
  const { isMobile } = useBreakpointHelpers()

  return (
    <div className={className}>
      {isMobile ? mobileContent : children}
    </div>
  )
}
