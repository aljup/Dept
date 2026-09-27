package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing an installment or payment against a debt (دفعة سداد أو تحصيل).
 */
@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["debtId"]),
        Index(value = ["userId"]),
        Index(value = ["personName"])
    ]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val debtId: Long,
    val userId: Long,
    val personName: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val method: String = METHOD_CASH,
    val notes: String = ""
) {
    companion object {
        const val METHOD_CASH = "نقدي"
        const val METHOD_BANK_TRANSFER = "تحويل بنكي"
        const val METHOD_CARD = "بطاقة / شبكة"
        const val METHOD_CHECK = "شيك"
        const val METHOD_OTHER = "أخرى"

        val ALL_METHODS = listOf(
            METHOD_CASH,
            METHOD_BANK_TRANSFER,
            METHOD_CARD,
            METHOD_CHECK,
            METHOD_OTHER
        )
    }
}
