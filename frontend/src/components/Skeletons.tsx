import { Skeleton } from '@/components/ui/skeleton';
import { Card, CardContent } from '@/components/ui/card';

/**
 * Skeleton for stat cards shown on the Dashboard
 */
export function StatCardSkeleton() {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex items-center mb-2">
          <Skeleton className="w-10 h-10 rounded-lg mr-2" />
          <Skeleton className="w-24 h-6" />
        </div>
        <Skeleton className="w-16 h-12" />
      </CardContent>
    </Card>
  );
}

/**
 * Skeleton for the Dashboard page
 */
export function DashboardSkeleton() {
  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-4">
        <div>
          <Skeleton className="w-36 h-10" />
          <Skeleton className="w-64 h-5 mt-1" />
        </div>
        <Skeleton className="w-28 h-9 rounded-md" />
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        {[1, 2, 3, 4].map((i) => (
          <StatCardSkeleton key={i} />
        ))}
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-3">
        {/* Active Cycles */}
        <div className="md:col-span-8">
          <Card>
            <CardContent className="p-4">
              <div className="flex justify-between mb-2">
                <Skeleton className="w-28 h-7" />
                <Skeleton className="w-20 h-8 rounded-md" />
              </div>
              <div className="space-y-2">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="p-3 border border-border rounded-lg">
                    <div className="flex justify-between mb-1">
                      <Skeleton className="w-44 h-6" />
                      <Skeleton className="w-16 h-6 rounded-full" />
                    </div>
                    <Skeleton className="w-full h-2" />
                    <div className="flex justify-between mt-1">
                      <Skeleton className="w-24 h-4" />
                      <Skeleton className="w-20 h-4" />
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Recent Pitches */}
        <div className="md:col-span-4">
          <Card>
            <CardContent className="p-4">
              <Skeleton className="w-28 h-7 mb-2" />
              <div className="space-y-2">
                {[1, 2, 3, 4, 5].map((i) => (
                  <div key={i} className="flex items-center gap-2">
                    <Skeleton className="w-8 h-8 rounded-full" />
                    <div className="flex-1">
                      <Skeleton className="w-4/5 h-5" />
                      <Skeleton className="w-1/2 h-4" />
                    </div>
                    <Skeleton className="w-14 h-6 rounded-full" />
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Hill Chart Widget Skeleton */}
      <div className="mt-3">
        <Card>
          <CardContent className="p-4">
            <div className="flex justify-between mb-2">
              <Skeleton className="w-36 h-7" />
              <Skeleton className="w-24 h-8 rounded-md" />
            </div>
            <Skeleton className="w-full h-48 rounded-lg" />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

/**
 * Skeleton for Risk Insights Card
 */
export function RiskInsightsSkeleton() {
  return (
    <Card>
      <CardContent className="p-4">
        {/* Header */}
        <div className="flex justify-between items-center mb-3">
          <div className="flex items-center gap-1">
            <Skeleton className="w-6 h-6 rounded-full" />
            <Skeleton className="w-28 h-6" />
          </div>
          <Skeleton className="w-8 h-8 rounded-full" />
        </div>

        {/* Risk Score */}
        <div className="flex items-center gap-2 mb-3">
          <Skeleton className="w-20 h-20 rounded-full" />
          <div>
            <Skeleton className="w-24 h-5" />
            <Skeleton className="w-20 h-6 mt-1 rounded-md" />
          </div>
        </div>

        {/* Progress bar */}
        <Skeleton className="w-full h-2 mb-3 rounded-full" />

        {/* Sections */}
        {[1, 2, 3].map((i) => (
          <div key={i} className="mb-2">
            <div className="flex items-center gap-1 mb-1">
              <Skeleton className="w-5 h-5 rounded-full" />
              <Skeleton className="w-24 h-5" />
            </div>
            <div className="space-y-1 pl-4">
              <Skeleton className="w-[90%] h-4" />
              <Skeleton className="w-3/4 h-4" />
            </div>
          </div>
        ))}
      </CardContent>
    </Card>
  );
}

/**
 * Skeleton for Cycle List page
 */
export function CycleListSkeleton() {
  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-3">
        <Skeleton className="w-24 h-10" />
        <Skeleton className="w-28 h-9 rounded-md" />
      </div>

      {/* Cycle Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
        {[1, 2, 3, 4, 5, 6].map((i) => (
          <Card key={i}>
            <CardContent className="p-4">
              <div className="flex justify-between mb-2">
                <Skeleton className="w-36 h-7" />
                <Skeleton className="w-16 h-6 rounded-full" />
              </div>
              <Skeleton className="w-full h-4 mb-1" />
              <Skeleton className="w-3/5 h-4 mb-2" />
              <Skeleton className="w-full h-2 rounded-full mb-1" />
              <div className="flex justify-between">
                <Skeleton className="w-20 h-3" />
                <Skeleton className="w-14 h-3" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

/**
 * Skeleton for Pitch Detail page
 */
export function PitchDetailSkeleton() {
  return (
    <div>
      {/* Header */}
      <div className="mb-3">
        <div className="flex items-center gap-2 mb-1">
          <Skeleton className="w-72 h-10" />
          <Skeleton className="w-20 h-7 rounded-full" />
        </div>
        <Skeleton className="w-48 h-5" />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-12 gap-3">
        {/* Main Content */}
        <div className="md:col-span-8 space-y-3">
          <Card>
            <CardContent className="p-4">
              <Skeleton className="w-24 h-6 mb-2" />
              <Skeleton className="w-full h-4" />
              <Skeleton className="w-full h-4" />
              <Skeleton className="w-[70%] h-4" />
            </CardContent>
          </Card>

          {/* Hill Chart */}
          <Card>
            <CardContent className="p-4">
              <Skeleton className="w-28 h-6 mb-2" />
              <Skeleton className="w-full h-48 rounded-lg" />
            </CardContent>
          </Card>

          {/* Work Logs */}
          <Card>
            <CardContent className="p-4">
              <Skeleton className="w-24 h-6 mb-2" />
              <div className="space-y-2">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="flex gap-2">
                    <Skeleton className="w-10 h-10 rounded-full" />
                    <div className="flex-1">
                      <Skeleton className="w-36 h-5" />
                      <Skeleton className="w-full h-4" />
                    </div>
                    <Skeleton className="w-14 h-4" />
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="md:col-span-4">
          <RiskInsightsSkeleton />
        </div>
      </div>
    </div>
  );
}

/**
 * Skeleton for Hill Chart
 */
export function HillChartSkeleton() {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="flex justify-between items-center mb-2">
          <Skeleton className="w-36 h-7" />
          <div className="flex gap-1">
            <Skeleton className="w-20 h-8 rounded-md" />
            <Skeleton className="w-20 h-8 rounded-md" />
          </div>
        </div>
        <Skeleton className="w-full h-72 rounded-lg mb-2" />
        {/* Legend */}
        <div className="flex justify-center gap-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex items-center gap-1">
              <Skeleton className="w-3 h-3 rounded-full" />
              <Skeleton className="w-14 h-4" />
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

/**
 * Generic table skeleton
 */
export function TableSkeleton({ rows = 5, columns = 4 }: { rows?: number; columns?: number }) {
  return (
    <div>
      {/* Header row */}
      <div className="flex gap-2 py-2 border-b-2 border-border">
        {Array.from({ length: columns }).map((_, i) => (
          <Skeleton key={i} className="h-6" style={{ width: `${100 / columns}%` }} />
        ))}
      </div>
      {/* Data rows */}
      {Array.from({ length: rows }).map((_, rowIdx) => (
        <div key={rowIdx} className="flex gap-2 py-2 border-b border-border">
          {Array.from({ length: columns }).map((_, colIdx) => (
            <Skeleton key={colIdx} className="h-5" style={{ width: `${100 / columns}%` }} />
          ))}
        </div>
      ))}
    </div>
  );
}

/**
 * Generic form skeleton
 */
export function FormSkeleton({ fields = 4 }: { fields?: number }) {
  return (
    <Card className="max-w-xl">
      <CardContent className="p-4">
        <div className="space-y-3">
          {Array.from({ length: fields }).map((_, i) => (
            <Skeleton key={i} className="h-14 rounded-md" />
          ))}
          <div className="flex gap-2 justify-end">
            <Skeleton className="w-20 h-9 rounded-md" />
            <Skeleton className="w-24 h-9 rounded-md" />
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

/**
 * Skeleton for project cards
 */
export function ProjectCardsSkeleton() {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
      {[1, 2, 3, 4, 5, 6].map((i) => (
        <Card key={i}>
          <CardContent className="p-4">
            <div className="flex items-center mb-2">
              <Skeleton className="w-10 h-10 rounded-full mr-2" />
              <div>
                <Skeleton className="w-28 h-6" />
                <Skeleton className="w-14 h-5 mt-0.5 rounded-full" />
              </div>
            </div>
            <Skeleton className="w-full h-4" />
            <Skeleton className="w-4/5 h-4" />
            <div className="flex gap-1 mt-2">
              <Skeleton className="w-20 h-6 rounded-full" />
              <Skeleton className="w-16 h-6 rounded-full" />
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

/**
 * Skeleton for Health Overview page
 */
export function HealthOverviewSkeleton() {
  return (
    <div className="p-3">
      {/* Page Header */}
      <Skeleton className="w-2/5 h-12 mb-1" />
      <Skeleton className="w-3/5 h-6 mb-4" />

      {/* Tabs */}
      <div className="border-b border-border mb-3">
        <div className="flex gap-3 pb-1">
          <Skeleton className="w-36 h-12" />
          <Skeleton className="w-36 h-12" />
        </div>
      </div>

      {/* Cycle Health Cards */}
      <div className="grid grid-cols-1 gap-3">
        {[1, 2, 3].map((i) => (
          <Card key={i}>
            <CardContent className="p-4">
              {/* Cycle Header */}
              <div className="flex justify-between items-center mb-3">
                <div>
                  <Skeleton className="w-60 h-9" />
                  <Skeleton className="w-44 h-5 mt-1" />
                </div>
                <Skeleton className="w-20 h-8 rounded-md" />
              </div>

              {/* Stats Row */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mb-3">
                {[1, 2, 3, 4].map((j) => (
                  <div key={j} className="text-center p-2 bg-muted/50 rounded-lg">
                    <Skeleton className="w-20 h-10 mx-auto mb-1" />
                    <Skeleton className="w-24 h-5 mx-auto" />
                  </div>
                ))}
              </div>

              {/* Progress Bar */}
              <div className="mb-3">
                <Skeleton className="w-36 h-5 mb-1" />
                <Skeleton className="w-full h-2 rounded-full" />
              </div>

              {/* Pitch Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2">
                {[1, 2, 3].map((k) => (
                  <Card key={k} className="border">
                    <CardContent className="p-3">
                      <div className="flex justify-between items-start mb-2">
                        <Skeleton className="w-[70%] h-6" />
                        <Skeleton className="w-6 h-6 rounded-full" />
                      </div>
                      <Skeleton className="w-1/2 h-5 mb-1" />
                      <Skeleton className="w-full h-2 rounded-full mb-2" />
                      <div className="flex gap-1">
                        <Skeleton className="w-16 h-6 rounded-full" />
                        <Skeleton className="w-14 h-6 rounded-full" />
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Legend Section */}
      <div className="mt-4">
        <Card>
          <CardContent className="p-4">
            <Skeleton className="w-48 h-6 mb-2" />
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="flex items-center gap-1">
                  <Skeleton className="w-4 h-4 rounded-full" />
                  <Skeleton className="w-36 h-5" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
