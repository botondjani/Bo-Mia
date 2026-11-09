package app.tend.data

import android.content.Context
import app.tend.core.LiveWeather
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The camp's weather from Open-Meteo (free, no account): finding a city by name, and the weather
 * there now. Only the city's coordinates are sent, rounded to about a kilometre; never the phone's
 * location.
 */
object WeatherService {
    data class Place(val name: String, val area: String, val country: String, val countryCode: String, val lat: Double, val lon: Double) {
        val label: String get() = listOf(name, area.takeIf { it.isNotBlank() && it != name }, country).filterNotNull().filter { it.isNotBlank() }.joinToString(", ")
    }

    private fun get(url: String): String? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000; conn.readTimeout = 8_000
        conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }.getOrNull()

    /** Cities matching [query] (a few, biggest first as the service ranks them); empty when offline. */
    fun search(query: String): List<Place> {
        if (query.trim().length < 2) return emptyList()
        val body = get("https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(query.trim(), "UTF-8")}&count=6&language=en&format=json") ?: return emptyList()
        return runCatching {
            val a = JSONObject(body).optJSONArray("results") ?: return emptyList()
            (0 until a.length()).map { a.getJSONObject(it) }.map { o ->
                Place(o.optString("name"), o.optString("admin1"), o.optString("country"), o.optString("country_code"), o.optDouble("latitude"), o.optDouble("longitude"))
            }.filter { !it.lat.isNaN() && !it.lon.isNaN() }
        }.getOrDefault(emptyList())
    }

    /** Fetches the weather for the chosen city when the last one is over an hour old; true when it changed what's shown. */
    fun refresh(c: Context, force: Boolean = false): Boolean {
        val store = TendStore(c)
        val place = store.weatherPlace ?: return false
        if (!force && System.currentTimeMillis() - LiveWeather.fetchedAt < 60 * 60_000L) return false
        val lat = "%.2f".format(java.util.Locale.ROOT, place.lat)
        val lon = "%.2f".format(java.util.Locale.ROOT, place.lon)
        val body = get("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=weather_code&daily=sunrise,sunset&timezone=auto&forecast_days=1") ?: return false
        return runCatching {
            val o = JSONObject(body)
            val code = o.getJSONObject("current").getInt("weather_code")
            val daily = o.optJSONObject("daily")
            fun hourOf(key: String): Float? = daily?.optJSONArray(key)?.optString(0)?.substringAfter('T')?.split(':')?.takeIf { it.size >= 2 }
                ?.let { (h, m) -> h.toFloatOrNull()?.plus((m.toFloatOrNull() ?: 0f) / 60f) }
            val before = LiveWeather.current
            store.keepWeather(LiveWeather.fromWmo(code), hourOf("sunrise") ?: 6.5f, hourOf("sunset") ?: 18.6f)
            before != LiveWeather.current
        }.getOrDefault(false)
    }
}
