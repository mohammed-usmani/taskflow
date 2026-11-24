package com.taskflow.task

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate

interface TaskRepository : JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // PUT / PATCH / DELETE /api/tasks/{id}: the task, only if its project is mine.
    // Path: task.project.owner.id
    fun findByIdAndProjectOwnerId(id: Long, ownerId: Long): Task?

    // GET /api/projects/{id}/summary: count per status, ONE query
    @Query("""
        SELECT t.status AS status, COUNT(t) AS count
        FROM Task t
        WHERE t.project.id = :projectId
        GROUP BY t.status
    """)
    fun countByStatus(projectId: Long): List<StatusCount>

    // ...and the overdue count
    @Query("""
        SELECT COUNT(t) FROM Task t
        WHERE t.project.id = :projectId
          AND t.dueDate < :today
          AND t.status <> com.taskflow.task.TaskStatus.DONE
    """)
    fun countOverdue(projectId: Long, today: LocalDate): Long
}

// Projection: Spring fills this interface from the query's aliases (status, count)
interface StatusCount {
    val status: TaskStatus
    val count: Long
}