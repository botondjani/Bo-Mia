package app.tend.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * The camp's sounds, made here rather than recorded: a guitar (plucked strings), the fire, the
 * river, a creaky swing, a music box. Played at media volume; each kind can be turned off on its own.
 */
object Sounds {
    /** The kinds of sound, each with its own switch. */
    enum class Kind(val key: String, val title: String, val about: String) {
        MUSIC("sound_music", "Music", "The guitar, music box tunes"),
        CAMP("sound_camp", "Camp sounds", "The fire, the river, the swing, the birds"),
        LITTLE("sound_little", "Little sounds", "Acorns, chimes, things put down"),
    }
    private const val RATE = 22050
    private val worker = Executors.newSingleThreadExecutor()
    private var main: AudioTrack? = null
    private var extra: AudioTrack? = null
    private var chord = 0
    private val cache = HashMap<String, ShortArray>()

    // (Before the kinds there was one switch, "sounds": it's where each kind starts from.)
    fun on(c: Context, kind: Kind) = c.getSharedPreferences("tend", Context.MODE_PRIVATE).let { it.getBoolean(kind.key, it.getBoolean("sounds", true)) }
    fun set(c: Context, kind: Kind, on: Boolean) {
        c.getSharedPreferences("tend", Context.MODE_PRIVATE).edit().putBoolean(kind.key, on).apply()
        if (!on) stop()
    }

    /** What kind of sound using [thing] makes. */
    private fun kindOf(thing: String) = when (thing) {
        "guitar", "rocking_chair", "wind_chimes", "tent", "bell_tent", "cabin", "hammock", "string_lights", "log_bench" -> Kind.MUSIC
        "campfire", "pizza_oven", "kettle", "woodpile", "canoe", "raft", "bird_feeder", "fishing_rod", "dock", "swing" -> Kind.CAMP
        else -> Kind.LITTLE
    }

    /** The sound for the companion using [thing], as long as the use lasts. */
    fun forUse(c: Context, thing: String, delayMs: Long = 0) {
        if (!on(c, kindOf(thing))) return
        if (delayMs > 0) worker.execute { runCatching { Thread.sleep(delayMs) } }
        val secs = Uses.seconds(thing)
        when (thing) {
            "guitar" -> play("song", true) { song(secs) }
            "campfire", "pizza_oven" -> play("fire", true) { fire(secs) }
            "kettle" -> play("kettle", true) { mix(fire(secs), whistle(secs)) }
            "woodpile" -> play("wood", true) { mix(thump(secs, 1.1f), fire(secs)) }
            "canoe" -> play("paddle", true) { paddles(secs) }
            "raft" -> play("raft", true) { river(secs, 0.5f) }
            "rocking_chair" -> play("hum", true) { musicBox(HUM, 0.45f, secs) }
            "wind_chimes" -> play("chimes", true) { musicBox(CHIMES, 0.3f, secs) }
            "bird_feeder" -> play("birds", true) { birds(secs) }
            "fishing_rod" -> play("fish", true) { mix(river(secs, 0.25f), splash(secs, 4f)) }
            "dock" -> play("dock", true) { river(secs, 0.4f) }
            "swing" -> play("swing", true) { creaks(secs) }
            "tent", "bell_tent", "cabin", "hammock" -> play("lullaby", true) { musicBox(LULLABY, 0.55f, secs) }
            "string_lights" -> play("dance", true) { musicBox(DANCE, 0.22f, secs) }
            "log_bench" -> play("hum", true) { musicBox(HUM, 0.45f, secs) }
            // An activity done (acted out, acorns flying): the acorns dropping in.
            "act_read", "act_run", "act_walk", "act_stretch", "act_hearts", "act_tidy", "act_cheer" -> play("acorns", false) { acornDrops() }
            else -> play("chime", false) { musicBox(CHIME, 0.12f, CHIME_SECS) }
        }
    }

    /** One more strum on the guitar, on top of the song. */
    fun strum(c: Context) {
        if (!on(c, Kind.MUSIC)) return
        chord = (chord + 1) % CHORDS.size
        val key = "strum$chord"
        play(key, false) { strumOf(CHORDS[chord], 2.2f) }
    }

