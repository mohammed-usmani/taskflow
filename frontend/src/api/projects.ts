import { http } from './client'
import type { Project } from './types'

export const projectsApi = {
  list: () => http.get<Project[]>('/projects').then((r) => r.data),
  create: (name: string, description: string | null) =>
    http.post<Project>('/projects', { name, description }).then((r) => r.data),
}
