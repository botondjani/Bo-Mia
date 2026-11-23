package app.tend.core

/**
 * The settings as they're organised from now on (see the plan): how strict tend is, what the
 * evening does, what each picked app does when opened, the weekend's extra hour and how much the
 * companion says. Each is one choice instead of several switches that touched the same thing.
 */
enum class StrictLevel { EASY, BALANCED, STRICT }

/** After the evening starts: nothing different, a longer and sleepy Megállító, or the apps closed. */
enum class EveningMode { OFF, SLOW, LOCK }

/** What opening a picked app does: the Megállító, only counting its time, or closing only Reels and Shorts. */
enum class AppMode { STOP, COUNT, SHORTS }

/** The weekend's extra hour: never, every weekend day, or offered once the minutes run out. */
enum class WeekendMode { OFF, ALWAYS, ASK }

/** How much the companion says in notifications. */
enum class NotesLevel { QUIET, NORMAL, CHATTY }

/** The new settings, taken over once from the switches the earlier versions had. */
data class NewSettings(
    val strict: StrictLevel,
    val evening: EveningMode,
    val weekend: WeekendMode,
    val notes: NotesLevel,
    /** Picked apps whose Megállító was turned off: they only count their time now. */
    val countOnly: Set<String>,
)

object SettingsMove {
    /**
     * From the old switches: strict mode on → Strict, else Balanced; the bedtime lock on → the
     * evening closes the apps, sleepy pauses on → it slows them, else nothing; every note turned off
     * → quiet, else normal; an app with its Megállító turned off → it only counts. The weekend asks,
     * as it did.
     */
    fun from(
        strict: Boolean, bedtimeLock: Boolean, sleepyPauses: Boolean,
        morning: Boolean, nudges: Boolean, recap: Boolean, letter: Boolean,
        pauseOff: Set<String>,
    ) = NewSettings(
        strict = if (strict) StrictLevel.STRICT else StrictLevel.BALANCED,
        evening = when { bedtimeLock -> EveningMode.LOCK; sleepyPauses -> EveningMode.SLOW; else -> EveningMode.OFF },
        weekend = WeekendMode.ASK,
        notes = if (!morning && !nudges && !recap && !letter) NotesLevel.QUIET else NotesLevel.NORMAL,
        countOnly = pauseOff,
    )
}
