package com.example.data.repository

import com.example.data.dao.UserDao
import com.example.data.model.User
import com.example.data.preferences.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    suspend fun register(
        fullName: String,
        email: String,
        passwordPlain: String,
        pinCode: String? = null,
        currency: String = "ر.س"
    ): Result<User> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val existing = userDao.getUserByEmailSync(normalizedEmail)
        if (existing != null) {
            return@withContext Result.failure(Exception("هذا البريد الإلكتروني مسجل مسبقاً!"))
        }

        val passwordHash = hashPassword(passwordPlain)
        val newUser = User(
            fullName = fullName.trim(),
            email = normalizedEmail,
            passwordHash = passwordHash,
            pinCode = pinCode?.trim()?.ifEmpty { null },
            currency = currency
        )

        try {
            val generatedId = userDao.insertUser(newUser)
            val createdUser = newUser.copy(id = generatedId)
            sessionManager.saveLoginSession(
                userId = generatedId,
                email = normalizedEmail,
                name = createdUser.fullName,
                hasPin = !createdUser.pinCode.isNullOrEmpty(),
                pin = createdUser.pinCode,
                currency = currency
            )
            Result.success(createdUser)
        } catch (e: Exception) {
            Result.failure(Exception("فشل إنشاء الحساب: ${e.message}"))
        }
    }

    suspend fun login(email: String, passwordPlain: String): Result<User> = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val user = userDao.getUserByEmailSync(normalizedEmail)
            ?: return@withContext Result.failure(Exception("الحساب غير موجود. يرجى التأكد من البريد الإلكتروني"))

        val inputHash = hashPassword(passwordPlain)
        if (user.passwordHash != inputHash) {
            return@withContext Result.failure(Exception("كلمة المرور غير صحيحة"))
        }

        sessionManager.saveLoginSession(
            userId = user.id,
            email = user.email,
            name = user.fullName,
            hasPin = !user.pinCode.isNullOrEmpty(),
            pin = user.pinCode,
            currency = user.currency
        )
        Result.success(user)
    }

    suspend fun verifyPin(pin: String): Result<User> = withContext(Dispatchers.IO) {
        val currentUserId = sessionManager.getUserId()
        if (currentUserId == -1L) {
            return@withContext Result.failure(Exception("لا توجد جلسة نشطة"))
        }

        val user = userDao.getUserByIdSync(currentUserId)
            ?: return@withContext Result.failure(Exception("المستخدم غير موجود"))

        if (user.pinCode == pin.trim()) {
            Result.success(user)
        } else {
            Result.failure(Exception("رمز الـ PIN غير صحيح!"))
        }
    }

    suspend fun updatePin(userId: Long, newPin: String?) = withContext(Dispatchers.IO) {
        userDao.updatePinCode(userId, newPin)
    }

    suspend fun ensureDefaultAdminCreated(debtDao: com.example.data.dao.DebtDao? = null): User = withContext(Dispatchers.IO) {
        val adminEmail = "admin@duyuni.app"
        val existing = userDao.getUserByEmailSync(adminEmail)
            ?: userDao.getUserByEmailSync("admin@admin.com")

        // Clean up any old mock debts from previous tests
        debtDao?.purgeSampleDebts()

        if (existing != null) {
            return@withContext existing
        }

        val adminPasswordHash = hashPassword("admin123")
        val adminUser = User(
            fullName = "مشرف النظام",
            email = adminEmail,
            passwordHash = adminPasswordHash,
            pinCode = "1234",
            currency = "ر.س"
        )
        val adminId = userDao.insertUser(adminUser)
        adminUser.copy(id = adminId)
    }

    suspend fun getCurrentUser(): User? = withContext(Dispatchers.IO) {
        val id = sessionManager.getUserId()
        if (id != -1L) userDao.getUserByIdSync(id) else null
    }

    fun observeUser(id: Long): Flow<User?> = userDao.getUserById(id)

    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun deleteUser(userId: Long) = withContext(Dispatchers.IO) {
        userDao.deleteUserById(userId)
    }

    suspend fun resetUserPin(userId: Long, newPin: String? = null) = withContext(Dispatchers.IO) {
        userDao.updatePinCode(userId, newPin)
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun isUserLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun getSessionManager(): SessionManager = sessionManager
}
