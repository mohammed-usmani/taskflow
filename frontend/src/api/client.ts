import axios, { AxiosError } from 'axios'
import type { ApiError } from './types'

/**
 * The one HTTP client. Components never call axios directly; they call the
 * functions in api/*.ts, which use this.
 */
export const http = axios.create({ baseURL: '/api' })

const TOKEN_KEY = 'taskflow.token'
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

// Every request carries the JWT, if we have one.
http.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// A 401 on any call except login/register means the token is missing or expired.
// AuthContext registers what to do (log out, go to /login); the client doesn't know about React.
let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && !error.config?.url?.startsWith('/auth/')) onUnauthorized()
    return Promise.reject(error)
  },
)

/** A message a user can read, from any error the API layer throws. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error)) {
    if (!error.response) return 'Cannot reach the server. Check your connection and try again.'
    return error.response.data?.message ?? `Request failed (${error.response.status})`
  }
  return 'Something went wrong.'
}

/** The backend's field-level validation errors as { field: message }. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!axios.isAxiosError<ApiError>(error)) return {}
  return Object.fromEntries((error.response?.data?.fieldErrors ?? []).map((e) => [e.field, e.message]))
}
