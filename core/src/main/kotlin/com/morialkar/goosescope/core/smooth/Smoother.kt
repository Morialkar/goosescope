package com.morialkar.goosescope.core.smooth

import com.morialkar.goosescope.core.Tuning
import com.morialkar.goosescope.core.catalog.BaseColor
import com.morialkar.goosescope.core.classify.BaseClassifier
import com.morialkar.goosescope.core.color.Oklab

data class SmoothedReading(val base: BaseColor, val color: Oklab)

/**
 * SPEC F5, ARCHITECTURE §3.5. Moyenne exponentielle sur la couleur, puis hystérésis sur le nom:
 * la marge de distance empêche le clignotement à une frontière, le délai court garde la latence basse.
 */
class Smoother(
    private val classifier: BaseClassifier,
    private val alpha: Double = Tuning.SMOOTHING_ALPHA,
    private val margin: Double = Tuning.HYSTERESIS_MARGIN,
    private val dwellMs: Long = Tuning.HYSTERESIS_DWELL_MS,
) {
    private var smoothed: Oklab? = null
    private var current: BaseColor? = null
    private var candidate: BaseColor? = null
    private var candidateSince = 0L
    private var lastTimestamp = Long.MIN_VALUE

    init {
        require(alpha > 0.0 && alpha <= 1.0) { "alpha hors de ]0, 1]: $alpha" }
        require(margin >= 0.0 && dwellMs >= 0) { "marge et délai doivent être positifs" }
    }

    fun push(c: Oklab, timestampMs: Long): SmoothedReading {
        require(timestampMs >= lastTimestamp) { "Horodatage qui recule: $timestampMs < $lastTimestamp" }
        lastTimestamp = timestampMs

        val previous = smoothed
        val color = if (previous == null) c else Oklab(
            previous.l + alpha * (c.l - previous.l),
            previous.a + alpha * (c.a - previous.a),
            previous.b + alpha * (c.b - previous.b),
        )
        smoothed = color

        val best = classifier.classify(color)
        val shown = current
        if (shown == null) {
            current = best.base
        } else if (best.base == shown || classifier.distanceTo(shown, color) - best.distance < margin) {
            candidate = null
        } else {
            if (candidate != best.base) {
                candidate = best.base
                candidateSince = timestampMs
            }
            if (timestampMs - candidateSince >= dwellMs) {
                current = best.base
                candidate = null
            }
        }
        return SmoothedReading(checkNotNull(current), color)
    }

    fun reset() {
        smoothed = null
        current = null
        candidate = null
        lastTimestamp = Long.MIN_VALUE
    }
}
