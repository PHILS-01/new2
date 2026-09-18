package com.femi.calculator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.femi.calculator.databinding.ActivityMainBinding
import com.femi.calculator.engine.EquationEngine
import com.femi.calculator.engine.ExpressionEvaluator
import com.femi.calculator.engine.Fraction
import com.femi.calculator.engine.IntegralEngine
import com.femi.calculator.engine.SubjectException
import com.femi.calculator.engine.SubjectSolver
import com.femi.calculator.util.HistoryStore
import com.femi.calculator.util.MemoryStore
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val evaluator = ExpressionEvaluator()
    private lateinit var historyStore: HistoryStore
    private lateinit var memoryStore: MemoryStore
    private var expression = StringBuilder()
    private var lastResult: Double? = null
    private var showAsFraction = false
    private var scientificFormat = false
    private var justEvaluated = false
    private var shiftActive = false

    private var lastDetailExpression = ""
    private var lastDetailAnswer = ""
    private var lastDetailSteps = ""

    private data class DualKey(val primary: String, val primaryToken: String, val secondary: String, val secondaryToken: String)

    private val relationalOps = listOf("=", ">", "<", "≥", "≤")
    private val superscriptMap = mapOf('0' to '⁰','1' to '¹','2' to '²','3' to '³','4' to '⁴','5' to '⁵','6' to '⁶','7' to '⁷','8' to '⁸','9' to '⁹','-' to '⁻')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        historyStore = HistoryStore(this)
        memoryStore = MemoryStore(this)

        wireNavigation()
        wireKeypad()
        wireFunctionButtons()
        wireMemoryAndVariables()
        wireDisplayControls()
        wireEnter()
        updateMemoryIndicator()
        updateShiftVisual()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(com.femi.calculator.R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_graph -> startActivity(Intent(this, GraphActivity::class.java))
            R.id.menu_matrix -> startActivity(Intent(this, MatrixActivity::class.java))
            R.id.menu_finance -> startActivity(Intent(this, FinanceActivity::class.java))
            R.id.menu_more -> startActivity(Intent(this, MoreActivity::class.java))
            R.id.menu_science -> startActivity(Intent(this, ScienceActivity::class.java))
            R.id.menu_statistics -> startActivity(Intent(this, StatisticsActivity::class.java))
            R.id.menu_history -> startActivity(Intent(this, HistoryActivity::class.java))
        }
        return true
    }

    private fun wireNavigation() {
        binding.navStatistics.setOnClickListener { startActivity(Intent(this, StatisticsActivity::class.java)) }
        binding.navMatrices.setOnClickListener { startActivity(Intent(this, MatrixActivity::class.java)) }
        binding.navScience.setOnClickListener { startActivity(Intent(this, ScienceActivity::class.java)) }
    }

    private fun wireKeypad() {
        val digits = mapOf(binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2", binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5", binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8", binding.btn9 to "9", binding.btnDot to ".")
        digits.forEach { (button, token) -> button.setOnClickListener { appendInput(token, true) } }
        binding.btnPlus.setOnClickListener { appendOperator("+") }
        binding.btnMinus.setOnClickListener { appendOperator("-") }
        binding.btnMultiply.setOnClickListener { appendOperator("*") }
        binding.btnDivide.setOnClickListener { appendOperator("/") }
        binding.btnOpenParen.setOnClickListener { if (shiftActive) { appendInput("!", false); consumeShift() } else appendInput("(", false) }
        binding.btnCloseParen.setOnClickListener { if (shiftActive) showNcrDialog() else appendInput(")", false) }
        binding.btnSign.setOnClickListener {
            if (expression.isNotEmpty() && expression[0] == '-') expression.deleteCharAt(0) else expression.insert(0, "-")
            refreshExpressionDisplay()
        }
        binding.btnExp.setOnClickListener { appendInput("×10^", false) }
        binding.btnClear.setOnClickListener {
            expression.clear(); lastResult = null; justEvaluated = false
            binding.txtResult.text = "0"; binding.txtDetailsLink.visibility = View.GONE
            refreshExpressionDisplay()
        }
        binding.btnDel.setOnClickListener {
            if (expression.isNotEmpty()) expression.deleteCharAt(expression.length - 1)
            refreshExpressionDisplay()
        }
        binding.btnEnter.setOnClickListener { evaluateOrSolve() }
    }

    private fun wireDisplayControls() {
        binding.btnDeg.setOnClickListener {
            evaluator.angleMode = if (evaluator.angleMode == ExpressionEvaluator.AngleMode.DEG) ExpressionEvaluator.AngleMode.RAD else ExpressionEvaluator.AngleMode.DEG
            binding.btnDeg.text = evaluator.angleMode.name
        }
        binding.btnShift.setOnClickListener { shiftActive = !shiftActive; updateShiftVisual() }
        binding.btnFse.setOnClickListener {
            scientificFormat = !scientificFormat
            lastResult?.let { showResult(it) }
            Toast.makeText(this, if (scientificFormat) "Scientific format ON" else "Scientific format OFF", Toast.LENGTH_SHORT).show()
        }
        binding.btnMenu.setOnClickListener { showToolsMenu(binding.btnMenu) }
        binding.btnLeft.setOnClickListener { moveCursor(-1) }
        binding.btnRight.setOnClickListener { moveCursor(1) }
        binding.txtResult.setOnLongClickListener { copyToClipboard(binding.txtResult.text.toString()); true }
        binding.txtExpression.setOnLongClickListener { showCopyPasteMenu(it); true }
        binding.txtDetailsLink.setOnClickListener { openDetailPage() }
    }

    private fun showToolsMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Graph")
        popup.menu.add("History")
        popup.menu.add("Finance")
        popup.menu.add("More tools")
        popup.setOnMenuItemClickListener {
            when (it.title.toString()) {
                "Graph" -> startActivity(Intent(this, GraphActivity::class.java))
                "History" -> startActivity(Intent(this, HistoryActivity::class.java))
                "Finance" -> startActivity(Intent(this, FinanceActivity::class.java))
                else -> startActivity(Intent(this, MoreActivity::class.java))
            }; true
        }
        popup.show()
    }

    private fun moveCursor(delta: Int) {
        if (expression.isEmpty()) return
        val text = expression.toString()
        expression.clear()
        val pos = (text.length + delta).coerceIn(0, text.length)
        expression.append(text.substring(0, pos)).append(text.substring(pos))
        refreshExpressionDisplay()
    }

    private fun wireFunctionButtons() {
        binding.btnDrg.setOnClickListener { binding.btnDeg.performClick() }
        binding.btnSigma.setOnClickListener { showSumProductDialog() }
        binding.btnDerivative.setOnClickListener { if (shiftActive) { consumeShift(); showIntegralDialog() } else showDerivativeDialog() }
                binding.btnSin.setOnClickListener { appendFunction(if (shiftActive) "asin(" else "sin(") }
        binding.btnCos.setOnClickListener { appendFunction(if (shiftActive) "acos(" else "cos(") }
        binding.btnTan.setOnClickListener { appendFunction(if (shiftActive) "atan(" else "tan(") }
        binding.btnImag.setOnClickListener { appendInput("i", false) }
        binding.btnLogBase.setOnClickListener { showLogBaseDialog() }
        binding.btnPower.setOnClickListener { appendInput(if (shiftActive) "^3" else "^2", false) }
        binding.btnRoot.setOnClickListener { appendFunction(if (shiftActive) "cbrt(" else "sqrt(") }
        binding.btnRootIndex.setOnClickListener { showNthRootDialog() }
        binding.btnTenPower.setOnClickListener { appendInput(if (shiftActive) "log(" else "10^", false) }
        binding.btnNaturalLog.setOnClickListener { appendInput(if (shiftActive) "ln(" else "e^", false) }
        binding.btnLim.setOnClickListener { showLimitDialog() }
        binding.btnFrac.setOnClickListener { showFractionDialog() }
        binding.btnChange.setOnClickListener { onChangePressed() }
        binding.btnMemoryPlus.setOnClickListener {
            if (shiftActive) { appendInput(formatNumber(memoryStore.get()), false); consumeShift() }
            else { memoryStore.add(lastResult ?: currentValueOrNull() ?: 0.0); updateMemoryIndicator() }
        }
        binding.btnMemoryMinus.setOnClickListener {
            if (shiftActive) { memoryStore.set(lastResult ?: currentValueOrNull() ?: 0.0); updateMemoryIndicator(); consumeShift() }
            else { memoryStore.subtract(lastResult ?: currentValueOrNull() ?: 0.0); updateMemoryIndicator() }
        }
        binding.btnHistory.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.btnVariable.setOnClickListener { showVariablePicker { appendInput(it, false) } }
        binding.btnVarX.setOnClickListener { appendInput(if (shiftActive) "y" else "x", false); consumeShift() }
        binding.btnPiHvpLabel.setOnClickListener { appendFunction(if (shiftActive) "e" else "pi") }
    }

    private fun wireMemoryAndVariables() { binding.btnMc.setOnClickListener { memoryStore.clear(); updateMemoryIndicator() } }

    private fun appendFunction(token: String) { appendInput(token, false); consumeShift() }
    private fun appendInput(token: String, numeric: Boolean) {
        if (justEvaluated && (numeric || token == "(" || token.firstOrNull()?.isLetter() == true)) { expression.clear(); justEvaluated = false }
        expression.append(token); refreshExpressionDisplay()
    }
    private fun appendOperator(token: String) { justEvaluated = false; expression.append(token); refreshExpressionDisplay() }
    private fun consumeShift() { if (shiftActive) { shiftActive = false; updateShiftVisual() } }
    private fun updateShiftVisual() { binding.btnShift.setBackgroundResource(if (shiftActive) R.drawable.calc_button_orange else R.drawable.calc_button_green) }

    private fun updateMemoryIndicator() {
        val m = memoryStore.get()
        binding.txtMemory.text = if (m == 0.0) "" else "M = ${formatNumber(m)}"
    }

    private fun showVariablePicker(onPicked: (String) -> Unit) {
        val letters = ('a'..'z').map { it.toString() }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Choose a variable (a–z)").setItems(letters) { _, which -> onPicked(letters[which]) }.show()
    }

    private fun showFractionDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(2)) }
        val numerator = EditText(this).apply { hint = "Numerator (number or a–z)"; inputType = InputType.TYPE_CLASS_TEXT }
        val denominator = EditText(this).apply { hint = "Denominator (number or a–z)"; inputType = InputType.TYPE_CLASS_TEXT }
        box.addView(numerator); box.addView(denominator)
        AlertDialog.Builder(this).setTitle("Fraction").setView(box).setPositiveButton("Insert") { _, _ ->
            val n = numerator.text.toString().trim(); val d = denominator.text.toString().trim()
            if (n.isNotEmpty() && d.isNotEmpty()) { appendInput("($n)/($d)", false) } else Toast.makeText(this, "Enter both numerator and denominator", Toast.LENGTH_SHORT).show()
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showLogBaseDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(2)) }
        val base = EditText(this).apply { hint = "Base x"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val value = EditText(this).apply { hint = "y"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        box.addView(base); box.addView(value)
        AlertDialog.Builder(this).setTitle("logₓ(y)").setView(box).setPositiveButton("Calculate") { _, _ ->
            try {
                val b = base.text.toString().toDouble(); val y = value.text.toString().toDouble()
                if (b <= 0 || b == 1.0 || y <= 0) throw IllegalArgumentException()
                presentDirectResult(ln(y) / ln(b), "log_$b($y)", "Change of base: ln(y) / ln(base)")
            } catch (_: Exception) { Toast.makeText(this, "Base must be > 0 and ≠ 1; y must be > 0", Toast.LENGTH_LONG).show() }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showNcrDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(2)) }
        val n = EditText(this).apply { hint = "n"; inputType = InputType.TYPE_CLASS_NUMBER }
        val r = EditText(this).apply { hint = "r"; inputType = InputType.TYPE_CLASS_NUMBER }
        box.addView(n); box.addView(r)
        AlertDialog.Builder(this).setTitle("nCr").setView(box).setPositiveButton("Calculate") { _, _ ->
            try { val nv=n.text.toString().toInt(); val rv=r.text.toString().toInt(); if (nv<0 || rv<0 || rv>nv) throw IllegalArgumentException(); var ans=1.0; for(i in 1..rv) ans=ans*(nv-rv+i)/i; presentDirectResult(ans, "$nv C $rv", "Combination nCr") } catch (_:Exception) { Toast.makeText(this,"Require integers with 0 ≤ r ≤ n",Toast.LENGTH_SHORT).show() }; consumeShift()
        }.setNegativeButton("Cancel") { _, _ -> consumeShift() }.show()
    }

    private fun showNthRootDialog() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(2)) }
        val index = EditText(this).apply { hint = "Root index n"; inputType = InputType.TYPE_CLASS_NUMBER }
        val value = EditText(this).apply { hint = "Value x"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        box.addView(index); box.addView(value)
        AlertDialog.Builder(this).setTitle("n-th root").setView(box).setPositiveButton("Calculate") { _, _ ->
            try { presentDirectResult(value.text.toString().toDouble().pow(1.0 / index.text.toString().toDouble()), "${index.text}√${value.text}", "n-th root") } catch (_: Exception) { Toast.makeText(this, "Check the root index and value", Toast.LENGTH_SHORT).show() }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showSumProductDialog() {
        val input = EditText(this).apply { hint = "Numbers separated by commas"; inputType = InputType.TYPE_CLASS_TEXT }
        AlertDialog.Builder(this).setTitle(if (shiftActive) "Π Product" else "Σ Sum").setView(input).setPositiveButton("Calculate") { _, _ ->
            try {
                val values = input.text.toString().split(",").filter { it.isNotBlank() }.map { it.trim().toDouble() }
                if (values.isEmpty()) throw IllegalArgumentException()
                val result = if (shiftActive) values.fold(1.0) { a, b -> a * b } else values.sum()
                presentDirectResult(result, if (shiftActive) "Π(${values.joinToString(",")})" else "Σ(${values.joinToString(",")})", "Statistical sum/product")
                consumeShift()
            } catch (_: Exception) { Toast.makeText(this, "Enter comma-separated numbers", Toast.LENGTH_SHORT).show() }
        }.setNegativeButton("Cancel") { _, _ -> consumeShift() }.show()
    }

    private fun showDerivativeDialog() {
        val box = calculusBox("f(x)", "x", "At x (optional)")
        AlertDialog.Builder(this).setTitle("d/dx").setView(box.container).setPositiveButton("Calculate") { _, _ ->
            try {
                val expr = box.a.text.toString().trim(); val variable = box.b.text.toString().trim().ifEmpty { "x" }; val x0 = box.c.text.toString().trim()
                if (expr.isEmpty()) throw IllegalArgumentException()
                val derivative = IntegralEngine.symbolicDerivative(expr, variable[0])
                if (x0.isEmpty()) binding.txtResult.text = "$derivative + C?".replace(" + C?", "") else presentDirectResult(evaluator.evaluate(derivative.replace(variable, "(${x0})")), "d/d$variable($expr)", "Numerical derivative from symbolic derivative")
            } catch (_: Exception) { Toast.makeText(this, "Could not differentiate this expression", Toast.LENGTH_LONG).show() }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun showIntegralDialog() {
        val box = calculusBox("f(x)", "x", "Lower bound (optional)")
        val upper = EditText(this).apply { hint = "Upper bound (optional)"; inputType = InputType.TYPE_CLASS_TEXT }
        box.container.addView(upper)
        AlertDialog.Builder(this).setTitle("∫ f(x) dx").setView(box.container).setPositiveButton("Calculate") { _, _ ->
            try {
                val expr = box.a.text.toString().trim(); val variable = box.b.text.toString().trim().ifEmpty { "x" }; val lo = box.c.text.toString().trim(); val hi = upper.text.toString().trim()
                if (expr.isEmpty()) throw IllegalArgumentException()
                if (lo.isEmpty() && hi.isEmpty()) {
                    val anti = IntegralEngine.symbolicIntegral(expr, variable[0])
                    presentTextResult("∫($expr)d$variable = $anti + C", "Indefinite integral", "Symbolic antiderivative")
                } else {
                    if (lo.isEmpty() || hi.isEmpty()) throw IllegalArgumentException("Enter both bounds or leave both empty")
                    val a = lo.toDouble(); val b = hi.toDouble()
                    val value = EquationEngine.numericalIntegral({ x -> evaluator.evaluate(expr.replace(variable, "($x)")) }, a, b)
                    presentDirectResult(value, "∫[$a,$b] $expr d$variable", "Simpson numerical integration")
                }
            } catch (e: Exception) { Toast.makeText(this, e.message ?: "Could not integrate", Toast.LENGTH_LONG).show() }
        }.setNegativeButton("Cancel", null).show()
    }

    private data class CalculusBox(val container: LinearLayout, val a: EditText, val b: EditText, val c: EditText)
    private fun calculusBox(aHint: String, bHint: String, cHint: String): CalculusBox {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(2)) }
        val a = EditText(this).apply { hint = aHint; inputType = InputType.TYPE_CLASS_TEXT }
        val b = EditText(this).apply { hint = "Variable: $bHint"; inputType = InputType.TYPE_CLASS_TEXT }
        val c = EditText(this).apply { hint = cHint; inputType = InputType.TYPE_CLASS_TEXT }
        container.addView(a); container.addView(b); container.addView(c)
        return CalculusBox(container, a, b, c)
    }

    private fun showLimitDialog() {
        val box = calculusBox("f(x)", "x", "Approach value")
        AlertDialog.Builder(this).setTitle("lim").setView(box.container).setPositiveButton("Calculate") { _, _ ->
            try {
                val expr = box.a.text.toString(); val variable = box.b.text.toString().ifEmpty { "x" }; val point = box.c.text.toString().toDouble(); val h = 1e-5
                val left = evaluator.evaluate(expr.replace(variable, "(${point - h})")); val right = evaluator.evaluate(expr.replace(variable, "(${point + h})"))
                presentDirectResult((left + right) / 2.0, "lim $variable→$point $expr", "Two-sided numerical limit")
            } catch (_: Exception) { Toast.makeText(this, "Could not evaluate the limit", Toast.LENGTH_SHORT).show() }
        }.setNegativeButton("Cancel", null).show()
    }

    private fun presentDirectResult(value: Double, expr: String, steps: String) {
        lastResult = value; showResult(value); lastDetailExpression = expr; lastDetailAnswer = formatNumber(value); lastDetailSteps = steps; binding.txtDetailsLink.visibility = View.VISIBLE; historyStore.add(expr, formatNumber(value)); expression = StringBuilder(formatNumber(value)); justEvaluated = true; refreshExpressionDisplay()
    }
    private fun presentTextResult(text: String, expr: String, steps: String) { lastResult = null; binding.txtResult.text = text; lastDetailExpression = expr; lastDetailAnswer = text; lastDetailSteps = steps; binding.txtDetailsLink.visibility = View.VISIBLE; historyStore.add(expr, text) }

    private fun onChangePressed() {
        val exprStr = expression.toString()
        if (!exprStr.contains("=")) { Toast.makeText(this, "Type an equation with '=' first", Toast.LENGTH_SHORT).show(); return }
        showVariablePicker { letter ->
            try {
                val normalized = exprStr.replace("×", "*").replace("÷", "/").replace("−", "-")
                val subject = SubjectSolver.makeSubject(normalized, letter[0])
                presentTextResult(subject, prettifyForDisplay(exprStr), "Made '$letter' the subject of the equation.")
            } catch (e: SubjectException) { Toast.makeText(this, e.message ?: "Could not isolate the variable", Toast.LENGTH_LONG).show() }
        }
    }

    private fun currentValueOrNull(): Double? = try { evaluator.evaluate(expression.toString()) } catch (_: Exception) { null }

    private fun wireEnter() {
        // Enter is wired in keypad; this method intentionally remains as the single entry point for future keyboard support.
    }

    private fun evaluateOrSolve() {
        val exprStr = expression.toString().trim(); if (exprStr.isEmpty()) return
        val rel = relationalOps.firstNotNullOfOrNull { op -> exprStr.indexOf(op).takeIf { it >= 0 }?.let { op to it } }
        if (rel != null) solveEquationOrInequality(exprStr, rel.first, rel.second) else evaluatePlain(exprStr)
    }

    private fun evaluatePlain(exprStr: String) {
        try {
            val result = evaluator.evaluate(exprStr); lastResult = result; showResult(result)
            historyStore.add(prettifyForDisplay(exprStr), formatNumber(result)); lastDetailExpression = prettifyForDisplay(exprStr); lastDetailAnswer = formatNumber(result); lastDetailSteps = ""; binding.txtDetailsLink.visibility = View.VISIBLE
            expression = StringBuilder(formatNumber(result)); justEvaluated = true; refreshExpressionDisplay()
        } catch (_: Exception) { binding.txtResult.text = "Error"; binding.txtDetailsLink.visibility = View.GONE }
    }

    private fun solveEquationOrInequality(exprStr: String, op: String, opIndex: Int) {
        val left = exprStr.substring(0, opIndex); val right = exprStr.substring(opIndex + op.length)
        if (left.isBlank() || right.isBlank()) { binding.txtResult.text = "Error"; return }
        val otherVarPattern = Regex("(?<![a-zA-Z])[a-wyzA-WYZ](?![a-zA-Z])")
        if (otherVarPattern.containsMatchIn(left + right)) { Toast.makeText(this, "Only single-variable x equations solve automatically", Toast.LENGTH_LONG).show(); return }
        val f: (Double) -> Double = { x -> evaluator.evaluate(left.replace("x", "($x)")) - evaluator.evaluate(right.replace("x", "($x)")) }
        val poly = try { EquationEngine.tryExtractPolynomial(f) } catch (_: Exception) { null }
        if (poly == null) { binding.txtResult.text = "Not solvable as linear/quadratic in x"; return }
        val isLinear = abs(poly.a) < 1e-9
        try {
            if (op == "=") {
                if (isLinear) presentSolveResult("x = ${formatNumber(EquationEngine.solveLinear(poly.b, poly.c))}", prettifyForDisplay(exprStr), "Linear equation")
                else {
                    val r = QuadraticSolver.solve(poly.a, poly.b, poly.c)
                    val ans = if (r.real) "x1 = ${formatNumber(r.x1)}, x2 = ${formatNumber(r.x2)}" else "x1 = ${formatNumber(r.x1)}+${formatNumber(r.imagPart)}i, x2 = ${formatNumber(r.x2)}-${formatNumber(r.imagPart)}i"
                    presentSolveResult(ans, prettifyForDisplay(exprStr), "Quadratic equation")
                }
            } else {
                val ineqOp = when (op) { ">" -> EquationEngine.InequalityOp.GT; "<" -> EquationEngine.InequalityOp.LT; "≥" -> EquationEngine.InequalityOp.GTE; else -> EquationEngine.InequalityOp.LTE }
                if (isLinear) {
                    val r = EquationEngine.solveLinearInequality(poly.b, poly.c, ineqOp); presentSolveResult("x ${EquationEngine.opSymbol(r.op)} ${formatNumber(r.threshold)}", prettifyForDisplay(exprStr), "Linear inequality")
                } else {
                    val r = EquationEngine.solveQuadraticInequality(poly.a, poly.b, poly.c, ineqOp)
                    val ans = when (r.type) { EquationEngine.QuadIneqType.ALL_REALS -> "All real numbers"; EquationEngine.QuadIneqType.NO_SOLUTION -> "No solution"; EquationEngine.QuadIneqType.INTERVAL -> "${formatNumber(r.lo)} ${if (r.inclusive) "≤" else "<"} x ${if (r.inclusive) "≤" else "<"} ${formatNumber(r.hi)}"; EquationEngine.QuadIneqType.OUTER -> "x ${if (r.inclusive) "≤" else "<"} ${formatNumber(r.lo)} or x ${if (r.inclusive) "≥" else ">"} ${formatNumber(r.hi)}" }
                    presentSolveResult(ans, prettifyForDisplay(exprStr), "Quadratic inequality")
                }
            }
        } catch (_: Exception) { binding.txtResult.text = "Error solving" }
    }

    private fun presentSolveResult(answer: String, exprDisplay: String, steps: String) { binding.txtResult.text = answer; lastResult = null; historyStore.add(exprDisplay, answer); lastDetailExpression = exprDisplay; lastDetailAnswer = answer; lastDetailSteps = steps; binding.txtDetailsLink.visibility = View.VISIBLE }

    private fun showResult(result: Double) {
        binding.txtResult.text = if (showAsFraction) try { Fraction.fromDecimal(result).toString() } catch (_: Exception) { formatNumber(result) } else formatNumber(result)
    }

    private fun showCopyPasteMenu(view: View) {
        val popup = PopupMenu(this, view); popup.menu.add("Copy"); popup.menu.add("Paste")
        popup.setOnMenuItemClickListener { if (it.title == "Copy") copyToClipboard(expression.toString()) else pasteFromClipboard(); true }; popup.show()
    }
    private fun copyToClipboard(text: String) { if (text.isBlank()) return; (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("calculator", text)); Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show() }
    private fun pasteFromClipboard() { val clip = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip; if (clip != null && clip.itemCount > 0) { expression.append(clip.getItemAt(0).coerceToText(this).toString()); refreshExpressionDisplay() } }

    private fun openDetailPage() { startActivity(Intent(this, DetailResultActivity::class.java).apply { putExtra(DetailResultActivity.EXTRA_EXPRESSION, lastDetailExpression); putExtra(DetailResultActivity.EXTRA_RESULT, lastDetailAnswer); putExtra(DetailResultActivity.EXTRA_MODE, evaluator.angleMode.name); putExtra(DetailResultActivity.EXTRA_FRACTION, lastDetailSteps) }) }

    private fun refreshExpressionDisplay() { binding.txtExpression.text = prettifyForDisplay(expression.toString()) }
    private fun prettifyForDisplay(raw: String): String {
        var s = raw.replace("asin(", "sin⁻¹(").replace("acos(", "cos⁻¹(").replace("atan(", "tan⁻¹(").replace("sqrt(", "√(").replace("cbrt(", "∛(").replace("*", "×").replace("/", "÷")
        val sb = StringBuilder(); var i = 0
        while (i < s.length) {
            if (s[i] == '^') { var j = i + 1; if (j < s.length && s[j] == '-') j++; val start = j; while (j < s.length && s[j].isDigit()) j++; if (j > start) { sb.append(s.substring(i + 1, j).map { superscriptMap[it] ?: it }.joinToString("")); i = j; continue } }
            sb.append(s[i]); i++
        }
        return sb.toString()
    }
    private fun formatNumber(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "Error"
        if (scientificFormat && v != 0.0) return String.format(Locale.US, "%.8e", v)
        if (v == v.toLong().toDouble() && abs(v) < 1e15) return v.toLong().toString()
        return String.format(Locale.US, "%.10f", v).trimEnd('0').trimEnd('.')
    }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
