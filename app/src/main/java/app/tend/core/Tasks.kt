package app.tend.core

/** How a task is verified: measured, timed, or checked off by hand (capped daily). */
enum class Verification(val label: String) {
    AUTOMATIC("automatic"), FOCUS_TIMER("timer"), CHECK_OFF("check off")
}

data class TaskDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val rewardMinutes: Int,
    val verification: Verification,
    /** Steps for outside, exercise minutes for move, focus minutes for reading; 0 for check-offs. */
    val target: Int,
    val emoji: String,
) {
    val manual get() = verification == Verification.CHECK_OFF
}

object Tasks {
    const val MAX_SELECTED = 5
    const val CUSTOM_REWARD = 10
    private const val CUSTOM_PREFIX = "custom:"

    val MOVE = TaskDef("move", "Workout or run", "Automatic · 30 min of exercise", 30, Verification.AUTOMATIC, 30, "🏃")
    val OUTSIDE = TaskDef("outside", "Walk outside", "Automatic · 4,000 steps", 20, Verification.AUTOMATIC, 4000, "🌳")
    val READ = TaskDef("read", "Read or learn", "Focus timer · 20 min", 15, Verification.FOCUS_TIMER, 20, "📖")
    val FRIENDS = TaskDef("friends", "Friends & family", "Check off · time together, a call", 15, Verification.CHECK_OFF, 0, "🤗")
    val HOBBY = TaskDef("hobby", "Hobby", "Check off · make, play, create", 15, Verification.CHECK_OFF, 0, "🎨")
    val CHORES = TaskDef("chores", "Tidy up", "Check off · a bit of home care", 10, Verification.CHECK_OFF, 0, "🧺")
    val COOK = TaskDef("cook", "Cook something good", "Check off · a healthy meal", 10, Verification.CHECK_OFF, 0, "🥗")

    /** What onboarding suggests. Nothing is pre-selected. */
    val presets = listOf(MOVE, OUTSIDE, READ, FRIENDS, HOBBY, CHORES, COOK)

    /** What users who onboarded before activities were selectable keep. */
    val legacyDefault = listOf(MOVE, OUTSIDE, READ)

    fun custom(name: String, emoji: String): TaskDef {
        val clean = name.replace(Regex("[|\\n,]"), " ").replace(Regex("\\s+"), " ").trim().take(40)
        val slug = clean.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "own" }
        return TaskDef(CUSTOM_PREFIX + slug, clean, "Check off · your own", CUSTOM_REWARD, Verification.CHECK_OFF, 0, emoji)
    }

    fun isCustom(task: TaskDef) = task.id.startsWith(CUSTOM_PREFIX)

    fun next(day: DayState, selected: List<TaskDef>): TaskDef? = selected.firstOrNull { it.id !in day.completedTasks }

    /** Measured progress towards an automatic task's target, or null for other tasks. */
    fun progress(task: TaskDef, day: DayState): Long? = when (task.id) {
        MOVE.id -> day.exerciseMinutes.toLong()
        OUTSIDE.id -> day.steps
        else -> null
    }
}

/** Today's activity from Health Connect (later Strava). Null = not connected. */
interface ActivitySource {
    suspend fun stepsToday(): Long?
    suspend fun exerciseMinutesToday(): Int?
}

/** Which of the selected automatic tasks the given activity numbers complete. */
fun completedAutomaticTasks(steps: Long?, exerciseMinutes: Int?, selected: List<TaskDef>): List<TaskDef> = buildList {
    if (Tasks.MOVE in selected && exerciseMinutes != null && exerciseMinutes >= Tasks.MOVE.target) add(Tasks.MOVE)
    if (Tasks.OUTSIDE in selected && steps != null && steps >= Tasks.OUTSIDE.target) add(Tasks.OUTSIDE)
}
