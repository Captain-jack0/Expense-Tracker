import { useState } from 'react';
import type { Bucket, Category, CreateCategoryRequest } from '../../types';
import { BUCKETS, bucketKind } from '../../constants/categoryMeta';
import IconPicker from './IconPicker';
import ColorPicker from './ColorPicker';

interface CategoryFormProps {
  /** When provided the form is in edit mode. */
  category?: Category;
  isSubmitting?: boolean;
  onSubmit: (data: CreateCategoryRequest) => void;
  onCancel: () => void;
}

function CategoryForm({ category, isSubmitting, onSubmit, onCancel }: CategoryFormProps) {
  const [name, setName] = useState(category?.name ?? '');
  const [bucket, setBucket] = useState<Bucket>(category?.bucket ?? 'NEEDS');
  const [icon, setIcon] = useState<string | null>(category?.icon ?? null);
  const [color, setColor] = useState<string | null>(category?.color ?? null);
  const [error, setError] = useState<string | null>(null);

  const isEdit = Boolean(category);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Name is required');
      return;
    }
    onSubmit({ name: trimmed, kind: bucketKind(bucket), bucket, icon, color });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={onCancel}
      role="dialog"
      aria-modal="true"
      aria-label={isEdit ? 'Edit category' : 'New category'}
    >
      <form
        onClick={(e) => e.stopPropagation()}
        onSubmit={handleSubmit}
        className="card w-full max-w-md space-y-4"
      >
        <h2 className="text-xl font-semibold">{isEdit ? 'Edit category' : 'New category'}</h2>

        <div>
          <label htmlFor="cat-name" className="mb-1 block text-sm font-medium text-gray-700">
            Name
          </label>
          <input
            id="cat-name"
            className="input"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              if (error) setError(null);
            }}
            placeholder="e.g. Groceries"
            autoFocus
            maxLength={100}
          />
        </div>

        <div>
          <label htmlFor="cat-bucket" className="mb-1 block text-sm font-medium text-gray-700">
            Bucket
          </label>
          <select
            id="cat-bucket"
            className="input"
            value={bucket}
            onChange={(e) => setBucket(e.target.value as Bucket)}
          >
            {BUCKETS.map((b) => (
              <option key={b.value} value={b.value}>
                {b.label} ({b.kind === 'INCOME' ? 'Income' : 'Expense'})
              </option>
            ))}
          </select>
        </div>

        <div>
          <span className="mb-1 block text-sm font-medium text-gray-700">Icon</span>
          <IconPicker value={icon} onChange={setIcon} />
        </div>

        <div>
          <span className="mb-1 block text-sm font-medium text-gray-700">Color</span>
          <ColorPicker value={color} onChange={setColor} />
        </div>

        {error && <p className="text-sm text-danger">{error}</p>}

        <div className="flex justify-end gap-2 pt-2">
          <button type="button" className="btn btn-secondary" onClick={onCancel}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={isSubmitting}>
            {isSubmitting ? 'Saving…' : isEdit ? 'Save changes' : 'Create'}
          </button>
        </div>
      </form>
    </div>
  );
}

export default CategoryForm;
