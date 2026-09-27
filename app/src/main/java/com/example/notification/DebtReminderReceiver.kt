package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.model.Debt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DebtReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        DebtReminderScheduler.rescheduleAllActiveDebts(context)
                    }

                    DebtReminderScheduler.ACTION_REMIND_DEBT -> {
                        val debtId = intent.getLongExtra(DebtReminderScheduler.EXTRA_DEBT_ID, -1L)
                        if (debtId != -1L) {
                            val db = AppDatabase.getDatabase(context)
                            val debt = db.debtDao().getDebtByIdSync(debtId)
                            if (debt != null && debt.status == Debt.STATUS_ACTIVE) {
                                NotificationHelper.showDebtNotification(context, debt)
                            }
                        }
                    }

                    DebtReminderScheduler.ACTION_DAILY_CHECK -> {
                        NotificationHelper.checkUpcomingDebtsAndNotify(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
