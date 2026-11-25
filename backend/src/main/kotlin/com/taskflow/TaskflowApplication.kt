package com.taskflow

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication

// We authenticate with our own JWT filter, so Spring's default in-memory user
// (and its "Using generated security password" log line) is switched off.
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class TaskflowApplication

fun main(args: Array<String>) {
    runApplication<TaskflowApplication>(*args)
}
