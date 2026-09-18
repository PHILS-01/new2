package com.femi.calculator.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.femi.calculator.databinding.ActivityDetailResultBinding

class DetailResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EXPRESSION = "extra_expression"
        const val EXTRA_RESULT = "extra_result"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_FRACTION = "extra_fraction"
    }

    private lateinit var binding: ActivityDetailResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val expression = intent.getStringExtra(EXTRA_EXPRESSION) ?: ""
        val result = intent.getStringExtra(EXTRA_RESULT) ?: ""
        val mode = intent.getStringExtra(EXTRA_MODE) ?: ""
        val fraction = intent.getStringExtra(EXTRA_FRACTION) ?: ""

        binding.txtDetailExpr.text = expression
        binding.txtDetailResult.text = result
        binding.txtDetailMode.text = mode

        when {
            fraction.isBlank() -> {
                binding.txtDetailFractionLabel.visibility = android.view.View.GONE
                binding.txtDetailFraction.visibility = android.view.View.GONE
            }
            fraction.matches(Regex("-?\\d+/-?\\d+")) -> {
                binding.txtDetailFractionLabel.text = "As a fraction"
                binding.txtDetailFraction.text = fraction
            }
            else -> {
                binding.txtDetailFractionLabel.text = "Steps"
                binding.txtDetailFraction.text = fraction
            }
        }

        binding.btnDetailCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("result", result))
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
