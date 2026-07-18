import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { categoriesApi } from '../services/api';
import type { Category, CreateCategoryRequest, UpdateCategoryRequest } from '../types';
import { optimisticListUpdate } from './optimisticList';

export const CATEGORIES_KEY = ['categories'] as const;

/** Fetches the current user's categories. */
export function useCategories() {
  return useQuery({
    queryKey: CATEGORIES_KEY,
    queryFn: categoriesApi.getAll,
  });
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCategoryRequest) => categoriesApi.create(data),
    ...optimisticListUpdate<Category, CreateCategoryRequest>(queryClient, CATEGORIES_KEY, (list, data) => [
      ...list,
      {
        id: `temp-${crypto.randomUUID()}`,
        name: data.name,
        kind: data.kind,
        bucket: data.bucket,
        icon: data.icon ?? null,
        color: data.color ?? null,
        isSystem: false,
        sortOrder: list.length,
      },
    ]),
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCategoryRequest }) =>
      categoriesApi.update(id, data),
    ...optimisticListUpdate<Category, { id: string; data: UpdateCategoryRequest }>(
      queryClient,
      CATEGORIES_KEY,
      (list, { id, data }) =>
        list.map((c) =>
          c.id === id ? { ...c, ...data, icon: data.icon ?? null, color: data.color ?? null } : c,
        ),
    ),
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => categoriesApi.delete(id),
    ...optimisticListUpdate<Category, string>(queryClient, CATEGORIES_KEY, (list, id) =>
      list.filter((c) => c.id !== id),
    ),
  });
}
