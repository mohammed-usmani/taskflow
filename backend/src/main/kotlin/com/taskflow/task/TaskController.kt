package com.taskflow.task

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * URLs follow the brief: tasks are created and listed under their project,
 * but edited by their own id (/api/tasks/{id}), since a task id is unique on its own.
 */
@RestController
class TaskController(private val taskService: TaskService) {

    // ?status=TODO&priority=HIGH&assigneeId=3&sort=dueDate&direction=desc&page=0&size=20
    // @ModelAttribute binds those query params onto TaskListQuery (missing ones keep their defaults).
    @GetMapping("/api/projects/{projectId}/tasks")
    fun list(
        @AuthenticationPrincipal userId: Long,
        @PathVariable projectId: Long,
        @ModelAttribute query: TaskListQuery,
    ): PageResponse<TaskResponse> = taskService.list(userId, projectId, query)

    @PostMapping("/api/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: Long,
        @PathVariable projectId: Long,
        @Valid @RequestBody req: CreateTaskRequest,
    ): TaskResponse = taskService.create(userId, projectId, req)

    @GetMapping("/api/projects/{projectId}/summary")
    fun summary(@AuthenticationPrincipal userId: Long, @PathVariable projectId: Long): TaskSummaryResponse =
        taskService.summary(userId, projectId)

    @PutMapping("/api/tasks/{id}")
    fun update(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateTaskRequest,
    ): TaskResponse = taskService.update(userId, id, req)

    @PatchMapping("/api/tasks/{id}/status")
    fun updateStatus(
        @AuthenticationPrincipal userId: Long,
        @PathVariable id: Long,
        @Valid @RequestBody req: UpdateStatusRequest,
    ): TaskResponse = taskService.updateStatus(userId, id, req)

    // 204 No Content: deleted, nothing to send back.
    @DeleteMapping("/api/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal userId: Long, @PathVariable id: Long) = taskService.delete(userId, id)
}
