package com.femi.calculator.engine

import kotlin.math.abs

/** Small symbolic calculus helper for common single-variable school expressions. */
object IntegralEngine {
    fun symbolicIntegral(expr: String, variable: Char = 'x'): String = integrateTerms(normalize(expr), variable)

    fun symbolicDerivative(expr: String, variable: Char = 'x'): String = differentiateTerms(normalize(expr), variable)

    private fun normalize(s: String): String = s.replace(" ", "").replace("×", "*").replace("−", "-")

    private fun splitTopLevel(expr: String): List<Pair<Int, String>> {
        val out = mutableListOf<Pair<Int, String>>(); var depth = 0; var sign = 1; val cur = StringBuilder()
        for (ch in expr) {
            if (ch == '(') { depth++; cur.append(ch) }
            else if (ch == ')') { depth--; cur.append(ch) }
            else if ((ch == '+' || ch == '-') && depth == 0 && cur.isNotEmpty()) {
                out.add(sign to cur.toString()); cur.clear(); sign = if (ch == '-') -1 else 1
            } else if ((ch == '+' || ch == '-') && depth == 0 && cur.isEmpty()) sign = if (ch == '-') -1 else 1
            else cur.append(ch)
        }
        if (cur.isNotEmpty()) out.add(sign to cur.toString())
        return out
    }

    private data class Monomial(val coefficient: Double, val power: Int)

    private fun parseMonomial(term: String, variable: Char): Monomial? {
        val t = term.trim().removeSurrounding("(", ")")
        val v = variable.toString()
        if (!t.contains(v)) return t.toDoubleOrNull()?.let { Monomial(it, 0) }
        val pattern = Regex("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)?)(?:\\*)?" + Regex.escape(v) + "(?:\\^(\\d+))?$")
        val m = pattern.matchEntire(t) ?: return null
        val coeff = when (val c = m.groupValues[1]) { "", "+" -> 1.0; "-" -> -1.0; else -> c.toDouble() }
        return Monomial(coeff, m.groupValues[2].ifEmpty { "1" }.toInt())
    }

    private fun integrateTerms(expr: String, variable: Char): String {
        val pieces = splitTopLevel(expr)
        val results = mutableListOf<String>()
        for ((sign, raw) in pieces) {
            val t = raw
            val mono = parseMonomial(t, variable)
            if (mono != null) {
                val p = mono.power + 1; val c = mono.coefficient / p
                results.add(formatSigned(sign * c, p, variable)); continue
            }
            val sin = Regex("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)?)\\*?sin\\(" + Regex.escape(variable.toString()) + "\\)$").matchEntire(t)
            if (sin != null) { val c = coefficient(sin.groupValues[1]); results.add(formatText(sign * -c, "cos($variable)")); continue }
            val cos = Regex("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)?)\\*?cos\\(" + Regex.escape(variable.toString()) + "\\)$").matchEntire(t)
            if (cos != null) { val c = coefficient(cos.groupValues[1]); results.add(formatText(sign * c, "sin($variable)")); continue }
            val exp = Regex("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)?)\\*?e\\^" + Regex.escape(variable.toString()) + "$").matchEntire(t)
            if (exp != null) { val c = coefficient(exp.groupValues[1]); results.add(formatText(sign * c, "e^$variable")); continue }
            throw IllegalArgumentException("Indefinite integral currently supports polynomials, sin(x), cos(x), and e^x")
        }
        return joinResults(results)
    }

    private fun differentiateTerms(expr: String, variable: Char): String {
        val pieces = splitTopLevel(expr); val results = mutableListOf<String>()
        for ((sign, raw) in pieces) {
            val mono = parseMonomial(raw, variable)
            if (mono != null) {
                if (mono.power == 0) continue
                val c = sign * mono.coefficient * mono.power; val p = mono.power - 1
                results.add(formatSigned(sign * c, p, variable)); continue
            }
            val sin = Regex("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)?)\\*?sin\\(" + Regex.escape(variable.toString()) + "\\)$").matchEntire(raw)
            if (sin != null) { results.add(formatText(sign * coefficient(sin.groupValues[1]), "cos($variable)")); continue }
            val cos = Regex("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)?)\\*?cos\\(" + Regex.escape(variable.toString()) + "\\)$").matchEntire(raw)
            if (cos != null) { results.add(formatText(sign * -coefficient(cos.groupValues[1]), "sin($variable)")); continue }
            val exp = Regex("^([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)?)\\*?e\\^" + Regex.escape(variable.toString()) + "$").matchEntire(raw)
            if (exp != null) { results.add(formatText(sign * coefficient(exp.groupValues[1]), "e^$variable")); continue }
            throw IllegalArgumentException("Derivative currently supports polynomials, sin(x), cos(x), and e^x")
        }
        return joinResults(results).ifEmpty { "0" }
    }

    private fun coefficient(s: String): Double = when (s) { "", "+" -> 1.0; "-" -> -1.0; else -> s.toDouble() }
    private fun formatSigned(c: Double, power: Int, v: Char): String {
        val body = if (power == 0) formatNum(c) else {
            val coeff = if (abs(c - 1.0) < 1e-12) "" else if (abs(c + 1.0) < 1e-12) "-" else "${formatNum(c)}*"
            coeff + v + if (power == 1) "" else "^$power"
        }
        return body
    }
    private fun formatText(c: Double, body: String): String = if (abs(c - 1.0) < 1e-12) body else if (abs(c + 1.0) < 1e-12) "-$body" else "${formatNum(c)}*$body"
    private fun joinResults(parts: List<String>): String {
        if (parts.isEmpty()) return "0"
        var out = ""
        for (p in parts) {
            if (out.isEmpty()) out = p
            else if (p.startsWith("-")) out += " - " + p.removePrefix("-")
            else out += " + $p"
        }
        return out
    }
    private fun formatNum(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else "%.6f".format(java.util.Locale.US, v).trimEnd('0').trimEnd('.')
}
