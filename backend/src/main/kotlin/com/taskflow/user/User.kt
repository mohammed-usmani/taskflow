package com.taskflow.user

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant


@Entity
@Table(name = "users")
class User (
    @Column(nullable = false, length = 100) var name: String,
    @Column(nullable = false, length = 255) var email: String,
    @Column(nullable = false, length = 100) var passwordHash: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set

}