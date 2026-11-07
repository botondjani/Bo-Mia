package app.tend.core

/**
 * The first steps: six small things for the first days, each worth acorns, so the camp grows from
 * day one and every part of tend gets tried once. Claimed one by one; the card goes when all are.
 */
enum class Step(val title: String, val how: String, val acorns: Int) {
    ACTIVITY("Do one activity", "Home → Instead of scrolling → start one", 15),
    PLACE("Put something in your camp", "Tap your acorns (top right) → Place things", 10),
    USE("Watch your companion use it", "Tap a thing in the camp", 10),
    NOT_NOW("Say \"Not now\" once", "When a paused app opens", 10),
    WIDGET("Add the widget", "Settings → Add the widget", 10),
    FRIEND("Invite a friend", "Clearing → Friends → Invite", 15),
}

data class StepState(val step: Step, val done: Boolean, val claimed: Boolean) {
    val claimable: Boolean get() = done && !claimed
}

object FirstSteps {
    fun of(done: Set<Step>, claimed: Set<Step>): List<StepState> = Step.entries.map { StepState(it, it in done || it in claimed, it in claimed) }

    /** Shown until every step is claimed. */
    fun finished(states: List<StepState>) = states.all { it.claimed }
}
