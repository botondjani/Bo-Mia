package app.tend.core

/** The four character states. A mirror of the day, never a punishment. */
enum class Mood { HAPPY, TIRED, SCROLLING, OUTSIDE }

/** Bo and Mia: one shared rig, differing only in hair and colour. */
enum class Avatar(val displayName: String) { BO("Bo"), MIA("Mia") }

object Moods {
    const val LOW_BUDGET_MINUTES = 10

    /** Mood for home screen and widget. SCROLLING is only shown on the delayed-open screen. */
    fun of(cfg: BudgetConfig, day: DayState): Mood = when {
        Budget.remainingMinutes(cfg, day) < LOW_BUDGET_MINUTES -> Mood.TIRED
        Tasks.MOVE.id in day.completedTasks || Tasks.OUTSIDE.id in day.completedTasks -> Mood.OUTSIDE
        else -> Mood.HAPPY
    }

    /** Companion line: kind nudges, never shaming. */
    fun line(cfg: BudgetConfig, day: DayState): String {
        val outside = Tasks.OUTSIDE.id in day.completedTasks || Tasks.MOVE.id in day.completedTasks
        return when {
            Budget.isLocked(cfg, day) -> "Budget's empty for now. Want to earn some back together?"
            outside && Tasks.READ.id !in day.completedTasks -> "We already went outside today. Felt good! Read a bit tonight?"
            !outside -> "We haven't been outside in a while. Quick walk?"
            else -> "What a day. Proud of us."
        }
    }
}
