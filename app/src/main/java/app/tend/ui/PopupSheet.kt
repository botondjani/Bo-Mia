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
    // A steady scrim over the app underneath. No live blur: the system blur re-renders whatever plays
    // behind (a reel, a video) and flickers with it on many phones.
    val scrim = SCRIM
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

private const val SCRIM = 0.62f
