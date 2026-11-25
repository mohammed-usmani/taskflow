package com.taskflow.config

import com.taskflow.project.Project
import com.taskflow.project.ProjectRepository
import com.taskflow.task.Task
import com.taskflow.task.TaskPriority
import com.taskflow.task.TaskRepository
import com.taskflow.task.TaskStatus
import com.taskflow.user.User
import com.taskflow.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Sample data so a reviewer can log in straight away (README lists the login).
 * Runs once at startup; does nothing if the demo user already exists, so restarts
 * don't duplicate it. Turn off with SEED_DEMO_DATA=false.
 *
 * Writes through repositories, not services, on purpose: the overdue example task
 * has a past due date, which the create endpoint (rightly) refuses.
 */
@Component
@ConditionalOnProperty("app.seed.enabled", havingValue = "true")
class DemoDataSeeder(
    private val users: UserRepository,
    private val projects: ProjectRepository,
    private val tasks: TaskRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (users.existsByEmailIgnoreCase(DEMO_EMAIL)) return
        val demo = users.save(User("Demo User", DEMO_EMAIL, passwordEncoder.encode(DEMO_PASSWORD)))
        val teammate = users.save(User("Sam Patel", "sam@taskflow.dev", passwordEncoder.encode(DEMO_PASSWORD)))
        val project = projects.save(Project("Website relaunch", "Redesign and launch the marketing site", demo))
        val today = LocalDate.now()
        tasks.saveAll(
            listOf(
                Task("Write landing page copy", "Hero, features, pricing", TaskStatus.TODO, TaskPriority.HIGH, project, demo, today.plusDays(2)),
                Task("Pick a colour palette", null, TaskStatus.TODO, TaskPriority.LOW, project, teammate, today.plusDays(7)),
                Task("Fix mobile navbar", "Menu overlaps the logo under 400px", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, project, teammate, today.plusDays(1)),
                Task("Set up analytics", null, TaskStatus.TODO, TaskPriority.MEDIUM, project, null, today.minusDays(1)),   // overdue
                Task("Buy domain", null, TaskStatus.DONE, TaskPriority.HIGH, project, demo, today.minusDays(3)),
            ),
        )
        log.info("Seeded demo data: log in as {}", DEMO_EMAIL)
    }

    companion object {
        const val DEMO_EMAIL = "demo@taskflow.dev"
        const val DEMO_PASSWORD = "demo1234"
    }
}
