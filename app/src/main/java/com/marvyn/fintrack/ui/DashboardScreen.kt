package com.marvyn.fintrack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marvyn.fintrack.data.Categories
import com.marvyn.fintrack.data.Transaction
import com.marvyn.fintrack.ui.theme.ExpenseRed
import com.marvyn.fintrack.ui.theme.IncomeGreen
import java.time.YearMonth

@Composable
fun DashboardScreen(
    transactions: List<Transaction>,
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val month = viewModel.selectedMonth
    val monthly = transactions.filter { it.isIn(month) }
    val incomeCents = monthly.filter { it.isIncome }.sumOf { it.amountCents }
    val expenseCents = monthly.filterNot { it.isIncome }.sumOf { it.amountCents }
    val netCents = incomeCents - expenseCents
    val reviewCount = monthly.count { it.needsReview }

    val byCategory: List<Pair<String, Long>> = monthly
        .filterNot { it.isIncome }
        .groupBy { it.category ?: Categories.OTHER }
        .map { (cat, list) -> cat to list.sumOf { it.amountCents } }
        .sortedByDescending { it.second }

    val trend = buildTrend(transactions, month)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        MonthSwitcher(
            month = month,
            canGoNext = month.isBefore(YearMonth.now()),
            onPrev = viewModel::previousMonth,
            onNext = viewModel::nextMonth
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Income", formatCents(incomeCents), IncomeGreen, Modifier.weight(1f))
            StatCard("Spending", formatCents(expenseCents), ExpenseRed, Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Net this month", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    formatSignedCents(netCents),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (netCents >= 0) IncomeGreen else ExpenseRed
                )
            }
        }

        if (reviewCount > 0) {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Text(
                    "$reviewCount transaction(s) need review — open Activity to confirm income vs. spending.",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFB26A00)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Last 6 months",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        MonthlyTrendChart(trend, Modifier.fillMaxWidth())

        Spacer(Modifier.height(24.dp))
        Text(
            "Spending by category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))

        if (byCategory.isEmpty()) {
            Text(
                "No spending recorded for this month.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val maxCategory = byCategory.maxOf { it.second }.coerceAtLeast(1L)
            byCategory.forEach { (category, spent) ->
                val budget = viewModel.budgets[category] ?: 0L
                CategoryBar(category, spent, budget, maxCategory)
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MonthSwitcher(
    month: YearMonth,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
        }
        Text(
            formatMonth(month),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Next month",
                tint = if (canGoNext) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                }
            )
        }
    }
}

@Composable
private fun StatCard(title: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                amount,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun CategoryBar(category: String, spentCents: Long, budgetCents: Long, maxCents: Long) {
    val hasBudget = budgetCents > 0L
    val fraction = if (hasBudget) {
        (spentCents.toFloat() / budgetCents).coerceIn(0f, 1f)
    } else {
        (spentCents.toFloat() / maxCents).coerceIn(0f, 1f)
    }
    val overBudget = hasBudget && spentCents > budgetCents
    val barColor = if (overBudget) ExpenseRed else categoryColor(category)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(category, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (hasBudget) "${formatCents(spentCents)} / ${formatCents(budgetCents)}"
                else formatCents(spentCents),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (overBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        if (overBudget) {
            Text(
                "Over budget by ${formatCents(spentCents - budgetCents)}",
                style = MaterialTheme.typography.labelSmall,
                color = ExpenseRed
            )
        }
    }
}

private fun buildTrend(transactions: List<Transaction>, anchor: YearMonth): List<MonthBars> {
    val months = (5 downTo 0).map { anchor.minusMonths(it.toLong()) }
    return months.map { ym ->
        val inMonth = transactions.filter { it.isIn(ym) }
        MonthBars(
            label = formatMonthShort(ym),
            incomeCents = inMonth.filter { it.isIncome }.sumOf { it.amountCents },
            spendingCents = inMonth.filterNot { it.isIncome }.sumOf { it.amountCents }
        )
    }
}
