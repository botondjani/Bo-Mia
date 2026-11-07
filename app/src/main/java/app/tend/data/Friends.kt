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

    // An invite opened before logging in waits here until there's an account.
    private fun prefs(c: Context) = c.getSharedPreferences(Account.PREFS_NAME, Context.MODE_PRIVATE)
    fun pendingCode(c: Context): String? = prefs(c).getString("pending_invite", null)
    fun setPendingCode(c: Context, code: String?) = prefs(c).edit().apply { if (code == null) remove("pending_invite") else putString("pending_invite", code) }.apply()
}
