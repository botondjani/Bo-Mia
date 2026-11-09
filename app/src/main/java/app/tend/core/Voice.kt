package app.tend.core

import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * How the companion talks: warm, a little funny, never a to-do list and never shaming. They notice
 * the time, the weekend, the weather and the season; they remember yesterday and a good run of days;
 * they see who's at the camp. A line holds for a few hours (no flicker), then something new.
 */
object Voice {
    /** What the companion knows right now. */
    data class Moment(
        val name: String,
        val now: LocalDateTime,
        val cfg: BudgetConfig,
        val day: DayState,
        val selected: List<TaskDef>,
        /** Task ids done yesterday. */
        val yesterday: Set<String> = emptySet(),
        /** Days in a row with something done, yesterday counting back (today not included). */
        val streak: Int = 0,
        val weather: Weather = Weather.CLEAR,
        val season: Season = Season.of(now.toLocalDate()),
        /** An animal waiting at the camp. */
        val visitor: Visitor? = null,
        /** Friends phone-free at the fire right now. */
        val atFire: List<String> = emptyList(),
        val welcomeBack: Boolean = false,
        /** Out of minutes means the apps really close (the pause permission is there). */
        val canClose: Boolean = true,
    )

    fun home(m: Moment): String {
        val done = m.selected.filter { it.id in m.day.completedTasks }
        val next = Tasks.next(m.day, m.selected)
        // The few things that matter more than chat.
        if (m.selected.isEmpty()) return "Pick a few things you'd rather do, and we'll do them together."
        if (Budget.isLocked(m.cfg, m.day) && !m.welcomeBack && m.canClose) return next?.let { "Out of minutes: your apps are closed. ${it.title} opens them again (+${it.rewardMinutes} min)." }
            ?: "Out of minutes: your apps are closed until tomorrow."
        if (m.welcomeBack && done.isEmpty()) return "Welcome back! I missed you. Let's start small today: ${next!!.title.lowercase()}?"
        m.atFire.firstOrNull()?.let { f -> return pick(m, "fire", listOf("$f is at our fire, phone down. Join them?", "Look who's by the fire: $f. Shall we sit with them?")) }
        m.visitor?.let { v -> return pick(m, "visitor", listOf("${v.aName.replaceFirstChar { it.uppercase() }} is at the camp! Come say hello.", "Shh… ${v.aName} came by. Go and greet it?")) }

        val hour = m.now.hour
        val weekend = m.now.dayOfWeek == DayOfWeek.SATURDAY || m.now.dayOfWeek == DayOfWeek.SUNDAY
        if (done.isEmpty()) {
            val n = next!!
            val ideas = buildList {
                // Remembering: yesterday, and a good run.
                m.selected.firstOrNull { it.id in m.yesterday }?.let { y -> add("Yesterday we ${Tasks.pastTense(y)}. Again today?") }
                if (m.streak >= 3) add("${m.streak} good days in a row! Let's keep the fire going.")
                // The weather and the season.
                when (m.weather) {
                    Weather.RAIN -> add(m.selected.firstOrNull { it.id == Tasks.READ.id }?.let { "Rain on the tent. Perfect for a book, don't you think?" } ?: "It's raining. I've got the umbrella. ${n.title}?")
                    Weather.SNOW -> add("It's snowing! I want to see our footprints. ${n.title}?")
                    Weather.FOG -> if (hour < 11) add("Foggy morning, all soft. ${n.title}?")
                    Weather.CLEAR -> if (m.season == Season.SUMMER && hour in 9..19) add("Sun's out. ${n.title} sounds like a plan.")
                    Weather.CLOUDY -> {}
                }
                if (m.season == Season.AUTUMN && m.weather != Weather.RAIN) add("The leaves are crunchy today. ${n.title}?")
                if (m.season == Season.SPRING && m.weather == Weather.CLEAR) add("Everything's blooming. ${n.title}?")
                // The time of day, and the weekend.
                when {
                    hour < 9 -> add("Morning! I'm up, the fire's lit. ${n.title} first?")
                    hour >= 20 -> add("Late already. Something calm before bed?")
                    weekend -> add("Weekend! No rush today. ${n.title}, whenever you like.")
                    hour in 12..13 -> add("Lunch break? A little ${n.title.lowercase()} would fit right in.")
                    else -> add("What shall we do today? ${n.title} sounds nice.")
                }
            }
            // One quirk at most among the ideas: now and then, not every other time.
            return pick(m, "idea", ideas + pick(m, "quirk", quirks(m)))
        }
        val praise = when {
            next == null -> listOf("Everything's done. Proud of us.", "All of it, done. I'm doing a little dance.", "That's the whole list. Feet up by the fire?")
            done.size == 1 -> listOf("Nice start!", "That's one. The camp feels it.", "Good one. ${next.title} next, whenever.")
            done.size == 2 -> listOf("Look at us!", "Two already. I'm so proud.", "We're on a roll.")
            else -> listOf("You're doing great!", "What a day we're having.", "I'll tell the squirrels about this.")
        }
        return "Today we already ${joinAnd(done.map(Tasks::pastTense))}. ${pick(m, "praise", praise)}"
    }

