package app.tend.flows

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.tend.core.*
import app.tend.ui.*
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "hu-w400dp-h1400dp-xxhdpi")
class WardrobeTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun migrationAndNight() {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication()
        val p = ctx.getSharedPreferences("tend", 0)
        // An existing user: glasses and leaf earned before (seen), leaf taken off, a beret bought and worn.
        p.edit().putStringSet("acc_seen", setOf("GLASSES", "LEAF", "NIGHTCAP")).putStringSet("acc_off", setOf("LEAF"))
            .putStringSet("outfits", setOf("BERET")).putString("outfit_worn", "BERET").commit()
        val s = app.tend.data.TendStore(ctx)
        s.accessories()
        // Nothing earned this week (no history): only the bought beret was showing, so only it is on.
        assertEquals(mapOf(Slot.HEAD to Accessory.BERET), s.wardrobe)
        assertEquals(setOf(Accessory.GLASSES, Accessory.LEAF, Accessory.NIGHTCAP, Accessory.BERET), s.owned)
        s.wear(Accessory.GLASSES)
        s.eveningFrom = 21
        assertEquals(setOf(Accessory.NIGHTCAP, Accessory.GLASSES), s.dressed(java.time.LocalDateTime.of(2026, 11, 23, 23, 0)))
        assertEquals(setOf(Accessory.BERET, Accessory.GLASSES), s.dressed(java.time.LocalDateTime.of(2026, 11, 23, 12, 0)))
        s.toggleWear(Accessory.BERET)
        assertEquals(mapOf(Slot.EYES to Accessory.GLASSES), s.wardrobe)
    }

    @Test fun ui() = shot(false, "")
    

    private var bought: Accessory? = null
    private fun shot(dark: Boolean, tag: String) {
        ThemeChoice.mode = if (dark) "dark" else "light"
        val owned = setOf(Accessory.GLASSES, Accessory.LEAF, Accessory.BERET, Accessory.HAT, Accessory.SCARF)
        val chosen = mapOf(Slot.HEAD to Accessory.BERET, Slot.EYES to Accessory.GLASSES)
        rule.setContent { TendTheme {
            Column(Modifier.fillMaxSize().background(Brand.Sage).padding(16.dp)) {
                Wardrobe(Avatar.BO, chosen.values.toSet(), chosen, owned, setOf(Accessory.GLASSES), true, 120, false, {}, {}, {}, { bought = it; true }, {})
            }
        } }
        rule.onNodeWithText("Fej".uppercase()).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Varázslósüveg").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Megveszem").performClick()
        rule.waitForIdle()
        org.junit.Assert.assertEquals(Accessory.WIZARD_HAT, bought)
    }
}
