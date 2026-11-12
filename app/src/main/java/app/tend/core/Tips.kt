package app.tend.core

/**
 * Small tips on the camp at home: only ones that still apply (no widget yet, no town yet, nothing
 * moved yet), a different one each time, and not often: every other day, each a few days in all.
 */
enum class Tip {
    MOVE, WIDGET, TOWN;

    fun text(name: String) = when (this) {
        MOVE -> tr("Tip: hold a thing in the camp to move it", "Tipp: tarts nyomva egy tárgyat a táborban, és arrébb teheted")
        WIDGET -> tr("Tip: put $name on your home screen", "Tipp: $name a kezdőképernyődre is kiülhet")
        TOWN -> tr("Tip: add your town for its real weather", "Tipp: add meg a városod, és a táborban is olyan idő lesz")
    }

    companion object {
        /** How many days a tip is shown at most. */
        const val DAYS_EACH = 3

        /**
         * Today's tip, if any: one of [eligible] not yet shown [DAYS_EACH] days ([shown]: days so far,
         * today not counted), taking turns, on every other day ([day]: days since some fixed date).
         */
        fun today(eligible: List<Tip>, shown: Map<Tip, Int>, day: Long): Tip? {
            val left = eligible.filter { (shown[it] ?: 0) < DAYS_EACH }
            if (left.isEmpty() || day % 2 != 0L) return null
            return left[((day / 2) % left.size).toInt()]
        }
    }
}
