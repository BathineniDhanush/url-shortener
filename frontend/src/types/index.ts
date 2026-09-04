export interface CreateLinkRequest {
  destinationUrl: string;
  customAlias?: string | null;
  expiresAt?: string | null;
}

export interface LinkResponse {
  id: string;
  code: string;
  shortUrl: string;
  destinationUrl: string;
  status: 'ACTIVE' | 'DISABLED';
  expiresAt?: string | null;
  createdAt: string;
  version: number;
  ownerToken?: string;
}

export interface UpdateLinkRequest {
  expectedVersion: number;
  destinationUrl?: string | null;
  status?: 'ACTIVE' | 'DISABLED' | null;
  expiresAt?: string | null;
}

export interface AnalyticsResponse {
  code: string;
  totalClicks: number;
}

export interface SystemInfo {
  service: string;
  version: string;
  runtimeRole: 'API' | 'WORKER';
}

export interface HealthResponse {
  status: string;
}

export interface Problem {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
  errors?: string[];
}
