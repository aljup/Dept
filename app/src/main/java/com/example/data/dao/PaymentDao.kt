package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY date DESC")
    fun getAllPayments(userId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllPaymentsSync(userId: Long): List<Payment>

    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPaymentsSystemWide(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE debtId = :debtId ORDER BY date DESC")
    fun getPaymentsForDebt(debtId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE debtId = :debtId ORDER BY date DESC")
    suspend fun getPaymentsForDebtSync(debtId: Long): List<Payment>

    @Query("SELECT * FROM payments WHERE userId = :userId AND personName = :personName ORDER BY date DESC")
    fun getPaymentsForPerson(userId: Long, personName: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    fun getPaymentById(id: Long): Flow<Payment?>

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentByIdSync(id: Long): Payment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    @Delete
    suspend fun deletePayment(payment: Payment)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM payments WHERE debtId = :debtId")
    suspend fun deletePaymentsByDebtId(debtId: Long)

    @Query("DELETE FROM payments WHERE personName = :personName")
    suspend fun deletePaymentsByPerson(personName: String)

    @Query("UPDATE payments SET personName = :newName WHERE personName = :oldName")
    suspend fun updatePersonNameInPayments(oldName: String, newName: String)

    @Query("SELECT SUM(amount) FROM payments WHERE debtId = :debtId")
    fun getTotalPaidForDebt(debtId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM payments WHERE debtId = :debtId")
    suspend fun getTotalPaidForDebtSync(debtId: Long): Double?

    @Query("SELECT SUM(amount) FROM payments WHERE userId = :userId")
    fun getTotalPaymentsUser(userId: Long): Flow<Double?>
}
