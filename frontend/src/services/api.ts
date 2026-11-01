import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  User,
  Transaction,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  TransactionFilters,
  Category,
  CreateCategoryRequest,
  UpdateCategoryRequest,
  SubCategory,
  CreateSubCategoryRequest,
  UpdateSubCategoryRequest,
  PaginatedResponse,
} from '../types';

// ============================================
// Axios Instance Configuration
// ============================================

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// ============================================
// Errors
// ============================================

/**
 * Error thrown by every API call. `message` is the backend's own
 * `{ success: false, error }` text when there is one (e.g. "Invalid email
 * or password"), so pages can show it directly via `err.message`.
 */
export class ApiError extends Error {
  readonly status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/** Converts an axios (or any) error into an {@link ApiError} carrying the backend message. */
export function toApiError(error: unknown): Error {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Partial<ApiResponse<unknown>> | undefined;
    if (data?.error || data?.message) {
      return new ApiError((data.error || data.message) as string, error.response?.status);
    }
    if (error.response) {
      return new ApiError(`Request failed (HTTP ${error.response.status})`, error.response.status);
    }
    return new ApiError('Network error: could not reach the server. Please try again.');
  }
  return error instanceof Error ? error : new ApiError('Unexpected error');
}

// ============================================
// Session storage helpers
// ============================================

const clearSession = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
};

// ============================================
// Request Interceptor (Add Auth Token)
// ============================================

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// ============================================
// Response Interceptor (Handle Errors & Token Refresh)
// ============================================

/**
 * Endpoints whose 401 means "bad credentials / bad refresh token", NOT
 * "access token expired". They must never trigger a refresh + redirect,
 * otherwise a wrong password reloads the login page and the error is lost.
 */
const NO_REFRESH_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh'];

const isNoRefreshEndpoint = (url?: string) =>
  !!url && NO_REFRESH_ENDPOINTS.some((path) => url.endsWith(path));

/**
 * In-flight refresh, shared so that several requests failing with 401 at
 * the same time trigger a single /auth/refresh call instead of racing.
 */
let refreshPromise: Promise<string> | null = null;

const refreshAccessToken = (): Promise<string> => {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        throw new ApiError('Your session has expired. Please log in again.', 401);
      }
      // Plain axios (not apiClient) so this call bypasses these interceptors.
      const { data } = await axios.post<ApiResponse<AuthResponse>>(
        `${API_BASE_URL}/auth/refresh`,
        { refreshToken }
      );
      if (!data.success || !data.data) {
        throw new ApiError(data.error || 'Your session has expired. Please log in again.', 401);
      }
      localStorage.setItem('accessToken', data.data.accessToken);
      localStorage.setItem('refreshToken', data.data.refreshToken);
      return data.data.accessToken;
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
};

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    const originalRequest = error.config as RetriableConfig | undefined;

    // Expired access token: refresh once, then replay the original request.
    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isNoRefreshEndpoint(originalRequest.url)
    ) {
      originalRequest._retry = true;
      try {
        const accessToken = await refreshAccessToken();
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed - end the session and send the user to log in.
        clearSession();
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return Promise.reject(toApiError(refreshError));
      }
    }

    return Promise.reject(toApiError(error));
  }
);

// ============================================
// Auth API
// ============================================

export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>(
      '/auth/login',
      credentials
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Login failed');
    }
    return data.data;
  },

  register: async (userData: RegisterRequest): Promise<AuthResponse> => {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>(
      '/auth/register',
      userData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Registration failed');
    }
    return data.data;
  },

  /** Revokes the refresh token server-side, then clears the local session. */
  logout: async (): Promise<void> => {
    try {
      await apiClient.post('/auth/logout');
    } finally {
      clearSession();
    }
  },

  getCurrentUser: async (): Promise<User> => {
    const { data } = await apiClient.get<ApiResponse<User>>('/auth/me');
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch user');
    }
    return data.data;
  },
};

// ============================================
// Transactions API
// ============================================

