import { useState } from 'react';
import type { SubCategory } from '../../types';
import IconPicker from './IconPicker';
import ColorPicker from './ColorPicker';

export interface SubCategoryFormValues {
  name: string;
  icon: string | null;
  color: string | null;
  monthlyLimit: number | null;
}

interface SubCategoryFormProps {
  categoryName: string;
  /** When provided the form is in edit mode. */
  subCategory?: SubCategory;
  isSubmitting?: boolean;
  onSubmit: (values: SubCategoryFormValues) => void;
  onCancel: () => void;
}

function SubCategoryForm({
  categoryName,
  subCategory,
  isSubmitting,
  onSubmit,
  onCancel,
}: SubCategoryFormProps) {
  const [name, setName] = useState(subCategory?.name ?? '');
  const [icon, setIcon] = useState<string | null>(subCategory?.icon ?? null);
  const [color, setColor] = useState<string | null>(subCategory?.color ?? null);
  const [limit, setLimit] = useState(
    subCategory?.monthlyLimit != null ? String(subCategory.monthlyLimit) : '',
  );
  const [error, setError] = useState<string | null>(null);

  const isEdit = Boolean(subCategory);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (!trimmed) {
      setError('Name is required');
      return;
    }
    let monthlyLimit: number | null = null;
    if (limit.trim()) {
      const parsed = Number(limit);
      if (Number.isNaN(parsed) || parsed <= 0) {
        setError('Monthly limit must be greater than 0');
        return;
      }
      monthlyLimit = parsed;
    }
    onSubmit({ name: trimmed, icon, color, monthlyLimit });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      onClick={onCancel}
      role="dialog"
      aria-modal="true"
      aria-label={isEdit ? 'Edit sub-category' : 'New sub-category'}
    >
      <form
        onClick={(e) => e.stopPropagation()}
        onSubmit={handleSubmit}
        className="card w-full max-w-md space-y-4"
      >
        <div>
          <h2 className="text-xl font-semibold">
            {isEdit ? 'Edit sub-category' : 'New sub-category'}
          </h2>
          <p className="text-sm text-gray-500">in {categoryName}</p>
        </div>

        <div>
          <label htmlFor="sub-name" className="mb-1 block text-sm font-medium text-gray-700">
            Name
          </label>
          <input
            id="sub-name"
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
          <label htmlFor="sub-limit" className="mb-1 block text-sm font-medium text-gray-700">
            Monthly limit <span className="text-gray-400">(optional)</span>
          </label>
          <input
            id="sub-limit"
            className="input"
            type="number"
            min="0"
            step="0.01"
            value={limit}
            onChange={(e) => {
              setLimit(e.target.value);
              if (error) setError(null);
            }}
            placeholder="No limit"
          />
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

export default SubCategoryForm;
