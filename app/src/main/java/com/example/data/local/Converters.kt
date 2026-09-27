package com.example.data.local

import androidx.room.TypeConverter
import java.util.Date

/**
 * Room TypeConverters for managing conversions between custom types and SQLite primitives.
 */
class Converters {

    // Date <-> Long converters
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // List<String> <-> String (comma-separated or delimited) converters for categories/tags
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}
