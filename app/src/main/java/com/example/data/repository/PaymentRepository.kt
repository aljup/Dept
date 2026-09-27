package com.example.data.repository

import com.example.data.dao.DebtDao
import com.example.data.dao.PaymentDao
import com.example.data.model.Debt
import com.example.data.model.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val debtDao: DebtDao
) {

    fun getAllPayments(userId: Long): Flow<List<Payment>> = paymentDao.getAllPayments(userId)

    fun getAllPaymentsSystemWide(): Flow<List<Payment>> = paymentDao.getAllPaymentsSystemWide()

    fun getPaymentsForDebt(debtId: Long): Flow<List<Payment>> = paymentDao.getPaymentsForDebt(debtId)

    fun getPaymentsForPerson(userId: Long, personName: String): Flow<List<Payment>> =
        paymentDao.getPaymentsForPerson(userId, personName)

    fun getPaymentById(id: Long): Flow<Payment?> = paymentDao.getPaymentById(id)

    fun getTotalPaidForDebt(debtId: Long): Flow<Double?> = paymentDao.getTotalPaidForDebt(debtId)

    suspend fun addPayment(payment: Payment): Result<Long> = withContext(Dispatchers.IO) {
        if (payment.amount <= 0.0) {
            return@withContext Result.failure(IllegalArgumentException("يجب أن يكون مبلغ الدفعة أكبر من صفر"))
        }

        try {
            val id = paymentDao.insertPayment(payment)
            recalculateDebtStatus(payment.debtId)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePayment(payment: Payment): Result<Unit> = withContext(Dispatchers.IO) {
        if (payment.amount <= 0.0) {
            return@withContext Result.failure(IllegalArgumentException("يجب أن يكون مبلغ الدفعة أكبر من صفر"))
        }

        try {
            paymentDao.updatePayment(payment)
            recalculateDebtStatus(payment.debtId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePayment(payment: Payment): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            paymentDao.deletePayment(payment)
            recalculateDebtStatus(payment.debtId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePaymentById(id: Long, debtId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            paymentDao.deletePaymentById(id)
            recalculateDebtStatus(debtId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recalculates the total payments for the given debt and automatically adjusts its status.
     * If total paid >= debt amount, marks as PAID. Otherwise marks as ACTIVE.
     */
    private suspend fun recalculateDebtStatus(debtId: Long) {
        val debt = debtDao.getDebtByIdSync(debtId) ?: return
        val totalPaid = paymentDao.getTotalPaidForDebtSync(debtId) ?: 0.0

        val newStatus = if (totalPaid >= debt.amount) {
            Debt.STATUS_PAID
        } else {
            Debt.STATUS_ACTIVE
        }

        if (debt.status != newStatus) {
            debtDao.updateDebt(debt.copy(status = newStatus))
        }
    }
}
