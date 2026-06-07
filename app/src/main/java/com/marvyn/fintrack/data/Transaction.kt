package com.marvyn.fintrack.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single money movement. Amounts are stored as integer cents to avoid
 * floating-point rounding problems.
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountCents: Long,
    val isIncome: Boolean,
    val description: String,
    val sourcePackage: String,
    val sourceApp: String,
    val postedAt: Long,
    val category: String? = null,
    val rawText: String = "",
    /** true when the parser could not confidently tell income from expense */
    val needsReview: Boolean = false
)
