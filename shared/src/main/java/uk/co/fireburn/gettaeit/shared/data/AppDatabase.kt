package uk.co.fireburn.gettaeit.shared.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, UserPreferences::class, ShoppingItemEntity::class, TaskTombstone::class],
    version = 20,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun shoppingItemDao(): ShoppingItemDao
    abstract fun taskTombstoneDao(): TaskTombstoneDao

    companion object {
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN effortLevel TEXT NOT NULL DEFAULT 'MEDIUM'")
            }
        }
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN frictionNote TEXT")
            }
        }
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN routineTemplatesJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS shopping_items (id TEXT NOT NULL, title TEXT NOT NULL, category TEXT NOT NULL, isBought INTEGER NOT NULL, supermarket TEXT, createdAt INTEGER NOT NULL, PRIMARY KEY(id))")
            }
        }
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN wearHapticsEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()
                db.execSQL("ALTER TABLE tasks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT $now")
            }
        }
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN xp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN dailySpoons INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN lastSpoonUpdateDate INTEGER NOT NULL DEFAULT 0")
            }
        }
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_preferences ADD COLUMN unlockedStickersJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
        internal val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `task_tombstones` (`id` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        private val ALL_MIGRATIONS = arrayOf(
            MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15,
            MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20
        )

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "get_tae_it_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    // Only pre-release installs older than v10 may be wiped. Anything newer must
                    // have a real migration (see AppDatabaseMigrationTest) rather than silently
                    // losing someone's tasks.
                    .fallbackToDestructiveMigrationFrom(true, *(1..9).toList().toIntArray())
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
