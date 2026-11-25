import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { errorMessage, fieldErrors } from '../api/client'
import { Button } from '../components/Button'
import { Field, inputClass } from '../components/Field'
import { AuthCard } from './AuthLayout'

// Same rules as the backend's RegisterRequest, so most mistakes are caught before a request.
const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be at most 100 characters'),
  email: z.email('Enter a valid email').max(255),
  password: z.string().min(8, 'Password must be 8-72 characters').max(72, 'Password must be 8-72 characters'),
})
type Form = z.infer<typeof schema>

export function RegisterPage() {
  const { register: signUp } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState<string | null>(null)
  const { register, handleSubmit, setError, formState } = useForm<Form>({ resolver: zodResolver(schema) })
  const { errors, isSubmitting } = formState

  const onSubmit = async ({ name, email, password }: Form) => {
    setServerError(null)
    try {
      await signUp(name, email, password)
      navigate('/projects', { replace: true })
    } catch (e) {
      // The backend still has the final say (e.g. "Email is already registered"): show its
      // field errors under the right input, anything else above the button.
      const fields = fieldErrors(e)
      Object.entries(fields).forEach(([field, message]) => setError(field as keyof Form, { message }))
      if (!Object.keys(fields).length) setServerError(errorMessage(e))
    }
  }

  return (
    <AuthCard title="Create an account">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <Field label="Name" id="name" error={errors.name?.message}>
          <input id="name" autoComplete="name" className={inputClass} aria-invalid={!!errors.name} {...register('name')} />
        </Field>
        <Field label="Email" id="email" error={errors.email?.message}>
          <input id="email" type="email" autoComplete="email" className={inputClass}
                 aria-invalid={!!errors.email} {...register('email')} />
        </Field>
        <Field label="Password" id="password" error={errors.password?.message}>
          <input id="password" type="password" autoComplete="new-password" className={inputClass}
                 aria-invalid={!!errors.password} {...register('password')} />
        </Field>
        {serverError && <p role="alert" className="text-sm text-red-600">{serverError}</p>}
        <Button type="submit" loading={isSubmitting} className="w-full">Create account</Button>
      </form>
      <p className="mt-4 text-center text-sm text-slate-600">
        Already registered? <Link to="/login" className="font-medium text-indigo-600 hover:underline">Log in</Link>
      </p>
    </AuthCard>
  )
}
