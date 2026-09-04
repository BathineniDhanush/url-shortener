import apiClient from './client';
import type {
  CreateLinkRequest,
  LinkResponse,
  UpdateLinkRequest,
  AnalyticsResponse,
  SystemInfo,
  HealthResponse
} from '../types';

export const linkService = {
  async createLink(data: CreateLinkRequest): Promise<LinkResponse> {
    const response = await apiClient.post<LinkResponse>('/api/v1/links', data);
    return response.data;
  },

  async getLinkDetails(code: string, token: string): Promise<LinkResponse> {
    const response = await apiClient.get<LinkResponse>(`/api/v1/links/${code}`, {
      headers: { 'X-Link-Owner-Token': token },
    });
    return response.data;
  },

  async updateLink(code: string, token: string, data: UpdateLinkRequest): Promise<LinkResponse> {
    const response = await apiClient.patch<LinkResponse>(`/api/v1/links/${code}`, data, {
      headers: { 'X-Link-Owner-Token': token },
    });
    return response.data;
  },

  async deleteLink(code: string, token: string, expectedVersion: number): Promise<void> {
    await apiClient.delete(`/api/v1/links/${encodeURIComponent(code)}`, {
      params: { expectedVersion },
      headers: { 'X-Link-Owner-Token': token },
    });
  },

  async getLinkAnalytics(code: string, token: string): Promise<AnalyticsResponse> {
    const response = await apiClient.get<AnalyticsResponse>(`/api/v1/links/${code}/analytics`, {
      headers: { 'X-Link-Owner-Token': token },
    });
    return response.data;
  },

  async getSystemInfo(): Promise<SystemInfo> {
    const response = await apiClient.get<SystemInfo>('/api/v1/system/info');
    return response.data;
  },

  async getHealth(): Promise<HealthResponse> {
    const response = await apiClient.get<HealthResponse>('/actuator/health');
    if (!response.data || typeof response.data.status !== 'string') {
      throw new Error('The API returned a malformed health response.');
    }
    return response.data;
  },
};
