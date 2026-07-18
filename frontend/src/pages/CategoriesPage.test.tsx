import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import CategoriesPage from './CategoriesPage';
import type { Category } from '../types';

vi.mock('../services/api', () => ({
  categoriesApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
  subCategoriesApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}));

import { categoriesApi, subCategoriesApi } from '../services/api';

const mockApi = vi.mocked(categoriesApi);
const mockSubApi = vi.mocked(subCategoriesApi);

function cat(overrides: Partial<Category>): Category {
  return {
    id: crypto.randomUUID(),
    name: 'Category',
    kind: 'EXPENSE',
    bucket: 'NEEDS',
    icon: '🛒',
    color: '#2563eb',
    isSystem: false,
    sortOrder: 0,
    ...overrides,
  };
}

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <CategoriesPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('CategoriesPage', () => {
  it('groups categories by bucket and makes system categories read-only', async () => {
    mockApi.getAll.mockResolvedValue([
      cat({ name: 'Salary', kind: 'INCOME', bucket: 'INCOME', isSystem: true }),
      cat({ name: 'Groceries', bucket: 'NEEDS', isSystem: false }),
    ]);

    renderPage();

    expect(await screen.findByText('Groceries')).toBeInTheDocument();
    // Bucket headings render (use the heading role: "Income" also appears as a
    // card's kind label, so a plain text query would be ambiguous).
    expect(screen.getByRole('heading', { name: 'Income' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Needs' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Savings' })).toBeInTheDocument();

    // System category: shows the System badge, no edit/delete controls
    expect(screen.getByText('System')).toBeInTheDocument();
    expect(screen.queryByLabelText('Edit Salary')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Delete Salary')).not.toBeInTheDocument();

    // User category: editable + deletable
    expect(screen.getByLabelText('Edit Groceries')).toBeInTheDocument();
    expect(screen.getByLabelText('Delete Groceries')).toBeInTheDocument();
  });

  it('shows an error state when the list fails to load', async () => {
    mockApi.getAll.mockRejectedValue(new Error('boom'));
    renderPage();
    expect(await screen.findByText('boom')).toBeInTheDocument();
  });

  it('validates that a name is required', async () => {
    mockApi.getAll.mockResolvedValue([]);
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: '+ New category' }));
    await user.click(screen.getByRole('button', { name: 'Create' }));

    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    expect(mockApi.create).not.toHaveBeenCalled();
  });

  it('optimistically adds a new category before the server responds', async () => {
    mockApi.getAll.mockResolvedValue([]);
    // Keep create pending so we can observe the optimistic row.
    let resolveCreate!: (c: Category) => void;
    mockApi.create.mockReturnValue(new Promise<Category>((res) => { resolveCreate = res; }));

    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: '+ New category' }));
    await user.type(screen.getByLabelText('Name'), 'Coffee');
    await user.click(screen.getByRole('option', { name: '☕' }));      // icon picker
    await user.click(screen.getByLabelText('#f39c12'));                // color picker
    await user.selectOptions(screen.getByLabelText('Bucket'), 'WANTS');
    await user.click(screen.getByRole('button', { name: 'Create' }));

    // Optimistic card appears while the request is still pending
    expect(await screen.findByText('Coffee')).toBeInTheDocument();
    expect(mockApi.create).toHaveBeenCalledWith(
      expect.objectContaining({ name: 'Coffee', bucket: 'WANTS', kind: 'EXPENSE', icon: '☕' }),
    );

    resolveCreate(cat({ name: 'Coffee', bucket: 'WANTS' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('rolls the list back if creating fails', async () => {
    mockApi.getAll.mockResolvedValue([]);
    mockApi.create.mockRejectedValue(new Error('nope'));
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByRole('button', { name: '+ New category' }));
    await user.type(screen.getByLabelText('Name'), 'Doomed');
    await user.click(screen.getByRole('button', { name: 'Create' }));

    // After the failed mutation settles, the optimistic row is gone.
    await waitFor(() => expect(screen.queryByText('Doomed')).not.toBeInTheDocument());
  });

  it('edits an existing user category', async () => {
    const existing = cat({ name: 'Groceries', bucket: 'NEEDS' });
    mockApi.getAll.mockResolvedValue([existing]);
    mockApi.update.mockResolvedValue({ ...existing, name: 'Food' });
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByLabelText('Edit Groceries'));
    const nameInput = screen.getByLabelText('Name');
    await user.clear(nameInput);
    await user.type(nameInput, 'Food');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(mockApi.update).toHaveBeenCalledWith(existing.id, expect.objectContaining({ name: 'Food' })),
    );
  });

  it('deletes a category after confirmation', async () => {
    const existing = cat({ name: 'Groceries', bucket: 'NEEDS' });
    mockApi.getAll.mockResolvedValue([existing]);
    mockApi.delete.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByLabelText('Delete Groceries'));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(mockApi.delete).toHaveBeenCalledWith(existing.id));
  });

  it('expands a category to reveal its sub-category manager', async () => {
    const parent = cat({ name: 'Needs', bucket: 'NEEDS' });
    mockApi.getAll.mockResolvedValue([parent]);
    mockSubApi.getAll.mockResolvedValue([]); // no sub-categories yet

    const user = userEvent.setup();
    renderPage();

    await user.click(await screen.findByLabelText('Expand Needs'));

    // Empty-state guidance is shown for a category with no sub-categories
    expect(await screen.findByText('No sub-categories yet')).toBeInTheDocument();
    expect(mockSubApi.getAll).toHaveBeenCalledWith(parent.id);
  });
});
