package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an individual person or organization (شخص أو جهة تعامل).
 */
@Entity(
    tableName = "persons",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "name"], unique = true)
    ]
)
data class Person(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
