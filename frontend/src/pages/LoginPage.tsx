import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { errorMessage } from '../api/client'
import { Button } from '../components/Button'
import { Field, inputClass } from '../components/Field'
import { AuthCard } from './AuthLayout'

const schema = z.object({
  email: z.email('Enter a valid email'),
  password: z.string().min(1, 'Password is required'),
})
type Form = z.infer<typeof schema>

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as { from?: string; reason?: string } | null
  const [serverError, setServerError] = useState<string | null>(null)
  const { register, handleSubmit, formState } = useForm<Form>({ resolver: zodResolver(schema) })
  const { errors, isSubmitting } = formState

  const onSubmit = async ({ email, password }: Form) => {
    setServerError(null)
    try {
      await login(email, password)
      navigate(state?.from ?? '/projects', { replace: true }) // back to where they were headed
    } catch (e) {
      setServerError(errorMessage(e))
    }
  }

  return (
    <AuthCard title="Log in">
      {state?.reason === 'expired' && (
        <p className="mb-4 rounded-md bg-amber-50 p-3 text-sm text-amber-800">Your session expired. Please log in again.</p>
      )}
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <Field label="Email" id="email" error={errors.email?.message}>
          <input id="email" type="email" autoComplete="email" className={inputClass}
                 aria-invalid={!!errors.email} {...register('email')} />
        </Field>
        <Field label="Password" id="password" error={errors.password?.message}>
          <input id="password" type="password" autoComplete="current-password" className={inputClass}
                 aria-invalid={!!errors.password} {...register('password')} />
        </Field>
        {serverError && <p role="alert" className="text-sm text-red-600">{serverError}</p>}
        <Button type="submit" loading={isSubmitting} className="w-full">Log in</Button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-600">
        No account? <Link to="/register" className="font-medium text-indigo-600 hover:underline">Register</Link>
      </p>
    </AuthCard>
  )
}
