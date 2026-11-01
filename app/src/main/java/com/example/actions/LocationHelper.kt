package com.example.actions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.util.Log

class LocationHelper(private val context: Context) {
    private val TAG = "LocationHelper"

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): Location? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        try {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (gpsLocation != null) return gpsLocation

            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (networkLocation != null) return networkLocation
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
        }
        return null
    }

    fun openMapSearch(query: String) {
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webFallback = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(webFallback)
        }
    }
}
