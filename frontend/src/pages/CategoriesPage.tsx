import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import type { Bucket, Category, CreateCategoryRequest } from '../types';
import { BUCKETS } from '../constants/categoryMeta';
import {
  useCategories,
  useCreateCategory,
  useUpdateCategory,
  useDeleteCategory,
} from '../hooks/useCategories';
import BucketSection from '../components/categories/BucketSection';
import CategoryForm from '../components/categories/CategoryForm';

type FormState = { mode: 'create' } | { mode: 'edit'; category: Category } | null;

function CategoriesPage() {
  const { data: categories, isLoading, isError, error } = useCategories();
  const createMutation = useCreateCategory();
  const updateMutation = useUpdateCategory();
  const deleteMutation = useDeleteCategory();

  const [form, setForm] = useState<FormState>(null);
  const [confirmDelete, setConfirmDelete] = useState<Category | null>(null);

  const grouped = useMemo(() => {
    const map: Record<Bucket, Category[]> = { INCOME: [], NEEDS: [], WANTS: [], SAVINGS: [], OTHER: [] };
    for (const c of categories ?? []) map[c.bucket]?.push(c);
    return map;
  }, [categories]);

  const handleSubmit = (data: CreateCategoryRequest) => {
    if (form?.mode === 'edit') {
      updateMutation.mutate({ id: form.category.id, data }, { onSuccess: () => setForm(null) });
    } else {
      createMutation.mutate(data, { onSuccess: () => setForm(null) });
    }
  };

  const confirmDeletion = () => {
    if (!confirmDelete) return;
    deleteMutation.mutate(confirmDelete.id);
    setConfirmDelete(null);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <div>
            <div className="flex items-center gap-2 text-sm text-gray-500">
              <Link to="/dashboard" className="hover:text-primary-600">Dashboard</Link>
              <span>/</span>
              <span>Categories</span>
            </div>
            <h1 className="text-2xl font-bold text-gray-900">Categories</h1>
          </div>
          <button className="btn btn-primary" onClick={() => setForm({ mode: 'create' })}>
            + New category
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-5xl space-y-6 px-4 py-8 sm:px-6 lg:px-8">
        {isLoading && <p className="py-12 text-center text-gray-500">Loading categories…</p>}

        {isError && (
          <div className="card text-center">
            <p className="text-danger">{(error as Error)?.message ?? 'Failed to load categories'}</p>
          </div>
        )}

        {!isLoading && !isError &&
          BUCKETS.map((b) => (
            <BucketSection
              key={b.value}
              bucket={b.value}
              accent={b.accent}
              categories={grouped[b.value]}
              onEdit={(category) => setForm({ mode: 'edit', category })}
              onDelete={(category) => setConfirmDelete(category)}
            />
          ))}
      </main>

      {form && (
        <CategoryForm
          category={form.mode === 'edit' ? form.category : undefined}
          isSubmitting={createMutation.isPending || updateMutation.isPending}
          onSubmit={handleSubmit}
          onCancel={() => setForm(null)}
        />
      )}

      {confirmDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          onClick={() => setConfirmDelete(null)}
          role="dialog"
          aria-modal="true"
        >
          <div className="card w-full max-w-sm space-y-4" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-lg font-semibold">Delete category</h2>
            <p className="text-sm text-gray-600">
              Delete <span className="font-medium">{confirmDelete.name}</span>? This can't be undone.
            </p>
            <div className="flex justify-end gap-2">
              <button className="btn btn-secondary" onClick={() => setConfirmDelete(null)}>
                Cancel
              </button>
              <button className="btn btn-danger" onClick={confirmDeletion}>
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default CategoriesPage;
