import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { subCategoriesApi } from '../services/api';
import type { CreateSubCategoryRequest, SubCategory, UpdateSubCategoryRequest } from '../types';
import { optimisticListUpdate } from './optimisticList';

export const subCategoriesKey = (categoryId: string) => ['subcategories', categoryId] as const;

/** Fetches the sub-categories of a single category (lazy — only when mounted). */
export function useSubCategories(categoryId: string) {
  return useQuery({
    queryKey: subCategoriesKey(categoryId),
    queryFn: () => subCategoriesApi.getAll(categoryId),
  });
}

export function useCreateSubCategory(categoryId: string) {
  const queryClient = useQueryClient();
  const key = subCategoriesKey(categoryId);
  return useMutation({
    mutationFn: (data: CreateSubCategoryRequest) => subCategoriesApi.create(data),
    ...optimisticListUpdate<SubCategory, CreateSubCategoryRequest>(queryClient, key, (list, data) => [
      ...list,
      {
        id: `temp-${crypto.randomUUID()}`,
        categoryId: data.categoryId,
        name: data.name,
        icon: data.icon ?? null,
        color: data.color ?? null,
        monthlyLimit: data.monthlyLimit ?? null,
        isSystem: false,
      },
    ]),
  });
}

export function useUpdateSubCategory(categoryId: string) {
  const queryClient = useQueryClient();
  const key = subCategoriesKey(categoryId);
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateSubCategoryRequest }) =>
      subCategoriesApi.update(id, data),
    ...optimisticListUpdate<SubCategory, { id: string; data: UpdateSubCategoryRequest }>(
      queryClient,
      key,
      (list, { id, data }) =>
        list.map((s) =>
          s.id === id
            ? { ...s, ...data, icon: data.icon ?? null, color: data.color ?? null, monthlyLimit: data.monthlyLimit ?? null }
            : s,
        ),
    ),
  });
}

export function useDeleteSubCategory(categoryId: string) {
  const queryClient = useQueryClient();
  const key = subCategoriesKey(categoryId);
  return useMutation({
    mutationFn: (id: string) => subCategoriesApi.delete(id),
    ...optimisticListUpdate<SubCategory, string>(queryClient, key, (list, id) =>
      list.filter((s) => s.id !== id),
    ),
  });
}
