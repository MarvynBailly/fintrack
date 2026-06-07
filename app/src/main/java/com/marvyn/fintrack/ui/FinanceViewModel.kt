package com.marvyn.fintrack.ui

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marvyn.fintrack.data.AppDatabase
import com.marvyn.fintrack.data.Categories
import com.marvyn.fintrack.data.Prefs
import com.marvyn.fintrack.data.Transaction
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

data class InstalledApp(val packageName: String, val label: String)

class FinanceViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).transactionDao()
    private val prefs = Prefs(app)

    val transactions: StateFlow<List<Transaction>> =
        dao.observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    /** Package names currently being monitored (mirror of Prefs for the UI). */
    val monitored = mutableStateListOf<String>().apply { addAll(prefs.monitored()) }

    /** All launchable installed apps, sorted by label. Loaded once. */
    val installedApps = mutableStateListOf<InstalledApp>()

    var notificationAccessGranted by mutableStateOf(false)
        private set

    /** Month currently shown on the dashboard (defaults to this month). */
    var selectedMonth by mutableStateOf(YearMonth.now())
        private set

    /** Monthly budgets per category, in cents (mirror of Prefs for the UI). */
    val budgets = mutableStateMapOf<String, Long>().apply { putAll(prefs.budgets()) }

    init {
        loadInstalledApps()
    }

    fun previousMonth() {
        selectedMonth = selectedMonth.minusMonths(1)
    }

    fun nextMonth() {
        if (selectedMonth.isBefore(YearMonth.now())) {
            selectedMonth = selectedMonth.plusMonths(1)
        }
    }

    fun setBudget(category: String, cents: Long) {
        prefs.setBudget(category, cents)
        if (cents > 0) budgets[category] = cents else budgets.remove(category)
    }

    fun setCategory(transaction: Transaction, category: String) {
        viewModelScope.launch {
            dao.update(
                transaction.copy(
                    category = category,
                    isIncome = category == Categories.INCOME,
                    needsReview = false
                )
            )
        }
    }

    fun refreshNotificationAccess() {
        val ctx = getApplication<Application>()
        val flat = Settings.Secure.getString(
            ctx.contentResolver, "enabled_notification_listeners"
        ).orEmpty()
        notificationAccessGranted = flat.split(":").any { it.contains(ctx.packageName) }
    }

    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun toggleMonitored(pkg: String) {
        val on = !monitored.contains(pkg)
        prefs.setMonitored(pkg, on)
        if (on) monitored.add(pkg) else monitored.remove(pkg)
    }

    fun addManual(
        amountCents: Long,
        isIncome: Boolean,
        description: String,
        category: String,
        postedAt: Long
    ) {
        viewModelScope.launch {
            dao.insert(
                Transaction(
                    amountCents = amountCents,
                    isIncome = isIncome,
                    description = description.ifBlank { "Manual entry" },
                    sourcePackage = "manual",
                    sourceApp = "Manual",
                    postedAt = postedAt,
                    category = if (isIncome) Categories.INCOME else category,
                    needsReview = false
                )
            )
        }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { dao.delete(transaction) }
    }

    private fun loadInstalledApps() {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val apps = resolved
            .map { it.activityInfo.packageName }
            .distinct()
            .map { pkg ->
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) {
                    pkg
                }
                InstalledApp(pkg, label)
            }
            .sortedBy { it.label.lowercase() }
        installedApps.clear()
        installedApps.addAll(apps)
    }
}
