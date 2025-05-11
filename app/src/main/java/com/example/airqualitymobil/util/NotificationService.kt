package com.example.airqualitymobil.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.airqualitymobil.R
import com.example.airqualitymobil.data.AirQualityData
import com.example.airqualitymobil.ui.MainActivity

/**
 * Hava kalitesi uyarıları için bildirim yönetimini sağlayan servis.
 */
class NotificationService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "air_quality_alerts"
        const val NOTIFICATION_ID_CO2 = 1001
        const val NOTIFICATION_ID_TVOC = 1002

        // Eşik değerleri
        const val CO2_WARNING_THRESHOLD = 1000  // ppm
        const val CO2_DANGER_THRESHOLD = 2000   // ppm
        const val TVOC_WARNING_THRESHOLD = 1000 // ppb
        const val TVOC_DANGER_THRESHOLD = 2000  // ppb
    }

    init {
        createNotificationChannel()
    }

    /**
     * Bildirim kanalı oluşturur (Android 8.0+ için gerekli)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hava Kalitesi Uyarıları"
            val descriptionText = "Hava kalitesi ile ilgili önemli uyarılar"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d("NotificationService", "Notification channel created")
        }
    }

    /**
     * Hava kalitesi verilerini kontrol eder ve gerekirse bildirim gösterir
     */
    fun checkAndNotify(data: AirQualityData) {
        checkCO2Level(data.co2Value)
        checkTVOCLevel(data.tvocValue)
    }

    /**
     * CO2 seviyelerini kontrol eder ve gerekirse bildirim gösterir
     */
    private fun checkCO2Level(co2Value: Int) {
        when {
            co2Value >= CO2_DANGER_THRESHOLD -> {
                showNotification(
                    NOTIFICATION_ID_CO2,
                    "CO2 Seviyesi Tehlikeli",
                    "CO2 seviyesi $co2Value ppm ile tehlikeli seviyede. Lütfen odayı havalandırın."
                )
                Log.w("NotificationService", "Dangerous CO2 level detected: $co2Value ppm")
            }
            co2Value >= CO2_WARNING_THRESHOLD -> {
                showNotification(
                    NOTIFICATION_ID_CO2,
                    "CO2 Seviyesi Yüksek",
                    "CO2 seviyesi $co2Value ppm ile yüksek. Odayı havalandırmanız önerilir."
                )
                Log.w("NotificationService", "Warning CO2 level detected: $co2Value ppm")
            }
        }
    }

    /**
     * TVOC seviyelerini kontrol eder ve gerekirse bildirim gösterir
     */
    private fun checkTVOCLevel(tvocValue: Int) {
        when {
            tvocValue >= TVOC_DANGER_THRESHOLD -> {
                showNotification(
                    NOTIFICATION_ID_TVOC,
                    "TVOC Seviyesi Tehlikeli",
                    "TVOC seviyesi $tvocValue ppb ile tehlikeli seviyede. Lütfen odayı havalandırın."
                )
                Log.w("NotificationService", "Dangerous TVOC level detected: $tvocValue ppb")
            }
            tvocValue >= TVOC_WARNING_THRESHOLD -> {
                showNotification(
                    NOTIFICATION_ID_TVOC,
                    "TVOC Seviyesi Yüksek",
                    "TVOC seviyesi $tvocValue ppb ile yüksek. Odayı havalandırmanız önerilir."
                )
                Log.w("NotificationService", "Warning TVOC level detected: $tvocValue ppb")
            }
        }
    }

    /**
     * Belirtilen bilgilerle bir bildirim gösterir
     */
    private fun showNotification(notificationId: Int, title: String, content: String) {
        try {
            // Ana aktiviteye gidecek intent
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Bildirim oluştur
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.img) // Bu ikonu res/drawable klasörünüze eklemelisiniz
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Bildirim göster
            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS
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
                notify(notificationId, builder.build())
                Log.d("NotificationService", "Notification shown: $title")
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error showing notification", e)
        }
    }
}