package uk.co.fireburn.gettaeit.shared.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.co.fireburn.gettaeit.shared.data.TaskEntity
import java.util.UUID

class DependencyGraphTest {

    @Test
    fun `adding a direct back-edge is rejected`() {
        val first = task()
        val second = task(dependencies = listOf(first.id))

        assertTrue(
            DependencyGraph.wouldCreateCycle(
                taskId = first.id,
                proposedDependencyId = second.id,
                allTasks = listOf(first, second)
            )
        )
    }

    @Test
    fun `adding an independent dependency is allowed`() {
        val first = task()
        val second = task()

        assertFalse(
            DependencyGraph.wouldCreateCycle(
                taskId = first.id,
                proposedDependencyId = second.id,
                allTasks = listOf(first, second)
            )
        )
    }

    @Test
    fun `transitive unblock count includes every downstream task once`() {
        val blocker = task()
        val middle = task(dependencies = listOf(blocker.id))
        val end = task(dependencies = listOf(middle.id))
        val alsoBlocked = task(dependencies = listOf(blocker.id))

        assertEquals(
            3,
            DependencyGraph.transitiveUnblockCount(blocker.id, listOf(blocker, middle, end, alsoBlocked))
        )
    }

    private fun task(dependencies: List<UUID> = emptyList()) = TaskEntity(
        title = "Task",
        dependencyIds = dependencies
    )
}
