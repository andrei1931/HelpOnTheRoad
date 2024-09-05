package com.kot.helpontheroad

import android.content.Context
import android.util.Log
import okhttp3.*
import java.io.IOException
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray

class ServiceRepository(val context: Context) {

    fun getNearbyServicesFromAPI(latitude: Double, longitude: Double, radius: Double) {
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=service&lat=$latitude&lon=$longitude&radius=$radius"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API", "Failed to fetch services: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("R", "Unexpected code $response")
                } else {
                    response.body?.let {
                        val responseData = it.string()
                        Log.d("API", "Response: $responseData")  // Verifică logul pentru răspunsul API-ului
                        val services = parseJson(responseData)
                        saveServicesToDatabase(services)
                    }
                }
            }
        })
    }

    // Funcție de parsare a răspunsului JSON
    private fun parseJson(responseData: String): List<Service> {
        val services = mutableListOf<Service>()

        // Parse the response as a JSON Array
        val jsonArray = JSONArray(responseData)

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)

            // Check if the "name" field exists and is not empty
            val name = jsonObject.optString("name", "")
            if (name.isNotEmpty()) {
                // Extract the necessary fields with appropriate types
                val id = jsonObject.optInt("place_id", 0)
                val latitude = jsonObject.optDouble("lat", 0.0)
                val longitude = jsonObject.optDouble("lon", 0.0)
                val type = jsonObject.optString("class", "unknown") // Default type to "unknown" if not present

                // Create a Service object and add it to the list
                val service = Service(id, name, type, latitude, longitude)
                services.add(service)
            }
        }

        return services
    }



    // Funcție de salvare a serviciilor în baza de date
    private fun saveServicesToDatabase(services: List<Service>) {
        val dbHelper = DatabaseHelper(context)
        for (service in services) {
            val result = dbHelper.insertService(service.name, service.type, service.latitude, service.longitude)
            if (result != -1L) {
                Log.d("Database", "Inserted service: ${service.name}")
            } else {
                Log.e("Database", "Failed to insert service: ${service.name}")
            }
        }
    }
}
