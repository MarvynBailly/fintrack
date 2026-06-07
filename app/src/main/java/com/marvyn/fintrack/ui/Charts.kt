package com.marvyn.fintrack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marvyn.fintrack.data.Categories
import com.marvyn.fintrack.ui.theme.ExpenseRed
import com.marvyn.fintrack.ui.theme.IncomeGreen

/** Stable color per category for charts and breakdown bars. */
fun categoryColor(category: String): Color = when (category) {
    Categories.INCOME -> Color(0xFF2E7D32)
    "Groceries" -> Color(0xFF43A047)
    "Dining" -> Color(0xFFEF6C00)
    "Shopping" -> Color(0xFF8E24AA)
    "Transport" -> Color(0xFF1E88E5)
    "Bills" -> Color(0xFFC62828)
    "Entertainment" -> Color(0xFFD81B60)
    "Health" -> Color(0xFF00897B)
    else -> Color(0xFF757575)
}

data class MonthBars(
    val label: String,
    val incomeCents: Long,
    val spendingCents: Long
)

@Composable
fun MonthlyTrendChart(
    data: List<MonthBars>,
    modifier: Modifier = Modifier,
    plotHeight: Dp = 140.dp
) {
    val maxCents = (data.maxOfOrNull { maxOf(it.incomeCents, it.spendingCents) } ?: 1L)
        .coerceAtLeast(1L)

    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(plotHeight),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEach { bars ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        VerticalBar(bars.incomeCents.toFloat() / maxCents, IncomeGreen)
                        Spacer(Modifier.width(3.dp))
                        VerticalBar(bars.spendingCents.toFloat() / maxCents, ExpenseRed)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(bars.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(IncomeGreen, "Income")
            LegendDot(ExpenseRed, "Spending")
        }
    }
}

@Composable
private fun VerticalBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .width(12.dp)
            .fillMaxHeight(fraction.coerceIn(0f, 1f))
            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
            .background(color)
    )
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
