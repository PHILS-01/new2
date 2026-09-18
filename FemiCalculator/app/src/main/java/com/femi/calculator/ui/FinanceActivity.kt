package com.femi.calculator.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.femi.calculator.databinding.ActivityFinanceBinding
import com.femi.calculator.engine.FinanceEngine
import java.util.Locale

class FinanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFinanceBinding

    private data class CalcType(
        val label: String,
        val hints: List<String> // 2-4 field hints; empty string hides the field
    )

    private val calcTypes = listOf(
        CalcType("Simple Interest", listOf("Principal", "Rate % per year", "Years", "")),
        CalcType("Compound Interest", listOf("Principal", "Rate % per year", "Years", "Compounds per year")),
        CalcType("Future Value", listOf("Present Value", "Rate % per year", "Years", "Compounds per year")),
        CalcType("Present Value", listOf("Future Value", "Rate % per year", "Years", "Compounds per year")),
        CalcType("Loan Payment", listOf("Principal", "Rate % per period", "Number of periods", "")),
        CalcType("Annuity Future Value", listOf("Payment per period", "Rate % per period", "Number of periods", "")),
        CalcType("Annuity Present Value", listOf("Payment per period", "Rate % per period", "Number of periods", "")),
        CalcType("Solve Interest Rate", listOf("Present Value", "Future Value", "Number of periods", "")),
        CalcType("Solve Time (periods)", listOf("Present Value", "Future Value", "Rate % per period", "")),
        CalcType("GDP (Expenditure Approach)", listOf("Consumption (C)", "Investment (I)", "Government Spending (G)", "Net Exports (X − M)")),
        CalcType("GDP Growth Rate", listOf("Previous GDP", "Current GDP", "", ""))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinanceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val adapter = ArrayAdapter(this, com.femi.calculator.R.layout.spinner_item_white, calcTypes.map { it.label })
        adapter.setDropDownViewResource(com.femi.calculator.R.layout.spinner_dropdown_item_white)
        binding.spinnerCalcType.adapter = adapter
        binding.spinnerCalcType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                applyHints(calcTypes[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        applyHints(calcTypes[0])

        binding.btnCalculate.setOnClickListener { calculate() }
    }

    private fun applyHints(type: CalcType) {
        val fields = listOf(binding.inputA, binding.inputB, binding.inputC, binding.inputD)
        for (i in fields.indices) {
            val hint = type.hints.getOrElse(i) { "" }
            fields[i].hint = hint
            fields[i].visibility = if (hint.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            fields[i].setText("")
        }
        binding.txtFinanceResult.text = ""
    }

    private fun num(view: android.widget.EditText): Double =
        view.text.toString().trim().toDouble()

    private fun calculate() {
        try {
            val type = calcTypes[binding.spinnerCalcType.selectedItemPosition]
            val a = num(binding.inputA)
            val b = num(binding.inputB)
            val c = if (binding.inputC.visibility == android.view.View.VISIBLE) num(binding.inputC) else 1.0
            val d = if (binding.inputD.visibility == android.view.View.VISIBLE) num(binding.inputD) else 1.0

            val result: String = when (type.label) {
                "Simple Interest" -> {
                    val interest = FinanceEngine.simpleInterest(a, b, c)
                    "Interest = ${fmt(interest)}\nTotal = ${fmt(a + interest)}"
                }
                "Compound Interest" -> {
                    val interest = FinanceEngine.compoundInterest(a, b, c, d)
                    val amount = FinanceEngine.compoundAmount(a, b, c, d)
                    "Interest = ${fmt(interest)}\nFinal Amount = ${fmt(amount)}"
                }
                "Future Value" -> "Future Value = ${fmt(FinanceEngine.futureValue(a, b, c, d))}"
                "Present Value" -> "Present Value = ${fmt(FinanceEngine.presentValue(a, b, c, d))}"
                "Loan Payment" -> "Payment per period = ${fmt(FinanceEngine.loanPayment(a, b, c))}"
                "Annuity Future Value" -> "Future Value = ${fmt(FinanceEngine.annuityFutureValue(a, b, c))}"
                "Annuity Present Value" -> "Present Value = ${fmt(FinanceEngine.annuityPresentValue(a, b, c))}"
                "Solve Interest Rate" -> "Rate = ${fmt(FinanceEngine.solveRatePercent(a, b, c))}% per period"
                "Solve Time (periods)" -> "Time = ${fmt(FinanceEngine.solveTimePeriods(a, b, c))} periods"
                "GDP (Expenditure Approach)" -> "GDP = ${fmt(FinanceEngine.gdpExpenditure(a, b, c, d))}"
                "GDP Growth Rate" -> "Growth Rate = ${fmt(FinanceEngine.gdpGrowthRate(a, b))}%"
                else -> "Unsupported"
            }
            binding.txtFinanceResult.text = result
        } catch (e: Exception) {
            binding.txtFinanceResult.text = "Please fill in all fields with valid numbers."
        }
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.4f", v)

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
