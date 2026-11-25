import { useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { errorMessage } from '../api/client'
import type { Task, TaskPriority, TaskQuery } from '../api/types'
import { Button } from '../components/Button'
import { SummaryBar } from '../components/SummaryBar'
import { TaskFilters } from '../components/TaskFilters'
import { SORTS, type FilterValues, type SortKey } from '../components/filterValues'
import { TaskFormModal } from '../components/TaskFormModal'
import { ErrorState, Loading } from '../components/States'
import { BoardColumns } from './BoardColumns'
import { BOARD_PAGE_SIZE, useBoard } from './useBoard'

export function ProjectBoardPage() {
  const projectId = Number(useParams().projectId)
  // Filters live in the URL (?priority=HIGH&sort=dueSoon): they survive a reload and can be shared.
  const [params, setParams] = useSearchParams()
  const filters: FilterValues = {
    priority: params.get('priority') ?? '',
    assigneeId: params.get('assignee') ?? '',
    sort: (params.get('sort') as SortKey) in SORTS ? (params.get('sort') as SortKey) : 'created',
  }
  const setFilters = (f: FilterValues) =>
    setParams(
      Object.fromEntries(
        Object.entries({ priority: f.priority, assignee: f.assigneeId, sort: f.sort === 'created' ? '' : f.sort }).filter(([, v]) => v),
      ),
      { replace: true },
    )
  const query: TaskQuery = {
    priority: (filters.priority || undefined) as TaskPriority | undefined,
    assigneeId: filters.assigneeId ? Number(filters.assigneeId) : undefined,
    sort: SORTS[filters.sort].sort,
    direction: SORTS[filters.sort].direction,
  }
  const filtering = query.priority !== undefined || query.assigneeId !== undefined
  const { project, tasks, summary, changeStatus } = useBoard(projectId, query)
  // null = closed, 'new' = create, a Task = edit that task
  const [form, setForm] = useState<Task | 'new' | null>(null)

  if (project.isPending) return <Loading label="Loading project…" />
  if (project.isError) return <ErrorState message={errorMessage(project.error)} onRetry={() => project.refetch()} />
  if (!project.data) return <ErrorState message="Project not found." />

  return (
    <section className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <Link to="/projects" className="text-sm text-indigo-600 hover:underline">← All projects</Link>
          <h1 className="mt-1 text-xl font-semibold text-slate-900">{project.data.name}</h1>
          {project.data.description && <p className="text-sm text-slate-600">{project.data.description}</p>}
        </div>
        <Button onClick={() => setForm('new')}>New task</Button>
      </div>

      {summary.isSuccess && <SummaryBar summary={summary.data} />}

      <TaskFilters value={filters} onChange={setFilters} />

      {changeStatus.isError && (
        <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700">
          Couldn't update the status: {errorMessage(changeStatus.error)}. The task was moved back.
        </p>
      )}

      {tasks.isPending ? (
        <Loading label="Loading tasks…" />
      ) : tasks.isError ? (
        <ErrorState message={errorMessage(tasks.error)} onRetry={() => tasks.refetch()} />
      ) : (
        <>
          {filtering && (
            <p className="text-sm text-slate-600" aria-live="polite">
              {tasks.data.totalItems} matching {tasks.data.totalItems === 1 ? 'task' : 'tasks'} (the summary counts the whole project)
            </p>
          )}
          {tasks.data.totalItems > BOARD_PAGE_SIZE && (
            <p className="text-sm text-amber-700">Showing the first {BOARD_PAGE_SIZE} of {tasks.data.totalItems} tasks.</p>
          )}
          <BoardColumns
            tasks={tasks.data.items}
            onStatusChange={(id, status) => changeStatus.mutate({ id, status })}
            onEdit={setForm}
          />
        </>
      )}

      {form && (
        <TaskFormModal projectId={projectId} task={form === 'new' ? undefined : form} onClose={() => setForm(null)} />
      )}
    </section>
  )
}

