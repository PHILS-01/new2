package com.femi.calculator.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.femi.calculator.databinding.ActivityMoreBinding
import com.femi.calculator.engine.*
import java.util.Locale

class MoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMoreBinding

    private data class Tool(
        val label: String,
        val fieldLabels: List<String>, // up to 6; blank = hidden
        val showList: Boolean = false,
        val showUnits: Boolean = false,
        val unitTable: Map<String, Double>? = null,
        val showExpr: Boolean = false,
        val showIneqOp: Boolean = false
    )

    private val lengthUnits = UnitConverter.lengthFactors.keys.sorted()
    private val weightUnits = UnitConverter.weightFactors.keys.sorted()
    private val volumeUnits = UnitConverter.volumeFactors.keys.sorted()
    private val tempUnits = listOf("C", "F", "K")

    private val tools = listOf(
        Tool("Statistics", emptyList(), showList = true),
        Tool("Percent Of (X% of Y)", listOf("X (percent)", "Y (of value)")),
        Tool("What Percent (X is what % of Y)", listOf("X", "Y")),
        Tool("Percent Change (X to Y)", listOf("From (X)", "To (Y)")),
        Tool("Simplify Ratio (a:b)", listOf("a", "b")),
        Tool("Solve Proportion (a:b = c:d)", listOf("a", "b", "c")),
        Tool("Quadratic Equation (ax²+bx+c=0)", listOf("a", "b", "c")),
        Tool("Geometry: Circle", listOf("Radius")),
        Tool("Geometry: Square", listOf("Side")),
        Tool("Geometry: Rectangle", listOf("Width", "Height")),
        Tool("Geometry: Triangle (sides a,b,c)", listOf("a", "b", "c")),
        Tool("Geometry: Trapezoid", listOf("Base a", "Base b", "Height", "Side c", "Side d")),
        Tool("Geometry: Sphere", listOf("Radius")),
        Tool("Geometry: Cylinder", listOf("Radius", "Height")),
        Tool("Geometry: Cone", listOf("Radius", "Height")),
        Tool("Geometry: Cube", listOf("Side")),
        Tool("Unit Convert: Length", listOf("Value"), showUnits = true, unitTable = UnitConverter.lengthFactors),
        Tool("Unit Convert: Weight", listOf("Value"), showUnits = true, unitTable = UnitConverter.weightFactors),
        Tool("Unit Convert: Volume", listOf("Value"), showUnits = true, unitTable = UnitConverter.volumeFactors),
        Tool("Unit Convert: Temperature", listOf("Value"), showUnits = true),
        Tool("Simultaneous Equations (ax+by=c, dx+ey=f)", listOf("a", "b", "c", "d", "e", "f")),
        Tool("Linear Inequality (ax + b [op] 0)", listOf("a", "b"), showIneqOp = true),
        Tool("Numerical Derivative at a point", listOf("x0 (point)"), showExpr = true),
        Tool("Numerical Definite Integral", listOf("a (lower bound)", "b (upper bound)"), showExpr = true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreBinding.inflate(layoutInflater)
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
        applyTool(tools[0])
        binding.btnCalculate.setOnClickListener { calculate() }

        val opAdapter = ArrayAdapter(this, com.femi.calculator.R.layout.spinner_item_white, listOf("> 0", "< 0", "≥ 0", "≤ 0"))
        opAdapter.setDropDownViewResource(com.femi.calculator.R.layout.spinner_dropdown_item_white)
        binding.spinnerIneqOp.adapter = opAdapter
    }

    private fun fieldViews(): List<EditText> =
        listOf(binding.inputA, binding.inputB, binding.inputC, binding.inputD, binding.inputE, binding.inputF)

    private fun applyTool(tool: Tool) {
        binding.inputList.visibility = if (tool.showList) View.VISIBLE else View.GONE
        binding.inputList.setText("")
        binding.inputExpr.visibility = if (tool.showExpr) View.VISIBLE else View.GONE
        binding.inputExpr.setText("")
        binding.spinnerIneqOp.visibility = if (tool.showIneqOp) View.VISIBLE else View.GONE
        binding.chartContainer.removeAllViews()
        binding.chartContainer.visibility = View.GONE

        val fields = fieldViews()
        for (i in fields.indices) {
            val label = tool.fieldLabels.getOrNull(i)
            fields[i].hint = label ?: ""
            fields[i].visibility = if (label != null) View.VISIBLE else View.GONE
            fields[i].setText("")
        }

        binding.rowUnits.visibility = if (tool.showUnits) View.VISIBLE else View.GONE
        if (tool.showUnits) {
            val unitNames = tool.unitTable?.keys?.sorted() ?: tempUnits
            val unitAdapter = ArrayAdapter(this, com.femi.calculator.R.layout.spinner_item_white, unitNames)
            unitAdapter.setDropDownViewResource(com.femi.calculator.R.layout.spinner_dropdown_item_white)
            binding.spinnerFromUnit.adapter = unitAdapter
            binding.spinnerToUnit.adapter = unitAdapter
            if (unitNames.size > 1) binding.spinnerToUnit.setSelection(1)
        }
        binding.txtMoreResult.text = ""
    }

    private fun num(v: EditText): Double = v.text.toString().trim().toDouble()

    private fun calculate() {
        try {
            val tool = tools[binding.spinnerTool.selectedItemPosition]
            val result: String = when {
                tool.label == "Statistics" -> statisticsResult()
                tool.label == "Percent Of (X% of Y)" -> {
                    val v = PercentageEngine.percentOf(num(binding.inputA), num(binding.inputB))
                    "Result = ${fmt(v)}"
                }
                tool.label == "What Percent (X is what % of Y)" -> {
                    val v = PercentageEngine.whatPercent(num(binding.inputA), num(binding.inputB))
                    "${fmt(v)}%"
                }
                tool.label == "Percent Change (X to Y)" -> {
                    val v = PercentageEngine.percentChange(num(binding.inputA), num(binding.inputB))
                    "${fmt(v)}% change"
                }
                tool.label == "Simplify Ratio (a:b)" -> {
                    val (a, b) = RatioEngine.simplify(num(binding.inputA).toLong(), num(binding.inputB).toLong())
                    "$a : $b"
                }
                tool.label == "Solve Proportion (a:b = c:d)" -> {
                    val d = RatioEngine.solveProportion(num(binding.inputA), num(binding.inputB), num(binding.inputC))
                    "d = ${fmt(d)}"
                }
                tool.label.startsWith("Quadratic") -> quadraticResult()
                tool.label == "Geometry: Circle" -> {
                    val s = GeometryEngine.circle(num(binding.inputA))
                    "Area = ${fmt(s.area)}\nCircumference = ${fmt(s.perimeter)}"
                }
                tool.label == "Geometry: Square" -> {
                    val s = GeometryEngine.square(num(binding.inputA))
                    "Area = ${fmt(s.area)}\nPerimeter = ${fmt(s.perimeter)}"
                }
                tool.label == "Geometry: Rectangle" -> {
                    val s = GeometryEngine.rectangle(num(binding.inputA), num(binding.inputB))
                    "Area = ${fmt(s.area)}\nPerimeter = ${fmt(s.perimeter)}"
                }
                tool.label == "Geometry: Triangle (sides a,b,c)" -> {
                    val s = GeometryEngine.triangle(num(binding.inputA), num(binding.inputB), num(binding.inputC))
                    "Area = ${fmt(s.area)}\nPerimeter = ${fmt(s.perimeter)}"
                }
                tool.label == "Geometry: Trapezoid" -> {
                    val s = GeometryEngine.trapezoid(num(binding.inputA), num(binding.inputB), num(binding.inputC), num(binding.inputD), num(binding.inputE))
                    "Area = ${fmt(s.area)}\nPerimeter = ${fmt(s.perimeter)}"
                }
                tool.label == "Geometry: Sphere" -> {
                    val s = GeometryEngine.sphere(num(binding.inputA))
                    "Volume = ${fmt(s.volume)}\nSurface Area = ${fmt(s.surfaceArea)}"
                }
                tool.label == "Geometry: Cylinder" -> {
                    val s = GeometryEngine.cylinder(num(binding.inputA), num(binding.inputB))
                    "Volume = ${fmt(s.volume)}\nSurface Area = ${fmt(s.surfaceArea)}"
                }
                tool.label == "Geometry: Cone" -> {
                    val s = GeometryEngine.cone(num(binding.inputA), num(binding.inputB))
                    "Volume = ${fmt(s.volume)}\nSurface Area = ${fmt(s.surfaceArea)}"
                }
                tool.label == "Geometry: Cube" -> {
                    val s = GeometryEngine.cube(num(binding.inputA))
                    "Volume = ${fmt(s.volume)}\nSurface Area = ${fmt(s.surfaceArea)}"
                }
                tool.label == "Unit Convert: Length" -> unitConvertResult(UnitConverter.lengthFactors)
                tool.label == "Unit Convert: Weight" -> unitConvertResult(UnitConverter.weightFactors)
                tool.label == "Unit Convert: Volume" -> unitConvertResult(UnitConverter.volumeFactors)
                tool.label == "Unit Convert: Temperature" -> temperatureConvertResult()
                tool.label.startsWith("Simultaneous") -> simultaneousResult()
                tool.label.startsWith("Linear Inequality") -> { inequalityResult(); return }
                tool.label.startsWith("Numerical Derivative") -> derivativeResult()
                tool.label.startsWith("Numerical Definite Integral") -> integralResult()
                else -> "Unsupported"
            }
            binding.txtMoreResult.text = result
        } catch (e: Exception) {
            binding.txtMoreResult.text = "Please check your input values."
        }
    }

    private fun statisticsResult(): String {
        val values = binding.inputList.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it.toDouble() }
        val r = StatisticsEngine.analyze(values)
        return buildString {
            append("Count = ${r.count}\n")
            append("Sum = ${fmt(r.sum)}\n")
            append("Mean = ${fmt(r.mean)}\n")
            append("Median = ${fmt(r.median)}\n")
            append("Mode = ${if (r.mode.isEmpty()) "none" else r.mode.joinToString(", ") { fmt(it) }}\n")
            append("Variance = ${fmt(r.variance)}\n")
            append("Std Dev = ${fmt(r.stdDev)}\n")
            append("Mean Deviation = ${fmt(r.meanDeviation)}\n")
            append("Min = ${fmt(r.min)}, Max = ${fmt(r.max)}, Range = ${fmt(r.range)}")
        }
    }

    private fun quadraticResult(): String {
        val res = QuadraticSolver.solve(num(binding.inputA), num(binding.inputB), num(binding.inputC))
        return if (res.real) {
            "x1 = ${fmt(res.x1)}\nx2 = ${fmt(res.x2)}"
        } else {
            "x1 = ${fmt(res.x1)} + ${fmt(res.imagPart)}i\nx2 = ${fmt(res.x2)} - ${fmt(res.imagPart)}i"
        }
    }

    private fun unitConvertResult(table: Map<String, Double>): String {
        val from = binding.spinnerFromUnit.selectedItem as String
        val to = binding.spinnerToUnit.selectedItem as String
        val v = UnitConverter.convert(num(binding.inputA), from, to, table)
        return "${fmt(num(binding.inputA))} $from = ${fmt(v)} $to"
    }

    private fun temperatureConvertResult(): String {
        val from = binding.spinnerFromUnit.selectedItem as String
        val to = binding.spinnerToUnit.selectedItem as String
        val value = num(binding.inputA)
        val celsius = when (from) {
            "C" -> value
            "F" -> UnitConverter.fahrenheitToCelsius(value)
            "K" -> UnitConverter.kelvinToCelsius(value)
            else -> value
        }
        val result = when (to) {
            "C" -> celsius
            "F" -> UnitConverter.celsiusToFahrenheit(celsius)
            "K" -> UnitConverter.celsiusToKelvin(celsius)
            else -> celsius
        }
        return "${fmt(value)}$from = ${fmt(result)}$to"
    }

    private fun simultaneousResult(): String {
        val r = EquationEngine.solveSimultaneous(
            num(binding.inputA), num(binding.inputB), num(binding.inputC),
            num(binding.inputD), num(binding.inputE), num(binding.inputF)
        )
        return "x = ${fmt(r.x)}\ny = ${fmt(r.y)}"
    }

    private fun inequalityResult() {
        try {
            val op = when (binding.spinnerIneqOp.selectedItemPosition) {
                0 -> EquationEngine.InequalityOp.GT
                1 -> EquationEngine.InequalityOp.LT
                2 -> EquationEngine.InequalityOp.GTE
                else -> EquationEngine.InequalityOp.LTE
            }
            val r = EquationEngine.solveLinearInequality(num(binding.inputA), num(binding.inputB), op)
            binding.txtMoreResult.text = "x ${EquationEngine.opSymbol(r.op)} ${fmt(r.threshold)}"
            val numberLine = NumberLineView(this)
            numberLine.setThreshold(r.threshold, r.op)
            binding.chartContainer.removeAllViews()
            binding.chartContainer.addView(numberLine)
            binding.chartContainer.visibility = View.VISIBLE
        } catch (e: Exception) {
            binding.txtMoreResult.text = "Please check your input values."
        }
    }

    private fun buildFunction(expr: String): (Double) -> Double = { x ->
        val evaluator = ExpressionEvaluator(ExpressionEvaluator.AngleMode.RAD)
        evaluator.evaluate(expr.replace("x", "($x)"))
    }

    private fun derivativeResult(): String {
        val expr = binding.inputExpr.text.toString()
        if (expr.isBlank()) throw IllegalArgumentException("Enter an expression in x")
        val f = buildFunction(expr)
        val x0 = num(binding.inputA)
        val d = EquationEngine.numericalDerivative(f, x0)
        return "f'(${fmt(x0)}) ≈ ${fmt(d)}"
    }

    private fun integralResult(): String {
        val expr = binding.inputExpr.text.toString()
        if (expr.isBlank()) throw IllegalArgumentException("Enter an expression in x")
        val f = buildFunction(expr)
        val a = num(binding.inputA)
        val b = num(binding.inputB)
        val result = EquationEngine.numericalIntegral(f, a, b)
        return "∫ f(x) dx from ${fmt(a)} to ${fmt(b)} ≈ ${fmt(result)}"
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.4f", v)

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
