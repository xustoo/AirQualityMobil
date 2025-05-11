package com.example.airqualitymobil.data.repository

import android.util.Log
import com.example.airqualitymobil.data.AirQualityData
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AirQualityRepository {
    private val databaseInstanceUrl = "https://airqualityceng318-default-rtdb.europe-west1.firebasedatabase.app/"
    // Firebase instance with your database URL
    private val database = FirebaseDatabase.getInstance(databaseInstanceUrl)

    // Reference to the "test" node
    private val airQualityRef = database.getReference("test")

    init {
        Log.d("FirebaseRepo", "Repository initialized with URL: $databaseInstanceUrl")
        Log.d("FirebaseRepo", "Reference path: test")

        // Remove the problematic setLogLevel command
        // Firebase offline persistence should be set only in Application class
    }

    // Manual parsing function for debugging
    private fun parseAirQualityData(snapshot: DataSnapshot): AirQualityData? {
        try {
            val json = JSONObject(snapshot.value.toString())
            Log.d("FirebaseRepo", "Manual parsing JSON: $json")

            return AirQualityData(
                altitudeValue = json.optDouble("altitudeValue", 0.0),
                co2Value = json.optInt("co2Value", 0),
                deviceName = json.optString("deviceName", ""),
                humValue = json.optDouble("humValue", 0.0),
                pressureValue = json.optDouble("pressureValue", 0.0),
                tempValue = json.optDouble("tempValue", 0.0),
                time = json.optString("time", ""),
                tvocValue = json.optInt("tvocValue", 0)
            )
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Manual parsing error", e)
            return null
        }
    }

    // One-time fetch with Coroutines
    suspend fun fetchLatestAirQualityOnce(): Result<AirQualityData?> {
        return try {
            Log.d("FirebaseRepo", "Attempting to fetch data once")

            // Suspend function with explicit callback for more control
            val result = suspendCoroutine<DataSnapshot> { continuation ->
                airQualityRef.get().addOnSuccessListener { snapshot ->
                    continuation.resume(snapshot)
                }.addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
            }

            Log.d("FirebaseRepo", "Data snapshot exists: ${result.exists()}")
            Log.d("FirebaseRepo", "Data snapshot value: ${result.value}")

            if (result.exists()) {
                try {
                    // First try normal Firebase deserialization
                    val data = result.getValue(AirQualityData::class.java)

                    if (data != null) {
                        Log.d("FirebaseRepo", "Data fetched successfully: $data")
                        Result.success(data)
                    } else {
                        // If normal deserialization fails, try manual parsing
                        Log.w("FirebaseRepo", "Firebase deserialization returned null, trying manual parsing")
                        val manualData = parseAirQualityData(result)

                        if (manualData != null) {
                            Log.d("FirebaseRepo", "Manual parsing successful: $manualData")
                            Result.success(manualData)
                        } else {
                            Log.e("FirebaseRepo", "Both deserialization methods failed")
                            Result.failure(Exception("Failed to parse data"))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FirebaseRepo", "Error parsing data", e)

                    // Try manual parsing as fallback
                    val manualData = parseAirQualityData(result)
                    if (manualData != null) {
                        Log.d("FirebaseRepo", "Manual parsing successful after exception: $manualData")
                        Result.success(manualData)
                    } else {
                        Result.failure(e)
                    }
                }
            } else {
                Log.d("FirebaseRepo", "No data exists at the specified path")
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error fetching data once", e)
            Result.failure(e)
        }
    }

    // Continuous listening with Kotlin Flow
    fun listenForAirQualityChanges(): Flow<Result<AirQualityData?>> = callbackFlow {
        Log.d("FirebaseRepo", "Starting to listen for changes")

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseRepo", "Data changed. Exists: ${snapshot.exists()}")
                Log.d("FirebaseRepo", "Data snapshot value: ${snapshot.value}")

                if (snapshot.exists()) {
                    try {
                        // First try normal Firebase deserialization
                        val data = snapshot.getValue(AirQualityData::class.java)

                        if (data != null) {
                            Log.d("FirebaseRepo", "Data parsed: $data")
                            trySend(Result.success(data))
                        } else {
                            // If normal deserialization fails, try manual parsing
                            Log.w("FirebaseRepo", "Firebase deserialization returned null, trying manual parsing")
                            val manualData = parseAirQualityData(snapshot)

                            if (manualData != null) {
                                Log.d("FirebaseRepo", "Manual parsing successful: $manualData")
                                trySend(Result.success(manualData))
                            } else {
                                Log.e("FirebaseRepo", "Both deserialization methods failed")
                                trySend(Result.failure(Exception("Failed to parse data")))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("FirebaseRepo", "Error parsing data", e)

                        // Try manual parsing as fallback
                        val manualData = parseAirQualityData(snapshot)
                        if (manualData != null) {
                            Log.d("FirebaseRepo", "Manual parsing successful after exception: $manualData")
                            trySend(Result.success(manualData))
                        } else {
                            trySend(Result.failure(e))
                        }
                    }
                } else {
                    Log.d("FirebaseRepo", "No data at specified path")
                    trySend(Result.success(null))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseRepo", "Error listening to data", error.toException())
                trySend(Result.failure(error.toException()))
                close(error.toException())
            }
        }

        // Start listening
        airQualityRef.addValueEventListener(valueEventListener)

        // Remove listener when Flow is cancelled
        awaitClose {
            Log.d("FirebaseRepo", "Removing listener as Flow is closing.")
            airQualityRef.removeEventListener(valueEventListener)
        }
    }

    // Function to check available paths in Firebase
    suspend fun checkAvailablePaths(): List<String> {
        val paths = mutableListOf<String>()
        try {
            // Get root node
            val rootSnapshot = database.reference.get().await()
            Log.d("FirebaseRepo", "Root snapshot exists: ${rootSnapshot.exists()}")
            Log.d("FirebaseRepo", "Root snapshot value: ${rootSnapshot.value}")

            // List child nodes
            rootSnapshot.children.forEach { child ->
                val key = child.key ?: "unknown"
                paths.add(key)
                Log.d("FirebaseRepo", "Found path: $key, value type: ${child.value?.javaClass?.simpleName}")

                // Try to get first level children too, for deeper debugging
                child.children.forEach { grandchild ->
                    val grandchildKey = grandchild.key ?: "unknown"
                    paths.add("$key/$grandchildKey")
                    Log.d("FirebaseRepo", "Found sub-path: $key/$grandchildKey, value type: ${grandchild.value?.javaClass?.simpleName}")
                }
            }

            Log.d("FirebaseRepo", "Available paths: ${paths.joinToString()}")
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error checking paths", e)
        }
        return paths
    }
}