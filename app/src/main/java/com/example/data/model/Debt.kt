package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a debt or financial transaction.
 *
 * @param id Unique auto-generated identifier.
 * @param userId ID of the account owner.
 * @param name Name of the debtor or creditor (المدين أو الدائن).
 * @param amount Total transaction amount (المبلغ).
 * @param type Transaction type: "CREDITOR" (لك / مستحق لك) or "DEBTOR" (عليك / مستحق عليك).
 * @param date Transaction timestamp (تاريخ المعاملة).
 * @param dueDate Optional repayment due date (تاريخ الاستحقاق).
 * @param notes Extra notes or description (ملاحظات).
 * @param status Status of the debt: "ACTIVE" (نشط) or "PAID" (مسدد).
 * @param category Category: "عام", "شخصي", "عمل", "عائلي", "قرض", "سلفة", etc.
 */
@Entity(
    tableName = "debts",
    indices = [Index(value = ["userId"]), Index(value = ["status"]), Index(value = ["type"])]
)
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val amount: Double,
    val type: String, // CREDITOR (لك) or DEBTOR (عليك)
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val notes: String = "",
    val status: String = STATUS_ACTIVE, // ACTIVE or PAID
    val category: String = "عام"
) {
    companion object {
        const val TYPE_CREDITOR = "CREDITOR" // لك (أقرضته)
        const val TYPE_DEBTOR = "DEBTOR"     // عليك (اقترضت منه)

        const val STATUS_ACTIVE = "ACTIVE"   // نشط
        const val STATUS_PAID = "PAID"       // مسدد
    }

    val isPaid: Boolean
        get() = status == STATUS_PAID

    val isCreditor: Boolean
        get() = type == TYPE_CREDITOR
}
