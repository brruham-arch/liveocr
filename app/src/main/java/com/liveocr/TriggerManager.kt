package com.liveocr

import android.content.Context

object TriggerManager {

    private const val PREFS_NAME = "liveocr_prefs"
    private const val KEY_TRIGGERS = "triggers"
    private const val KEY_INTERVAL = "interval"
    private const val SEPARATOR = "|||"

    /**
     * Add a keyword trigger with optional shell action.
     * action = "" means just show notification.
     */
    fun addTrigger(context: Context, keyword: String, action: String) {
        val triggers = getTriggers(context).toMutableMap()
        triggers[keyword.lowercase()] = action
        saveTriggers(context, triggers)
    }

    fun removeTrigger(context: Context, keyword: String) {
        val triggers = getTriggers(context).toMutableMap()
        triggers.remove(keyword.lowercase())
        saveTriggers(context, triggers)
    }

    fun getTriggers(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TRIGGERS, "") ?: return emptyMap()
        if (raw.isEmpty()) return emptyMap()
        return raw.split(SEPARATOR).mapNotNull { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
    }

    private fun saveTriggers(context: Context, triggers: Map<String, String>) {
        val raw = triggers.entries.joinToString(SEPARATOR) { "${it.key}=${it.value}" }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TRIGGERS, raw).apply()
    }

    fun setInterval(context: Context, seconds: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_INTERVAL, seconds).apply()
    }

    fun getInterval(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_INTERVAL, 2L)
    }

    /**
     * Check if any keyword matches the given OCR text.
     * Returns list of matched (keyword, action) pairs.
     */
    fun checkTriggers(context: Context, text: String): List<Pair<String, String>> {
        val lower = text.lowercase()
        return getTriggers(context).entries
            .filter { lower.contains(it.key) }
            .map { it.key to it.value }
    }
}
