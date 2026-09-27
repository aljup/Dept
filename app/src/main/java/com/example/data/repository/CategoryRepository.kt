package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.DebtDao
import com.example.data.model.Category
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val debtDao: DebtDao
) {

    private val defaultCategories = listOf(
        "شخصي",
        "عمل",
        "عائلي",
        "قرض",
        "سلفة",
        "تسوق",
        "عام"
    )

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun ensureDefaultCategories() = withContext(Dispatchers.IO) {
        if (categoryDao.count() == 0) {
            val list = defaultCategories.map { Category(name = it, isSystem = true) }
            categoryDao.insertCategories(list)
        }
    }

    suspend fun addCategory(name: String): Result<Long> = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(Exception("اسم التصنيف لا يمكن أن يكون فارغاً"))
        }
        val existing = categoryDao.getAllCategoriesSync().any { it.name.equals(trimmed, ignoreCase = true) }
        if (existing) {
            return@withContext Result.failure(Exception("التصنيف موجود مسبقاً"))
        }

        try {
            val id = categoryDao.insertCategory(Category(name = trimmed, isSystem = false))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameCategory(category: Category, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(Exception("الاسم الجديد غير صالح"))
        }
        val oldName = category.name
        try {
            categoryDao.updateCategory(category.copy(name = trimmed))
            debtDao.renameCategoryInDebts(oldName, trimmed)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategory(category: Category): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            categoryDao.deleteCategory(category)
            // Re-assign debts with this category to 'عام'
            debtDao.renameCategoryInDebts(category.name, "عام")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
