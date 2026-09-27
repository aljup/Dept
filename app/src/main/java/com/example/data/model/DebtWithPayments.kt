package com.example.data.model

data class DebtWithPayments(
    val debt: Debt,
    val payments: List<Payment> = emptyList(),
    val totalPaid: Double = 0.0,
    val remainingAmount: Double = debt.amount,
    val progress: Float = 0f
) {
    companion object {
        fun from(debt: Debt, payments: List<Payment>): DebtWithPayments {
            val totalPaid = payments.sumOf { it.amount }
            val remaining = (debt.amount - totalPaid).coerceAtLeast(0.0)
            val progress = if (debt.amount > 0.0) {
                ((totalPaid / debt.amount).toFloat()).coerceIn(0f, 1f)
            } else 1f

            return DebtWithPayments(
                debt = debt,
                payments = payments,
                totalPaid = totalPaid,
                remainingAmount = remaining,
                progress = progress
            )
        }
    }
}
