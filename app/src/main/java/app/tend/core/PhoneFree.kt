package app.tend.core

/**
 * Phone-free sessions: a task counts when the phone wasn't really used for its whole length. Only
 * time in other apps counts: a screen that's just on (the lock screen, the home screen, tend
 * itself, a call, a workout tracker) doesn't; [GRACE_SECONDS] covers a quick look at something.
 */
object PhoneFree {
    const val GRACE_SECONDS = 180L

    /** Timers that need the screen off: phone-free activities, and a workout when nothing tracks exercise. */
    fun runsPhoneFree(task: TaskDef) = task.verification == Verification.PHONE_FREE || task.id == Tasks.MOVE.id

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

    /** What the phone did, from the system's usage events: an app came up or went away, or the screen went off. */
    enum class Kind { FOREGROUND, BACKGROUND, SCREEN_OFF }
    data class Event(val time: Long, val kind: Kind, val pkg: String? = null)

    /**
     * Time in apps other than [open] between [start] and [now], from usage events in order. The
     * session starts in tend, so nothing is in use at [start].
     */
    fun appUseMillis(start: Long, now: Long, events: List<Event>, open: Set<String>): Long {
        var fg: String? = null
        var since = start
        var total = 0L
        fun using() = fg != null && fg !in open
        for (e in events) {
            if (e.time < start || e.time > now) continue
            val was = using()
            when (e.kind) {
                Kind.FOREGROUND -> fg = e.pkg
                Kind.BACKGROUND -> if (e.pkg == fg) fg = null
                Kind.SCREEN_OFF -> fg = null
            }
            if (!was && using()) since = e.time
            if (was && !using()) total += e.time - since
        }
        if (using()) total += now - since
        return total
    }

    enum class Outcome { RUNNING, DONE, BROKEN }

    fun outcome(task: TaskDef, start: Long, now: Long, useMillis: Long): Outcome = when {
        useMillis > GRACE_SECONDS * 1000 -> Outcome.BROKEN
        now - start >= task.target * 60_000L -> Outcome.DONE
        else -> Outcome.RUNNING
    }
}
