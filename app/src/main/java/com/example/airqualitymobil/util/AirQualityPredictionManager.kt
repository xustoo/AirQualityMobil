package com.example.airqualitymobil.prediction

import android.util.Log
import com.example.airqualitymobil.data.AirQualityData
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

/**
 * Hava kalitesi verilerini kullanarak basit tahminler yapan sınıf.
 * Belirli bir sayıda geçmiş ölçümü saklar ve eğilimleri analiz eder.
 */
class AirQualityPredictionManager(private val maxHistorySize: Int = 10) {

    // Geçmiş ölçüm verileri
    private val history = ArrayList<AirQualityData>()

    /**
     * Tahmin sonucu veri sınıfı
     */
    data class AirQualityPrediction(
        val summary: String,
        val details: String = ""
    )

    /**
     * Yeni bir ölçüm ekler ve gerekirse en eski veriyi çıkarır
     */
    fun addMeasurement(data: AirQualityData) {
        history.add(data)
        // Maksimum boyutu kontrol et ve gerekirse eski veriyi kaldır
        if (history.size > maxHistorySize) {
            history.removeAt(0)
        }
        Log.d("PredictionManager", "Added new measurement. History size: ${history.size}")
    }

    /**
     * Mevcut verilere göre tahmin oluşturur
     */
    fun getPrediction(): AirQualityPrediction {
        if (history.isEmpty()) {
            return AirQualityPrediction("Henüz tahmin için yeterli veri bulunmuyor.")
        }

        // Son veriyi al
        val latestData = history.last()

        // Yeterli geçmiş veri yoksa basit durum raporu ver
        if (history.size < 2) {
            return createStatusReport(latestData)
        }

        // Eğilim analizi için yeterli veri varsa
        return analyzeTrend()
    }

    /**
     * Son veri durumuna göre basit bir durum raporu
     */
    private fun createStatusReport(data: AirQualityData): AirQualityPrediction {
        val co2Status = when {
            data.co2Value < 800 -> "iyi"
            data.co2Value < 1500 -> "kabul edilebilir"
            else -> "yüksek"
        }

        val tvocStatus = when {
            data.tvocValue < 500 -> "düşük"
            data.tvocValue < 1500 -> "orta"
            else -> "yüksek"
        }

        val tempStatus = when {
            data.tempValue < 18 -> "serin"
            data.tempValue < 24 -> "ılıman"
            else -> "sıcak"
        }

        val humStatus = when {
            data.humValue < 30 -> "kuru"
            data.humValue < 60 -> "konforlu"
            else -> "nemli"
        }

        val summary = "Mevcut hava kalitesi: CO2 seviyesi $co2Status, VOC seviyesi $tvocStatus."
        val details = "Ortam sıcaklığı $tempStatus (${String.format("%.1f", data.tempValue)}°C)" +
                " ve nem seviyesi $humStatus (${String.format("%.1f", data.humValue)}%)." +
                " CO2: ${data.co2Value} ppm, TVOC: ${data.tvocValue} ppb" +
                " değerlerindedir. Basınç: ${String.format("%.1f", data.pressureValue)} hPa."

        return AirQualityPrediction(summary, details)
    }

    /**
     * Geçmiş verilere bakarak eğilim analizi yapar
     */
    private fun analyzeTrend(): AirQualityPrediction {
        // En son ve daha önceki veriyi karşılaştır
        val latest = history.last()
        val previous = history[history.size - 2]

        // CO2 değişimi analizi
        val co2Change = latest.co2Value - previous.co2Value
        val co2Trend = when {
            abs(co2Change) < 50 -> "stabil"
            co2Change > 0 -> "artıyor"
            else -> "düşüyor"
        }

        // TVOC değişimi analizi
        val tvocChange = latest.tvocValue - previous.tvocValue
        val tvocTrend = when {
            abs(tvocChange) < 50 -> "stabil"
            tvocChange > 0 -> "artıyor"
            else -> "düşüyor"
        }

        // Sıcaklık değişimi
        val tempChange = latest.tempValue - previous.tempValue
        val tempTrend = when {
            abs(tempChange) < 0.5 -> "stabil"
            tempChange > 0 -> "artıyor"
            else -> "düşüyor"
        }

        // Nem değişimi
        val humChange = latest.humValue - previous.humValue
        val humTrend = when {
            abs(humChange) < 2.0 -> "stabil"
            humChange > 0 -> "artıyor"
            else -> "düşüyor"
        }

        // Uzun süreli trend analizi (eğer yeterli veri varsa)
        var longTermAnalysis = ""
        if (history.size >= 5) {
            val oldData = history[0]
            // CO2 uzun vadeli değişim yüzdesi
            val co2LongChange = ((latest.co2Value - oldData.co2Value) / oldData.co2Value.toDouble()) * 100

            longTermAnalysis = "Son ${history.size} ölçüm içinde " +
                    "CO2 seviyesi ${String.format("%.1f", co2LongChange)}% " +
                    if (co2LongChange > 5) "artış" else if (co2LongChange < -5) "düşüş" else "değişim" +
                            " gösterdi. "
        }

        val summary = "CO2 seviyesi $co2Trend ve TVOC seviyesi $tvocTrend."

        val details = "Mevcut CO2: ${latest.co2Value} ppm (${if (co2Change > 0) "+" else ""}$co2Change ppm), " +
                "TVOC: ${latest.tvocValue} ppb (${if (tvocChange > 0) "+" else ""}$tvocChange ppb). " +
                "Sıcaklık $tempTrend (${String.format("%.1f", latest.tempValue)}°C), " +
                "nem $humTrend (${String.format("%.1f", latest.humValue)}%). " +
                longTermAnalysis

        return AirQualityPrediction(summary, details)
    }

    /**
     * Geçmiş verilerini temizler
     */
    fun clearHistory() {
        history.clear()
        Log.d("PredictionManager", "History cleared")
    }
}