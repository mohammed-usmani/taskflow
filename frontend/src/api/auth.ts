import { http } from './client'
import type { AuthResponse, User } from './types'

export const authApi = {
  login: (email: string, password: string) =>
    http.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data),
  register: (name: string, email: string, password: string) =>
    http.post<AuthResponse>('/auth/register', { name, email, password }).then((r) => r.data),
  me: () => http.get<User>('/users/me').then((r) => r.data),
}
