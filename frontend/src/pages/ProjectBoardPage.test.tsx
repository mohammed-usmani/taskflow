import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { authApi } from '../api/auth'
import { projectsApi } from '../api/projects'
import { tasksApi } from '../api/tasks'
import type { Task } from '../api/types'
import { renderApp } from '../test/renderApp'

jest.mock('../api/auth')
jest.mock('../api/projects')
jest.mock('../api/tasks')
jest.mock('../api/users')
const auth = jest.mocked(authApi)
const projects = jest.mocked(projectsApi)
const tasks = jest.mocked(tasksApi)

const task = (id: number, title: string, extra: Partial<Task> = {}): Task => ({
  id, projectId: 1, title, description: null, status: 'TODO', priority: 'MEDIUM', dueDate: null, overdue: false,
  assignee: null, createdAt: '2026-11-25T10:00:00Z', updatedAt: '2026-11-25T10:00:00Z', ...extra,
})
const page = (items: Task[]) => ({ items, page: 0, size: 100, totalItems: items.length, totalPages: 1 })

beforeEach(() => {
  jest.resetAllMocks()
  localStorage.setItem('taskflow.token', 'jwt')
  auth.me.mockResolvedValue({ id: 1, name: 'Ali', email: 'ali@example.com' })
  projects.list.mockResolvedValue([{ id: 1, name: 'Website', description: null, createdAt: '2026-11-25T10:00:00Z' }])
  tasks.summary.mockResolvedValue({ counts: { TODO: 1, IN_PROGRESS: 0, DONE: 0 }, overdue: 1, total: 1 })
})

test('puts tasks in their column and flags overdue ones in words, not only colour', async () => {
  tasks.list.mockResolvedValue(page([task(1, 'Late task', { dueDate: '2026-11-01', overdue: true })]))
  renderApp('/projects/1')

  const todo = await screen.findByRole('region', { name: 'To Do' })
  const card = within(todo).getByRole('article', { name: 'Late task' })
  expect(within(card).getByText(/Overdue/)).toBeInTheDocument()
  expect(within(screen.getByRole('region', { name: 'Done' })).getByText('No tasks')).toBeInTheDocument()
  expect(within(screen.getByLabelText('Task summary')).getByText('Overdue').nextSibling).toHaveTextContent('1')
})

test('moving a task with the status dropdown moves the card at once and saves it', async () => {
  tasks.list.mockResolvedValueOnce(page([task(1, 'Ship it')])).mockResolvedValue(page([task(1, 'Ship it', { status: 'DONE' })]))
  let finish!: (t: Task) => void
  tasks.updateStatus.mockReturnValue(new Promise((resolve) => (finish = resolve)))   // server answers later
  renderApp('/projects/1')

  await userEvent.selectOptions(await screen.findByLabelText('Status of Ship it'), 'DONE')

  // Moved before the server answered (optimistic)
  expect(within(screen.getByRole('region', { name: 'Done' })).getByRole('article', { name: 'Ship it' })).toBeInTheDocument()
  expect(tasks.updateStatus).toHaveBeenCalledWith(1, 'DONE')
  finish(task(1, 'Ship it', { status: 'DONE' }))
  expect(await within(screen.getByRole('region', { name: 'Done' })).findByRole('article', { name: 'Ship it' })).toBeInTheDocument()
})

test('if saving the status fails, the card goes back and the user is told', async () => {
  tasks.list.mockResolvedValue(page([task(1, 'Ship it')]))
  tasks.updateStatus.mockRejectedValue(new Error('down'))
  renderApp('/projects/1')

  await userEvent.selectOptions(await screen.findByLabelText('Status of Ship it'), 'DONE')

  expect(await screen.findByText(/Couldn't update the status/)).toBeInTheDocument()
  expect(within(screen.getByRole('region', { name: 'To Do' })).getByRole('article', { name: 'Ship it' })).toBeInTheDocument()
})

test('filters and sort go to the API and into the URL', async () => {
  const { usersApi } = jest.requireMock('../api/users') as { usersApi: { list: jest.Mock } }
  usersApi.list.mockResolvedValue([{ id: 2, name: 'Sam', email: 'sam@example.com' }])
  tasks.list.mockResolvedValue(page([task(1, 'Ship it')]))
  const { router } = renderApp('/projects/1')

  await userEvent.selectOptions(await screen.findByLabelText('Priority'), 'HIGH')
  await userEvent.selectOptions(screen.getByLabelText('Assignee'), await screen.findByRole('option', { name: 'Sam' }))
  await userEvent.selectOptions(screen.getByLabelText('Sort by'), 'Due date (soonest)')

  expect(tasks.list).toHaveBeenLastCalledWith(1, { priority: 'HIGH', assigneeId: 2, sort: 'dueDate', direction: 'asc', size: 100 })
  expect(router.state.location.search).toBe('?priority=HIGH&assignee=2&sort=dueSoon')

  await userEvent.click(screen.getByRole('button', { name: 'Clear filters' }))
  expect(router.state.location.search).toBe('?sort=dueSoon')   // sort stays, filters go
})

test('filters are read back from the URL on load', async () => {
  tasks.list.mockResolvedValue(page([]))
  renderApp('/projects/1?priority=LOW&sort=priority')

  expect(await screen.findByLabelText('Priority')).toHaveValue('LOW')
  expect(tasks.list).toHaveBeenCalledWith(1, expect.objectContaining({ priority: 'LOW', sort: 'priority', direction: 'desc' }))
})
