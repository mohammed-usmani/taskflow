package com.taskflow.task

import com.taskflow.common.BadRequestException
import com.taskflow.common.NotFoundException
import com.taskflow.project.Project
import com.taskflow.project.ProjectService
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskServiceTest {
    private val tasks = mockk<TaskRepository>()
    private val users = mockk<UserRepository>()
    private val projectService = mockk<ProjectService>()
    private val service = TaskService(tasks, users, projectService)

    // Entities get id/timestamps from the database; in unit tests we set them via reflection.
    private fun <T : Any> T.set(field: String, value: Any) = also {
        it.javaClass.getDeclaredField(field).apply { isAccessible = true }.set(it, value)
    }
    private val owner = User("Ali", "ali@example.com", "HASH").set("id", 7L)
    private val project = Project("Website", owner = owner).set("id", 1L)
    private fun Task.saved() = set("id", 10L).set("createdAt", Instant.now()).set("updatedAt", Instant.now())

    @Test
    fun `create uses defaults, trims, and links the assignee`() {
        val saved = slot<Task>()
        every { projectService.requireOwned(1, 7) } returns project
        every { users.findById(7) } returns Optional.of(owner)
        every { tasks.save(capture(saved)) } answers { saved.captured.saved() }

        val res = service.create(7, 1, CreateTaskRequest(title = "  Fix login  ", description = " ", assigneeId = 7))

        assertEquals("Fix login", saved.captured.title)
        assertNull(saved.captured.description)
        assertEquals(TaskStatus.TODO, res.status)
        assertEquals(TaskPriority.MEDIUM, res.priority)
        assertEquals("Ali", res.assignee?.name)
    }

    @Test
    fun `create with an unknown assignee is a 400 and saves nothing`() {
        every { projectService.requireOwned(1, 7) } returns project
        every { users.findById(99) } returns Optional.empty()

        assertThrows<BadRequestException> { service.create(7, 1, CreateTaskRequest(title = "x", assigneeId = 99)) }
        verify(exactly = 0) { tasks.save(any()) }
    }

    @Test
    fun `create in someone else's project fails before touching tasks`() {
        every { projectService.requireOwned(1, 8) } throws NotFoundException("Project not found")

        assertThrows<NotFoundException> { service.create(8, 1, CreateTaskRequest(title = "x")) }
        verify(exactly = 0) { tasks.save(any()) }
    }

    @Test
    fun `update replaces every field, including clearing the assignee`() {
        val task = Task("Old", "old desc", project = project, assignee = owner).saved()
        every { tasks.findByIdAndProjectOwnerId(10, 7) } returns task
        every { tasks.saveAndFlush(task) } returns task

        service.update(7, 10, UpdateTaskRequest("New", null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null, null))

        assertEquals("New", task.title)
        assertNull(task.description)
        assertEquals(TaskStatus.IN_PROGRESS, task.status)
        assertEquals(TaskPriority.HIGH, task.priority)
        assertNull(task.assignee)
    }

    @Test
    fun `updateStatus changes only the status`() {
        val task = Task("Keep", priority = TaskPriority.LOW, project = project).saved()
        every { tasks.findByIdAndProjectOwnerId(10, 7) } returns task
        every { tasks.saveAndFlush(task) } returns task

        val res = service.updateStatus(7, 10, UpdateStatusRequest(TaskStatus.DONE))

        assertEquals(TaskStatus.DONE, res.status)
        assertEquals("Keep", res.title)
        assertEquals(TaskPriority.LOW, res.priority)
    }

    @Test
    fun `update, status change and delete on a task I don't own are 404`() {
        every { tasks.findByIdAndProjectOwnerId(10, 8) } returns null

        assertThrows<NotFoundException> { service.updateStatus(8, 10, UpdateStatusRequest(TaskStatus.DONE)) }
        assertThrows<NotFoundException> { service.delete(8, 10) }
        verify(exactly = 0) { tasks.delete(any<Task>()) }
    }
}
