import { AxiosError, AxiosHeaders } from 'axios'
import { errorMessage, fieldErrors } from './client'

function apiError(status: number, data: unknown) {
  const config = { headers: new AxiosHeaders() }
  return new AxiosError('fail', 'ERR', config, null, { status, statusText: '', headers: {}, config, data })
}

test('reads the backend message and field errors', () => {
  const err = apiError(400, { message: 'Validation failed', fieldErrors: [{ field: 'title', message: 'Title is required' }] })
  expect(errorMessage(err)).toBe('Validation failed')
  expect(fieldErrors(err)).toEqual({ title: 'Title is required' })
})

test('explains a network failure instead of showing a raw error', () => {
  expect(errorMessage(new AxiosError('Network Error'))).toMatch(/cannot reach the server/i)
})
