package com.taskflow.project

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectIntegrationTest(
    @Autowired private val mvc: MockMvc,
    @Autowired private val json: ObjectMapper,
) {
    private fun tokenFor(email: String): String {
        val body = mvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json.writeValueAsString(mapOf("name" to "U", "email" to email, "password" to "password123"))
        }.andReturn().response.contentAsString
        return json.readTree(body)["token"].asText()
    }

    private fun createProject(token: String, name: String) = mvc.post("/api/projects") {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        content = json.writeValueAsString(mapOf("name" to name))
    }

    @Test
    fun `create returns 201 and the list shows only my projects, newest first`() {
        val ali = tokenFor("ali@example.com")
        val sara = tokenFor("sara@example.com")
        createProject(ali, "First").andExpect {
            status { isCreated() }
            jsonPath("$.id") { isNumber() }
            jsonPath("$.createdAt") { isNotEmpty() }
        }
        createProject(ali, "Second")
        createProject(sara, "Sara's project")

        mvc.get("/api/projects") { header("Authorization", "Bearer $ali") }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].name") { value("Second") }
            jsonPath("$[*].owner") { doesNotExist() }
        }
    }

    @Test
    fun `blank name is 400 with a field error`() {
        createProject(tokenFor("ali@example.com"), "  ").andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("name") }
        }
    }

    @Test
    fun `projects need a token`() {
        mvc.get("/api/projects").andExpect { status { isUnauthorized() } }
    }
}
