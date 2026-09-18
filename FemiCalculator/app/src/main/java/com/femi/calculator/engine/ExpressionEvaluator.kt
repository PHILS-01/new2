package com.femi.calculator.engine

import kotlin.math.*

/**
 * A shunting-yard based scientific expression evaluator.
 * Supports: + - * / ^ % ( ) , sin cos tan asin acos atan sinh cosh tanh
 * log ln sqrt cbrt fact abs pi e, DEG/RAD angle mode.
 */
class ExpressionEvaluator(var angleMode: AngleMode = AngleMode.DEG) {

    enum class AngleMode { DEG, RAD }

    class EvalException(message: String) : Exception(message)

    private val functions1 = setOf(
        "sin", "cos", "tan", "asin", "acos", "atan",
        "sinh", "cosh", "tanh",
        "log", "ln", "sqrt", "cbrt", "abs", "fact"
    )

    fun evaluate(rawExpr: String): Double {
        var expr = rawExpr.trim()
        if (expr.isEmpty()) throw EvalException("Empty expression")
        expr = expr.replace("×", "*").replace("÷", "/").replace("−", "-")
        expr = expr.replace("π", "pi")
        val tokens = tokenize(expr)
        val rpn = toRpn(tokens)
        return evalRpn(rpn)
    }

    private sealed class Token
    private data class Num(val value: Double) : Token()
    private data class Ident(val name: String) : Token()
    private data class Op(val symbol: String) : Token()
    private object LParen : Token()
    private object RParen : Token()
    private object Comma : Token()

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    tokens.add(Num(expr.substring(start, i).toDouble()))
                }
                c.isLetter() -> {
                    val start = i
                    while (i < expr.length && expr[i].isLetter()) i++
                    val name = expr.substring(start, i)
                    tokens.add(Ident(name))
                }
                c == '(' -> { tokens.add(LParen); i++ }
                c == ')' -> { tokens.add(RParen); i++ }
                c == ',' -> { tokens.add(Comma); i++ }
                c in "+-*/^%!" -> { tokens.add(Op(c.toString())); i++ }
                else -> throw EvalException("Unexpected character: $c")
            }
        }
        return insertImplicitMultiplication(tokens)
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        val out = mutableListOf<Token>()
        for ((idx, t) in tokens.withIndex()) {
            if (idx > 0) {
                val prev = tokens[idx - 1]
                val needsMul =
                    (prev is Num || prev is RParen || (prev is Op && prev.symbol == "!")) &&
                    (t is Num || t is Ident || t is LParen)
                if (needsMul) out.add(Op("*"))
            }
            out.add(t)
        }
        return out
    }

    private fun precedence(op: String): Int = when (op) {
        "+", "-" -> 1
        "*", "/", "%" -> 2
        "u-" -> 3 // unary minus
        "^" -> 4
        "!" -> 5
        else -> 0
    }

    private fun isRightAssoc(op: String) = op == "^" || op == "u-"

    private fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()
        var prevToken: Token? = null

        fun isUnaryContext(): Boolean =
            prevToken == null || prevToken is Op || prevToken is LParen || prevToken is Comma

        for (t in tokens) {
            when (t) {
                is Num -> output.add(t)
                is Ident -> {
                    if (t.name == "pi") output.add(Num(PI))
                    else if (t.name == "e") output.add(Num(E))
                    else stack.addLast(t) // function name
                }
                is Op -> {
                    var opSym = t.symbol
                    if (opSym == "-" && isUnaryContext()) opSym = "u-"
                    if (opSym == "!") {
                        output.add(Op("!"))
                    } else {
                        while (stack.isNotEmpty() && stack.last() is Op) {
                            val top = (stack.last() as Op).symbol
                            if ((precedence(top) > precedence(opSym)) ||
                                (precedence(top) == precedence(opSym) && !isRightAssoc(opSym))
                            ) {
                                output.add(stack.removeLast())
                            } else break
                        }
                        stack.addLast(Op(opSym))
                    }
                }
                is LParen -> stack.addLast(t)
                is RParen -> {
                    while (stack.isNotEmpty() && stack.last() !is LParen) {
                        output.add(stack.removeLast())
                    }
                    if (stack.isEmpty()) throw EvalException("Mismatched parentheses")
                    stack.removeLast() // pop LParen
                    if (stack.isNotEmpty() && stack.last() is Ident) {
                        output.add(stack.removeLast())
                    }
                }
                is Comma -> {
                    while (stack.isNotEmpty() && stack.last() !is LParen) {
                        output.add(stack.removeLast())
                    }
                }
            }
            prevToken = t
        }
        while (stack.isNotEmpty()) {
            val top = stack.removeLast()
            if (top is LParen) throw EvalException("Mismatched parentheses")
            output.add(top)
        }
        return output
    }

    private fun evalRpn(rpn: List<Token>): Double {
        val stack = ArrayDeque<Double>()
        for (t in rpn) {
            when (t) {
                is Num -> stack.addLast(t.value)
                is Op -> {
                    when (t.symbol) {
                        "u-" -> stack.addLast(-pop(stack))
                        "!" -> stack.addLast(factorial(pop(stack)))
                        else -> {
                            val b = pop(stack)
                            val a = pop(stack)
                            stack.addLast(applyBinary(t.symbol, a, b))
                        }
                    }
                }
                is Ident -> {
                    val arg = pop(stack)
                    stack.addLast(applyFunction(t.name, arg))
                }
                else -> throw EvalException("Unexpected token in RPN")
            }
        }
        if (stack.size != 1) throw EvalException("Invalid expression")
        return stack.last()
    }

    private fun pop(stack: ArrayDeque<Double>): Double =
        stack.removeLastOrNull() ?: throw EvalException("Invalid expression")

    private fun applyBinary(op: String, a: Double, b: Double): Double = when (op) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        "/" -> if (b == 0.0) throw EvalException("Division by zero") else a / b
        "%" -> a % b
        "^" -> a.pow(b)
        else -> throw EvalException("Unknown operator $op")
    }

    private fun toRadiansIfNeeded(x: Double) = if (angleMode == AngleMode.DEG) Math.toRadians(x) else x
    private fun fromRadiansIfNeeded(x: Double) = if (angleMode == AngleMode.DEG) Math.toDegrees(x) else x

    private fun applyFunction(name: String, x: Double): Double = when (name) {
        "sin" -> sin(toRadiansIfNeeded(x))
        "cos" -> cos(toRadiansIfNeeded(x))
        "tan" -> tan(toRadiansIfNeeded(x))
        "asin" -> fromRadiansIfNeeded(asin(x))
        "acos" -> fromRadiansIfNeeded(acos(x))
        "atan" -> fromRadiansIfNeeded(atan(x))
        "sinh" -> sinh(x)
        "cosh" -> cosh(x)
        "tanh" -> tanh(x)
        "log" -> log10(x)
        "ln" -> ln(x)
        "sqrt" -> sqrt(x)
        "cbrt" -> cbrt(x)
        "abs" -> abs(x)
        "fact" -> factorial(x)
        else -> throw EvalException("Unknown function $name")
    }

    private fun factorial(x: Double): Double {
        if (x < 0 || x != floor(x)) throw EvalException("Factorial requires a non-negative integer")
        var result = 1.0
        var n = x.toInt()
        while (n > 1) { result *= n; n-- }
        return result
    }
}
