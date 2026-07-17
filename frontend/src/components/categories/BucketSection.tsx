import type { Bucket, Category } from '../../types';
import { BUCKET_LABELS } from '../../constants/categoryMeta';
import CategoryCard from './CategoryCard';

interface BucketSectionProps {
  bucket: Bucket;
  accent: string;
  categories: Category[];
  onEdit: (category: Category) => void;
  onDelete: (category: Category) => void;
}

/** One budgeting bucket with the categories that belong to it. */
function BucketSection({ bucket, accent, categories, onEdit, onDelete }: BucketSectionProps) {
  return (
    <section className="card">
      <div className="mb-3 flex items-center gap-2">
        <span className="h-3 w-3 rounded-full" style={{ backgroundColor: accent }} aria-hidden="true" />
        <h2 className="text-lg font-semibold">{BUCKET_LABELS[bucket]}</h2>
        <span className="text-sm text-gray-400">({categories.length})</span>
      </div>

      {categories.length === 0 ? (
        <p className="py-4 text-center text-sm text-gray-400">No categories in this bucket yet</p>
      ) : (
        <div className="space-y-2">
          {categories.map((category) => (
            <CategoryCard
              key={category.id}
              category={category}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </section>
  );
}

export default BucketSection;
