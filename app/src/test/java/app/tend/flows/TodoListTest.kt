package app.tend.flows

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import app.tend.MainActivity
import app.tend.data.TendStore
import org.junit.Rule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "hu-w400dp-h2000dp-xxhdpi")
class TodoListTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun save(n: String) {
        rule.waitForIdle(); rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
    }
    @Test fun home() {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication()
        val p = ctx.getSharedPreferences("tend", 0)
        p.edit().putBoolean("onboarded", true).putBoolean("tour_done", true).putInt("acorns", 140)
            .putStringSet("blocked", setOf("com.instagram.android")).putString("tasks", "read,move,outside")
            .putString("saving_for", "canoe").putBoolean("settings_moved_note", true).putStringSet("steps_claimed", app.tend.core.Step.entries.map { it.name }.toSet()).putInt("evening_from", 3).commit()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithText("Teendők (2)").assertExists(); rule.onNodeWithText("Teendők", substring = true).performClick(); rule.mainClock.advanceTimeBy(1500); rule.onNodeWithText("Átrendeztük a beállításokat", substring = true).assertExists(); rule.onNodeWithText("Nincs Megállító", substring = true).assertExists()
    }
}
