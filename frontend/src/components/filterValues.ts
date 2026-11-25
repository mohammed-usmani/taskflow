import type { TaskQuery } from '../api/types'

// Sort choices shown to the user, mapped to the API's sort + direction.
export const SORTS = {
  created: { label: 'Newest first', sort: 'createdAt', direction: 'desc' },
  dueSoon: { label: 'Due date (soonest)', sort: 'dueDate', direction: 'asc' },
  dueLate: { label: 'Due date (latest)', sort: 'dueDate', direction: 'desc' },
  priority: { label: 'Priority (high first)', sort: 'priority', direction: 'desc' },
} as const satisfies Record<string, Pick<TaskQuery, 'sort' | 'direction'> & { label: string }>
export type SortKey = keyof typeof SORTS

export interface FilterValues {
  priority: string // '' = any
  assigneeId: string // '' = anyone
  sort: SortKey
}
