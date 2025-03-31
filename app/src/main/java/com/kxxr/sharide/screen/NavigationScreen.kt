package com.kxxr.sharide.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.maps.model.LatLng

fun openGoogleMapsNavigation(
    context: Context,
    pickup: LatLng,
    stop: LatLng?,
    destination: LatLng
) {
    val waypoints = stop?.let { "|${it.latitude},${it.longitude}" } ?: ""
    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1" +
            "&origin=${pickup.latitude},${pickup.longitude}" +
            "&destination=${destination.latitude},${destination.longitude}" +
            "&waypoints=$waypoints" +
            "&travelmode=driving")

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps") // Ensure Google Maps opens
    }

    context.startActivity(intent)
}
