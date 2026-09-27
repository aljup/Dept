package com.example.data.model

sealed interface LedgerEntry {
    val entryId: String
    val date: Long
    val title: String
    val personName: String
    val amount: Double
    val badge: String
    val notes: String
    val isPositiveCashflow: Boolean // true: inward/received (أخضر), false: outward/payable (أحمر)

    data class DebtEntry(
        val debt: Debt
    ) : LedgerEntry {
        override val entryId: String = "debt_${debt.id}"
        override val date: Long = debt.date
        override val title: String = if (debt.type == Debt.TYPE_CREDITOR) "إقراض / تسجيل مستحق لك" else "اقتراض / التزام مالي عليك"
        override val personName: String = debt.name
        override val amount: Double = debt.amount
        override val badge: String = "${debt.category} • ${if (debt.isPaid) "مسدد ✓" else "نشط"}"
        override val notes: String = debt.notes
        override val isPositiveCashflow: Boolean = debt.type == Debt.TYPE_CREDITOR
    }

    data class PaymentEntry(
        val payment: Payment,
        val debtType: String? = null
    ) : LedgerEntry {
        override val entryId: String = "payment_${payment.id}"
        override val date: Long = payment.date
        override val title: String = if (debtType == Debt.TYPE_CREDITOR) "استلام / تحصيل دفعة" else "سداد / دفع قسط"
        override val personName: String = payment.personName
        override val amount: Double = payment.amount
        override val badge: String = "دفعة • ${payment.method}"
        override val notes: String = payment.notes
        override val isPositiveCashflow: Boolean = debtType == Debt.TYPE_CREDITOR
    }
}
