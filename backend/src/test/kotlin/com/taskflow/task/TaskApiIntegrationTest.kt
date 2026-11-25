package com.taskflow.task

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** The task endpoints over HTTP: routing, status codes, validation, JSON shape, ownership. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskApiIntegrationTest(
    @Autowired private val mvc: MockMvc,
    @Autowired private val json: ObjectMapper,
) {
    private lateinit var token: String
    private var projectId = 0L

    private fun register(email: String): String = json.readTree(
        mvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json.writeValueAsString(mapOf("name" to "U", "email" to email, "password" to "password123"))
        }.andReturn().response.contentAsString,
    )["token"].asText()

    // Inside any mvc.post/put/patch { } block: send `payload` as JSON with a token.
    private fun MockHttpServletRequestDsl.jsonBody(payload: Any, as_: String = token) {
        header("Authorization", "Bearer $as_")
        contentType = MediaType.APPLICATION_JSON
        content = json.writeValueAsString(payload)
    }

    private fun createTask(payload: Map<String, Any?>) = mvc.post("/api/projects/$projectId/tasks") { jsonBody(payload) }

    @BeforeEach
    fun setup() {
        token = register("me@example.com")
        projectId = json.readTree(
            mvc.post("/api/projects") { jsonBody(mapOf("name" to "Board")) }.andReturn().response.contentAsString,
        )["id"].asLong()
    }

    @Test
    fun `full task lifecycle`() {
        val created = createTask(mapOf("title" to "Write README", "priority" to "HIGH", "dueDate" to LocalDate.now().toString()))
            .andExpect {
                status { isCreated() }
                jsonPath("$.status") { value("TODO") }
                jsonPath("$.overdue") { value(false) }
                jsonPath("$.projectId") { value(projectId) }
            }.andReturn().response.contentAsString
        val id = json.readTree(created)["id"].asLong()

        mvc.patch("/api/tasks/$id/status") { jsonBody(mapOf("status" to "IN_PROGRESS")) }
            .andExpect { status { isOk() }; jsonPath("$.status") { value("IN_PROGRESS") } }

        mvc.put("/api/tasks/$id") { jsonBody(mapOf("title" to "Write README v2", "status" to "DONE", "priority" to "LOW")) }
            .andExpect { status { isOk() }; jsonPath("$.title") { value("Write README v2") }; jsonPath("$.dueDate") { doesNotExist() } }

        mvc.get("/api/projects/$projectId/tasks?status=DONE") { header("Authorization", "Bearer $token") }
            .andExpect { status { isOk() }; jsonPath("$.totalItems") { value(1) }; jsonPath("$.items[0].id") { value(id) } }

        mvc.get("/api/projects/$projectId/summary") { header("Authorization", "Bearer $token") }
            .andExpect { jsonPath("$.counts.DONE") { value(1) }; jsonPath("$.counts.TODO") { value(0) } }

        mvc.delete("/api/tasks/$id") { header("Authorization", "Bearer $token") }.andExpect { status { isNoContent() } }
        mvc.delete("/api/tasks/$id") { header("Authorization", "Bearer $token") }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `create validates title length and past due dates with field errors`() {
        createTask(mapOf("title" to "x".repeat(121), "dueDate" to LocalDate.now().minusDays(1).toString())).andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[?(@.field == 'title')].message") { value("Title must be at most 120 characters") }
            jsonPath("$.fieldErrors[?(@.field == 'dueDate')].message") { value("Due date cannot be in the past") }
        }
    }

    @Test
    fun `bad enum values are 400, in the body and in the query string`() {
        createTask(mapOf("title" to "t", "priority" to "URGENT")).andExpect { status { isBadRequest() } }
        mvc.get("/api/projects/$projectId/tasks?status=FOO") { header("Authorization", "Bearer $token") }
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `another user gets 404 on my project and my task`() {
        val id = json.readTree(createTask(mapOf("title" to "mine")).andReturn().response.contentAsString)["id"].asLong()
        val other = register("other@example.com")

        mvc.get("/api/projects/$projectId/tasks") { header("Authorization", "Bearer $other") }.andExpect { status { isNotFound() } }
        mvc.patch("/api/tasks/$id/status") { jsonBody(mapOf("status" to "DONE"), other) }
            .andExpect { status { isNotFound() } }
        mvc.delete("/api/tasks/$id") { header("Authorization", "Bearer $other") }.andExpect { status { isNotFound() } }
    }
}
