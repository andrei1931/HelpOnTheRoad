package com.kot.helpontheroad

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket

class MainActivity : AppCompatActivity() {

    private lateinit var sosButton: Button
    private lateinit var servicesButton: Button

    private lateinit var wifiP2pManager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: BroadcastReceiver
    private lateinit var intentFilter: IntentFilter
    private lateinit var dbHelper: DatabaseHelper

    private var currentLocation: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sosButton = findViewById(R.id.sos_button)
        servicesButton = findViewById(R.id.services_button)

        sosButton.setOnClickListener { sendSOS() }
        servicesButton.setOnClickListener { showServices() }
        dbHelper = DatabaseHelper(this)

        // Log all services in the database
        dbHelper.logAllServices()

        wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(this, mainLooper, null)

        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }

        receiver = WiFiDirectBroadcastReceiver(wifiP2pManager, channel, this)

        startLocationUpdates()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    private fun startLocationUpdates() {
        // Verifică permisiunea pentru locație
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Dacă permisiunea nu este acordată, solicită-o de la utilizator
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            // Dacă permisiunea este deja acordată, începe actualizarea locației
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Parametrii pentru cererea de locație
            val minTime: Long = 10000 // 10 secunde
            val minDistance: Float = 50f // 50 metri

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    // Locația s-a schimbat
                    currentLocation = "${location.latitude}, ${location.longitude}"
                    Log.d("Location Update", "Locația curentă: $currentLocation")

                    // Apelează funcția pentru a descărca serviciile în baza de date
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val radius = 1000.0 // De exemplu, 1000 de metri

                    val serviceRepository = ServiceRepository(this@MainActivity)
                    serviceRepository.getNearbyServicesFromAPI(latitude, longitude, radius)
                }

                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                    // Deprecated în noile versiuni, nu mai trebuie implementat
                }

                override fun onProviderEnabled(provider: String) {
                    // GPS-ul a fost activat
                    Log.d("Location", "GPS activat")
                    Toast.makeText(applicationContext, "GPS activat", Toast.LENGTH_SHORT).show()
                }

                override fun onProviderDisabled(provider: String) {
                    // GPS-ul a fost dezactivat
                    Log.e("Location", "GPS dezactivat")
                    Toast.makeText(applicationContext, "GPS dezactivat. Activează-l pentru a primi locații.", Toast.LENGTH_LONG).show()
                    // Aici poți redirecționa utilizatorul către setările de locație ale dispozitivului
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            }

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTime, minDistance,
                locationListener
            )
        }
    }

    private fun sendSOS() {
        discoverPeers()
    }

    private fun discoverPeers() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Toast.makeText(this@MainActivity, "Discovery Initiated", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(this@MainActivity, "Discovery Failed: $reason", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    fun connectToPeer(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.NEARBY_WIFI_DEVICES
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
        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Connection successful, proceed to send message
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(this@MainActivity, "Connect failed. Retry.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun sendSosMessage(info: WifiP2pInfo) {
        Thread {
            val host: InetAddress = info.groupOwnerAddress
            val socket = Socket(host, 8888)
            val stream: OutputStream = socket.getOutputStream()
            stream.write("SOS! I need help. My location is: $currentLocation".toByteArray())
            stream.close()
            socket.close()
        }.start()
    }

    private fun showServices() {
        val intent = Intent(this, ServicesActivity::class.java)
        startActivity(intent)
    }
}
