import axios from 'axios';
import type { Problem } from '../types';

export function apiErrorMessage(error: unknown, fallback: string): string {
  if (!axios.isAxiosError<Problem>(error)) return fallback;
  return error.response?.data?.detail || error.message || fallback;
}
