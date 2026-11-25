package com.taskflow.project

import com.taskflow.common.NotFoundException
import com.taskflow.user.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectService(
    private val projects: ProjectRepository,
    private val users: UserRepository,
) {
    @Transactional(readOnly = true)
    fun list(userId: Long): List<ProjectResponse> =
        projects.findAllByOwnerIdOrderByCreatedAtDesc(userId).map(ProjectResponse::from)

    @Transactional
    fun create(userId: Long, req: CreateProjectRequest): ProjectResponse {
        // getReferenceById = a proxy that only knows the id: no SELECT on users.
        // That's all JPA needs to write owner_id. (The user exists: their JWT proves it.)
        val owner = users.getReferenceById(userId)
        val project = Project(req.name.trim(), req.description?.trim()?.ifBlank { null }, owner)
        // IDENTITY ids make save() INSERT immediately (the DB generates the id),
        // so id and createdAt are already filled when ProjectResponse.from reads them.
        return ProjectResponse.from(projects.save(project))
    }

    /**
     * THE ownership check. Every task and summary endpoint calls this first.
     * 404, not 403: a 403 would confirm that project id exists and belongs to
     * someone else. To this user, a project they don't own doesn't exist.
     * (Want project members later? This is the one method to change.)
     */
    @Transactional(readOnly = true)
    fun requireOwned(projectId: Long, userId: Long): Project =
        projects.findByIdAndOwnerId(projectId, userId) ?: throw NotFoundException("Project not found")
}
