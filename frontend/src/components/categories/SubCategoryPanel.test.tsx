import { describe, it, expect, beforeEach, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import SubCategoryPanel from './SubCategoryPanel';
import type { Category, SubCategory } from '../../types';

vi.mock('../../services/api', () => ({
  subCategoriesApi: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
  },
}));

import { subCategoriesApi } from '../../services/api';

const mockApi = vi.mocked(subCategoriesApi);

const category: Category = {
  id: 'cat-1',
  name: 'Needs',
  kind: 'EXPENSE',
  bucket: 'NEEDS',
  icon: null,
  color: null,
  isSystem: false,
  sortOrder: 0,
};

function sub(overrides: Partial<SubCategory>): SubCategory {
  return {
    id: crypto.randomUUID(),
    categoryId: category.id,
    name: 'Sub',
    icon: '🛒',
    color: '#2563eb',
    monthlyLimit: null,
    isSystem: false,
    ...overrides,
  };
}

function renderPanel() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <SubCategoryPanel category={category} />
    </QueryClientProvider>,
  );
}

beforeEach(() => vi.clearAllMocks());

describe('SubCategoryPanel', () => {
  it('shows empty-state guidance when the category has no sub-categories', async () => {
    mockApi.getAll.mockResolvedValue([]);
    renderPanel();
    expect(await screen.findByText('No sub-categories yet')).toBeInTheDocument();
    // Guidance references the parent category name
    expect(screen.getByText(/Break/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '+ Add sub-category' })).toBeInTheDocument();
  });

  it('lists existing sub-categories with their monthly limit', async () => {
    mockApi.getAll.mockResolvedValue([sub({ name: 'Groceries', monthlyLimit: 600 })]);
    renderPanel();
    expect(await screen.findByText('Groceries')).toBeInTheDocument();
    expect(screen.getByText('Limit 600')).toBeInTheDocument();
  });

  it('surfaces an error when the sub-categories fail to load', async () => {
    mockApi.getAll.mockRejectedValue(new Error('kaboom'));
    renderPanel();
    expect(await screen.findByText('kaboom')).toBeInTheDocument();
  });

  it('optimistically adds a sub-category with a monthly limit', async () => {
    mockApi.getAll.mockResolvedValue([]);
    let resolveCreate!: (s: SubCategory) => void;
    mockApi.create.mockReturnValue(new Promise<SubCategory>((res) => { resolveCreate = res; }));
    const user = userEvent.setup();
    renderPanel();

    await user.click(await screen.findByRole('button', { name: '+ Add sub-category' }));
    await user.type(screen.getByLabelText('Name'), 'Groceries');
    await user.type(screen.getByLabelText(/Monthly limit/), '600');
    await user.click(screen.getByRole('option', { name: '🛒' }));
    await user.click(screen.getByRole('button', { name: 'Create' }));

    expect(await screen.findByText('Groceries')).toBeInTheDocument(); // optimistic row
    expect(mockApi.create).toHaveBeenCalledWith(
      expect.objectContaining({ categoryId: 'cat-1', name: 'Groceries', monthlyLimit: 600 }),
    );

    resolveCreate(sub({ name: 'Groceries', monthlyLimit: 600 }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('validates the name and the monthly limit', async () => {
    mockApi.getAll.mockResolvedValue([]);
    const user = userEvent.setup();
    renderPanel();

    await user.click(await screen.findByRole('button', { name: '+ Add sub-category' }));
    // Empty name
    await user.click(screen.getByRole('button', { name: 'Create' }));
    expect(await screen.findByText('Name is required')).toBeInTheDocument();
    // Non-positive limit
    await user.type(screen.getByLabelText('Name'), 'X');
    await user.type(screen.getByLabelText(/Monthly limit/), '0');
    await user.click(screen.getByRole('button', { name: 'Create' }));
    expect(await screen.findByText('Monthly limit must be greater than 0')).toBeInTheDocument();
    expect(mockApi.create).not.toHaveBeenCalled();
  });

  it('rolls back an optimistic add when creation fails', async () => {
    mockApi.getAll.mockResolvedValue([]);
    mockApi.create.mockRejectedValue(new Error('nope'));
    const user = userEvent.setup();
    renderPanel();

    await user.click(await screen.findByRole('button', { name: '+ Add sub-category' }));
    await user.type(screen.getByLabelText('Name'), 'Doomed');
    await user.click(screen.getByRole('button', { name: 'Create' }));

    await waitFor(() => expect(screen.queryByText('Doomed')).not.toBeInTheDocument());
  });

  it('edits an existing sub-category', async () => {
    const existing = sub({ name: 'Groceries' });
    mockApi.getAll.mockResolvedValue([existing]);
    mockApi.update.mockResolvedValue({ ...existing, name: 'Food' });
    const user = userEvent.setup();
    renderPanel();

    await user.click(await screen.findByLabelText('Edit Groceries'));
    const nameInput = screen.getByLabelText('Name');
    await user.clear(nameInput);
    await user.type(nameInput, 'Food');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() =>
      expect(mockApi.update).toHaveBeenCalledWith(existing.id, expect.objectContaining({ name: 'Food' })),
    );
  });

  it('deletes a sub-category after confirmation', async () => {
    const existing = sub({ name: 'Groceries' });
    mockApi.getAll.mockResolvedValue([existing]);
    mockApi.delete.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderPanel();

    await user.click(await screen.findByLabelText('Delete Groceries'));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Delete' }));

    await waitFor(() => expect(mockApi.delete).toHaveBeenCalledWith(existing.id));
  });
});
