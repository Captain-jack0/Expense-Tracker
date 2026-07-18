import type { QueryClient, QueryKey, UseMutationOptions } from '@tanstack/react-query';

/**
 * Optimistic-update wiring for a mutation that changes a cached list:
 * snapshot the list, apply `patch` immediately, roll back on error, and
 * re-sync from the server once the request settles.
 *
 * Shared by the category and sub-category mutation hooks.
 */
export function optimisticListUpdate<TItem, TVars>(
  queryClient: QueryClient,
  key: QueryKey,
  patch: (list: TItem[], vars: TVars) => TItem[],
): Pick<
  UseMutationOptions<unknown, Error, TVars, { previous?: TItem[] }>,
  'onMutate' | 'onError' | 'onSettled'
> {
  return {
    onMutate: async (vars) => {
      await queryClient.cancelQueries({ queryKey: key });
      const previous = queryClient.getQueryData<TItem[]>(key);
      queryClient.setQueryData<TItem[]>(key, (old) => patch(old ?? [], vars));
      return { previous };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.previous) queryClient.setQueryData(key, ctx.previous);
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: key });
    },
  };
}
