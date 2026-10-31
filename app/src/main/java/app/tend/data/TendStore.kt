package app.tend.data

import android.content.Context
import app.tend.core.Budget
import app.tend.core.BudgetConfig
import app.tend.core.Avatar
import app.tend.core.DayState
import app.tend.core.Garden
import app.tend.core.Plant
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

    /** Today's "Worth it?" answers, for the evening recap. */
    fun worthItToday(): List<WorthIt> {
        val file = File(context.filesDir, "answers.csv").takeIf { it.exists() } ?: return emptyList()
        val today = LocalDate.now().toString()
        return file.readLines().mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size < 4 || parts[1] != "worth_it" || !parts[0].startsWith(today)) return@mapNotNull null
            runCatching { WorthIt(LocalDateTime.parse(parts[0]).toLocalTime(), parts[3]) }.getOrNull()
        }
    }

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
            steps = prefs.getLong("day_steps", 0),
            exerciseMinutes = prefs.getInt("day_exercise", 0),
        )
    }

    private fun writeDay(d: DayState, extra: android.content.SharedPreferences.Editor.() -> Unit = {}) = edit {
        putString("day_date", d.date.toString())
        putInt("day_earned", d.earnedMinutes)
        putInt("day_manual", d.manualEarnedMinutes)
        putLong("day_used", d.usedSeconds)
        putInt("day_opens", d.opensToday)
        putStringSet("day_tasks", d.completedTasks)
        putLong("day_steps", d.steps)
        putInt("day_exercise", d.exerciseMinutes)
        extra()
    }

    private fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        TendWidget.refresh(context)
    }

    companion object {
        private const val PASS_VALID_MS = 15_000L
    }
}
