package com.taskflow.project

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

// What the client sends to create a project. Same validation rules as AuthDtos:
// @field: so Bean Validation sees the annotation, "" default so a missing name
// becomes a clean "Name is required" field error.
data class CreateProjectRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must be at most 100 characters")
    val name: String = "",

    // Optional. The column is TEXT (unlimited), but an API should still cap input.
    @field:Size(max = 2000, message = "Description must be at most 2000 characters")
    val description: String? = null,
)

// What the API returns. No owner object: every project a user sees is their own.
data class ProjectResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Instant,
) {
    companion object {
        // id and createdAt are null only before the first save; a project we return is always saved.
        fun from(p: Project) = ProjectResponse(p.id!!, p.name, p.description, p.createdAt!!)
    }
}
