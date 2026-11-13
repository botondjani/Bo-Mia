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
        /** Yesterday was missed, and the run was kept alive all the same. */
        val kept: Boolean = false,
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
        if (m.selected.isEmpty()) return tr("Pick a few things you'd rather do, and we'll do them together.", "Válassz pár dolgot, amit szívesebben csinálnál, és megcsináljuk együtt.")
        if (Budget.isLocked(m.cfg, m.day) && !m.welcomeBack && m.canClose) return next?.let { tr("Out of minutes: your apps are closed. ${it.title} opens them again (+${it.rewardMinutes} min).", "Elfogytak a percek: az appjaid zárva vannak. Ezzel újra kinyílnak: ${it.title} (+${it.rewardMinutes} perc).") }
            ?: tr("Out of minutes: your apps are closed until tomorrow.", "Elfogytak a percek: az appjaid holnapig zárva vannak.")
        if (m.welcomeBack && done.isEmpty()) return tr("Welcome back! I missed you. Let's start small today: ${next!!.title.lowercase()}?", "Jó újra látni! Hiányoztál. Kezdjük ma valami kicsivel: ${next!!.title.lowercase()}?")
        // Yesterday slipped by, and the run goes on: said first thing, before anything's done today.
        if (m.kept && done.isEmpty() && m.streak >= 2) return tr(
            "Yesterday slipped by. I kept the fire going: still ${m.streak} days in a row.",
            "A tegnap kimaradt. Őriztem a tüzet: még mindig ${m.streak} nap egymás után.",
        )
        m.atFire.firstOrNull()?.let { f -> return pick(m, "fire", listOf(tr("$f is at our fire, phone down. Join them?", "$f a tüzünknél ül, letette a telefont. Csatlakozunk?"), tr("Look who's by the fire: $f. Shall we sit with them?", "Nézd, ki ül a tűznél: $f. Odaülünk mellé?"))) }
        m.visitor?.let { v -> return pick(m, "visitor", listOf(tr("${v.aName.replaceFirstChar { it.uppercase() }} is at the camp! Come say hello.", "${v.aName.replaceFirstChar { it.uppercase() }} van a táborban! Gyere, köszönj neki."), tr("Shh… ${v.aName} came by. Go and greet it?", "Pszt… erre járt ${v.aName}. Köszönsz neki?"))) }

        val hour = m.now.hour
        val weekend = m.now.dayOfWeek == DayOfWeek.SATURDAY || m.now.dayOfWeek == DayOfWeek.SUNDAY
        if (done.isEmpty()) {
            val n = next!!
            val ideas = buildList {
                // Remembering: yesterday, and a good run.
                m.selected.firstOrNull { it.id in m.yesterday }?.let { y -> add(tr("Yesterday we ${Tasks.pastTense(y)}. Again today?", "Tegnap ${Tasks.pastTense(y)}. Ma is?")) }
                if (m.streak >= 3) add(tr("${m.streak} good days in a row! Let's keep the fire going.", "${m.streak} jó nap egymás után! Tartsuk életben a tüzet."))
                // The weather and the season.
                when (m.weather) {
                    Weather.RAIN -> add(m.selected.firstOrNull { it.id == Tasks.READ.id }?.let { tr("Rain on the tent. Perfect for a book, don't you think?", "Kopog az eső a sátron. Tökéletes idő egy könyvhöz, nem?") } ?: tr("It's raining. I've got the umbrella. ${n.title}?", "Esik. Nálam az esernyő. ${n.title}?"))
                    Weather.SNOW -> add(tr("It's snowing! I want to see our footprints. ${n.title}?", "Havazik! Látni akarom a lábnyomainkat. ${n.title}?"))
                    Weather.FOG -> if (hour < 11) add(tr("Foggy morning, all soft. ${n.title}?", "Ködös, puha reggel. ${n.title}?"))
                    Weather.CLEAR -> if (m.season == Season.SUMMER && hour in 9..19) add(tr("Sun's out. ${n.title} sounds like a plan.", "Kisütött a nap. ${n.title}: jó tervnek hangzik."))
                    Weather.CLOUDY -> {}
                }
                if (m.season == Season.AUTUMN && m.weather != Weather.RAIN) add(tr("The leaves are crunchy today. ${n.title}?", "Ma ropognak a levelek. ${n.title}?"))
                if (m.season == Season.SPRING && m.weather == Weather.CLEAR) add(tr("Everything's blooming. ${n.title}?", "Minden virágzik. ${n.title}?"))
                // The time of day, and the weekend.
                when {
                    hour < 9 -> add(tr("Morning! I'm up, the fire's lit. ${n.title} first?", "Jó reggelt! Fent vagyok, ég a tűz. Kezdjük ezzel: ${n.title.lowercase()}?"))
                    hour >= 20 -> add(tr("Late already. Something calm before bed?", "Már késő van. Valami nyugis lefekvés előtt?"))
                    weekend -> add(tr("Weekend! No rush today. ${n.title}, whenever you like.", "Hétvége! Ma nem sietünk. ${n.title}, amikor jólesik."))
                    hour in 12..13 -> add(tr("Lunch break? A little ${n.title.lowercase()} would fit right in.", "Ebédszünet? Épp beleférne: ${n.title.lowercase()}."))
                    else -> add(tr("What shall we do today? ${n.title} sounds nice.", "Mit csináljunk ma? Ez jól hangzik: ${n.title.lowercase()}."))
                }
            }
            // One quirk at most among the ideas: now and then, not every other time.
            return pick(m, "idea", ideas + pick(m, "quirk", quirks(m)))
        }
        val praise = when {
            next == null -> listOf(tr("Everything's done. Proud of us.", "Minden megvan. Büszke vagyok ránk."), tr("All of it, done. I'm doing a little dance.", "Mind megvan. Járok egy kis örömtáncot."), tr("That's the whole list. Feet up by the fire?", "Ennyi volt a lista. Lábat fel a tűz mellett?"))
            done.size == 1 -> listOf(tr("Nice start!", "Szép kezdés!"), tr("That's one. The camp feels it.", "Ez egy. A tábor is érzi."), tr("Good one. ${next.title} next, whenever.", "Ez jó volt. Jöhet majd ez is, ha kedved lesz: ${next.title.lowercase()}."))
            done.size == 2 -> listOf(tr("Look at us!", "Nézd, milyen ügyesek vagyunk!"), tr("Two already. I'm so proud.", "Már kettő. Nagyon büszke vagyok."), tr("We're on a roll.", "Lendületben vagyunk."))
            else -> listOf(tr("You're doing great!", "Nagyon jól megy!"), tr("What a day we're having.", "Micsoda napunk van."), tr("I'll tell the squirrels about this.", "Ezt elmesélem a mókusoknak."))
        }
        return tr("Today we already ${joinAnd(done.map(Tasks::pastTense))}. ${pick(m, "praise", praise)}", "Ma már ${joinAnd(done.map(Tasks::pastTense))}. ${pick(m, "praise", praise)}")
    }

    /** Under the camp, once the camp is familiar: what the companion is up to, by weather and season. */
    fun camp(name: String, now: LocalDateTime, weather: Weather, season: Season = Season.of(now.toLocalDate())): String {
        val m = Moment(name, now, BudgetConfig(), DayState(now.toLocalDate()), emptyList(), weather = weather, season = season)
        val lines = buildList {
            when (weather) {
                Weather.RAIN -> add(tr("$name listens to the rain on the tent. Tap something to do together.", "$name az esőt hallgatja a sátron. Koppints valamire, és csináljátok együtt."))
                Weather.SNOW -> add(tr("$name is trying to catch snowflakes. Tap something to do together.", "$name hópelyheket próbál elkapni. Koppints valamire, és csináljátok együtt."))
                Weather.FOG -> add(tr("$name can't see the far trees this morning. Cosy. Tap something.", "$name ma reggel nem látja a távoli fákat. Hangulatos. Koppints valamire."))
                else -> {}
            }
            when (season) {
                Season.AUTUMN -> add(tr("$name kicks through the leaves. Tap something to do together.", "$name a leveleket rugdossa. Koppints valamire, és csináljátok együtt."))
                Season.WINTER -> add(tr("$name keeps the fire going. Tap something to do together.", "$name őrzi a tüzet. Koppints valamire, és csináljátok együtt."))
                Season.SPRING -> add(tr("$name found the first flowers. Tap something to do together.", "$name megtalálta az első virágokat. Koppints valamire, és csináljátok együtt."))
                Season.SUMMER -> add(tr("$name hums by the river. Tap something to do together.", "$name dúdol a folyóparton. Koppints valamire, és csináljátok együtt."))
            }
            add(tr("Tap something: $name will use it. Hold it to move it.", "Koppints valamire: $name használni fogja. Tartsd nyomva, ha arrébb tennéd."))
        }
        return pick(m, "camp", lines)
    }

    /** Little things the companion says now and then, just because. */
    private fun quirks(m: Moment): List<String> = listOf(
        tr("I counted the acorns. Then I lost count. Then I ate one.", "Megszámoltam a makkokat. Aztán elvétettem. Aztán megettem egyet."),
        tr("I named a cloud after you. It's a good cloud.", "Elneveztem rólad egy felhőt. Jó kis felhő."),
        tr("Did you know squirrels forget most of their acorns? Same.", "Tudtad, hogy a mókusok a makkjaik nagy részét elfelejtik? Én is."),
        if (m.now.dayOfWeek == DayOfWeek.MONDAY) tr("Mondays are better by the fire. ${Tasks.next(m.day, m.selected)?.title ?: "Sit with me"}?", "Hétfőn jobb a tűz mellett. ${Tasks.next(m.day, m.selected)?.title ?: "Ülj ide mellém"}?") else tr("I practised my campfire song. Want to hear it later?", "Gyakoroltam a tábortűzi dalomat. Meghallgatod később?"),
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
        2 -> tr("${parts[0]} and ${parts[1]}", "${parts[0]} és ${parts[1]}")
        else -> parts.dropLast(1).joinToString(", ") + tr(" and ", " és ") + parts.last()
    }

    /** Good days in a row (see [Streak]: one missed day a week doesn't end it). */
    fun streakOf(days: List<DaySummary>, today: java.time.LocalDate): Streak.State =
        Streak.of(days.filter { it.tasksDone > 0 || it.done.isNotEmpty() }.map { it.date }.toSet(), today)

    fun streak(days: List<DaySummary>, today: java.time.LocalDate): Int = streakOf(days, today).days
}
