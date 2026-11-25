import type { TaskSummary } from '../api/types'
import { STATUSES } from '../api/types'
import { statusLabel } from './labels'

export function SummaryBar({ summary }: { summary: TaskSummary }) {
  return (
    <dl aria-label="Task summary" className="grid grid-cols-2 gap-2 sm:grid-cols-5">
      {STATUSES.map((s) => (
        <Stat key={s} label={statusLabel[s]} value={summary.counts[s]} />
      ))}
      <Stat label="Overdue" value={summary.overdue} alert={summary.overdue > 0} />
      <Stat label="Total" value={summary.total} />
    </dl>
  )
}

function Stat({ label, value, alert = false }: { label: string; value: number; alert?: boolean }) {
  return (
    <div className={`rounded-md border px-3 py-2 ${alert ? 'border-red-300 bg-red-50' : 'border-slate-200 bg-white'}`}>
      <dt className={`text-xs ${alert ? 'text-red-700' : 'text-slate-500'}`}>{label}</dt>
      <dd className={`text-lg font-semibold ${alert ? 'text-red-700' : 'text-slate-900'}`}>{value}</dd>
    </div>
  )
}
