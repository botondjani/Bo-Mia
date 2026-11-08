package app.tend.core

import java.time.LocalDate

/** The day's weather in the camp: the same all day (fog only in the morning), different from day to day. */
enum class Weather { CLEAR, CLOUDY, RAIN, FOG, SNOW;

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
