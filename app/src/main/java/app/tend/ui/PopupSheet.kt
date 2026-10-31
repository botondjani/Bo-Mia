package app.tend.ui

import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * A sheet that slides up over whatever app is open underneath, with a soft scrim.
 * Tapping the scrim calls [onDismiss].
 */
@Composable
fun PopupSheet(
    onDismiss: () -> Unit,
    minHeightFraction: Float = 0f,
    /** A floating card in the middle of the screen instead of a sheet from the bottom. */
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }
    // A change in the system blur eases the scrim over instead of jumping.
    val scrim by androidx.compose.animation.core.animateFloatAsState(if (blurOn.value) 0.25f else 0.55f, tween(450), label = "scrim")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val minHeight = maxHeight * minHeightFraction
        AnimatedVisibility(shown, enter = fadeIn(tween(260))) {
            Box(
                Modifier.fillMaxSize().background(Brand.Shade.copy(alpha = scrim))
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
        }
        if (centered) AnimatedVisibility(
            shown,
            enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = 500f), initialScale = 0.9f) + fadeIn(tween(180)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Column(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth().surface(radius = 32.dp, color = Brand.Sage)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        } else AnimatedVisibility(
            shown,
            enter = slideInVertically { it } + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = minHeight).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Brand.Sage)
                    // Swallow taps so they don't reach the scrim.
                    .clickable(remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                // With a minimum height, the first child sits at the top and the last at the bottom.
                verticalArrangement = if (minHeightFraction > 0f) Arrangement.SpaceBetween else Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/** Whether the system currently blurs behind our pop-ups; when it doesn't, the scrim gets darker. */
private val blurOn = androidx.compose.runtime.mutableStateOf(false)

/**
 * Blurs whatever app is underneath a pop-up window (Android 12+). The radius is set once:
 * changing it frame by frame made the blur flicker on some phones. Android can switch
 * cross-window blur off at any moment (battery saver, heat, some launchers), so we follow it
 * and darken the scrim instead, which keeps the pop-up looking the same either way.
 */
fun android.app.Activity.blurBehind() {
    blurOn.value = false
    if (android.os.Build.VERSION.SDK_INT < 31) return
    val prefs = getSharedPreferences("tend_ui", android.content.Context.MODE_PRIVATE)
    // This phone switched blur on and off under a pop-up before (a video playing, battery saver):
    // a steady dark scrim looks better than a flickering blur.
    if (prefs.getBoolean("blur_unsteady", false)) return
    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
    window.attributes = window.attributes.also { it.blurBehindRadius = BLUR_RADIUS }
    val wm = getSystemService(android.view.WindowManager::class.java)
    blurOn.value = wm.isCrossWindowBlurEnabled
    var flips = 0
    lateinit var listener: java.util.function.Consumer<Boolean>
    listener = java.util.function.Consumer<Boolean> { on ->
        if (++flips >= 2) {
            // Twice in one pop-up: stop blurring here and from now on.
            prefs.edit().putBoolean("blur_unsteady", true).apply()
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.also { it.blurBehindRadius = 0 }
            blurOn.value = false
            wm.removeCrossWindowBlurEnabledListener(listener)
        } else blurOn.value = on
    }
    wm.addCrossWindowBlurEnabledListener(mainExecutor, listener)
    (this as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
        override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) = wm.removeCrossWindowBlurEnabledListener(listener)
    })
}

private const val BLUR_RADIUS = 120
