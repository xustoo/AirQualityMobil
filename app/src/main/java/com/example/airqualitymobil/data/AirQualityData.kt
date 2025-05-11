package com.example.airqualitymobil.data

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

@IgnoreExtraProperties
data class AirQualityData(
    @get:PropertyName("altitudeValue") @set:PropertyName("altitudeValue")
    var altitudeValue: Double = 0.0,

    @get:PropertyName("co2Value") @set:PropertyName("co2Value")
    var co2Value: Int = 0,

    @get:PropertyName("deviceName") @set:PropertyName("deviceName")
    var deviceName: String = "",

    @get:PropertyName("humValue") @set:PropertyName("humValue")
    var humValue: Double = 0.0,

    @get:PropertyName("pressureValue") @set:PropertyName("pressureValue")
    var pressureValue: Double = 0.0,

    @get:PropertyName("tempValue") @set:PropertyName("tempValue")
    var tempValue: Double = 0.0,

    @get:PropertyName("time") @set:PropertyName("time")
    var time: String = "",

    @get:PropertyName("tvocValue") @set:PropertyName("tvocValue")
    var tvocValue: Int = 0
) {
    // No-argument constructor required for Firebase deserialization
    constructor() : this(0.0, 0, "", 0.0, 0.0, 0.0, "", 0)

    override fun toString(): String {
        return "AirQualityData(deviceName='$deviceName', time='$time', " +
                "tempValue=$tempValue, humValue=$humValue, co2Value=$co2Value, " +
                "tvocValue=$tvocValue, pressureValue=$pressureValue, altitudeValue=$altitudeValue)"
    }
}