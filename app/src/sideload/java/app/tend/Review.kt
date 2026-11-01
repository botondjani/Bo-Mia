package app.tend

import android.app.Activity

/** Side-loaded builds aren't on Google Play: nothing to review there. */
object Review {
    @Suppress("UNUSED_PARAMETER")
    fun ask(activity: Activity) = Unit
}
