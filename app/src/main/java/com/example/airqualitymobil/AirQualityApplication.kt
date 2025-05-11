package com.example.airqualitymobil

import android.app.Application
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.FirebaseApp

class AirQualityApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            // Initialize Firebase first
            FirebaseApp.initializeApp(this)
            Log.d("AirQualityApp", "Firebase app initialized successfully")

            // Then enable persistence
            val database = FirebaseDatabase.getInstance("https://airqualityceng318-default-rtdb.europe-west1.firebasedatabase.app/")
            database.setPersistenceEnabled(true)
            Log.d("AirQualityApp", "Firebase initialized successfully with persistence enabled")

            // Keep database connection active for faster data access
            database.goOnline()
        } catch (e: Exception) {
            // This might throw if the app is restarted or if persistence was already enabled
            Log.w("AirQualityApp", "Firebase setup error: ${e.message}")
        }
    }
}