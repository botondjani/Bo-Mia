package app.tend.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Friends: added with an invite (a link, a QR code or a 6-letter code), seen with their name,
 * companion, level and camp. All calls need an account; each returns null when it can't reach the
 * server, so the UI can say so.
 */
object Friends {
    data class Friend(
        val id: String, val name: String, val companion: String, val level: Int, val placed: Map<Int, String>,
        val week: String = "", val weekCount: Int = 0, val bridge: Boolean = false,
    )

    /** The web page that opens tend on an invite (and sends people without tend to Google Play). */
    fun inviteLink(code: String) = "https://botondjani.github.io/Bo-Mia/i/?c=$code"

    /** A code from a link (tend://invite?c=…, or the web page's ?c=…) or as typed; null when it isn't one. */
    fun codeFrom(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val raw = Regex("[?&]c=([A-Za-z0-9]{6})").find(text)?.groupValues?.get(1) ?: text.trim()
        return raw.uppercase().takeIf { Regex("^[A-Z2-9]{6}$").matches(it) }
    }

    /** Your invite code; [fresh] replaces it (old links stop working). */
    suspend fun myInvite(c: Context, fresh: Boolean = false): String? =
        Account.rpc(c, "tend_my_invite", JSONObject().put("p_new", fresh))?.takeIf { it.ok }?.body?.trim('"')?.takeIf { it.length == 6 }

    /** Who a code belongs to, before adding them; null when unknown (or your own). */
    suspend fun owner(c: Context, code: String): Friend? {
        val reply = Account.rpc(c, "tend_invite_owner", JSONObject().put("p_code", code))?.takeIf { it.ok } ?: return null
        return parse(reply.body).firstOrNull()
    }

    /** Adds the code's owner as a friend; true when you're friends now. */
    suspend fun accept(c: Context, code: String): Boolean {
        val reply = Account.rpc(c, "tend_accept_invite", JSONObject().put("p_code", code)) ?: return false
        return reply.ok && reply.body.trim() != "null"
    }

    /** Your friends, oldest friendship first; null when the server can't be reached. */
    suspend fun list(c: Context): List<Friend>? {
        val reply = Account.rpc(c, "tend_friends")?.takeIf { it.ok } ?: return null
        return parse(reply.body)
    }

    suspend fun remove(c: Context, id: String): Boolean =
        Account.rpc(c, "tend_remove_friend", JSONObject().put("p_friend", id))?.ok == true

