import { useState } from 'react';
import type { Category, SubCategory } from '../../types';
import {
  useSubCategories,
  useCreateSubCategory,
  useUpdateSubCategory,
  useDeleteSubCategory,
} from '../../hooks/useSubCategories';
import SubCategoryForm, { type SubCategoryFormValues } from './SubCategoryForm';

interface SubCategoryPanelProps {
  category: Category;
}

type FormState = { mode: 'create' } | { mode: 'edit'; sub: SubCategory } | null;

/** Expanded content under a category: manage its sub-categories. */
function SubCategoryPanel({ category }: SubCategoryPanelProps) {
  const { data: subs, isLoading, isError, error } = useSubCategories(category.id);
  const createMutation = useCreateSubCategory(category.id);
  const updateMutation = useUpdateSubCategory(category.id);
  const deleteMutation = useDeleteSubCategory(category.id);

  const [form, setForm] = useState<FormState>(null);
  const [confirmDelete, setConfirmDelete] = useState<SubCategory | null>(null);

  const handleSubmit = (values: SubCategoryFormValues) => {
    const data = { categoryId: category.id, ...values };
    if (form?.mode === 'edit') {
      updateMutation.mutate({ id: form.sub.id, data }, { onSuccess: () => setForm(null) });
    } else {
      createMutation.mutate(data, { onSuccess: () => setForm(null) });
    }
  };

  const confirmDeletion = () => {
    if (!confirmDelete) return;
    deleteMutation.mutate(confirmDelete.id);
    setConfirmDelete(null);
  };

  const list = subs ?? [];

  return (
    <div className="border-t border-gray-200 bg-gray-50 px-3 py-3">
      {isLoading && <p className="py-2 text-center text-sm text-gray-500">Loading sub-categories…</p>}

      {isError && (
        <p className="py-2 text-center text-sm text-danger">
          {(error as Error)?.message ?? 'Failed to load sub-categories'}
        </p>
      )}

      {!isLoading && !isError && list.length === 0 && (
        <div className="rounded-lg border border-dashed border-gray-300 bg-white px-4 py-5 text-center">
          <p className="text-sm font-medium text-gray-700">No sub-categories yet</p>
          <p className="mx-auto mt-1 max-w-sm text-xs text-gray-500">
            Break <span className="font-medium">{category.name}</span> into sub-categories (e.g.
            Groceries, Rent) to track spending in detail and set monthly limits.
          </p>
          <button className="btn btn-primary mt-3" onClick={() => setForm({ mode: 'create' })}>
            + Add sub-category
          </button>
        </div>
      )}

      {!isLoading && !isError && list.length > 0 && (
        <div className="space-y-1.5">
          {list.map((sub) => {
            const isOptimistic = sub.id.startsWith('temp-');
            return (
              <div
                key={sub.id}
                className={`flex items-center gap-2 rounded-lg bg-white px-3 py-2 ${
                  isOptimistic ? 'opacity-60' : ''
                }`}
              >
                <span aria-hidden="true">{sub.icon ?? '•'}</span>
                <span className="min-w-0 flex-1 truncate text-sm text-gray-800">{sub.name}</span>
                {sub.monthlyLimit != null && (
                  <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-500">
                    Limit {sub.monthlyLimit}
                  </span>
                )}
                <button
                  type="button"
                  onClick={() => setForm({ mode: 'edit', sub })}
                  disabled={isOptimistic}
                  className="rounded p-1 text-gray-500 hover:bg-gray-100 hover:text-primary-600 disabled:opacity-40"
                  aria-label={`Edit ${sub.name}`}
                >
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </button>
                <button
                  type="button"
                  onClick={() => setConfirmDelete(sub)}
                  disabled={isOptimistic}
                  className="rounded p-1 text-gray-500 hover:bg-red-50 hover:text-danger disabled:opacity-40"
                  aria-label={`Delete ${sub.name}`}
                >
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            );
          })}
          <button
            className="mt-1 text-sm font-medium text-primary-600 hover:text-primary-700"
            onClick={() => setForm({ mode: 'create' })}
          >
            + Add sub-category
          </button>
        </div>
      )}

      {form && (
        <SubCategoryForm
          categoryName={category.name}
          subCategory={form.mode === 'edit' ? form.sub : undefined}
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
            <h2 className="text-lg font-semibold">Delete sub-category</h2>
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

export default SubCategoryPanel;
