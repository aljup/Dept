package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.model.Debt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object DebtReminderScheduler {

    const val ACTION_REMIND_DEBT = "com.example.notification.ACTION_REMIND_DEBT"
    const val ACTION_DAILY_CHECK = "com.example.notification.ACTION_DAILY_CHECK"
    const val EXTRA_DEBT_ID = "extra_debt_id"

    private const val DAILY_CHECK_REQUEST_CODE = 999901

    /**
     * Schedules a local alarm for a specific debt on its due date.
     */
    fun scheduleDebtReminder(context: Context, debt: Debt) {
        val dueDate = debt.dueDate ?: return
        if (debt.status != Debt.STATUS_ACTIVE) {
            cancelDebtReminder(context, debt.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Schedule at 9:00 AM on the due date (or 10:00 AM if created after 9:00 AM on that day)
        val reminderCal = Calendar.getInstance().apply {
            timeInMillis = dueDate
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        var triggerTime = reminderCal.timeInMillis

        // If 9 AM on due date has already passed, but due date is today, trigger in 1 minute
        if (triggerTime <= now) {
            val endOfDay = Calendar.getInstance().apply {
                timeInMillis = dueDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            if (now < endOfDay) {
                triggerTime = now + 60_000 // In 1 minute
            } else {
                // Already passed
                return
            }
        }

        val intent = Intent(context, DebtReminderReceiver::class.java).apply {
            action = ACTION_REMIND_DEBT
            putExtra(EXTRA_DEBT_ID, debt.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            debt.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    /**
     * Cancels any scheduled reminder for a specific debt.
     */
    fun cancelDebtReminder(context: Context, debtId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DebtReminderReceiver::class.java).apply {
            action = ACTION_REMIND_DEBT
            putExtra(EXTRA_DEBT_ID, debtId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            debtId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Schedules a recurring daily check every morning at 09:00 AM to inspect upcoming due dates.
     */
    fun scheduleDailyPeriodicCheck(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, DebtReminderReceiver::class.java).apply {
            action = ACTION_DAILY_CHECK
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_CHECK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /**
     * Reschedules reminders for all active debts in the system (e.g., upon device reboot or app launch).
     */
    suspend fun rescheduleAllActiveDebts(context: Context) = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val allDebts = db.debtDao().getAllDebtsSystemWideSync()

        for (debt in allDebts) {
            if (debt.status == Debt.STATUS_ACTIVE && debt.dueDate != null) {
                scheduleDebtReminder(context, debt)
            }
        }

        scheduleDailyPeriodicCheck(context)
    }
}
