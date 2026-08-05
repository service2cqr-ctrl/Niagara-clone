package com.niagaraclone.launcher.data

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class WeatherData(
    val temperatureCelsius: Float,
    val weatherCode: Int,
    val cityName: String
) {
    val conditionIcon: String
        get() = when (weatherCode) {
            0 -> "☀️" // Clear sky
            1, 2, 3 -> "🌤️" // Partly cloudy
            45, 48 -> "🌫️" // Fog
            51, 53, 55, 61, 63, 65 -> "🌧️" // Rain
            71, 73, 75 -> "❄️" // Snow
            95, 96, 99 -> "⛈️" // Thunderstorm
            else -> "🌡️"
        }
}

class WeatherRepository(private val context: Context) {

    suspend fun fetchLiveWeather(lat: Double, lon: Double): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val currentWeather = json.getJSONObject("current_weather")

                val temp = currentWeather.getDouble("temperature").toFloat()
                val code = currentWeather.getInt("weathercode")
                val city = getCityNameFromCoords(lat, lon) ?: "Current Location"

                WeatherData(
                    temperatureCelsius = temp,
                    weatherCode = code,
                    cityName = city
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getCityNameFromCoords(lat: Double, lon: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
