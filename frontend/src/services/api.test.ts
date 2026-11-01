import axios, { AxiosError, AxiosHeaders } from 'axios';
import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import apiClient, {
  ApiError,
  authApi,
  categoriesApi,
  subCategoriesApi,
  toApiError,
  transactionsApi,
} from './api';

// ------------------------------------------------------------------
// A tiny fake backend: every request (apiClient AND the bare axios used
// for /auth/refresh) goes through `handler`, which returns [status, body].
// ------------------------------------------------------------------

type Handler = (config: InternalAxiosRequestConfig) => [number, unknown];
let handler: Handler;
const calls: InternalAxiosRequestConfig[] = [];

const fakeAdapter: AxiosAdapter = async (config) => {
  calls.push(config);
  const [status, data] = handler(config);
  const response: AxiosResponse = {
    data,
    status,
    statusText: String(status),
    headers: {},
    config,
  };
  if (status >= 400) {
    throw new AxiosError(`Request failed with status code ${status}`, 'ERR_BAD_RESPONSE', config, null, response);
  }
  return response;
};

const ok = <T>(data: T): [number, unknown] => [200, { success: true, data }];
const fail = (status: number, error: string): [number, unknown] => [status, { success: false, error }];
const path = (c: InternalAxiosRequestConfig) => (c.url ?? '').replace(/^https?:\/\/[^/]+\/api/, '');

const authResponse = {
  user: { id: 'u1', email: 'a@b.c', firstName: 'A', lastName: 'B' },
  accessToken: 'new-access',
  refreshToken: 'new-refresh',
};

let originalLocation: Location;

beforeEach(() => {
  calls.length = 0;
  apiClient.defaults.adapter = fakeAdapter;
  axios.defaults.adapter = fakeAdapter;
  localStorage.clear();
  originalLocation = window.location;
  Object.defineProperty(window, 'location', {
    configurable: true,
    value: { pathname: '/dashboard', href: 'http://localhost/dashboard' },
  });
});

afterEach(() => {
  Object.defineProperty(window, 'location', { configurable: true, value: originalLocation });
});

