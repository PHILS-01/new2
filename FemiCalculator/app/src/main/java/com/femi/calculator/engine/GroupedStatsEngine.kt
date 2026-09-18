package com.femi.calculator.engine

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln

data class ClassInterval(val lower: Double, val upper: Double, val frequency: Int, val cumulativeFrequency: Int) {
    val midpoint: Double get() = (lower + upper) / 2.0
}

object GroupedStatsEngine {

    data class FrequencyTable(val classes: List<ClassInterval>, val n: Int)

    data class GroupedResult(
        val mean: Double,
        val median: Double,
        val mode: Double,
        val q1: Double,
        val q2: Double,
        val q3: Double,
        val iqr: Double
    )

    /** Build a frequency table from a raw list of values using Sturges' rule for class count. */
    fun buildFromRawData(values: List<Double>): FrequencyTable {
        val n = values.size
        if (n < 2) throw IllegalArgumentException("Need at least 2 values")
        val minV = values.min()
        val maxV = values.max()
        val range = (maxV - minV).let { if (it == 0.0) 1.0 else it }
        val k = maxOf(1, ceil(1 + 3.322 * (ln(n.toDouble()) / ln(10.0))).toInt())
        val width = ceil(range / k)
        val start = floor(minV)

        val boundaries = mutableListOf(start)
        var b = start
        while (b < maxV) {
            b += width
            boundaries.add(b)
        }

        val classes = mutableListOf<ClassInterval>()
        var cum = 0
        for (i in 0 until boundaries.size - 1) {
            val lower = boundaries[i]
            val upper = boundaries[i + 1]
            val isLast = i == boundaries.size - 2
            val freq = values.count { v -> v >= lower && (if (isLast) v <= upper else v < upper) }
            cum += freq
            classes.add(ClassInterval(lower, upper, freq, cum))
        }
        return FrequencyTable(classes, n)
    }

    /** Build a frequency table using a user-selected first class interval. Subsequent classes use the same width. */
    fun buildFromRawData(values: List<Double>, firstLower: Double, firstUpper: Double): FrequencyTable {
        if (values.size < 2) throw IllegalArgumentException("Need at least 2 values")
        if (firstUpper <= firstLower) throw IllegalArgumentException("Upper class boundary must be greater than lower boundary")
        val width = firstUpper - firstLower
        val minV = values.min(); val maxV = values.max()
        var lower = firstLower
        while (lower > minV) lower -= width
        val classes = mutableListOf<ClassInterval>()
        var cum = 0
        var guard = 0
        while (lower <= maxV && guard++ < 1000) {
            val upper = lower + width
            val isLast = upper >= maxV
            val freq = values.count { v -> v >= lower && (if (isLast) v <= upper else v < upper) }
            cum += freq
            classes.add(ClassInterval(lower, upper, freq, cum))
            lower = upper
        }
        return FrequencyTable(classes, values.size)
    }

    /** Build a frequency table directly from user-supplied (lower, upper, frequency) triples. */
    fun buildFromDirectClasses(entries: List<Triple<Double, Double, Int>>): FrequencyTable {
        var cum = 0
        val classes = entries.sortedBy { it.first }.map { (lower, upper, freq) ->
            cum += freq
            ClassInterval(lower, upper, freq, cum)
        }
        val n = classes.sumOf { it.frequency }
        if (n < 1) throw IllegalArgumentException("Total frequency must be at least 1")
        return FrequencyTable(classes, n)
    }

    fun computeGroupedStats(table: FrequencyTable): GroupedResult {
        val classes = table.classes
        val n = table.n
        val mean = classes.sumOf { it.midpoint * it.frequency } / n

        fun cumBefore(index: Int): Int = if (index == 0) 0 else classes[index - 1].cumulativeFrequency

        fun interpolate(target: Double): Double {
            val idx = classes.indexOfFirst { it.cumulativeFrequency >= target }.let { if (it == -1) classes.size - 1 else it }
            val cls = classes[idx]
            val width = cls.upper - cls.lower
            return cls.lower + ((target - cumBefore(idx)) / cls.frequency) * width
        }

        val median = interpolate(n / 2.0)
        val q1 = interpolate(n / 4.0)
        val q3 = interpolate(3 * n / 4.0)

        val modalIdx = classes.indices.maxByOrNull { classes[it].frequency }!!
        val modalClass = classes[modalIdx]
        val f0 = if (modalIdx > 0) classes[modalIdx - 1].frequency else 0
        val f2 = if (modalIdx < classes.size - 1) classes[modalIdx + 1].frequency else 0
        val width = modalClass.upper - modalClass.lower
        val denom = (2 * modalClass.frequency - f0 - f2)
        val mode = if (denom == 0) modalClass.midpoint
                   else modalClass.lower + ((modalClass.frequency - f0).toDouble() / denom) * width

        return GroupedResult(mean, median, mode, q1, median, q3, q3 - q1)
    }
}
