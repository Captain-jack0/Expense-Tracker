// ============================================
// Core Entity Types (Based on Database Schema)
// ============================================

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  currency: string;
  timezone: string;
  isActive: boolean;
  emailVerified: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Account {
  id: string;
  userId: string;
  name: string;
  type: AccountType;
  currency: string;
  initialBalance: number;
  currentBalance: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  userId: string | null;
  name: string;
  type: TransactionType;
  icon: string | null;
  color: string | null;
  parentId: string | null;
  isSystem: boolean;
  createdAt: string;
}

export interface Transaction {
  id: string;
  userId: string;
  accountId: string;
  categoryId: string | null;
  type: TransactionType;
  amount: number;
  currency: string;
  description: string | null;
  transactionDate: string;
  createdAt: string;
  updatedAt: string;
  // Populated fields
  account?: Account;
  category?: Category;
}

export interface Goal {
  id: string;
  userId: string;
  name: string;
  targetAmount: number;
  currentAmount: number;
  targetDate: string | null;
  icon: string | null;
  color: string | null;
  status: GoalStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Investment {
  id: string;
  userId: string;
  accountId: string | null;
  symbol: string;
  name: string | null;
  type: InvestmentType;
  quantity: number;
  averageCost: number;
  currentPrice: number | null;
  lastPriceUpdate: string | null;
  createdAt: string;
  updatedAt: string;
  // Computed fields
  totalValue?: number;
  profitLoss?: number;
  profitLossPercentage?: number;
}

// ============================================
// Enums
// ============================================

export type AccountType =
  | 'checking'
  | 'savings'
  | 'credit'
  | 'investment'
  | 'cash';

export type TransactionType =
  | 'income'
  | 'expense'
  | 'transfer';

export type GoalStatus =
  | 'active'
  | 'completed'
  | 'cancelled';

export type InvestmentType =
  | 'stock'
  | 'etf'
  | 'mutual_fund'
  | 'crypto'
  | 'bond';

// ============================================
// API Request/Response Types
// ============================================

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

export interface PaginatedResponse<T> {
  success: boolean;
  data: T[];
  pagination: {
    total: number;
    page: number;
    limit: number;
    totalPages: number;
  };
}

// Auth
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
}

// Transactions
export interface CreateTransactionRequest {
  accountId: string;
  categoryId?: string;
  type: TransactionType;
  amount: number;
  currency?: string;
  description?: string;
  transactionDate: string;
}

export interface UpdateTransactionRequest extends Partial<CreateTransactionRequest> {
  id: string;
}

export interface TransactionFilters {
  accountId?: string;
  categoryId?: string;
  type?: TransactionType;
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
}

// Accounts
export interface CreateAccountRequest {
  name: string;
  type: AccountType;
  currency?: string;
  initialBalance?: number;
}

export interface UpdateAccountRequest extends Partial<CreateAccountRequest> {
  id: string;
}

// Goals
export interface CreateGoalRequest {
  name: string;
  targetAmount: number;
  targetDate?: string;
  icon?: string;
  color?: string;
}

export interface UpdateGoalRequest extends Partial<CreateGoalRequest> {
  id: string;
}

// Categories
export interface CreateCategoryRequest {
  name: string;
  type: TransactionType;
  icon?: string;
  color?: string;
  parentId?: string;
}

// Investments
export interface CreateInvestmentRequest {
  accountId?: string;
  symbol: string;
  name?: string;
  type: InvestmentType;
  quantity: number;
  averageCost: number;
}

export interface UpdateInvestmentRequest extends Partial<CreateInvestmentRequest> {
  id: string;
}

// ============================================
// Dashboard & Analytics Types
// ============================================

export interface DashboardData {
  totalBalance: number;
  totalIncome: number;
  totalExpenses: number;
  netSavings: number;
  accounts: Account[];
  recentTransactions: Transaction[];
  categoryBreakdown: CategoryBreakdown[];
  monthlyTrend: MonthlyTrend[];
}

export interface CategoryBreakdown {
  categoryId: string;
  categoryName: string;
  amount: number;
  percentage: number;
  color: string;
}

export interface MonthlyTrend {
  month: string;
  income: number;
  expenses: number;
  net: number;
}

// ============================================
// UI State Types
// ============================================

export interface LoadingState {
  isLoading: boolean;
  error: string | null;
}

export interface FormState<T> {
  data: T;
  errors: Partial<Record<keyof T, string>>;
  isSubmitting: boolean;
}
