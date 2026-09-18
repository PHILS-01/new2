package com.femi.calculator.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(val expression: String, val result: String, val timestamp: Long)

class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("femi_calc_history", Context.MODE_PRIVATE)
    private val key = "entries"
    private val maxEntries = 200

    fun add(expression: String, result: String) {
        val list = getAll().toMutableList()
        list.add(0, HistoryEntry(expression, result, System.currentTimeMillis()))
        while (list.size > maxEntries) list.removeAt(list.size - 1)
        save(list)
    }

    fun getAll(): List<HistoryEntry> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        val arr = JSONArray(raw)
        val out = mutableListOf<HistoryEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(HistoryEntry(o.getString("expr"), o.getString("result"), o.getLong("ts")))
        }
        return out
    }

    fun clear() {
        prefs.edit().remove(key).apply()
    }

    private fun save(list: List<HistoryEntry>) {
        val arr = JSONArray()
        for (e in list) {
            val o = JSONObject()
            o.put("expr", e.expression)
            o.put("result", e.result)
            o.put("ts", e.timestamp)
            arr.put(o)
        }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}

/** Simple persistent memory (M+, M-, MR, MC) for the calculator. */
class MemoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("femi_calc_memory", Context.MODE_PRIVATE)
    private val key = "memory_value"

    fun get(): Double = java.lang.Double.longBitsToDouble(
        prefs.getLong(key, java.lang.Double.doubleToLongBits(0.0))
    )

    fun set(value: Double) {
        prefs.edit().putLong(key, java.lang.Double.doubleToLongBits(value)).apply()
    }

    fun add(value: Double) = set(get() + value)
    fun subtract(value: Double) = set(get() - value)
    fun clear() = set(0.0)
}
