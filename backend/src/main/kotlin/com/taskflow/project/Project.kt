package com.taskflow.project

import com.taskflow.user.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
@Table(name = "projects")
class Project(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    var owner: User,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null
        protected set
}