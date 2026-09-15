package uk.co.fireburn.gettaeit.shared.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
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
            db.execSQL(INSERT_TASK)
            assertEquals(1, db.count("tasks"))
        }
    }

    @Test
    fun `v19 to v20 adds tombstones and keeps existing tasks`() {
        helper.createDatabase(TEST_DB, 19).use { it.execSQL(INSERT_TASK) }

        helper.runMigrationsAndValidate(TEST_DB, 20, true, AppDatabase.MIGRATION_19_20).use { db ->
            assertEquals(1, db.count("tasks"))
            db.execSQL("INSERT INTO task_tombstones (id, deletedAt) VALUES ('00000000-0000-0000-0000-000000000009', 1)")
            assertEquals(1, db.count("task_tombstones"))
        }
    }

    @Test
    fun `v20 to v21 keeps work hours and starts them on the hour`() {
        helper.createDatabase(TEST_DB, 20).use {
            it.execSQL("INSERT INTO user_preferences (id, workLocationRadius, isVacationMode, wearHapticsEnabled, xp, dailySpoons, lastSpoonUpdateDate, routineTemplatesJson, unlockedStickersJson, startHour, endHour, workingDays) VALUES (1, 100.0, 0, 1, 0, 5, 0, '[]', '[]', 8, 16, '[2,3,4,5,6]')")
        }

        helper.runMigrationsAndValidate(TEST_DB, 21, true, AppDatabase.MIGRATION_20_21).use { db ->
            db.query("SELECT startHour, startMinute, endHour, endMinute FROM user_preferences").use { cursor ->
                cursor.moveToFirst()
                assertEquals(listOf(8, 0, 16, 0), (0..3).map { cursor.getInt(it) })
            }
        }
    }

    private fun SupportSQLiteDatabase.count(table: String): Int =
        query("SELECT COUNT(*) FROM $table").use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private companion object {
        const val TEST_DB = "migration-test"
        const val INSERT_TASK = "INSERT INTO tasks (id, title, context, priority, effortLevel, isCompleted, isArchived, isSnoozed, recurrence, dependencyIds, isSubtask, xpValue, streakCount, updatedAt) VALUES ('00000000-0000-0000-0000-000000000001', 'Put the bins out', 'PERSONAL', 3, 'MEDIUM', 0, 0, 0, '{}', '[]', 0, 10, 0, 0)"
    }
}
