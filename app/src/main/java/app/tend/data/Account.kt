package app.tend.data

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The optional tend account: Google sign-in, or a 6-digit code by e-mail. Without it the app works
 * the same; with it the camp can be backed up (and, later, friends see it). Sessions live in the
 * app's private storage; tokens are refreshed when they're about to expire.
 */
object Account {
    private const val PREFS = "tend_account"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class User(val id: String, val email: String, val name: String)

    /** Signed in as whom, or null. */
    fun user(c: Context): User? = prefs(c).let { p ->
        val id = p.getString("user_id", null) ?: return null
        User(id, p.getString("email", "") ?: "", p.getString("name", "") ?: "")
    }

    /** A result for the UI: done, or a short message to show. */
    sealed class Result {
        data object Ok : Result()
        data class Failed(val message: String) : Result()
    }

    // ---- Google ----

    /**
     * Shows Google's own account sheet, then hands the ID token to Supabase. A random nonce ties the
     * token to this request: Google gets its SHA-256, Supabase the raw value to compare.
     */
    suspend fun signInWithGoogle(activity: Activity): Result {
        val rawNonce = ByteArray(16).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        val hashed = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray()).joinToString("") { "%02x".format(it) }
        val option = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
            .setServerClientId(Supabase.GOOGLE_WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setNonce(hashed)
            .build()
        val request = androidx.credentials.GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            androidx.credentials.CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            return Result.Failed("")
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            return Result.Failed("No Google account on this phone. Add one in the phone's settings, or log in with e-mail.")
        } catch (e: Exception) {
            return Result.Failed("Google sign-in didn't work: ${e.message ?: e.javaClass.simpleName}")
        }
        val google = runCatching {
            com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.createFrom(credential.data)
        }.getOrNull() ?: return Result.Failed("Google sign-in didn't work. Try again, or log in with e-mail.")
        val body = JSONObject().put("provider", "google").put("id_token", google.idToken).put("nonce", rawNonce).toString()
        val reply = withContext(Dispatchers.IO) { Supabase.post("/auth/v1/token?grant_type=id_token", body) }
        return keep(activity, reply, google.givenName ?: google.displayName)
    }

    // ---- E-mail code ----

    /** Sends a 6-digit code to [email]; a new address gets an account on the first code. */
    suspend fun sendEmailCode(email: String): Result {
        val body = JSONObject().put("email", email.trim()).put("create_user", true).toString()
        val reply = withContext(Dispatchers.IO) { Supabase.post("/auth/v1/otp", body) }
        return if (reply.ok) Result.Ok else Result.Failed(problem(reply, "Couldn't send the code."))
    }

    suspend fun verifyEmailCode(c: Context, email: String, code: String): Result {
        val body = JSONObject().put("type", "email").put("email", email.trim()).put("token", code.trim()).toString()
        val reply = withContext(Dispatchers.IO) { Supabase.post("/auth/v1/verify", body) }
        return keep(c, reply, null)
    }

    // ---- Session ----

    /** Saves the session from a token reply; the name comes from Google, else the e-mail. */
    private fun keep(c: Context, reply: Supabase.Reply, name: String?): Result {
        if (!reply.ok) return Result.Failed(problem(reply, "Couldn't log in."))
        val j = reply.json()
        val user = j.optJSONObject("user") ?: return Result.Failed("Couldn't log in.")
        val email = user.optString("email")
        prefs(c).edit()
            .putString("access_token", j.optString("access_token"))
            .putString("refresh_token", j.optString("refresh_token"))
            .putLong("expires_at", System.currentTimeMillis() / 1000 + j.optLong("expires_in", 3600))
            .putString("user_id", user.optString("id"))
            .putString("email", email)
            .putString("name", name?.takeIf { it.isNotBlank() } ?: email.substringBefore('@').replaceFirstChar { it.uppercase() })
            .apply()
        return Result.Ok
    }

    /** A valid access token, refreshed when it has less than a minute left; null when signed out (or it can't refresh). */
    suspend fun token(c: Context): String? = withContext(Dispatchers.IO) {
        val p = prefs(c)
        val access = p.getString("access_token", null) ?: return@withContext null
        if (p.getLong("expires_at", 0) - System.currentTimeMillis() / 1000 > 60) return@withContext access
        val refresh = p.getString("refresh_token", null) ?: return@withContext null
        val reply = Supabase.post("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh).toString())
        if (!reply.ok) {
            // A refresh token that's no longer valid means the session is over; offline keeps it.
            if (reply.code in 400..499) clear(c)
            return@withContext null
        }
        val j = reply.json()
        p.edit()
            .putString("access_token", j.optString("access_token"))
            .putString("refresh_token", j.optString("refresh_token"))
            .putLong("expires_at", System.currentTimeMillis() / 1000 + j.optLong("expires_in", 3600))
            .apply()
        j.optString("access_token")
    }

    /** Calls a database function as the signed-in user. */
    suspend fun rpc(c: Context, name: String, args: JSONObject = JSONObject()): Supabase.Reply? {
        val t = token(c) ?: return null
        return withContext(Dispatchers.IO) { Supabase.post("/rest/v1/rpc/$name", args.toString(), t) }
    }

    /** What friends see: name, companion, level and camp, and this week's activities for the together goal. */
    suspend fun saveProfile(c: Context) {
        val u = user(c) ?: return
        val me = Friends.me(c)
        rpc(
            c, "tend_save_profile",
            JSONObject().put("p_name", u.name).put("p_companion", me.companion).put("p_level", me.level)
                .put("p_placed", app.tend.core.Clearing.format(me.placed))
                .put("p_week", me.week).put("p_week_count", me.weekCount).put("p_bridge", me.bridge),
        )
    }

    suspend fun signOut(c: Context) {
        token(c)?.let { t -> withContext(Dispatchers.IO) { Supabase.post("/auth/v1/logout", "{}", t) } }
        clear(c)
    }

    /** Deletes the account and everything on the server; the phone keeps its own data. */
    suspend fun deleteAccount(c: Context): Result {
        val reply = rpc(c, "tend_delete_account") ?: return Result.Failed("Log in again to delete the account.")
        if (!reply.ok) return Result.Failed(problem(reply, "Couldn't delete the account. Check the connection and try again."))
        clear(c)
        return Result.Ok
    }

    private fun clear(c: Context) = prefs(c).edit().clear().apply()

    /** The server's own message when it gives one, else [fallback]; offline gets its own line. */
    private fun problem(reply: Supabase.Reply, fallback: String): String = when {
        reply.code == 0 -> "No connection. Try again when you're online."
        reply.code == 429 -> "Too many tries. Wait a few minutes, then try again."
        else -> reply.json().let { it.optString("msg").ifBlank { it.optString("error_description") }.ifBlank { it.optString("message") } }
            .takeIf { it.isNotBlank() }?.let { "$fallback ($it)" } ?: fallback
    }

    const val PREFS_NAME = PREFS
}