    private fun parse(body: String): List<Friend> = runCatching {
        val a = JSONArray(body)
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Friend(
                o.optString("user_id"), o.optString("name").ifBlank { "A friend" }, o.optString("companion", "BO"),
                o.optInt("level", 1), app.tend.core.Clearing.parse(o.optString("placed")),
                o.optString("week"), o.optInt("week_count"), o.optBoolean("bridge"),
            )
        }
    }.getOrDefault(emptyList())

    // ---- Together ----

    /** This week's together goal: who added how much (you first), the target, and whether it's reached. */
    class Week(val parts: List<Pair<Friend, Int>>, val target: Int) {
        val total: Int get() = parts.sumOf { it.second }
        val done: Boolean get() = total >= target
    }

    /** The together week from [me] and [friends]; a friend's count only counts in the same week. */
    fun week(me: Friend, friends: List<Friend>): Week {
        val parts = listOf(me to me.weekCount) + friends.map { it to if (it.week == me.week) it.weekCount else 0 }
        return Week(parts, app.tend.core.Together.target(parts.size))
    }

    /** Yourself, as friends see you: for the river and the together week. */
    fun me(c: Context): Friend {
        val store = TendStore(c)
        val today = java.time.LocalDate.now()
        return Friend(
            "", Account.user(c)?.name?.ifBlank { null } ?: store.avatar.displayName, store.avatar.name,
            app.tend.core.Progress.level(store.totalXp()).level, store.placed,
            app.tend.core.Progress.weekKey(today), app.tend.core.Together.activities(today, store.daySummaries()), store.bridgeBuilt,
        )
    }

    /** When the week's goal is reached for the first time: acorns, and the bridge if it isn't built. True then. */
    fun claimWeek(c: Context, week: Week, key: String): Boolean {
        val store = TendStore(c)
        if (!week.done || week.parts.size < 2 || key in store.togetherDone) return false
        store.togetherDone = store.togetherDone + key
        store.addAcorns(app.tend.core.Together.ACORNS, "together")
        return true
    }

    // ---- Notes: a wave, a visit, toilet paper ----

    /** What friends do to each other's camps. */
    enum class Kind(val id: String) {
        WAVE("wave"), VISIT("visit"), TP("tp");

        /** How it reads for whoever got it: "Anna waved at you". */
        fun received(name: String) = when (this) {
            WAVE -> "$name waved at you"
            VISIT -> "$name visited your camp"
            TP -> "$name toilet-papered your oak"
        }

        /** For the widget's small label: "Anna: visited". */
        val short: String get() = when (this) { WAVE -> "waved"; VISIT -> "visited"; TP -> "toilet paper!" }

        companion object { fun of(id: String) = entries.firstOrNull { it.id == id } }
    }

    data class Note(val id: Long, val from: String, val name: String, val companion: String, val kind: Kind, val at: Long, val cleared: Boolean = false)

    /** Sends [kind] to a friend; true when it went (false: not friends, already sent, or offline). */
    suspend fun send(c: Context, to: String, kind: Kind): Boolean {
        val reply = Account.rpc(c, "tend_send_note", JSONObject().put("p_to", to).put("p_kind", kind.id)) ?: return false
        return reply.ok && reply.body.trim() == "true"
    }

    /** What you did to friends' camps: kinds used in the last day, and whose oak still has your paper. */
    class Sent(val used: Map<String, Set<Kind>>, val paperUp: Set<String>)

    suspend fun sent(c: Context): Sent? {
        val reply = Account.rpc(c, "tend_sent")?.takeIf { it.ok } ?: return null
        return runCatching {
            val a = JSONArray(reply.body)
            val rows = (0 until a.length()).mapNotNull { i -> a.getJSONObject(i).let { o -> Kind.of(o.optString("kind"))?.let { Triple(o.optString("to_id"), it, o.optBoolean("cleared")) } } }
            Sent(rows.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }, rows.filter { it.second == Kind.TP && !it.third }.map { it.first }.toSet())
        }.getOrNull()
    }

    /**
     * Fetches what friends did to your camp and keeps it for the clearing page and the widget; tells
     * you with a notification when something new happened. True when something new came.
     */
    suspend fun syncInbox(c: Context): Boolean {
        if (Account.user(c) == null) return false
        val reply = Account.rpc(c, "tend_inbox")?.takeIf { it.ok } ?: return false
        val notes = parseNotes(reply.body)
        val p = prefs(c)
        val known = p.getLong("notes_newest", 0)
        p.edit().putString("notes", reply.body).putLong("notes_newest", maxOf(known, notes.maxOfOrNull { it.id } ?: 0)).apply()
        // Paper taken down while offline: tell the server now.
        if (notes.any { it.kind == Kind.TP && !it.cleared && it.id <= p.getLong("paper_down", 0) }) Account.rpc(c, "tend_clear_paper")
        val fresh = notes.filter { it.id > known && System.currentTimeMillis() - it.at < 6 * 3_600_000L }
        if (fresh.isNotEmpty()) notify(c, fresh)
        return notes.any { it.id > known }
    }

    /** Notes kept from the last sync, newest first. */
    fun notes(c: Context): List<Note> = parseNotes(prefs(c).getString("notes", null) ?: "[]")

    /** Who toilet-papered your oak (paper still up), newest first; empty when it's clean. */
    fun paperOnMe(c: Context): List<Note> = notes(c).filter { it.kind == Kind.TP && !it.cleared && it.id > prefs(c).getLong("paper_down", 0) }

    /** Takes the paper down: at once on this phone, and on the server when it can be reached. */
    suspend fun clearPaper(c: Context) {
        val newest = paperOnMe(c).maxOfOrNull { it.id } ?: return
        prefs(c).edit().putLong("paper_down", newest).apply()
        app.tend.widget.Widgets.refresh(c)
        Account.rpc(c, "tend_clear_paper")
    }

    /** Rubber ducks in your river: one for each visit in the last week that you haven't fished out. */
    fun ducks(c: Context): List<Note> {
        val caught = prefs(c).getStringSet("ducks_caught", emptySet())!!
        return notes(c).filter { it.kind == Kind.VISIT && it.id.toString() !in caught }
    }

    /** Fishes a duck out of the river; it joins the collection. */
    fun catchDuck(c: Context, note: Note) {
        val p = prefs(c)
        val caught = p.getStringSet("ducks_caught", emptySet())!!
        if (note.id.toString() in caught) return
        // Only ids still in the inbox are worth remembering.
        val live = notes(c).map { it.id.toString() }.toSet()
        p.edit().putStringSet("ducks_caught", caught.filter { it in live }.toSet() + note.id.toString()).apply()
        TendStore(c).ducksCaught += 1
        app.tend.widget.Widgets.refresh(c)
    }

    /** The newest note not yet seen on the clearing page, from the last day; for the widget. */
    fun unseen(c: Context, now: Long = System.currentTimeMillis()): Note? =
        notes(c).firstOrNull()?.takeIf { it.id > prefs(c).getLong("notes_seen", 0) && now - it.at < DAY_MS }

    fun markSeen(c: Context) {
        val newest = notes(c).maxOfOrNull { it.id } ?: return
        if (newest > prefs(c).getLong("notes_seen", 0)) {
            prefs(c).edit().putLong("notes_seen", newest).apply()
            app.tend.widget.Widgets.refresh(c)
        }
    }

    /** One notification for what's new: "Anna toilet-papered your oak" (and how many more). */
    private fun notify(c: Context, fresh: List<Note>) {
        if (!app.tend.reminder.RecapReminder.canNotify(c)) return
        val nm = c.getSystemService(android.app.NotificationManager::class.java)
        nm.createNotificationChannel(android.app.NotificationChannel(CHANNEL, "Friends", android.app.NotificationManager.IMPORTANCE_DEFAULT))
        val first = fresh.first()
        val text = when (first.kind) {
            Kind.TP -> "Tap the oak in your camp to unroll it."
            Kind.VISIT -> "They left a rubber duck in your river."
            Kind.WAVE -> "Wave back from your camp."
        } + if (fresh.size > 1) " And ${fresh.size - 1} more from friends." else ""
        val open = android.app.PendingIntent.getActivity(
            c, 21, android.content.Intent(c, app.tend.MainActivity::class.java).putExtra(app.tend.reminder.RecapReminder.EXTRA_OPEN, app.tend.reminder.WeeklyLetter.OPEN_CLEARING).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        nm.notify(
            NOTIFICATION_ID,
            android.app.Notification.Builder(c, CHANNEL).setSmallIcon(app.tend.R.drawable.ic_notification)
                .setContentTitle(first.kind.received(first.name)).setContentText(text)
                .setContentIntent(open).setAutoCancel(true).build(),
        )
    }

    private fun parseNotes(body: String): List<Note> = runCatching {
        val a = JSONArray(body)
        (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            val kind = Kind.of(o.optString("kind")) ?: return@mapNotNull null
            val at = runCatching { java.time.OffsetDateTime.parse(o.optString("created_at")).toInstant().toEpochMilli() }.getOrDefault(0L)
            Note(o.optLong("id"), o.optString("from_id"), o.optString("name").ifBlank { "A friend" }, o.optString("companion", "BO"), kind, at, o.optBoolean("cleared"))
        }
    }.getOrDefault(emptyList())

    private const val CHANNEL = "friends"
    private const val NOTIFICATION_ID = 31
    private const val DAY_MS = 24 * 3_600_000L

    // An invite opened before logging in waits here until there's an account.
    private fun prefs(c: Context) = c.getSharedPreferences(Account.PREFS_NAME, Context.MODE_PRIVATE)
    fun pendingCode(c: Context): String? = prefs(c).getString("pending_invite", null)
    fun setPendingCode(c: Context, code: String?) = prefs(c).edit().apply { if (code == null) remove("pending_invite") else putString("pending_invite", code) }.apply()
}
