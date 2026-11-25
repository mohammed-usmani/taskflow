package com.taskflow.task

import com.taskflow.common.BadRequestException
import com.taskflow.common.NotFoundException
import com.taskflow.project.CreateProjectRequest
import com.taskflow.project.ProjectService
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * Filters, sorting, paging and the summary run real SQL, so they're tested against
 * Postgres through the service (mocks can't tell us whether a Specification is right).
 * Calling the service directly skips @Valid, which lets us create past-due tasks.
 */
@SpringBootTest
@Transactional
class TaskQueryIntegrationTest(
    @Autowired private val taskService: TaskService,
    @Autowired private val projectService: ProjectService,
    @Autowired private val users: UserRepository,
) {
    private val today = LocalDate.now()
    private var me = 0L
    private var bob = 0L
    private var projectId = 0L

    @BeforeEach
    fun seed() {
        me = users.save(User("Me", "me@example.com", "h")).id!!
        bob = users.save(User("Bob", "bob@example.com", "h")).id!!
        projectId = projectService.create(me, CreateProjectRequest("Board")).id
        fun task(title: String, status: TaskStatus, priority: TaskPriority, due: LocalDate?, assignee: Long? = null) =
            taskService.create(me, projectId, CreateTaskRequest(title, null, status, priority, due, assignee))
        task("a", TaskStatus.TODO, TaskPriority.LOW, today.plusDays(3), bob)
        task("b", TaskStatus.TODO, TaskPriority.HIGH, today.minusDays(2))          // overdue
        task("c", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, null, bob)
        task("d", TaskStatus.DONE, TaskPriority.HIGH, today.minusDays(5))          // past but DONE: not overdue
    }

    private fun titles(q: TaskListQuery) = taskService.list(me, projectId, q).items.map { it.title }

    @Test
    fun `filters combine`() {
        assertEquals(listOf("a", "b"), titles(TaskListQuery(status = TaskStatus.TODO)))
        assertEquals(listOf("b", "d"), titles(TaskListQuery(priority = TaskPriority.HIGH)))
        assertEquals(listOf("a"), titles(TaskListQuery(status = TaskStatus.TODO, assigneeId = bob)))
    }

    @Test
    fun `priority sorts by rank, not alphabetically`() {
        // ties (b and d are both HIGH) are broken by id
        assertEquals(listOf("b", "d", "c", "a"), titles(TaskListQuery(sort = "priority", direction = "desc")))
    }

    @Test
    fun `due date sort puts tasks without a date last in both directions`() {
        assertEquals(listOf("d", "b", "a", "c"), titles(TaskListQuery(sort = "dueDate")))
        assertEquals(listOf("a", "b", "d", "c"), titles(TaskListQuery(sort = "dueDate", direction = "desc")))
    }

    @Test
    fun `pagination reports totals and overdue is flagged`() {
        val page = taskService.list(me, projectId, TaskListQuery(sort = "dueDate", page = 0, size = 3))
        assertEquals(3, page.items.size)
        assertEquals(4, page.totalItems)
        assertEquals(2, page.totalPages)
        assertEquals(listOf(false, true, false), page.items.map { it.overdue })   // d is DONE, b is overdue
    }

    @Test
    fun `summary counts every status and the overdue tasks`() {
        val s = taskService.summary(me, projectId)
        assertEquals(mapOf(TaskStatus.TODO to 2L, TaskStatus.IN_PROGRESS to 1L, TaskStatus.DONE to 1L), s.counts)
        assertEquals(1, s.overdue)
        assertEquals(4, s.total)
    }

    @Test
    fun `summary of an empty project has zeros, not missing keys`() {
        val empty = projectService.create(me, CreateProjectRequest("Empty")).id
        assertEquals(TaskStatus.entries.associateWith { 0L }, taskService.summary(me, empty).counts)
    }

    @Test
    fun `another user can't list or summarise my project, and bad sort is a 400`() {
        assertThrows<NotFoundException> { taskService.list(bob, projectId, TaskListQuery()) }
        assertThrows<NotFoundException> { taskService.summary(bob, projectId) }
        assertThrows<BadRequestException> { taskService.list(me, projectId, TaskListQuery(sort = "title")) }
    }
}
