package app.tend.core

/** "We tend to…" lines for the pause pop-up. Honest, never shaming; one per open, rotating. */
object Taglines {
    fun forOpen(opensToday: Int, companion: String): String {
        val lines = listOf(
            "We tend to waste much more time than we realize.",
            "We tend to open it without thinking. You just stopped to think.",
            "We tend to say \"just a minute\". $companion is counting with you.",
            "We tend to feel better after a walk than after a scroll.",
            "We tend to reach for the phone when we're bored. Bored is allowed.",
        )
        return lines[(opensToday - 1).coerceAtLeast(0) % lines.size]
    }
}
