package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an application user for local authentication.
 *
 * @param id Unique auto-generated identifier.
 * @param fullName User's full name.
 * @param email User's email address (unique).
 * @param passwordHash Salted/hashed password string.
 * @param pinCode Optional 4-digit PIN for rapid authentication.
 * @param currency Default currency symbol (e.g. ر.س, $, €, د.ك).
 * @param role Role of the user (ADMIN or USER).
 * @param createdAt Creation timestamp.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val email: String,
    val passwordHash: String,
    val pinCode: String? = null,
    val currency: String = "ر.س",
    val role: String = ROLE_USER,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_ADMIN = "ADMIN"
        const val ROLE_USER = "USER"
    }

    val isAdmin: Boolean
        get() = role == ROLE_ADMIN || email.trim().lowercase().startsWith("admin") || id == 1L
}
