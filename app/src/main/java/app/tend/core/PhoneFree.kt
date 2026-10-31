package app.tend.core

/**
 * Phone-free sessions: a task counts when the screen stayed mostly off for its whole length.
 * The screen is on when the session starts (the user just tapped Start), so the minutes until
 * they lock the phone count too; [GRACE_SECONDS] covers that and a quick glance at the time.
 */
object PhoneFree {
    const val GRACE_SECONDS = 180L

    /** A workout without a tracker: the screen stays off, apart from starting and a quick check. */
    const val WORKOUT_GRACE_SECONDS = 60L

    /** Timers that need the screen off: phone-free activities, and a workout when nothing tracks exercise. */
    fun runsPhoneFree(task: TaskDef) = task.verification == Verification.PHONE_FREE || task.id == Tasks.MOVE.id

    fun graceSeconds(task: TaskDef) = if (task.id == Tasks.MOVE.id) WORKOUT_GRACE_SECONDS else GRACE_SECONDS

    /**
     * Longest stretch with the screen off between [from] and [to], from (time, screenOn) transitions
     * in order. The screen counts as on at [from], so a stretch starts at a real "screen off".
     * Without a sleep tracker, last night's longest one stands in for sleep.
     */
    fun longestOffMillis(from: Long, to: Long, transitions: List<Pair<Long, Boolean>>): Long {
        var offSince: Long? = null
        var longest = 0L
        for ((t, on) in transitions) {
            if (t < from || t > to) continue
            if (!on && offSince == null) offSince = t
            if (on && offSince != null) { longest = maxOf(longest, t - offSince); offSince = null }
        }
        offSince?.let { longest = maxOf(longest, to - it) }
        return longest
    }

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
        screenOnMillis > graceSeconds(task) * 1000 -> Outcome.BROKEN
        now - start >= task.target * 60_000L -> Outcome.DONE
        else -> Outcome.RUNNING
    }
}
