package com.femi.calculator.engine

import kotlin.math.*

object StatisticsEngine {
    data class Result(
        val count: Int,
        val sum: Double,
        val mean: Double,
        val median: Double,
        val mode: List<Double>,
        val variance: Double,
        val stdDev: Double,
        val meanDeviation: Double,
        val min: Double,
        val max: Double,
        val range: Double
    )

    fun analyze(values: List<Double>): Result {
        if (values.isEmpty()) throw IllegalArgumentException("No data provided")
        val n = values.size
        val sum = values.sum()
        val mean = sum / n
        val sorted = values.sorted()
        val median = if (n % 2 == 0) (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0 else sorted[n / 2]
        val freq = values.groupingBy { it }.eachCount()
        val maxFreq = freq.values.maxOrNull() ?: 0
        val mode = if (maxFreq > 1) freq.filterValues { it == maxFreq }.keys.sorted() else emptyList()
        val variance = values.sumOf { (it - mean).pow(2) } / n
        val stdDev = sqrt(variance)
        val meanDeviation = values.sumOf { abs(it - mean) } / n
        return Result(n, sum, mean, median, mode, variance, stdDev, meanDeviation, sorted.first(), sorted.last(), sorted.last() - sorted.first())
    }
}

object QuadraticSolver {
    data class Result(val real: Boolean, val x1: Double, val x2: Double, val imagPart: Double = 0.0)

    fun solve(a: Double, b: Double, c: Double): Result {
        if (a == 0.0) throw IllegalArgumentException("Not quadratic: 'a' cannot be 0")
        val discriminant = b * b - 4 * a * c
        return if (discriminant >= 0) {
            val sq = sqrt(discriminant)
            Result(true, (-b + sq) / (2 * a), (-b - sq) / (2 * a))
        } else {
            val sq = sqrt(-discriminant)
            Result(false, -b / (2 * a), -b / (2 * a), sq / (2 * a))
        }
    }
}

object GeometryEngine {
    data class Shape(val area: Double, val perimeter: Double)

    fun circle(r: Double) = Shape(PI * r * r, 2 * PI * r)
    fun rectangle(w: Double, h: Double) = Shape(w * h, 2 * (w + h))
    fun triangle(a: Double, b: Double, c: Double): Shape {
        val s = (a + b + c) / 2
        val area = sqrt(s * (s - a) * (s - b) * (s - c))
        return Shape(area, a + b + c)
    }
    fun square(side: Double) = Shape(side * side, 4 * side)
    fun trapezoid(a: Double, b: Double, h: Double, c: Double, d: Double) =
        Shape((a + b) / 2 * h, a + b + c + d)

    data class Solid(val volume: Double, val surfaceArea: Double)
    fun sphere(r: Double) = Solid(4.0 / 3.0 * PI * r.pow(3), 4 * PI * r * r)
    fun cylinder(r: Double, h: Double) = Solid(PI * r * r * h, 2 * PI * r * (r + h))
    fun cone(r: Double, h: Double): Solid {
        val slant = sqrt(r * r + h * h)
        return Solid((1.0 / 3.0) * PI * r * r * h, PI * r * (r + slant))
    }
    fun cube(side: Double) = Solid(side.pow(3), 6 * side * side)
}

object PercentageEngine {
    fun percentOf(percent: Double, of: Double) = (percent / 100.0) * of
    fun whatPercent(part: Double, whole: Double) = (part / whole) * 100.0
    fun percentChange(from: Double, to: Double) = ((to - from) / from) * 100.0
}

object RatioEngine {
    private fun gcd(a: Long, b: Long): Long {
        var x = abs(a); var y = abs(b)
        while (y != 0L) { val t = y; y = x % y; x = t }
        return if (x == 0L) 1 else x
    }
    fun simplify(a: Long, b: Long): Pair<Long, Long> {
        val g = gcd(a, b)
        return Pair(a / g, b / g)
    }
    /** Solve a:b = c:x for missing term x, given three known values in order a,b,c (solve for d). */
    fun solveProportion(a: Double, b: Double, c: Double): Double = (b * c) / a
}

object UnitConverter {
    // length in meters
    val lengthFactors = mapOf(
        "mm" to 0.001, "cm" to 0.01, "m" to 1.0, "km" to 1000.0,
        "in" to 0.0254, "ft" to 0.3048, "yd" to 0.9144, "mi" to 1609.344
    )
    // weight in kilograms
    val weightFactors = mapOf(
        "mg" to 0.000001, "g" to 0.001, "kg" to 1.0, "t" to 1000.0,
        "oz" to 0.0283495, "lb" to 0.453592
    )
    // volume in liters
    val volumeFactors = mapOf(
        "ml" to 0.001, "l" to 1.0, "gal" to 3.78541, "qt" to 0.946353, "pt" to 0.473176, "cup" to 0.24
    )

    fun convert(value: Double, from: String, to: String, table: Map<String, Double>): Double {
        val fromFactor = table[from] ?: throw IllegalArgumentException("Unknown unit $from")
        val toFactor = table[to] ?: throw IllegalArgumentException("Unknown unit $to")
        return value * fromFactor / toFactor
    }

    fun celsiusToFahrenheit(c: Double) = c * 9.0 / 5.0 + 32.0
    fun fahrenheitToCelsius(f: Double) = (f - 32.0) * 5.0 / 9.0
    fun celsiusToKelvin(c: Double) = c + 273.15
    fun kelvinToCelsius(k: Double) = k - 273.15
}
