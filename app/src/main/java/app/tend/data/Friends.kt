package app.tend.data

import app.tend.core.tr

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
        /** Where they moved things in their camp (spot to scene fractions). */
        val layout: Map<Int, Pair<Float, Float>> = emptyMap(),
        /** Phone-free right now: since and until when (epoch ms), and doing what (a task id); null when not. */
        val focusStarted: Long? = null,
        val focusUntil: Long? = null,
        val focusTask: String? = null,
        /** What they did last ("went for a walk") and when, within the last two days. */
        val lastDone: String? = null,
        val lastDoneAt: Long? = null,
        /** Pranks may land in their camp (they can turn them off). */
        val pranks: Boolean = true,
        /** You asked them to be your buddy ("asked", "yes"), and they asked you; null when not. */
        val buddyOut: String? = null,
        val buddyIn: String? = null,
    )

    /** A friend at your campfire: phone-free right now. */
    data class AtFire(val id: String, val name: String, val companion: String, val started: Long, val until: Long, val task: String)

    /** The web page that opens tend on an invite (and sends people without tend to Google Play). */
    fun inviteLink(code: String) = "https://tend.hu/i/?c=$code"

    /** A code from a link (tend://invite?c=…, or the web page's ?c=…) or as typed; null when it isn't one. */
    fun codeFrom(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val raw = Regex("[?&]c=([A-Za-z0-9]{6})").find(text)?.groupValues?.get(1) ?: text.trim()
        return raw.uppercase().takeIf { Regex("^[A-Z2-9]{6}$").matches(it) }
    }

    /** Your invite code (it changes by itself when you remove a friend, so old links stop working). */
    suspend fun myInvite(c: Context): String? =
        Account.rpc(c, "tend_my_invite", JSONObject().put("p_new", false))?.takeIf { it.ok }?.body?.trim('"')?.takeIf { it.length == 6 }

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

    /** Your friends, oldest friendship first; null when the server can't be reached. Who's at the fire is kept too. */
    suspend fun list(c: Context): List<Friend>? {
        val reply = Account.rpc(c, "tend_friends")?.takeIf { it.ok } ?: return null
        // How many sit at a fire anywhere (null while tend is too small for it to say nothing about anyone).
        fireCount = Account.rpc(c, "tend_fire_count")?.takeIf { it.ok }?.body?.trim()?.toIntOrNull()
        return parse(reply.body).also { keepAtFire(c, it); last = it; celebrate(c, it) }
    }

    /** People phone-free at a fire right now, anywhere; null until tend has 20 people a day. */
    @Volatile var fireCount: Int? = null
        private set

    /** This week's together goal with friends as last heard; null with no friends. */
    fun lastWeek(c: Context): Week? = last.takeIf { it.isNotEmpty() }?.let { week(me(c), it) }

    /** Removes a friend for good: they can't add you again, nor you them. */
    suspend fun block(c: Context, id: String): Boolean =
        Account.rpc(c, "tend_block", JSONObject().put("p_friend", id))?.ok == true

    /** Sends what friends may see (and whether pranks may land here). Quietly does nothing offline. */
    suspend fun saveSettings(c: Context) {
        if (Account.user(c) == null) return
        val s = TendStore(c)
        Account.rpc(c, "tend_save_settings", JSONObject().put("p_pranks", s.pranksOn).put("p_share_done", s.shareDone).put("p_share_level", s.shareLevel).put("p_share_fire", s.shareFire))
    }

    // ---- Buddies: a friend who's told when you slip ----

    /** How many buddies you can have: one, three with Plus. */
    fun buddiesMax(c: Context) = if (TendStore(c).isPlus) 3 else 1

    suspend fun buddyAsk(c: Context, to: String): Boolean =
        Account.rpc(c, "tend_buddy_ask", JSONObject().put("p_to", to).put("p_max", buddiesMax(c)))?.takeIf { it.ok }?.body?.trim() == "true"

    suspend fun buddyAnswer(c: Context, from: String, yes: Boolean): Boolean =
        Account.rpc(c, "tend_buddy_answer", JSONObject().put("p_from", from).put("p_yes", yes))?.ok == true

    suspend fun buddyLeave(c: Context, other: String): Boolean =
        Account.rpc(c, "tend_buddy_leave", JSONObject().put("p_other", other))?.ok == true

    /** Tells your buddies you slipped: [STRICT_OFF] or [OVER_3]. Once a day per reason (the server keeps to it). */
    suspend fun buddyAlert(c: Context, reason: String) {
        if (Account.user(c) == null) return
        Account.rpc(c, "tend_buddy_alert", JSONObject().put("p_reason", reason))
    }
    const val STRICT_OFF = "strict_off"
    const val OVER_3 = "over_3"

    /**
     * Over the day's minutes on 3 days this week: your buddies hear of it, once a week. Checked
     * from the background; quietly does nothing offline.
     */
    suspend fun checkOver(c: Context) {
        val store = TendStore(c)
        if (Account.user(c) == null || !store.markOverToday()) return
        val week = app.tend.core.Progress.weekKey(java.time.LocalDate.now())
        if (store.overDaysThisWeek() >= 3 && store.overToldWeek != week) {
            buddyAlert(c, OVER_3)
            store.overToldWeek = week
        }
    }

    // ---- Inviting: acorns for both, once the invited one did a first activity ----

    const val INVITE_ACORNS = 30

    /** After your first activity: whoever invited you gets their acorns (by a note), and you yours. How many. */
    suspend fun claimInvite(c: Context): Int {
        val store = TendStore(c)
        if (Account.user(c) == null || store.inviteClaimed || store.daySummaries().sumOf { it.tasksDone } == 0) return 0
        val n = Account.rpc(c, "tend_invite_reward")?.takeIf { it.ok }?.body?.trim()?.toIntOrNull() ?: return 0
        // Asked once there are friends and an activity: whatever it says, it's settled (later friends invited you after this).
        if (last.isNotEmpty()) store.inviteClaimed = true
        if (n > 0) store.addAcorns(n * INVITE_ACORNS, "invite")
        return n
    }

    /** New friends since last heard: acorns for each (both sides get theirs), and said once ([takeNewFriends]). */
    private fun celebrate(c: Context, friends: List<Friend>) {
        val store = TendStore(c)
        val known = store.friendsCelebrated
        val fresh = friends.filter { it.id !in known }
        val acorns = store.celebrateFriends(fresh.map { it.id })
        if (acorns <= 0) return
        val first = known.isEmpty()
        val names = fresh.joinToString(tr(" and ", " és ")) { it.name }
        prefs(c).edit().putString(
            "friend_news",
            if (first) tr("You and $names are friends now: +$acorns acorns, and a signpost for your camp.", "Mostantól barátok vagytok: $names. +$acorns makk, és egy útjelző tábla a táborodba.")
            else tr("You and $names are friends now: +$acorns acorns.", "Mostantól barátok vagytok: $names. +$acorns makk."),
        ).apply()
        Stats.log(c, "friend_made", mapOf("first" to first))
    }

    /** What to say about a new friendship, once. */
    fun takeNewFriends(c: Context): String? = prefs(c).getString("friend_news", null)?.also { prefs(c).edit().remove("friend_news").apply() }

    /** Friends as last heard from the server (this run of the app). */
    @Volatile private var last: List<Friend> = emptyList()

    /** Friends who did something today, most recent first: for "Friends today" on home. */
    fun doneToday(now: Long = System.currentTimeMillis()): List<Friend> {
        val midnight = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return last.filter { it.lastDone != null && (it.lastDoneAt ?: 0) in midnight..now }.sortedByDescending { it.lastDoneAt }
    }

    /** A friend calling you to the campfire in the last half hour (the newest one), or null. */
    fun fireCall(c: Context, now: Long = System.currentTimeMillis()): Note? {
        val done = prefs(c).getLong("fire_call_done", 0)
        return notes(c).firstOrNull { it.kind == Kind.FIRE && it.id > done && now - it.at < FIRE_CALL_MS }
    }

    /** The call to the fire is answered or put away (or a timer of your own started): it's gone, here and in the notifications. */
    fun doneFireCall(c: Context) {
        val newest = notes(c).filter { it.kind == Kind.FIRE }.maxOfOrNull { it.id } ?: return
        prefs(c).edit().putLong("fire_call_done", newest).apply()
        c.getSystemService(android.app.NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    /** A call to the fire is worth answering for half an hour. */
    private const val FIRE_CALL_MS = 30 * 60_000L

    private fun time(s: String?): Long? = s?.takeIf { it.isNotBlank() && it != "null" }?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }

    // ---- The shared campfire ----

    /** Tells friends what you just did ("went for a walk"), for them to cheer. Quietly does nothing offline. */
    suspend fun setDone(c: Context, line: String) {
        if (Account.user(c) == null) return
        Account.rpc(c, "tend_set_done", JSONObject().put("p_done", line))
    }

    /** Tells friends you're phone-free until [until] (epoch ms), doing [task]; null clears it. Quietly does nothing offline. */
    suspend fun setFocus(c: Context, until: Long?, task: String?) {
        if (Account.user(c) == null) return
        val args = JSONObject().put("p_until", until?.let { java.time.Instant.ofEpochMilli(it).toString() } ?: JSONObject.NULL).put("p_task", task ?: JSONObject.NULL)
        Account.rpc(c, "tend_set_focus", args)
    }

    /**
     * Keeps who's at the fire now, and (for the together bonus) who was there in the last four hours:
     * a friend who finished a few minutes before you still counts.
     */
    private fun keepAtFire(c: Context, friends: List<Friend>) {
        val now = System.currentTimeMillis()
        val fresh = friends.filter { (it.focusUntil ?: 0) > now }.map { f ->
            AtFire(f.id, f.name, f.companion, f.focusStarted ?: now, f.focusUntil!!, f.focusTask ?: "")
        }
        // Earlier sessions not in the fresh list end at the latest now (stopped early, or over).
        val earlier = atFire(c, now - 4 * 3_600_000L).filter { old -> fresh.none { it.id == old.id && it.started == old.started } }
            .map { if (it.until > now) it.copy(until = now) else it }
        val a = JSONArray()
        (fresh + earlier).forEach { f -> a.put(JSONObject().put("id", f.id).put("name", f.name).put("companion", f.companion).put("started", f.started).put("until", f.until).put("task", f.task)) }
        prefs(c).edit().putString("at_fire", a.toString()).apply()
    }

    /** "Anna put her phone down": one note per friend's session, with "Join her" (opens tend to join). */
    fun notifyAtFire(c: Context) {
        val fresh = newAtFire(c)
        val store = TendStore(c)
        if (fresh.isEmpty() || !app.tend.reminder.RecapReminder.canNotify(c) || store.focusStartedAt > 0 || !app.tend.reminder.Notes.underCap(store)) return
        store.countNote()
        val f = fresh.first()
        val minutes = ((f.until - System.currentTimeMillis()) / 60_000L).toInt()
        val nm = c.getSystemService(android.app.NotificationManager::class.java)
        nm.createNotificationChannel(android.app.NotificationChannel(CHANNEL, tr("Friends", "Barátok"), android.app.NotificationManager.IMPORTANCE_DEFAULT))
        val open = android.app.PendingIntent.getActivity(
            c, 22, android.content.Intent(c, app.tend.MainActivity::class.java).putExtra(app.tend.reminder.RecapReminder.EXTRA_OPEN, OPEN_JOIN).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm.notify(
            23,
            android.app.Notification.Builder(c, CHANNEL).setSmallIcon(app.tend.R.drawable.ic_notification)
                .setContentTitle(tr("${f.name} put the phone down", "${f.name} letette a telefont"))
                .setContentText(tr("Phone-free for $minutes more minutes, at your campfire. Join? +${app.tend.core.Acorns.TOGETHER_FIRE} acorns each.", "Még $minutes percig telefon nélkül, a tábortüzednél. Csatlakozol? Fejenként +${app.tend.core.Acorns.TOGETHER_FIRE} makk."))
                .setContentIntent(open).setAutoCancel(true).setTimeoutAfter(f.until - System.currentTimeMillis()).build(),
        )
        Stats.log(c, "fire_note")
    }

    const val OPEN_JOIN = "join_fire"

    /** Friends phone-free right now, as last heard (no network). */
    fun atFire(c: Context, now: Long = System.currentTimeMillis()): List<AtFire> = runCatching {
        val a = JSONArray(prefs(c).getString("at_fire", "[]"))
        (0 until a.length()).map { i -> a.getJSONObject(i) }.map { o ->
            AtFire(o.optString("id"), o.optString("name"), o.optString("companion", "BO"), o.optLong("started"), o.optLong("until"), o.optString("task"))
        }.filter { it.until > now }
    }.getOrDefault(emptyList())

    /** Friends who were phone-free at some point between [from] and [to] (for the together bonus). */
    fun overlapping(c: Context, from: Long, to: Long): List<AtFire> = atFire(c, 0).filter { it.started < to && it.until > from }

    /**
     * New ones at the fire since last asked (each friend's session once): for "Anna put her phone
     * down", worth saying only with a while still to go.
     */
    fun newAtFire(c: Context, now: Long = System.currentTimeMillis()): List<AtFire> {
        val told = prefs(c).getStringSet("fire_told", emptySet())!!
        val fresh = atFire(c, now).filter { "${it.id}:${it.started}" !in told && it.until - now > 5 * 60_000L }
        if (fresh.isNotEmpty()) prefs(c).edit().putStringSet("fire_told", (told + fresh.map { "${it.id}:${it.started}" }).toList().takeLast(50).toSet()).apply()
        return fresh
    }

    suspend fun remove(c: Context, id: String): Boolean =
        Account.rpc(c, "tend_remove_friend", JSONObject().put("p_friend", id))?.ok == true

    private fun parse(body: String): List<Friend> = runCatching {
        val a = JSONArray(body)
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val placed = app.tend.core.Clearing.parse(o.optString("placed"))
            Friend(
                o.optString("user_id"), o.optString("name").ifBlank { tr("A friend", "Egy barátod") }, o.optString("companion", "BO"),
                o.optInt("level", 1), placed,
                o.optString("week"), o.optInt("week_count"), o.optBoolean("bridge"),
                app.tend.core.Clearing.parseLayout(o.optString("layout"), placed),
                time(o.optString("focus_started")), time(o.optString("focus_until")), o.optString("focus_task").ifBlank { null },
                o.optString("last_done").takeIf { it.isNotBlank() && it != "null" }, time(o.optString("last_done_at")),
                o.optBoolean("pranks", true),
                o.optString("buddy_out").takeIf { it == "asked" || it == "yes" }, o.optString("buddy_in").takeIf { it == "asked" || it == "yes" },
            )
        }
    }.getOrDefault(emptyList())

    // ---- Together ----

    /** This week's together goal: who added how much (you first), the target, and whether it's reached. */
    class Week(val parts: List<Pair<Friend, Int>>, val target: Int) {
        val total: Int get() = parts.sumOf { it.second }
        val done: Boolean get() = total >= target
        /** The step reached (bronze, silver, gold) and the next one. */
        val tier: app.tend.core.Together.Tier? get() = app.tend.core.Together.tier(total, parts.size)
        val next: app.tend.core.Together.Tier? get() = app.tend.core.Together.next(total, parts.size)
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
            store.layout,
        )
    }

    /**
     * Each step reached this week for the first time: its acorns (bronze and silver), and gold the
     * goal's (and the bridge if it isn't built). True when something was given.
     */
    fun claimWeek(c: Context, week: Week, key: String): Boolean {
        val store = TendStore(c)
        if (week.parts.size < 2) return false
        var given = false
        app.tend.core.Together.Tier.entries.filter { it.target(week.parts.size) <= week.total }.forEach { t ->
            if (t == app.tend.core.Together.Tier.GOLD) {
                if (key in store.togetherDone) return@forEach
                store.togetherDone = store.togetherDone + key
            } else {
                val k = "$key:${t.name}"
                if (k in store.togetherSteps) return@forEach
                store.togetherSteps = store.togetherSteps + k
            }
            store.addAcorns(t.acorns, "together")
            given = true
        }
        return given
    }

    // ---- Notes: a wave, a visit, toilet paper ----

    /** What friends do to each other's camps (a wave is a cheer now; older ones still read). */
    enum class Kind(val id: String) {
        WAVE("wave"), VISIT("visit"), TP("tp"), CHEER("cheer"), GIFT("gift"),
        /** "Come to the fire": let's put the phones down together, now. */
        FIRE("fire"),
        /** A thing for the camp, from the giver's chest into yours. */
        THING("thing"),
        /** Asks you to be their buddy. */
        BUDDY_ASK("buddy_ask"),
        /** Your buddy slipped (the note's thing says how: [STRICT_OFF] or [OVER_3]). */
        BUDDY_ALERT("buddy_alert"),
        /** Someone you invited did their first activity: acorns for you. */
        INVITE_BONUS("invite_bonus");

        /** How it reads for whoever got it: "Anna waved at you". */
        fun received(name: String) = when (this) {
            WAVE -> tr("$name waved at you", "$name integetett neked")
            VISIT -> tr("$name visited your camp", "$name meglátogatta a táborod")
            TP -> tr("$name toilet-papered your oak", "$name vécépapírral dobálta meg a tölgyfádat")
            CHEER -> tr("$name cheered you on", "$name megtapsolt")
            GIFT -> tr("$name sent you a present", "$name ajándékot küldött")
            FIRE -> tr("$name calls you to the campfire", "$name a tábortűzhöz hív")
            THING -> tr("$name sent you a present", "$name ajándékot küldött")
            BUDDY_ASK -> tr("$name asks you to be their buddy", "$name a pajtásának kér")
            BUDDY_ALERT -> tr("Your buddy $name could use you now", "$name pajtásod most rád számít")
            INVITE_BONUS -> tr("$name did a first activity: +$INVITE_ACORNS acorns", "$name megcsinálta az első tevékenységét: +$INVITE_ACORNS makk")
        }

        /** For the widget's small label: "Anna: visited". */
        val short: String get() = when (this) { WAVE -> tr("waved", "integetett"); VISIT -> tr("visited", "itt járt"); TP -> tr("toilet paper!", "vécépapír!"); CHEER -> tr("cheered!", "megtapsolt!"); GIFT -> tr("a present!", "ajándék!"); FIRE -> tr("to the fire!", "a tűzhöz hív!"); THING -> tr("a present!", "ajándék!"); BUDDY_ASK -> tr("a buddy?", "pajtás?"); BUDDY_ALERT -> tr("needs you", "rád számít"); INVITE_BONUS -> tr("+acorns", "+makk") }

        /** For the 1x1 widget's badge. */
        val emoji: String get() = when (this) { WAVE -> "\uD83D\uDC4B"; VISIT -> "\uD83E\uDD86"; TP -> "\uD83E\uDDFB"; CHEER -> "\uD83D\uDC4F"; GIFT -> "\uD83C\uDF81"; FIRE -> "\uD83D\uDD25"; THING -> "\uD83C\uDF81"; BUDDY_ASK, BUDDY_ALERT -> "\uD83E\uDD1D"; INVITE_BONUS -> "\uD83C\uDF30" }

        companion object { fun of(id: String) = entries.firstOrNull { it.id == id } }
    }

    data class Note(val id: Long, val from: String, val name: String, val companion: String, val kind: Kind, val at: Long, val cleared: Boolean = false, val thing: String? = null)

    /** Gives a friend [thing]: true when it went, false when refused (not friends, or 5 given today), null offline. */
    suspend fun giveThing(c: Context, to: String, thing: String): Boolean? {
        val reply = Account.rpc(c, "tend_give_thing", JSONObject().put("p_to", to).put("p_thing", thing))?.takeIf { it.ok } ?: return null
        return reply.body.trim() == "true"
    }

    /** Presents a day, in all (the server's limit). */
    const val GIFTS_A_DAY = 5

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
            val hourAgo = System.currentTimeMillis() - 3_600_000L
            val rows = (0 until a.length()).mapNotNull { i -> a.getJSONObject(i).let { o -> Kind.of(o.optString("kind"))?.let { Triple(o.optString("to_id"), it, o.optBoolean("cleared")) }
                // A call to the fire can be made again after an hour.
                ?.takeIf { it.second != Kind.FIRE || (time(o.optString("created_at")) ?: 0) > hourAgo } } }
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
        // A cheer is worth something: a few acorns each, up to a few a day.
        val paid = p.getLong("cheers_paid", 0)
        val cheers = notes.filter { it.kind == Kind.CHEER && it.id > maxOf(paid, known) }
        if (cheers.isNotEmpty()) {
            val store = TendStore(c)
            val today = java.time.LocalDate.now().toString()
            val countedToday = if (p.getString("cheers_day", "") == today) p.getInt("cheers_today", 0) else 0
            val counted = cheers.size.coerceAtMost((CHEERS_PAID_A_DAY - countedToday).coerceAtLeast(0))
            if (counted > 0) store.addAcorns(counted * CHEER_ACORNS, "cheer")
            p.edit().putLong("cheers_paid", cheers.maxOf { it.id }).putString("cheers_day", today).putInt("cheers_today", countedToday + counted).apply()
        }
        // Someone you invited did a first activity: acorns, each once.
        val store = TendStore(c)
        val bonusPaid = p.getLong("bonus_paid", 0)
        val bonuses = notes.filter { it.kind == Kind.INVITE_BONUS && it.id > bonusPaid }
        if (bonuses.isNotEmpty()) {
            store.addAcorns(bonuses.size * INVITE_ACORNS, "invite")
            p.edit().putLong("bonus_paid", bonuses.maxOf { it.id }).apply()
        }
        // Presents (a thing, or acorns from older versions): wrapped by the fire, each once.
        val upTo = store.giftsUpTo
        val presents = notes.filter { n ->
            (n.kind == Kind.THING && n.thing != null || n.kind == Kind.GIFT) &&
                // Before presents were kept with the camp, each kind had its own mark here.
                n.id > (if (upTo > 0) upTo else p.getLong(if (n.kind == Kind.GIFT) "gifts_opened" else "things_taken", 0))
        }
        store.wrap(presents.sortedBy { it.id }.map { TendStore.Wrapped(it.id, it.name, it.thing.takeIf { _ -> it.kind == Kind.THING }) })
        // The mark set from the first sync on, presents or not: it travels with the camp's backup, so a camp
        // restored on another phone doesn't get the presents it already opened a second time.
        if (upTo == 0L) notes.maxOfOrNull { it.id }?.let { newest -> if (newest > store.giftsUpTo) store.giftsUpTo = newest }
        // A call to the fire goes out at once (while it's on); everything else waits for the day's one summary.
        val call = fresh.filter { it.kind == Kind.FIRE && System.currentTimeMillis() - it.at < FIRE_CALL_MS }
        if (call.isNotEmpty()) notify(c, call)
        digest(c)
        return notes.any { it.id > known }
    }

    /** Notes not yet seen in the app (from the last day), newest first. */
    fun unseenNotes(c: Context, now: Long = System.currentTimeMillis()): List<Note> {
        val seen = prefs(c).getLong("notes_seen", 0)
        return notes(c).filter { it.id > seen && now - it.at < DAY_MS && (it.kind != Kind.FIRE || now - it.at < FIRE_CALL_MS) }
    }

    /** Notes kept from the last sync, newest first. */
    fun notes(c: Context): List<Note> = parseNotes(prefs(c).getString("notes", null) ?: "[]")

    /** Who toilet-papered your oak (paper still up), newest first; empty when it's clean. */
    fun paperOnMe(c: Context): List<Note> =
        if (!TendStore(c).pranksOn) emptyList() else notes(c).filter { it.kind == Kind.TP && !it.cleared && it.id > prefs(c).getLong("paper_down", 0) }

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

    /**
     * For the widget: the newest note still worth a look. Paper, a duck or a present for as long as
     * it's still in the camp (unrolled, fished out, opened: gone at once); a call to the fire while
     * it's on; a wave or a cheer until it's been seen in the app (or a day has gone by).
     */
    fun unseen(c: Context, now: Long = System.currentTimeMillis()): Note? {
        val seen = prefs(c).getLong("notes_seen", 0)
        val wrapped by lazy { TendStore(c).wrapped.map { it.id }.toSet() }
        return notes(c).firstOrNull { n ->
            when (n.kind) {
                Kind.TP -> paperOnMe(c).any { it.id == n.id }
                Kind.VISIT -> now - n.at < DAY_MS && ducks(c).any { it.id == n.id }
                Kind.GIFT, Kind.THING -> n.id in wrapped
                Kind.FIRE -> n.id > seen && now - n.at < FIRE_CALL_MS
                else -> n.id > seen && now - n.at < DAY_MS
            }
        }
    }

    /** Where a note is answered: the call joined, a wave or cheer in the friends list, the rest in the camp. */
    fun openFor(kind: Kind): String = when (kind) {
        Kind.FIRE -> OPEN_JOIN
        Kind.WAVE, Kind.CHEER, Kind.BUDDY_ASK, Kind.BUDDY_ALERT -> OPEN_NOTES
        else -> app.tend.reminder.WeeklyLetter.OPEN_CLEARING
    }

    const val OPEN_NOTES = "friend_notes"
    const val OPEN_THINGS = "friend_things"

    /** Who cheered you since the camp last showed it (names, newest first). */
    fun cheersToShow(c: Context): List<String> {
        val shown = prefs(c).getLong("cheers_shown", 0)
        return notes(c).filter { it.kind == Kind.CHEER && it.id > shown && System.currentTimeMillis() - it.at < DAY_MS }.map { it.name }.distinct()
    }

    fun markCheersShown(c: Context) {
        notes(c).filter { it.kind == Kind.CHEER }.maxOfOrNull { it.id }?.let { prefs(c).edit().putLong("cheers_shown", it).apply() }
    }

    fun markSeen(c: Context) {
        val newest = notes(c).maxOfOrNull { it.id } ?: return
        // Seen in the app: the note in the notifications has done its job.
        c.getSystemService(android.app.NotificationManager::class.java).cancel(NOTIFICATION_ID)
        if (newest > prefs(c).getLong("notes_seen", 0)) {
            prefs(c).edit().putLong("notes_seen", newest).apply()
            app.tend.widget.Widgets.refresh(c)
        }
    }

    /** Acorns for each cheer received, and how many cheers a day are worth them. */
    const val CHEER_ACORNS = 2
    private const val CHEERS_PAID_A_DAY = 5

    /** Acorns in a present from a friend (sent by older versions). */
    const val GIFT_ACORNS = 10

    /**
     * Friends' news in one note a day, from [DIGEST_HOUR] on: what came since the last one and
     * hasn't been seen in the app (not calls to the fire: those go at once). It counts toward the
     * day's cap of notes.
     */
    private fun digest(c: Context, now: java.time.LocalDateTime = java.time.LocalDateTime.now()) {
        val p = prefs(c)
        val today = now.toLocalDate().toString()
        if (now.hour < DIGEST_HOUR || p.getString("digest_day", "") == today) return
        val since = maxOf(p.getLong("digest_upto", 0), p.getLong("notes_seen", 0))
        val news = notes(c).filter { it.id > since && it.kind != Kind.FIRE && System.currentTimeMillis() - it.at < DAY_MS }
        if (news.isEmpty()) return
        val store = TendStore(c)
        if (!app.tend.reminder.Notes.underCap(store)) return
        p.edit().putString("digest_day", today).putLong("digest_upto", news.maxOf { it.id }).apply()
        notify(c, news)
        store.countNote()
    }

    /** Friends' news comes in one note, from this hour of the evening. */
    const val DIGEST_HOUR = 18

    /** One notification for what's new: "Anna toilet-papered your oak" (and how many more). */
    private fun notify(c: Context, fresh: List<Note>) {
        if (!app.tend.reminder.RecapReminder.canNotify(c)) return
        val nm = c.getSystemService(android.app.NotificationManager::class.java)
        nm.createNotificationChannel(android.app.NotificationChannel(CHANNEL, tr("Friends", "Barátok"), android.app.NotificationManager.IMPORTANCE_DEFAULT))
        val first = fresh.firstOrNull { it.kind == Kind.FIRE } ?: fresh.first()
        val text = when (first.kind) {
            Kind.TP -> tr("Tap the oak in your camp to unroll it.", "Koppints a tölgyfára a táborodban, és letekered.")
            Kind.VISIT -> tr("They left a rubber duck in your river.", "Egy gumikacsát hagyott a folyódban.")
            Kind.WAVE -> tr("Wave back from your camp.", "Tapsolj neki vissza a Barátoknál.")
            Kind.CHEER -> tr("For what you did today: +$CHEER_ACORNS acorns.", "Azért, amit ma csináltál: +$CHEER_ACORNS makk.")
            Kind.GIFT, Kind.THING -> tr("It's wrapped by your fire. Tap it to open.", "Becsomagolva vár a tüzednél. Koppints rá, és kibontod.")
            Kind.FIRE -> tr("Put your phones down together: +${app.tend.core.Acorns.TOGETHER_FIRE} acorns each. Tap to join.", "Tegyétek le együtt a telefont: fejenként +${app.tend.core.Acorns.TOGETHER_FIRE} makk. Koppints, és csatlakozol.")
            Kind.BUDDY_ASK -> tr("Say yes or no under Friends.", "A Barátoknál mondhatsz igent vagy nemet.")
            Kind.BUDDY_ALERT -> if (first.thing == STRICT_OFF) tr("They turned Strict off. A cheer might help.", "Kikapcsolta a Szigorút. Egy taps jól jöhet neki.") else tr("Over their minutes 3 times this week. A cheer might help.", "A héten háromszor túllépte a keretét. Egy taps jól jöhet neki.")
            Kind.INVITE_BONUS -> tr("Thanks for bringing them to the fire.", "Köszönjük, hogy elhoztad a tűzhöz.")
        } + if (fresh.size > 1) tr(" And ${fresh.size - 1} more from friends.", " És még ${fresh.size - 1} a barátaidtól.") else ""
        val open = android.app.PendingIntent.getActivity(
            // A call to the fire opens tend ready to join; the rest opens the camp, where it happened.
            c, if (first.kind == Kind.FIRE) 24 else 21,
            android.content.Intent(c, app.tend.MainActivity::class.java).putExtra(app.tend.reminder.RecapReminder.EXTRA_OPEN, openFor(first.kind)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        nm.notify(
            NOTIFICATION_ID,
            android.app.Notification.Builder(c, CHANNEL).setSmallIcon(app.tend.R.drawable.ic_notification)
                .setContentTitle(first.kind.received(first.name)).setContentText(text)
                .setContentIntent(open).setAutoCancel(true)
                // Gone by itself: a call when it's over, the rest by the evening.
                .setTimeoutAfter(if (first.kind == Kind.FIRE) (FIRE_CALL_MS - (System.currentTimeMillis() - first.at)).coerceAtLeast(60_000L) else 12 * 3_600_000L)
                .build(),
        )
    }

    private fun parseNotes(body: String): List<Note> = runCatching {
        val a = JSONArray(body)
        (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            val kind = Kind.of(o.optString("kind")) ?: return@mapNotNull null
            val at = runCatching { java.time.OffsetDateTime.parse(o.optString("created_at")).toInstant().toEpochMilli() }.getOrDefault(0L)
            Note(o.optLong("id"), o.optString("from_id"), o.optString("name").ifBlank { tr("A friend", "Egy barátod") }, o.optString("companion", "BO"), kind, at, o.optBoolean("cleared"), if (o.isNull("thing")) null else o.optString("thing"))
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
