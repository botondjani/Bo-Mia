package app.tend.core

/**
 * Challenges: small things to do, each worth acorns, in rounds. The first round is the first
 * days (try every part of tend once); when it's all claimed, the next round opens, and so on.
 * New rounds can be added at the end over time; claimed ones are kept by name.
 */
enum class Step(val title: String, val how: String, val acorns: Int, val round: Int) {
    // Round 1: the first days.
    ACTIVITY("Do one activity", "Start one under Instead of scrolling", 15, 0),
    PLACE("Get something for your camp", "Camp → Shop", 10, 0),
    USE("Watch your companion use it", "Tap a thing in the camp", 10, 0),
    MOVE("Move something in your camp", "Camp → hold a thing, then drag it", 10, 0),
    NOT_NOW("Say \"Not now\" once", "When a paused app opens", 10, 0),
    WIDGET("Swap the tend icon for Bo", "Add the 1×1 widget; it opens tend too", 10, 0),
    FRIEND("Invite a friend", "Camp → Friends → Invite", 15, 0),

    // Round 2: settling in.
    GREET("Say hello to a visitor", "Tap the animal in your camp", 10, 1),
    THREE_THINGS("Use 3 different things", "Tap things in the camp", 15, 1),
    THREE_DAYS("Be active on 3 days", "One activity a day is enough", 20, 1),
    TREES("Get something for the trees", "Camp → Shop → In the trees", 15, 1),
    BETTER_FIRE("Upgrade the fire", "Camp → Shop → The fire pit", 15, 1),
    EARLY_NIGHT("Say good night before 23:00", "Evening recap → Good night", 15, 1),

    // Round 3: a real camp.
    FIVE_ANIMALS("Meet 5 different animals", "A new one comes most days", 30, 2),
    TEN_ACTIVITIES("Do 10 activities", "Any mix, any days", 30, 2),
    SHELTER("A place to sleep", "Camp → Shop → A place to sleep", 20, 2),
    ON_THE_RIVER("Something on the river", "Camp → Shop → On the river", 25, 2),
    TOGETHER("Reach the together goal with friends", "Camp → Friends", 40, 2),
    RARE("Meet a rare animal", "They come after a good day", 40, 2),
}

data class StepState(val step: Step, val done: Boolean, val claimed: Boolean) {
    val claimable: Boolean get() = done && !claimed
}

object FirstSteps {
    fun of(done: Set<Step>, claimed: Set<Step>): List<StepState> = Step.entries.map { StepState(it, it in done || it in claimed, it in claimed) }

    /** The round being played: the first one with something left to claim; empty when all are done. */
    fun current(states: List<StepState>): List<StepState> {
        val round = states.filter { !it.claimed }.minOfOrNull { it.step.round } ?: return emptyList()
        return states.filter { it.step.round == round }
    }

    /** Which round (1-based) is being played, and how many there are. */
    fun roundOf(states: List<StepState>): Int = (current(states).firstOrNull()?.step?.round ?: Step.entries.maxOf { it.round }) + 1
    val rounds: Int get() = Step.entries.maxOf { it.round } + 1

    /** Shown until every challenge is claimed. */
    fun finished(states: List<StepState>) = states.all { it.claimed }
}
