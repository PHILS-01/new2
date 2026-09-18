package com.femi.calculator.engine

import kotlin.math.abs

data class Fraction(val numerator: Long, val denominator: Long) {

    companion object {
        private fun gcd(a: Long, b: Long): Long {
            var x = abs(a); var y = abs(b)
            while (y != 0L) { val t = y; y = x % y; x = t }
            return if (x == 0L) 1 else x
        }

        fun of(num: Long, den: Long): Fraction {
            if (den == 0L) throw ArithmeticException("Denominator cannot be zero")
            val sign = if (den < 0) -1 else 1
            val g = gcd(num, den)
            return Fraction(sign * num / g, sign * den / g)
        }

        /** Approximate a decimal value as a fraction using continued fractions. */
        fun fromDecimal(value: Double, maxDenominator: Long = 1_000_000): Fraction {
            val negative = value < 0
            var x = abs(value)
            var h1 = 1L; var h2 = 0L
            var k1 = 0L; var k2 = 1L
            var b = x
            do {
                val a = floor(b).toLong()
                var aux = h1; h1 = a * h1 + h2; h2 = aux
                aux = k1; k1 = a * k1 + k2; k2 = aux
                if (b - a < 1e-9) break
                b = 1.0 / (b - a)
            } while (k1 <= maxDenominator)
            val num = if (negative) -h1 else h1
            return of(num, k1)
        }

        private fun floor(v: Double) = kotlin.math.floor(v)
    }

    operator fun plus(o: Fraction) = of(numerator * o.denominator + o.numerator * denominator, denominator * o.denominator)
    operator fun minus(o: Fraction) = of(numerator * o.denominator - o.numerator * denominator, denominator * o.denominator)
    operator fun times(o: Fraction) = of(numerator * o.numerator, denominator * o.denominator)
    operator fun div(o: Fraction) = of(numerator * o.denominator, denominator * o.numerator)

    fun toDouble(): Double = numerator.toDouble() / denominator.toDouble()

    override fun toString(): String = if (denominator == 1L) "$numerator" else "$numerator/$denominator"
}
