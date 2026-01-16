import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Save, ArrowLeft, CheckCircle, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Alert, AlertDescription } from '../components/ui/alert';
import { useToast } from '../contexts';
import { customMetricService } from '../services/customMetricService';
import { cycleService } from '../services/cycleService';
import { pitchService } from '../services/pitchService';
import { teamService } from '../services/teamService';
import {
  MetricDataSource,
  MetricAggregationType,
  MetricDisplayFormat,
  CreateCustomMetricRequest
} from '../types/metrics';
import { Cycle, Pitch, Team } from '../types';
import { getUserFriendlyError } from '../utils/errorMessages';
import LoadingButton from '../components/LoadingButton';

export default function MetricBuilder() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();

  const [loading, setLoading] = useState(false);
  const [validating, setValidating] = useState(false);
  const [validationResult, setValidationResult] = useState<{ valid: boolean; error?: string } | null>(null);

  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);

  const [formData, setFormData] = useState<CreateCustomMetricRequest>({
    name: '',
    description: '',
    formula: '',
    dataSource: MetricDataSource.PITCH,
    aggregationType: MetricAggregationType.COUNT,
    displayFormat: MetricDisplayFormat.NUMBER
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    loadScopeData();
  }, []);

  useEffect(() => {
    if (isEdit && id) {
      loadMetric(parseInt(id));
    }
  }, [id, isEdit]);

  const loadScopeData = async () => {
    try {
      const [cyclesData, pitchesData, teamsData] = await Promise.all([
        cycleService.getAll(),
        pitchService.getAll(),
        teamService.getAll()
      ]);
      setCycles(cyclesData.data);
      setPitches(pitchesData.data);
      setTeams(teamsData.data);
    } catch (err) {
      console.error('Failed to load scope data:', err);
    }
  };

  const loadMetric = async (metricId: number) => {
    try {
      setLoading(true);
      const response = await customMetricService.getById(metricId);
      const metric = response;
      setFormData({
        name: metric.name,
        description: metric.description,
        formula: metric.formula,
        dataSource: metric.dataSource,
        aggregationType: metric.aggregationType,
        displayFormat: metric.displayFormat,
        filters: metric.filters
      });
    } catch (err) {
      showError(getUserFriendlyError(err, t('metricBuilder.loadFailed')));
    } finally {
      setLoading(false);
    }
  };

  const validateFormula = async () => {
    if (!formData.formula.trim()) {
      setValidationResult({ valid: false, error: t('metricBuilder.formulaRequired') });
      return;
    }

    try {
      setValidating(true);
      const response = await customMetricService.validateFormula(formData.formula);
      setValidationResult({
        valid: response.valid,
        error: response.errorMessage
      });
    } catch (err) {
      setValidationResult({ valid: false, error: t('metricBuilder.validationFailed') });
    } finally {
      setValidating(false);
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = t('metricBuilder.nameRequired');
    }

    if (!formData.formula.trim()) {
      newErrors.formula = t('metricBuilder.formulaRequired');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) return;

    try {
      setLoading(true);
      if (isEdit && id) {
        await customMetricService.update(parseInt(id), formData);
        showSuccess(t('metricBuilder.metricUpdated'));
      } else {
        await customMetricService.create(formData);
        showSuccess(t('metricBuilder.metricCreated'));
      }
      navigate('/metrics');
    } catch (err) {
      showError(getUserFriendlyError(err, t('metricBuilder.saveFailed')));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mx-auto py-6 px-4 max-w-4xl">
      <div className="mb-6">
        <Button variant="ghost" onClick={() => navigate('/metrics')}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          {t('metricBuilder.backToMetrics')}
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{isEdit ? t('metricBuilder.editMetric') : t('metricBuilder.createMetric')}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Name */}
            <div className="space-y-2">
              <Label htmlFor="name">{t('metricBuilder.metricName')} *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => {
                  setFormData({ ...formData, name: e.target.value });
                  setErrors({ ...errors, name: '' });
                }}
                placeholder={t('metricBuilder.namePlaceholder')}
                className={errors.name ? 'border-destructive' : ''}
              />
              {errors.name && (
                <p className="text-xs text-destructive">{errors.name}</p>
              )}
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label htmlFor="description">{t('metricBuilder.description')}</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder={t('metricBuilder.descriptionPlaceholder')}
                rows={2}
              />
            </div>

            {/* Data Source */}
            <div className="space-y-2">
              <Label>{t('metricBuilder.dataSource')} *</Label>
              <Select
                value={formData.dataSource}
                onValueChange={(value) => setFormData({ ...formData, dataSource: value as MetricDataSource })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={MetricDataSource.PITCH}>{t('metricBuilder.pitches')}</SelectItem>
                  <SelectItem value={MetricDataSource.CYCLE}>{t('metricBuilder.cycles')}</SelectItem>
                  <SelectItem value={MetricDataSource.TASK}>{t('metricBuilder.tasks')}</SelectItem>
                  <SelectItem value={MetricDataSource.WORK_LOG}>{t('metricBuilder.workLogs')}</SelectItem>
                  <SelectItem value={MetricDataSource.TEAM}>{t('metricBuilder.teams')}</SelectItem>
                  <SelectItem value={MetricDataSource.CUSTOM}>{t('metricBuilder.custom')}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Aggregation Type */}
            <div className="space-y-2">
              <Label>{t('metricBuilder.aggregationType')} *</Label>
              <Select
                value={formData.aggregationType}
                onValueChange={(value) => setFormData({ ...formData, aggregationType: value as MetricAggregationType })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={MetricAggregationType.COUNT}>{t('metricBuilder.count')}</SelectItem>
                  <SelectItem value={MetricAggregationType.SUM}>{t('metricBuilder.sum')}</SelectItem>
                  <SelectItem value={MetricAggregationType.AVG}>{t('metricBuilder.average')}</SelectItem>
                  <SelectItem value={MetricAggregationType.MIN}>{t('metricBuilder.minimum')}</SelectItem>
                  <SelectItem value={MetricAggregationType.MAX}>{t('metricBuilder.maximum')}</SelectItem>
                  <SelectItem value={MetricAggregationType.RATIO}>{t('metricBuilder.ratio')}</SelectItem>
                  <SelectItem value={MetricAggregationType.PERCENTAGE}>{t('metricBuilder.percentage')}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Display Format */}
            <div className="space-y-2">
              <Label>{t('metricBuilder.displayFormat')} *</Label>
              <Select
                value={formData.displayFormat}
                onValueChange={(value) => setFormData({ ...formData, displayFormat: value as MetricDisplayFormat })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={MetricDisplayFormat.NUMBER}>{t('metricBuilder.number')}</SelectItem>
                  <SelectItem value={MetricDisplayFormat.PERCENTAGE}>{t('metricBuilder.percentage')}</SelectItem>
                  <SelectItem value={MetricDisplayFormat.CURRENCY}>{t('metricBuilder.currency')}</SelectItem>
                  <SelectItem value={MetricDisplayFormat.DURATION}>{t('metricBuilder.duration')}</SelectItem>
                  <SelectItem value={MetricDisplayFormat.DECIMAL}>{t('metricBuilder.decimal')}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Scope Section */}
            <div className="border-t pt-4 space-y-4">
              <div>
                <h3 className="font-medium mb-1">{t('metricBuilder.scope')}</h3>
                <p className="text-sm text-muted-foreground">
                  {t('metricBuilder.scopeDesc')}
                </p>
              </div>

              {/* Cycle Scope */}
              <div className="space-y-2">
                <Label>{t('metricBuilder.cycle')}</Label>
                <Select
                  value={formData.cycleId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({
                    ...formData,
                    cycleId: value === 'none' ? undefined : parseInt(value)
                  })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('metricBuilder.allCycles')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">{t('metricBuilder.allCycles')}</SelectItem>
                    {cycles.map((cycle) => (
                      <SelectItem key={cycle.id} value={cycle.id.toString()}>
                        {cycle.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Pitch Scope */}
              <div className="space-y-2">
                <Label>{t('metricBuilder.pitch')}</Label>
                <Select
                  value={formData.pitchId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({
                    ...formData,
                    pitchId: value === 'none' ? undefined : parseInt(value)
                  })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('metricBuilder.allPitches')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">{t('metricBuilder.allPitches')}</SelectItem>
                    {pitches.map((pitch) => (
                      <SelectItem key={pitch.id} value={pitch.id.toString()}>
                        {pitch.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Team Scope */}
              <div className="space-y-2">
                <Label>{t('metricBuilder.team')}</Label>
                <Select
                  value={formData.teamId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({
                    ...formData,
                    teamId: value === 'none' ? undefined : parseInt(value)
                  })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder={t('metricBuilder.allTeams')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">{t('metricBuilder.allTeams')}</SelectItem>
                    {teams.map((team) => (
                      <SelectItem key={team.id} value={team.id.toString()}>
                        {team.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Formula */}
            <div className="space-y-2">
              <div className="flex justify-between items-center">
                <Label htmlFor="formula">{t('metricBuilder.formula')} *</Label>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={validateFormula}
                  disabled={validating || !formData.formula.trim()}
                >
                  {validating ? t('metricBuilder.validating') : t('metricBuilder.validateFormula')}
                </Button>
              </div>
              <Textarea
                id="formula"
                value={formData.formula}
                onChange={(e) => {
                  setFormData({ ...formData, formula: e.target.value });
                  setErrors({ ...errors, formula: '' });
                  setValidationResult(null);
                }}
                placeholder={t('metricBuilder.formulaPlaceholder')}
                rows={4}
                className={errors.formula ? 'border-destructive' : ''}
              />
              {errors.formula && (
                <p className="text-xs text-destructive">{errors.formula}</p>
              )}

              {validationResult && (
                <Alert variant={validationResult.valid ? 'default' : 'destructive'}>
                  {validationResult.valid ? (
                    <CheckCircle className="h-4 w-4" />
                  ) : (
                    <AlertCircle className="h-4 w-4" />
                  )}
                  <AlertDescription>
                    {validationResult.valid
                      ? t('metricBuilder.formulaValid')
                      : validationResult.error || t('metricBuilder.formulaInvalid')}
                  </AlertDescription>
                </Alert>
              )}

              <p className="text-xs text-muted-foreground">
                {t('metricBuilder.supportedOperators')}
                <br />
                {t('metricBuilder.supportedFunctions')}
                <br />
                {t('metricBuilder.fieldReferences')}
              </p>
            </div>

            {/* Actions */}
            <div className="flex justify-end gap-3 pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate('/metrics')}
                disabled={loading}
              >
                {t('metricBuilder.cancel')}
              </Button>
              <LoadingButton type="submit" loading={loading}>
                <Save className="mr-2 h-4 w-4" />
                {isEdit ? t('metricBuilder.updateMetric') : t('metricBuilder.createButton')}
              </LoadingButton>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
