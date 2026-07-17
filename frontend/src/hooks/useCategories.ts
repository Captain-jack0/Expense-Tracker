import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationOptions,
} from '@tanstack/react-query';
import { categoriesApi } from '../services/api';
import type { Category, CreateCategoryRequest, UpdateCategoryRequest } from '../types';

export const CATEGORIES_KEY = ['categories'] as const;

/** Fetches the current user's categories. */
export function useCategories() {
  return useQuery({
    queryKey: CATEGORIES_KEY,
    queryFn: categoriesApi.getAll,
  });
}

type Ctx = { previous?: Category[] };

/**
 * Shared optimistic-update wiring: snapshot the list, apply `patch`
 * immediately, roll back on error, and re-sync from the server on settle.
 */
function optimistic<TVars>(
  queryClient: ReturnType<typeof useQueryClient>,
  patch: (list: Category[], vars: TVars) => Category[],
): Pick<UseMutationOptions<unknown, Error, TVars, Ctx>, 'onMutate' | 'onError' | 'onSettled'> {
  return {
    onMutate: async (vars) => {
      await queryClient.cancelQueries({ queryKey: CATEGORIES_KEY });
      const previous = queryClient.getQueryData<Category[]>(CATEGORIES_KEY);
      queryClient.setQueryData<Category[]>(CATEGORIES_KEY, (old) => patch(old ?? [], vars));
      return { previous };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.previous) queryClient.setQueryData(CATEGORIES_KEY, ctx.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: CATEGORIES_KEY });
    },
  };
}

export function useCreateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateCategoryRequest) => categoriesApi.create(data),
    ...optimistic<CreateCategoryRequest>(queryClient, (list, data) => {
      const optimisticCategory: Category = {
        id: `temp-${crypto.randomUUID()}`,
        name: data.name,
        kind: data.kind,
        bucket: data.bucket,
        icon: data.icon ?? null,
        color: data.color ?? null,
        isSystem: false,
        sortOrder: list.length,
      };
      return [...list, optimisticCategory];
    }),
  });
}

export function useUpdateCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCategoryRequest }) =>
      categoriesApi.update(id, data),
    ...optimistic<{ id: string; data: UpdateCategoryRequest }>(queryClient, (list, { id, data }) =>
      list.map((c) => (c.id === id ? { ...c, ...data, icon: data.icon ?? null, color: data.color ?? null } : c)),
    ),
  });
}

export function useDeleteCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => categoriesApi.delete(id),
    ...optimistic<string>(queryClient, (list, id) => list.filter((c) => c.id !== id)),
  });
}
