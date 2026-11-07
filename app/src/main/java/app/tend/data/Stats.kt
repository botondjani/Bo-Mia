package app.tend.data

import android.content.Context
import app.tend.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.UUID

/**
 * Opt-in, anonymous usage counts: a random install id, the day since install, an event name and a
 * few small props. No names, no app names, no health data. Until the user answers, events wait in a
 * small file on the phone; "no" deletes them and stops logging, "yes" sends them in batches.
 */
object Stats {
    private const val URL_RPC = "${Supabase.URL_BASE}/rest/v1/rpc/tend_log"
    private const val KEY = Supabase.KEY
    private const val MAX_QUEUED = 300
    const val PREFS = "tend_stats"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun queue(c: Context) = File(c.filesDir, "stats_queue.jsonl")

    /** null: not asked yet. */
    fun consent(c: Context): Boolean? = prefs(c).let { if (it.contains("consent")) it.getBoolean("consent", false) else null }

    fun setConsent(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("consent", on).apply()
        if (on) flush(c) else queue(c).delete()
    }

    @Synchronized
    fun log(c: Context, name: String, props: Map<String, Any> = emptyMap()) {
        if (consent(c) == false) return
        runCatching {
            val p = prefs(c)
            val today = LocalDate.now().toEpochDay()
            val first = p.getLong("first_day", -1).takeIf { it >= 0 } ?: today.also { p.edit().putLong("first_day", it).apply() }
            val id = p.getString("id", null) ?: UUID.randomUUID().toString().also { p.edit().putString("id", it).apply() }
            val e = JSONObject().put("install_id", id).put("name", name).put("day", (today - first).toInt())
                .put("props", JSONObject(props)).put("v", BuildConfig.VERSION_NAME)
                .put("flavor", if (BuildConfig.PLAY) "play" else "sideload").put("sdk", android.os.Build.VERSION.SDK_INT)
            val f = queue(c)
            if (f.exists() && f.length() > MAX_QUEUED * 400L) return // stuck offline for long: stop growing
            f.appendText(e.toString() + "\n")
        }
    }

    /** Once a day, when tend is opened. */
    fun appOpen(c: Context) {
        val today = LocalDate.now().toString()
        if (prefs(c).getString("open_day", "") == today) return
        prefs(c).edit().putString("open_day", today).apply()
        log(c, "app_open")
    }

    /** Sends what's queued, off the main thread; keeps it for next time if offline. */
    fun flush(c: Context) {
        if (consent(c) != true) return
        val app = c.applicationContext
        Thread {
            // Take the batch out under the lock, send without it (logging never waits on the network).
            val lines = synchronized(this) {
                val f = queue(app)
                val l = runCatching { f.readLines().filter { it.isNotBlank() }.takeLast(MAX_QUEUED) }.getOrDefault(emptyList())
                f.delete()
                l
            }
            if (lines.isEmpty()) return@Thread
            val code = runCatching {
                val body = JSONObject().put("events", JSONArray(lines.map { JSONObject(it) })).toString()
                val conn = URL(URL_RPC).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"; conn.doOutput = true
                conn.connectTimeout = 10_000; conn.readTimeout = 10_000
                conn.setRequestProperty("apikey", KEY)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toByteArray()) }
                conn.responseCode.also { conn.disconnect() }
            }.getOrDefault(0)
            // Offline or server trouble: put the batch back in front for next time. 4xx: bad data, dropped.
            if (code !in 200..499 && consent(app) == true) synchronized(this) {
                val f = queue(app)
                val newer = if (f.exists()) f.readText() else ""
                f.writeText(lines.joinToString("\n", postfix = "\n") + newer)
            }
        }.start()
    }
}
