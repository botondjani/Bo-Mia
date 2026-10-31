package app.tend.core

/**
 * Phone-free sessions: a task counts when the screen stayed mostly off for its whole length.
 * The screen is on when the session starts (the user just tapped Start), so the minutes until
 * they lock the phone count too; [GRACE_SECONDS] covers that and a quick glance at the time.
 */
object PhoneFree {
    const val GRACE_SECONDS = 180L

    /** Screen-on time between [start] and [now], from (time, screenOn) transitions in order. */
    fun screenOnMillis(start: Long, now: Long, transitions: List<Pair<Long, Boolean>>): Long {
        var on = true
        var since = start
        var total = 0L
        for ((t, nowOn) in transitions) {
            if (t < start || t > now) continue
            if (nowOn && !on) { on = true; since = t }
            if (!nowOn && on) { total += t - since; on = false }
        }
        if (on) total += now - since
        return total
    }

    enum class Outcome { RUNNING, DONE, BROKEN }

    fun outcome(task: TaskDef, start: Long, now: Long, screenOnMillis: Long): Outcome = when {
        screenOnMillis > GRACE_SECONDS * 1000 -> Outcome.BROKEN
        now - start >= task.target * 60_000L -> Outcome.DONE
        else -> Outcome.RUNNING
    }
}