describe('error messages', () => {
  it('a wrong password surfaces the backend message and does NOT refresh or redirect', async () => {
    handler = () => fail(401, 'Invalid email or password');

    await expect(authApi.login({ email: 'a@b.c', password: 'nope' })).rejects.toThrow(
      'Invalid email or password',
    );
    expect(calls.map(path)).toEqual(['/auth/login']); // no /auth/refresh attempt
    expect(window.location.href).toBe('http://localhost/dashboard'); // no redirect
  });

  it('a 409 on register surfaces "Email is already registered"', async () => {
    handler = () => fail(409, 'Email is already registered');
    const err = await authApi
      .register({ email: 'a@b.c', password: 'password1', firstName: 'A', lastName: 'B' })
      .catch((e) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect(err.message).toBe('Email is already registered');
    expect(err.status).toBe(409);
  });

  it('toApiError falls back sensibly without a backend message', () => {
    const config = { headers: new AxiosHeaders() } as InternalAxiosRequestConfig;
    const noBody = new AxiosError('x', 'ERR', config, null, {
      data: '', status: 502, statusText: '', headers: {}, config,
    });
    expect(toApiError(noBody).message).toBe('Request failed (HTTP 502)');
    expect(toApiError(new AxiosError('Network Error', 'ERR_NETWORK', config)).message).toMatch(/Network error/);
    const plain = new Error('boom');
    expect(toApiError(plain)).toBe(plain);
    expect(toApiError('weird').message).toBe('Unexpected error');
  });

  it('a successful HTTP response with success:false still throws the backend error', async () => {
    handler = () => [200, { success: false, error: 'Nope' }];
    await expect(authApi.getCurrentUser()).rejects.toThrow('Nope');
  });
});

describe('token refresh', () => {
  it('refreshes an expired access token once and replays the request', async () => {
    localStorage.setItem('accessToken', 'old-access');
    localStorage.setItem('refreshToken', 'old-refresh');
    handler = (c) => {
      if (path(c) === '/auth/refresh') return ok(authResponse);
      const auth = String(c.headers?.Authorization ?? '');
      return auth === 'Bearer new-access' ? ok(authResponse.user) : fail(401, 'Invalid or expired token');
    };

    const user = await authApi.getCurrentUser();

    expect(user.email).toBe('a@b.c');
    expect(localStorage.getItem('accessToken')).toBe('new-access');
    expect(localStorage.getItem('refreshToken')).toBe('new-refresh');
    expect(calls.map(path)).toEqual(['/auth/me', '/auth/refresh', '/auth/me']);
  });

  it('concurrent 401s share a single refresh call', async () => {
    localStorage.setItem('accessToken', 'old-access');
    localStorage.setItem('refreshToken', 'old-refresh');
    handler = (c) => {
      if (path(c) === '/auth/refresh') return ok(authResponse);
      return String(c.headers?.Authorization) === 'Bearer new-access'
        ? ok([])
        : fail(401, 'Invalid or expired token');
    };

    await Promise.all([categoriesApi.getAll(), transactionsApi.getAll(), subCategoriesApi.getAll()]);

    expect(calls.filter((c) => path(c) === '/auth/refresh')).toHaveLength(1);
  });

  it('when refresh fails it clears the session and redirects to /login', async () => {
    localStorage.setItem('accessToken', 'old-access');
    localStorage.setItem('refreshToken', 'revoked');
    localStorage.setItem('user', '{}');
    handler = (c) =>
      path(c) === '/auth/refresh'
        ? fail(401, 'Refresh token has been revoked')
        : fail(401, 'Invalid or expired token');

    await expect(categoriesApi.getAll()).rejects.toThrow('Refresh token has been revoked');
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
    expect(localStorage.getItem('user')).toBeNull();
    expect(window.location.href).toBe('/login');
  });

  it('without a refresh token it reports an expired session', async () => {
    handler = () => fail(401, 'Invalid or expired token');
    await expect(categoriesApi.getAll()).rejects.toThrow(/session has expired/);
  });

  it('a refresh response with success:false ends the session', async () => {
    localStorage.setItem('refreshToken', 'r');
    handler = (c) => (path(c) === '/auth/refresh' ? [200, { success: false }] : fail(401, 'expired'));
    await expect(categoriesApi.getAll()).rejects.toThrow(/session has expired/);
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  it('does not redirect again when already on /login', async () => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      value: { pathname: '/login', href: 'http://localhost/login' },
    });
    handler = () => fail(401, 'expired');
    await expect(categoriesApi.getAll()).rejects.toThrow();
    expect(window.location.href).toBe('http://localhost/login');
  });
});

describe('auth API', () => {
  it('attaches the stored access token', async () => {
    localStorage.setItem('accessToken', 'tok');
    handler = () => ok(authResponse.user);
    await authApi.getCurrentUser();
    expect(String(calls[0].headers?.Authorization)).toBe('Bearer tok');
  });

  it('login and register return the auth payload', async () => {
    handler = () => ok(authResponse);
    await expect(authApi.login({ email: 'a@b.c', password: 'p' })).resolves.toEqual(authResponse);
    await expect(
      authApi.register({ email: 'a@b.c', password: 'password1', firstName: 'A', lastName: 'B' }),
    ).resolves.toEqual(authResponse);
  });

  it('login/register throw on a success:false body', async () => {
    handler = () => [200, { success: false }];
    await expect(authApi.login({ email: 'a', password: 'b' })).rejects.toThrow('Login failed');
    await expect(
      authApi.register({ email: 'a', password: 'b', firstName: 'c', lastName: 'd' }),
    ).rejects.toThrow('Registration failed');
  });

  it('logout calls the server and always clears the local session', async () => {
    localStorage.setItem('accessToken', 'a');
    localStorage.setItem('refreshToken', 'r');
    localStorage.setItem('user', '{}');
    handler = () => fail(500, 'down');
    await expect(authApi.logout()).rejects.toThrow('down');
    expect(calls.map(path)).toEqual(['/auth/logout']);
    expect(localStorage.length).toBe(0);
  });
});

