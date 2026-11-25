import { Link, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { Button } from '../components/Button'

/** Header shown on every logged-in page. */
export function AppLayout() {
  const { user, logout } = useAuth()
  return (
    <div className="min-h-screen bg-slate-50">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <Link to="/projects" className="font-semibold text-indigo-600">TaskFlow</Link>
          <div className="flex items-center gap-3 text-sm">
            <span className="hidden text-slate-600 sm:inline">{user?.name}</span>
            <Button variant="secondary" onClick={() => logout()}>Log out</Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
