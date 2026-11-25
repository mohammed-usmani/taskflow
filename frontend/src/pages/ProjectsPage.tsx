import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link } from 'react-router'
import { projectsApi } from '../api/projects'
import { queryKeys } from '../api/queryKeys'
import { errorMessage, fieldErrors } from '../api/client'
import { Button } from '../components/Button'
import { Field, inputClass } from '../components/Field'
import { Modal } from '../components/Modal'
import { EmptyState, ErrorState, Loading } from '../components/States'

export function ProjectsPage() {
  const [creating, setCreating] = useState(false)
  const projects = useQuery({ queryKey: queryKeys.projects, queryFn: projectsApi.list })

  return (
    <section>
      <div className="mb-6 flex items-center justify-between gap-4">
        <h1 className="text-xl font-semibold text-slate-900">Your projects</h1>
        <Button onClick={() => setCreating(true)}>New project</Button>
      </div>

      {projects.isPending ? (
        <Loading label="Loading projects…" />
      ) : projects.isError ? (
        <ErrorState message={errorMessage(projects.error)} onRetry={() => projects.refetch()} />
      ) : projects.data.length === 0 ? (
        <EmptyState title="No projects yet" hint="Create your first project to start adding tasks." />
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {projects.data.map((p) => (
            <li key={p.id}>
              <Link
                to={`/projects/${p.id}`}
                className="block h-full rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition hover:border-indigo-300 hover:shadow"
              >
                <h2 className="font-medium text-slate-900">{p.name}</h2>
                {p.description && <p className="mt-1 line-clamp-2 text-sm text-slate-600">{p.description}</p>}
                <p className="mt-3 text-xs text-slate-400">Created {new Date(p.createdAt).toLocaleDateString()}</p>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {creating && <CreateProjectModal onClose={() => setCreating(false)} />}
    </section>
  )
}

const schema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(100, 'Name must be at most 100 characters'),
  description: z.string().max(2000, 'Description must be at most 2000 characters'),
})
type Form = z.infer<typeof schema>

function CreateProjectModal({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const { register, handleSubmit, setError, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', description: '' },
  })

  const create = useMutation({
    mutationFn: (f: Form) => projectsApi.create(f.name, f.description.trim() || null),
    onSuccess: () => {
      // The list is stale now; refetch it rather than patching the cache by hand.
      queryClient.invalidateQueries({ queryKey: queryKeys.projects })
      onClose()
    },
    onError: (e) => Object.entries(fieldErrors(e)).forEach(([f, message]) => setError(f as keyof Form, { message })),
  })

  return (
    <Modal title="New project" onClose={onClose}>
      <form onSubmit={handleSubmit((f) => create.mutate(f))} noValidate className="space-y-4">
        <Field label="Name" id="project-name" error={errors.name?.message}>
          <input id="project-name" className={inputClass} aria-invalid={!!errors.name} {...register('name')} />
        </Field>
        <Field label="Description (optional)" id="project-description" error={errors.description?.message}>
          <textarea id="project-description" rows={3} className={inputClass} {...register('description')} />
        </Field>
        {create.isError && !Object.keys(fieldErrors(create.error)).length && (
          <p role="alert" className="text-sm text-red-600">{errorMessage(create.error)}</p>
        )}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
          <Button type="submit" loading={create.isPending}>Create project</Button>
        </div>
      </form>
    </Modal>
  )
}
