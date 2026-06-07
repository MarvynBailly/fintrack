package com.marvyn.fintrack.data

import android.content.Context

/**
 * Stores the set of app package names the user wants FinTrack to watch for
 * transaction notifications. The notification service and the UI both read
 * from this single source of truth.
 */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)

    fun monitored(): Set<String> =
        sp.getStringSet(KEY_MONITORED, emptySet())?.toSet() ?: emptySet()

    fun isMonitored(pkg: String): Boolean = monitored().contains(pkg)

    fun setMonitored(pkg: String, on: Boolean) {
        val updated = monitored().toMutableSet()
        if (on) updated.add(pkg) else updated.remove(pkg)
        sp.edit().putStringSet(KEY_MONITORED, updated).apply()
    }

    /** Monthly budget per category, in cents. 0 means no budget set. */
    fun budget(category: String): Long = sp.getLong(BUDGET_PREFIX + category, 0L)

    fun budgets(): Map<String, Long> =
        Categories.EXPENSE.associateWith { budget(it) }.filterValues { it > 0L }

    fun setBudget(category: String, cents: Long) {
        sp.edit().putLong(BUDGET_PREFIX + category, cents.coerceAtLeast(0L)).apply()
    }

    companion object {
        private const val KEY_MONITORED = "monitored_packages"
        private const val BUDGET_PREFIX = "budget_"
    }
}
