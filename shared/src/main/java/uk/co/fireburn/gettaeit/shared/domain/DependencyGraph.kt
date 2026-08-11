package uk.co.fireburn.gettaeit.shared.domain

import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.UUID

/**
 * Pure helpers for reasoning about the task dependency graph: how many other
 * tasks a task is holding up, and whether wiring up a new blocker would create
 * a loop (A blocks B blocks A).
 */
object DependencyGraph {

    /**
     * True if making [taskId] depend on [proposedDependencyId] would create a cycle,
     * i.e. [proposedDependencyId] (directly or transitively) already depends on [taskId].
     * [taskId] is null for a not-yet-saved task, which can never be part of an existing
     * cycle since nothing can depend on an id that doesn't exist yet.
     */
    fun wouldCreateCycle(
        taskId: UUID?,
        proposedDependencyId: UUID,
        allTasks: List<TaskEntity>
    ): Boolean {
        if (taskId == null) return false
        if (proposedDependencyId == taskId) return true

        val byId = allTasks.associateBy { it.id }
        val visited = mutableSetOf<UUID>()
        val stack = ArrayDeque<UUID>().apply { add(proposedDependencyId) }

        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (!visited.add(current)) continue
            if (current == taskId) return true
            byId[current]?.dependencyIds?.forEach { stack.add(it) }
        }
        return false
    }

    /**
     * Direct + transitive count of tasks that are unblocked by [taskId] completing —
     * i.e. how many other tasks have [taskId] anywhere in their dependency chain.
     * A task blocking three other tasks that each block one more thing counts as 4,
     * not 3 — the whole chain is riding on it.
     */
    fun transitiveUnblockCount(taskId: UUID, allTasks: List<TaskEntity>): Int {
        val dependents = mutableMapOf<UUID, MutableList<UUID>>()
        allTasks.forEach { task ->
            task.dependencyIds.forEach { depId ->
                dependents.getOrPut(depId) { mutableListOf() }.add(task.id)
            }
        }
        val visited = mutableSetOf<UUID>()
        val stack = ArrayDeque<UUID>().apply { add(taskId) }
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            dependents[current]?.forEach { child ->
                if (visited.add(child)) stack.add(child)
            }
        }
        return visited.size
    }
}
