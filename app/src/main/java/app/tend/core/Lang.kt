package app.tend.core

import java.util.Locale

/**
 * The app's language: English or Hungarian, chosen in setup (and in settings), the phone's own to
 * start with. Every text in the app is written in both, side by side, with [tr]. It's set once when
 * the app starts (names of things are made then); changing it starts the app again.
 */
object Lang {
    @Volatile var hu = false

    val code: String get() = if (hu) "hu" else "en"

    /** For dates and numbers. */
    val locale: Locale get() = if (hu) Locale("hu") else Locale.ENGLISH

    /** The language to start with on a phone set to [system] (a language code like "hu"). */
    fun startingWith(saved: String?, system: String): Boolean = (saved ?: system) == "hu"
}

/** [en] or [hu], whichever the app speaks. */
fun tr(en: String, hu: String): String = if (Lang.hu) hu else en
