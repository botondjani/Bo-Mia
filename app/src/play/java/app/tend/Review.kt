package app.tend

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/** Google Play's in-app review sheet. Play decides whether it actually shows (it's rate-limited). */
object Review {
    fun ask(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful) manager.launchReviewFlow(activity, request.result)
        }
    }
}
