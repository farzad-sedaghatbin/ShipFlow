import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDateTime } from '../utils/dateLocalization';
import { Plus, Calculator, Edit, Trash2, Copy } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { useToast } from '../contexts';
import { customMetricService } from '../services/customMetricService';
import { CustomMetric, MetricValue } from '../types/metrics';
import { getUserFriendlyError } from '../utils/errorMessages';
import { TableSkeleton } from '../components/Skeletons';

export default function CustomMetrics() {
  const { t, i18n } = useTranslation();
  const [metrics, setMetrics] = useState<CustomMetric[]>([]);
  const [metricValues, setMetricValues] = useState<Map<number, MetricValue>>(new Map());
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    loadMetrics();
  }, []);

  const loadMetrics = async () => {
    try {
      setLoading(true);
      const response = await customMetricService.getAll();
      setMetrics(response);
      
      // Calculate values for all metrics
      const values = new Map<number, MetricValue>();
      await Promise.all(
        response.map(async (metric: CustomMetric) => {
          try {
            const valueResponse = await customMetricService.calculate(metric.id);
            values.set(metric.id, valueResponse);
          } catch (err) {
            console.error(`Failed to calculate metric ${metric.id}:`, err);
          }
        })
      );
      setMetricValues(values);
    } catch (err) {
      showError(getUserFriendlyError(err, t('customMetrics.loadFailed')));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm(t('customMetrics.deleteConfirm'))) return;
    
    try {
      await customMetricService.delete(id);
      showSuccess(t('customMetrics.deleteSuccess'));
      loadMetrics();
    } catch (err) {
      showError(getUserFriendlyError(err, t('customMetrics.deleteFailed')));
    }
  };

  const handleDuplicate = async (metric: CustomMetric) => {
    try {
      await customMetricService.create({
        name: `${metric.name} (Copy)`,
        description: metric.description,
        formula: metric.formula,
        dataSource: metric.dataSource,
        aggregationType: metric.aggregationType,
        displayFormat: metric.displayFormat,
        filters: metric.filters
      });
      showSuccess(t('customMetrics.duplicateSuccess'));
      loadMetrics();
    } catch (err) {
      showError(getUserFriendlyError(err, t('customMetrics.duplicateFailed')));
    }
  };

  const filteredMetrics = metrics.filter(m =>
    m.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    m.description?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    m.formula.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold">{t('customMetrics.title')}</h1>
        </div>
        <TableSkeleton rows={5} columns={5} />
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">{t('customMetrics.title')}</h1>
        <Button onClick={() => navigate('/metrics/new')}>
          <Plus className="mr-2 h-4 w-4" />
          {t('customMetrics.createMetric')}
        </Button>
      </div>

      <Card className="mb-6">
        <CardHeader>
          <CardTitle>{t('customMetrics.searchMetrics')}</CardTitle>
        </CardHeader>
        <CardContent>
          <Input
            placeholder={t('customMetrics.searchPlaceholder')}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </CardContent>
      </Card>

      {filteredMetrics.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Calculator className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold mb-2">{t('customMetrics.noMetricsFound')}</h3>
            <p className="text-muted-foreground mb-4">
              {searchTerm ? t('customMetrics.tryDifferentSearch') : t('customMetrics.firstMetricPrompt')}
            </p>
            {!searchTerm && (
              <Button onClick={() => navigate('/metrics/new')}>
                <Plus className="mr-2 h-4 w-4" />
                {t('customMetrics.createMetric')}
              </Button>
            )}
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4">
          {filteredMetrics.map((metric) => {
            const value = metricValues.get(metric.id);
            return (
              <Card key={metric.id}>
                <CardHeader>
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <CardTitle className="text-xl">{metric.name}</CardTitle>
                      {metric.description && (
                        <p className="text-sm text-muted-foreground mt-1">{metric.description}</p>
                      )}
                    </div>
                    <div className="text-right">
                      {value ? (
                        <div>
                          <div className="text-3xl font-bold">{value.formattedValue}</div>
                          <div className="text-xs text-muted-foreground">
                            {t('customMetrics.updated', { date: formatLocalizedDateTime(new Date(value.calculatedAt), i18n.language) })}
                          </div>
                        </div>
                      ) : (
                        <div className="text-sm text-muted-foreground">{t('customMetrics.calculating')}</div>
                      )}
                    </div>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    <div className="flex items-center gap-4 text-sm">
                      <span className="font-medium">{t('customMetrics.formula')}</span>
                      <code className="bg-muted px-2 py-1 rounded text-xs">{metric.formula}</code>
                    </div>
                    <div className="flex items-center gap-4 text-sm">
                      <span className="font-medium">{t('customMetrics.dataSource')}</span>
                      <span className="text-muted-foreground">{metric.dataSource}</span>
                      <span className="font-medium ml-4">{t('customMetrics.aggregation')}</span>
                      <span className="text-muted-foreground">{metric.aggregationType}</span>
                      <span className="font-medium ml-4">{t('customMetrics.format')}</span>
                      <span className="text-muted-foreground">{metric.displayFormat}</span>
                    </div>
                    <div className="flex justify-end gap-2 pt-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/metrics/${metric.id}/history`)}
                      >
                        {t('customMetrics.viewHistory')}
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleDuplicate(metric)}
                      >
                        <Copy className="mr-2 h-4 w-4" />
                        {t('customMetrics.duplicate')}
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/metrics/${metric.id}/edit`)}
                      >
                        <Edit className="mr-2 h-4 w-4" />
                        {t('customMetrics.edit')}
                      </Button>
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => handleDelete(metric.id)}
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        {t('customMetrics.delete')}
                      </Button>
                    </div>
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
