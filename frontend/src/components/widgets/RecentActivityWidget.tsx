import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Activity } from 'lucide-react';
import RecentActivityFeed from '../RecentActivityFeed';

interface RecentActivityWidgetProps {
  projectId?: number;
}

export function RecentActivityWidget({ projectId }: RecentActivityWidgetProps) {
  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          <Activity className="w-4 h-4 text-emerald-500" />
          Recent Activity
        </CardTitle>
      </CardHeader>
      <CardContent className="pt-0">
        <div className="-mt-2">
          <RecentActivityFeed 
            maxItems={5} 
            compact 
            projectId={projectId}
          />
        </div>
      </CardContent>
    </Card>
  );
}
