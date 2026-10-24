package com.morialkar.goosescope.core.sample

import com.morialkar.goosescope.core.Tuning

/**
 * SPEC F8, ARCHITECTURE §3.4. Décide sur la luminance de toute l'image, pas du réticule:
 * un objet noir bien éclairé doit rester « noir », pas « trop sombre ».
 */
class DarknessGate(private val threshold: Double = Tuning.TOO_DARK_MEAN_LUMINANCE) {
    fun isTooDark(meanLuminance: Double): Boolean {
        require(meanLuminance.isFinite() && meanLuminance in 0.0..1.0) { "Luminance hors de [0, 1]: $meanLuminance" }
        return meanLuminance < threshold
    }
}
