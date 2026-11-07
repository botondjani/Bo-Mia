package app.tend.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppsTest {
    @Test fun heavyAppsToAdd() {
        val minutes = mapOf("insta" to 95, "maps" to 12, "yt" to 40, "chat" to 20, "gone" to 300)
        val installed = listOf("insta", "maps", "yt", "chat")
        // 20+ a day, installed, not picked yet, most used first.
        assertEquals(listOf("yt", "chat"), AppCatalog.heavyToAdd(minutes, installed, picked = setOf("insta")))
        assertEquals(emptyList<String>(), AppCatalog.heavyToAdd(emptyMap(), installed, emptySet()))
    }
}
