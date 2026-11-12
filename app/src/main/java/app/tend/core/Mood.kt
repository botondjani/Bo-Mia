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
    fun status(name: String, mood: Mood): String = when (mood) {
        Mood.HAPPY -> tr("$name is happy", "$name jókedvű")
        Mood.TIRED -> tr("$name is sleepy", "$name fáradt")
        Mood.SCROLLING -> tr("$name is scrolling", "$name görget")
        Mood.OUTSIDE -> tr("$name is glowing", "$name ragyog")
        Mood.SLEEPING -> tr("$name is asleep", "$name alszik")
        Mood.SLEEPY -> tr("$name is sleepy", "$name álmos")
    }

    /**
     * Companion line. Before anything is done: one gentle idea. After that: a look back at what
     * we already did today, with praise that grows with the day. Never a to-do list, never shaming.
     */
    fun line(cfg: BudgetConfig, day: DayState, selected: List<TaskDef>, welcomeBack: Boolean = false, canClose: Boolean = true): String {
        val done = selected.filter { it.id in day.completedTasks }
        val next = Tasks.next(day, selected)
        if (selected.isEmpty()) return tr("Pick a few things you'd rather do, and we'll do them together.", "Válassz pár dolgot, amit szívesebben csinálnál, és megcsináljuk együtt.")
        // Out of minutes: say plainly what that means and the way back.
        if (Budget.isLocked(cfg, day) && !welcomeBack && canClose) return next?.let { tr("Out of minutes: your apps are closed. ${it.title} opens them again (+${it.rewardMinutes} min).", "Elfogytak a percek: az appjaid zárva vannak. Ezzel újra kinyílnak: ${it.title} (+${it.rewardMinutes} perc).") }
            ?: tr("Out of minutes: your apps are closed until tomorrow.", "Elfogytak a percek: az appjaid holnapig zárva vannak.")
        if (done.isEmpty()) return when {
            // Back after a few days: no catching up, just one small thing.
            welcomeBack -> tr("Welcome back! I missed you. Let's start small today: ${next!!.title.lowercase()}?", "Jó újra látni! Hiányoztál. Kezdjük ma valami kicsivel: ${next!!.title.lowercase()}?")
            next == Tasks.OUTSIDE -> tr("We haven't been outside in a while. Quick walk?", "Rég voltunk a szabadban. Egy gyors séta?")
            else -> tr("What shall we do today? ${next!!.title} sounds nice.", "Mit csináljunk ma? Ez jól hangzik: ${next!!.title.lowercase()}.")
        }
        val praise = when {
            next == null -> tr("Everything's done. Proud of us.", "Minden megvan. Büszke vagyok ránk.")
            done.size == 1 -> tr("Nice start!", "Szép kezdés!")
            done.size == 2 -> tr("Look at us!", "Nézd, milyen ügyesek vagyunk!")
            else -> tr("You're doing great!", "Nagyon jól megy!")
        }
        return tr("Today we already ${joinAnd(done.map(Tasks::pastTense))}. $praise", "Ma már ${joinAnd(done.map(Tasks::pastTense))}. $praise")
    }

    private fun joinAnd(parts: List<String>) = when (parts.size) {
        1 -> parts[0]
        2 -> tr("${parts[0]} and ${parts[1]}", "${parts[0]} és ${parts[1]}")
        else -> parts.dropLast(1).joinToString(", ") + tr(" and ", " és ") + parts.last()
    }
}
