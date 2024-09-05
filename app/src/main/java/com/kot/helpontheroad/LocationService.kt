package com.kot.helpontheroad

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class LocationService(val context: Context) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        requestLocationUpdates()
    }

    private fun requestLocationUpdates() {
        // Verifică dacă permisiunile pentru locație sunt acordate
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Dacă permisiunile nu sunt acordate, poți cere permisiuni
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // Interval de 10 secunde
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // Declanșează descărcarea automată a serviciilor la fiecare actualizare de locație
                    fetchServicesForLocation(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun fetchServicesForLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val radius = 1000.0 // Raza în metri

        // Creează o instanță a ServiceRepository și apelează API-ul
        val serviceRepository = ServiceRepository(context)
        serviceRepository.getNearbyServicesFromAPI(latitude, longitude, radius)
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
