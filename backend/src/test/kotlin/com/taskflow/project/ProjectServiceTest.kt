package com.taskflow.project

import com.taskflow.common.NotFoundException
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ProjectServiceTest {
    private val projects = mockk<ProjectRepository>()
    private val users = mockk<UserRepository>()
    private val service = ProjectService(projects, users)
    private val owner = User("Ali", "ali@example.com", "HASH")

    // id and createdAt are set by the database; in a unit test we set them via reflection.
    private fun Project.saved(id: Long) = also {
        Project::class.java.getDeclaredField("id").apply { isAccessible = true }.set(it, id)
        Project::class.java.getDeclaredField("createdAt").apply { isAccessible = true }.set(it, Instant.now())
    }

    @Test
    fun `requireOwned returns the project when the user owns it`() {
        val project = Project("Website", owner = owner).saved(1)
        every { projects.findByIdAndOwnerId(1, 7) } returns project

        assertSame(project, service.requireOwned(1, 7))
    }

    @Test
    fun `requireOwned throws NotFound when the project is someone else's or missing`() {
        every { projects.findByIdAndOwnerId(1, 8) } returns null

        assertThrows<NotFoundException> { service.requireOwned(1, 8) }
    }

    @Test
    fun `create trims input, stores blank description as null, and sets the owner`() {
        val saved = slot<Project>()
        every { users.getReferenceById(7) } returns owner
        every { projects.save(capture(saved)) } answers { saved.captured.saved(3) }

        val res = service.create(7, CreateProjectRequest("  Website  ", "   "))

        assertEquals("Website", saved.captured.name)
        assertNull(saved.captured.description)
        assertSame(owner, saved.captured.owner)
        assertEquals(3, res.id)
    }
}
