package com.kxxr.sharide.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [NotificationEntity::class], version = 2) // Step 1: Change version to 2
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        // Step 2: Define the migration from version 1 to 2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Step 3: Add new column `postTime` with default value ''
                database.execSQL("ALTER TABLE notifications ADD COLUMN postTime TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification_database"
                )
                    .addMigrations(MIGRATION_1_2) // Step 4: Apply the migration
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

