package com.marvyn.fintrack.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY postedAt DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    /** Used to suppress duplicate notifications for the same charge. */
    @Query("SELECT COUNT(*) FROM transactions WHERE sourcePackage = :pkg AND amountCents = :amount AND postedAt >= :since")
    suspend fun countRecent(pkg: String, amount: Long, since: Long): Int
}
