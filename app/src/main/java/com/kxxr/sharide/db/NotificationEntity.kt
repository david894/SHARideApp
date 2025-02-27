package com.kxxr.sharide.db
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val image: Int,
    val title: String,
    val description: String,
    val time: String,
)
