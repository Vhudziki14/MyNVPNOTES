package com.example.mynvpnotes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteTitle = intent.getStringExtra("NOTE_TITLE") ?: "Note Reminder"
        val noteContent = intent.getStringExtra("NOTE_CONTENT") ?: "Check your note."
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel for Android Oreo (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "note_reminders", // Channel ID
                "Note Reminders", // Channel Name
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for note reminder notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notification = NotificationCompat.Builder(context, "note_reminders")
            .setSmallIcon(R.drawable.ic_notification) // Ensure this drawable exists
            .setContentTitle(noteTitle)
            .setContentText(noteContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Show the notification using a unique ID
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
