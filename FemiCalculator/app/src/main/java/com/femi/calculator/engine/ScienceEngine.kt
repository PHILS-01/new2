package com.femi.calculator.engine

import kotlin.math.ln
import kotlin.math.pow

/**
 * Electrolysis / electrochemical cell calculations.
 * n = moles, V = volume (dm3), m = mass (g), mo = molar mass (g/mol),
 * F = Faraday constant (C, typically 96500), I = current (A), t = time (s),
 * e = electron-to-mole ratio, Q = quantity of electricity (C).
 */
object ElectrochemistryEngine {

    const val DEFAULT_FARADAY = 96500.0
    private const val MOLAR_VOLUME = 22.4 // dm3 at STP

    fun moleFromQ(q: Double, f: Double, e: Double): Double = q / (f * e)

    fun moleFromCurrentTime(i: Double, t: Double, f: Double, e: Double): Double = (i * t) / (f * e)

    fun massFromQ(molarMass: Double, q: Double, f: Double, e: Double): Double = (molarMass * q) / (f * e)

    fun massFromCurrentTime(molarMass: Double, i: Double, t: Double, f: Double, e: Double): Double =
        (i * t * molarMass) / (f * e)

    fun volumeFromQ(f: Double, e: Double, q: Double): Double = (MOLAR_VOLUME * q) / (f * e)

    fun volumeFromCurrentTime(i: Double, t: Double, f: Double, e: Double): Double =
        (MOLAR_VOLUME * i * t) / (f * e)

    fun chargeFromMole(n: Double, f: Double, e: Double): Double = n * f * e

    fun chargeFromMass(m: Double, molarMass: Double, f: Double, e: Double): Double = (m / molarMass) * f * e

    fun chargeFromVolume(v: Double, f: Double, e: Double): Double = (v / MOLAR_VOLUME) * f * e

    fun emfOfCell(eCathode: Double, eAnode: Double): Double = eCathode - eAnode

    fun faradToCoulomb(farads: Double): Double = DEFAULT_FARADAY * farads

    /** Solve for the electron-to-mole ratio (e) given I, t, molar mass, F, and mass. */
    fun electronMoleRatioFromMass(i: Double, t: Double, molarMass: Double, f: Double, m: Double): Double =
        (i * t * molarMass) / (f * m)
}

/**
 * Radioactive decay / half-life calculations.
 * t = elapsed time, t1/2 = half-life, N/No = remaining fraction.
 */
object HalfLifeEngine {

    /** Solve for half-life given elapsed time t and remaining fraction N/No. */
    fun halfLifeFromTimeAndFraction(t: Double, fraction: Double): Double =
        (-1 * t * ln(2.0)) / ln(fraction)

    /** Solve for remaining fraction N/No given elapsed time t and half-life. */
    fun fractionFromTimeAndHalfLife(t: Double, halfLife: Double): Double =
        2.0.pow(-(t / halfLife))

    /** Solve for elapsed time given half-life and remaining fraction N/No. */
    fun timeFromHalfLifeAndFraction(halfLife: Double, fraction: Double): Double =
        (-1 * halfLife * ln(fraction)) / ln(2.0)
}
