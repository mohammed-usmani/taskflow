import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { authApi } from '../api/auth'
import { projectsApi } from '../api/projects'
import type { Project } from '../api/types'
import { renderApp } from '../test/renderApp'

jest.mock('../api/auth')
jest.mock('../api/projects')
const auth = jest.mocked(authApi)
const projects = jest.mocked(projectsApi)

const project = (id: number, name: string): Project => ({ id, name, description: null, createdAt: '2026-11-25T10:00:00Z' })

beforeEach(() => {
  jest.resetAllMocks()
  localStorage.setItem('taskflow.token', 'jwt') // logged in: AuthProvider verifies it with me()
  auth.me.mockResolvedValue({ id: 1, name: 'Ali', email: 'ali@example.com' })
})

test('shows an empty state, then the new project after creating one', async () => {
  projects.list.mockResolvedValueOnce([]).mockResolvedValueOnce([project(1, 'Website')])
  projects.create.mockResolvedValue(project(1, 'Website'))
  renderApp('/projects')

  expect(await screen.findByText('No projects yet')).toBeInTheDocument()

  await userEvent.click(screen.getByRole('button', { name: 'New project' }))
  const dialog = screen.getByRole('dialog', { name: 'New project' })
  await userEvent.type(within(dialog).getByLabelText('Name'), '  Website  ')
  await userEvent.click(within(dialog).getByRole('button', { name: 'Create project' }))

  expect(await screen.findByRole('link', { name: /Website/ })).toHaveAttribute('href', '/projects/1')
  expect(projects.create).toHaveBeenCalledWith('Website', null)   // trimmed; empty description sent as null
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
})

test('a blank name is rejected in the form, without a request', async () => {
  projects.list.mockResolvedValue([])
  renderApp('/projects')

  await userEvent.click(await screen.findByRole('button', { name: 'New project' }))
  await userEvent.type(screen.getByLabelText('Name'), '   ')
  await userEvent.click(screen.getByRole('button', { name: 'Create project' }))

  expect(await screen.findByText('Name is required')).toBeInTheDocument()
  expect(projects.create).not.toHaveBeenCalled()
})

test('shows an error with a retry button when loading fails', async () => {
  projects.list.mockRejectedValueOnce(new Error('boom')).mockResolvedValueOnce([project(2, 'Retry worked')])
  renderApp('/projects')

  await userEvent.click(await screen.findByRole('button', { name: 'Try again' }))
  expect(await screen.findByText('Retry worked')).toBeInTheDocument()
})
