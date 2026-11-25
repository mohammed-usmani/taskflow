import type { ReactNode } from 'react'
import type { Task, TaskStatus } from '../api/types'
import { STATUSES } from '../api/types'
import { formatDate, priorityLabel, statusLabel } from './labels'

const priorityStyle = {
  LOW: 'bg-slate-100 text-slate-700',
  MEDIUM: 'bg-amber-100 text-amber-800',
  HIGH: 'bg-red-100 text-red-800',
} as const

interface Props {
  task: Task
  onStatusChange: (status: TaskStatus) => void
  onEdit?: () => void
  dragHandle?: ReactNode // the board passes one; the drag overlay copy has none
}

/**
 * One task on the board. Overdue is shown with a red border AND the word "Overdue",
 * so it doesn't rely on colour alone.
 */
export function TaskCard({ task, onStatusChange, onEdit, dragHandle }: Props) {
  return (
    <article
      aria-label={task.title}
      className={`rounded-md border bg-white p-3 shadow-sm ${task.overdue ? 'border-red-400 ring-1 ring-red-200' : 'border-slate-200'}`}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex min-w-0 items-start gap-1">
          {dragHandle}
          <h3 className="text-sm font-medium break-words text-slate-900">{task.title}</h3>
        </div>
        <span className={`shrink-0 rounded px-2 py-0.5 text-xs font-medium ${priorityStyle[task.priority]}`}>
          {priorityLabel[task.priority]}
        </span>
      </div>
      {task.description && <p className="mt-1 line-clamp-2 text-xs text-slate-600">{task.description}</p>}

      <dl className="mt-3 space-y-1 text-xs text-slate-600">
        <div className="flex gap-1">
          <dt className="sr-only">Due</dt>
          <dd className={task.overdue ? 'font-medium text-red-700' : ''}>
            {task.dueDate ? `Due ${formatDate(task.dueDate)}` : 'No due date'}
            {task.overdue && ' · Overdue'}
          </dd>
        </div>
        <div className="flex gap-1">
          <dt className="sr-only">Assignee</dt>
          <dd>{task.assignee ? task.assignee.name : 'Unassigned'}</dd>
        </div>
      </dl>

      <div className="mt-3 flex items-center justify-between gap-2">
        <select
          aria-label={`Status of ${task.title}`}
          value={task.status}
          onChange={(e) => onStatusChange(e.target.value as TaskStatus)}
          className="rounded border border-slate-300 bg-white px-2 py-1 text-xs"
        >
          {STATUSES.map((s) => (
            <option key={s} value={s}>{statusLabel[s]}</option>
          ))}
        </select>
        {onEdit && (
          <button onClick={onEdit} className="text-xs font-medium text-indigo-600 hover:underline">
            Edit
          </button>
        )}
      </div>
    </article>
  )
}
