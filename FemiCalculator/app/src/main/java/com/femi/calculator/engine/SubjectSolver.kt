package com.femi.calculator.engine

class SubjectException(message: String) : Exception(message)

/**
 * Rearranges a linear equation to isolate a chosen variable as the subject.
 * This is deliberately bounded to terms that are simple products of factors
 * (numbers, single-letter variables, or a fully self-contained function call) —
 * it will refuse (rather than silently guess) on anything nonlinear in the
 * chosen variable, or where the variable is nested inside a sub-expression
 * like (a+b)*c.
 */
object SubjectSolver {

    private val FUNCTION_NAMES = listOf(
        "asin", "acos", "atan", "sinh", "cosh", "tanh",
        "sqrt", "cbrt", "abs", "log", "sin", "cos", "tan", "ln"
    ).sortedByDescending { it.length }

    private fun maskFunctionNames(term: String): String {
        var masked = term
        for (name in FUNCTION_NAMES) {
            masked = masked.replace(name, "#".repeat(name.length))
        }
        return masked
    }

    /** Splits an expression into signed top-level terms (respecting parentheses). */
    private fun splitTerms(expr: String): List<Pair<Int, String>> {
        val terms = mutableListOf<Pair<Int, String>>()
        var depth = 0
        var sign = 1
        var leadingSign = 1
        val current = StringBuilder()
        for (ch in expr) {
            when {
                ch == '(' -> { depth++; current.append(ch) }
                ch == ')' -> { depth--; current.append(ch) }
                (ch == '+' || ch == '-') && depth == 0 -> {
                    if (current.isEmpty()) {
                        leadingSign *= if (ch == '-') -1 else 1
                    } else {
                        val lastCh = current.last()
                        if (lastCh in "+-*/^(") {
                            current.append(ch)
                        } else {
                            terms.add((sign * leadingSign) to current.toString().trim())
                            current.clear()
                            leadingSign = 1
                            sign = if (ch == '-') -1 else 1
                        }
                    }
                }
                else -> current.append(ch)
            }
        }
        if (current.toString().isNotBlank()) terms.add((sign * leadingSign) to current.toString().trim())
        return terms
    }

    private fun parenDepthAt(s: String, idx: Int): Int {
        var depth = 0
        for (i in 0 until idx) {
            if (s[i] == '(') depth++
            else if (s[i] == ')') depth--
        }
        return depth
    }

    private fun cleanCoefficient(raw: String): String {
        var s = raw.trim()
        while (s.startsWith("*")) s = s.substring(1).trim()
        while (s.endsWith("*")) s = s.substring(0, s.length - 1).trim()
        return if (s.isEmpty()) "1" else s
    }

    /** Classifies one signed term relative to the chosen variable, adding it to the right bucket. */
    private fun classifyTerm(
        sign: Int, text: String, varChar: Char, flipSignForVarTerm: Boolean,
        varTerms: MutableList<Pair<Int, String>>, otherTerms: MutableList<Pair<Int, String>>
    ) {
        val masked = maskFunctionNames(text)
        val count = masked.count { it == varChar }
        when {
            count == 0 -> otherTerms.add(sign to text)
            count == 1 -> {
                val idx = masked.indexOf(varChar)
                if (idx + 1 < masked.length && masked[idx + 1] == '^') {
                    throw SubjectException("'$text' is nonlinear in $varChar")
                }
                if (parenDepthAt(masked, idx) > 0 && text.trim() != "($varChar)") {
                    throw SubjectException("'$text' is too complex (nested) for Change")
                }
                val coeffRaw = text.substring(0, idx) + text.substring(idx + 1)
                val coeffSign = if (flipSignForVarTerm) -sign else sign
                varTerms.add(coeffSign to cleanCoefficient(coeffRaw))
            }
            else -> throw SubjectException("'$text' is nonlinear in $varChar")
        }
    }

    private fun buildSum(terms: List<Pair<Int, String>>): String {
        if (terms.isEmpty()) return "0"
        val sb = StringBuilder()
        for ((i, pair) in terms.withIndex()) {
            val (sign, text) = pair
            if (i == 0) sb.append(if (sign < 0) "-($text)" else "($text)")
            else sb.append(if (sign < 0) " - ($text)" else " + ($text)")
        }
        return sb.toString()
    }

    /** Rearranges `equation` (must contain exactly one '=') to make varChar the subject. */
    fun makeSubject(equation: String, varChar: Char): String {
        val sides = equation.split("=")
        if (sides.size != 2) throw SubjectException("Expression must be a single equation (one '=')")
        val left = sides[0]
        val right = sides[1]

        val varTerms = mutableListOf<Pair<Int, String>>()
        val otherLeft = mutableListOf<Pair<Int, String>>()
        val otherRight = mutableListOf<Pair<Int, String>>()

        for ((sign, text) in splitTerms(left)) classifyTerm(sign, text, varChar, false, varTerms, otherLeft)
        for ((sign, text) in splitTerms(right)) classifyTerm(sign, text, varChar, true, varTerms, otherRight)

        if (varTerms.isEmpty()) throw SubjectException("$varChar was not found in the equation")

        val numeratorTerms = otherRight + otherLeft.map { (s, t) -> (-s) to t }
        val coeffStr = buildSum(varTerms)
        val numeratorStr = buildSum(numeratorTerms)

        return "$varChar = ($numeratorStr) / ($coeffStr)"
    }
}
