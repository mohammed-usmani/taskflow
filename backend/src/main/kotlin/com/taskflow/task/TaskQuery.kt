package com.taskflow.task

import com.taskflow.common.BadRequestException
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

/**
 * Filters for GET /api/projects/{id}/tasks, as composable Specifications.
 *
 * Each filter is one small predicate. We combine only the ones the client sent
 * (null = "not filtered"), so there's one query method instead of a findBy... for
 * every combination of status/priority/assignee.
 */
object TaskFilters {
    fun inProject(projectId: Long) = Specification<Task> { root, _, cb ->
        cb.equal(root.get<Any>("project").get<Long>("id"), projectId)
    }

    fun hasStatus(status: TaskStatus) = Specification<Task> { root, _, cb -> cb.equal(root.get<TaskStatus>("status"), status) }

    fun hasPriority(priority: TaskPriority) = Specification<Task> { root, _, cb -> cb.equal(root.get<TaskPriority>("priority"), priority) }

    fun assignedTo(userId: Long) = Specification<Task> { root, _, cb ->
        cb.equal(root.get<Any>("assignee").get<Long>("id"), userId)
    }
}

/**
 * STRATEGY pattern: each sort option is an object that knows how to build its ORDER BY.
 * The service picks one from the request and never branches on "if sort == ...".
 * Adding a sort = adding one enum entry.
 *
 * Why not Spring's Sort("priority")? priority is stored as text, so it would sort
 * alphabetically: HIGH, LOW, MEDIUM. We sort by its rank instead.
 */
enum class TaskSort(val param: String) {
    DUE_DATE("dueDate") {
        // Tasks without a due date go last in both directions.
        override fun orders(root: Root<Task>, cb: CriteriaBuilder, asc: Boolean): List<Order> {
            val due = root.get<java.time.LocalDate>("dueDate")
            val noDate = cb.selectCase<Int>().`when`(cb.isNull(due), 1).otherwise(0)
            return listOf(cb.asc(noDate), dir(cb, due, asc))
        }
    },
    PRIORITY("priority") {
        override fun orders(root: Root<Task>, cb: CriteriaBuilder, asc: Boolean): List<Order> {
            val p = root.get<TaskPriority>("priority")
            val rank = cb.selectCase<Int>()
                .`when`(cb.equal(p, TaskPriority.LOW), 1)
                .`when`(cb.equal(p, TaskPriority.MEDIUM), 2)
                .otherwise(3)
            return listOf(dir(cb, rank, asc))
        }
    },
    CREATED_AT("createdAt") {
        override fun orders(root: Root<Task>, cb: CriteriaBuilder, asc: Boolean) =
            listOf(dir(cb, root.get<java.time.Instant>("createdAt"), asc))
    };

    abstract fun orders(root: Root<Task>, cb: CriteriaBuilder, asc: Boolean): List<Order>

    protected fun dir(cb: CriteriaBuilder, e: Expression<*>, asc: Boolean): Order = if (asc) cb.asc(e) else cb.desc(e)

    /**
     * ORDER BY as a Specification, so it travels with the filters into findAll(spec, pageable).
     * Spring also runs the spec for the COUNT(*) query of the page; ORDER BY is skipped there.
     * id (always ascending, i.e. oldest first) is the final tie-breaker: without a unique
     * last key, rows with equal values can swap between pages and a task shows up twice or never.
     */
    fun asSpec(asc: Boolean) = Specification<Task> { root, query, cb ->
        if (query != null && query.resultType != java.lang.Long::class.java && query.resultType != Long::class.java) {
            query.orderBy(orders(root, cb, asc) + cb.asc(root.get<Long>("id")))
        }
        null   // no WHERE condition, ordering only
    }

    companion object {
        fun from(param: String): TaskSort = entries.firstOrNull { it.param.equals(param, ignoreCase = true) }
            ?: throw BadRequestException("sort must be one of: ${entries.joinToString { it.param }}")
    }
}
