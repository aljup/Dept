package com.example.data.repository

import com.example.data.dao.DebtDao
import com.example.data.model.Debt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DebtRepository(private val debtDao: DebtDao) {

    fun getAllDebts(userId: Long): Flow<List<Debt>> = debtDao.getAllDebts(userId)

    fun getAllDebtsSystemWide(): Flow<List<Debt>> = debtDao.getAllDebtsSystemWide()

    fun getDebtById(id: Long): Flow<Debt?> = debtDao.getDebtById(id)

    suspend fun getDebtByIdSync(id: Long): Debt? = withContext(Dispatchers.IO) {
        debtDao.getDebtByIdSync(id)
    }

    suspend fun insertDebt(debt: Debt): Long = withContext(Dispatchers.IO) {
        debtDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: Debt) = withContext(Dispatchers.IO) {
        debtDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: Debt) = withContext(Dispatchers.IO) {
        debtDao.deleteDebt(debt)
    }

    suspend fun deleteDebtById(id: Long) = withContext(Dispatchers.IO) {
        debtDao.deleteDebtById(id)
    }

    suspend fun toggleDebtStatus(debt: Debt) = withContext(Dispatchers.IO) {
        val newStatus = if (debt.status == Debt.STATUS_ACTIVE) Debt.STATUS_PAID else Debt.STATUS_ACTIVE
        debtDao.updateDebt(debt.copy(status = newStatus))
    }

    suspend fun markAsPaid(id: Long) = withContext(Dispatchers.IO) {
        debtDao.updateDebtStatus(id, Debt.STATUS_PAID)
    }

    suspend fun markAsActive(id: Long) = withContext(Dispatchers.IO) {
        debtDao.updateDebtStatus(id, Debt.STATUS_ACTIVE)
    }

    suspend fun renamePerson(oldName: String, newName: String) = withContext(Dispatchers.IO) {
        debtDao.renamePersonInDebts(oldName.trim(), newName.trim())
    }

    suspend fun deleteDebtsByPerson(name: String) = withContext(Dispatchers.IO) {
        debtDao.deleteDebtsByPerson(name.trim())
    }

    suspend fun settleDebtsByPerson(name: String) = withContext(Dispatchers.IO) {
        debtDao.settleDebtsByPerson(name.trim())
    }

    fun getAllPersonNames(): Flow<List<String>> = debtDao.getAllPersonNames()

    fun getDebtsByPerson(name: String): Flow<List<Debt>> = debtDao.getDebtsByPerson(name.trim())

    fun getTotalLent(userId: Long): Flow<Double?> = debtDao.getTotalLent(userId)

    fun getTotalBorrowed(userId: Long): Flow<Double?> = debtDao.getTotalBorrowed(userId)

    fun getTotalPaid(userId: Long): Flow<Double?> = debtDao.getTotalPaid(userId)

    suspend fun deleteDebtsByUserId(userId: Long) = withContext(Dispatchers.IO) {
        debtDao.deleteDebtsByUserId(userId)
    }

    suspend fun purgeSampleDebts() = withContext(Dispatchers.IO) {
        debtDao.purgeSampleDebts()
    }
}
