import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
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
      showError(getUserFriendlyError(err, 'Failed to load metric'));
    } finally {
      setLoading(false);
    }
  };

  const validateFormula = async () => {
    if (!formData.formula.trim()) {
      setValidationResult({ valid: false, error: 'Formula is required' });
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
      setValidationResult({ valid: false, error: 'Failed to validate formula' });
    } finally {
      setValidating(false);
    }
  };

  const validateForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Metric name is required';
    }

    if (!formData.formula.trim()) {
      newErrors.formula = 'Formula is required';
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
        showSuccess('Metric updated successfully');
      } else {
        await customMetricService.create(formData);
        showSuccess('Metric created successfully');
      }
      navigate('/metrics');
    } catch (err) {
      showError(getUserFriendlyError(err, 'Failed to save metric'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container mx-auto py-6 px-4 max-w-4xl">
      <div className="mb-6">
        <Button variant="ghost" onClick={() => navigate('/metrics')}>
          <ArrowLeft className="mr-2 h-4 w-4" />
          Back to Metrics
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{isEdit ? 'Edit Metric' : 'Create Custom Metric'}</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Name */}
            <div className="space-y-2">
              <Label htmlFor="name">Metric Name *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => {
                  setFormData({ ...formData, name: e.target.value });
                  setErrors({ ...errors, name: '' });
                }}
                placeholder="e.g., Cycle Completion Rate"
                className={errors.name ? 'border-destructive' : ''}
              />
              {errors.name && (
                <p className="text-xs text-destructive">{errors.name}</p>
              )}
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Describe what this metric measures..."
                rows={2}
              />
            </div>

            {/* Data Source */}
            <div className="space-y-2">
              <Label>Data Source *</Label>
              <Select
                value={formData.dataSource}
                onValueChange={(value) => setFormData({ ...formData, dataSource: value as MetricDataSource })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={MetricDataSource.PITCH}>Pitches</SelectItem>
                  <SelectItem value={MetricDataSource.CYCLE}>Cycles</SelectItem>
                  <SelectItem value={MetricDataSource.TASK}>Tasks</SelectItem>
                  <SelectItem value={MetricDataSource.WORK_LOG}>Work Logs</SelectItem>
                  <SelectItem value={MetricDataSource.TEAM}>Teams</SelectItem>
                  <SelectItem value={MetricDataSource.CUSTOM}>Custom</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Aggregation Type */}
            <div className="space-y-2">
              <Label>Aggregation Type *</Label>
              <Select
                value={formData.aggregationType}
                onValueChange={(value) => setFormData({ ...formData, aggregationType: value as MetricAggregationType })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={MetricAggregationType.COUNT}>Count</SelectItem>
                  <SelectItem value={MetricAggregationType.SUM}>Sum</SelectItem>
                  <SelectItem value={MetricAggregationType.AVG}>Average</SelectItem>
                  <SelectItem value={MetricAggregationType.MIN}>Minimum</SelectItem>
                  <SelectItem value={MetricAggregationType.MAX}>Maximum</SelectItem>
                  <SelectItem value={MetricAggregationType.RATIO}>Ratio</SelectItem>
                  <SelectItem value={MetricAggregationType.PERCENTAGE}>Percentage</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Display Format */}
            <div className="space-y-2">
              <Label>Display Format *</Label>
              <Select
                value={formData.displayFormat}
                onValueChange={(value) => setFormData({ ...formData, displayFormat: value as MetricDisplayFormat })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value={MetricDisplayFormat.NUMBER}>Number</SelectItem>
                  <SelectItem value={MetricDisplayFormat.PERCENTAGE}>Percentage</SelectItem>
                  <SelectItem value={MetricDisplayFormat.CURRENCY}>Currency</SelectItem>
                  <SelectItem value={MetricDisplayFormat.DURATION}>Duration</SelectItem>
                  <SelectItem value={MetricDisplayFormat.DECIMAL}>Decimal</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Scope Section */}
            <div className="border-t pt-4 space-y-4">
              <div>
                <h3 className="font-medium mb-1">Scope (Optional)</h3>
                <p className="text-sm text-muted-foreground">
                  Limit this metric to a specific cycle, pitch, or team
                </p>
              </div>

              {/* Cycle Scope */}
              <div className="space-y-2">
                <Label>Cycle</Label>
                <Select
                  value={formData.cycleId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ 
                    ...formData, 
                    cycleId: value === 'none' ? undefined : parseInt(value) 
                  })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="All cycles" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">All cycles</SelectItem>
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
                <Label>Pitch</Label>
                <Select
                  value={formData.pitchId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ 
                    ...formData, 
                    pitchId: value === 'none' ? undefined : parseInt(value) 
                  })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="All pitches" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">All pitches</SelectItem>
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
                <Label>Team</Label>
                <Select
                  value={formData.teamId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ 
                    ...formData, 
                    teamId: value === 'none' ? undefined : parseInt(value) 
                  })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="All teams" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">All teams</SelectItem>
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
                <Label htmlFor="formula">Formula *</Label>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={validateFormula}
                  disabled={validating || !formData.formula.trim()}
                >
                  {validating ? 'Validating...' : 'Validate Formula'}
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
                placeholder="e.g., completedPitches / totalPitches * 100"
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
                      ? 'Formula is valid'
                      : validationResult.error || 'Formula is invalid'}
                  </AlertDescription>
                </Alert>
              )}

              <p className="text-xs text-muted-foreground">
                Supported operators: +, -, *, /, ( )
                <br />
                Supported functions: SUM, AVG, COUNT, MIN, MAX
                <br />
                Field references: pitch.appetiteDays, workLog.hours, task.status, etc.
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
                Cancel
              </Button>
              <LoadingButton type="submit" loading={loading}>
                <Save className="mr-2 h-4 w-4" />
                {isEdit ? 'Update Metric' : 'Create Metric'}
              </LoadingButton>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