    /** Under the camp, once the camp is familiar: what the companion is up to, by weather and season. */
    fun camp(name: String, now: LocalDateTime, weather: Weather, season: Season = Season.of(now.toLocalDate())): String {
        val m = Moment(name, now, BudgetConfig(), DayState(now.toLocalDate()), emptyList(), weather = weather, season = season)
        val lines = buildList {
            when (weather) {
                Weather.RAIN -> add("$name listens to the rain on the tent. Tap something to do together.")
                Weather.SNOW -> add("$name is trying to catch snowflakes. Tap something to do together.")
                Weather.FOG -> add("$name can't see the far trees this morning. Cosy. Tap something.")
                else -> {}
            }
            when (season) {
                Season.AUTUMN -> add("$name kicks through the leaves. Tap something to do together.")
                Season.WINTER -> add("$name keeps the fire going. Tap something to do together.")
                Season.SPRING -> add("$name found the first flowers. Tap something to do together.")
                Season.SUMMER -> add("$name hums by the river. Tap something to do together.")
            }
            add("Tap something: $name will use it. Hold it to move it.")
        }
        return pick(m, "camp", lines)
    }

    /** Little things the companion says now and then, just because. */
    private fun quirks(m: Moment): List<String> = listOf(
        "I counted the acorns. Then I lost count. Then I ate one.",
        "I named a cloud after you. It's a good cloud.",
        "Did you know squirrels forget most of their acorns? Same.",
        if (m.now.dayOfWeek == DayOfWeek.MONDAY) "Mondays are better by the fire. ${Tasks.next(m.day, m.selected)?.title ?: "Sit with me"}?" else "I practised my campfire song. Want to hear it later?",
    )

    /**
     * One of [options], the same for about three hours (and for the same [topic]), so the line
     * doesn't change every time the screen is drawn but isn't the same all day either.
     */
    private fun pick(m: Moment, topic: String, options: List<String>): String {
        val slot = m.now.toLocalDate().toEpochDay() * 8 + m.now.hour / 3
        val i = java.util.Random(slot * 31 + topic.hashCode()).nextInt(options.size)
        return options[i]
    }

    private fun joinAnd(parts: List<String>) = when (parts.size) {
        1 -> parts[0]
        2 -> "${parts[0]} and ${parts[1]}"
        else -> parts.dropLast(1).joinToString(", ") + " and " + parts.last()
    }

    /** Days in a row with something done, counting back from yesterday. */
    fun streak(days: List<DaySummary>, today: java.time.LocalDate): Int {
        val active = days.filter { it.tasksDone > 0 || it.done.isNotEmpty() }.map { it.date }.toSet()
        var n = 0
        var d = today.minusDays(1)
        while (d in active) { n++; d = d.minusDays(1) }
        return n
    }
}
