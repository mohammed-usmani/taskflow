// Mirrors the backend DTOs (TaskDtos.kt, ProjectDtos.kt, AuthDtos.kt).

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH'

export const STATUSES: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE']
export const PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH']

export interface User {
  id: number
  name: string
  email: string
}

export interface AuthResponse {
  token: string
  user: User
}

export interface Project {
  id: number
  name: string
  description: string | null
  createdAt: string
}

export interface Task {
  id: number
  projectId: number
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null // "YYYY-MM-DD"
  overdue: boolean
  assignee: User | null
  createdAt: string
  updatedAt: string
}

export interface TaskInput {
  title: string
  description: string | null
  status: TaskStatus
  priority: TaskPriority
  dueDate: string | null
  assigneeId: number | null
}

export interface TaskSummary {
  counts: Record<TaskStatus, number>
  overdue: number
  total: number
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface TaskQuery {
  status?: TaskStatus
  priority?: TaskPriority
  assigneeId?: number
  sort?: 'dueDate' | 'priority' | 'createdAt'
  direction?: 'asc' | 'desc'
  page?: number
  size?: number
}

// The backend's ErrorResponse (common/Errors.kt)
export interface ApiError {
  status: number
  error: string
  message: string
  path: string
  fieldErrors: { field: string; message: string }[]
}
