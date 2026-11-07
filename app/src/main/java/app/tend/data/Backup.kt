package app.tend.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * A backup of what makes tend yours: settings, today, acorns and the camp (the "tend" preferences)
 * and the logs behind the history, levels and patterns. Not included: the background monitor's
 * moment-to-moment state, crash reports, the anonymous stats queue, the account itself, and no
 * health readings: today's steps, exercise and sleep stay on the phone (Health Connect fills them
 * again); only which activities were done is saved.
 */
object Backup {
    private const val VERSION = 1
    private val HEALTH_KEYS = setOf("day_steps", "day_exercise", "day_sleep")
    private val FILES = listOf("history.csv", "answers.csv", "acorns.csv")

    /** Everything to save, as one JSON object. */
    fun snapshot(c: Context): JSONObject {
        val prefs = JSONObject()
        c.getSharedPreferences("tend", Context.MODE_PRIVATE).all.filterKeys { it !in HEALTH_KEYS }.forEach { (key, value) ->
            val entry = when (value) {
                is String -> JSONObject().put("t", "s").put("v", value)
                is Int -> JSONObject().put("t", "i").put("v", value)
                is Long -> JSONObject().put("t", "l").put("v", value)
                is Boolean -> JSONObject().put("t", "b").put("v", value)
                is Float -> JSONObject().put("t", "f").put("v", value.toDouble())
                is Set<*> -> JSONObject().put("t", "set").put("v", JSONArray(value.map { it.toString() }))
                else -> null
            }
            entry?.let { prefs.put(key, it) }
        }
        val files = JSONObject()
        FILES.forEach { name -> File(c.filesDir, name).takeIf { it.exists() }?.let { files.put(name, it.readText()) } }
        return JSONObject().put("v", VERSION).put("prefs", prefs).put("files", files)
    }

    /** Replaces this phone's data with [data]. The caller restarts the app afterwards. */
    fun restore(c: Context, data: JSONObject) {
        val prefs = data.optJSONObject("prefs") ?: return
        val editor = c.getSharedPreferences("tend", Context.MODE_PRIVATE).edit().clear()
        prefs.keys().forEach { key ->
            val e = prefs.optJSONObject(key) ?: return@forEach
            when (e.optString("t")) {
                "s" -> editor.putString(key, e.optString("v"))
                "i" -> editor.putInt(key, e.optInt("v"))
                "l" -> editor.putLong(key, e.optLong("v"))
                "b" -> editor.putBoolean(key, e.optBoolean("v"))
                "f" -> editor.putFloat(key, e.optDouble("v").toFloat())
                "set" -> editor.putStringSet(key, e.optJSONArray("v")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet())
            }
        }
        editor.commit()
        val files = data.optJSONObject("files") ?: JSONObject()
        FILES.forEach { name ->
            val f = File(c.filesDir, name)
            if (files.has(name)) f.writeText(files.getString(name)) else f.delete()
        }
    }

    // ---- With the server ----

    private fun accountPrefs(c: Context) = c.getSharedPreferences(Account.PREFS_NAME, Context.MODE_PRIVATE)

    /** When the last backup went up (epoch ms), 0 for never. */
    fun lastBackupAt(c: Context): Long = accountPrefs(c).getLong("last_backup", 0)

    /** Sends a backup now; true when it's saved. */
    suspend fun upload(c: Context): Boolean {
        val args = JSONObject().put("p_data", snapshot(c)).put("p_version", app.tend.BuildConfig.VERSION_NAME)
        val ok = Account.rpc(c, "tend_save_backup", args)?.ok == true
        if (ok) accountPrefs(c).edit().putLong("last_backup", System.currentTimeMillis()).apply()
        return ok
    }

    /** At most once every 12 hours, quietly, when signed in. */
    suspend fun uploadIfDue(c: Context) {
        if (Account.user(c) == null) return
        if (System.currentTimeMillis() - lastBackupAt(c) < 12 * 3_600_000L) return
        upload(c)
    }

    class Saved(val data: JSONObject, val updatedAt: String)

    /** The account's backup on the server, or null (none, or offline). */
    suspend fun fetch(c: Context): Saved? {
        val reply = Account.rpc(c, "tend_load_backup") ?: return null
        if (!reply.ok) return null
        val row = runCatching { JSONArray(reply.body).optJSONObject(0) }.getOrNull() ?: return null
        val data = row.optJSONObject("data") ?: return null
        return Saved(data, row.optString("updated_at"))
    }
}
