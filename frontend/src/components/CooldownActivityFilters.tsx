import { useTranslation } from 'react-i18next';
import { Card, CardHeader } from './ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { CooldownActivityType, CooldownActivityStatus } from '../services/cooldownActivityService';

interface CooldownActivityFiltersProps {
  filterType: string;
  filterStatus: string;
  onTypeChange: (value: string) => void;
  onStatusChange: (value: string) => void;
}

export default function CooldownActivityFilters({
  filterType,
  filterStatus,
  onTypeChange,
  onStatusChange,
}: CooldownActivityFiltersProps) {
  const { t } = useTranslation();

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-4">
          <div className="flex-1">
            <label className="text-sm font-medium mb-2 block">{t('cooldownActivity.filterByType')}</label>
            <Select value={filterType} onValueChange={onTypeChange}>
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('cooldownActivity.allTypes')}</SelectItem>
                <SelectItem value={CooldownActivityType.TECH_DEBT}>{t('cooldownActivity.types.techDebt')}</SelectItem>
                <SelectItem value={CooldownActivityType.BUG_FIX}>{t('cooldownActivity.types.bugFix')}</SelectItem>
                <SelectItem value={CooldownActivityType.REFACTORING}>{t('cooldownActivity.types.refactoring')}</SelectItem>
                <SelectItem value={CooldownActivityType.KICKOFF_PREP}>{t('cooldownActivity.types.kickoffPrep')}</SelectItem>
                <SelectItem value={CooldownActivityType.LEARNING}>{t('cooldownActivity.types.learning')}</SelectItem>
                <SelectItem value={CooldownActivityType.QA}>{t('cooldownActivity.types.qa')}</SelectItem>
                <SelectItem value={CooldownActivityType.DOCUMENTATION}>{t('cooldownActivity.types.documentation')}</SelectItem>
                <SelectItem value={CooldownActivityType.INFRASTRUCTURE}>{t('cooldownActivity.types.infrastructure')}</SelectItem>
                <SelectItem value={CooldownActivityType.RESEARCH}>{t('cooldownActivity.types.research')}</SelectItem>
                <SelectItem value={CooldownActivityType.PLANNING}>{t('cooldownActivity.types.planning')}</SelectItem>
                <SelectItem value={CooldownActivityType.OTHER}>{t('cooldownActivity.types.other')}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="flex-1">
            <label className="text-sm font-medium mb-2 block">{t('cooldownActivity.filterByStatus')}</label>
            <Select value={filterStatus} onValueChange={onStatusChange}>
              <SelectTrigger className="w-48">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">{t('cooldownActivity.allStatuses')}</SelectItem>
                <SelectItem value={CooldownActivityStatus.PLANNED}>{t('cooldownActivity.statuses.planned')}</SelectItem>
                <SelectItem value={CooldownActivityStatus.IN_PROGRESS}>{t('cooldownActivity.statuses.inProgress')}</SelectItem>
                <SelectItem value={CooldownActivityStatus.COMPLETED}>{t('cooldownActivity.statuses.completed')}</SelectItem>
                <SelectItem value={CooldownActivityStatus.SKIPPED}>{t('cooldownActivity.statuses.skipped')}</SelectItem>
                <SelectItem value={CooldownActivityStatus.BLOCKED}>{t('cooldownActivity.statuses.blocked')}</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </CardHeader>
    </Card>
  );
}
