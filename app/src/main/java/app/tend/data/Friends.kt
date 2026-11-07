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
    data class Friend(val id: String, val name: String, val companion: String, val level: Int, val placed: Map<Int, String>)

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
            )
        }
    }.getOrDefault(emptyList())

    // ---- Notes: a wave, a gift, a prank ----

    /** What friends can send each other. Pranks sit in the friend's camp for a day. */
    enum class Kind(val id: String, val label: String, val prank: Boolean = false) {
        WAVE("wave", "Wave"),
        GIFT("gift", "Gift $GIFT_ACORNS acorns"),
        DUCK("duck", "Rubber duck", prank = true),
        GNOME("gnome", "Garden gnome", prank = true),
        FLAMINGO("flamingo", "Plastic flamingo", prank = true),
        TP("tp", "Toilet paper", prank = true);

        /** How it reads for whoever got it: "Anna waved". */
        fun received(name: String) = when (this) {
            WAVE -> "$name waved at you"
            GIFT -> "$name sent you $GIFT_ACORNS acorns"
            DUCK -> "$name put a rubber duck in your river"
            GNOME -> "$name snuck a garden gnome into your camp"
            FLAMINGO -> "$name planted a plastic flamingo in your camp"
            TP -> "$name toilet-papered your oak"
        }

        /** For the widget's small label: "Anna: a rubber duck". */
        val short: String get() = when (this) {
            WAVE -> "waved"; GIFT -> "+$GIFT_ACORNS acorns"; DUCK -> "a rubber duck"
            GNOME -> "a gnome"; FLAMINGO -> "a flamingo"; TP -> "toilet paper!"
        }

        companion object { fun of(id: String) = entries.firstOrNull { it.id == id } }
    }

    data class Note(val id: Long, val from: String, val name: String, val companion: String, val kind: Kind, val at: Long)

    /** Sends [kind] to a friend; true when it went (false: not friends, already sent today, or offline). */
    suspend fun send(c: Context, to: String, kind: Kind): Boolean {
        val reply = Account.rpc(c, "tend_send_note", JSONObject().put("p_to", to).put("p_kind", kind.id)) ?: return false
        return reply.ok && reply.body.trim() == "true"
    }

    /** What you sent in the last day, as friend id to kinds; null when offline. */
    suspend fun sent(c: Context): Map<String, Set<Kind>>? {
        val reply = Account.rpc(c, "tend_sent")?.takeIf { it.ok } ?: return null
        return runCatching {
            val a = JSONArray(reply.body)
            (0 until a.length()).mapNotNull { i -> a.getJSONObject(i).let { o -> Kind.of(o.optString("kind"))?.let { o.optString("to_id") to it } } }
                .groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        }.getOrNull()
    }

    /**
     * Fetches notes from friends (last 7 days) and keeps them for the clearing page and the
     * widget; gifts not seen before are added to the acorns. True when something new came.
     */
    suspend fun syncInbox(c: Context): Boolean {
        if (Account.user(c) == null) return false
        val reply = Account.rpc(c, "tend_inbox")?.takeIf { it.ok } ?: return false
        val notes = parseNotes(reply.body)
        val p = prefs(c)
        val known = p.getLong("notes_newest", 0)
        p.edit().putString("notes", reply.body).putLong("notes_newest", maxOf(known, notes.maxOfOrNull { it.id } ?: 0)).apply()
        // Gifts count once: the newest gift id credited is kept with the acorns (and so in the backup).
        val store = TendStore(c)
        val credited = store.giftsUpTo
        val gifts = notes.filter { it.kind == Kind.GIFT && it.id > credited }
        if (gifts.isNotEmpty()) {
            store.addAcorns(GIFT_ACORNS * gifts.size, "gift")
            store.giftsUpTo = gifts.maxOf { it.id }
        }
        return notes.any { it.id > known }
    }

    /** Notes kept from the last sync, newest first. */
    fun notes(c: Context): List<Note> = parseNotes(prefs(c).getString("notes", null) ?: "[]")

    /** Pranks in your own camp right now (sent in the last day). */
    fun pranksOnMe(c: Context, now: Long = System.currentTimeMillis()): Set<Kind> =
        notes(c).filter { it.kind.prank && now - it.at < DAY_MS }.map { it.kind }.toSet()

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

    private fun parseNotes(body: String): List<Note> = runCatching {
        val a = JSONArray(body)
        (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            val kind = Kind.of(o.optString("kind")) ?: return@mapNotNull null
            val at = runCatching { java.time.OffsetDateTime.parse(o.optString("created_at")).toInstant().toEpochMilli() }.getOrDefault(0L)
            Note(o.optLong("id"), o.optString("from_id"), o.optString("name").ifBlank { "A friend" }, o.optString("companion", "BO"), kind, at)
        }
    }.getOrDefault(emptyList())

    const val GIFT_ACORNS = 10
    private const val DAY_MS = 24 * 3_600_000L

    // An invite opened before logging in waits here until there's an account.
    private fun prefs(c: Context) = c.getSharedPreferences(Account.PREFS_NAME, Context.MODE_PRIVATE)
    fun pendingCode(c: Context): String? = prefs(c).getString("pending_invite", null)
    fun setPendingCode(c: Context, code: String?) = prefs(c).edit().apply { if (code == null) remove("pending_invite") else putString("pending_invite", code) }.apply()
}
