import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { projectsApi } from '../api/projects'
import { queryKeys } from '../api/queryKeys'
import { tasksApi } from '../api/tasks'
import type { Page, Task, TaskQuery, TaskStatus } from '../api/types'

// ponytail: the board loads one page of up to 100 tasks (the API's max page size).
// Past that, add per-column paging or infinite scroll.
export const BOARD_PAGE_SIZE = 100

/** Everything the board page reads from and writes to the server. */
export function useBoard(projectId: number, query: TaskQuery) {
  const queryClient = useQueryClient()
  const fullQuery = { ...query, size: BOARD_PAGE_SIZE }

  // No GET /projects/{id} in the API: reuse the (usually cached) list for the name.
  const project = useQuery({
    queryKey: queryKeys.projects,
    queryFn: projectsApi.list,
    select: (list) => list.find((p) => p.id === projectId) ?? null,
  })
  const tasks = useQuery({
    queryKey: queryKeys.tasks(projectId, fullQuery),
    queryFn: () => tasksApi.list(projectId, fullQuery),
    // Changing a filter keeps the current cards on screen until the new list arrives,
    // instead of flashing a spinner.
    placeholderData: keepPreviousData,
  })
  const summary = useQuery({ queryKey: queryKeys.summary(projectId), queryFn: () => tasksApi.summary(projectId) })

  /**
   * OPTIMISTIC update: move the card at once, then tell the server. If the server
   * refuses, put the old data back. Either way, refetch so we end up with the truth
   * (the server also recomputes `overdue` and the summary counts).
   */
  const changeStatus = useMutation({
    mutationFn: ({ id, status }: { id: number; status: TaskStatus }) => tasksApi.updateStatus(id, status),
    onMutate: async ({ id, status }) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.tasks(projectId) }) // a late refetch mustn't overwrite us
      const previous = queryClient.getQueriesData<Page<Task>>({ queryKey: queryKeys.tasks(projectId) })
      queryClient.setQueriesData<Page<Task>>({ queryKey: queryKeys.tasks(projectId) }, (page) =>
        page && { ...page, items: page.items.map((t) => (t.id === id ? { ...t, status, overdue: t.overdue && status !== 'DONE' } : t)) },
      )
      return { previous }
    },
    onError: (_err, _vars, context) => context?.previous.forEach(([key, data]) => queryClient.setQueryData(key, data)),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.tasks(projectId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.summary(projectId) })
    },
  })

  return { project, tasks, summary, changeStatus }
}
