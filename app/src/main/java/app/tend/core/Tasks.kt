package app.tend.core

/** How a task is verified: measured, timed, phone-free, or checked off by hand (capped daily). */
enum class Verification(val label: String) {
    AUTOMATIC(tr("automatic", "automatikus")), FOCUS_TIMER(tr("timer", "időzítő")), PHONE_FREE(tr("phone-free", "telefonmentes")), CHECK_OFF(tr("check off", "pipálható"));

    /** Started with a button and runs for [TaskDef.target] minutes. */
    val timed get() = this == FOCUS_TIMER || this == PHONE_FREE
}

data class TaskDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val rewardMinutes: Int,
    val verification: Verification,
    /** Steps for outside, minutes for exercise, sleep and timers; 0 for check-offs. */
    val target: Int,
    /** Key into tend's icon set (ui/TendIcons). */
    val icon: String,
) {
    val manual get() = verification == Verification.CHECK_OFF

    // The same activity whatever its reward today (rewards are shared out from the budget).
    override fun equals(other: Any?) = other is TaskDef && other.id == id
    override fun hashCode() = id.hashCode()
}

object Tasks {
    const val MAX_SELECTED = 10
    const val CUSTOM_REWARD = 10
    private const val CUSTOM_PREFIX = "custom:"

    val MOVE = TaskDef("move", tr("Workout or run", "Edzés vagy futás"), tr("Automatic · 30 min of exercise", "Automatikus · 30 perc mozgás"), 30, Verification.AUTOMATIC, 30, "workout")
    val OUTSIDE = TaskDef("outside", tr("Walk outside", "Séta a szabadban"), tr("Automatic · 4,000 steps", "Automatikus · 4000 lépés"), 20, Verification.AUTOMATIC, 4000, "outside")
    val READ = TaskDef("read", tr("Read", "Olvasás"), tr("Focus timer · 20 min", "Fókuszidőzítő · 20 perc"), 15, Verification.FOCUS_TIMER, 20, "read")
    val LEARN = TaskDef("learn", tr("Learn something", "Tanulás"), tr("Focus timer · 30 min", "Fókuszidőzítő · 30 perc"), 20, Verification.FOCUS_TIMER, 30, "learn")
    val SLEEP = TaskDef("sleep", tr("Sleep well", "Jó alvás"), tr("Automatic · 7 h of sleep", "Automatikus · 7 óra alvás"), 20, Verification.AUTOMATIC, 420, "moon")
    val FRIENDS = TaskDef("friends", tr("Friends & family", "Barátok és család"), tr("Phone-free · 30 min together", "Telefonmentes · 30 perc együtt"), 15, Verification.PHONE_FREE, 30, "friends")
    val HOBBY = TaskDef("hobby", tr("Hobby", "Hobbi"), tr("Phone-free · 30 min", "Telefonmentes · 30 perc"), 15, Verification.PHONE_FREE, 30, "hobby")
    val CHORES = TaskDef("chores", tr("Tidy up", "Rendrakás"), tr("Phone-free · 20 min", "Telefonmentes · 20 perc"), 10, Verification.PHONE_FREE, 20, "chores")
    val COOK = TaskDef("cook", tr("Cook something good", "Főzés"), tr("Phone-free · 20 min", "Telefonmentes · 20 perc"), 10, Verification.PHONE_FREE, 20, "cook")

    /** What onboarding suggests. Nothing is pre-selected. */
    const val TIMER_ID = "timer"

    /** Just a timer, no activity: no minutes, but every app waits until it ends, the same as on an activity's timer. */
    fun timer(minutes: Int) = TaskDef(TIMER_ID, tr("Phone down", "Telefon le"), tr("Phone down · $minutes min", "Telefon le · $minutes perc"), 0, Verification.FOCUS_TIMER, minutes, "sparkle")

    const val TIMER_MIN = 15
    const val TIMER_MAX = 120
    const val TIMER_STEP = 5

    /** A plain timer's length: 15 to 120 minutes, in steps of 5. */
    fun clampTimer(minutes: Int): Int = ((minutes.coerceIn(TIMER_MIN, TIMER_MAX) + TIMER_STEP / 2) / TIMER_STEP * TIMER_STEP).coerceIn(TIMER_MIN, TIMER_MAX)

    val presets = listOf(MOVE, OUTSIDE, SLEEP, READ, LEARN, FRIENDS, HOBBY, CHORES, COOK)

    /** What users who onboarded before activities were selectable keep. */
    val legacyDefault = listOf(MOVE, OUTSIDE, READ)

