package app.tend.data

import android.content.Context
import app.tend.core.Budget
import app.tend.core.BudgetConfig
import app.tend.core.Avatar
import app.tend.core.Accessories
import app.tend.core.Accessory
import app.tend.core.Answer
import app.tend.core.DayState
import app.tend.core.DaySummary
import app.tend.core.LockSchedule
import app.tend.core.Plant
import app.tend.core.TaskDef
import app.tend.core.Tasks
import kotlinx.coroutines.launch
import app.tend.core.WorthIt
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Local persistence (SharedPreferences + two CSV logs). Every state change refreshes the widget.
 * Day rollover happens on read: yesterday is closed (acorns, history), everything else resets.
 */
class TendStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("tend", Context.MODE_PRIVATE)

    init {
        // The last fetched weather, once per process: the camp can show it before the next fetch.
        if (app.tend.core.LiveWeather.fetchedAt == 0L) prefs.getString("weather_code", null)?.let { code ->
            runCatching { app.tend.core.Weather.valueOf(code) }.getOrNull()?.let { w ->
                app.tend.core.LiveWeather.current = w
                app.tend.core.LiveWeather.fetchedAt = prefs.getLong("weather_at", 0)
                app.tend.core.LiveWeather.sunrise = prefs.getFloat("sunrise", 6.5f)
                app.tend.core.LiveWeather.sunset = prefs.getFloat("sunset", 18.6f)
            }
        }
    }

    var avatar: Avatar
        get() = Avatar.valueOf(prefs.getString("avatar", Avatar.BO.name)!!)
        set(v) = edit { putString("avatar", v.name) }

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) = edit { putBoolean("onboarded", v) }

    /** The things the user would rather do than scroll, in their order (at most [Tasks.MAX_SELECTED]). */
    val selectedTasks: List<TaskDef>
        get() {
            val ids = prefs.getString("tasks", null)
                ?: return if (onboarded) Tasks.legacyDefault else emptyList() // onboarded before picking existed
            val known = (Tasks.presets + customTasks).associateBy { it.id }
            return Tasks.withRewards(config, ids.split(',').mapNotNull { known[it] })
        }

    /** User-made activities, stored as "id|icon|title|minutes" lines (minutes 0 or missing: tick off). */
    private val customTasks: List<TaskDef>
        get() = prefs.getString("custom_tasks", "")!!.lines().mapNotNull { line ->
            val p = line.split('|')
            if (p.size >= 3) Tasks.custom(p[2], LEGACY_EMOJI[p[1]] ?: p[1], p.getOrNull(3)?.toIntOrNull() ?: 0).copy(id = p[0]) else null
        }

    /** Activities have been picked (or the defaults taken) at least once. */
    val tasksChosen: Boolean get() = prefs.contains("tasks")

    /** The three to start with: a workout, a walk, reading. */
    fun useDefaultTasks() = edit { putString("tasks", Tasks.legacyDefault.joinToString(",") { it.id }) }

    /** Adds or removes a preset or custom activity; adding is refused beyond the limit. */
    fun toggleTask(task: TaskDef) {
        val current = selectedTasks
        val next = when {
            current.any { it.id == task.id } -> current.filterNot { it.id == task.id }
            current.size >= Tasks.MAX_SELECTED -> return
            else -> current + task
        }
        val customs = if (Tasks.isCustom(task)) {
            if (next.any { it.id == task.id }) customTasks.filterNot { it.id == task.id } + task
            else customTasks.filterNot { it.id == task.id }
        } else customTasks
        edit {
            putString("custom_tasks", customs.joinToString("\n") { "${it.id}|${it.icon}|${it.title}|${it.target}" })
            putString("tasks", next.joinToString(",") { it.id })
        }
    }

    /** The user went through App info → Allow restricted settings (or said it's already done). */
    var restrictedUnlocked: Boolean
        get() = prefs.getBoolean("restricted_unlocked", false)
        set(v) = edit { putBoolean("restricted_unlocked", v) }

    /** Health Connect has seen a workout lately; otherwise the workout runs as a phone-off timer. */
    var exerciseTracked: Boolean
        get() = prefs.getBoolean("exercise_tracked", false)
        set(v) = edit { putBoolean("exercise_tracked", v) }

    /** The date whose sleep was estimated from the screen, not read from a tracker. */
    var sleepEstimatedOn: String
        get() = prefs.getString("sleep_estimated_on", "") ?: ""
        set(v) = edit { putString("sleep_estimated_on", v) }

    /** When onboarding finished (0 for installs from before this was kept). */
    var onboardedAt: Long
        get() = prefs.getLong("onboarded_at", 0)
        set(v) = edit { putLong("onboarded_at", v) }

    /** "light" (default), "dark" or "system" (follow the phone). */
    var themeMode: String
        get() = prefs.getString("theme", "light") ?: "light"
        set(v) = edit { putString("theme", v) }

    /** Notifications: the 8:30 plan (and the hello after two quiet days). */
    var notesMorning: Boolean
        get() = prefs.getBoolean("notes_morning", true)
        set(v) = edit { putBoolean("notes_morning", v) }

    /** Notifications: "10 minutes left". */
    var notesNudges: Boolean
        get() = prefs.getBoolean("notes_nudges", true)
        set(v) = edit { putBoolean("notes_nudges", v) }

    /** The day tend was opened again after two days or more away: Bo says welcome back. */
    var welcomeBackOn: String
        get() = prefs.getString("welcome_back_on", "") ?: ""
        set(v) = edit { putString("welcome_back_on", v) }

    /** When "Not now" on the next steps lets them come back once (important ones only); MAX_VALUE: never. */
    var nextStepsAgainAt: Long
        get() = prefs.getLong("next_steps_again_at", 0)
        set(v) = edit { putLong("next_steps_again_at", v) }

    /** Last time tend was opened, for the "misses you" note. */
    var lastOpenedAt: Long
        get() = prefs.getLong("last_opened_at", 0)
        set(v) = edit { putLong("last_opened_at", v) }

    fun noteSentOn(kind: String): String = prefs.getString("note_$kind", "") ?: ""

    fun markNoteSent(kind: String, day: String) {
        edit { putString("note_$kind", day) }
        countNote()
    }

    fun notesSentToday(): Int =
        if (prefs.getString("notes_day", "") == LocalDate.now().toString()) prefs.getInt("notes_count", 0) else 0

    /** Every note that goes out counts toward the daily cap. */
    fun countNote() {
        val n = notesSentToday() + 1
        edit { putString("notes_day", LocalDate.now().toString()); putInt("notes_count", n) }
    }

    // ---- The clearing: acorns and things ----

    /** Acorns to spend. New users start with a few; old plants and logs are turned into acorns once. */
    val acorns: Int
        get() {
            if (!prefs.contains("acorns")) {
                val old = prefs.getString("plants", "")!!.split(',').filter { it.isNotEmpty() }
                    .mapNotNull { runCatching { Plant.valueOf(it) }.getOrNull() }
                prefs.edit().putInt("acorns", app.tend.core.Clearing.STARTING_ACORNS + app.tend.core.Clearing.acornsFor(old)).apply()
            }
            return prefs.getInt("acorns", 0)
        }

    /** Adds (or takes back) acorns and notes why, for "today you earned". */
    @Synchronized
    fun addAcorns(amount: Int, why: String, on: LocalDate = LocalDate.now(), refreshWidgets: Boolean = true) {
        val now = acorns
        if (refreshWidgets) edit { putInt("acorns", (now + amount).coerceAtLeast(0)) }
        else prefs.edit().putInt("acorns", (now + amount).coerceAtLeast(0)).apply()
        append("acorns.csv", "$on,$why,$amount")
    }

    /** Acorns earned today, by source ("activities", "not_now", ...); buying and selling aren't counted. */
    fun acornsToday(): Map<String, Int> = lines("acorns.csv").mapNotNull { line ->
        val p = line.split(',')
        if (p.size == 3 && p[0] == LocalDate.now().toString() && p[1] != "spent" && p[1] != "sold") p[1] to (p[2].toIntOrNull() ?: 0) else null
    }.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }

    /** What last night's day close gave (within budget, below your normal). */
    fun acornsLastNight(): Int = LocalDate.now().minusDays(1).toString().let { y ->
        lines("acorns.csv").sumOf { line -> line.split(',').takeIf { it.size == 3 && it[0] == y && it[1] == "day" }?.get(2)?.toIntOrNull() ?: 0 }
    }

    /** First steps already claimed (their acorns added). */
    var stepsClaimed: Set<app.tend.core.Step>
        get() = prefs.getStringSet("steps_claimed", emptySet())!!.mapNotNull { runCatching { app.tend.core.Step.valueOf(it) }.getOrNull() }.toSet()
        set(v) = edit { putStringSet("steps_claimed", v.map { it.name }.toSet()) }

    /** The things in the camp the companion has used (for challenges). */
    val usedThings: Set<String> get() = prefs.getStringSet("used_things", emptySet())!!.toSet() + (if (prefs.getBoolean("used_thing", false)) setOf("?") else emptySet())

    fun markUsed(thing: String) {
        if (thing !in usedThings) prefs.edit().putStringSet("used_things", usedThings - "?" + thing).apply()
    }

    /** An early night has been had at least once (for challenges). */
    val hadEarlyNight: Boolean get() = prefs.getString("early_night", "").orEmpty().isNotEmpty()

    /** An invite has been sent once (a first step). */
    var invited: Boolean
        get() = prefs.getBoolean("invited", false)
        set(v) { prefs.edit().putBoolean("invited", v).apply() }

    /** Claims a first step: its acorns, once. */
    fun claimStep(step: app.tend.core.Step) {
        if (step in stepsClaimed) return
        stepsClaimed = stepsClaimed + step
        addAcorns(step.acorns, "steps")
    }

    /** Today's visitor, still waiting to be greeted; null once greeted today. */
    fun visitorWaiting(today: LocalDate = LocalDate.now(), now: java.time.LocalTime = java.time.LocalTime.now()): app.tend.core.Visitor? {
        if (prefs.getString("visitor_greeted", "") == today.toString()) return null
        // Animals come in the morning: from sunrise until noon (then they're off again).
        val hour = now.hour + now.minute / 60f
        if (hour < app.tend.core.LiveWeather.sunrise || hour >= 12f) return null
        val y = daySummaries().firstOrNull { it.date == today.minusDays(1) }
        val good = y != null && (y.unusedMinutes > 0 || y.tasksDone > 0)
        return app.tend.core.Visitors.of(today, good, feeder = "bird_feeder" in placed.values)
    }

    /** Animals greeted so far, and how often. */
    val guestbook: Map<app.tend.core.Visitor, Int> get() = app.tend.core.Visitors.parse(prefs.getString("guestbook", "")!!)

    /** Greets today's visitor: into the guestbook, with a few acorns. */
    fun greetVisitor(today: LocalDate = LocalDate.now()): app.tend.core.Visitor? {
        val v = visitorWaiting(today) ?: return null
        val book = guestbook.toMutableMap().apply { this[v] = (this[v] ?: 0) + 1 }
        prefs.edit().putString("visitor_greeted", today.toString()).putString("guestbook", app.tend.core.Visitors.format(book)).apply()
        addAcorns(v.rarity.acorns, "visitor")
        return v
    }

    /** Where the companion stays in the camp: the spot of the last thing used (null: the usual place). */
    var restSpot: Int?
        get() = prefs.getInt("rest_spot", -1).takeIf { it >= 0 }
        set(v) { prefs.edit().putInt("rest_spot", v ?: -1).apply() }

    /** How many times "Worth it?" was asked today. */
    fun worthItAskedToday(): Int = if (prefs.getString("worth_asked_day", "") == LocalDate.now().toString()) prefs.getInt("worth_asked", 0) else 0

    fun markWorthItAsked() {
        prefs.edit().putString("worth_asked_day", LocalDate.now().toString()).putInt("worth_asked", worthItAskedToday() + 1).apply()
    }

    /** "Good night" pressed: the paused apps rest until this time (epoch ms); 0 when not. */
    var sleepUntil: Long
        get() = prefs.getLong("sleep_until", 0)
        set(v) { prefs.edit().putLong("sleep_until", v).apply() }

    /**
     * Good night: the paused apps rest until 7:00, the companion goes to bed in the camp, and an
     * early night (before 23:00, once a night) is worth a few acorns. Returns the acorns given.
     */
    fun goodNight(now: java.time.LocalDateTime = java.time.LocalDateTime.now()): Int {
        val wake = (if (now.hour < 7) now.toLocalDate() else now.toLocalDate().plusDays(1)).atTime(7, 0)
        sleepUntil = wake.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        // To bed: the shelter, if there's one.
        app.tend.core.Clearing.spots.filter { it.kind == app.tend.core.SpotKind.SHELTER }.firstOrNull { placed[it.id] != null }?.let { restSpot = it.id }
        val night = (if (now.hour < 7) now.toLocalDate().minusDays(1) else now.toLocalDate()).toString()
        if (now.hour in 18..22 && prefs.getString("early_night", "") != night) {
            prefs.edit().putString("early_night", night).apply()
            addAcorns(EARLY_NIGHT_ACORNS, "early_night")
            return EARLY_NIGHT_ACORNS
        }
        return 0
    }

    /** The lock in force now: a bedtime or work-hours schedule, or "Good night" until 7:00. */
    fun activeLock(): LockSchedule? =
        if (isPaused) null else LockSchedule.activeOf(lockSchedules) ?: LockSchedule.GOOD_NIGHT.takeIf { System.currentTimeMillis() < sleepUntil }

    /** Woken up before 7:00: the apps are back. */
    /** Up again before 7:00: the apps wake (not in strict mode). */
    fun wakeUp() { if (!isStrict) sleepUntil = 0 }

    /** Rubber ducks fished out of the river: one for each friend's visit. */
    var ducksCaught: Int
        get() = prefs.getInt("ducks_caught", 0)
        set(v) { prefs.edit().putInt("ducks_caught", v).apply() }

    /** What stands where: spot id to thing id. */
    var placed: Map<Int, String>
        get() {
            val saved = prefs.getString("placed", "")!!
            // Things no longer sold: refunded in full, once, and taken away.
            val retired = saved.split(',').mapNotNull { it.substringAfter(':', "").takeIf { id -> id in app.tend.core.Things.retired } }
            if (retired.isNotEmpty()) {
                val kept = app.tend.core.Clearing.parse(saved)
                prefs.edit().putString("placed", app.tend.core.Clearing.format(kept)).apply()
                addAcorns(retired.sumOf { app.tend.core.Things.retired.getValue(it) }, "sold", refreshWidgets = false)
                return kept
            }
            val now = app.tend.core.Clearing.parse(saved)
            // Things with their own spot, placed elsewhere before: moved there when it's free.
            val moved = now.entries.fold(now) { acc, (spot, id) ->
                val home = app.tend.core.Things.byId(id)?.spot
                if (home != null && home != spot && acc[home] == null) acc - spot + (home to id) else acc
            }
            if (moved != now) prefs.edit().putString("placed", app.tend.core.Clearing.format(moved)).apply()
            return moved
        }
        set(v) = edit { putString("placed", app.tend.core.Clearing.format(v)) }

    /** Buys [thingId] from the shop: it goes to a free spot of its kind, or swaps out the cheapest one there. */
    fun buy(thingId: String): Boolean {
        val thing = app.tend.core.Things.byId(thingId) ?: return false
        val spot = app.tend.core.Clearing.spotFor(placed, thing) ?: return false
        return buy(spot, thingId)
    }

    /** Where movable things were put (spot to scene fractions); others stand at their usual place. */
    val layout: Map<Int, Pair<Float, Float>>
        get() = app.tend.core.Clearing.parseLayout(prefs.getString("layout", "")!!, placed)

    private fun saveLayout(l: Map<Int, Pair<Float, Float>>) = edit { putString("layout", app.tend.core.Clearing.formatLayout(l)) }

    /** Puts the movable thing at [spot] at (x, y) on the meadow (kept to where it may stand); false when it can't move. */
    fun place(spot: Int, x: Float, y: Float): Boolean {
        val thing = placed[spot]?.let(app.tend.core.Things::byId)?.takeIf(app.tend.core.Clearing::movable) ?: return false
        saveLayout(layout + (spot to app.tend.core.Clearing.keepOnMeadow(thing.kind, x, y)))
        return true
    }

    /** Sells [thingId] wherever it stands. */
    fun sell(thingId: String) {
        placed.entries.firstOrNull { it.value == thingId }?.let { sell(it.key) }
    }

    /** Buys [thingId] for [spotId]; a thing already there is sold first. False when it doesn't fit or isn't affordable. */
    @Synchronized
    fun buy(spotId: Int, thingId: String): Boolean {
        val spot = app.tend.core.Clearing.spot(spotId) ?: return false
        val thing = app.tend.core.Things.byId(thingId) ?: return false
        if (thing.kind != spot.kind) return false
        // A season's things are sold only in that season.
        if (thing.season != null && thing.season != app.tend.core.Season.of(LocalDate.now()) && thingId !in placed.values) return false
        if (thing.season != null && !isPlus && thingId !in placed.values) return false // the season's own come with Plus
        // An event's things: only while it's on, and with Plus, like the seasons'.
        if (thing.event != null && (!thing.event.on(LocalDate.now()) || !isPlus) && thingId !in placed.values) return false
        val refund = placed[spotId]?.let(app.tend.core.Things::byId)?.let(app.tend.core.Clearing::sellValue) ?: 0
        if (acorns + refund < thing.price) return false
        if (refund > 0) addAcorns(refund, "sold")
        addAcorns(-thing.price, "spent")
        placed = placed + (spotId to thingId)
        return true
    }

    /** Sells what stands on [spotId] for half its price. */
    @Synchronized
    fun sell(spotId: Int) {
        val thing = placed[spotId]?.let(app.tend.core.Things::byId) ?: return
        addAcorns(app.tend.core.Clearing.sellValue(thing), "sold")
        val l = layout
        placed = placed - spotId
        if (spotId in l) saveLayout(l - spotId) // the next thing there starts at the usual place
    }

    /** A break from all limits until this moment (epoch ms); 0 = none. */
    var pausedUntil: Long
        get() = prefs.getLong("paused_until", 0)
        private set(v) = edit { putLong("paused_until", v) }

    /** Days that were (partly) on a break: no day-close acorns, left out of the week and the normal. */
    val pausedDays: Set<String> get() = prefs.getStringSet("paused_days", emptySet())!!.toSet()

    val isPaused: Boolean get() = System.currentTimeMillis() < pausedUntil

    /** A break for [days] days: today counts as the first, it ends at midnight after the last. */
    fun pause(days: Int) {
        if (isStrict) return
        val start = LocalDate.now()
        val end = start.plusDays(days.toLong())
        pausedUntil = end.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        edit { putStringSet("paused_days", pausedDays + (0 until days).map { start.plusDays(it.toLong()).toString() }) }
        Stats.log(context, "paused", mapOf("days" to days))
    }

    /** Back from a break early: the days not reached are no longer marked. */
    fun resume() {
        val today = LocalDate.now()
        edit { putStringSet("paused_days", pausedDays.filter { LocalDate.parse(it) <= today }.toSet()) }
        pausedUntil = 0
    }

    /** Your normal: minutes a day in the picked apps before tend; null until known. */
    var normalMinutes: Int?
        get() = prefs.getInt("normal_min", -1).takeIf { it >= 0 }
        set(v) = edit { putInt("normal_min", v ?: -1) }

    /**
     * Learns your normal once from the phone's own usage stats: the picked apps' daily average in
     * the 14 days before tend. Without usage access (or with no data) it tries again next time; if
     * it never works, the first week with tend becomes the normal (see [closeDayAcorns]).
     */
    fun learnNormal() {
        if (normalMinutes != null || blockedPackages.isEmpty()) return
        val until = onboardedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        val from = until - 14 * 86_400_000L
        val stats = runCatching {
            context.getSystemService(android.app.usage.UsageStatsManager::class.java).queryAndAggregateUsageStats(from, until)
        }.getOrNull()
        if (stats.isNullOrEmpty()) return
        normalMinutes = (blockedPackages.sumOf { stats[it]?.totalTimeInForeground ?: 0L } / 60_000L / 14).toInt()
    }

    /**
     * Each picked app's minutes a day: in the 14 days before tend, and in the last 7 (since tend, if
     * that's sooner). Most used before first; empty without usage access.
     */
    fun appMinutes(now: Long = System.currentTimeMillis()): List<Triple<String, Int, Int>> {
        val start = onboardedAt.takeIf { it > 0 } ?: return emptyList()
        val usm = context.getSystemService(android.app.usage.UsageStatsManager::class.java) ?: return emptyList()
        fun minutes(from: Long, until: Long) = runCatching { usm.queryAndAggregateUsageStats(from, until) }.getOrNull().orEmpty()
            .mapValues { it.value.totalTimeInForeground / 60_000.0 }
        val before = minutes(start - 14 * 86_400_000L, start)
        val from = maxOf(start, now - 7 * 86_400_000L)
        val days = ((now - from) / 86_400_000.0).coerceAtLeast(1.0)
        val after = minutes(from, now)
        return blockedPackages.map { Triple(it, ((before[it] ?: 0.0) / 14).toInt(), ((after[it] ?: 0.0) / days).toInt()) }
            .filter { it.second > 0 || it.third > 0 }
            .sortedByDescending { it.second }
    }

    /** At day close: within the budget, and below your normal (or, without one, a first week's average). */
    private fun closeDayAcorns(d: DayState) {
        if (d.date.toString() in pausedDays) return
        val used = ((d.usedSeconds + 59) / 60).toInt()
        val available = Budget.remainingMinutes(config, d) + used
        // Plain writes here: refreshing the widgets would read today() again before the new day is saved.
        val normal = normalMinutes ?: app.tend.core.Acorns.normalFrom(lines("history.csv").filter { it.substringBefore(',') !in pausedDays }.mapNotNull { it.split(',').getOrNull(1)?.toIntOrNull() })
            ?.also { prefs.edit().putInt("normal_min", it).apply() }
        val earned = app.tend.core.Acorns.atDayClose(used, available, normal)
        if (earned > 0) addAcorns(earned, "day", on = d.date, refreshWidgets = false)
    }

    /** The companion's level the user has already seen celebrated. */
    var levelSeen: Int
        get() = prefs.getInt("level_seen", 1)
        set(v) = edit { putInt("level_seen", v) }

    /** Weeks whose challenge was completed ("2026-W45"), each worth bonus XP. */
    var challengesDone: Set<String>
        get() = prefs.getStringSet("challenges_done", emptySet())!!.toSet()
        set(v) = edit { putStringSet("challenges_done", v) }

    /** Weeks whose together goal was reached with friends ("2026-W45"); the first one built the bridge. */
    var togetherDone: Set<String>
        get() = prefs.getStringSet("together_done", emptySet())!!.toSet()
        set(v) = edit { putStringSet("together_done", v) }

    val bridgeBuilt: Boolean get() = togetherDone.isNotEmpty()

    /** All XP: activities plus completed weekly challenges. */
    fun skillXp(): Map<app.tend.core.Skill, Int> = app.tend.core.Progress.skillXp(daySummaries())

    fun totalXp(): Int = skillXp().values.sum() + challengesDone.size * app.tend.core.Progress.XP_PER_CHALLENGE

    /** The Play review sheet was offered (once). */
    var reviewAsked: Boolean
        get() = prefs.getBoolean("review_asked", false)
        set(v) = edit { putBoolean("review_asked", v) }

    /** The first-run tour of the home screen has been shown (or skipped). */
    var tourDone: Boolean
        get() = prefs.getBoolean("tour_done", false)
        set(v) = edit { putBoolean("tour_done", v) }

    /** The one-time "there's a widget" tip was seen. */
    var widgetTipDone: Boolean
        get() = prefs.getBoolean("widget_tip_done", false)
        set(v) = edit { putBoolean("widget_tip_done", v) }

    /** Evening mode starts at this hour (until 5 am); -1 = off. */
    /** When the evening starts (any of [app.tend.core.Budget.EVENING_HOURS]): tonight's recap, its reminder and, if on, the sleepy pause. */
    var eveningFrom: Int
        get() = prefs.getInt("evening_from", 22).let { if (it < 0) 22 else it } // -1 was "evening mode off"
        set(v) = edit { putInt("evening_from", v) }

    /** In the evening the pause waits longer and the companion is sleepy. */
    var eveningPause: Boolean
        get() = prefs.getBoolean("evening_pause", prefs.getInt("evening_from", 22) >= 0)
        set(v) = edit { putBoolean("evening_pause", v) }

    /** A short note every Sunday evening about the week. */
    var weeklyLetter: Boolean
        get() = prefs.getBoolean("weekly_letter", true)
        set(v) = edit { putBoolean("weekly_letter", v) }

    var blockedPackages: Set<String>
        get() = prefs.getStringSet("blocked", emptySet())!!
        set(v) = edit { putStringSet("blocked", v) }

    var config: BudgetConfig
        get() = BudgetConfig(
            totalMinutes = prefs.getInt("cfg_total", BudgetConfig().totalMinutes),
            freeMinutes = prefs.getInt("cfg_free", BudgetConfig().freeMinutes),
        )
        set(v) = edit { putInt("cfg_total", v.totalMinutes); putInt("cfg_free", v.freeMinutes) }

    /** Start of the running reading session (wall clock), 0 when none. Survives the app being closed. */
    var focusStartedAt: Long
        get() = prefs.getLong("focus_started", 0)
        set(v) = edit { putLong("focus_started", v) }

    /** A phone-free session the phone interrupted (task id), shown once on the home screen. */
    var timerBroken: String
        get() = prefs.getString("timer_broken", "")!!
        set(v) = edit { putString("timer_broken", v) }

    /** Which timer task is running (read, learn or a phone-free one). */
    var focusTaskId: String
        get() = prefs.getString("focus_task", Tasks.READ.id)!!
        set(v) = edit { putString("focus_task", v) }

    /** Length of a plain timer ([Tasks.timer]) while one runs. */
    var focusTimerMinutes: Int
        get() = prefs.getInt("focus_timer_min", 25)
        set(v) = edit { putInt("focus_timer_min", v) }

    val focusTask: TaskDef
        get() = if (focusTaskId == Tasks.TIMER_ID) Tasks.timer(focusTimerMinutes) else selectedTasks.firstOrNull { it.id == focusTaskId } ?: Tasks.presets.firstOrNull { it.id == focusTaskId } ?: Tasks.READ

    /** Days closed since install. */
    val daysTended: Int get() { today(); return prefs.getInt("days_tended", 0) }

    /**
     * Today's state. At the first read of a new day, yesterday is closed: its acorns are counted,
     * it goes into the history, the day count goes up, everything else resets.
     */
    @Synchronized
    fun today(): DayState {
        val now = LocalDate.now()
        val stored = readDay()
        if (stored != null && stored.date == now) return stored
        val fresh = DayState(now)
        stored?.let { d -> closeDayAcorns(d) }
        stored?.let { d -> summary(d).let { Stats.log(context, "day_closed", mapOf("used_min" to it.usedMinutes, "unused_min" to it.unusedMinutes, "tasks" to it.tasksDone)); append("history.csv", "${it.date},${it.usedMinutes},${it.unusedMinutes},${it.tasksDone},${it.done.joinToString(";")}") } }
        writeDay(fresh) {
            if (stored != null) putInt("days_tended", prefs.getInt("days_tended", 0) + 1)
        }
        return fresh
    }

    @Synchronized
    fun update(change: (DayState) -> DayState): DayState {
        val before = today()
        return change(before).also { after ->
            writeDay(after)
            // Acorns follow today's activities (an undo takes them back), up to the daily cap.
            val delta = app.tend.core.Acorns.forActivities(after.completedTasks.size) - app.tend.core.Acorns.forActivities(before.completedTasks.size)
            if (delta != 0) addAcorns(delta, "activities")
            (after.completedTasks - before.completedTasks).forEach { id ->
                Stats.log(context, "activity_done", mapOf("task" to if (id.startsWith("custom:")) "custom" else id))
            }
            // Friends can cheer it: what was done last, in a few words.
            (after.completedTasks - before.completedTasks).lastOrNull()?.let { id ->
                val task = selectedTasks.firstOrNull { it.id == id } ?: Tasks.presets.firstOrNull { it.id == id }
                val line = task?.let(Tasks::doneLine) ?: "did something of their own"
                val ctx = context.applicationContext
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { runCatching { Friends.setDone(ctx, line) } }
            }
        }
    }

    fun credit(taskId: String, minutes: Int, manual: Boolean) =
        update { Budget.credit(config, it, taskId, minutes, manual) }

    // ---- Where the camp is, and a little about you (all optional) ----

    /** Asked once in the camp for a town (for those who set up tend before it was asked during setup). */
    var townAsked: Boolean
        get() = prefs.getBoolean("town_asked", false)
        set(v) = edit { putBoolean("town_asked", v) }

    /** The city whose weather the camp has (chosen by name; never the phone's location). */
    val weatherPlace: WeatherService.Place?
        get() = prefs.getString("weather_place", null)?.let { j ->
            runCatching { org.json.JSONObject(j).let { o -> WeatherService.Place(o.getString("name"), o.optString("area"), o.optString("country"), o.optString("cc"), o.getDouble("lat"), o.getDouble("lon")) } }.getOrNull()
        }

    fun setWeatherPlace(p: WeatherService.Place?) {
        edit {
            if (p == null) remove("weather_place")
            else putString("weather_place", org.json.JSONObject().put("name", p.name).put("area", p.area).put("country", p.country).put("cc", p.countryCode).put("lat", p.lat).put("lon", p.lon).toString())
            remove("weather_code"); remove("weather_at")
        }
        app.tend.core.LiveWeather.current = null
        app.tend.core.LiveWeather.fetchedAt = 0
        logProfile()
    }

    /** The fetched weather, kept for the widgets and the next start. */
    fun keepWeather(w: app.tend.core.Weather, sunrise: Float, sunset: Float) {
        val now = System.currentTimeMillis()
        app.tend.core.LiveWeather.current = w; app.tend.core.LiveWeather.fetchedAt = now
        app.tend.core.LiveWeather.sunrise = sunrise; app.tend.core.LiveWeather.sunset = sunset
        edit { putString("weather_code", w.name); putLong("weather_at", now); putFloat("sunrise", sunrise); putFloat("sunset", sunset) }
    }

    /** Age range ("25-34") and gender, if given; only ever counted (with consent), never shown. */
    var ageBand: String?
        get() = prefs.getString("age_band", null)
        set(v) { edit { if (v == null) remove("age_band") else putString("age_band", v) }; logProfile() }

    var gender: String?
        get() = prefs.getString("gender", null)
        set(v) { edit { if (v == null) remove("gender") else putString("gender", v) }; logProfile() }

    /** For the counts (sent only with consent): age range, gender and country, never the city. */
    private fun logProfile() = Stats.log(context, "profile", mapOf("age" to (ageBand ?: ""), "gender" to (gender ?: ""), "country" to (weatherPlace?.countryCode ?: "")))

    // ---- tend Plus ----

    /**
     * When the free Plus trial began: the first time this is asked (so everyone gets the full 14
     * days, people already using tend included).
     */
    val plusTrialStart: Long
        get() = prefs.getLong("plus_trial_start", 0).takeIf { it > 0 }
            ?: System.currentTimeMillis().also { prefs.edit().putLong("plus_trial_start", it).apply(); Stats.log(context, "trial_started") }

    /** Bought (to come with Google Play billing). */
    var plusPaid: Boolean
        get() = prefs.getBoolean("plus_paid", false)
        set(v) = edit { putBoolean("plus_paid", v) }

    val trialEndsAt: Long get() = plusTrialStart + PLUS_TRIAL_DAYS * 86_400_000L

    /** Plus is on: bought, still in the trial, or not sold yet (then everyone has it: nothing is locked that can't be bought). */
    val isPlus: Boolean get() = plusPaid || !PLUS_ON_SALE || System.currentTimeMillis() < trialEndsAt

    /** The free trial is counting down (only once Plus can be bought). */
    val plusTrialRunning: Boolean get() = PLUS_ON_SALE && !plusPaid && isPlus

    /** Days left of the trial, counting today (1 on its last day; 0 once it's over or bought). */
    val trialDaysLeft: Int
        get() = if (plusPaid) 0 else ((trialEndsAt - System.currentTimeMillis() + 86_399_999L) / 86_400_000L).toInt().coerceAtLeast(0)

    /** The trial ended and the end screen hasn't been shown yet. */
    var trialEndShown: Boolean
        get() = prefs.getBoolean("trial_end_shown", false)
        set(v) = edit { putBoolean("trial_end_shown", v) }

    /** The "a few days left" note went out. */
    var trialNoteSent: Boolean
        get() = prefs.getBoolean("trial_note_sent", false)
        set(v) = edit { putBoolean("trial_note_sent", v) }

    /** Test builds: move the trial's start ([daysAgo] days back; 0 restarts it), and forget the notes. */
    fun setTrialStart(daysAgo: Int) = edit {
        putLong("plus_trial_start", System.currentTimeMillis() - daysAgo * 86_400_000L)
        putBoolean("trial_end_shown", false); putBoolean("trial_note_sent", false)
    }

    /** The companion peeks in after a long stretch in a paused app (on unless turned off). */
    var peekOn: Boolean
        get() = prefs.getBoolean("peek_on", true)
        set(v) = edit { putBoolean("peek_on", v) }

    /** Who the last phone-free activity was done together with ("Anna and Ben"), for the celebration; read once. */
    var togetherWith: String
        get() = prefs.getString("together_with", "")!!
        set(v) = edit { putString("together_with", v) }

    // ---- Outfits ----

    /** The seasons' outfits bought so far (kept for good). */
    val outfits: Set<Accessory>
        get() = prefs.getStringSet("outfits", emptySet())!!.mapNotNull { n -> Accessory.entries.firstOrNull { it.name == n && it.outfit != null } }.toSet()

    /** The outfit being worn, if any. */
    val outfitWorn: Accessory?
        get() = prefs.getString("outfit_worn", null)?.let { n -> outfits.firstOrNull { it.name == n } }

    /** Buys this season's outfit (with Plus, for acorns) and puts it on. False when it can't be bought now. */
    fun buyOutfit(a: Accessory): Boolean {
        if (a.outfit == null || a in outfits) return false
        if (!isPlus || a.outfit != app.tend.core.Season.of(LocalDate.now()) || acorns < Accessory.OUTFIT_PRICE) return false
        addAcorns(-Accessory.OUTFIT_PRICE, "outfit")
        edit { putStringSet("outfits", outfits.map { it.name }.toSet() + a.name); putString("outfit_worn", a.name) }
        Stats.log(context, "outfit", mapOf("which" to a.name.lowercase()))
        return true
    }

    /** Puts on an outfit that's been bought, or (null) takes it off. */
    fun wearOutfit(a: Accessory?) = edit { if (a != null && a in outfits) putString("outfit_worn", a.name) else remove("outfit_worn") }

    // ---- Limits for single apps ----

    /** An app's own daily limit in minutes, on top of the shared budget (pkg to minutes). */
    val appLimits: Map<String, Int>
        get() = prefs.getString("app_limits", "")!!.split(';').mapNotNull {
            val (pkg, m) = it.split('=').takeIf { p -> p.size == 2 } ?: return@mapNotNull null
            m.toIntOrNull()?.takeIf { v -> v > 0 }?.let { v -> pkg to v }
        }.toMap()

    /** Sets (or with null, removes) [pkg]'s own limit; in strict mode only a tighter one. False when it wasn't allowed. */
    fun setAppLimit(pkg: String, minutes: Int?): Boolean {
        val now = appLimits[pkg]
        if (isStrict && (minutes == null || (now != null && minutes > now))) return false
        val next = if (minutes == null) appLimits - pkg else appLimits + (pkg to minutes)
        edit { putString("app_limits", next.entries.joinToString(";") { "${it.key}=${it.value}" }) }
        return true
    }

    /** Seconds in each paused app today. */
    fun appSecondsToday(): Map<String, Long> {
        if (prefs.getString("app_sec_day", "") != LocalDate.now().toString()) return emptyMap()
        return prefs.getString("app_sec", "")!!.split(';').mapNotNull {
            val (pkg, v) = it.split('=').takeIf { p -> p.size == 2 } ?: return@mapNotNull null
            v.toLongOrNull()?.let { s -> pkg to s }
        }.toMap()
    }

    fun addAppSeconds(pkg: String, seconds: Long) {
        if (seconds <= 0) return
        val now = appSecondsToday()
        val next = now + (pkg to (now[pkg] ?: 0) + seconds)
        // Plain write: this runs every second while an app is open, and the widgets don't show it.
        prefs.edit().putString("app_sec_day", LocalDate.now().toString()).putString("app_sec", next.entries.joinToString(";") { "${it.key}=${it.value}" }).apply()
    }

    /** [pkg] has used up its own limit today. */
    fun appLocked(pkg: String?): Boolean {
        if (!isPlus) return false // a Plus feature: without it, the limits rest (and come back with it)
        val limit = pkg?.let { appLimits[it] } ?: return false
        return (appSecondsToday()[pkg] ?: 0) >= limit * 60L
    }

    // ---- Strict mode ----

    /**
     * No shortcuts: no 1-minute looks, no 5 minutes for acorns, no waking the apps after good night,
     * no break; limits only get tighter. Turning it off takes effect the next day.
     */
    val isStrict: Boolean
        get() = isPlus && prefs.getBoolean("strict", false) && prefs.getLong("strict_ends", 0).let { it == 0L || System.currentTimeMillis() < it }

    /** Strict mode was turned off and ends at this moment (the coming midnight); 0 when it isn't ending. */
    val strictEndsAt: Long get() = if (isStrict) prefs.getLong("strict_ends", 0) else 0

    fun setStrict(on: Boolean) {
        if (on) edit { putBoolean("strict", true); putLong("strict_ends", 0) }
        else if (isStrict) {
            val midnight = LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            edit { putLong("strict_ends", midnight) }
        }
        Stats.log(context, "strict", mapOf("on" to on))
    }

    /** Out of minutes: today's one "5 minutes now" is still there, and there are acorns for it. */
    val canBorrow: Boolean
        get() = !isStrict && prefs.getString("borrow_on", "") != LocalDate.now().toString() && acorns >= app.tend.core.Acorns.BORROW

    /** Spends acorns on five uncounted minutes in [pkg], once a day. */
    fun borrow(pkg: String): Boolean {
        if (!canBorrow) return false
        addAcorns(-app.tend.core.Acorns.BORROW, "borrow")
        // A minute's grace to get the app open; the session itself ends after the five minutes.
        val until = System.currentTimeMillis() + (app.tend.core.Acorns.BORROW_MINUTES + 1) * 60_000L
        edit { putString("borrow_on", LocalDate.now().toString()); putString("borrow_pkg", pkg); putLong("borrow_until", until) }
        grantPass(pkg, app.tend.core.Acorns.BORROW_MINUTES)
        Stats.log(context, "borrowed", emptyMap())
        return true
    }

    /** [pkg] is open on borrowed minutes: they aren't counted and don't close it. */
    fun borrowing(pkg: String?): Boolean =
        pkg != null && prefs.getString("borrow_pkg", null) == pkg && System.currentTimeMillis() < prefs.getLong("borrow_until", 0)

    /** Short-lived permission to open a package after the delay screen. */
    /** [minutes] > 0: the open lasts that long, then tend checks in again ("Open for 5 min"). */
    fun grantPass(pkg: String, minutes: Int = 0) =
        edit { putString("pass_pkg", pkg); putLong("pass_at", System.currentTimeMillis()); putInt("pass_minutes", minutes) }

    /** The minutes of a fresh pass for [pkg] (0 = open-ended), or null when there is none. */
    fun consumePass(pkg: String): Int? {
        val ok = prefs.getString("pass_pkg", null) == pkg &&
            System.currentTimeMillis() - prefs.getLong("pass_at", 0) < PASS_VALID_MS
        if (!ok) return null
        edit { remove("pass_pkg") }
        return prefs.getInt("pass_minutes", 0)
    }

    /** "Quick look": open for one minute without the wait, a few times a day. */
    fun quickLooksLeft(): Int = if (isStrict) 0 else
        QUICK_LOOKS_PER_DAY - if (prefs.getString("quick_day", "") == LocalDate.now().toString()) prefs.getInt("quick_used", 0) else 0

    fun useQuickLook() {
        val used = QUICK_LOOKS_PER_DAY - quickLooksLeft() + 1
        edit { putString("quick_day", LocalDate.now().toString()); putInt("quick_used", used) }
    }

    /** Scheduled locks: bedtime and work hours, both off until turned on. */
    val lockSchedules: List<LockSchedule>
        get() = listOf(readLock("lock_bed", LockSchedule.BEDTIME), readLock("lock_work", LockSchedule.WORK))

    fun setLockSchedule(s: LockSchedule) =
        edit { putString(if (s.label == LockSchedule.BEDTIME.label) "lock_bed" else "lock_work", "${s.on}|${s.from}|${s.to}") }

    private fun readLock(key: String, default: LockSchedule): LockSchedule {
        val p = prefs.getString(key, null)?.split('|') ?: return default
        return runCatching { default.copy(on = p[0].toBoolean(), from = p[1].toInt(), to = p[2].toInt()) }.getOrDefault(default)
    }

    /** The background monitor was found stopped for a while (battery savers do that). */
    var monitorSlept: Boolean
        get() = prefs.getBoolean("monitor_slept", false)
        set(v) = edit { putBoolean("monitor_slept", v) }

    /** "Not now"s said today. */
    val notNowsToday: Int
        get() = if (prefs.getString("not_now_day", "") == LocalDate.now().toString()) prefs.getInt("not_now_count", 0) else 0

    /** Reels and Shorts closed (Plus, optional; Android's accessibility switch must be on too). */
    var shortsGuardOn: Boolean
        get() = prefs.getBoolean("shorts_guard", false)
        set(v) = edit { putBoolean("shorts_guard", v) }

    /** Reels and Shorts closed today. */
    val shortsClosedToday: Int
        get() = if (prefs.getString("shorts_day", "") == LocalDate.now().toString()) prefs.getInt("shorts_count", 0) else 0

    fun countShortsClosed() {
        val today = LocalDate.now().toString()
        edit { putString("shorts_day", today); putInt("shorts_count", shortsClosedToday + 1) }
    }

    /** How far you've come: days at the camp, activities done, hours got back (minutes left unused). */
    fun totals(): app.tend.core.Totals = daySummaries().let { days ->
        app.tend.core.Totals(daysTended, days.sumOf { it.tasksDone }, days.sumOf { it.unusedMinutes } / 60)
    }

    /** Milestones already celebrated in the camp. */
    var milestonesSeen: Set<app.tend.core.Milestone>
        get() = prefs.getStringSet("milestones_seen", emptySet())!!.mapNotNull { runCatching { app.tend.core.Milestone.valueOf(it) }.getOrNull() }.toSet()
        set(v) = edit { putStringSet("milestones_seen", v.map { it.name }.toSet()) }

    /** The week whose story card was put away (the date of its Sunday). */
    var weekStoryDone: String
        get() = prefs.getString("week_story_done", "") ?: ""
        set(v) = edit { putString("week_story_done", v) }

    /** The day Bo's quest was last claimed (its acorns added). */
    var questClaimedOn: String
        get() = prefs.getString("quest_claimed", "") ?: ""
        set(v) = edit { putString("quest_claimed", v) }

    /** Acorns the next "Not now" earns today (0 once today's are all earned). */
    val nextNotNowAcorns: Int
        get() {
            val n = if (prefs.getString("not_now_day", "") == LocalDate.now().toString()) prefs.getInt("not_now_count", 0) else 0
            return app.tend.core.Acorns.forNotNows(n + 1) - app.tend.core.Acorns.forNotNows(n)
        }

    /** Answers to "What are you looking for?" and "Worth it?" — source of the weekly patterns. */
    fun logAnswer(kind: String, pkg: String, answer: String, sessionSeconds: Long = 0) {
        Stats.log(context, kind, mapOf((if (kind == "pause") "outcome" else "answer") to answer))
        if (kind == "pause" && answer == "not_now") {
            val today = LocalDate.now().toString()
            val n = if (prefs.getString("not_now_day", "") == today) prefs.getInt("not_now_count", 0) else 0
            edit { putString("not_now_day", today); putInt("not_now_count", n + 1) }
            val delta = app.tend.core.Acorns.forNotNows(n + 1) - app.tend.core.Acorns.forNotNows(n)
            if (delta > 0) addAcorns(delta, "not_now")
        }
        append("answers.csv", "${LocalDateTime.now()},$kind,$pkg,$answer,$sessionSeconds")
    }

    /** Every case where blocking did not run: monitor stopped, or an open was not caught. */
    fun logMiss(reason: String, pkg: String = "") = append("misses.csv", "${LocalDateTime.now()},$reason,$pkg")

    /** All logged answers (looking-for and worth-it), oldest first. */
    fun answers(): List<Answer> = lines("answers.csv").mapNotNull { line ->
        val p = line.split(',')
        if (p.size < 4) null else runCatching { Answer(LocalDateTime.parse(p[0]), p[1], p[2], p[3]) }.getOrNull()
    }

    /** Today's "Worth it?" answers, for the evening recap. */
    fun worthItToday(): List<WorthIt> = answers()
        .filter { it.kind == "worth_it" && it.time.toLocalDate() == LocalDate.now() }
        .map { WorthIt(it.time.toLocalTime(), it.answer) }

    /** Closed days from the history log, plus today so far. */
    fun daySummaries(): List<DaySummary> {
        val paused = pausedDays
        return (lines("history.csv").mapNotNull { line ->
            val p = line.split(',')
            runCatching {
                DaySummary(LocalDate.parse(p[0]), p[1].toInt(), p[2].toInt(), p[3].toInt(), p.getOrNull(4)?.split(';')?.filter { it.isNotEmpty() }?.toSet().orEmpty())
            }.getOrNull()
        } + summary(today())).map { if (it.date.toString() in paused) it.copy(paused = true) else it }
    }

    /**
     * What the companion wears now (habits held over the last 7 days), and what is on the shelf:
     * earned before, waiting for the habit to come back.
     */
    /** Accessories worn now that weren't seen before: for "Bo got a lucky leaf!". */
    fun takeNewAccessories(): Set<Accessory> {
        val before = prefs.getStringSet("acc_seen", emptySet())!!
        return accessories().first.filter { it.name !in before }.toSet()
    }

    fun accessories(): Pair<Set<Accessory>, Set<Accessory>> {
        val today = LocalDate.now()
        val byDate = daySummaries().associateBy { it.date }
        // The last six closed days plus today as it stands, so a first activity earns the leaf right away.
        val week = (6 downTo 1).map { byDate[today.minusDays(it.toLong())]?.done.orEmpty() } + listOf(today().completedTasks)
        val earned = Accessories.active(week)
        val seen = prefs.getStringSet("acc_seen", emptySet())!!
        if (!seen.containsAll(earned.map { it.name })) prefs.edit().putStringSet("acc_seen", seen + earned.map { it.name }).apply()
        val shelf = Accessory.habits.filter { it.name in seen && it !in earned }.toSet()
        accessoriesEarned = earned
        return (Accessories.worn(earned, accessoriesOff) + listOfNotNull(outfitWorn)) to shelf
    }

    /** Earned right now (as of the last [accessories] read), worn or not. */
    var accessoriesEarned: Set<Accessory> = emptySet()
        private set

    /** Earned ones the user took off (or that gave their place on the head to another). */
    val accessoriesOff: Set<Accessory>
        get() = prefs.getStringSet("acc_off", emptySet())!!.mapNotNull { n -> Accessory.entries.firstOrNull { it.name == n } }.toSet()

    /** Puts an earned accessory on or takes it off. */
    fun toggleAccessory(a: Accessory) {
        accessories()
        edit { putStringSet("acc_off", Accessories.toggle(a, accessoriesEarned, accessoriesOff).map { it.name }.toSet()) }
    }

    private fun summary(d: DayState) = DaySummary(
        d.date, ((d.usedSeconds + 59) / 60).toInt(), Budget.unusedMinutesAtClose(config, d), d.completedTasks.size, d.completedTasks,
    )

    private fun lines(name: String): List<String> =
        File(context.filesDir, name).takeIf { it.exists() }?.readLines().orEmpty()

    /** Evening recap reminder. */
    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder_on", false)
        set(v) = edit { putBoolean("reminder_on", v) }

    /** The home screen asked once; don't ask again. */
    var reminderPrompted: Boolean
        get() = prefs.getBoolean("reminder_prompted", false)
        set(v) = edit { putBoolean("reminder_prompted", v) }

    /** Opened tonight's recap already: the reminder stays quiet. */
    var recapSeenOn: String
        get() = prefs.getString("recap_seen", "")!!
        set(v) = edit { putString("recap_seen", v) }

    fun missCount(): Int = File(context.filesDir, "misses.csv").takeIf { it.exists() }?.readLines()?.size ?: 0

    private fun append(name: String, line: String) {
        File(context.filesDir, name).appendText(line.replace("\n", " ") + "\n")
    }

    private fun readDay(): DayState? {
        val date = prefs.getString("day_date", null) ?: return null
        return DayState(
            date = LocalDate.parse(date),
            earnedMinutes = prefs.getInt("day_earned", 0),
            manualEarnedMinutes = prefs.getInt("day_manual", 0),
            usedSeconds = prefs.getLong("day_used", 0),
            opensToday = prefs.getInt("day_opens", 0),
            completedTasks = prefs.getStringSet("day_tasks", emptySet())!!,
            grants = prefs.getString("day_grants", "")!!.split(';').mapNotNull {
                it.split('=').takeIf { p -> p.size == 2 }?.let { (k, v) -> v.toIntOrNull()?.let { m -> k to m } }
            }.toMap(),
            steps = prefs.getLong("day_steps", 0),
            exerciseMinutes = prefs.getInt("day_exercise", 0),
            sleepMinutes = prefs.getInt("day_sleep", 0),
        )
    }

    private fun writeDay(d: DayState, extra: android.content.SharedPreferences.Editor.() -> Unit = {}) = edit {
        putString("day_date", d.date.toString())
        putInt("day_earned", d.earnedMinutes)
        putInt("day_manual", d.manualEarnedMinutes)
        putLong("day_used", d.usedSeconds)
        putInt("day_opens", d.opensToday)
        putStringSet("day_tasks", d.completedTasks)
        putString("day_grants", d.grants.entries.joinToString(";") { "${it.key}=${it.value}" })
        putLong("day_steps", d.steps)
        putInt("day_exercise", d.exerciseMinutes)
        putInt("day_sleep", d.sleepMinutes)
        extra()
    }

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        app.tend.widget.Widgets.refresh(context)
    }

    companion object {
        const val EARLY_NIGHT_ACORNS = 5
        const val PLUS_TRIAL_DAYS = 14
        /** Whether Plus can be bought (Google Play billing). Until it can, it's on for everyone and no trial counts down. */
        const val PLUS_ON_SALE = false
        private const val PASS_VALID_MS = 15_000L
        const val QUICK_LOOKS_PER_DAY = 5

        /** Own activities saved with an emoji before tend had its own icons. */
        private val LEGACY_EMOJI = mapOf(
            "🎸" to "music", "🎹" to "music", "🧘" to "sun", "✍️" to "pen", "🐶" to "paw",
            "🌱" to "sprout", "🧩" to "star", "🚲" to "bike",
        )
    }
}