describe('resource APIs', () => {
  const tx = { id: 't1', name: 'Rent' };

  it('transactions CRUD + pagination hit the right endpoints', async () => {
    handler = (c) => {
      if (path(c).startsWith('/transactions/paginated')) {
        return [200, { success: true, data: [tx], pagination: { total: 1, page: 1, limit: 20, totalPages: 1 } }];
      }
      return ok(c.method === 'delete' ? null : tx);
    };

    await transactionsApi.getAll({ type: 'EXPENSE' });
    const page = await transactionsApi.getPaginated(2, 10, { categoryId: 'c1' });
    await transactionsApi.getById('t1');
    await transactionsApi.create({} as never);
    await transactionsApi.update('t1', {} as never);
    await transactionsApi.delete('t1');

    expect(page.pagination.total).toBe(1);
    expect(calls[1].params).toEqual({ page: 2, limit: 10, categoryId: 'c1' });
    expect(calls.map((c) => `${c.method} ${path(c)}`)).toEqual([
      'get /transactions',
      'get /transactions/paginated',
      'get /transactions/t1',
      'post /transactions',
      'put /transactions/t1',
      'delete /transactions/t1',
    ]);
  });

  it('categories and sub-categories CRUD hit the right endpoints', async () => {
    handler = (c) => ok(c.method === 'delete' ? null : { id: 'x' });

    await categoriesApi.getAll();
    await categoriesApi.create({} as never);
    await categoriesApi.update('c1', {} as never);
    await categoriesApi.delete('c1');
    await subCategoriesApi.getAll('c1');
    await subCategoriesApi.create({} as never);
    await subCategoriesApi.update('s1', {} as never);
    await subCategoriesApi.delete('s1');

    expect(calls[4].params).toEqual({ categoryId: 'c1' });
    expect(calls.map((c) => `${c.method} ${path(c)}`)).toEqual([
      'get /categories',
      'post /categories',
      'put /categories/c1',
      'delete /categories/c1',
      'get /subcategories',
      'post /subcategories',
      'put /subcategories/s1',
      'delete /subcategories/s1',
    ]);
  });

  it('every call throws its fallback message on a success:false body', async () => {
    handler = () => [200, { success: false }];
    const attempts: Array<[() => Promise<unknown>, string]> = [
      [() => transactionsApi.getAll(), 'Failed to fetch transactions'],
      [() => transactionsApi.getPaginated(), 'Failed to fetch transactions'],
      [() => transactionsApi.getById('t'), 'Failed to fetch transaction'],
      [() => transactionsApi.create({} as never), 'Failed to create transaction'],
      [() => transactionsApi.update('t', {} as never), 'Failed to update transaction'],
      [() => transactionsApi.delete('t'), 'Failed to delete transaction'],
      [() => categoriesApi.getAll(), 'Failed to fetch categories'],
      [() => categoriesApi.create({} as never), 'Failed to create category'],
      [() => categoriesApi.update('c', {} as never), 'Failed to update category'],
      [() => categoriesApi.delete('c'), 'Failed to delete category'],
      [() => subCategoriesApi.getAll(), 'Failed to fetch sub-categories'],
      [() => subCategoriesApi.create({} as never), 'Failed to create sub-category'],
      [() => subCategoriesApi.update('s', {} as never), 'Failed to update sub-category'],
      [() => subCategoriesApi.delete('s'), 'Failed to delete sub-category'],
      [() => authApi.getCurrentUser(), 'Failed to fetch user'],
    ];
    for (const [call, message] of attempts) {
      await expect(call()).rejects.toThrow(message);
    }
  });

  it('request interceptor passes through request errors', async () => {
    type Handlers = { handlers: Array<{ rejected: (e: unknown) => Promise<unknown> }> };
    const onRejected = (apiClient.interceptors.request as unknown as Handlers).handlers[0].rejected;
    const boom = new Error('boom');
    await expect(onRejected(boom)).rejects.toBe(boom);
  });
});
