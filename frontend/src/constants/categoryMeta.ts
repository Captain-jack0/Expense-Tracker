import type { Bucket, CategoryKind } from '../types';

/** Display order + labels for the five budgeting buckets. */
export const BUCKETS: { value: Bucket; label: string; kind: CategoryKind; accent: string }[] = [
  { value: 'INCOME', label: 'Income', kind: 'INCOME', accent: '#27ae60' },
  { value: 'NEEDS', label: 'Needs', kind: 'EXPENSE', accent: '#2563eb' },
  { value: 'WANTS', label: 'Wants', kind: 'EXPENSE', accent: '#f39c12' },
  { value: 'SAVINGS', label: 'Savings', kind: 'EXPENSE', accent: '#8b5cf6' },
  { value: 'OTHER', label: 'Other', kind: 'EXPENSE', accent: '#64748b' },
];

export const BUCKET_ORDER: Bucket[] = BUCKETS.map((b) => b.value);

export const BUCKET_LABELS: Record<Bucket, string> = Object.fromEntries(
  BUCKETS.map((b) => [b.value, b.label]),
) as Record<Bucket, string>;

/** The default kind implied by a bucket (Income vs. the expense buckets). */
export const bucketKind = (bucket: Bucket): CategoryKind =>
  bucket === 'INCOME' ? 'INCOME' : 'EXPENSE';

/** Emoji presets for the icon picker — finance-oriented, kept small on purpose. */
export const CATEGORY_ICONS: string[] = [
  '💰', '💵', '💳', '🏦', '📈', '🛒', '🍔', '☕', '🏠', '🚗',
  '⛽', '💡', '📱', '🎬', '✈️', '🏥', '💊', '📚', '👕', '🎁',
  '🎮', '🐶', '💪', '✂️', '🔧', '🎓', '💼', '❤️',
];

/** Swatch presets for the color picker (hex). */
export const CATEGORY_COLORS: string[] = [
  '#2563eb', '#27ae60', '#f39c12', '#e74c3c', '#8b5cf6', '#ec4899',
  '#14b8a6', '#f59e0b', '#64748b', '#0ea5e9', '#84cc16', '#ef4444',
];
