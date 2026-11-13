package app.tend.core

import java.time.LocalDate

/** The day's weather in the camp: the same all day (fog only in the morning), different from day to day. */
enum class Weather { CLEAR, CLOUDY, RAIN, FOG, SNOW;

    /** In a few words, for a message. */
    fun say(): String = when (this) {
        CLEAR -> tr("sunny", "napos")
        CLOUDY -> tr("cloudy", "felhős")
        RAIN -> tr("rainy", "esős")
        FOG -> tr("foggy", "ködös")
        SNOW -> tr("snowy", "havas")
    }

    companion object {
        /** For [date] at [hour]: a coin thrown per day, weighted by [season]. */
        fun of(date: LocalDate, hour: Int, season: Season = Season.of(date)): Weather {
            val r = java.util.Random(date.toEpochDay() * 7919L + 17L).nextFloat()
            val morning = hour < 10
            return when (season) {
                Season.WINTER -> when { r < 0.45f -> SNOW; r < 0.6f && morning -> FOG; r < 0.75f -> CLOUDY; else -> CLEAR }
                Season.AUTUMN -> when { r < 0.3f -> RAIN; r < 0.5f && morning -> FOG; r < 0.65f -> CLOUDY; else -> CLEAR }
                Season.SPRING -> when { r < 0.25f -> RAIN; r < 0.35f && morning -> FOG; r < 0.5f -> CLOUDY; else -> CLEAR }
                Season.SUMMER -> when { r < 0.12f -> RAIN; r < 0.25f -> CLOUDY; else -> CLEAR }
            }
        }
    }
}

/**
 * The real weather where the user said they live (a city they picked; never their location),
 * as last fetched, with that day's sunrise and sunset. Empty until a city is chosen: then each day
 * keeps its made-up weather.
 */
object LiveWeather {
    /** The weather now, if known and fresh enough (3 hours). */
    @Volatile var current: Weather? = null
    @Volatile var fetchedAt: Long = 0
    /** Sunrise and sunset as hours of the day (7.5 = 7:30); the usual ones until known. */
    @Volatile var sunrise: Float = 6.5f
    @Volatile var sunset: Float = 18.6f

    fun fresh(now: Long = System.currentTimeMillis()): Weather? = current?.takeIf { now - fetchedAt < 3 * 3_600_000L }

    /** WMO weather codes (as Open-Meteo gives them) to the camp's weather. */
    fun fromWmo(code: Int): Weather = when (code) {
        0, 1 -> Weather.CLEAR
        2, 3 -> Weather.CLOUDY
        45, 48 -> Weather.FOG
        in 51..67, in 80..82, in 95..99 -> Weather.RAIN
        in 71..77, 85, 86 -> Weather.SNOW
        else -> Weather.CLOUDY
    }
}
