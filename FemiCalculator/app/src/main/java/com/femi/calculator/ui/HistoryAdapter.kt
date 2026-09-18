package com.femi.calculator.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.femi.calculator.databinding.ItemHistoryBinding
import com.femi.calculator.util.HistoryEntry

class HistoryAdapter(private val entries: List<HistoryEntry>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.binding.txtHistExpr.text = entry.expression
        holder.binding.txtHistResult.text = "= ${entry.result}"
    }

    override fun getItemCount(): Int = entries.size
}
