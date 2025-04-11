package com.kxxr.sharide.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.maps.model.LatLng
import com.kxxr.sharide.R

fun openGoogleMapsNavigation(
    context: Context,
    pickup: LatLng,
    stop: LatLng?,
    destination: LatLng
) {
    val waypoints = stop?.let { "|${it.latitude},${it.longitude}" } ?: ""
    val baseUrl = context.getString(R.string.google_map_navigation_url)
    val packageUrl = context.getString(R.string.google_map_navigation_package)
    val uri = Uri.parse(baseUrl +
            "&origin=${pickup.latitude},${pickup.longitude}" +
            "&destination=${destination.latitude},${destination.longitude}" +
            "&waypoints=$waypoints" +
            "&travelmode=driving")

    // Ensure Google Maps opens
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage(packageUrl)
    }

    context.startActivity(intent)
}
