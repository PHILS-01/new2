package com.femi.calculator.engine

import kotlin.math.pow
import kotlin.math.ln

object FinanceEngine {

    /** Simple interest: I = P * r * t (r as decimal, t in years) */
    fun simpleInterest(principal: Double, ratePercent: Double, years: Double): Double =
        principal * (ratePercent / 100.0) * years

    /** Compound interest amount: A = P (1 + r/n)^(n*t) */
    fun compoundAmount(principal: Double, ratePercent: Double, years: Double, compoundsPerYear: Double): Double {
        val r = ratePercent / 100.0
        return principal * (1 + r / compoundsPerYear).pow(compoundsPerYear * years)
    }

    fun compoundInterest(principal: Double, ratePercent: Double, years: Double, compoundsPerYear: Double): Double =
        compoundAmount(principal, ratePercent, years, compoundsPerYear) - principal

    /** Future value of a single lump sum with compounding. */
    fun futureValue(presentValue: Double, ratePercent: Double, years: Double, compoundsPerYear: Double): Double =
        compoundAmount(presentValue, ratePercent, years, compoundsPerYear)

    /** Present value of a future amount. */
    fun presentValue(futureValue: Double, ratePercent: Double, years: Double, compoundsPerYear: Double): Double {
        val r = ratePercent / 100.0
        return futureValue / (1 + r / compoundsPerYear).pow(compoundsPerYear * years)
    }

    /** Fixed loan payment (amortizing), rate per period as percent, n = number of periods. */
    fun loanPayment(principal: Double, ratePercentPerPeriod: Double, numPeriods: Double): Double {
        val r = ratePercentPerPeriod / 100.0
        if (r == 0.0) return principal / numPeriods
        return principal * r / (1 - (1 + r).pow(-numPeriods))
    }

    /** Future value of an ordinary annuity (equal payments each period). */
    fun annuityFutureValue(payment: Double, ratePercentPerPeriod: Double, numPeriods: Double): Double {
        val r = ratePercentPerPeriod / 100.0
        if (r == 0.0) return payment * numPeriods
        return payment * (((1 + r).pow(numPeriods) - 1) / r)
    }

    /** Present value of an ordinary annuity. */
    fun annuityPresentValue(payment: Double, ratePercentPerPeriod: Double, numPeriods: Double): Double {
        val r = ratePercentPerPeriod / 100.0
        if (r == 0.0) return payment * numPeriods
        return payment * ((1 - (1 + r).pow(-numPeriods)) / r)
    }

    /** Solve for the interest rate (percent per period) given PV, FV, and n, for lump-sum compounding. */
    fun solveRatePercent(presentValue: Double, futureValue: Double, numPeriods: Double): Double {
        val ratio = futureValue / presentValue
        return (ratio.pow(1.0 / numPeriods) - 1) * 100.0
    }

    /** Solve for time (periods) given PV, FV, and rate percent per period, for lump-sum compounding. */
    fun solveTimePeriods(presentValue: Double, futureValue: Double, ratePercentPerPeriod: Double): Double {
        val r = ratePercentPerPeriod / 100.0
        return ln(futureValue / presentValue) / ln(1 + r)
    }

    /** GDP via the expenditure approach: GDP = C + I + G + (X - M). */
    fun gdpExpenditure(consumption: Double, investment: Double, govSpending: Double, netExports: Double): Double =
        consumption + investment + govSpending + netExports

    /** GDP growth rate as a percent: (current - previous) / previous * 100. */
    fun gdpGrowthRate(previousGdp: Double, currentGdp: Double): Double =
        ((currentGdp - previousGdp) / previousGdp) * 100.0
}
