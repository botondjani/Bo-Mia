package app.tend.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** The tend backend: one Supabase project, reached over plain HTTPS. */
object Supabase {
    const val URL_BASE = "https://tnrwkgcjqbnewsnfbkvi.supabase.co"
    const val KEY = "sb_publishable_3A9vBgp4nRblyuukPo2wBQ_VDBA3Izg"

    /** Google's web client id: Supabase checks that Google's ID tokens were made for it. */
    const val GOOGLE_WEB_CLIENT_ID = "1051107306969-u9i4buqc5p06uif2hujbqd7ivhehl40r.apps.googleusercontent.com"

    class Reply(val code: Int, val body: String) {
        val ok get() = code in 200..299
        fun json(): JSONObject = runCatching { JSONObject(body) }.getOrDefault(JSONObject())
    }

    /** A blocking POST; call it off the main thread. [token] is a signed-in user's access token. */
    fun post(path: String, body: String, token: String? = null): Reply = runCatching {
        val conn = URL(URL_BASE + path).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"; conn.doOutput = true
        conn.connectTimeout = 15_000; conn.readTimeout = 15_000
        conn.setRequestProperty("apikey", KEY)
        conn.setRequestProperty("Content-Type", "application/json")
        token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
        conn.outputStream.use { it.write(body.toByteArray()) }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        Reply(code, text)
    }.getOrElse { Reply(0, "") }
}
