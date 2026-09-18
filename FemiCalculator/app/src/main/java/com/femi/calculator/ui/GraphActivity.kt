package com.femi.calculator.ui

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.femi.calculator.databinding.ActivityGraphBinding

class GraphActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGraphBinding
    private val plotColors = listOf(
        Color.parseColor("#FF9800"), // accent orange
        Color.parseColor("#4CAF50"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#E91E63"),
        Color.parseColor("#9C27B0")
    )
    private val functions = mutableListOf<GraphView.PlotFunction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGraphBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnAddFunction.setOnClickListener {
            val expr = binding.editFunction.text.toString().trim()
            if (expr.isNotEmpty()) {
                val color = plotColors[functions.size % plotColors.size]
                functions.add(GraphView.PlotFunction(expr, color))
                binding.graphView.setFunctions(functions)
                updateFunctionListLabel()
            }
        }

        binding.btnResetView.setOnClickListener { binding.graphView.resetView() }
        binding.btnClearFunctions.setOnClickListener {
            functions.clear()
            binding.graphView.setFunctions(functions)
            updateFunctionListLabel()
        }

        // Plot the default seed function immediately.
        functions.add(GraphView.PlotFunction("x^2", plotColors[0]))
        binding.graphView.setFunctions(functions)
        updateFunctionListLabel()
    }

    private fun updateFunctionListLabel() {
        binding.txtFunctionList.text = functions.joinToString("   ") { "y = ${it.expression}" }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
