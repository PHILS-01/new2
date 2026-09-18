package com.femi.calculator.engine

import kotlin.math.sqrt

object CorrelationEngine {

    data class RankedItem(
        val label: String,
        val a: Double,
        val b: Double,
        val rankA: Double,
        val rankB: Double,
        val d: Double,
        val dSquared: Double
    )

    data class SpearmanResult(
        val items: List<RankedItem>,
        val n: Int,
        val sumDSquared: Double,
        val rs: Double
    )

    /** Ranks values ascending (rank 1 = smallest), averaging ranks across ties. */
    fun rankValues(values: List<Double>): List<Double> {
        val n = values.size
        val order = values.indices.sortedBy { values[it] }
        val ranks = DoubleArray(n)
        var i = 0
        while (i < n) {
            var j = i
            while (j + 1 < n && values[order[j + 1]] == values[order[i]]) j++
            val avgRank = (i + 1 + j + 1) / 2.0
            for (k in i..j) ranks[order[k]] = avgRank
            i = j + 1
        }
        return ranks.toList()
    }

    fun spearman(labels: List<String>, aValues: List<Double>, bValues: List<Double>): SpearmanResult {
        if (aValues.size != bValues.size || aValues.size != labels.size) {
            throw IllegalArgumentException("Label, A, and B lists must be the same length")
        }
        val n = aValues.size
        if (n < 2) throw IllegalArgumentException("Need at least 2 data pairs")

        val ranksA = rankValues(aValues)
        val ranksB = rankValues(bValues)

        val items = (0 until n).map { i ->
            val d = ranksA[i] - ranksB[i]
            RankedItem(labels[i], aValues[i], bValues[i], ranksA[i], ranksB[i], d, d * d)
        }
        val sumDSquared = items.sumOf { it.dSquared }
        val rs = 1.0 - (6.0 * sumDSquared) / (n.toDouble() * (n.toDouble() * n - 1))

        return SpearmanResult(items, n, sumDSquared, rs)
    }
}

object RegressionEngine {

    data class LinearRegressionResult(
        val n: Int,
        val slope: Double,
        val intercept: Double,
        val r: Double,
        val rSquared: Double,
        val sumX: Double,
        val sumY: Double,
        val sumXY: Double,
        val sumX2: Double,
        val sumY2: Double
    )

    /** Simple linear regression y = a + bx, plus Pearson's r. */
    fun linearRegression(x: List<Double>, y: List<Double>): LinearRegressionResult {
        if (x.size != y.size) throw IllegalArgumentException("X and Y must have the same number of values")
        val n = x.size
        if (n < 2) throw IllegalArgumentException("Need at least 2 data points")

        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.indices.sumOf { x[it] * y[it] }
        val sumX2 = x.sumOf { it * it }
        val sumY2 = y.sumOf { it * it }

        val denominator = n * sumX2 - sumX * sumX
        if (denominator == 0.0) throw IllegalArgumentException("Cannot fit a line (all X values are identical)")

        val slope = (n * sumXY - sumX * sumY) / denominator
        val intercept = (sumY - slope * sumX) / n

        val rNumerator = n * sumXY - sumX * sumY
        val rDenominator = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))
        val r = if (rDenominator == 0.0) 0.0 else rNumerator / rDenominator

        return LinearRegressionResult(n, slope, intercept, r, r * r, sumX, sumY, sumXY, sumX2, sumY2)
    }
}
