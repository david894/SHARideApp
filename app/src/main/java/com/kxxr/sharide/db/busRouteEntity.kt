package com.kxxr.sharide.db

import com.google.gson.annotations.SerializedName

data class BusRoute(
    @SerializedName("route name") val routeName: String,
    val stops: List<String>,
    @SerializedName("arrival times") val arrivalTimes: List<String>
)

data class BusData(
    val routes: List<BusRoute>
)
