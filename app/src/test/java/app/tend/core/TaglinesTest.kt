package app.tend.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaglinesTest {
    @Test fun `first open of the day gets the signature line, then they rotate`() {
        assertEquals("We tend to waste much more time than we realize.", Taglines.forOpen(1, "Bo"))
        assertTrue(Taglines.forOpen(3, "Mia").contains("Mia"))
        assertEquals(Taglines.forOpen(1, "Bo"), Taglines.forOpen(6, "Bo"))
    }
}
