package com.taskflow.project

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/projects")
class ProjectController(private val projectService: ProjectService) {

    // userId comes from the JWT (JwtAuthFilter), never from the request body or URL,
    // so a client can't claim to be someone else.
    @GetMapping
    fun list(@AuthenticationPrincipal userId: Long): List<ProjectResponse> = projectService.list(userId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal userId: Long,
        @Valid @RequestBody req: CreateProjectRequest,
    ): ProjectResponse = projectService.create(userId, req)
}
