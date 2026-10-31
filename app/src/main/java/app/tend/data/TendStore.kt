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
import app.tend.core.Garden
import app.tend.core.Plant
import app.tend.core.TaskDef
import app.tend.core.Tasks
import app.tend.core.WorthIt
import app.tend.widget.TendWidget
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Local persistence (SharedPreferences + two CSV logs). Every state change refreshes the widget.
 * Day rollover happens on read: yesterday's unused minutes go to the garden, everything else resets.
 */
class TendStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("tend", Context.MODE_PRIVATE)

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
            return ids.split(',').mapNotNull { known[it] }
        }

    /** User-made check-off activities, stored as "id|icon|title" lines. */
    private val customTasks: List<TaskDef>
        get() = prefs.getString("custom_tasks", "")!!.lines().mapNotNull { line ->
            val p = line.split('|')
            if (p.size == 3) Tasks.custom(p[2], LEGACY_EMOJI[p[1]] ?: p[1]).copy(id = p[0]) else null
        }

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
            putString("custom_tasks", customs.joinToString("\n") { "${it.id}|${it.icon}|${it.title}" })
            putString("tasks", next.joinToString(",") { it.id })
        }
    }

    /** The user went through App info → Allow restricted settings (or said it's already done). */
    var restrictedUnlocked: Boolean
        get() = prefs.getBoolean("restricted_unlocked", false)
        set(v) = edit { putBoolean("restricted_unlocked", v) }

    /** The first-run tour of the home screen has been shown (or skipped). */
    var tourDone: Boolean
        get() = prefs.getBoolean("tour_done", false)
        set(v) = edit { putBoolean("tour_done", v) }

    /** The one-time "there's a widget" tip was seen. */
    var widgetTipDone: Boolean
        get() = prefs.getBoolean("widget_tip_done", false)
        set(v) = edit { putBoolean("widget_tip_done", v) }

    /** Evening mode starts at this hour (until 5 am); -1 = off. */
    var eveningFrom: Int
        get() = prefs.getInt("evening_from", 22)
        set(v) = edit { putInt("evening_from", v) }

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

    val focusTask: TaskDef
        get() = Tasks.presets.firstOrNull { it.id == focusTaskId } ?: Tasks.READ

    /** One plant per closed day that had unused minutes, oldest first. */
    val plants: List<Plant>
        get() {
            today()
            return prefs.getString("plants", "")!!.split(',').filter { it.isNotEmpty() }.map { Plant.valueOf(it) }
        }

    /** Days closed since install: drives the weekly home growth. */
    val daysTended: Int get() { today(); return prefs.getInt("days_tended", 0) }

    /**
     * Today's state. At the first read of a new day, yesterday is closed: its unused minutes
     * become a plant, the home's day count goes up, everything else resets.
     */
    @Synchronized
    fun today(): DayState {
        val now = LocalDate.now()
        val stored = readDay()
        if (stored != null && stored.date == now) return stored
        val fresh = DayState(now)
        val plant = stored?.let { Garden.plantFor(Budget.gardenMinutesAtClose(config, it)) }
        val plants = prefs.getString("plants", "")!!
        stored?.let { d -> summary(d).let { append("history.csv", "${it.date},${it.usedMinutes},${it.unusedMinutes},${it.tasksDone},${it.done.joinToString(";")}") } }
        writeDay(fresh) {
            if (plant != null) putString("plants", if (plants.isEmpty()) plant.name else "$plants,${plant.name}")
            if (stored != null) putInt("days_tended", prefs.getInt("days_tended", 0) + 1)
        }
        return fresh
    }

    @Synchronized
    fun update(change: (DayState) -> DayState): DayState = change(today()).also { writeDay(it) }

    fun credit(taskId: String, minutes: Int, manual: Boolean) =
        update { Budget.credit(config, it, taskId, minutes, manual) }

    /** Short-lived permission to open a package after the delay screen. */
    fun grantPass(pkg: String) = edit { putString("pass_pkg", pkg); putLong("pass_at", System.currentTimeMillis()) }

    fun consumePass(pkg: String): Boolean {
        val ok = prefs.getString("pass_pkg", null) == pkg &&
            System.currentTimeMillis() - prefs.getLong("pass_at", 0) < PASS_VALID_MS
        if (ok) edit { remove("pass_pkg") }
        return ok
    }

    /** Answers to "What are you looking for?" and "Worth it?" — source of the weekly patterns. */
    fun logAnswer(kind: String, pkg: String, answer: String, sessionSeconds: Long = 0) =
        append("answers.csv", "${LocalDateTime.now()},$kind,$pkg,$answer,$sessionSeconds")

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
    fun daySummaries(): List<DaySummary> = lines("history.csv").mapNotNull { line ->
        val p = line.split(',')
        runCatching {
            DaySummary(LocalDate.parse(p[0]), p[1].toInt(), p[2].toInt(), p[3].toInt(), p.getOrNull(4)?.split(';')?.filter { it.isNotEmpty() }?.toSet().orEmpty())
        }.getOrNull()
    } + summary(today())

    /**
     * What the companion wears now (habits held over the last 7 days), and what is on the shelf:
     * earned before, waiting for the habit to come back.
     */
    fun accessories(): Pair<Set<Accessory>, Set<Accessory>> {
        val today = LocalDate.now()
        val byDate = daySummaries().associateBy { it.date }
        val week = (6 downTo 0).map { byDate[today.minusDays(it.toLong())]?.done.orEmpty() }
        val worn = Accessories.active(week)
        val seen = prefs.getStringSet("acc_seen", emptySet())!!
        if (!seen.containsAll(worn.map { it.name })) prefs.edit().putStringSet("acc_seen", seen + worn.map { it.name }).apply()
        val shelf = Accessory.entries.filter { it.name in seen && it !in worn }.toSet()
        return worn to shelf
    }

    private fun summary(d: DayState) = DaySummary(
        d.date, ((d.usedSeconds + 59) / 60).toInt(), Budget.gardenMinutesAtClose(config, d), d.completedTasks.size, d.completedTasks,
    )

    private fun lines(name: String): List<String> =
        File(context.filesDir, name).takeIf { it.exists() }?.readLines().orEmpty()

    /** Evening recap reminder. */
    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder_on", false)
        set(v) = edit { putBoolean("reminder_on", v) }

    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 21)
        set(v) = edit { putInt("reminder_hour", v) }

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
        private const val PASS_VALID_MS = 15_000L

        /** Own activities saved with an emoji before tend had its own icons. */
        private val LEGACY_EMOJI = mapOf(
            "🎸" to "music", "🎹" to "music", "🧘" to "sun", "✍️" to "pen", "🐶" to "paw",
            "🌱" to "sprout", "🧩" to "star", "🚲" to "bike",
        )
    }
}
