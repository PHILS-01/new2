package com.femi.calculator.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.femi.calculator.databinding.ActivityHistoryBinding
import com.femi.calculator.util.HistoryStore

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyStore: HistoryStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        historyStore = HistoryStore(this)
        binding.recyclerHistory.layoutManager = LinearLayoutManager(this)

        binding.btnClearHistory.setOnClickListener {
            historyStore.clear()
            loadHistory()
        }

        loadHistory()
    }

    private fun loadHistory() {
        val entries = historyStore.getAll()
        binding.recyclerHistory.adapter = HistoryAdapter(entries)
        binding.txtEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