export const transactionsApi = {
  getAll: async (filters?: TransactionFilters): Promise<Transaction[]> => {
    const { data } = await apiClient.get<ApiResponse<Transaction[]>>('/transactions', {
      params: filters,
    });
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch transactions');
    }
    return data.data;
  },

  /** `page` is 1-based; the backend clamps `limit` to 1..100. */
  getPaginated: async (
    page: number = 1,
    limit: number = 20,
    filters?: TransactionFilters
  ): Promise<PaginatedResponse<Transaction>> => {
    const { data } = await apiClient.get<PaginatedResponse<Transaction>>(
      '/transactions/paginated',
      {
        params: { page, limit, ...filters },
      }
    );
    if (!data.success) {
      throw new Error('Failed to fetch transactions');
    }
    return data;
  },

  getById: async (id: string): Promise<Transaction> => {
    const { data } = await apiClient.get<ApiResponse<Transaction>>(
      `/transactions/${id}`
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch transaction');
    }
    return data.data;
  },

  create: async (transactionData: CreateTransactionRequest): Promise<Transaction> => {
    const { data } = await apiClient.post<ApiResponse<Transaction>>(
      '/transactions',
      transactionData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to create transaction');
    }
    return data.data;
  },

  update: async (
    id: string,
    transactionData: UpdateTransactionRequest
  ): Promise<Transaction> => {
    const { data } = await apiClient.put<ApiResponse<Transaction>>(
      `/transactions/${id}`,
      transactionData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to update transaction');
    }
    return data.data;
  },

  delete: async (id: string): Promise<void> => {
    const { data } = await apiClient.delete<ApiResponse<void>>(`/transactions/${id}`);
    if (!data.success) {
      throw new Error(data.error || 'Failed to delete transaction');
    }
  },
};

// ============================================
// Categories API
// ============================================

export const categoriesApi = {
  getAll: async (): Promise<Category[]> => {
    const { data } = await apiClient.get<ApiResponse<Category[]>>('/categories');
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch categories');
    }
    return data.data;
  },

  create: async (categoryData: CreateCategoryRequest): Promise<Category> => {
    const { data } = await apiClient.post<ApiResponse<Category>>(
      '/categories',
      categoryData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to create category');
    }
    return data.data;
  },

  update: async (id: string, categoryData: UpdateCategoryRequest): Promise<Category> => {
    const { data } = await apiClient.put<ApiResponse<Category>>(
      `/categories/${id}`,
      categoryData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to update category');
    }
    return data.data;
  },

  delete: async (id: string): Promise<void> => {
    const { data } = await apiClient.delete<ApiResponse<void>>(`/categories/${id}`);
    if (!data.success) {
      throw new Error(data.error || 'Failed to delete category');
    }
  },
};

// ============================================
// Sub-categories API
// ============================================

export const subCategoriesApi = {
  getAll: async (categoryId?: string): Promise<SubCategory[]> => {
    const { data } = await apiClient.get<ApiResponse<SubCategory[]>>('/subcategories', {
      params: categoryId ? { categoryId } : undefined,
    });
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch sub-categories');
    }
    return data.data;
  },

  create: async (subCategoryData: CreateSubCategoryRequest): Promise<SubCategory> => {
    const { data } = await apiClient.post<ApiResponse<SubCategory>>(
      '/subcategories',
      subCategoryData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to create sub-category');
    }
    return data.data;
  },

  update: async (id: string, subCategoryData: UpdateSubCategoryRequest): Promise<SubCategory> => {
    const { data } = await apiClient.put<ApiResponse<SubCategory>>(
      `/subcategories/${id}`,
      subCategoryData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to update sub-category');
    }
    return data.data;
  },

  delete: async (id: string): Promise<void> => {
    const { data } = await apiClient.delete<ApiResponse<void>>(`/subcategories/${id}`);
    if (!data.success) {
      throw new Error(data.error || 'Failed to delete sub-category');
    }
  },
};

// Accounts, goals, investments and dashboard endpoints are intentionally not
// wired up yet: the backend does not implement them (Phase 2/3). Their
// request/response types remain in ../types for when they land.

// Export the configured axios instance for custom requests
export default apiClient;
