package app.tend.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.tend.core.Avatar
import app.tend.core.Mood
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * The Bo/Mia rig contract. Drop a file matching it at `app/src/main/assets/tend_character.riv`
 * and every character in the app switches to Rive. See docs/rive-rig.md.
 */
object RiveRig {
    const val ASSET = "tend_character.riv"
    const val ARTBOARD = "Character"
    const val STATE_MACHINE = "Mood"

    /** Number input: [Mood.ordinal] (0 happy, 1 tired, 2 scrolling, 3 outside). */
    const val INPUT_MOOD = "mood"

    /** Boolean input: false = Bo, true = Mia. */
    const val INPUT_MIA = "mia"

    /** Optional trigger: fired when the character is tapped. */
    const val INPUT_TAP = "tap"

    private var loaded = false
    private var bytes: ByteArray? = null

    /** The rig file, or null while there is none (then the drawn character is used). */
    @Synchronized
    fun bytes(context: Context): ByteArray? {
        if (!loaded) {
            bytes = runCatching { context.assets.open(ASSET).use { it.readBytes() } }.getOrNull()
            loaded = true
        }
        return bytes
    }
}

@Composable
fun CharacterView(avatar: Avatar, mood: Mood, size: Dp, modifier: Modifier = Modifier) {
    val rig = RiveRig.bytes(LocalContext.current)
    if (rig != null) RiveCharacter(rig, avatar, mood, size, modifier)
    else DrawnCharacter(avatar, mood, size, modifier)
}

@Composable
private fun RiveCharacter(rig: ByteArray, avatar: Avatar, mood: Mood, size: Dp, modifier: Modifier) {
    var view by remember { mutableStateOf<RiveAnimationView?>(null) }
    AndroidView(
        factory = { context ->
            RiveAnimationView(context, null).apply {
                setRiveBytes(rig, artboardName = RiveRig.ARTBOARD, stateMachineName = RiveRig.STATE_MACHINE, autoplay = true)
                view = this
            }
        },
        update = { v ->
            // A rig missing an input must not crash the app; it just won't react to it.
            runCatching { v.setNumberState(RiveRig.STATE_MACHINE, RiveRig.INPUT_MOOD, mood.ordinal.toFloat()) }
            runCatching { v.setBooleanState(RiveRig.STATE_MACHINE, RiveRig.INPUT_MIA, avatar == Avatar.MIA) }
        },
        modifier = modifier.size(size).clickable(remember { MutableInteractionSource() }, indication = null) {
            runCatching { view?.fireState(RiveRig.STATE_MACHINE, RiveRig.INPUT_TAP) }
        },
    )
}

/** Fallback until the Rive rig exists: the drawn character breathes, blinks and glances around. */
@Composable
private fun DrawnCharacter(avatar: Avatar, mood: Mood, size: Dp, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "character")
    val breathe by t.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(if (mood == Mood.TIRED) 3600 else 2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val look by t.animateFloat(
        0f, 0f,
        infiniteRepeatable(keyframes {
            durationMillis = 9000
            0f at 0; 0f at 3000; -1f at 3300; -1f at 4800; 0f at 5100; 1f at 6600; 1f at 7800; 0f at 8100
        }),
        label = "look",
    )
    val blink = remember { Animatable(0f) }
    LaunchedEffect(mood) {
        while (true) {
            delay(Random.nextLong(2500, 5500))
            blink.animateTo(1f, tween(90, easing = LinearEasing))
            blink.animateTo(0f, tween(130, easing = LinearEasing))
        }
    }
    // Scrolling stares at the phone; tired doesn't look around.
    val glance = if (mood == Mood.HAPPY || mood == Mood.OUTSIDE) look else 0f
    Canvas(modifier.size(size)) {
        drawIntoCanvas {
            CharacterPainter.draw(
                it.nativeCanvas, avatar, mood, this.size.minDimension,
                CharacterPainter.Pose(blink = blink.value, breathe = breathe, look = glance),
            )
        }
    }
}
