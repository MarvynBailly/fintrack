package com.marvyn.fintrack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.marvyn.fintrack.data.Categories
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    LifecycleResumeEffect(Unit) {
        viewModel.refreshNotificationAccess()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { viewModel.refreshNotificationAccess() }

    val granted = viewModel.notificationAccessGranted
    var editingCategory by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // --- Notification access ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (granted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (granted) "Notification access: ON" else "Notification access: OFF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (granted) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "FinTrack reads transaction notifications from the apps you select below. " +
                            "Nothing leaves your phone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { viewModel.openNotificationAccessSettings() }) {
                        Text(if (granted) "Manage access" else "Grant notification access")
                    }
                }
            }

            // --- Budgets ---
            Spacer(Modifier.height(24.dp))
            Text(
                "Monthly budgets",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Tap a category to set a monthly limit. Progress shows on the dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Categories.EXPENSE.forEach { category ->
                val budget = viewModel.budgets[category] ?: 0L
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(category, style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = { editingCategory = category }) {
                        Text(if (budget > 0) formatCents(budget) else "Set budget")
                    }
                }
            }

            // --- Apps to monitor ---
            Spacer(Modifier.height(24.dp))
            Text(
                "Apps to monitor",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Pick your bank, card, and payment apps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        items(viewModel.installedApps, key = { it.packageName }) { app ->
            val checked = viewModel.monitored.contains(app.packageName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Checkbox(
                    checked = checked,
                    onCheckedChange = { viewModel.toggleMonitored(app.packageName) }
                )
            }
            HorizontalDivider()
        }
    }

    editingCategory?.let { category ->
        BudgetDialog(
            category = category,
            currentCents = viewModel.budgets[category] ?: 0L,
            onDismiss = { editingCategory = null },
            onSave = { cents ->
                viewModel.setBudget(category, cents)
                editingCategory = null
            }
        )
    }

}

@Composable
private fun BudgetDialog(
    category: String,
    currentCents: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var text by remember {
        mutableStateOf(if (currentCents > 0) (currentCents / 100.0).toString() else "")
    }
    val cents = text.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$category budget") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Monthly limit (USD)") },
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(cents) }) {
                Text(if (cents > 0) "Save" else "Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
