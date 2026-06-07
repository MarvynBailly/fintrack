package com.marvyn.fintrack.parser

import kotlin.math.roundToLong

/**
 * Extracts a dollar amount from a notification's text and tries to classify it
 * as income or expense by keyword. Anything ambiguous is flagged for review so
 * the user can correct it in the app.
 *
 * Credit-card notifications get special handling: a charge/purchase always
 * counts as an expense even though the word "credit" appears, and purely
 * informational card alerts (statements, limits, payment reminders, rewards)
 * are skipped so they don't become phantom transactions.
 */
object TransactionParser {

    // Matches $1,234.56 / $1234 / $ 99.9 etc. Captures the numeric portion.
    private val amountRegex = Regex(
        """\$\s?([0-9]{1,3}(?:,[0-9]{3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""
    )

    // Notifications that report account status rather than a money movement.
    // These often carry a dollar amount (a limit, a statement total, an amount
    // due) but recording it would invent a transaction that never happened.
    // Common on credit cards.
    private val ignorePhrases = listOf(
        "available credit", "credit limit", "credit line",
        "statement balance", "statement is ready", "statement is now available",
        "new statement", "e-statement",
        "minimum payment", "payment due", "payment is due", "amount due", "due date",
        "autopay is scheduled", "autopay scheduled", "automatic payment is scheduled",
        "rewards balance", "points balance", "reward points"
    )

    private val incomeKeywords = listOf(
        "deposit", "deposited", "received", "credited", "credit of", "refund",
        "refunded", "cashback", "cash back", "reimburs", "payroll", "direct deposit",
        "you got", "added to your", "incoming", "salary", "transfer in", "received from"
    )

    private val expenseKeywords = listOf(
        "spent", "paid", "payment of", "purchase", "purchased", "debited", "debit of",
        "withdraw", "withdrawn", "sent", "charged", "charge of", "bought",
        "you paid", "transaction of", "transfer to", "sent to", "withdrawal",
        // Credit-card charge phrasings.
        "purchase approved", "charge approved", "transaction approved",
        "swiped", "card was used", "made a purchase"
    )

    // Unambiguous outflow words. When one of these is present the notification
    // is a charge/purchase, so it stays an expense even if an income keyword
    // also matched — e.g. "$50 charged to your credit card" contains "credit".
    private val chargeKeywords = listOf(
        "charged", "you spent", "spent", "purchase", "purchased", "swiped",
        "debited", "debit of", "withdraw", "withdrawn", "bought"
    )

    data class Parsed(
        val amountCents: Long,
        val isIncome: Boolean,
        val needsReview: Boolean
    )

    fun parse(text: String): Parsed? {
        if (text.isBlank()) return null
        val lower = text.lowercase()

        // Drop informational alerts before they can become transactions.
        if (ignorePhrases.any { lower.contains(it) }) return null

        val match = amountRegex.find(text) ?: return null
        val numeric = match.groupValues[1].replace(",", "")
        val cents = try {
            (numeric.toDouble() * 100).roundToLong()
        } catch (e: NumberFormatException) {
            return null
        }
        if (cents <= 0) return null

        val looksIncome = incomeKeywords.any { lower.contains(it) }
        val looksExpense = expenseKeywords.any { lower.contains(it) }
        val definiteCharge = chargeKeywords.any { lower.contains(it) }

        // A definite charge is always an expense (the credit-card case). Past
        // that, be confident only when exactly one side matches; otherwise
        // default to expense (the common case) but flag it for the user.
        val isIncome = looksIncome && !looksExpense && !definiteCharge
        val needsReview = !definiteCharge && (looksIncome == looksExpense)

        return Parsed(cents, isIncome, needsReview)
    }
}
