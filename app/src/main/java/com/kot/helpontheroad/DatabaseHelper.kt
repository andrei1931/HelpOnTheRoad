package com.kot.helpontheroad

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.Context
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "onroad.db"
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createServicesTable = "CREATE TABLE services (id INTEGER PRIMARY KEY, name TEXT, type TEXT, latitude REAL, longitude REAL)"
        db.execSQL(createServicesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS services")
        onCreate(db)
    }

    fun getServicesNearby(latitude: Double, longitude: Double, radius: Double): List<Service> {
        val db = this.readableDatabase
        val services = mutableListOf<Service>()

        // Formulează o interogare SQL pentru a selecta serviciile în apropiere
        val query = """
            SELECT * FROM services
            WHERE ABS(latitude - ?) <= ? AND ABS(longitude - ?) <= ?
        """
        val cursor: Cursor = db.rawQuery(query, arrayOf(
            latitude.toString(), radius.toString(),
            longitude.toString(), radius.toString()
        ))

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))

                services.add(Service(id, name, type, lat, lon))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return services
    }
    fun logAllServices() {
        val db = this.readableDatabase
        val query = "SELECT * FROM services"
        val cursor: Cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                val lat = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val lon = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))

                Log.d("ServiceLog", "ID: $id, Name: $name, Type: $type, Latitude: $lat, Longitude: $lon")
            } while (cursor.moveToNext())
        } else {
            Log.d("ServiceLog", "No services found in the database.")
        }
        cursor.close()
    }
    fun insertService(name: String, type: String, latitude: Double, longitude: Double):Long {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("name", name)
            put("type", type)
            put("latitude", latitude)
            put("longitude", longitude)
        }
       return db.insert("services", null, contentValues)
    }


}
