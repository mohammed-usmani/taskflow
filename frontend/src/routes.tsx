import { isAxiosError } from 'axios'
import type { QueryClientConfig } from '@tanstack/react-query'
import { Navigate, type RouteObject } from 'react-router'
import { AuthRoot } from './auth/AuthContext'
import { GuestRoute, ProtectedRoute } from './auth/ProtectedRoute'
import { AppLayout } from './pages/AppLayout'
import { LoginPage } from './pages/LoginPage'
import { ProjectBoardPage } from './pages/ProjectBoardPage'
import { ProjectsPage } from './pages/ProjectsPage'
import { RegisterPage } from './pages/RegisterPage'

export const queryClientOptions = {
  defaultOptions: {
    // Don't retry 4xx errors (a 404 won't fix itself); retry other failures once.
    queries: { retry: (count, error) => !(isAxiosError(error) && (error.response?.status ?? 500) < 500) && count < 1 },
  },
} satisfies QueryClientConfig

// Exported (not just used by App) so tests render the real route tree in a memory router.
export const routes: RouteObject[] = [
  {
    element: <AuthRoot />,
    children: [
      {
        element: <GuestRoute />,
        children: [
          { path: '/login', element: <LoginPage /> },
          { path: '/register', element: <RegisterPage /> },
        ],
      },
      {
        element: <ProtectedRoute />,
        children: [
          {
            element: <AppLayout />,
            children: [
              { path: '/projects', element: <ProjectsPage /> },
              { path: '/projects/:projectId', element: <ProjectBoardPage /> },
            ],
          },
        ],
      },
      { path: '*', element: <Navigate to="/projects" replace /> },
    ],
  },
]
