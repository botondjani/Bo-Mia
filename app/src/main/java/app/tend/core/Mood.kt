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

    /** A two-word status for the widget: "Bo is happy". */
    fun status(name: String, mood: Mood): String = "$name is " + when (mood) {
        Mood.HAPPY -> "happy"
        Mood.TIRED -> "sleepy"
        Mood.SCROLLING -> "scrolling"
        Mood.OUTSIDE -> "glowing"
    }

    /** Companion line: kind nudges towards the user's own picks, never shaming. */
    fun line(cfg: BudgetConfig, day: DayState, selected: List<TaskDef>): String {
        val done = selected.count { it.id in day.completedTasks }
        val next = Tasks.next(day, selected)
        return when {
            Budget.isLocked(cfg, day) -> "Budget's empty for now. Want to earn some back together?"
            next == null && done > 0 -> "What a day. Proud of us."
            next == null -> "Pick a few things you'd rather do, and we'll do them together."
            done == 0 && next == Tasks.OUTSIDE -> "We haven't been outside in a while. Quick walk?"
            done == 0 -> "What shall we do today? ${next.title} sounds nice."
            else -> "Nice one! ${next.title} next?"
        }
    }
}
