package com.kxxr.sharide.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passenger")
data class Passenger(
    @PrimaryKey val passengerId: String = "",
    val name: String = "",
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val imageRes: String = "",
    val time: String = ""
)
