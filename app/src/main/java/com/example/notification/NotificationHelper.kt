package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.Debt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object NotificationHelper {

    const val CHANNEL_ID = "debt_due_reminders_channel"
    private const val CHANNEL_NAME = "تذكيرات مواعيد استحقاق الديون"
    private const val CHANNEL_DESCRIPTION = "إشعارات لتذكيرك بمواعيد استحقاق سداد وتحصيل الديون والمستحقات المالية"
    private const val PREFS_NOTIFICATIONS = "debt_notification_prefs"
    private const val KEY_LAST_NOTIFIED_PREFIX = "notified_debt_"

    /**
     * Initializes the notification channel on Android 8.0 (API 26) and above.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Posts an immediate notification for a specific debt.
     */
    fun showDebtNotification(context: Context, debt: Debt) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_DEBT_ID", debt.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            debt.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val numFormatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
        }
        val formattedAmount = numFormatter.format(debt.amount)

        val isCreditor = debt.type == Debt.TYPE_CREDITOR
        val title = if (isCreditor) {
            "تذكير بموعد تحصيل مستحق: ${debt.name} 💰"
        } else {
            "تذكير بموعد سداد التزام: ${debt.name} ⚠️"
        }

        val dueDateStr = debt.dueDate?.let {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            sdf.format(Date(it))
        } ?: "غير محدد"

        val daysDifference = debt.dueDate?.let { getDaysDifference(it) } ?: 0
        val timingStatus = when {
            daysDifference < 0 -> "متأخر منذ ${-daysDifference} يوم!"
            daysDifference == 0L -> "يستحق اليوم!"
            daysDifference == 1L -> "يستحق غداً"
            else -> "يستحق خلال $daysDifference أيام ($dueDateStr)"
        }

        val bigText = buildString {
            if (isCreditor) {
                append("لك مبلغ مستحق بقيمة: $formattedAmount\n")
            } else {
                append("عليك التزام مالي بقيمة: $formattedAmount\n")
            }
            append("حالة الموعد: $timingStatus\n")
            append("التصنيف: ${debt.category}")
            if (debt.notes.isNotBlank()) {
                append("\nملاحظة: ${debt.notes}")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_bell)
            .setContentTitle(title)
            .setContentText("${if (isCreditor) "مستحق لك" else "التزام عليك"}: $formattedAmount • $timingStatus")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(if (isCreditor) 0xFF059669.toInt() else 0xFFDC2626.toInt())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(debt.id.toInt(), notification)
            recordNotificationSent(context, debt.id)
        } catch (_: SecurityException) {
            // Handled when notification permission is not granted yet
        }
    }

    /**
     * Checks all active debts in database and triggers notifications for those upcoming or due today.
     * Returns the count of notifications sent.
     */
    suspend fun checkUpcomingDebtsAndNotify(context: Context): Int = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val allDebts = db.debtDao().getAllDebtsSystemWideSync()

        var countSent = 0
        val now = System.currentTimeMillis()

        for (debt in allDebts) {
            if (debt.status != Debt.STATUS_ACTIVE) continue
            val due = debt.dueDate ?: continue

            val daysDiff = getDaysDifference(due)
            // Remind if: Overdue (up to 7 days), Due Today, Due Tomorrow, or Due within 3 days
            val shouldRemind = daysDiff in -7L..2L

            if (shouldRemind && !wasNotifiedToday(context, debt.id)) {
                showDebtNotification(context, debt)
                countSent++
            }
        }

        countSent
    }

    /**
     * Calculates the difference in days between the target timestamp and today.
     * 0 = today, 1 = tomorrow, -1 = yesterday.
     */
    private fun getDaysDifference(targetTimestamp: Long): Long {
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = targetTimestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
    }

    private fun wasNotifiedToday(context: Context, debtId: Long): Boolean {
        val prefs = getPrefs(context)
        val lastNotified = prefs.getLong(KEY_LAST_NOTIFIED_PREFIX + debtId, 0L)
        if (lastNotified == 0L) return false

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return lastNotified >= todayCal.timeInMillis
    }

    private fun recordNotificationSent(context: Context, debtId: Long) {
        val prefs = getPrefs(context)
        prefs.edit().putLong(KEY_LAST_NOTIFIED_PREFIX + debtId, System.currentTimeMillis()).apply()
    }
}
