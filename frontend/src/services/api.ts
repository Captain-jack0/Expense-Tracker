import axios from 'axios';
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios';
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  User,
  Account,
  CreateAccountRequest,
  UpdateAccountRequest,
  Transaction,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  TransactionFilters,
  Goal,
  CreateGoalRequest,
  UpdateGoalRequest,
  Category,
  CreateCategoryRequest,
  Investment,
  CreateInvestmentRequest,
  UpdateInvestmentRequest,
  DashboardData,
  PaginatedResponse,
} from '../types';

// ============================================
// Axios Instance Configuration
// ============================================

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

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

apiClient.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error) => {
    const originalRequest = error.config;

    // Handle 401 Unauthorized - Token expired
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          throw new Error('No refresh token available');
        }

        // Attempt to refresh the token
        const { data } = await axios.post<ApiResponse<AuthResponse>>(
          `${API_BASE_URL}/auth/refresh`,
          { refreshToken }
        );

        if (data.success && data.data) {
          localStorage.setItem('accessToken', data.data.accessToken);
          localStorage.setItem('refreshToken', data.data.refreshToken);

          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${data.data.accessToken}`;
          return apiClient(originalRequest);
        }
      } catch (refreshError) {
        // Refresh failed - logout user
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
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

  logout: async (): Promise<void> => {
    try {
      await apiClient.post('/auth/logout');
    } finally {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  },

  getCurrentUser: async (): Promise<User> => {
    const { data } = await apiClient.get<ApiResponse<User>>('/auth/me');
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch user');
    }
    return data.data;
  },

  refreshToken: async (refreshToken: string): Promise<AuthResponse> => {
    const { data } = await apiClient.post<ApiResponse<AuthResponse>>(
      '/auth/refresh',
      { refreshToken }
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Token refresh failed');
    }
    return data.data;
  },
};

// ============================================
// Accounts API
// ============================================

export const accountsApi = {
  getAll: async (): Promise<Account[]> => {
    const { data } = await apiClient.get<ApiResponse<Account[]>>('/accounts');
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch accounts');
    }
    return data.data;
  },

  getById: async (id: string): Promise<Account> => {
    const { data } = await apiClient.get<ApiResponse<Account>>(`/accounts/${id}`);
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch account');
    }
    return data.data;
  },

  create: async (accountData: CreateAccountRequest): Promise<Account> => {
    const { data } = await apiClient.post<ApiResponse<Account>>(
      '/accounts',
      accountData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to create account');
    }
    return data.data;
  },

  update: async (id: string, accountData: UpdateAccountRequest): Promise<Account> => {
    const { data } = await apiClient.put<ApiResponse<Account>>(
      `/accounts/${id}`,
      accountData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to update account');
    }
    return data.data;
  },

  delete: async (id: string): Promise<void> => {
    const { data } = await apiClient.delete<ApiResponse<void>>(`/accounts/${id}`);
    if (!data.success) {
      throw new Error(data.error || 'Failed to delete account');
    }
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
// Goals API
// ============================================

export const goalsApi = {
  getAll: async (): Promise<Goal[]> => {
    const { data } = await apiClient.get<ApiResponse<Goal[]>>('/goals');
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch goals');
    }
    return data.data;
  },

  getById: async (id: string): Promise<Goal> => {
    const { data } = await apiClient.get<ApiResponse<Goal>>(`/goals/${id}`);
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch goal');
    }
    return data.data;
  },

  create: async (goalData: CreateGoalRequest): Promise<Goal> => {
    const { data } = await apiClient.post<ApiResponse<Goal>>('/goals', goalData);
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to create goal');
    }
    return data.data;
  },

  update: async (id: string, goalData: UpdateGoalRequest): Promise<Goal> => {
    const { data } = await apiClient.put<ApiResponse<Goal>>(`/goals/${id}`, goalData);
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to update goal');
    }
    return data.data;
  },

  delete: async (id: string): Promise<void> => {
    const { data } = await apiClient.delete<ApiResponse<void>>(`/goals/${id}`);
    if (!data.success) {
      throw new Error(data.error || 'Failed to delete goal');
    }
  },

  addContribution: async (id: string, amount: number): Promise<Goal> => {
    const { data } = await apiClient.post<ApiResponse<Goal>>(
      `/goals/${id}/contribute`,
      { amount }
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to add contribution');
    }
    return data.data;
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

  getById: async (id: string): Promise<Category> => {
    const { data } = await apiClient.get<ApiResponse<Category>>(`/categories/${id}`);
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch category');
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

  delete: async (id: string): Promise<void> => {
    const { data } = await apiClient.delete<ApiResponse<void>>(`/categories/${id}`);
    if (!data.success) {
      throw new Error(data.error || 'Failed to delete category');
    }
  },
};

// ============================================
// Investments API
// ============================================

export const investmentsApi = {
  getAll: async (): Promise<Investment[]> => {
    const { data } = await apiClient.get<ApiResponse<Investment[]>>('/investments');
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch investments');
    }
    return data.data;
  },

  getById: async (id: string): Promise<Investment> => {
    const { data } = await apiClient.get<ApiResponse<Investment>>(
      `/investments/${id}`
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch investment');
    }
    return data.data;
  },

  create: async (investmentData: CreateInvestmentRequest): Promise<Investment> => {
    const { data } = await apiClient.post<ApiResponse<Investment>>(
      '/investments',
      investmentData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to create investment');
    }
    return data.data;
  },

  update: async (
    id: string,
    investmentData: UpdateInvestmentRequest
  ): Promise<Investment> => {
    const { data } = await apiClient.put<ApiResponse<Investment>>(
      `/investments/${id}`,
      investmentData
    );
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to update investment');
    }
    return data.data;
  },

  delete: async (id: string): Promise<void> => {
    const { data } = await apiClient.delete<ApiResponse<void>>(`/investments/${id}`);
    if (!data.success) {
      throw new Error(data.error || 'Failed to delete investment');
    }
  },
};

// ============================================
// Dashboard API
// ============================================

export const dashboardApi = {
  getData: async (): Promise<DashboardData> => {
    const { data } = await apiClient.get<ApiResponse<DashboardData>>('/dashboard');
    if (!data.success || !data.data) {
      throw new Error(data.error || 'Failed to fetch dashboard data');
    }
    return data.data;
  },
};

// Export the configured axios instance for custom requests
export default apiClient;
