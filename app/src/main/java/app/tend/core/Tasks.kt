package app.tend.core

enum class Verification { AUTOMATIC, FOCUS_TIMER }

/** The three MVP task types. Health Connect wiring comes later (see [ActivitySource]). */
data class TaskDef(
    val id: String,
    val title: String,
    val subtitle: String,
    val rewardMinutes: Int,
    val verification: Verification,
    /** Steps for outside, exercise minutes for move, focus minutes for reading. */
    val target: Int,
)

object Tasks {
    val MOVE = TaskDef("move", "Workout or run", "Automatic · Health Connect", 30, Verification.AUTOMATIC, 30)
    val OUTSIDE = TaskDef("outside", "Time outside", "Automatic · steps", 20, Verification.AUTOMATIC, 4000)
    val READ = TaskDef("read", "Read for 20 min", "Focus timer · reads with you", 15, Verification.FOCUS_TIMER, 20)

    val all = listOf(MOVE, OUTSIDE, READ)

    fun next(day: DayState): TaskDef? = all.firstOrNull { it.id !in day.completedTasks }
}

/** Today's activity from Health Connect (later Strava). Null = not connected. */
interface ActivitySource {
    suspend fun stepsToday(): Long?
    suspend fun exerciseMinutesToday(): Int?
}

/** Placeholder until Health Connect is wired in. */
object NotConnectedActivitySource : ActivitySource {
    override suspend fun stepsToday(): Long? = null
    override suspend fun exerciseMinutesToday(): Int? = null
}

/** Which automatic tasks the given activity numbers complete. */
fun completedAutomaticTasks(steps: Long?, exerciseMinutes: Int?): List<TaskDef> = buildList {
    if (exerciseMinutes != null && exerciseMinutes >= Tasks.MOVE.target) add(Tasks.MOVE)
    if (steps != null && steps >= Tasks.OUTSIDE.target) add(Tasks.OUTSIDE)
}