    /** Something set down on the grass: a soft, woody knock. */
    fun place(c: Context) { if (on(c, Kind.LITTLE)) play("place", false) { knock() } }

    /** A short happy chime (once through): a duck caught, something bought. */
    fun chime(c: Context) { if (on(c, Kind.LITTLE)) play("chime", false) { musicBox(CHIME, 0.12f, CHIME_SECS) } }

    /** Acorns claimed: a few soft wooden taps, like acorns dropping into a pouch. */
    fun acorns(c: Context) { if (on(c, Kind.LITTLE)) play("acorns", false) { acornDrops() } }

    fun stop() = worker.execute { main?.runCatching { stop(); release() }; main = null }

    private fun play(key: String, isMain: Boolean, make: () -> ShortArray) = worker.execute {
        // A sound that can't be made is only a missing sound, never a crash.
        val pcm = runCatching { cache.getOrPut(key, make) }.getOrNull() ?: return@execute
        // The one it replaces goes first: two long sounds at once can be more than the phone gives an app.
        if (isMain) { main?.runCatching { stop(); release() }; main = null } else { extra?.runCatching { stop(); release() }; extra = null }
        runCatching {
            // Long ones (the guitar's song, the fire) are fed in as they play, short ones loaded whole.
            val stream = pcm.size > RATE * 3
            val min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(if (stream) AudioTrack.MODE_STREAM else AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(if (stream) maxOf(min, RATE / 2) * 2 else pcm.size * 2).build()
            if (isMain) main = track else extra = track
            if (stream) {
                track.play()
                Thread {
                    runCatching {
                        var i = 0
                        while (i < pcm.size && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            val n = track.write(pcm, i, minOf(RATE / 4, pcm.size - i))
                            if (n <= 0) break
                            i += n
                        }
                    }
                }.start()
            } else {
                track.write(pcm, 0, pcm.size)
                track.play()
            }
        }.onFailure { android.util.Log.w("Sounds", "Couldn't play $key", it) }
    }

    // ---- Making the sounds ----

    private fun buffer(secs: Float) = FloatArray((secs * RATE).toInt())

    /**
     * To 16 bits: a soft fade in and out and a gentle limiter; with [rms], brought to that loudness
     * first, so the sounds sit at even levels next to each other (none blaring, none lost).
     */
    private fun toPcm(f: FloatArray, gain: Float = 1f, rms: Float? = null): ShortArray {
        val fade = (RATE * 0.03f).toInt()
        // Long ones ebb away at the end instead of stopping short.
        val out = if (f.size > RATE * 3) (RATE * 0.6f).toInt() else fade
        val level = rms?.let { target ->
            val now = kotlin.math.sqrt(f.sumOf { (it * gain).toDouble() * it * gain } / f.size.coerceAtLeast(1)).toFloat()
            if (now > 1e-6f) target / now else 1f
        } ?: 1f
        return ShortArray(f.size) { i ->
            val env = minOf(1f, i / fade.toFloat(), (f.size - i) / out.toFloat())
            val x = kotlin.math.tanh(f[i] * gain * level * env)
            (x * 26000).toInt().toShort()
        }
    }

    private fun mix(a: ShortArray, b: ShortArray) = ShortArray(maxOf(a.size, b.size)) { i ->
        ((if (i < a.size) a[i].toInt() else 0) + (if (i < b.size) b[i].toInt() else 0)).coerceIn(-32000, 32000).toShort()
    }

    /** One plucked string (Karplus-Strong) added into [out] from [start]. */
    private fun pluck(out: FloatArray, start: Int, freq: Float, amp: Float, rnd: java.util.Random) {
        val n = (RATE / freq).toInt().coerceAtLeast(2)
        val ring = FloatArray(n) { (rnd.nextFloat() * 2 - 1) * amp }
        // Softened before it rings (like a fingertip, not a pick): warmer, less metallic.
        repeat(2) { for (k in 0 until n) ring[k] = (ring[(k + n - 1) % n] + 2 * ring[k] + ring[(k + 1) % n]) / 4f }
        var idx = 0
        var i = start
        while (i < out.size) {
            val next = (idx + 1) % n
            val v = ring[idx]
            out[i] += v
            ring[idx] = 0.4985f * (v + ring[next])
            idx = next
            i++
            if (i - start > RATE * 3) break
        }
    }

    private val CHORDS = listOf(
        floatArrayOf(98.0f, 123.47f, 146.83f, 196.0f, 246.94f, 392.0f), // G
        floatArrayOf(130.81f, 164.81f, 196.0f, 261.63f, 329.63f), // C
        floatArrayOf(82.41f, 123.47f, 164.81f, 196.0f, 246.94f, 329.63f), // Em
        floatArrayOf(146.83f, 220.0f, 293.66f, 369.99f), // D
    )

    private fun strumOf(chord: FloatArray, secs: Float, down: Boolean = true): ShortArray {
        val out = buffer(secs)
        strumInto(out, 0, chord, down, 0.5f, java.util.Random(chord.size.toLong()))
        return toPcm(out, 0.9f)
    }

    private fun strumInto(out: FloatArray, at: Int, chord: FloatArray, down: Boolean, amp: Float, rnd: java.util.Random) {
        val strings = if (down) chord.toList() else chord.reversed()
        strings.forEachIndexed { k, f -> pluck(out, at + k * (RATE * 0.012f).toInt(), f, amp, rnd) }
    }

    /** A little campfire song: G, C, Em, D, down-down-up strums. */
    private fun song(secs: Float): ShortArray {
        val out = buffer(secs)
        val rnd = java.util.Random(7)
        val beat = 0.5f
        var t = 0.05f
        var bar = 0
        while (t < secs - 0.5f) {
            val chord = CHORDS[bar % CHORDS.size]
            for ((offset, down, amp) in listOf(Triple(0f, true, 0.5f), Triple(beat, true, 0.35f), Triple(beat * 1.5f, false, 0.25f), Triple(beat * 2.5f, false, 0.25f), Triple(beat * 3f, true, 0.35f))) {
                val at = ((t + offset) * RATE).toInt()
                if (at < out.size) strumInto(out, at, chord, down, amp, rnd)
            }
            t += beat * 4
            bar++
        }
        return toPcm(room(out), 0.8f, rms = 0.085f)
    }

    /** Crackling fire: a low rumble with pops. */
    private fun fire(secs: Float): ShortArray {
        val out = buffer(secs)
        val rnd = java.util.Random(3)
        var brown = 0f
        for (i in out.indices) {
            brown = (brown + (rnd.nextFloat() * 2 - 1) * 0.02f) * 0.995f
            out[i] = brown * 0.6f
        }
        repeat((secs * 9).toInt()) {
            val at = rnd.nextInt(out.size)
            val len = (RATE * (0.004f + rnd.nextFloat() * 0.02f)).toInt()
            val amp = 0.15f + rnd.nextFloat() * 0.45f
            // Each pop is a short burst, softened (low-passed) so it crackles rather than hisses.
            var lp = 0f
            for (k in 0 until len) if (at + k < out.size) {
                lp += ((rnd.nextFloat() * 2 - 1) - lp) * 0.35f
                out[at + k] += lp * amp * 1.6f * exp(-k / (len / 4f))
            }
        }
        return toPcm(out, 1.3f, rms = 0.065f)
    }

    /** Birdsong: little chirps, quick rising whistles in twos and threes. */
    private fun birds(secs: Float): ShortArray {
        val out = buffer(secs)
        val rnd = java.util.Random(13)
        var t = 0.2f
        while (t < secs - 0.4f) {
            repeat(2 + rnd.nextInt(2)) { k ->
                val start = ((t + k * 0.11f) * RATE).toInt()
                val f0 = 2100f + rnd.nextFloat() * 700f
                var ph = 0.0
                for (i in 0 until (RATE * 0.08f).toInt()) {
                    if (start + i >= out.size) break
                    val x = i / (RATE * 0.08f)
                    // A little rise with a trill in it, softly in and out.
                    ph += 2 * PI * (f0 + 900f * x + 120f * sin(2 * PI * 38 * i / RATE)) / RATE
                    out[start + i] += (sin(ph) * sin(PI * x) * sin(PI * x) * 0.22).toFloat()
                }
            }
            t += 0.6f + rnd.nextFloat() * 0.9f
        }
        return toPcm(out, rms = 0.045f)
    }

    /** The kettle starts to sing towards the end. */
    private fun whistle(secs: Float): ShortArray {
        val out = buffer(secs)
        var ph = 0.0
        for (i in out.indices) {
            val t = i / RATE.toFloat()
            val rise = ((t - secs * 0.45f) / (secs * 0.4f)).coerceIn(0f, 1f)
            ph += 2 * PI * (900 + 500 * rise) / RATE
            out[i] = sin(ph).toFloat() * 0.18f * rise
        }
        return toPcm(out)
    }

    /** Three small wooden taps, quick and soft, rising and falling a little. */
    private fun acornDrops(): ShortArray {
        val out = buffer(0.42f)
        for ((at, f) in listOf(0.0 to 1250.0, 0.085 to 1480.0, 0.16 to 1180.0)) {
            val start = (at * RATE).toInt()
            for (k in 0 until (RATE * 0.12f).toInt()) {
                if (start + k >= out.size) break
                val t = k.toDouble() / RATE
                out[start + k] += ((sin(2 * PI * f * t) * exp(-t / 0.016) + sin(2 * PI * f * 0.5 * t) * exp(-t / 0.03) * 0.4) * 0.28).toFloat()
            }
        }
        return toPcm(out)
    }

    private fun knock(): ShortArray {
        val out = buffer(0.3f)
        val rnd = java.util.Random(11)
        for (k in out.indices) {
            val t = k.toDouble() / RATE
            val body = (sin(2 * PI * 190 * t) * 0.6 + sin(2 * PI * 330 * t) * 0.25) * exp(-t / 0.04)
            val tap = (rnd.nextDouble() * 2 - 1) * exp(-t / 0.006) * 0.25
            out[k] = ((body + tap) * 0.55).toFloat()
        }
        return toPcm(out)
    }

    private fun thump(secs: Float, at: Float): ShortArray {
        val out = buffer(secs)
        val start = (at * RATE).toInt()
        for (k in 0 until RATE / 4) if (start + k < out.size) out[start + k] = sin(2 * PI * 70 * k / RATE).toFloat() * exp(-k / (RATE * 0.05f)) * 0.9f
        return toPcm(out)
    }

    /** The river flowing: soft filtered noise that swells. */
    private fun river(secs: Float, level: Float): ShortArray {
        val out = buffer(secs)
        val rnd = java.util.Random(5)
        var lp = 0f
        var lp2 = 0f
        for (i in out.indices) {
            lp += ((rnd.nextFloat() * 2 - 1) - lp) * 0.05f
            lp2 += (lp - lp2) * 0.25f
            out[i] = lp2 * level * 1.3f * (0.7f + 0.3f * sin(i / RATE.toFloat() * 1.3f))
        }
        return toPcm(out, 1.5f, rms = 0.12f * level)
    }

    private fun splash(secs: Float, at: Float): ShortArray {
        val out = buffer(secs)
        val rnd = java.util.Random(9)
        val start = (at * RATE).toInt()
        var lp = 0f
        for (k in 0 until (RATE * 0.5f).toInt()) {
            if (start + k >= out.size) break
            lp += ((rnd.nextFloat() * 2 - 1) - lp) * 0.12f
            out[start + k] = lp * exp(-k / (RATE * 0.12f)) * 2.2f
        }
        return toPcm(out)
    }

    private fun paddles(secs: Float): ShortArray {
        var all = river(secs, 0.3f)
        var t = 0.4f
        while (t < secs - 0.5f) { all = mix(all, splash(secs, t)); t += 1.05f }
        return all
    }

    /**
     * The swing, in time with how it's drawn (back and forth as sin(2.6 t)): a soft rush of air as it
     * sweeps through the bottom, and the rope creaking quietly on the branch at each end.
     */
    private fun creaks(secs: Float): ShortArray {
        val out = buffer(secs)
        val rnd = java.util.Random(17)
        val w = 2.6f
        // Air: low, soft noise swelling with the speed (fastest at the bottom of the arc).
        var lp = 0f
        var lp2 = 0f
        for (i in out.indices) {
            val t = i / RATE.toFloat()
            val speed = kotlin.math.abs(kotlin.math.cos(w * t))
            lp += ((rnd.nextFloat() * 2 - 1) - lp) * 0.06f
            lp2 += (lp - lp2) * 0.3f
            out[i] = lp2 * speed * speed * 0.55f
        }
        // The rope at each end: a short stick-slip creak through a woody resonance, a little different each time.
        var n = 0
        var t = (PI / 2 / w).toFloat()
        while (t < secs - 0.3f) {
            val f = 330.0 + (n % 3) * 35.0
            val r = 0.996
            val a1 = 2 * r * kotlin.math.cos(2 * PI * f / RATE)
            val a2 = -r * r
            var y1 = 0.0
            var y2 = 0.0
            val start = (t * RATE).toInt()
            val len = (RATE * 0.18f).toInt()
            var next = 0
            for (k in 0 until len) {
                if (start + k >= out.size) break
                // Friction: little catches, a few dozen a second, slowing down as it settles.
                var x = 0.0
                if (k >= next) { x = (0.6 + rnd.nextDouble() * 0.4); next = k + (RATE / (55 - 30.0 * k / len)).toInt() }
                val y = a1 * y1 + a2 * y2 + x
                y2 = y1; y1 = y
                val env = sin(PI * k / len)
                out[start + k] += (y * 0.0045 * env).toFloat()
            }
            n++
            t += (PI / w).toFloat()
        }
        return toPcm(out, 1.2f, rms = 0.055f)
    }

    // Tunes for the music box: (semitones from A4, beats).
    private val LULLABY = listOf(7 to 1f, 4 to 1f, 4 to 2f, 5 to 1f, 2 to 1f, 2 to 2f, 0 to 1f, 2 to 1f, 4 to 1f, 5 to 1f, 7 to 1f, 7 to 1f, 7 to 2f)
    private val DANCE = listOf(0 to 1f, 4 to 1f, 7 to 1f, 12 to 1f, 7 to 1f, 4 to 1f, 2 to 1f, 5 to 1f, 9 to 1f, 14 to 1f, 9 to 1f, 5 to 1f, 4 to 1f, 7 to 1f, 11 to 1f, 16 to 2f)
    private val HUM = listOf(0 to 2f, 2 to 1f, 4 to 1f, 2 to 2f, 0 to 2f, -3 to 2f, 0 to 4f)
    private val CHIME = listOf(12 to 1f, 16 to 1f, 19 to 1f, 24 to 3f)
    /** Long enough for the chime once through (a music box tune repeats to fill its time). */
    private const val CHIME_SECS = 1.7f
    private val CHIMES = listOf(19 to 1f, 24 to 2f, 16 to 1f, 21 to 3f, 12 to 1f, 19 to 2f, 26 to 1f, 24 to 4f)

    /** A tune on a music box: sine bells with a bright overtone, each ringing out. */
    private fun musicBox(tune: List<Pair<Int, Float>>, beat: Float, secs: Float): ShortArray {
        val out = buffer(secs)
        var t = 0.05f
        var i = 0
        // A short one plays once through and rings out; a long one repeats to fill its time.
        while (t < secs - 0.3f && (secs > 3f || i < tune.size)) {
            val (semi, beats) = tune[i % tune.size]
            val f = 440.0 * Math.pow(2.0, semi / 12.0)
            val start = (t * RATE).toInt()
            for (k in 0 until (RATE * 1.2f).toInt()) {
                if (start + k >= out.size) break
                val x = k / RATE.toDouble()
                out[start + k] += ((sin(2 * PI * f * x) + 0.3 * sin(2 * PI * f * 3 * x)) * exp(-x * 3.5) * 0.3).toFloat()
            }
            t += beats * beat
            i++
        }
        return toPcm(room(out), rms = if (secs > 3f) 0.075f else 0.085f)
    }

    /** A little room around a sound (a few soft echoes, fading): warmer, less like a beep. */
    private fun room(dry: FloatArray): FloatArray {
        val out = dry.copyOf()
        for ((ms, fb) in listOf(29.7f to 0.62f, 37.1f to 0.58f, 41.1f to 0.55f)) {
            val d = (RATE * ms / 1000f).toInt()
            val comb = FloatArray(dry.size)
            for (i in dry.indices) comb[i] = dry[i] + (if (i >= d) comb[i - d] * fb else 0f)
            for (i in out.indices) out[i] += comb[i] * 0.12f
        }
        return out
    }
}
