package app.tend.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The camp scene is one and the same on home and on the camp page: going from one to the other it
 * glides and grows into place instead of the page just changing. Set by the page switcher.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
class CampShare(val shared: SharedTransitionScope, val visibility: AnimatedVisibilityScope) {
    /** Pages are changing right now. */
    val moving: Boolean get() = visibility.transition.let { it.currentState != it.targetState }
}

val LocalCampShare = staticCompositionLocalOf<CampShare?> { null }

/**
 * For what home and the camp page have in common (the camp scene's drawing, the header): it stays
 * put or glides over as the page changes. Null outside the app's page switcher.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun sharedCamp(key: String = "camp"): Modifier? {
    val s = LocalCampShare.current ?: return null
    return with(s.shared) { Modifier.sharedElement(rememberSharedContentState(key), s.visibility) }
}

/**
 * For the rest of a page, under what home and the camp share: it rises into place as the page
 * opens and sinks away as it closes, so going into the camp (and back) reads as going somewhere,
 * while the camp itself stays put.
 */
@OptIn(ExperimentalSharedTransitionApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
fun Modifier.pageBody(): Modifier {
    val s = LocalCampShare.current ?: return this
    return with(s.visibility) {
        this@pageBody.animateEnterExit(
            enter = androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(420, delayMillis = 60, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 4 } +
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300, delayMillis = 90)),
            exit = androidx.compose.animation.slideOutVertically(androidx.compose.animation.core.tween(220)) { it / 6 } +
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)),
        )
    }
}
