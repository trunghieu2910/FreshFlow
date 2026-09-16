import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { normalizeApiError } from './error';

/**
 * Base URL resolved from environment variables with fallback
 */
export const API_BASE_URL: string =
  (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_URL) ||
  (typeof import.meta !== 'undefined' && import.meta.env?.VITE_API_BASE_URL) ||
  (typeof process !== 'undefined' && process.env?.VITE_API_URL) ||
  (typeof process !== 'undefined' && process.env?.VITE_API_BASE_URL) ||
  'http://localhost:8080';

/**
 * Singleton Axios client instance configured for FreshFlow API
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

/**
 * Request Interceptor: Injects X-User-Id identity header for merchant operations
 */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const actorUserId = localStorage.getItem('freshflow_actor_user_id') || '1';
    config.headers.set('X-User-Id', actorUserId);
    return config;
  },
  (error: unknown) => {
    return Promise.reject(normalizeApiError(error));
  }
);

/**
 * Response Interceptor: Unwraps response data and normalizes errors into ApiError
 */
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data;
  },
  (error: unknown) => {
    return Promise.reject(normalizeApiError(error));
  }
);
