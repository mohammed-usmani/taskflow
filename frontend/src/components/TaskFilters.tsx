import { useQuery } from '@tanstack/react-query'
import { queryKeys } from '../api/queryKeys'
import { usersApi } from '../api/users'
import { PRIORITIES } from '../api/types'
import { SORTS, type FilterValues, type SortKey } from './filterValues'
import { priorityLabel } from './labels'

const selectClass = 'rounded-md border border-slate-300 bg-white px-2 py-1.5 text-sm'

export function TaskFilters({ value, onChange }: { value: FilterValues; onChange: (v: FilterValues) => void }) {
  const users = useQuery({ queryKey: queryKeys.users, queryFn: usersApi.list })
  const set = (patch: Partial<FilterValues>) => onChange({ ...value, ...patch })
  const active = value.priority !== '' || value.assigneeId !== ''

  return (
    <div role="search" aria-label="Filter tasks" className="flex flex-wrap items-end gap-3">
      <label className="flex flex-col gap-1 text-xs text-slate-600">
        Priority
        <select className={selectClass} value={value.priority} onChange={(e) => set({ priority: e.target.value })}>
          <option value="">Any</option>
          {PRIORITIES.map((p) => <option key={p} value={p}>{priorityLabel[p]}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1 text-xs text-slate-600">
        Assignee
        <select className={selectClass} value={value.assigneeId} onChange={(e) => set({ assigneeId: e.target.value })}>
          <option value="">Anyone</option>
          {users.data?.map((u) => <option key={u.id} value={u.id}>{u.name}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1 text-xs text-slate-600">
        Sort by
        <select className={selectClass} value={value.sort} onChange={(e) => set({ sort: e.target.value as SortKey })}>
          {Object.entries(SORTS).map(([key, s]) => <option key={key} value={key}>{s.label}</option>)}
        </select>
      </label>
      {active && (
        <button onClick={() => set({ priority: '', assigneeId: '' })} className="pb-2 text-sm text-indigo-600 hover:underline">
          Clear filters
        </button>
      )}
    </div>
  )
}
