package com.morialkar.goosescope.core.color

/** Espace OKLab (ARCHITECTURE §1.2): distances perceptuellement uniformes. */
data class Oklab(val l: Double, val a: Double, val b: Double) {
    init {
        require(l.isFinite() && a.isFinite() && b.isFinite()) { "Composante OKLab non finie: ($l, $a, $b)" }
    }
}

/** Forme polaire d'OKLab: clarté, chroma, teinte en degrés [0, 360). */
data class Oklch(val l: Double, val c: Double, val hDeg: Double)
