import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { authApi } from '../api/auth'
import { projectsApi } from '../api/projects'
import { tasksApi } from '../api/tasks'
import { usersApi } from '../api/users'
import type { Task } from '../api/types'
import { renderApp } from '../test/renderApp'

jest.mock('../api/auth')
jest.mock('../api/projects')
jest.mock('../api/tasks')
jest.mock('../api/users')
const tasks = jest.mocked(tasksApi)

const sam = { id: 2, name: 'Sam', email: 'sam@example.com' }
const existing: Task = {
  id: 9, projectId: 1, title: 'Old title', description: 'notes', status: 'IN_PROGRESS', priority: 'LOW',
  dueDate: '2020-01-01', overdue: true, assignee: sam, createdAt: '2026-11-25T10:00:00Z', updatedAt: '2026-11-25T10:00:00Z',
}

beforeEach(() => {
  jest.resetAllMocks()
  localStorage.setItem('taskflow.token', 'jwt')
  jest.mocked(authApi).me.mockResolvedValue({ id: 1, name: 'Ali', email: 'ali@example.com' })
  jest.mocked(projectsApi).list.mockResolvedValue([{ id: 1, name: 'Website', description: null, createdAt: '2026-11-25T10:00:00Z' }])
  jest.mocked(usersApi).list.mockResolvedValue([{ id: 1, name: 'Ali', email: 'ali@example.com' }, sam])
  tasks.summary.mockResolvedValue({ counts: { TODO: 0, IN_PROGRESS: 1, DONE: 0 }, overdue: 1, total: 1 })
  tasks.list.mockResolvedValue({ items: [existing], page: 0, size: 100, totalItems: 1, totalPages: 1 })
})

test('create: validates title and past due dates, then sends a clean payload', async () => {
  tasks.create.mockResolvedValue(existing)
  renderApp('/projects/1')
  await userEvent.click(await screen.findByRole('button', { name: 'New task' }))
  const dialog = screen.getByRole('dialog', { name: 'New task' })

  // invalid first: no title, a date in the past
  await userEvent.type(within(dialog).getByLabelText('Due date (optional)'), '2020-01-01')
  await userEvent.click(within(dialog).getByRole('button', { name: 'Create task' }))
  expect(await within(dialog).findByText('Title is required')).toBeInTheDocument()
  expect(within(dialog).getByText('Due date cannot be in the past')).toBeInTheDocument()
  expect(tasks.create).not.toHaveBeenCalled()

  await userEvent.type(within(dialog).getByLabelText('Title'), 'Write tests')
  await userEvent.clear(within(dialog).getByLabelText('Due date (optional)'))
  await userEvent.selectOptions(within(dialog).getByLabelText('Priority'), 'HIGH')
  await userEvent.selectOptions(await within(dialog).findByLabelText('Assignee'), 'Sam')
  await userEvent.click(within(dialog).getByRole('button', { name: 'Create task' }))

  expect(tasks.create).toHaveBeenCalledWith(1, {
    title: 'Write tests', description: null, status: 'TODO', priority: 'HIGH', dueDate: null, assigneeId: 2,
  })
  expect(await screen.findByRole('button', { name: 'New task' })).toBeInTheDocument()
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
})

test('edit: pre-fills the task, allows a past due date, and can unassign', async () => {
  tasks.update.mockResolvedValue(existing)
  renderApp('/projects/1')
  await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
  const dialog = screen.getByRole('dialog', { name: 'Edit task' })

  expect(within(dialog).getByLabelText('Title')).toHaveValue('Old title')
  await within(dialog).findByRole('option', { name: 'Sam' })   // users loaded
  expect(within(dialog).getByLabelText('Assignee')).toHaveValue('2')

  await userEvent.selectOptions(within(dialog).getByLabelText('Assignee'), 'Unassigned')
  await userEvent.click(within(dialog).getByRole('button', { name: 'Save changes' }))

  expect(tasks.update).toHaveBeenCalledWith(9, {
    title: 'Old title', description: 'notes', status: 'IN_PROGRESS', priority: 'LOW', dueDate: '2020-01-01', assigneeId: null,
  })
})

test('delete asks for confirmation first', async () => {
  tasks.remove.mockResolvedValue({} as never)
  renderApp('/projects/1')
  await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
  const dialog = screen.getByRole('dialog', { name: 'Edit task' })

  await userEvent.click(within(dialog).getByRole('button', { name: 'Delete task' }))
  expect(tasks.remove).not.toHaveBeenCalled()
  await userEvent.click(within(dialog).getByRole('button', { name: 'Delete' }))
  expect(tasks.remove).toHaveBeenCalledWith(9)
})

test('if the user list fails to load, the current assignee is still shown and kept on save', async () => {
  jest.mocked(usersApi).list.mockRejectedValue(new Error('down'))
  tasks.update.mockResolvedValue(existing)
  renderApp('/projects/1')
  await userEvent.click(await screen.findByRole('button', { name: 'Edit' }))
  const dialog = screen.getByRole('dialog', { name: 'Edit task' })

  expect(await within(dialog).findByText(/Couldn't load users/)).toBeInTheDocument()
  expect(within(dialog).getByLabelText('Assignee')).toHaveDisplayValue('Sam')   // not "Unassigned"
  await userEvent.click(within(dialog).getByRole('button', { name: 'Save changes' }))
  expect(tasks.update).toHaveBeenCalledWith(9, expect.objectContaining({ assigneeId: 2 }))
})
