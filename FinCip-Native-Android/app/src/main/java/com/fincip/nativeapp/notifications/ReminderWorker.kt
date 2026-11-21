package com.fincip.nativeapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fincip.nativeapp.R
import com.fincip.nativeapp.data.FinanceStore
import com.fincip.nativeapp.data.arrayOrEmpty
import com.fincip.nativeapp.data.objects
import com.fincip.nativeapp.data.string
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val raw = runCatching { FinanceStore(context).loadWorkspace() }.getOrNull() ?: return Result.success()
        val workspace = runCatching { com.google.gson.JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return Result.success()
        val prefs = context.getSharedPreferences("fincip_reminder_state", Context.MODE_PRIVATE)
        workspace.arrayOrEmpty("recurringRules").objects().forEach { bill ->
            val date = runCatching { LocalDate.parse(bill.string("nextDate")) }.getOrNull() ?: return@forEach
            val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
            val id = bill.string("id", bill.string("title"))
            val key = "${id}_${date}"
            if (days in 0..3 && !prefs.getBoolean(key, false)) {
                val channelId = "fincip_bills"
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(channelId, "Bill reminders", NotificationManager.IMPORTANCE_DEFAULT))
                val amount = bill.string("currency", "INR") + " " + bill.get("amount")?.asString.orEmpty()
                val title = bill.string("title", "Upcoming payment")
                val note = when (days) { 0L -> "Due today · $amount"; 1L -> "Due tomorrow · $amount"; else -> "Due in $days days · $amount" }
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(note)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(note))
                    .setAutoCancel(true)
                    .build()
                NotificationManagerCompat.from(context).notify(key.hashCode(), notification)
                prefs.edit().putBoolean(key, true).apply()
            }
        }
        return Result.success()
    }
}
