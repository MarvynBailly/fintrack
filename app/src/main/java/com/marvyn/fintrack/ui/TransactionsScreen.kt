package com.marvyn.fintrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.marvyn.fintrack.data.Categories
import com.marvyn.fintrack.data.Transaction
import com.marvyn.fintrack.ui.theme.ExpenseRed
import com.marvyn.fintrack.ui.theme.IncomeGreen
import kotlin.math.roundToLong

@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add") }
            )
        }
    ) { inner ->
        if (transactions.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No transactions yet.\nThey'll appear here automatically once notification access is on — or tap Add, or load sample data in Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        onSetCategory = { cat -> viewModel.setCategory(tx, cat) },
                        onDelete = { viewModel.delete(tx) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { cents, isIncome, desc, category, postedAt ->
                viewModel.addManual(cents, isIncome, desc, category, postedAt)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TransactionRow(
    tx: Transaction,
    onSetCategory: (String) -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val amountColor = if (tx.isIncome) IncomeGreen else ExpenseRed
    val sign = if (tx.isIncome) "+" else "-"
    val category = tx.category ?: Categories.OTHER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tx.needsReview) Modifier.background(Color(0xFFFFF8E1)) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(categoryColor(category))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tx.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "$category · ${tx.sourceApp} · ${formatTimestamp(tx.postedAt)}" +
                    if (tx.needsReview) " · needs review" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "$sign${formatCents(tx.amountCents)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                Categories.ALL.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(if (cat == category) "✓ $cat" else cat) },
                        onClick = { menuOpen = false; onSetCategory(cat) }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuOpen = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (cents: Long, isIncome: Boolean, description: String, category: String, postedAt: Long) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(Categories.OTHER) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    val nowMillis = remember { System.currentTimeMillis() }

    val cents = amountText.toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: 0L
    val valid = cents > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add transaction") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Spending") }
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Income") }
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (USD)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true
                )
                if (!isIncome) {
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedButton(onClick = { categoryMenuOpen = true }) {
                            Text("Category: $category")
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = categoryMenuOpen,
                            onDismissRequest = { categoryMenuOpen = false }
                        ) {
                            Categories.EXPENSE.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = { category = cat; categoryMenuOpen = false }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(cents, isIncome, description, category, nowMillis) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
