package app.tend.flows

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onAllNodesWithText
import app.tend.core.*
import app.tend.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "hu-w400dp-h860dp-xxhdpi")
class NewUserFlowTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    /** A moment on the page ([n] names it, for reading the test). */
    private fun save(@Suppress("UNUSED_PARAMETER") n: String) {
        // Time passes, on the frame clock and the system's (some steps wait on it).
        rule.mainClock.advanceTimeBy(2000); org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofSeconds(2)); rule.waitForIdle()
    }
    @Test fun skipPermissions() {
        ThemeChoice.mode = "light"
        val perms = PermissionActions(Permissions(false, false), false, false, {}, {}, {})
        var finished = false
        var acorns by androidx.compose.runtime.mutableStateOf(50)
        var placed by androidx.compose.runtime.mutableStateOf(mapOf<Int, String>())
        rule.setContent { TendTheme {
            Onboarding(AppCatalog(rule.activity), Avatar.BO, BudgetConfig(), setOf("x"), perms, {}, {}, {}, tasks = emptyList(),
                account = { AccountCard(onChanged = {}) }, placed = placed, acorns = acorns,
                onBuy = { placed = mapOf(0 to it); acorns -= 40; true }) { finished = true }
        } }
        save("n0")
        rule.onNodeWithText("regisztráció nélkül", substring = true).performClick(); save("n1")
        rule.onNodeWithText("Tovább, Bo jön velem").performClick(); save("n2")
        rule.onNodeWithText("Értem").performClick(); save("n3")
        rule.onNodeWithText("Folytatom nélküle").performClick(); rule.waitForIdle(); save("n3b")
        rule.onNodeWithText("Folytatom így").performClick(); save("n4")
        rule.onNodeWithText("Tovább 1 appal").performClick(); save("n5")
        rule.onNodeWithText("Tovább").performClick(); save("n6")
        rule.onNodeWithText("Most csak a Telefon le").performClick(); save("n7")
        rule.onAllNodesWithText("Tábortűz", substring = true)[0].performClick(); rule.mainClock.advanceTimeBy(15000); save("n8")
        rule.onNodeWithText("Indítás").performClick(); rule.waitForIdle()
        org.junit.Assert.assertTrue(finished)
    }
    @Test fun backFromAppsWithPermissionsOn() {
        ThemeChoice.mode = "light"
        val perms = PermissionActions(Permissions(true, true), false, false, {}, {}, {})
        rule.setContent { TendTheme {
            Onboarding(AppCatalog(rule.activity), Avatar.BO, BudgetConfig(), setOf("x"), perms, {}, {}, {}, tasks = emptyList(), signedIn = true) {}
        } }
        rule.onNodeWithText("Tovább, Bo jön velem").performClick(); save("b1")
        rule.onNodeWithText("Értem").performClick(); save("b2")
        // All on already: Continue, not a bounce.
        rule.onNodeWithText("Tovább").performClick(); save("b3")
        rule.onNodeWithText("Tovább 1 appal").assertExists()
        rule.activity.onBackPressedDispatcher.let { d -> rule.runOnUiThread { d.onBackPressed() } }; save("b4")
        rule.mainClock.advanceTimeBy(3000); save("b5")
        // Still on the permissions page, and back again goes further back.
        rule.onNodeWithText("Két engedély kell", substring = true).assertExists()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }; save("b6")
        rule.onNodeWithText("Értem").assertExists()
    }
}
