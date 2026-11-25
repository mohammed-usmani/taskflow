import type { TaskQuery } from './types'

/**
 * React Query cache keys in one place. A mutation invalidates by prefix:
 * invalidating ['tasks', 5] refetches every filtered/sorted list of project 5.
 */
export const queryKeys = {
  projects: ['projects'] as const,
  users: ['users'] as const,
  tasks: (projectId: number, query?: TaskQuery) =>
    query ? (['tasks', projectId, query] as const) : (['tasks', projectId] as const),
  summary: (projectId: number) => ['summary', projectId] as const,
}
