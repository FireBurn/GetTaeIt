package uk.co.fireburn.gettaeit.shared.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun `exported v19 schema builds a usable database`() {
        helper.createDatabase(TEST_DB, 19).use { db ->
            db.execSQL("INSERT INTO tasks (id, title, context, priority, effortLevel, isCompleted, isArchived, isSnoozed, recurrence, dependencyIds, isSubtask, xpValue, streakCount, updatedAt) VALUES ('00000000-0000-0000-0000-000000000001', 'Put the bins out', 'PERSONAL', 3, 'MEDIUM', 0, 0, 0, '{}', '[]', 0, 10, 0, 0)")
            db.query("SELECT COUNT(*) FROM tasks").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
