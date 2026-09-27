package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Debt
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY date DESC")
    fun getAllDebts(userId: Long): Flow<List<Debt>>

    @Query("SELECT * FROM debts ORDER BY date DESC")
    fun getAllDebtsSystemWide(): Flow<List<Debt>>

    @Query("SELECT * FROM debts ORDER BY date DESC")
    suspend fun getAllDebtsSystemWideSync(): List<Debt>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    fun getDebtById(id: Long): Flow<Debt?>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getDebtByIdSync(id: Long): Debt?

    @Query("SELECT * FROM debts WHERE userId = :userId AND status = :status ORDER BY date DESC")
    fun getDebtsByStatus(userId: Long, status: String): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE userId = :userId AND type = :type ORDER BY date DESC")
    fun getDebtsByType(userId: Long, type: String): Flow<List<Debt>>

    @Query("SELECT * FROM debts WHERE name = :personName ORDER BY date DESC")
    fun getDebtsByPerson(personName: String): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)

    @Query("UPDATE debts SET status = :status WHERE id = :id")
    suspend fun updateDebtStatus(id: Long, status: String)

    @Query("UPDATE debts SET name = :newName WHERE name = :oldName")
    suspend fun renamePersonInDebts(oldName: String, newName: String)

    @Query("DELETE FROM debts WHERE name = :personName")
    suspend fun deleteDebtsByPerson(personName: String)

    @Query("UPDATE debts SET status = 'PAID' WHERE name = :personName")
    suspend fun settleDebtsByPerson(personName: String)

    @Query("UPDATE debts SET category = :newCategory WHERE category = :oldCategory")
    suspend fun renameCategoryInDebts(oldCategory: String, newCategory: String)

    @Query("SELECT DISTINCT name FROM debts ORDER BY name ASC")
    fun getAllPersonNames(): Flow<List<String>>

    @Query("SELECT DISTINCT category FROM debts")
    fun getAllUsedCategories(): Flow<List<String>>

    @Query("DELETE FROM debts WHERE userId = :userId")
    suspend fun deleteDebtsByUserId(userId: Long)

    @Query("SELECT SUM(amount) FROM debts WHERE userId = :userId AND type = 'CREDITOR' AND status = 'ACTIVE'")
    fun getTotalLent(userId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM debts WHERE userId = :userId AND type = 'DEBTOR' AND status = 'ACTIVE'")
    fun getTotalBorrowed(userId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM debts WHERE userId = :userId AND status = 'PAID'")
    fun getTotalPaid(userId: Long): Flow<Double?>

    @Query("DELETE FROM debts WHERE name IN ('محمد خالد', 'مؤسسة الأفق للتجارة', 'شركة التجهيزات المكتبية', 'سالم عبد الله')")
    suspend fun purgeSampleDebts()
}
