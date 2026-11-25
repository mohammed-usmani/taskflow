import { useState } from 'react'
import {
  DndContext, DragOverlay, KeyboardSensor, PointerSensor, rectIntersection, useDraggable, useDroppable, useSensor, useSensors,
  type Announcements, type DragEndEvent, type DragStartEvent, type KeyboardCoordinateGetter,
} from '@dnd-kit/core'
import { STATUSES, type Task, type TaskStatus } from '../api/types'
import { TaskCard } from '../components/TaskCard'
import { statusLabel } from '../components/labels'

interface Props {
  tasks: Task[]
  onStatusChange: (id: number, status: TaskStatus) => void
  onEdit: (task: Task) => void
}

/**
 * The three columns, with drag-and-drop between them (@dnd-kit).
 * A drop calls the same onStatusChange as the dropdown, so it gets the same optimistic
 * update and rollback. The dropdown stays as a simple alternative.
 *
 * Dragging starts from a handle, not the whole card, so clicks and key presses on the
 * card's dropdown and Edit link never start a drag. Keyboard: focus the handle, Space to
 * pick up, arrow keys to move column, Space to drop, Escape to cancel.
 */
export function BoardColumns({ tasks, onStatusChange, onEdit }: Props) {
  const [dragging, setDragging] = useState<Task | null>(null)
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }), // mouse + touch; a tap isn't a drag
    useSensor(KeyboardSensor, { coordinateGetter: columnByColumn }),
  )

  const onDragStart = (e: DragStartEvent) => setDragging(taskOf(e.active.data.current))
  const onDragEnd = (e: DragEndEvent) => {
    setDragging(null)
    const task = taskOf(e.active.data.current)
    const to = e.over?.id as TaskStatus | undefined
    if (task && to && to !== task.status) onStatusChange(task.id, to)
  }

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={rectIntersection}
      onDragStart={onDragStart}
      onDragEnd={onDragEnd}
      onDragCancel={() => setDragging(null)}
      accessibility={{ announcements }}
    >
      <div className="grid gap-4 md:grid-cols-3">
        {STATUSES.map((status) => (
          <Column
            key={status}
            status={status}
            tasks={tasks.filter((t) => t.status === status)}
            onStatusChange={onStatusChange}
            onEdit={onEdit}
          />
        ))}
      </div>
      {/* The card that follows the pointer. Rendered on top of everything, so it isn't clipped by its column.
          dropAnimation={null}: the default animation flies the copy back to where the card started,
          which looks like the move failed. The optimistic update already puts the card in its new column. */}
      <DragOverlay dropAnimation={null}>{dragging && <TaskCard task={dragging} onStatusChange={() => {}} />}</DragOverlay>
    </DndContext>
  )
}

function Column({ status, tasks, onStatusChange, onEdit }: { status: TaskStatus } & Props) {
  const { setNodeRef, isOver } = useDroppable({ id: status })
  return (
    <section
      ref={setNodeRef}
      aria-label={statusLabel[status]}
      data-column={status}
      className={`rounded-lg p-3 transition-colors ${isOver ? 'bg-indigo-100 ring-2 ring-indigo-300' : 'bg-slate-100'}`}
    >
      <h2 className="mb-3 flex items-center justify-between text-sm font-semibold text-slate-700">
        {statusLabel[status]}
        <span className="rounded-full bg-white px-2 text-xs text-slate-500">{tasks.length}</span>
      </h2>
      {tasks.length === 0 ? (
        <p className="rounded-md border border-dashed border-slate-300 p-4 text-center text-xs text-slate-500">No tasks</p>
      ) : (
        <ul className="space-y-2">
          {tasks.map((t) => (
            <DraggableCard key={t.id} task={t} onStatusChange={onStatusChange} onEdit={onEdit} />
          ))}
        </ul>
      )}
    </section>
  )
}

function DraggableCard({ task, onStatusChange, onEdit }: { task: Task } & Omit<Props, 'tasks'>) {
  const { setNodeRef, setActivatorNodeRef, listeners, attributes, isDragging } = useDraggable({ id: task.id, data: { task } })
  return (
    <li ref={setNodeRef} className={isDragging ? 'opacity-40' : ''}>
      <TaskCard
        task={task}
        onStatusChange={(s) => onStatusChange(task.id, s)}
        onEdit={() => onEdit(task)}
        dragHandle={
          <button
            ref={setActivatorNodeRef}
            {...listeners}
            {...attributes}
            aria-label={`Move ${task.title}`}
            className="-ml-1 cursor-grab touch-none rounded px-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600 active:cursor-grabbing"
          >
            ⠿
          </button>
        }
      />
    </li>
  )
}

const taskOf = (data: unknown) => (data as { task?: Task } | undefined)?.task ?? null

/**
 * Keyboard movement: by default each arrow press moves the card 25px, which would take
 * a dozen presses to cross a column. This jumps straight to the next column instead
 * (Right/Down = next status, Left/Up = previous; Down/Up matter on mobile, where columns stack).
 */
const columnByColumn: KeyboardCoordinateGetter = (event, { context }) => {
  const step = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 }[event.code]
  if (!step) return undefined
  event.preventDefault()
  const current = (context.over?.id as TaskStatus | undefined) ?? taskOf(context.active?.data.current)?.status ?? 'TODO'
  const next = STATUSES[Math.min(Math.max(STATUSES.indexOf(current) + step, 0), STATUSES.length - 1)]
  const target = context.droppableRects.get(next)
  const card = context.collisionRect
  if (!target || !card) return undefined
  // Centre the card inside the target column.
  return { x: target.left + (target.width - card.width) / 2, y: target.top + Math.min(40, Math.max(0, (target.height - card.height) / 2)) }
}

// What screen readers hear (the defaults would say "draggable item 5" and "droppable area IN_PROGRESS").
const announcements: Announcements = {
  onDragStart: ({ active }) =>
    `Picked up ${taskOf(active.data.current)?.title}. Use the arrow keys to move between columns, Space to drop, Escape to cancel.`,
  onDragOver: ({ active, over }) =>
    over ? `${taskOf(active.data.current)?.title} is over ${statusLabel[over.id as TaskStatus]}.` : undefined,
  onDragEnd: ({ active, over }) =>
    over ? `${taskOf(active.data.current)?.title} moved to ${statusLabel[over.id as TaskStatus]}.` : `${taskOf(active.data.current)?.title} dropped.`,
  onDragCancel: ({ active }) => `Moving ${taskOf(active.data.current)?.title} cancelled.`,
}
