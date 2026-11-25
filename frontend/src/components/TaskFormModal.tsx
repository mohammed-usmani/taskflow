import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { errorMessage, fieldErrors } from '../api/client'
import { queryKeys } from '../api/queryKeys'
import { tasksApi } from '../api/tasks'
import { usersApi } from '../api/users'
import { PRIORITIES, STATUSES, type Task, type TaskInput } from '../api/types'
import { Button } from './Button'
import { Field, inputClass } from './Field'
import { Modal } from './Modal'
import { priorityLabel, statusLabel, todayIso } from './labels'

// Mirrors CreateTaskRequest / UpdateTaskRequest. Form fields are strings ("" = not set).
const baseSchema = z.object({
  title: z.string().trim().min(1, 'Title is required').max(120, 'Title must be at most 120 characters'),
  description: z.string().max(2000, 'Description must be at most 2000 characters'),
  status: z.enum(STATUSES),
  priority: z.enum(PRIORITIES),
  dueDate: z.string(),
  assigneeId: z.string(),
})
// "Not in the past" applies on create only, like the backend: an overdue task must stay editable.
const createSchema = baseSchema.refine((f) => !f.dueDate || f.dueDate >= todayIso(), {
  path: ['dueDate'],
  message: 'Due date cannot be in the past',
})
type Form = z.infer<typeof baseSchema>

interface Props {
  projectId: number
  task?: Task // absent = create
  onClose: () => void
}

/** One form for create and edit (PUT sends every field, so the shapes are the same). */
export function TaskFormModal({ projectId, task, onClose }: Props) {
  const editing = task !== undefined
  const queryClient = useQueryClient()
  const users = useQuery({ queryKey: queryKeys.users, queryFn: usersApi.list })
  const [confirmDelete, setConfirmDelete] = useState(false)

  const { register, handleSubmit, setError, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(editing ? baseSchema : createSchema),
    defaultValues: {
      title: task?.title ?? '',
      description: task?.description ?? '',
      status: task?.status ?? 'TODO',
      priority: task?.priority ?? 'MEDIUM',
      dueDate: task?.dueDate ?? '',
      assigneeId: task?.assignee ? String(task.assignee.id) : '',
    },
  })

  const refreshBoard = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.tasks(projectId) })
    queryClient.invalidateQueries({ queryKey: queryKeys.summary(projectId) })
    onClose()
  }

  const save = useMutation({
    mutationFn: (f: Form) => {
      const input: TaskInput = {
        title: f.title,
        description: f.description.trim() || null,
        status: f.status,
        priority: f.priority,
        dueDate: f.dueDate || null,
        assigneeId: f.assigneeId ? Number(f.assigneeId) : null,
      }
      return editing ? tasksApi.update(task.id, input) : tasksApi.create(projectId, input)
    },
    onSuccess: refreshBoard,
    onError: (e) => Object.entries(fieldErrors(e)).forEach(([f, message]) => setError(f as keyof Form, { message })),
  })
  const remove = useMutation({ mutationFn: () => tasksApi.remove(task!.id), onSuccess: refreshBoard })

  const failure = (save.isError && !Object.keys(fieldErrors(save.error)).length && save.error) || (remove.isError && remove.error)

  return (
    <Modal title={editing ? 'Edit task' : 'New task'} onClose={onClose}>
      <form onSubmit={handleSubmit((f) => save.mutate(f))} noValidate className="space-y-4">
        <Field label="Title" id="task-title" error={errors.title?.message}>
          <input id="task-title" className={inputClass} aria-invalid={!!errors.title} {...register('title')} />
        </Field>
        <Field label="Description (optional)" id="task-description" error={errors.description?.message}>
          <textarea id="task-description" rows={3} className={inputClass} {...register('description')} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Status" id="task-status">
            <select id="task-status" className={inputClass} {...register('status')}>
              {STATUSES.map((s) => <option key={s} value={s}>{statusLabel[s]}</option>)}
            </select>
          </Field>
          <Field label="Priority" id="task-priority">
            <select id="task-priority" className={inputClass} {...register('priority')}>
              {PRIORITIES.map((p) => <option key={p} value={p}>{priorityLabel[p]}</option>)}
            </select>
          </Field>
          <Field label="Due date (optional)" id="task-due" error={errors.dueDate?.message}>
            <input id="task-due" type="date" min={editing ? undefined : todayIso()} className={inputClass}
                   aria-invalid={!!errors.dueDate} {...register('dueDate')} />
          </Field>
          <Field label="Assignee" id="task-assignee" error={errors.assigneeId?.message}>
            {/* Mount the real select only once the options exist. A select can't display a value
                that isn't among its options, so while users load (or if loading fails) it would
                show "Unassigned" for an assigned task. The form still holds the real value. */}
            {!users.isSuccess ? (
              <select id="task-assignee" className={inputClass} disabled>
                <option>{users.isError ? task?.assignee?.name ?? 'Unassigned' : 'Loading…'}</option>
              </select>
            ) : (
              <select id="task-assignee" className={inputClass} {...register('assigneeId')}>
                <option value="">Unassigned</option>
                {users.data?.map((u) => <option key={u.id} value={u.id}>{u.name}</option>)}
              </select>
            )}
            {users.isError && <p className="text-xs text-red-600">Couldn't load users: {errorMessage(users.error)}</p>}
          </Field>
        </div>

        {failure && <p role="alert" className="text-sm text-red-600">{errorMessage(failure)}</p>}

        <div className="flex flex-wrap items-center justify-between gap-2">
          {editing ? (
            // Two-step delete instead of window.confirm: same safety, no blocking browser dialog.
            confirmDelete ? (
              <span className="flex items-center gap-2 text-sm">
                Delete this task?
                <Button type="button" variant="danger" loading={remove.isPending} onClick={() => remove.mutate()}>Delete</Button>
                <Button type="button" variant="secondary" onClick={() => setConfirmDelete(false)}>Keep</Button>
              </span>
            ) : (
              <Button type="button" variant="secondary" onClick={() => setConfirmDelete(true)}>Delete task</Button>
            )
          ) : <span />}
          <div className="flex gap-2">
            <Button type="button" variant="secondary" onClick={onClose}>Cancel</Button>
            <Button type="submit" loading={save.isPending}>{editing ? 'Save changes' : 'Create task'}</Button>
          </div>
        </div>
      </form>
    </Modal>
  )
}
