import { useState } from 'react';
import type { Category } from '../../types';
import SubCategoryPanel from './SubCategoryPanel';

interface CategoryCardProps {
  category: Category;
  onEdit: (category: Category) => void;
  onDelete: (category: Category) => void;
}

/**
 * Single category row. Expands to reveal its sub-category manager.
 * System categories are read-only (no edit/delete of the category itself).
 */
function CategoryCard({ category, onEdit, onDelete }: CategoryCardProps) {
  const isOptimistic = category.id.startsWith('temp-');
  const [expanded, setExpanded] = useState(false);

  return (
    <div className={`overflow-hidden rounded-lg border border-gray-200 bg-white ${isOptimistic ? 'opacity-60' : ''}`}>
      <div className="flex items-center gap-3 px-3 py-2.5">
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          disabled={isOptimistic}
          className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600 disabled:opacity-30"
          aria-label={`${expanded ? 'Collapse' : 'Expand'} ${category.name}`}
          aria-expanded={expanded}
        >
          <svg
            className={`h-4 w-4 transition-transform ${expanded ? 'rotate-90' : ''}`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </button>

        <span
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-lg"
          style={{ backgroundColor: (category.color ?? '#e5e7eb') + '22' }}
          aria-hidden="true"
        >
          {category.icon ?? '🏷️'}
        </span>

        <div className="min-w-0 flex-1">
          <p className="truncate font-medium text-gray-900">{category.name}</p>
          <p className="text-xs text-gray-500">{category.kind === 'INCOME' ? 'Income' : 'Expense'}</p>
        </div>

        {category.isSystem ? (
          <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-500">System</span>
        ) : (
          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={() => onEdit(category)}
              disabled={isOptimistic}
              className="rounded p-1.5 text-gray-500 hover:bg-gray-100 hover:text-primary-600 disabled:opacity-40"
              aria-label={`Edit ${category.name}`}
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
            </button>
            <button
              type="button"
              onClick={() => onDelete(category)}
              disabled={isOptimistic}
              className="rounded p-1.5 text-gray-500 hover:bg-red-50 hover:text-danger disabled:opacity-40"
              aria-label={`Delete ${category.name}`}
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        )}
      </div>

      {expanded && !isOptimistic && <SubCategoryPanel category={category} />}
    </div>
  );
}

export default CategoryCard;
