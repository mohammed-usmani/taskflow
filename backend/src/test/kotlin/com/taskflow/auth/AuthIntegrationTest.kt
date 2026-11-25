package com.taskflow.auth

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

/**
 * Full stack through real HTTP handling: security filter chain, validation,
 * controller, service, JPA, Postgres. @Transactional rolls every test back,
 * so nothing is left in the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest(
    @Autowired private val mvc: MockMvc,
    @Autowired private val json: ObjectMapper,
) {
    private fun register(email: String = "ali@example.com", password: String = "password123") =
        mvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = json.writeValueAsString(mapOf("name" to "Ali", "email" to email, "password" to password))
        }

    @Test
    fun `register returns 201 with a token and no password hash`() {
        register().andExpect {
            status { isCreated() }
            jsonPath("$.token") { isNotEmpty() }
            jsonPath("$.user.email") { value("ali@example.com") }
            jsonPath("$.user.passwordHash") { doesNotExist() }
        }
    }

    @Test
    fun `duplicate email is 409 even with different case`() {
        register()
        register(email = "ALI@Example.com").andExpect {
            status { isConflict() }
            jsonPath("$.message") { value("Email is already registered") }
        }
    }

    @Test
    fun `invalid body is 400 with field level errors`() {
        mvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"","email":"not-an-email","password":"short"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[?(@.field == 'name')]") { exists() }
            jsonPath("$.fieldErrors[?(@.field == 'email')]") { exists() }
            jsonPath("$.fieldErrors[?(@.field == 'password')]") { exists() }
        }
    }

    @Test
    fun `login with wrong password is 401, right password returns a token`() {
        register()
        mvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"ali@example.com","password":"wrong-password"}"""
        }.andExpect { status { isUnauthorized() } }

        mvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"Ali@Example.com","password":"password123"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { isNotEmpty() }
        }
    }

    @Test
    fun `protected endpoint needs a valid token`() {
        mvc.get("/api/users/me").andExpect {
            status { isUnauthorized() }
            jsonPath("$.message") { value("Missing, invalid or expired token") }
        }
        mvc.get("/api/users/me") { header("Authorization", "Bearer garbage") }
            .andExpect { status { isUnauthorized() } }

        val body = register().andReturn().response.contentAsString
        val token = json.readTree(body)["token"].asText()
        mvc.get("/api/users/me") { header("Authorization", "Bearer $token") }.andExpect {
            status { isOk() }
            jsonPath("$.email") { value("ali@example.com") }
        }
    }
}
