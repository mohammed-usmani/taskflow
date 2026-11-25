import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, AxiosHeaders } from 'axios'
import { authApi } from '../api/auth'
import { renderApp } from '../test/renderApp'

// Replace the API calls only; everything else (routing, context, form) is real.
jest.mock('../api/auth')
const api = jest.mocked(authApi)

beforeEach(() => {
  localStorage.clear()
  jest.resetAllMocks()
})

test('a logged-out user opening a protected page lands on the login form', async () => {
  const { router } = renderApp('/projects')
  expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/login')
})

test('shows field errors and does not call the API when the form is invalid', async () => {
  renderApp('/login')
  await userEvent.type(await screen.findByLabelText('Email'), 'not-an-email')
  await userEvent.click(screen.getByRole('button', { name: 'Log in' }))

  expect(await screen.findByText('Enter a valid email')).toBeInTheDocument()
  expect(screen.getByText('Password is required')).toBeInTheDocument()
  expect(api.login).not.toHaveBeenCalled()
})

test('logs in, stores the token and shows the projects page', async () => {
  api.login.mockResolvedValue({ token: 'jwt-123', user: { id: 1, name: 'Ali', email: 'ali@example.com' } })
  const { router } = renderApp('/login')

  await userEvent.type(await screen.findByLabelText('Email'), 'ali@example.com')
  await userEvent.type(screen.getByLabelText('Password'), 'password123')
  await userEvent.click(screen.getByRole('button', { name: 'Log in' }))

  expect(await screen.findByRole('heading', { name: 'Your projects' })).toBeInTheDocument()
  expect(router.state.location.pathname).toBe('/projects')
  expect(localStorage.getItem('taskflow.token')).toBe('jwt-123')
  expect(api.login).toHaveBeenCalledWith('ali@example.com', 'password123')
})

test("shows the server's message when the password is wrong", async () => {
  const config = { headers: new AxiosHeaders() }
  api.login.mockRejectedValue(
    new AxiosError('401', 'ERR', config, null, {
      status: 401, statusText: '', headers: {}, config, data: { message: 'Invalid email or password' },
    }),
  )
  renderApp('/login')

  await userEvent.type(await screen.findByLabelText('Email'), 'ali@example.com')
  await userEvent.type(screen.getByLabelText('Password'), 'wrong-password')
  await userEvent.click(screen.getByRole('button', { name: 'Log in' }))

  expect(await screen.findByText('Invalid email or password')).toBeInTheDocument()
  expect(localStorage.getItem('taskflow.token')).toBeNull()
})
