package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.CategoryDao
import com.example.data.dao.DebtDao
import com.example.data.dao.PaymentDao
import com.example.data.dao.PersonDao
import com.example.data.dao.UserDao
import com.example.data.model.Category
import com.example.data.model.Debt
import com.example.data.model.Payment
import com.example.data.model.Person
import com.example.data.model.User

@Database(
    entities = [Debt::class, User::class, Category::class, Person::class, Payment::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun debtDao(): DebtDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun personDao(): PersonDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "debt_manager_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
