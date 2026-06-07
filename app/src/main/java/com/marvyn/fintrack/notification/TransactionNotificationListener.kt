package com.marvyn.fintrack.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.marvyn.fintrack.data.AppDatabase
import com.marvyn.fintrack.data.Categories
import com.marvyn.fintrack.data.Prefs
import com.marvyn.fintrack.data.Transaction
import com.marvyn.fintrack.parser.TransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reads notifications from the apps the user chose to monitor, extracts a
 * transaction, and records it. Everything stays on-device.
 */
class TransactionNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val prefs = Prefs(applicationContext)
        if (!prefs.isMonitored(pkg)) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        val haystack = listOf(title, text, bigText)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val parsed = TransactionParser.parse(haystack) ?: return
        val appLabel = loadLabel(pkg)
        val description = (bigText.ifBlank { text }.ifBlank { title }).ifBlank { appLabel }
        val postedAt = sbn.postTime

        scope.launch {
            val dao = AppDatabase.get(applicationContext).transactionDao()
            // Drop repeats of the same amount from the same app within a minute.
            if (dao.countRecent(pkg, parsed.amountCents, postedAt - DEDUP_WINDOW_MS) > 0) {
                return@launch
            }
            dao.insert(
                Transaction(
                    amountCents = parsed.amountCents,
                    isIncome = parsed.isIncome,
                    description = description.take(200),
                    sourcePackage = pkg,
                    sourceApp = appLabel,
                    postedAt = postedAt,
                    category = Categories.categorize(haystack, parsed.isIncome),
                    rawText = haystack.take(500),
                    needsReview = parsed.needsReview
                )
            )
        }
    }

    private fun loadLabel(pkg: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val DEDUP_WINDOW_MS = 60_000L
    }
}
