package app.tend.core

/**
 * Challenges: small things to do, each worth acorns, in rounds. The first round is the first
 * days (try every part of tend once); when it's all claimed, the next round opens, and so on.
 * New rounds can be added at the end over time; claimed ones are kept by name.
 */
enum class Step(val title: String, val how: String, val acorns: Int, val round: Int) {
    // Round 1: the first days.
    ACTIVITY(tr("Do one activity", "Csinálj meg egy tevékenységet"), tr("Start one under Instead of scrolling", "Indíts el egyet a „Görgetés helyett” alatt"), 15, 0),
    PLACE(tr("Get something for your camp", "Szerezz valamit a táborodba"), tr("Camp → Shop", "Tábor → Bolt"), 10, 0),
    USE(tr("Watch your companion use it", "Nézd meg, ahogy a társad használja"), tr("Tap a thing in the camp", "Koppints egy tárgyra a táborban"), 10, 0),
    MOVE(tr("Move something in your camp", "Tegyél arrébb valamit a táborban"), tr("Camp → hold a thing, then drag it", "Tábor → tartsd nyomva, aztán húzd"), 10, 0),
    NOT_NOW(tr("Say \"Not now\" once", "Mondd egyszer: „Most nem”"), tr("When a paused app opens", "Amikor megnyílik egy szüneteltetett app"), 10, 0),
    WIDGET(tr("Swap the tend icon for Bo", "Cseréld a tend ikonját Bo-ra"), tr("Add the 1×1 widget; it opens tend too", "Tedd ki az 1×1-es widgetet; az is megnyitja a tendet"), 10, 0),
    FRIEND(tr("Invite a friend", "Hívj meg egy barátot"), tr("Camp → Friends → Invite", "Tábor → Barátok → Meghívás"), 15, 0),

    // Round 2: settling in.
    GREET(tr("Say hello to a visitor", "Köszönj egy látogatónak"), tr("Tap the animal in your camp", "Koppints az állatra a táborodban"), 10, 1),
    THREE_THINGS(tr("Use 3 different things", "Használj 3 különböző tárgyat"), tr("Tap things in the camp", "Koppints a tárgyakra a táborban"), 15, 1),
    THREE_DAYS(tr("Be active on 3 days", "Legyél aktív 3 napon"), tr("One activity a day is enough", "Napi egy tevékenység elég"), 20, 1),
    TREES(tr("Get something for the trees", "Szerezz valamit a fák közé"), tr("Camp → Shop → In the trees", "Tábor → Bolt → A fák között"), 15, 1),
    BETTER_FIRE(tr("Upgrade the fire", "Fejleszd a tüzet"), tr("Camp → Shop → The fire pit", "Tábor → Bolt → A tűzrakóhely"), 15, 1),
    EARLY_NIGHT(tr("Say good night before 23:00", "Mondj jó éjt 23:00 előtt"), tr("Evening recap → Good night", "Esti összefoglaló → Jó éjt"), 15, 1),

    // Round 3: a real camp.
    FIVE_ANIMALS(tr("Meet 5 different animals", "Találkozz 5 különböző állattal"), tr("A new one comes most days", "A legtöbb nap jön egy új"), 30, 2),
    TEN_ACTIVITIES(tr("Do 10 activities", "Csinálj meg 10 tevékenységet"), tr("Any mix, any days", "Bármit, bármelyik napokon"), 30, 2),
    SHELTER(tr("A place to sleep", "Egy hely az alváshoz"), tr("Camp → Shop → A place to sleep", "Tábor → Bolt → Hely az alváshoz"), 20, 2),
    ON_THE_RIVER(tr("Something on the river", "Valami a folyóra"), tr("Camp → Shop → On the river", "Tábor → Bolt → A folyón"), 25, 2),
    TOGETHER(tr("Reach the together goal with friends", "Érjétek el a közös célt a barátaiddal"), tr("Camp → Friends", "Tábor → Barátok"), 40, 2),
    RARE(tr("Meet a rare animal", "Találkozz egy ritka állattal"), tr("They come after a good day", "Egy jó nap után jönnek"), 40, 2),
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
