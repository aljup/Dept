package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Person
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Query("SELECT * FROM persons WHERE userId = :userId ORDER BY name ASC")
    fun getAllPersons(userId: Long): Flow<List<Person>>

    @Query("SELECT * FROM persons WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllPersonsSync(userId: Long): List<Person>

    @Query("SELECT * FROM persons ORDER BY name ASC")
    fun getAllPersonsSystemWide(): Flow<List<Person>>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    fun getPersonById(id: Long): Flow<Person?>

    @Query("SELECT * FROM persons WHERE id = :id LIMIT 1")
    suspend fun getPersonByIdSync(id: Long): Person?

    @Query("SELECT * FROM persons WHERE userId = :userId AND name = :name LIMIT 1")
    suspend fun getPersonByName(userId: Long, name: String): Person?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: Person): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPersons(persons: List<Person>)

    @Update
    suspend fun updatePerson(person: Person)

    @Delete
    suspend fun deletePerson(person: Person)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deletePersonById(id: Long)

    @Query("DELETE FROM persons WHERE userId = :userId")
    suspend fun deletePersonsByUserId(userId: Long)

    @Query("SELECT COUNT(*) FROM persons WHERE userId = :userId")
    suspend fun countPersons(userId: Long): Int
}
