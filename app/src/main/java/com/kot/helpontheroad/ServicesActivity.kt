package com.kot.helpontheroad

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener

class ServicesActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var servicesListView: ListView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationRequestCode = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        dbHelper = DatabaseHelper(this)
        servicesListView = findViewById(R.id.services_list_view)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationRequestCode)
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener<Location> { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                val radius = 3.1 // Radius de căutare în grade (aproximativ 10 km)
                Log.d("ServicesActivity", "Current location: Lat: $latitude, Lon: $longitude")
                displayNearbyServices(latitude, longitude, radius)
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayNearbyServices(latitude: Double, longitude: Double, radius: Double) {
        val services = dbHelper.getServicesNearby(latitude, longitude, radius)

        // Log services retrieved from the database
        for (service in services) {
            Log.d("ServicesActivity", "Service Found: ${service.name} - ${service.type} at Lat: ${service.latitude}, Lon: ${service.longitude}")
        }

        val serviceNames = services.map { "${it.name} - ${it.type} - ${it.latitude}, ${it.longitude}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, serviceNames)
        servicesListView.adapter = adapter
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
