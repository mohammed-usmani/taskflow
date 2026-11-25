import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './useAuth'
import { Loading } from '../components/States'

/** Wraps every page that needs a login. Logged-out users go to /login and come back afterwards. */
export function ProtectedRoute() {
  const { user, checking } = useAuth()
  const location = useLocation()
  if (checking) return <Loading label="Checking your session…" />
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}

/** The opposite: a logged-in user opening /login or /register goes straight to their projects. */
export function GuestRoute() {
  const { user, checking } = useAuth()
  if (checking) return <Loading label="Checking your session…" />
  return user ? <Navigate to="/projects" replace /> : <Outlet />
}
