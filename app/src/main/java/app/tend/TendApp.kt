package app.tend

import android.app.Application
import android.content.Context
import java.io.File
import java.time.LocalDateTime

/** Keeps the last crash's stack trace, so a tester can share it from the home screen. */
class TendApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                crashFile(this).writeText("tend crash · ${LocalDateTime.now()} · thread ${thread.name}\n\n${error.stackTraceToString()}")
            }
            previous?.uncaughtException(thread, error)
        }
    }

    companion object {
        fun crashFile(context: Context) = File(context.filesDir, "last_crash.txt")
    }
}
