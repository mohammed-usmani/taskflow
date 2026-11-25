package com.taskflow.task

import com.taskflow.common.BadRequestException
import com.taskflow.common.NotFoundException
import com.taskflow.project.ProjectService
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TaskService(
    private val tasks: TaskRepository,
    private val users: UserRepository,
    private val projectService: ProjectService,
) {
    @Transactional(readOnly = true)
    fun list(userId: Long, projectId: Long, q: TaskListQuery): PageResponse<TaskResponse> {
        projectService.requireOwned(projectId, userId)
        val spec = Specification.allOf(
            listOfNotNull(
                TaskFilters.inProject(projectId),
                q.status?.let(TaskFilters::hasStatus),
                q.priority?.let(TaskFilters::hasPriority),
                q.assigneeId?.let(TaskFilters::assignedTo),
                TaskSort.from(q.sort).asSpec(asc = !q.direction.equals("desc", ignoreCase = true)),
            ),
        )
        // ponytail: page size capped at 100 so one request can't pull the whole table.
        val page = tasks.findAll(spec, PageRequest.of(q.page.coerceAtLeast(0), q.size.coerceIn(1, 100)))
        val today = LocalDate.now()
        return PageResponse(page.content.map { TaskResponse.from(it, today) }, page.number, page.size, page.totalElements, page.totalPages)
    }

    @Transactional(readOnly = true)
    fun summary(userId: Long, projectId: Long): TaskSummaryResponse {
        projectService.requireOwned(projectId, userId)
        val found = tasks.countByStatus(projectId).associate { it.status to it.count }
        // GROUP BY returns no row for a status with no tasks; the UI wants 0, not a missing key.
        val counts = TaskStatus.entries.associateWith { found[it] ?: 0L }
        return TaskSummaryResponse(counts, tasks.countOverdue(projectId, LocalDate.now()), counts.values.sum())
    }

    @Transactional
    fun create(userId: Long, projectId: Long, req: CreateTaskRequest): TaskResponse {
        val project = projectService.requireOwned(projectId, userId)   // 404 if not mine
        val task = Task(
            title = req.title.trim(),
            description = req.description.clean(),
            status = req.status,
            priority = req.priority,
            project = project,
            assignee = req.assigneeId?.let(::findAssignee),
            dueDate = req.dueDate,
        )
        return TaskResponse.from(tasks.save(task))   // IDENTITY: INSERT runs now, id/timestamps set
    }

    @Transactional
    fun update(userId: Long, taskId: Long, req: UpdateTaskRequest): TaskResponse {
        val task = requireOwnedTask(taskId, userId)
        task.title = req.title.trim()
        task.description = req.description.clean()
        task.status = req.status!!          // @NotNull already rejected null with a 400
        task.priority = req.priority!!
        task.dueDate = req.dueDate
        task.assignee = req.assigneeId?.let(::findAssignee)
        // No save() needed: a loaded entity is "managed", Hibernate sees the changes and
        // UPDATEs at commit. We flush now only so @UpdateTimestamp is set in the response.
        return TaskResponse.from(tasks.saveAndFlush(task))
    }

    @Transactional
    fun updateStatus(userId: Long, taskId: Long, req: UpdateStatusRequest): TaskResponse {
        val task = requireOwnedTask(taskId, userId)
        task.status = req.status!!
        return TaskResponse.from(tasks.saveAndFlush(task))
    }

    @Transactional
    fun delete(userId: Long, taskId: Long) {
        tasks.delete(requireOwnedTask(taskId, userId))
    }

    // Task's ownership goes through its project: task.project.owner.id == me. One query.
    private fun requireOwnedTask(taskId: Long, userId: Long): Task =
        tasks.findByIdAndProjectOwnerId(taskId, userId) ?: throw NotFoundException("Task not found")

    // Assumption (README): any registered user can be assigned. 400, not 404: the URL
    // is fine, it's a value in the body that's wrong.
    private fun findAssignee(id: Long): User =
        users.findById(id).orElseThrow { BadRequestException("Assignee $id does not exist") }

    private fun String?.clean() = this?.trim()?.ifBlank { null }
}
