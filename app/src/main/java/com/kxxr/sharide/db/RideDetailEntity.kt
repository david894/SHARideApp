package com.kxxr.sharide.db

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "ride_detail")
data class RideDetail(
    @PrimaryKey val rideId: String = "",
    val driverId: String = "",
    val location: String = "",
    val stop: String = "",
    val destination: String = "",
    val time: String = "",
    val passengerIds: List<String> = emptyList(),
    val capacity: Int = 0,
    val date: String = "",
    val rideStatus: String ="",
)
