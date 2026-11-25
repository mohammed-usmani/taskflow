package com.taskflow.task

import com.taskflow.auth.UserResponse
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate

/*
 * Requests. Same conventions as AuthDtos/ProjectDtos (@field:, "" defaults).
 * Enums: if a client sends "priority": "URGENT", Jackson can't build the enum and
 * GlobalExceptionHandler answers 400 "Malformed request body".
 * assigneeId, not a User: the client only knows ids; the service looks the user up.
 */
data class CreateTaskRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 120, message = "Title must be at most 120 characters")
    val title: String = "",

    @field:Size(max = 2000, message = "Description must be at most 2000 characters")
    val description: String? = null,

    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,

    // The brief: "dueDate not in the past on create". Today is allowed.
    @field:FutureOrPresent(message = "Due date cannot be in the past")
    val dueDate: LocalDate? = null,

    val assigneeId: Long? = null,
)

/*
 * PUT = replace every editable field, so all of them are sent (null clears
 * description / dueDate / assignee). No @FutureOrPresent here on purpose: the rule
 * is "on create", and an existing task whose date has passed must stay editable.
 */
data class UpdateTaskRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 120, message = "Title must be at most 120 characters")
    val title: String = "",

    @field:Size(max = 2000, message = "Description must be at most 2000 characters")
    val description: String? = null,

    @field:NotNull(message = "Status is required")
    val status: TaskStatus? = null,

    @field:NotNull(message = "Priority is required")
    val priority: TaskPriority? = null,

    val dueDate: LocalDate? = null,
    val assigneeId: Long? = null,
)

// PATCH /api/tasks/{id}/status. Nullable + @NotNull so a missing status is a clean field error.
data class UpdateStatusRequest(
    @field:NotNull(message = "Status is required")
    val status: TaskStatus? = null,
)

data class TaskResponse(
    val id: Long,
    val projectId: Long,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val priority: TaskPriority,
    val dueDate: LocalDate?,
    val overdue: Boolean,          // computed once here, so the board and the summary agree
    val assignee: UserResponse?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        // Factory (entity -> DTO). `today` is a parameter so tests can pin the date.
        fun from(t: Task, today: LocalDate = LocalDate.now()) = TaskResponse(
            id = t.id!!,
            projectId = t.project.id!!,
            title = t.title,
            description = t.description,
            status = t.status,
            priority = t.priority,
            dueDate = t.dueDate,
            overdue = isOverdue(t.dueDate, t.status, today),
            assignee = t.assignee?.let(UserResponse::from),
            createdAt = t.createdAt!!,
            updatedAt = t.updatedAt!!,
        )

        // Same rule as TaskRepository.countOverdue: past due and not finished.
        fun isOverdue(dueDate: LocalDate?, status: TaskStatus, today: LocalDate) =
            dueDate != null && dueDate.isBefore(today) && status != TaskStatus.DONE
    }
}

// GET /api/projects/{id}/summary
data class TaskSummaryResponse(
    val counts: Map<TaskStatus, Long>,   // always all three statuses, zeros included
    val overdue: Long,
    val total: Long,
)

/*
 * Our own page shape instead of returning Spring's Page<T> directly: Page's JSON
 * has internal fields (pageable, sort...) and Spring warns its format isn't stable.
 */
data class PageResponse<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
)

// Query parameters of GET /api/projects/{id}/tasks; Spring binds ?status=TODO&page=0... onto it.
data class TaskListQuery(
    val status: TaskStatus? = null,
    val priority: TaskPriority? = null,
    val assigneeId: Long? = null,
    val sort: String = "createdAt",     // dueDate | priority | createdAt
    val direction: String = "asc",      // asc | desc
    val page: Int = 0,
    val size: Int = 20,
)
