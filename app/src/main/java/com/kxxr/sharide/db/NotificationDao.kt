package com.kxxr.sharide.db
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY id DESC")
    suspend fun getAllNotifications(): List<NotificationEntity>

    @Query("DELETE FROM notifications WHERE time = '0 hours left'")
    fun deleteExpiredNotifications()

    @Query("SELECT * FROM notifications WHERE title = :title AND description = :description LIMIT 1")
    suspend fun getNotification(title: String, description: String): NotificationEntity?

    @Query("UPDATE notifications SET time = :newTime WHERE title = :title AND description = :description")
    suspend fun updateNotificationTime(title: String, description: String, newTime: String)

    @Query("DELETE FROM notifications")
    fun clearAllNotifications()

}
