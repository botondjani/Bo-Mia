package app.tend

import android.app.Application
import android.content.Context
import java.io.File
import java.time.LocalDateTime

/** Keeps the last crash's stack trace, so a tester can share it from the home screen. */
class TendApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // The language first: names of things are made in it as they're first used.
        app.tend.core.Lang.hu = app.tend.core.Lang.startingWith(
            getSharedPreferences("tend", MODE_PRIVATE).getString("lang", null), resources.configuration.locales[0].language,
        )
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                crashFile(this).writeText("tend crash · ${LocalDateTime.now()} · thread ${thread.name}\n\n${error.stackTraceToString()}")
            }
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        /**
         * Changes the app's language and starts the app again (everything is named in the language
         * it started in). Nothing happens when it's the language already in use.
         */
        fun setLanguage(context: Context, code: String) {
            if (code == app.tend.core.Lang.code) return
            context.getSharedPreferences("tend", MODE_PRIVATE).edit().putString("lang", code).commit()
            restart(context)
        }

        /** Starts the app again from scratch, on the data as it is now. */
        fun restart(context: Context) {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
            // Whatever was still on its way to disk goes with it.
            context.getSharedPreferences("tend", MODE_PRIVATE).edit().commit()
            context.startActivity(android.content.Intent.makeRestartActivityTask(launch.component))
            Runtime.getRuntime().exit(0)
        }

        fun crashFile(context: Context) = File(context.filesDir, "last_crash.txt")
    }
}
