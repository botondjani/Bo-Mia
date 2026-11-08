package app.tend.core

/** The four character states. A mirror of the day, never a punishment. */
enum class Mood { HAPPY, TIRED, SCROLLING, OUTSIDE, /** Asleep after "Good night": eyes closed, a smile, z z. */ SLEEPING, /** The evening: heavy lids, still smiling. */ SLEEPY }

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

    /** In the evening a content companion is sleepy instead (the widget and home say so). */
    fun atNight(mood: Mood, night: Boolean): Mood =
        if (night && (mood == Mood.HAPPY || mood == Mood.OUTSIDE)) Mood.SLEEPY else mood

    /** A two-word status for the widget: "Bo is happy". */
    fun status(name: String, mood: Mood): String = "$name is " + when (mood) {
        Mood.HAPPY -> "happy"
        Mood.TIRED -> "sleepy"
        Mood.SCROLLING -> "scrolling"
        Mood.OUTSIDE -> "glowing"
        Mood.SLEEPING -> "asleep"
        Mood.SLEEPY -> "sleepy"
    }

    /**
     * Companion line. Before anything is done: one gentle idea. After that: a look back at what
     * we already did today, with praise that grows with the day. Never a to-do list, never shaming.
     */
    fun line(cfg: BudgetConfig, day: DayState, selected: List<TaskDef>, welcomeBack: Boolean = false): String {
        val done = selected.filter { it.id in day.completedTasks }
        val next = Tasks.next(day, selected)
        if (selected.isEmpty()) return "Pick a few things you'd rather do, and we'll do them together."
        // Out of minutes: say plainly what that means and the way back.
        if (Budget.isLocked(cfg, day) && !welcomeBack) return next?.let { "Out of minutes: your apps are closed. ${it.title} opens them again (+${it.rewardMinutes} min)." }
            ?: "Out of minutes: your apps are closed until tomorrow."
        if (done.isEmpty()) return when {
            // Back after a few days: no catching up, just one small thing.
            welcomeBack -> "Welcome back! I missed you. Let's start small today: ${next!!.title.lowercase()}?"
            next == Tasks.OUTSIDE -> "We haven't been outside in a while. Quick walk?"
            else -> "What shall we do today? ${next!!.title} sounds nice."
        }
        val praise = when {
            next == null -> "Everything's done. Proud of us."
            done.size == 1 -> "Nice start!"
            done.size == 2 -> "Look at us!"
            else -> "You're doing great!"
        }
        return "Today we already ${joinAnd(done.map(Tasks::pastTense))}. $praise"
    }

    private fun joinAnd(parts: List<String>) = when (parts.size) {
        1 -> parts[0]
        2 -> "${parts[0]} and ${parts[1]}"
        else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    }
}
