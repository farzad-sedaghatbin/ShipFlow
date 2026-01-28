import api from './api';
import {
  CustomMetric,
  CreateCustomMetricRequest,
  UpdateCustomMetricRequest,
  MetricValue,
  MetricValidationResult
} from '../types/metrics';

export const customMetricService = {
  /**
   * Create a new custom metric
   */
  create: async (data: CreateCustomMetricRequest) => {
    const response = await api.post<CustomMetric>('/metrics/custom', data);
    return response.data;
  },

  /**
   * Get all custom metrics for the current user
   */
  getAll: async () => {
    const response = await api.get<CustomMetric[]>('/metrics/custom');
    return response.data;
  },

  /**
   * Get a specific custom metric by ID
   */
  getById: async (id: number) => {
    const response = await api.get<CustomMetric>(`/metrics/custom/${id}`);
    return response.data;
  },

  /**
   * Update an existing custom metric
   */
  update: async (id: number, data: UpdateCustomMetricRequest) => {
    const response = await api.put<CustomMetric>(`/metrics/custom/${id}`, data);
    return response.data;
  },

  /**
   * Delete a custom metric
   */
  delete: async (id: number) => {
    const response = await api.delete(`/metrics/custom/${id}`);
    return response.data;
  },

  /**
   * Calculate the current value of a metric
   */
  calculate: async (id: number) => {
    const response = await api.post<MetricValue>(`/metrics/custom/${id}/calculate`);
    return response.data;
  },

  /**
   * Get historical values for a metric
   */
  getHistory: async (id: number, days: number = 30) => {
    const response = await api.get<MetricValue[]>(`/metrics/custom/${id}/history`, {
      params: { days }
    });
    return response.data;
  },

  /**
   * Validate a metric formula
   */
  validateFormula: async (formula: string) => {
    const response = await api.post<MetricValidationResult>('/metrics/custom/validate', formula, {
      headers: { 'Content-Type': 'text/plain' }
    });
    return response.data;
  }
};

export default customMetricService;
