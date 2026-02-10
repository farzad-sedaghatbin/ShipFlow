import { useTranslation } from 'react-i18next';
import { Button } from './ui/button';
import { Badge } from './ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from './ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from './ui/dropdown-menu';
import { Plus, MoreVertical, Pencil, Trash2 } from 'lucide-react';
import { CooldownActivityDTO } from '../services/cooldownActivityService';
import {
  getActivityTypeIcon,
  getActivityTypeBadgeColor,
  getStatusBadgeColor,
  getStatusIcon,
} from '../utils/cooldownActivityUtils';

interface CooldownActivityTableProps {
  activities: CooldownActivityDTO[];
  onEdit: (activity: CooldownActivityDTO) => void;
  onDelete: (activity: CooldownActivityDTO) => void;
  onCreate: () => void;
}

export default function CooldownActivityTable({
  activities,
  onEdit,
  onDelete,
  onCreate,
}: CooldownActivityTableProps) {
  const { t } = useTranslation();

  if (activities.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-muted-foreground">{t('cooldownActivity.noActivities')}</p>
        <Button onClick={onCreate} variant="outline" className="mt-4">
          <Plus className="h-4 w-4 me-2" />
          {t('cooldownActivity.createFirstActivity')}
        </Button>
      </div>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>{t('cooldownActivity.title')}</TableHead>
          <TableHead>{t('cooldownActivity.type')}</TableHead>
          <TableHead>{t('cooldownActivity.status')}</TableHead>
          <TableHead>{t('cooldownActivity.assignee')}</TableHead>
          <TableHead className="text-end">{t('cooldownActivity.estimated')}</TableHead>
          <TableHead className="text-end">{t('cooldownActivity.actual')}</TableHead>
          <TableHead className="text-end">{t('common.actions')}</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {activities.map((activity) => (
          <TableRow key={activity.id}>
            <TableCell>
              <div>
                <div className="font-medium">{activity.title}</div>
                {activity.description && (
                  <div className="text-sm text-muted-foreground line-clamp-1">
                    {activity.description}
                  </div>
                )}
              </div>
            </TableCell>
            <TableCell>
              <Badge variant="outline" className={getActivityTypeBadgeColor(activity.activityType)}>
                {getActivityTypeIcon(activity.activityType)} {t(`cooldownActivity.types.${activity.activityType.toLowerCase()}`)}
              </Badge>
            </TableCell>
            <TableCell>
              <Badge variant="outline" className={`${getStatusBadgeColor(activity.status)} flex items-center gap-1 w-fit`}>
                {getStatusIcon(activity.status)}
                {t(`cooldownActivity.statuses.${activity.status === 'IN_PROGRESS' ? 'inprogress' : activity.status.toLowerCase()}`)}
              </Badge>
            </TableCell>
            <TableCell>
              {activity.assigneeUsername || (
                <span className="text-muted-foreground">{t('cooldownActivity.unassigned')}</span>
              )}
            </TableCell>
            <TableCell className="text-end">
              {activity.estimatedHours ? `${activity.estimatedHours}h` : '-'}
            </TableCell>
            <TableCell className="text-end">
              {activity.actualHours ? `${activity.actualHours}h` : '-'}
            </TableCell>
            <TableCell className="text-end">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="sm">
                    <MoreVertical className="h-4 w-4" />
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end">
                  <DropdownMenuItem onClick={() => onEdit(activity)}>
                    <Pencil className="h-4 w-4 me-2" />
                    {t('common.edit')}
                  </DropdownMenuItem>
                  <DropdownMenuItem
                    onClick={() => onDelete(activity)}
                    className="text-red-600"
                  >
                    <Trash2 className="h-4 w-4 me-2" />
                    {t('common.delete')}
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
