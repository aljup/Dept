package com.example.data.repository

import com.example.data.dao.DebtDao
import com.example.data.dao.PaymentDao
import com.example.data.dao.PersonDao
import com.example.data.model.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PersonRepository(
    private val personDao: PersonDao,
    private val debtDao: DebtDao,
    private val paymentDao: PaymentDao
) {

    fun getAllPersons(userId: Long): Flow<List<Person>> = personDao.getAllPersons(userId)

    fun getPersonById(id: Long): Flow<Person?> = personDao.getPersonById(id)

    suspend fun savePerson(person: Person): Result<Long> = withContext(Dispatchers.IO) {
        val trimmedName = person.name.trim()
        if (trimmedName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("يرجى إدخال اسم الشخص أو الجهة"))
        }

        try {
            val existing = personDao.getPersonByName(person.userId, trimmedName)
            if (existing != null && existing.id != person.id) {
                return@withContext Result.failure(IllegalArgumentException("يوجد شخص مسجل بهذا الاسم بالفعل"))
            }

            val id = if (person.id == 0L) {
                personDao.insertPerson(person.copy(name = trimmedName))
            } else {
                personDao.updatePerson(person.copy(name = trimmedName))
                person.id
            }
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePersonWithRename(oldName: String, updatedPerson: Person): Result<Unit> = withContext(Dispatchers.IO) {
        val newName = updatedPerson.name.trim()
        if (newName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("الاسم الجديد غير صالح"))
        }

        try {
            personDao.updatePerson(updatedPerson.copy(name = newName))
            if (oldName != newName) {
                debtDao.renamePersonInDebts(oldName, newName)
                paymentDao.updatePersonNameInPayments(oldName, newName)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePerson(person: Person, deleteAssociatedDebts: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            personDao.deletePerson(person)
            if (deleteAssociatedDebts) {
                debtDao.deleteDebtsByPerson(person.name)
                paymentDao.deletePaymentsByPerson(person.name)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Auto-sync: Creates entries in persons table for any unique names in debts that aren't yet in persons.
     */
    suspend fun syncPersonsFromDebts(userId: Long) = withContext(Dispatchers.IO) {
        val existingPersons = personDao.getAllPersonsSync(userId).map { it.name.trim().lowercase() }.toSet()
        val allDebts = debtDao.getAllDebtsSystemWideSync().filter { it.userId == userId }

        val newPersonsToAdd = mutableListOf<Person>()
        for (debt in allDebts) {
            val name = debt.name.trim()
            if (name.isNotEmpty() && !existingPersons.contains(name.lowercase())) {
                if (newPersonsToAdd.none { it.name.equals(name, ignoreCase = true) }) {
                    newPersonsToAdd.add(
                        Person(
                            userId = userId,
                            name = name,
                            phone = "",
                            notes = "تمت إضافته تلقائياً من سجل المعاملات",
                            createdAt = debt.date
                        )
                    )
                }
            }
        }

        if (newPersonsToAdd.isNotEmpty()) {
            personDao.insertPersons(newPersonsToAdd)
        }
    }
}