    /** The user's own activity: ticked off by hand, or with [minutes] a phone-free timer. */
    fun custom(name: String, icon: String, minutes: Int = 0): TaskDef {
        val clean = name.replace(Regex("[|\\n,]"), " ").replace(Regex("\\s+"), " ").trim().take(40)
        val slug = clean.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "own" + (clean.hashCode() and 0xffff) }
        return if (minutes > 0) TaskDef(CUSTOM_PREFIX + slug, clean, tr("Phone-free · $minutes min", "Telefonmentes · $minutes perc"), maxOf(CUSTOM_REWARD, minutes / 2), Verification.PHONE_FREE, minutes, icon)
        else TaskDef(CUSTOM_PREFIX + slug, clean, tr("Check off · your own", "Pipálható · saját"), CUSTOM_REWARD, Verification.CHECK_OFF, 0, icon)
    }

    /** One-tap ideas for your own activities: name, icon, minutes (0 = tick off). */
    val ownIdeas = listOf(
        Triple(tr("Meditate", "Meditáció"), "sparkle", 10), Triple(tr("Journal", "Naplóírás"), "pen", 0), Triple(tr("Play music", "Zenélés"), "music", 30),
        Triple(tr("Walk the dog", "Kutyasétáltatás"), "paw", 0), Triple(tr("Stretch", "Nyújtás"), "sun", 0), Triple(tr("Bike ride", "Biciklizés"), "bike", 30),
        Triple(tr("Garden", "Kertészkedés"), "sprout", 0), Triple(tr("Call someone", "Felhívok valakit"), "heart", 0),
    )

    /**
     * Shares the earnable minutes (max minus free) out over the picked activities, weighted by effort
     * (a workout counts double a tidy-up). Doing all of them reaches the daily max; picking more
     * activities makes each one worth a little less. Rounded to 5, at least 5 each.
     */
    fun withRewards(cfg: BudgetConfig, picked: List<TaskDef>): List<TaskDef> {
        if (picked.isEmpty()) return picked
        val weight = { t: TaskDef -> (presets.firstOrNull { it == t }?.rewardMinutes ?: t.rewardMinutes).coerceAtLeast(1) }
        val total = picked.sumOf(weight).toDouble()
        val pool = (cfg.totalMinutes - cfg.freeMinutes).coerceAtLeast(0)
        return picked.map { t ->
            val share = pool * weight(t) / total
            t.copy(rewardMinutes = (Math.round(share / 5) * 5).toInt().coerceAtLeast(5))
        }
    }

    fun isCustom(task: TaskDef) = task.id.startsWith(CUSTOM_PREFIX)

    fun next(day: DayState, selected: List<TaskDef>): TaskDef? = selected.firstOrNull { it.id !in day.completedTasks }

    /** Today's list: still-to-do first, finished ones sink to the bottom (order kept within each group). */
    fun ordered(day: DayState, selected: List<TaskDef>): List<TaskDef> = selected.sortedBy { it.id in day.completedTasks }

    /** How the companion talks about a finished task: "Today we already …". */
    /**
     * What a friend sees you did ("went for a walk"). Your own activities say only that: their
     * names can be personal.
     */
    fun doneLine(task: TaskDef): String = when (task.id) {
        MOVE.id -> "worked out"
        OUTSIDE.id -> "went for a walk"
        SLEEP.id -> "slept well"
        READ.id -> "read for a while"
        LEARN.id -> "learned something"
        FRIENDS.id -> "made time for friends"
        HOBBY.id -> "made time for a hobby"
        CHORES.id -> "tidied up"
        COOK.id -> "cooked something good"
        else -> "did something of their own"
    }

    /**
     * A friend's [doneLine] as it's shown here: it travels in English (it's what older versions
     * send too), and is said in this phone's language.
     */
    fun doneShown(line: String): String = when (line) {
        "worked out" -> tr("worked out", "edzett")
        "went for a walk" -> tr("went for a walk", "sétált egyet")
        "slept well" -> tr("slept well", "jól aludt")
        "read for a while" -> tr("read for a while", "olvasott egy kicsit")
        "learned something" -> tr("learned something", "tanult valamit")
        "made time for friends" -> tr("made time for friends", "időt szánt a barátaira")
        "made time for a hobby" -> tr("made time for a hobby", "a hobbijával foglalkozott")
        "tidied up" -> tr("tidied up", "rendet rakott")
        "cooked something good" -> tr("cooked something good", "főzött valami finomat")
        "did something of their own" -> tr("did something of their own", "a saját dolgával foglalkozott")
        else -> line
    }

    fun pastTense(task: TaskDef): String = when (task.id) {
        MOVE.id -> tr("moved", "mozogtunk")
        OUTSIDE.id -> tr("went outside", "kint voltunk a szabadban")
        SLEEP.id -> tr("slept well", "jól aludtunk")
        READ.id -> tr("read", "olvastunk")
        LEARN.id -> tr("learned something", "tanultunk valamit")
        FRIENDS.id -> tr("made time for our people", "időt szántunk a szeretteinkre")
        HOBBY.id -> tr("played with a hobby", "hobbiztunk")
        CHORES.id -> tr("tidied up", "rendet raktunk")
        COOK.id -> tr("cooked something good", "főztünk valami finomat")
        else -> tr("did ${task.title.lowercase()}", "megvolt ez is: ${task.title.lowercase()}")
    }

    /** Measured progress towards an automatic task's target, or null for other tasks. */
    fun progress(task: TaskDef, day: DayState): Long? = when (task.id) {
        MOVE.id -> day.exerciseMinutes.toLong()
        OUTSIDE.id -> day.steps
        SLEEP.id -> day.sleepMinutes.toLong()
        else -> null
    }
}

/** Today's activity from Health Connect (later Strava). Null = not connected. */
interface ActivitySource {
    suspend fun stepsToday(): Long?
    suspend fun exerciseMinutesToday(): Int?
    suspend fun sleepMinutesLastNight(): Int?
}

/** Which of the selected automatic tasks the given activity numbers complete. */
fun completedAutomaticTasks(steps: Long?, exerciseMinutes: Int?, selected: List<TaskDef>, sleepMinutes: Int? = null): List<TaskDef> = buildList {
    // The picked instances, so today's shared-out reward is what gets credited.
    fun picked(t: TaskDef) = selected.firstOrNull { it == t }
    if (exerciseMinutes != null && exerciseMinutes >= Tasks.MOVE.target) picked(Tasks.MOVE)?.let(::add)
    if (steps != null && steps >= Tasks.OUTSIDE.target) picked(Tasks.OUTSIDE)?.let(::add)
    if (sleepMinutes != null && sleepMinutes >= Tasks.SLEEP.target) picked(Tasks.SLEEP)?.let(::add)
}
