package com.femi.calculator.engine

import kotlin.math.PI
import kotlin.math.sin

/**
 * A curated set of the most commonly used formulas per physics topic —
 * not exhaustive, just the ones actually reached for day to day.
 */
object MechanicsEngine {
    fun finalVelocity(u: Double, a: Double, t: Double): Double = u + a * t
    fun displacement(u: Double, a: Double, t: Double): Double = u * t + 0.5 * a * t * t
    fun velocitySquared(u: Double, a: Double, s: Double): Double = u * u + 2 * a * s
    fun force(m: Double, a: Double): Double = m * a
    fun momentum(m: Double, v: Double): Double = m * v
    fun kineticEnergy(m: Double, v: Double): Double = 0.5 * m * v * v
    fun potentialEnergy(m: Double, g: Double, h: Double): Double = m * g * h
    fun power(work: Double, t: Double): Double = work / t
}

object MagnetismEngine {
    /** Force on a current-carrying conductor in a magnetic field: F = BIL sin(theta). */
    fun forceOnConductor(b: Double, i: Double, l: Double, thetaDeg: Double): Double =
        b * i * l * sin(Math.toRadians(thetaDeg))

    /** Force on a moving charge: F = qvB sin(theta). */
    fun forceOnMovingCharge(q: Double, v: Double, b: Double, thetaDeg: Double): Double =
        q * v * b * sin(Math.toRadians(thetaDeg))

    /** Magnetic field around a long straight wire: B = (mu0 * I) / (2 * pi * r). */
    fun fieldAroundWire(i: Double, r: Double): Double {
        val mu0 = 4 * PI * 1e-7
        return (mu0 * i) / (2 * PI * r)
    }

    /** Torque on a current loop: tau = N I A B sin(theta). */
    fun torqueOnCoil(n: Double, i: Double, a: Double, b: Double, thetaDeg: Double): Double =
        n * i * a * b * sin(Math.toRadians(thetaDeg))
}

object ElectromagnetismEngine {
    /** Faraday's law: EMF = -N * (delta Phi / delta t); returns magnitude. */
    fun inducedEmf(n: Double, deltaFlux: Double, deltaT: Double): Double = n * deltaFlux / deltaT

    /** Motional EMF: EMF = B L v. */
    fun motionalEmf(b: Double, l: Double, v: Double): Double = b * l * v

    /** Energy stored in an inductor: E = 1/2 L I^2. */
    fun inductorEnergy(l: Double, i: Double): Double = 0.5 * l * i * i

    /** Transformer voltage ratio: Vs = Vp * (Ns / Np). */
    fun transformerSecondaryVoltage(vp: Double, np: Double, ns: Double): Double = vp * (ns / np)
}

object QuantumEngine {
    const val PLANCK = 6.62607015e-34 // J·s
    const val LIGHT_SPEED = 2.99792458e8 // m/s

    /** Photon energy: E = h f. */
    fun photonEnergyFromFrequency(f: Double): Double = PLANCK * f

    /** Photon energy from wavelength: E = h c / lambda. */
    fun photonEnergyFromWavelength(lambda: Double): Double = (PLANCK * LIGHT_SPEED) / lambda

    /** de Broglie wavelength: lambda = h / (m v). */
    fun deBroglieWavelength(m: Double, v: Double): Double = PLANCK / (m * v)

    /** Photoelectric effect max kinetic energy: KEmax = h f - workFunction. */
    fun photoelectricKineticEnergy(f: Double, workFunctionJ: Double): Double = PLANCK * f - workFunctionJ

    /** Bohr model hydrogen energy level (eV): E_n = -13.6 / n^2. */
    fun hydrogenEnergyLevel(n: Double): Double = -13.6 / (n * n)
}

object SpectroscopyEngine {
    /** Rydberg formula: 1/lambda = R (1/n1^2 - 1/n2^2). Returns wavelength in meters. */
    fun rydbergWavelength(n1: Double, n2: Double): Double {
        val rydberg = 1.097e7 // per meter
        val inverseLambda = rydberg * (1.0 / (n1 * n1) - 1.0 / (n2 * n2))
        return 1.0 / inverseLambda
    }

    /** Energy of a spectral transition: deltaE = h c / lambda. */
    fun transitionEnergy(lambda: Double): Double = (QuantumEngine.PLANCK * QuantumEngine.LIGHT_SPEED) / lambda

    /** Wavenumber: v-bar = 1 / lambda. */
    fun wavenumber(lambda: Double): Double = 1.0 / lambda

    /** Doppler shift fraction for light: delta-lambda / lambda = v / c. */
    fun dopplerShiftFraction(v: Double): Double = v / QuantumEngine.LIGHT_SPEED
}

object WavesEngine {
    fun waveSpeed(f: Double, lambda: Double): Double = f * lambda
    fun period(f: Double): Double = 1.0 / f
    /** Standing wave harmonic frequency on a string fixed at both ends: f_n = n v / (2 L). */
    fun stringHarmonicFrequency(n: Double, v: Double, l: Double): Double = (n * v) / (2 * l)
    fun intensity(power: Double, area: Double): Double = power / area
}

object FluidMechanicsEngine {
    fun pressureFromForce(force: Double, area: Double): Double = force / area
    /** Hydrostatic pressure: P = rho g h. */
    fun hydrostaticPressure(density: Double, g: Double, h: Double): Double = density * g * h
    /** Archimedes' principle: buoyant force = rho_fluid * g * V_displaced. */
    fun buoyantForce(fluidDensity: Double, g: Double, volumeDisplaced: Double): Double =
        fluidDensity * g * volumeDisplaced
    /** Continuity equation, solve for v2: A1 v1 = A2 v2. */
    fun continuityV2(a1: Double, v1: Double, a2: Double): Double = (a1 * v1) / a2
    /** Bernoulli's equation at equal height, solve for P2. */
    fun bernoulliP2(p1: Double, density: Double, v1: Double, v2: Double): Double =
        p1 + 0.5 * density * (v1 * v1 - v2 * v2)
}
