package app.tend.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

/** Three skills that grow with the activities: what you do shows up as levels. */
enum class Skill(val label: String) {
    BODY("Body"), MIND("Mind"), REST("Rest");

    companion object {
        fun of(taskId: String): Skill = when (taskId) {
            "move", "outside", "chores", "cook" -> BODY
            "sleep", "friends" -> REST
            else -> MIND // reading, learning, hobbies and your own
        }
    }
}

/** A level and how far into it you are. */
data class Level(val level: Int, val into: Int, val needed: Int) {
    val fraction: Float get() = into / needed.toFloat()
}

/** This week's one goal, and how far along it is. */
data class Challenge(val title: String, val unit: String, val target: Int, val progress: Int, val weekKey: String) {
    val done: Boolean get() = progress >= target
}

object Progress {
    const val XP_PER_ACTIVITY = 20
    const val XP_PER_CHALLENGE = 100

    /** Level 2 after 40 XP (two activities), then each level asks 20 more than the one before. */
    fun level(xp: Int): Level {
        var left = xp.coerceAtLeast(0)
        var level = 1
        var need = 40
        while (left >= need) { left -= need; level++; need += 20 }
        return Level(level, left, need)
    }

    /** XP per skill from every activity ever done. */
    fun skillXp(days: List<DaySummary>): Map<Skill, Int> {
        val xp = Skill.entries.associateWith { 0 }.toMutableMap()
        days.forEach { d -> d.done.forEach { id -> xp[Skill.of(id)] = xp.getValue(Skill.of(id)) + XP_PER_ACTIVITY } }
        return xp
    }

    fun weekKey(date: LocalDate): String = "${date.get(IsoFields.WEEK_BASED_YEAR)}-W${date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

    /**
     * This week's challenge (Monday to Sunday), picked from what the user does, the same all week.
     * Progress counts days (or activities) since Monday.
     */
    fun challenge(today: LocalDate, picked: List<String>, days: List<DaySummary>): Challenge {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = days.filter { !it.date.isBefore(monday) && !it.date.isAfter(today) }
        fun daysWith(ids: Set<String>) = week.count { d -> d.done.any { it in ids } }
        val options = buildList {
            if ("move" in picked) add(Challenge("3 workout days", "workout days", 3, daysWith(setOf("move")), ""))
            if ("outside" in picked) add(Challenge("4 days outside", "days outside", 4, daysWith(setOf("outside")), ""))
            if ("sleep" in picked) add(Challenge("5 good nights", "good nights", 5, daysWith(setOf("sleep")), ""))
            if ("read" in picked || "learn" in picked) add(Challenge("4 days of reading or learning", "days", 4, daysWith(setOf("read", "learn")), ""))
            add(Challenge("Something done on 5 days", "active days", 5, week.count { it.done.isNotEmpty() || it.tasksDone > 0 }, ""))
            add(Challenge("10 activities this week", "activities", 10, week.sumOf { maxOf(it.done.size, it.tasksDone) }, ""))
        }
        val key = weekKey(today)
        return options[Math.floorMod(key.hashCode(), options.size)].copy(weekKey = key)
    }
}
