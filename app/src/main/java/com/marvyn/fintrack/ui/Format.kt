package com.marvyn.fintrack.ui

import com.marvyn.fintrack.data.Transaction
import java.text.NumberFormat
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
private val dateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)
private val monthFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)

fun formatCents(cents: Long): String = currencyFormat.format(cents / 100.0)

fun formatSignedCents(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    return sign + currencyFormat.format(kotlin.math.abs(cents) / 100.0)
}

fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(dateFormat)

fun formatMonth(month: YearMonth): String = month.format(monthFormat)

fun formatMonthShort(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMM", Locale.US))

fun Transaction.yearMonth(): YearMonth =
    YearMonth.from(
        Instant.ofEpochMilli(postedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    )

fun Transaction.isIn(month: YearMonth): Boolean = yearMonth() == month
