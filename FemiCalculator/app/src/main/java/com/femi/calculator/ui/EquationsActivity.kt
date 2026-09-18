package com.femi.calculator.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.femi.calculator.databinding.ActivityEquationsBinding
import com.femi.calculator.engine.EquationEngine
import com.femi.calculator.engine.EquationException
import com.femi.calculator.engine.ExpressionEvaluator
import com.femi.calculator.engine.QuadraticSolver
import java.util.Locale

class EquationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEquationsBinding
    private val evaluator = ExpressionEvaluator(ExpressionEvaluator.AngleMode.RAD)

    private enum class Mode { LINEAR, SIMULTANEOUS, QUADRATIC, INEQUALITY, DERIVATIVE, INTEGRAL }

    private data class Tool(val label: String, val mode: Mode, val formula: String, val fieldLabels: List<String>)

    private val tools = listOf(
        Tool("Linear Equation (ax + b = 0)", Mode.LINEAR, "x = −b / a", listOf("a", "b")),
        Tool("Simultaneous Equations (2×2)", Mode.SIMULTANEOUS, "ax+by=c,  dx+ey=f", listOf("a", "b", "c", "d", "e", "f")),
        Tool("Quadratic Equation (ax²+bx+c=0)", Mode.QUADRATIC, "x = (−b ± √(b²−4ac)) / 2a", listOf("a", "b", "c")),
        Tool("Linear Inequality (ax + b [op] 0)", Mode.INEQUALITY, "Solve for x, then plot on a number line", listOf("a", "b")),
        Tool("Numerical Derivative at a point", Mode.DERIVATIVE, "f'(x0) ≈ (f(x0+h) − f(x0−h)) / 2h", listOf("x0 (point)")),
        Tool("Numerical Definite Integral", Mode.INTEGRAL, "∫f(x)dx from a to b (Simpson's rule)", listOf("a (lower bound)", "b (upper bound)"))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val adapter = ArrayAdapter(this, com.femi.calculator.R.layout.spinner_item_white, tools.map { it.label })
        adapter.setDropDownViewResource(com.femi.calculator.R.layout.spinner_dropdown_item_white)
        binding.spinnerTool.adapter = adapter
        binding.spinnerTool.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyTool(tools[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val opAdapter = ArrayAdapter(this, com.femi.calculator.R.layout.spinner_item_white, listOf("> 0", "< 0", "≥ 0", "≤ 0"))
        opAdapter.setDropDownViewResource(com.femi.calculator.R.layout.spinner_dropdown_item_white)
        binding.spinnerIneqOp.adapter = opAdapter

        applyTool(tools[0])
        binding.btnSolve.setOnClickListener { solve() }
    }

    private fun fieldViews(): List<android.widget.EditText> =
        listOf(binding.inputA, binding.inputB, binding.inputC, binding.inputD, binding.inputE, binding.inputF)

    private fun applyTool(tool: Tool) {
        binding.txtFormula.text = tool.formula
        binding.txtEquationResult.text = ""
        binding.chartContainer.removeAllViews()
        binding.chartContainer.visibility = View.GONE

        binding.inputExpr.visibility = if (tool.mode == Mode.DERIVATIVE || tool.mode == Mode.INTEGRAL) View.VISIBLE else View.GONE
        binding.inputExpr.setText("")
        binding.spinnerIneqOp.visibility = if (tool.mode == Mode.INEQUALITY) View.VISIBLE else View.GONE

        val fields = fieldViews()
        for (i in fields.indices) {
            val label = tool.fieldLabels.getOrNull(i)
            fields[i].hint = label ?: ""
            fields[i].visibility = if (label != null) View.VISIBLE else View.GONE
            fields[i].setText("")
        }
    }

    private fun num(v: android.widget.EditText): Double = v.text.toString().trim().toDouble()

    private fun buildFunction(expr: String): (Double) -> Double = { x ->
        val substituted = expr.replace("x", "($x)")
        evaluator.evaluate(substituted)
    }

    private fun solve() {
        val tool = tools[binding.spinnerTool.selectedItemPosition]
        try {
            when (tool.mode) {
                Mode.LINEAR -> {
                    val x = EquationEngine.solveLinear(num(binding.inputA), num(binding.inputB))
                    binding.txtEquationResult.text = "x = ${fmt(x)}"
                }
                Mode.SIMULTANEOUS -> {
                    val r = EquationEngine.solveSimultaneous(
                        num(binding.inputA), num(binding.inputB), num(binding.inputC),
                        num(binding.inputD), num(binding.inputE), num(binding.inputF)
                    )
                    binding.txtEquationResult.text = "x = ${fmt(r.x)}\ny = ${fmt(r.y)}"
                }
                Mode.QUADRATIC -> {
                    val r = QuadraticSolver.solve(num(binding.inputA), num(binding.inputB), num(binding.inputC))
                    binding.txtEquationResult.text = if (r.real) {
                        "x1 = ${fmt(r.x1)}\nx2 = ${fmt(r.x2)}"
                    } else {
                        "x1 = ${fmt(r.x1)} + ${fmt(r.imagPart)}i\nx2 = ${fmt(r.x2)} - ${fmt(r.imagPart)}i"
                    }
                }
                Mode.INEQUALITY -> {
                    val op = when (binding.spinnerIneqOp.selectedItemPosition) {
                        0 -> EquationEngine.InequalityOp.GT
                        1 -> EquationEngine.InequalityOp.LT
                        2 -> EquationEngine.InequalityOp.GTE
                        else -> EquationEngine.InequalityOp.LTE
                    }
                    val r = EquationEngine.solveLinearInequality(num(binding.inputA), num(binding.inputB), op)
                    binding.txtEquationResult.text = "x ${EquationEngine.opSymbol(r.op)} ${fmt(r.threshold)}"
                    val numberLine = NumberLineView(this)
                    numberLine.setThreshold(r.threshold, r.op)
                    binding.chartContainer.removeAllViews()
                    binding.chartContainer.addView(numberLine)
                    binding.chartContainer.visibility = View.VISIBLE
                }
                Mode.DERIVATIVE -> {
                    val expr = binding.inputExpr.text.toString()
                    if (expr.isBlank()) throw IllegalArgumentException("Enter an expression in x")
                    val f = buildFunction(expr)
                    val x0 = num(binding.inputA)
                    val d = EquationEngine.numericalDerivative(f, x0)
                    binding.txtEquationResult.text = "f'(${fmt(x0)}) ≈ ${fmt(d)}"
                }
                Mode.INTEGRAL -> {
                    val expr = binding.inputExpr.text.toString()
                    if (expr.isBlank()) throw IllegalArgumentException("Enter an expression in x")
                    val f = buildFunction(expr)
                    val a = num(binding.inputA)
                    val b = num(binding.inputB)
                    val result = EquationEngine.numericalIntegral(f, a, b)
                    binding.txtEquationResult.text = "∫ f(x) dx from ${fmt(a)} to ${fmt(b)} ≈ ${fmt(result)}"
                }
            }
        } catch (e: EquationException) {
            binding.txtEquationResult.text = "Error: ${e.message}"
        } catch (e: Exception) {
            binding.txtEquationResult.text = "Please check your input values."
        }
    }

    private fun fmt(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "undefined"
        return String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.')
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
