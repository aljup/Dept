package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Debt
import com.example.notification.DebtReminderScheduler
import com.example.notification.NotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ديوني", appName)
  }

  @Test
  fun `notification channel is created properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    NotificationHelper.createNotificationChannel(context)
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = manager.getNotificationChannel(NotificationHelper.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals("تذكيرات مواعيد استحقاق الديون", channel.name)
  }

  @Test
  fun `schedule and cancel debt reminder does not crash`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val debt = Debt(
        id = 101L,
        userId = 1L,
        name = "محمد أحمد",
        amount = 750.0,
        type = Debt.TYPE_CREDITOR,
        date = System.currentTimeMillis(),
        dueDate = System.currentTimeMillis() + 86400000L,
        category = "شخصي",
        status = Debt.STATUS_ACTIVE
    )
    DebtReminderScheduler.scheduleDebtReminder(context, debt)
    DebtReminderScheduler.cancelDebtReminder(context, debt.id)
    DebtReminderScheduler.scheduleDailyPeriodicCheck(context)
  }
}

