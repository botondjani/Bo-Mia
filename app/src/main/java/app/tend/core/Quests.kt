package app.tend.core

import java.time.LocalDate

/** Bo's quest for the day: one small, doable thing, with acorns for it. */
data class Quest(val id: String, val title: String, val how: String, val acorns: Int)

object Quests {
    /** The day's quest, the same all day: picked from what fits (your activities, your minutes). */
    fun of(date: LocalDate, selected: List<TaskDef>, freeMinutes: Int): Quest {
        val options = buildList {
            selected.forEach { add(Quest("do:${it.id}", tr("${it.title} today", "Ma: ${it.title.lowercase()}"), tr("One of yours, any time today", "Egy a sajátjaid közül, ma bármikor"), 15)) }
            if (selected.size >= 2) add(Quest("two", tr("Two activities today", "Két tevékenység ma"), tr("Any two of yours", "Bármelyik kettő a sajátjaid közül"), 20))
            add(Quest("notnow", tr("Say \"Not now\" twice", "Mondd kétszer: „Most nem”"), tr("When a paused app opens", "Amikor megnyílik egy szüneteltetett app"), 10))
            add(Quest("under:$freeMinutes", tr("Stay under $freeMinutes min today", "Maradj ma $freeMinutes perc alatt"), tr("Counts from 20:00", "20:00-tól számít"), 20))
        }
        return options[java.util.Random(date.toEpochDay() * 7919 + 13).nextInt(options.size)]
    }

    /** Whether [q] is done: by today's activities, today's "Not now"s, and (from 20:00) the minutes used. */
    fun done(q: Quest, day: DayState, notNows: Int, hour: Int): Boolean = when {
        q.id.startsWith("do:") -> q.id.removePrefix("do:") in day.completedTasks
        q.id == "two" -> day.completedTasks.size >= 2
        q.id == "notnow" -> notNows >= 2
        q.id.startsWith("under:") -> hour >= 20 && day.usedSeconds < (q.id.removePrefix("under:").toIntOrNull() ?: 0) * 60L
        else -> false
    }
}
