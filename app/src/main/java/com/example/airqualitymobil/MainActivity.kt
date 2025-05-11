package com.example.airqualitymobil.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.airqualitymobil.prediction.AirQualityPredictionManager
import com.example.airqualitymobil.viewmodel.AirQualityViewModel
import com.example.airqualitymobil.data.AirQualityData
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")
        setContent {
            val viewModel: AirQualityViewModel = viewModel()
            AirQualityAppTheme {
                AirQualityScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirQualityScreen(viewModel: AirQualityViewModel) {
    val airQualityData by viewModel.latestAirQuality.observeAsState()
    val errorMessage by viewModel.errorMessages.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(initial = true)
    val availablePaths by viewModel.availablePaths.observeAsState(initial = emptyList())
    val prediction by viewModel.prediction.observeAsState()

    // Debug logging to see what states we're getting
    LaunchedEffect(airQualityData, errorMessage, isLoading) {
        Log.d("AirQualityScreen", "State update - Data: $airQualityData, Error: $errorMessage, Loading: $isLoading")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hava Kalitesi Takip") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    isLoading == true -> {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text(
                            text = "Veriler yükleniyor...",
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    errorMessage != null -> {
                        Text(
                            text = "Hata: $errorMessage",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (availablePaths.isNotEmpty()) {
                            Text(
                                text = "Firebase'de bulunan kök düğümler:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )

                            availablePaths.forEach { path ->
                                Text(
                                    text = "- $path",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Button(onClick = { viewModel.fetchLatestAirQualityOnce() }) {
                                Text("Verileri Yenile")
                            }

                            Button(onClick = { viewModel.checkAvailablePaths() }) {
                                Text("Firebase Yapısını Kontrol Et")
                            }
                        }
                    }
                    airQualityData != null -> {
                        // Ana veri kartı
                        AirQualityDataCard(data = airQualityData!!, onRefresh = { viewModel.fetchLatestAirQualityOnce() })

                        // Tahmin kartı - veri varsa göster
                        prediction?.let { predictionData ->
                            AirQualityPredictionCard(prediction = predictionData)
                        }
                    }
                    else -> {
                        Text(
                            text = "Veri bulunamadı",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        if (availablePaths.isNotEmpty()) {
                            Text(
                                text = "Firebase'de bulunan kök düğümler:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )

                            availablePaths.forEach { path ->
                                Text(
                                    text = "- $path",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Button(onClick = { viewModel.fetchLatestAirQualityOnce() }) {
                                Text("Verileri Yenile")
                            }

                            Button(onClick = { viewModel.checkAvailablePaths() }) {
                                Text("Firebase Yapısını Kontrol Et")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AirQualityDataCard(data: AirQualityData, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = data.deviceName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Divider(modifier = Modifier.padding(vertical = 4.dp))

            DataRow(label = "Zaman:", value = data.time)
            DataRow(label = "Sıcaklık:", value = "${String.format("%.1f", data.tempValue)} °C")
            DataRow(label = "Nem:", value = "${String.format("%.1f", data.humValue)} %")
            DataRow(label = "CO2 Değeri:", value = "${data.co2Value} ppm")
            DataRow(label = "TVOC Değeri:", value = "${data.tvocValue} ppb")
            DataRow(label = "Basınç:", value = "${String.format("%.2f", data.pressureValue)} hPa")
            DataRow(label = "Rakım:", value = "${String.format("%.2f", data.altitudeValue)} m")

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            val airQualityStatus = getAirQualityStatus(data)
            val statusColor = getAirQualityStatusColor(airQualityStatus)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hava Kalitesi: $airQualityStatus",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Button(
                onClick = onRefresh,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Yenile")
            }
        }
    }
}

@Composable
fun AirQualityPredictionCard(prediction: AirQualityPredictionManager.AirQualityPrediction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Tahmin",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = prediction.summary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (prediction.details.isNotEmpty()) {
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Detaylar:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = prediction.details,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

fun getAirQualityStatus(data: AirQualityData): String {
    return when {
        data.co2Value > 1200 || data.tvocValue > 2000 -> "Kötü"
        data.co2Value > 800 || data.tvocValue > 1000 -> "Orta"
        else -> "İyi"
    }
}

fun getAirQualityStatusColor(status: String): Color {
    return when (status) {
        "İyi" -> Color(0xFF4CAF50)  // Yeşil
        "Orta" -> Color(0xFFFFC107)  // Sarı
        "Kötü" -> Color(0xFFF44336)  // Kırmızı
        else -> Color.Gray
    }
}

@Composable
fun AirQualityAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF1976D2),
            onPrimary = Color.White,
            secondary = Color(0xFF03A9F4),
            surfaceVariant = Color(0xFFF5F5F5)
        ),
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val sampleData = AirQualityData(
        deviceName = "Hava Kalite Sensörü",
        time = "2025-05-14 15:30:45",
        humValue = 45.5,
        tempValue = 26.8,
        co2Value = 430,
        tvocValue = 120,
        pressureValue = 1013.2,
        altitudeValue = 340.5
    )

    AirQualityAppTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AirQualityDataCard(data = sampleData, onRefresh = {})
            Spacer(modifier = Modifier.height(16.dp))
            AirQualityPredictionCard(
                prediction = AirQualityPredictionManager.AirQualityPrediction(
                    summary = "Hava kalitesi önümüzdeki 6 saat içinde stabil kalacak.",
                    details = "CO2 seviyesi normal aralıkta. TVOC değerleri düşük seyretmekte."
                )
            )
        }
    }
}