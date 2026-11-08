package app.tend.core

/** How a task is verified: measured, timed, phone-free, or checked off by hand (capped daily). */
enum class Verification(val label: String) {
    AUTOMATIC("automatic"), FOCUS_TIMER("timer"), PHONE_FREE("phone-free"), CHECK_OFF("check off");

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

    val MOVE = TaskDef("move", "Workout or run", "Automatic · 30 min of exercise", 30, Verification.AUTOMATIC, 30, "workout")
    val OUTSIDE = TaskDef("outside", "Walk outside", "Automatic · 4,000 steps", 20, Verification.AUTOMATIC, 4000, "outside")
    val READ = TaskDef("read", "Read", "Focus timer · 20 min", 15, Verification.FOCUS_TIMER, 20, "read")
    val LEARN = TaskDef("learn", "Learn something", "Focus timer · 30 min", 20, Verification.FOCUS_TIMER, 30, "learn")
    val SLEEP = TaskDef("sleep", "Sleep well", "Automatic · 7 h of sleep", 20, Verification.AUTOMATIC, 420, "moon")
    val FRIENDS = TaskDef("friends", "Friends & family", "Phone-free · 30 min together", 15, Verification.PHONE_FREE, 30, "friends")
    val HOBBY = TaskDef("hobby", "Hobby", "Phone-free · 30 min", 15, Verification.PHONE_FREE, 30, "hobby")
    val CHORES = TaskDef("chores", "Tidy up", "Phone-free · 20 min", 10, Verification.PHONE_FREE, 20, "chores")
    val COOK = TaskDef("cook", "Cook something good", "Phone-free · 20 min", 10, Verification.PHONE_FREE, 20, "cook")

    /** What onboarding suggests. Nothing is pre-selected. */
    val presets = listOf(MOVE, OUTSIDE, SLEEP, READ, LEARN, FRIENDS, HOBBY, CHORES, COOK)

    /** What users who onboarded before activities were selectable keep. */
    val legacyDefault = listOf(MOVE, OUTSIDE, READ)

    /** The user's own activity: ticked off by hand, or with [minutes] a phone-free timer. */
    fun custom(name: String, icon: String, minutes: Int = 0): TaskDef {
        val clean = name.replace(Regex("[|\\n,]"), " ").replace(Regex("\\s+"), " ").trim().take(40)
        val slug = clean.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "own" }
        return if (minutes > 0) TaskDef(CUSTOM_PREFIX + slug, clean, "Phone-free · $minutes min", maxOf(CUSTOM_REWARD, minutes / 2), Verification.PHONE_FREE, minutes, icon)
        else TaskDef(CUSTOM_PREFIX + slug, clean, "Check off · your own", CUSTOM_REWARD, Verification.CHECK_OFF, 0, icon)
    }

    /** One-tap ideas for your own activities: name, icon, minutes (0 = tick off). */
    val ownIdeas = listOf(
        Triple("Meditate", "sparkle", 10), Triple("Journal", "pen", 0), Triple("Play music", "music", 30),
        Triple("Walk the dog", "paw", 0), Triple("Stretch", "sun", 0), Triple("Bike ride", "bike", 30),
        Triple("Garden", "sprout", 0), Triple("Call someone", "heart", 0),
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

    fun pastTense(task: TaskDef): String = when (task.id) {
        MOVE.id -> "moved"
        OUTSIDE.id -> "went outside"
        SLEEP.id -> "slept well"
        READ.id -> "read"
        LEARN.id -> "learned something"
        FRIENDS.id -> "made time for our people"
        HOBBY.id -> "played with a hobby"
        CHORES.id -> "tidied up"
        COOK.id -> "cooked something good"
        else -> "did ${task.title.lowercase()}"
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
