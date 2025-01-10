package com.restaurantrecommender

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import android.location.Geocoder

class OpenMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_map)

        // Configure OSMDroid
        Configuration.getInstance().load(applicationContext, applicationContext.getSharedPreferences("osm_prefs", MODE_PRIVATE))

        // Initialize the MapView
        mapView = findViewById(R.id.open_map)
        mapView.setMultiTouchControls(true)

        // Get data from intent
        val name = intent.getStringExtra("name") ?: "Location"
        val address = intent.getStringExtra("address") ?: ""

        // Use Geocoder to convert address to latitude and longitude
        val geocoder = Geocoder(this, Locale.getDefault())
        val location = geocoder.getFromLocationName(address, 1)
        if (location != null && location.isNotEmpty()) {
            val latitude = location[0].latitude
            val longitude = location[0].longitude
            Log.d("OpenMapActivity", "Geocoded Address: $address -> ($latitude, $longitude)")

            // Set map position and marker
            val locationPoint = GeoPoint(latitude, longitude)
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(locationPoint)

            val marker = Marker(mapView).apply {
                position = locationPoint
                title = name
            }
            mapView.overlays.add(marker)
        } else {
            Log.e("OpenMapActivity", "Geocoding failed for address: $address")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach() // Clean up resources
    }
}
