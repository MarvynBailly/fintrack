package com.marvyn.fintrack.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionParserTest {

    @Test
    fun creditCardCharge_isExpense_notFlagged() {
        val p = TransactionParser.parse("\$50.00 charged to your credit card ending 1234")!!
        assertEquals(5000L, p.amountCents)
        assertFalse("a card charge must not be income", p.isIncome)
        assertFalse("a clear charge should not need review", p.needsReview)
    }

    @Test
    fun creditCardPurchaseApproved_isExpense() {
        val p = TransactionParser.parse(
            "Purchase approved: \$12.99 at Starbucks on your Visa credit card"
        )!!
        assertFalse(p.isIncome)
        assertFalse(p.needsReview)
    }

    @Test
    fun statementCredit_staysIncome() {
        val p = TransactionParser.parse("A credit of \$25.00 was posted to your account")!!
        assertTrue(p.isIncome)
    }

    @Test
    fun refundToCard_staysIncome() {
        val p = TransactionParser.parse("Refund of \$30.00 to your card ending 1234")!!
        assertTrue(p.isIncome)
    }

    @Test
    fun availableCredit_isIgnored() {
        assertNull(TransactionParser.parse("Your available credit is now \$5,000.00"))
    }

    @Test
    fun creditLimit_isIgnored() {
        assertNull(TransactionParser.parse("Your credit limit has been increased to \$10,000"))
    }

    @Test
    fun statementBalance_isIgnored() {
        assertNull(
            TransactionParser.parse("Your statement balance of \$542.10 is ready to view")
        )
    }

    @Test
    fun paymentDueReminder_isIgnored() {
        assertNull(
            TransactionParser.parse("Reminder: minimum payment of \$35.00 is due June 20")
        )
    }

    @Test
    fun rewardsBalance_isIgnored() {
        assertNull(TransactionParser.parse("You have a rewards balance of \$42.18"))
    }

    @Test
    fun successfulAutopayCharge_isStillRecorded() {
        // A completed autopay is a real expense; only scheduled/upcoming
        // autopay reminders should be skipped.
        val p = TransactionParser.parse("Your autopay payment of \$80.00 to Electric Co was paid")!!
        assertFalse(p.isIncome)
    }

    @Test
    fun spendWithRemainingBalance_capturesTheSpend() {
        // The spend amount comes first; the trailing balance must not hijack it.
        val p = TransactionParser.parse(
            "You spent \$50.00 at Target. Remaining balance: \$1,200.00"
        )!!
        assertEquals(5000L, p.amountCents)
        assertFalse(p.isIncome)
    }

    @Test
    fun plainDeposit_isIncome() {
        val p = TransactionParser.parse("Direct deposit of \$2,000.00 received")!!
        assertTrue(p.isIncome)
        assertFalse(p.needsReview)
    }

    @Test
    fun noAmount_returnsNull() {
        assertNull(TransactionParser.parse("Your card was used today"))
    }
}
