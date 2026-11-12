package app.tend.core

/** "We tend to…" lines for the pause pop-up. Honest, never shaming; one per open, rotating. */
object Taglines {
    fun forOpen(opensToday: Int, companion: String): String {
        val lines = listOf(
            tr("We tend to waste much more time than we realize.", "Sokkal több időnk megy el így, mint gondolnánk."),
            tr("We tend to open it without thinking. You just stopped to think.", "Gondolkodás nélkül szoktuk megnyitni. Te most megálltál egy pillanatra."),
            tr("We tend to say \"just a minute\". $companion is counting with you.", "„Csak egy perc” – szoktuk mondani. $companion veled számol."),
            tr("We tend to feel better after a walk than after a scroll.", "Egy séta után jobban érezzük magunkat, mint görgetés után."),
            tr("We tend to reach for the phone when we're bored. Bored is allowed.", "Unalmunkban nyúlunk a telefonért. Unatkozni szabad."),
        )
        return lines[(opensToday - 1).coerceAtLeast(0) % lines.size]
    }
}
