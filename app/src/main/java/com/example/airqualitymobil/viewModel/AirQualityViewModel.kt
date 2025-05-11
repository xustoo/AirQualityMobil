package com.example.airqualitymobil.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.airqualitymobil.data.repository.AirQualityRepository
import com.example.airqualitymobil.data.AirQualityData
import com.example.airqualitymobil.notification.NotificationService
import com.example.airqualitymobil.prediction.AirQualityPredictionManager
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AirQualityViewModel(application: Application) : AndroidViewModel(application) {

    // Repository'yi kullan
    private val repository = AirQualityRepository()

    // Bildirim servisini oluştur
    private val notificationService = NotificationService(application.applicationContext)

    // Tahmin yöneticisi
    private val predictionManager = AirQualityPredictionManager(maxHistorySize = 10)

    private val _latestAirQuality = MutableLiveData<AirQualityData?>()
    val latestAirQuality: LiveData<AirQualityData?> = _latestAirQuality

    private val _errorMessages = MutableLiveData<String?>()
    val errorMessages: LiveData<String?> = _errorMessages

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _availablePaths = MutableLiveData<List<String>>()
    val availablePaths: LiveData<List<String>> = _availablePaths

    // Tahmin için LiveData
    private val _prediction = MutableLiveData<AirQualityPredictionManager.AirQualityPrediction>()
    val prediction: LiveData<AirQualityPredictionManager.AirQualityPrediction> = _prediction

    init {
        Log.d("AirQualityViewModel", "ViewModel initialized")
        checkAvailablePaths() // Firebase veri yollarını kontrol et
        fetchLatestAirQualityOnce() // Önce tek seferlik veri çek
        startListeningForAirQualityChanges() // Sonra dinlemeye başla
    }

    fun fetchLatestAirQualityOnce() {
        viewModelScope.launch {
            Log.d("AirQualityViewModel", "Fetching data once")
            _isLoading.value = true
            _errorMessages.value = null // Önceki hataları temizle

            repository.fetchLatestAirQualityOnce()
                .onSuccess { data ->
                    Log.d("AirQualityViewModel", "Data fetched successfully: $data")
                    processNewData(data)
                }
                .onFailure { error ->
                    Log.e("AirQualityViewModel", "Error fetching data", error)
                    _errorMessages.value = "Veri çekilirken hata: ${error.message}"
                }

            _isLoading.value = false
        }
    }

    fun startListeningForAirQualityChanges() {
        Log.d("AirQualityViewModel", "Starting to listen for data changes")
        _isLoading.value = true
        _errorMessages.value = null

        // Repository'den Flow kullan
        repository.listenForAirQualityChanges()
            .onEach { result ->
                _isLoading.value = false
                result.onSuccess { data ->
                    Log.d("AirQualityViewModel", "Data received: $data")
                    processNewData(data)
                }.onFailure { error ->
                    Log.e("AirQualityViewModel", "Error in data stream", error)
                    _errorMessages.value = "Veri dinlenirken hata: ${error.message}"
                }
            }
            .catch { error ->
                Log.e("AirQualityViewModel", "Flow collection error", error)
                _isLoading.value = false
                _errorMessages.value = "Veri akışında hata: ${error.message}"
            }
            .launchIn(viewModelScope)
    }

    // Yeni veri işleme metodu
    private fun processNewData(data: AirQualityData?) {
        _latestAirQuality.value = data

        if (data == null) {
            _errorMessages.value = "Veri yolunda hiç veri bulunamadı. Firebase yapısını kontrol edin."
            return
        }

        // Veri geçmişe eklenir ve tahmin güncellenir
        predictionManager.addMeasurement(data)
        _prediction.value = predictionManager.getPrediction()

        // Bildirim kontrolü
        notificationService.checkAndNotify(data)
    }

    // Firebase'deki mevcut veri yollarını kontrol et
    fun checkAvailablePaths() {
        viewModelScope.launch {
            try {
                val paths = repository.checkAvailablePaths()
                _availablePaths.value = paths
                Log.d("AirQualityViewModel", "Available paths: ${paths.joinToString()}")
            } catch (e: Exception) {
                Log.e("AirQualityViewModel", "Error checking paths", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Flow viewModelScope ile otomatik olarak iptal edilecek
        Log.d("AirQualityViewModel", "ViewModel cleared")
    }
}