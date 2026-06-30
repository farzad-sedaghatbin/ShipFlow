import api from './api';
import {
  CustomFieldDefinition,
  CustomFieldValue,
  CustomFieldEntityType,
  CreateCustomFieldDefinitionRequest,
  UpdateCustomFieldDefinitionRequest,
  BulkUpsertCustomFieldValuesRequest,
} from '../types';

export const customFieldService = {
  getDefinitions: (entityType: CustomFieldEntityType, projectId?: number) =>
    api.get<CustomFieldDefinition[]>('/custom-fields/definitions', {
      params: { entityType, ...(projectId != null ? { projectId } : {}) },
    }),

  createDefinition: (req: CreateCustomFieldDefinitionRequest) =>
    api.post<CustomFieldDefinition>('/custom-fields/definitions', req),

  updateDefinition: (id: number, req: UpdateCustomFieldDefinitionRequest) =>
    api.put<CustomFieldDefinition>(`/custom-fields/definitions/${id}`, req),

  deleteDefinition: (id: number) =>
    api.delete(`/custom-fields/definitions/${id}`),

  getValues: (entityType: CustomFieldEntityType, entityId: number) =>
    api.get<CustomFieldValue[]>('/custom-fields/values', {
      params: { entityType, entityId },
    }),

  bulkUpsertValues: (req: BulkUpsertCustomFieldValuesRequest) =>
    api.put<CustomFieldValue[]>('/custom-fields/values/bulk', req),
};
