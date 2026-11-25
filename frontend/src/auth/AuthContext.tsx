import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Outlet, useNavigate } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { authApi } from '../api/auth'
import { setUnauthorizedHandler, tokenStore } from '../api/client'
import type { User } from '../api/types'
import { AuthContext, type AuthState } from './useAuth'

/**
 * Who is logged in. Context (not React Query) because it's app-wide client state that
 * changes rarely; server data (projects, tasks) lives in React Query.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [checking, setChecking] = useState(() => tokenStore.get() !== null)

  const logout = useCallback(
    (reason?: 'expired') => {
      tokenStore.clear()
      setUser(null)
      queryClient.clear() // never show the previous user's cached data to the next one
      navigate('/login', { replace: true, state: reason ? { reason } : undefined })
    },
    [navigate, queryClient],
  )

  // Any 401 from the API (token expired or revoked) logs out cleanly.
  useEffect(() => {
    setUnauthorizedHandler(() => logout('expired'))
  }, [logout])

  // Page reload: we only have the token in localStorage. Ask the API who it belongs to;
  // an expired token gets a 401 here and the handler above logs out.
  useEffect(() => {
    if (!tokenStore.get()) return
    authApi
      .me()
      .then(setUser)
      .catch(() => {})
      .finally(() => setChecking(false))
  }, [])

  const value = useMemo<AuthState>(
    () => ({
      user,
      checking,
      logout,
      login: async (email, password) => {
        const res = await authApi.login(email, password)
        tokenStore.set(res.token)
        setUser(res.user)
      },
      register: async (name, email, password) => {
        const res = await authApi.register(name, email, password)
        tokenStore.set(res.token)
        setUser(res.user)
      },
    }),
    [user, checking, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

/** Top route element: AuthProvider sits inside the router (it navigates on logout) and inside React Query (it clears the cache). */
export function AuthRoot() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}
